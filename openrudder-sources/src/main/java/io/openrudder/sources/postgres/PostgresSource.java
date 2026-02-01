package io.openrudder.sources.postgres;

import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.source.AbstractSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class PostgresSource extends AbstractSource<PostgresSourceConfig> {

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine;
    private ExecutorService executor;

    public PostgresSource(PostgresSourceConfig config) {
        super(config);
    }

    public static PostgresSourceBuilder builder() {
        return new PostgresSourceBuilder();
    }

    @Override
    protected Flux<ChangeEvent> doStart() {
        if (!config.isCdcEnabled()) {
            return startPollingMode();
        }

        return startCdcMode();
    }

    private Flux<ChangeEvent> startCdcMode() {
        return Flux.<ChangeEvent>create(sink -> {
            try {
                Properties props = buildDebeziumConfig();
                
                Configuration debeziumConfig = Configuration.from(props);

                debeziumEngine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                    .using(debeziumConfig.asProperties())
                    .notifying(record -> {
                        try {
                            ChangeEvent changeEvent = convertToChangeEvent(record.record());
                            if (changeEvent != null) {
                                sink.next(changeEvent);
                                emitEvent(changeEvent);
                            }
                        } catch (Exception e) {
                            log.error("Error processing change event", e);
                            sink.error(e);
                        }
                    })
                    .using((success, message, error) -> {
                        if (error != null) {
                            log.error("Debezium engine error: {}", message, error);
                            sink.error(error);
                        } else {
                            log.info("Debezium engine completed: {}", message);
                            sink.complete();
                        }
                    })
                    .build();

                executor = Executors.newSingleThreadExecutor();
                executor.execute(debeziumEngine);

            } catch (Exception e) {
                log.error("Failed to start Debezium engine", e);
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Properties buildDebeziumConfig() {
        Properties props = new Properties();
        
        props.setProperty("name", "openrudder-postgres-connector-" + config.getName());
        props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore");
        props.setProperty("offset.flush.interval.ms", "60000");
        
        props.setProperty("database.hostname", config.getHost());
        props.setProperty("database.port", String.valueOf(config.getPort()));
        props.setProperty("database.user", config.getUsername());
        props.setProperty("database.password", config.getPassword());
        props.setProperty("database.dbname", config.getDatabase());
        props.setProperty("database.server.name", config.getName());
        
        if (config.getSchema() != null) {
            props.setProperty("schema.include.list", config.getSchema());
        }
        
        if (config.getTables() != null && !config.getTables().isEmpty()) {
            String tableList = String.join(",", config.getTables());
            props.setProperty("table.include.list", tableList);
        }
        
        if (config.getSlotName() != null) {
            props.setProperty("slot.name", config.getSlotName());
        } else {
            props.setProperty("slot.name", "openrudder_" + config.getName());
        }
        
        if (config.getPublicationName() != null) {
            props.setProperty("publication.name", config.getPublicationName());
        } else {
            props.setProperty("publication.name", "openrudder_publication");
        }
        
        props.setProperty("plugin.name", "pgoutput");
        props.setProperty("publication.autocreate.mode", "filtered");
        props.setProperty("snapshot.mode", "initial");
        
        return props;
    }

    private ChangeEvent convertToChangeEvent(SourceRecord record) {
        if (record.value() == null) {
            return null;
        }

        Struct value = (Struct) record.value();
        String operation = value.getString("op");
        
        if (operation == null) {
            return null;
        }

        ChangeEvent.ChangeType changeType = switch (operation) {
            case "c", "r" -> ChangeEvent.ChangeType.INSERT;
            case "u" -> ChangeEvent.ChangeType.UPDATE;
            case "d" -> ChangeEvent.ChangeType.DELETE;
            default -> null;
        };

        if (changeType == null) {
            return null;
        }

        Struct after = value.getStruct("after");
        Struct before = value.getStruct("before");
        Struct source = value.getStruct("source");

        String table = source.getString("table");
        Object entityId = extractPrimaryKey(after != null ? after : before);

        return ChangeEvent.builder()
            .id(java.util.UUID.randomUUID().toString())
            .type(changeType)
            .entityType(table)
            .entityId(entityId)
            .before(before != null ? structToMap(before) : null)
            .after(after != null ? structToMap(after) : null)
            .timestamp(Instant.now())
            .sourceId(getId())
            .metadata(extractMetadata(source))
            .build();
    }

    private Map<String, Object> structToMap(Struct struct) {
        Map<String, Object> map = new HashMap<>();
        for (Field field : struct.schema().fields()) {
            map.put(field.name(), struct.get(field));
        }
        return map;
    }

    private Object extractPrimaryKey(Struct struct) {
        if (struct == null) {
            return null;
        }
        
        if (struct.schema().field("id") != null) {
            return struct.get("id");
        }
        
        for (Field field : struct.schema().fields()) {
            if (field.name().endsWith("_id") || field.name().equals("pk")) {
                return struct.get(field);
            }
        }
        
        return null;
    }

    private Map<String, Object> extractMetadata(Struct source) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("database", source.getString("db"));
        metadata.put("schema", source.getString("schema"));
        metadata.put("table", source.getString("table"));
        metadata.put("lsn", source.getInt64("lsn"));
        return metadata;
    }

    private Flux<ChangeEvent> startPollingMode() {
        return Flux.error(new UnsupportedOperationException("Polling mode not yet implemented"));
    }

    @Override
    protected Mono<Void> doStop() {
        return Mono.fromRunnable(() -> {
            try {
                if (debeziumEngine != null) {
                    debeziumEngine.close();
                }
                if (executor != null) {
                    executor.shutdown();
                }
            } catch (Exception e) {
                log.error("Error stopping PostgresSource", e);
            }
        });
    }

    @Override
    protected Flux<ChangeEvent> doSnapshot() {
        return Flux.empty();
    }

    public static class PostgresSourceBuilder {
        private String name;
        private String host = "localhost";
        private int port = 5432;
        private String database;
        private String username;
        private String password;
        private String schema = "public";
        private java.util.List<String> tables;
        private boolean cdcEnabled = true;
        private String slotName;
        private String publicationName;

        public PostgresSourceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PostgresSourceBuilder host(String host) {
            this.host = host;
            return this;
        }

        public PostgresSourceBuilder port(int port) {
            this.port = port;
            return this;
        }

        public PostgresSourceBuilder database(String database) {
            this.database = database;
            return this;
        }

        public PostgresSourceBuilder username(String username) {
            this.username = username;
            return this;
        }

        public PostgresSourceBuilder password(String password) {
            this.password = password;
            return this;
        }

        public PostgresSourceBuilder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public PostgresSourceBuilder tables(java.util.List<String> tables) {
            this.tables = tables;
            return this;
        }

        public PostgresSourceBuilder table(String table) {
            if (this.tables == null) {
                this.tables = new java.util.ArrayList<>();
            }
            this.tables.add(table);
            return this;
        }

        public PostgresSourceBuilder cdcEnabled(boolean cdcEnabled) {
            this.cdcEnabled = cdcEnabled;
            return this;
        }

        public PostgresSourceBuilder connectionString(String connectionString) {
            return this;
        }

        public PostgresSource build() {
            PostgresSourceConfig config = PostgresSourceConfig.builder()
                .name(name)
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .schema(schema)
                .tables(tables)
                .cdcEnabled(cdcEnabled)
                .slotName(slotName)
                .publicationName(publicationName)
                .build();
            
            return new PostgresSource(config);
        }
    }
}
