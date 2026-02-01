package io.openrudder.core.query.graph;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.JoinDefinition;
import io.openrudder.core.query.pattern.Pattern;
import io.openrudder.core.query.pattern.PatternMatch;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Graph storage for nodes and relationships across sources.
 * Supports cross-source joins via synthetic relationships.
 */
public interface GraphStore {
    
    /**
     * Apply a change event to the graph.
     */
    void applyChange(ChangeEvent change);
    
    /**
     * Get a node by ID.
     */
    Optional<Node> getNode(Object nodeId);
    
    /**
     * Get nodes by label.
     */
    Set<Node> getNodesByLabel(String label);
    
    /**
     * Get nodes by label and property value.
     */
    Set<Node> getNodesByProperty(String label, String property, Object value);
    
    /**
     * Get all relationships of a specific type.
     */
    Set<Relationship> getRelationshipsByType(String type);
    
    /**
     * Get relationships connected to a node.
     */
    Set<Relationship> getNodeRelationships(Object nodeId);
    
    /**
     * Execute a graph pattern match.
     */
    List<PatternMatch> match(Pattern pattern);
    
    /**
     * Create synthetic relationships for cross-source joins.
     */
    void createJoinRelationships(JoinDefinition join);
    
    /**
     * Clear all data for a specific source.
     */
    void clearSource(String sourceId);
    
    /**
     * Get total node count.
     */
    long getNodeCount();
    
    /**
     * Get total relationship count.
     */
    long getRelationshipCount();
}
