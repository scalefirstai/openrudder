package io.openrudder.reactions.kafka;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.ReactionConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaReactionTest {

    @Test
    void shouldSendAddedMessageToKafka() {
        KafkaSender<String, String> sender = mockKafkaSender();
        ReactionConfig config = createConfig("test-topic");
        KafkaReaction reaction = new KafkaReaction(config, sender);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldSendUpdatedMessageToKafka() {
        KafkaSender<String, String> sender = mockKafkaSender();
        ReactionConfig config = createConfig("test-topic");
        KafkaReaction reaction = new KafkaReaction(config, sender);

        QueryResult before = createQueryResult("result-1", Map.of("id", 1, "name", "Old"));
        QueryResult after = createQueryResult("result-1", Map.of("id", 1, "name", "New"));

        StepVerifier.create(reaction.doProcessUpdated(before, after, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldSendTombstoneForDeletedMessage() {
        KafkaSender<String, String> sender = mockKafkaSender();
        ReactionConfig config = createConfig("test-topic");
        KafkaReaction reaction = new KafkaReaction(config, sender);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessDeleted(result, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldUseCustomKeyField() {
        KafkaSender<String, String> sender = mockKafkaSender();
        ReactionConfig config = createConfigWithKeyField("test-topic", "customId");
        KafkaReaction reaction = new KafkaReaction(config, sender);

        QueryResult result = createQueryResult("result-1", Map.of("customId", "key-123", "name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldUseResultIdWhenKeyFieldMissing() {
        KafkaSender<String, String> sender = mockKafkaSender();
        ReactionConfig config = createConfig("test-topic");
        KafkaReaction reaction = new KafkaReaction(config, sender);

        QueryResult result = createQueryResult("result-123", Map.of("name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldCreateReactionWithCorrectMetadata() {
        KafkaSender<String, String> sender = mockKafkaSender();
        ReactionConfig config = createConfig("test-topic");
        KafkaReaction reaction = new KafkaReaction(config, sender);

        assertThat(reaction.id()).isEqualTo("kafka-1");
        assertThat(reaction.name()).isEqualTo("Kafka Reaction");
        assertThat(reaction.kind()).isEqualTo("kafka");
    }

    @SuppressWarnings("unchecked")
    private KafkaSender<String, String> mockKafkaSender() {
        KafkaSender<String, String> sender = mock(KafkaSender.class);
        SenderResult<Void> senderResult = mock(SenderResult.class);
        when(sender.send(any(Mono.class))).thenReturn(reactor.core.publisher.Flux.just(senderResult));
        return sender;
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

    private ReactionConfig createConfigWithKeyField(String topic, String keyField) {
        return new ReactionConfig(
            "kafka-1",
            "Kafka Reaction",
            "kafka",
            Set.of("query-1"),
            Map.of(),
            Map.of("topic", topic, "keyField", keyField),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );
    }

    private QueryResult createQueryResult(String resultId, Map<String, Object> data) {
        return QueryResult.builder()
            .id(resultId)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
}
