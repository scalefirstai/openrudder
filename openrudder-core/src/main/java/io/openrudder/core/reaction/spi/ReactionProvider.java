package io.openrudder.core.reaction.spi;

import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;

public interface ReactionProvider {
    
    String kind();
    
    Reaction create(ReactionConfig config);
    
    ValidationResult validate(ReactionConfig config);
    
    ConfigSchema configSchema();
}
