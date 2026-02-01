package io.openrudder.core.query.parser;

import io.openrudder.core.query.graph.GraphStore;
import io.openrudder.core.query.pattern.PatternMatch;

/**
 * Represents a WHERE clause predicate.
 */
public interface Predicate {
    
    /**
     * Evaluate this predicate against a pattern match.
     */
    boolean evaluate(PatternMatch match, GraphStore store);
    
    /**
     * Get the predicate expression as string.
     */
    String expression();
}
