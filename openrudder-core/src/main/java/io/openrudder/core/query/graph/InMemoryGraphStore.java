package io.openrudder.core.query.graph;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.JoinDefinition;
import io.openrudder.core.query.pattern.Pattern;
import io.openrudder.core.query.pattern.PatternMatch;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory graph store with indexing for fast pattern matching.
 * Thread-safe implementation using concurrent data structures.
 */
@Slf4j
public class InMemoryGraphStore implements GraphStore {
    
    // Primary storage
    private final Map<Object, Node> nodes = new ConcurrentHashMap<>();
    private final Map<Object, Relationship> relationships = new ConcurrentHashMap<>();
    
    // Indexes for fast lookup
    private final Map<String, Set<Object>> nodesByLabel = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<Object, Set<Object>>>> nodesByLabelAndProperty = new ConcurrentHashMap<>();
    private final Map<String, Set<Object>> relationshipsByType = new ConcurrentHashMap<>();
    private final Map<Object, Set<Object>> relationshipsByNode = new ConcurrentHashMap<>();
    private final Map<String, Set<Object>> nodesBySource = new ConcurrentHashMap<>();
    
    @Override
    public void applyChange(ChangeEvent change) {
        switch (change.getType()) {
            case INSERT, SNAPSHOT -> handleInsert(change);
            case UPDATE -> handleUpdate(change);
            case DELETE -> handleDelete(change);
        }
    }
    
    private void handleInsert(ChangeEvent change) {
        Object nodeId = change.getEntityId();
        Map<String, Object> data = change.getAfter();
        
        if (data == null) {
            log.warn("Insert event with null data: {}", change.getId());
            return;
        }
        
        // Create node from change event
        Node node = Node.builder()
            .id(nodeId)
            .labels(Set.of(change.getEntityType()))
            .properties(new HashMap<>(data))
            .sourceId(change.getSourceId())
            .build();
        
        addNode(node);
        log.debug("Inserted node: {} with labels: {}", nodeId, node.getLabels());
    }
    
    private void handleUpdate(ChangeEvent change) {
        Object nodeId = change.getEntityId();
        Map<String, Object> newData = change.getAfter();
        
        if (newData == null) {
            log.warn("Update event with null data: {}", change.getId());
            return;
        }
        
        // Remove old node and indexes
        Node oldNode = nodes.get(nodeId);
        if (oldNode != null) {
            removeNodeFromIndexes(oldNode);
        }
        
        // Create updated node
        Node node = Node.builder()
            .id(nodeId)
            .labels(Set.of(change.getEntityType()))
            .properties(new HashMap<>(newData))
            .sourceId(change.getSourceId())
            .build();
        
        addNode(node);
        log.debug("Updated node: {}", nodeId);
    }
    
    private void handleDelete(ChangeEvent change) {
        Object nodeId = change.getEntityId();
        Node node = nodes.remove(nodeId);
        
        if (node != null) {
            removeNodeFromIndexes(node);
            
            // Remove relationships connected to this node
            Set<Object> relIds = relationshipsByNode.remove(nodeId);
            if (relIds != null) {
                relIds.forEach(relId -> {
                    Relationship rel = relationships.remove(relId);
                    if (rel != null) {
                        removeRelationshipFromIndexes(rel);
                    }
                });
            }
            
            log.debug("Deleted node: {}", nodeId);
        }
    }
    
    private void addNode(Node node) {
        nodes.put(node.getId(), node);
        
        // Index by labels
        for (String label : node.getLabels()) {
            nodesByLabel.computeIfAbsent(label, k -> ConcurrentHashMap.newKeySet())
                .add(node.getId());
        }
        
        // Index by label and properties
        for (String label : node.getLabels()) {
            Map<String, Map<Object, Set<Object>>> labelIndex = 
                nodesByLabelAndProperty.computeIfAbsent(label, k -> new ConcurrentHashMap<>());
            
            if (node.getProperties() != null) {
                for (Map.Entry<String, Object> prop : node.getProperties().entrySet()) {
                    Map<Object, Set<Object>> propIndex = 
                        labelIndex.computeIfAbsent(prop.getKey(), k -> new ConcurrentHashMap<>());
                    propIndex.computeIfAbsent(prop.getValue(), k -> ConcurrentHashMap.newKeySet())
                        .add(node.getId());
                }
            }
        }
        
        // Index by source
        nodesBySource.computeIfAbsent(node.getSourceId(), k -> ConcurrentHashMap.newKeySet())
            .add(node.getId());
    }
    
    private void removeNodeFromIndexes(Node node) {
        // Remove from label index
        for (String label : node.getLabels()) {
            Set<Object> labelNodes = nodesByLabel.get(label);
            if (labelNodes != null) {
                labelNodes.remove(node.getId());
            }
        }
        
        // Remove from property indexes
        for (String label : node.getLabels()) {
            Map<String, Map<Object, Set<Object>>> labelIndex = nodesByLabelAndProperty.get(label);
            if (labelIndex != null && node.getProperties() != null) {
                for (Map.Entry<String, Object> prop : node.getProperties().entrySet()) {
                    Map<Object, Set<Object>> propIndex = labelIndex.get(prop.getKey());
                    if (propIndex != null) {
                        Set<Object> nodeIds = propIndex.get(prop.getValue());
                        if (nodeIds != null) {
                            nodeIds.remove(node.getId());
                        }
                    }
                }
            }
        }
        
        // Remove from source index
        Set<Object> sourceNodes = nodesBySource.get(node.getSourceId());
        if (sourceNodes != null) {
            sourceNodes.remove(node.getId());
        }
    }
    
    private void addRelationship(Relationship rel) {
        relationships.put(rel.getId(), rel);
        
        // Index by type
        relationshipsByType.computeIfAbsent(rel.getType(), k -> ConcurrentHashMap.newKeySet())
            .add(rel.getId());
        
        // Index by connected nodes
        relationshipsByNode.computeIfAbsent(rel.getStartNodeId(), k -> ConcurrentHashMap.newKeySet())
            .add(rel.getId());
        relationshipsByNode.computeIfAbsent(rel.getEndNodeId(), k -> ConcurrentHashMap.newKeySet())
            .add(rel.getId());
    }
    
    private void removeRelationshipFromIndexes(Relationship rel) {
        // Remove from type index
        Set<Object> typeRels = relationshipsByType.get(rel.getType());
        if (typeRels != null) {
            typeRels.remove(rel.getId());
        }
        
        // Remove from node indexes
        Set<Object> startRels = relationshipsByNode.get(rel.getStartNodeId());
        if (startRels != null) {
            startRels.remove(rel.getId());
        }
        Set<Object> endRels = relationshipsByNode.get(rel.getEndNodeId());
        if (endRels != null) {
            endRels.remove(rel.getId());
        }
    }
    
    @Override
    public Optional<Node> getNode(Object nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }
    
    @Override
    public Set<Node> getNodesByLabel(String label) {
        Set<Object> nodeIds = nodesByLabel.get(label);
        if (nodeIds == null) {
            return Set.of();
        }
        
        return nodeIds.stream()
            .map(nodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Node> getNodesByProperty(String label, String property, Object value) {
        Map<String, Map<Object, Set<Object>>> labelIndex = nodesByLabelAndProperty.get(label);
        if (labelIndex == null) {
            return Set.of();
        }
        
        Map<Object, Set<Object>> propIndex = labelIndex.get(property);
        if (propIndex == null) {
            return Set.of();
        }
        
        Set<Object> nodeIds = propIndex.get(value);
        if (nodeIds == null) {
            return Set.of();
        }
        
        return nodeIds.stream()
            .map(nodes::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Relationship> getRelationshipsByType(String type) {
        Set<Object> relIds = relationshipsByType.get(type);
        if (relIds == null) {
            return Set.of();
        }
        
        return relIds.stream()
            .map(relationships::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    @Override
    public Set<Relationship> getNodeRelationships(Object nodeId) {
        Set<Object> relIds = relationshipsByNode.get(nodeId);
        if (relIds == null) {
            return Set.of();
        }
        
        return relIds.stream()
            .map(relationships::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    @Override
    public List<PatternMatch> match(Pattern pattern) {
        // Basic pattern matching - will be enhanced with full Cypher support
        log.debug("Pattern matching not yet fully implemented");
        return List.of();
    }
    
    @Override
    public void createJoinRelationships(JoinDefinition join) {
        join.validate();
        
        if (join.getKeys().size() != 2) {
            log.warn("Only 2-way joins are currently supported");
            return;
        }
        
        JoinDefinition.JoinKey key1 = join.getKeys().get(0);
        JoinDefinition.JoinKey key2 = join.getKeys().get(1);
        
        // Get nodes for each label
        Set<Node> nodes1 = getNodesByLabel(key1.getLabel());
        Set<Node> nodes2 = getNodesByLabel(key2.getLabel());
        
        // Create synthetic relationships where properties match
        int syntheticRelCount = 0;
        for (Node n1 : nodes1) {
            Object value1 = n1.getProperty(key1.getProperty());
            if (value1 == null) continue;
            
            for (Node n2 : nodes2) {
                Object value2 = n2.getProperty(key2.getProperty());
                if (value2 == null) continue;
                
                if (value1.equals(value2)) {
                    // Create synthetic relationship
                    String relId = "synthetic_" + join.getJoinId() + "_" + n1.getId() + "_" + n2.getId();
                    Relationship rel = Relationship.builder()
                        .id(relId)
                        .type("JOINED_BY_" + join.getJoinId().toUpperCase())
                        .startNodeId(n1.getId())
                        .endNodeId(n2.getId())
                        .properties(Map.of(
                            "joinId", join.getJoinId(),
                            "matchedValue", value1
                        ))
                        .sourceId("synthetic")
                        .synthetic(true)
                        .build();
                    
                    addRelationship(rel);
                    syntheticRelCount++;
                }
            }
        }
        
        log.info("Created {} synthetic relationships for join: {}", syntheticRelCount, join.getJoinId());
    }
    
    @Override
    public void clearSource(String sourceId) {
        Set<Object> sourceNodeIds = nodesBySource.remove(sourceId);
        if (sourceNodeIds != null) {
            sourceNodeIds.forEach(nodeId -> {
                Node node = nodes.remove(nodeId);
                if (node != null) {
                    removeNodeFromIndexes(node);
                }
            });
        }
        
        log.info("Cleared {} nodes from source: {}", 
            sourceNodeIds != null ? sourceNodeIds.size() : 0, sourceId);
    }
    
    @Override
    public long getNodeCount() {
        return nodes.size();
    }
    
    @Override
    public long getRelationshipCount() {
        return relationships.size();
    }
    
    /**
     * Get all nodes (for testing/debugging).
     */
    public Collection<Node> getAllNodes() {
        return nodes.values();
    }
    
    /**
     * Get all relationships (for testing/debugging).
     */
    public Collection<Relationship> getAllRelationships() {
        return relationships.values();
    }
    
    /**
     * Clear all data.
     */
    public void clear() {
        nodes.clear();
        relationships.clear();
        nodesByLabel.clear();
        nodesByLabelAndProperty.clear();
        relationshipsByType.clear();
        relationshipsByNode.clear();
        nodesBySource.clear();
        log.info("Graph store cleared");
    }
}
