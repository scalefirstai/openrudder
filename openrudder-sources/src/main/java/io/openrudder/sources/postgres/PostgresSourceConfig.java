package io.openrudder.sources.postgres;

import io.openrudder.core.source.SourceConfig;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class PostgresSourceConfig implements SourceConfig {
    String name;
    String host;
    int port;
    String database;
    String username;
    String password;
    String schema;
    List<String> tables;
    boolean cdcEnabled;
    String slotName;
    String publicationName;
    @Builder.Default
    boolean enabled = true;
    Map<String, Object> additionalProperties;

    public String getConnectionString() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
    }

    public String getJdbcUrl() {
        return getConnectionString();
    }
}
