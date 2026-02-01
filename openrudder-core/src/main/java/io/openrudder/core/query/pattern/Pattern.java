package io.openrudder.core.query.pattern;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.graph.GraphStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a graph pattern from a MATCH clause.
 */
public interface Pattern {
    
    /**
     * Check if an entity matches this pattern.
     */
    boolean matches(Map<String, Object> entity, GraphStore store);
    
    /**
     * Get all matches for this pattern in the graph.
     */
    List<PatternMatch> findMatches(GraphStore store);
    
    /**
     * Check if a change affects this pattern.
     */
    boolean isAffectedBy(ChangeEvent change);
    
    /**
     * Get the labels involved in this pattern.
     */
    Set<String> labels();
    
    /**
     * Get the relationship types in this pattern.
     */
    Set<String> relationshipTypes();
    
    /**
     * Get the variable names used in this pattern.
     */
    Set<String> variables();
}
