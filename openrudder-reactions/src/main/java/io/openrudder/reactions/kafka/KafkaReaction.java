package io.openrudder.reactions.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.AbstractReaction;
import io.openrudder.core.reaction.ReactionConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.util.Map;

@Slf4j
public class KafkaReaction extends AbstractReaction {
    
    private final KafkaSender<String, String> sender;
    private final String topic;
    private final String keyField;
    private final ObjectMapper objectMapper;
    
    public KafkaReaction(ReactionConfig config, KafkaSender<String, String> sender) {
        super(config);
        this.sender = sender;
        this.topic = (String) config.properties().get("topic");
        this.keyField = (String) config.properties().getOrDefault("keyField", "id");
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            ReactionConfig.QueryConfig queryConfig) {
        
        String key = extractKey(result);
        String value = serializeResult(result, queryId, "added");
        
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topic, key, value);
        
        return sender
            .send(Mono.just(SenderRecord.create(record, null)))
            .then()
            .doOnSuccess(v -> log.debug("Sent ADDED message to Kafka topic {}: {}", topic, key))
            .doOnError(e -> log.error("Failed to send message to Kafka: {}", e.getMessage()));
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        
        String key = extractKey(after);
        String value = serializeUpdate(before, after, queryId);
        
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topic, key, value);
        
        return sender
            .send(Mono.just(SenderRecord.create(record, null)))
            .then()
            .doOnSuccess(v -> log.debug("Sent UPDATED message to Kafka topic {}: {}", topic, key))
            .doOnError(e -> log.error("Failed to send message to Kafka: {}", e.getMessage()));
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        
        String key = extractKey(result);
        
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topic, key, null);
        
        return sender
            .send(Mono.just(SenderRecord.create(record, null)))
            .then()
            .doOnSuccess(v -> log.debug("Sent DELETED (tombstone) message to Kafka topic {}: {}", topic, key))
            .doOnError(e -> log.error("Failed to send message to Kafka: {}", e.getMessage()));
    }
    
    private String extractKey(QueryResult result) {
        Object keyValue = result.data().get(keyField);
        return keyValue != null ? keyValue.toString() : result.resultId();
    }
    
    private String serializeResult(QueryResult result, String queryId, String changeType) {
        Map<String, Object> payload = Map.of(
            "queryId", queryId,
            "changeType", changeType,
            "result", result.data()
        );
        return toJson(payload);
    }
    
    private String serializeUpdate(QueryResult before, QueryResult after, String queryId) {
        Map<String, Object> payload = Map.of(
            "queryId", queryId,
            "changeType", "updated",
            "before", before.data(),
            "after", after.data()
        );
        return toJson(payload);
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}
