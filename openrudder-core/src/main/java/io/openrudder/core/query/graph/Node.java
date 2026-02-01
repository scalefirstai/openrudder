package io.openrudder.core.query.graph;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * Node in the graph.
 */
@Value
@Builder
public class Node {
    Object id;
    Set<String> labels;
    Map<String, Object> properties;
    String sourceId;
    
    /**
     * Get a property value.
     */
    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
    
    /**
     * Check if node has a specific label.
     */
    public boolean hasLabel(String label) {
        return labels != null && labels.contains(label);
    }
}
