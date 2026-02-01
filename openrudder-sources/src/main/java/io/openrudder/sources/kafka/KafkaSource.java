package io.openrudder.sources.kafka;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.source.AbstractSource;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class KafkaSource extends AbstractSource<KafkaSourceConfig> {

    private KafkaReceiver<String, String> kafkaReceiver;

    public KafkaSource(KafkaSourceConfig config) {
        super(config);
    }

    public static KafkaSourceBuilder builder() {
        return new KafkaSourceBuilder();
    }

    @Override
    protected Flux<ChangeEvent> doStart() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, config.getKeyDeserializer());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, config.getValueDeserializer());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        if (config.getAdditionalProperties() != null) {
            consumerProps.putAll(config.getAdditionalProperties());
        }

        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(consumerProps)
            .subscription(config.getTopics());

        kafkaReceiver = KafkaReceiver.create(receiverOptions);

        return kafkaReceiver.receive()
            .map(record -> {
                ChangeEvent event = ChangeEvent.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .type(ChangeEvent.ChangeType.INSERT)
                    .entityType(record.topic())
                    .entityId(record.key())
                    .after(Map.of("value", record.value(), "partition", record.partition(), "offset", record.offset()))
                    .timestamp(Instant.ofEpochMilli(record.timestamp()))
                    .sourceId(getId())
                    .build();
                
                record.receiverOffset().acknowledge();
                emitEvent(event);
                return event;
            });
    }

    @Override
    protected Mono<Void> doStop() {
        return Mono.empty();
    }

    @Override
    protected Flux<ChangeEvent> doSnapshot() {
        return Flux.empty();
    }

    public static class KafkaSourceBuilder {
        private String name;
        private String bootstrapServers;
        private java.util.List<String> topics;
        private String groupId;

        public KafkaSourceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public KafkaSourceBuilder bootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
            return this;
        }

        public KafkaSourceBuilder topics(java.util.List<String> topics) {
            this.topics = topics;
            return this;
        }

        public KafkaSourceBuilder topic(String topic) {
            if (this.topics == null) {
                this.topics = new java.util.ArrayList<>();
            }
            this.topics.add(topic);
            return this;
        }

        public KafkaSourceBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public KafkaSource build() {
            KafkaSourceConfig config = KafkaSourceConfig.builder()
                .name(name)
                .bootstrapServers(bootstrapServers)
                .topics(topics)
                .groupId(groupId)
                .build();
            
            return new KafkaSource(config);
        }
    }
}
