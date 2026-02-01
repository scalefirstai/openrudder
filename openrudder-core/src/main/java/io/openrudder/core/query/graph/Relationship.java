package io.openrudder.core.query.graph;

import java.util.Map;

/**
 * Relationship in the graph.
 */
public class Relationship {
    private final Object id;
    private final String type;
    private final Object startNodeId;
    private final Object endNodeId;
    private final Map<String, Object> properties;
    private final String sourceId;
    private final boolean synthetic;
    
    public Relationship(Object id, String type, Object startNodeId, Object endNodeId, 
                       Map<String, Object> properties, String sourceId, boolean synthetic) {
        this.id = id;
        this.type = type;
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
        this.properties = properties;
        this.sourceId = sourceId;
        this.synthetic = synthetic;
    }
    
    public static RelationshipBuilder builder() {
        return new RelationshipBuilder();
    }
    
    public Object getId() {
        return id;
    }
    
    public String getType() {
        return type;
    }
    
    public Object getStartNodeId() {
        return startNodeId;
    }
    
    public Object getEndNodeId() {
        return endNodeId;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public String getSourceId() {
        return sourceId;
    }
    
    public boolean isSynthetic() {
        return synthetic;
    }
    
    /**
     * Get a property value.
     */
    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
    
    public static class RelationshipBuilder {
        private Object id;
        private String type;
        private Object startNodeId;
        private Object endNodeId;
        private Map<String, Object> properties;
        private String sourceId;
        private boolean synthetic = false;
        
        public RelationshipBuilder id(Object id) {
            this.id = id;
            return this;
        }
        
        public RelationshipBuilder type(String type) {
            this.type = type;
            return this;
        }
        
        public RelationshipBuilder startNodeId(Object startNodeId) {
            this.startNodeId = startNodeId;
            return this;
        }
        
        public RelationshipBuilder endNodeId(Object endNodeId) {
            this.endNodeId = endNodeId;
            return this;
        }
        
        public RelationshipBuilder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }
        
        public RelationshipBuilder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }
        
        public RelationshipBuilder synthetic(boolean synthetic) {
            this.synthetic = synthetic;
            return this;
        }
        
        public Relationship build() {
            return new Relationship(id, type, startNodeId, endNodeId, properties, sourceId, synthetic);
        }
    }
}
