package io.openrudder.reactions.kafka;

import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ReactionProvider;
import io.openrudder.core.reaction.spi.ValidationResult;
import reactor.kafka.sender.KafkaSender;

import java.util.ArrayList;
import java.util.List;

public class KafkaReactionProvider implements ReactionProvider {
    
    private final KafkaSender<String, String> sender;
    
    public KafkaReactionProvider(KafkaSender<String, String> sender) {
        this.sender = sender;
    }
    
    @Override
    public String kind() {
        return "kafka";
    }
    
    @Override
    public Reaction create(ReactionConfig config) {
        return new KafkaReaction(config, sender);
    }
    
    @Override
    public ValidationResult validate(ReactionConfig config) {
        List<String> errors = new ArrayList<>();
        
        if (!config.properties().containsKey("topic")) {
            errors.add("topic is required");
        }
        
        if (sender == null) {
            errors.add("KafkaSender is required");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.ofValid() 
            : ValidationResult.ofInvalid(errors.toArray(String[]::new));
    }
    
    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(
            "kafka",
            "Kafka reaction - publishes query results to Kafka topics",
            List.of(
                new ConfigSchema.PropertySchema("topic", "string", true, 
                    "Kafka topic to publish to", null),
                new ConfigSchema.PropertySchema("keyField", "string", false, 
                    "Field name to use as Kafka message key", "id")
            )
        );
    }
}
