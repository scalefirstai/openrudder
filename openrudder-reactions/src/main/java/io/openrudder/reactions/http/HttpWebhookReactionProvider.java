package io.openrudder.reactions.http;

import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ReactionProvider;
import io.openrudder.core.reaction.spi.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpWebhookReactionProvider implements ReactionProvider {
    
    @Override
    public String kind() {
        return "http";
    }
    
    @Override
    public Reaction create(ReactionConfig config) {
        return new HttpWebhookReaction(config);
    }
    
    @Override
    public ValidationResult validate(ReactionConfig config) {
        List<String> errors = new ArrayList<>();
        
        if (!config.properties().containsKey("webhookUrl")) {
            errors.add("webhookUrl is required");
        }
        
        String method = (String) config.properties().get("method");
        if (method != null) {
            try {
                org.springframework.http.HttpMethod.valueOf(method.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid HTTP method: " + method);
            }
        }
        
        return errors.isEmpty() 
            ? ValidationResult.ofValid() 
            : ValidationResult.ofInvalid(errors.toArray(String[]::new));
    }
    
    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(
            "http",
            "HTTP webhook reaction - sends HTTP requests when query results change",
            List.of(
                new ConfigSchema.PropertySchema("webhookUrl", "string", true, 
                    "URL to send webhooks to", null),
                new ConfigSchema.PropertySchema("method", "string", false, 
                    "HTTP method (GET, POST, PUT, PATCH, DELETE)", "POST"),
                new ConfigSchema.PropertySchema("headers", "map", false, 
                    "HTTP headers to include in requests", Map.of())
            )
        );
    }
}
