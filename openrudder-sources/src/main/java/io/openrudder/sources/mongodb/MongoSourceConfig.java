package io.openrudder.sources.mongodb;

import io.openrudder.core.source.SourceConfig;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MongoSourceConfig implements SourceConfig {
    String name;
    String connectionString;
    String database;
    List<String> collections;
    @Builder.Default
    boolean enabled = true;
    @Builder.Default
    boolean fullDocument = true;
}
