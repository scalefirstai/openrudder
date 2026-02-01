package io.openrudder.core.query.parser;

import io.openrudder.core.query.pattern.PatternMatch;

/**
 * Represents a RETURN clause projection.
 */
public interface Projection {
    
    /**
     * Get the alias for this projection.
     */
    String alias();
    
    /**
     * Evaluate this projection against a pattern match.
     */
    Object evaluate(PatternMatch match);
    
    /**
     * Get the projection expression as string.
     */
    String expression();
}
