package io.openrudder.core.query.pattern;

import io.openrudder.core.query.graph.Node;
import io.openrudder.core.query.graph.Relationship;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * A concrete match of a pattern in the graph.
 */
@Value
@Builder
public class PatternMatch {
    Pattern pattern;
    Map<String, Node> nodes;
    Map<String, Relationship> relationships;
    
    /**
     * Get a node by its variable name in the pattern.
     */
    public Node getNode(String variable) {
        return nodes.get(variable);
    }
    
    /**
     * Get a relationship by its variable name.
     */
    public Relationship getRelationship(String variable) {
        return relationships.get(variable);
    }
}
