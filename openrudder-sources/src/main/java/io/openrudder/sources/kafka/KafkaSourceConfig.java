package io.openrudder.sources.kafka;

import io.openrudder.core.source.SourceConfig;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class KafkaSourceConfig implements SourceConfig {
    String name;
    String bootstrapServers;
    List<String> topics;
    String groupId;
    @Builder.Default
    String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
    @Builder.Default
    String valueDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
    @Builder.Default
    boolean enabled = true;
    Map<String, Object> additionalProperties;
}
