package io.openrudder.sources.mongodb;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.source.AbstractSource;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MongoSource extends AbstractSource<MongoSourceConfig> {

    private MongoClient mongoClient;

    public MongoSource(MongoSourceConfig config) {
        super(config);
    }

    public static MongoSourceBuilder builder() {
        return new MongoSourceBuilder();
    }

    @Override
    protected Flux<ChangeEvent> doStart() {
        mongoClient = MongoClients.create(config.getConnectionString());
        MongoDatabase database = mongoClient.getDatabase(config.getDatabase());

        return Flux.from(database.watch())
            .map(this::convertToChangeEvent)
            .doOnNext(this::emitEvent);
    }

    private ChangeEvent convertToChangeEvent(ChangeStreamDocument<Document> changeDoc) {
        OperationType operationType = changeDoc.getOperationType();
        
        ChangeEvent.ChangeType changeType = switch (operationType) {
            case INSERT -> ChangeEvent.ChangeType.INSERT;
            case UPDATE, REPLACE -> ChangeEvent.ChangeType.UPDATE;
            case DELETE -> ChangeEvent.ChangeType.DELETE;
            default -> null;
        };

        if (changeType == null) {
            return null;
        }

        Document fullDocument = changeDoc.getFullDocument();
        String namespace = changeDoc.getNamespace() != null ? 
            changeDoc.getNamespace().getCollectionName() : "unknown";
        
        Object documentKey = changeDoc.getDocumentKey() != null ? 
            changeDoc.getDocumentKey().get("_id") : null;

        Map<String, Object> afterData = fullDocument != null ? 
            new HashMap<>(fullDocument) : null;

        return ChangeEvent.builder()
            .id(java.util.UUID.randomUUID().toString())
            .type(changeType)
            .entityType(namespace)
            .entityId(documentKey)
            .after(afterData)
            .timestamp(Instant.now())
            .sourceId(getId())
            .build();
    }

    @Override
    protected Mono<Void> doStop() {
        return Mono.fromRunnable(() -> {
            if (mongoClient != null) {
                mongoClient.close();
            }
        });
    }

    @Override
    protected Flux<ChangeEvent> doSnapshot() {
        return Flux.empty();
    }

    public static class MongoSourceBuilder {
        private String name;
        private String connectionString;
        private String database;
        private java.util.List<String> collections;

        public MongoSourceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MongoSourceBuilder connectionString(String connectionString) {
            this.connectionString = connectionString;
            return this;
        }

        public MongoSourceBuilder database(String database) {
            this.database = database;
            return this;
        }

        public MongoSourceBuilder collections(java.util.List<String> collections) {
            this.collections = collections;
            return this;
        }

        public MongoSource build() {
            MongoSourceConfig config = MongoSourceConfig.builder()
                .name(name)
                .connectionString(connectionString)
                .database(database)
                .collections(collections)
                .build();
            
            return new MongoSource(config);
        }
    }
}
