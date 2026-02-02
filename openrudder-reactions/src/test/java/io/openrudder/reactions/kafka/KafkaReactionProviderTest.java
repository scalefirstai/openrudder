package io.openrudder.reactions.kafka;

import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ValidationResult;
import org.junit.jupiter.api.Test;
import reactor.kafka.sender.KafkaSender;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaReactionProviderTest {

    @Test
    void shouldReturnCorrectKind() {
        KafkaSender<String, String> sender = mock(KafkaSender.class);
        KafkaReactionProvider provider = new KafkaReactionProvider(sender);
        
        assertThat(provider.kind()).isEqualTo("kafka");
    }

    @Test
    void shouldCreateKafkaReaction() {
        KafkaSender<String, String> sender = mock(KafkaSender.class);
        KafkaReactionProvider provider = new KafkaReactionProvider(sender);
        ReactionConfig config = createConfig("test-topic");

        Reaction reaction = provider.create(config);

        assertThat(reaction).isInstanceOf(KafkaReaction.class);
        assertThat(reaction.id()).isEqualTo("kafka-1");
    }

    @Test
    void shouldValidateSuccessfullyWithValidConfig() {
        KafkaSender<String, String> sender = mock(KafkaSender.class);
        KafkaReactionProvider provider = new KafkaReactionProvider(sender);
        ReactionConfig config = createConfig("test-topic");

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldFailValidationWhenTopicMissing() {
        KafkaSender<String, String> sender = mock(KafkaSender.class);
        KafkaReactionProvider provider = new KafkaReactionProvider(sender);
        ReactionConfig config = new ReactionConfig(
            "kafka-1",
            "Kafka Reaction",
            "kafka",
            Set.of("query-1"),
            Map.of(),
            Map.of(),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("topic is required");
    }

    @Test
    void shouldFailValidationWhenSenderIsNull() {
        KafkaReactionProvider provider = new KafkaReactionProvider(null);
        ReactionConfig config = createConfig("test-topic");

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("KafkaSender is required");
    }

    @Test
    void shouldReturnConfigSchema() {
        KafkaSender<String, String> sender = mock(KafkaSender.class);
        KafkaReactionProvider provider = new KafkaReactionProvider(sender);

        ConfigSchema schema = provider.configSchema();

        assertThat(schema.kind()).isEqualTo("kafka");
        assertThat(schema.description()).contains("Kafka");
        assertThat(schema.properties()).hasSize(2);
        assertThat(schema.properties()).anyMatch(p -> p.name().equals("topic") && p.required());
        assertThat(schema.properties()).anyMatch(p -> p.name().equals("keyField") && !p.required());
    }

    private ReactionConfig createConfig(String topic) {
        return new ReactionConfig(
            "kafka-1",
            "Kafka Reaction",
            "kafka",
            Set.of("query-1"),
            Map.of(),
            Map.of("topic", topic, "keyField", "id"),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );
    }
}
