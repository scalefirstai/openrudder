package io.openrudder.reactions.debug;

import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ReactionProvider;
import io.openrudder.core.reaction.spi.ValidationResult;

import java.util.List;

public class DebugReactionProvider implements ReactionProvider {
    
    @Override
    public String kind() {
        return "debug";
    }
    
    @Override
    public Reaction create(ReactionConfig config) {
        return new DebugReaction(config);
    }
    
    @Override
    public ValidationResult validate(ReactionConfig config) {
        return ValidationResult.ofValid();
    }
    
    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(
            "debug",
            "Debug reaction - logs all changes to console for development and debugging",
            List.of(
                new ConfigSchema.PropertySchema("verbose", "boolean", false, 
                    "Show full result data instead of just field names", false)
            )
        );
    }
}
