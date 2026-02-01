package io.openrudder.core.query.graph;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Relationship in the graph.
 */
@Value
@Builder
public class Relationship {
    Object id;
    String type;
    Object startNodeId;
    Object endNodeId;
    Map<String, Object> properties;
    String sourceId;
    @Builder.Default
    boolean synthetic = false;
    
    /**
     * Get a property value.
     */
    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
}
