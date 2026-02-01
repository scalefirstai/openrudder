package io.openrudder.core.query.graph;

import java.util.Map;
import java.util.Set;

/**
 * Node in the graph.
 */
public class Node {
    private final Object id;
    private final Set<String> labels;
    private final Map<String, Object> properties;
    private final String sourceId;
    
    public Node(Object id, Set<String> labels, Map<String, Object> properties, String sourceId) {
        this.id = id;
        this.labels = labels;
        this.properties = properties;
        this.sourceId = sourceId;
    }
    
    public static NodeBuilder builder() {
        return new NodeBuilder();
    }
    
    public Object getId() {
        return id;
    }
    
    public Set<String> getLabels() {
        return labels;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
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
    
    public static class NodeBuilder {
        private Object id;
        private Set<String> labels;
        private Map<String, Object> properties;
        private String sourceId;
        
        public NodeBuilder id(Object id) {
            this.id = id;
            return this;
        }
        
        public NodeBuilder labels(Set<String> labels) {
            this.labels = labels;
            return this;
        }
        
        public NodeBuilder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }
        
        public NodeBuilder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        public Node build() {
            return new Node(id, labels, properties, sourceId);
        }
    }
}
