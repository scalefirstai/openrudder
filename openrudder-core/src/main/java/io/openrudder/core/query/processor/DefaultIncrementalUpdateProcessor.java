package io.openrudder.core.query.processor;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.*;
import io.openrudder.core.query.cache.ResultSetCache;
import io.openrudder.core.query.graph.GraphStore;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;

/**
 * Default implementation of incremental update processing.
 * Uses graph store and result cache for efficient delta computation.
 */
@Slf4j
public class DefaultIncrementalUpdateProcessor implements IncrementalUpdateProcessor {
    
    private final GraphStore graphStore;
    private final ResultSetCache resultCache;
    
    public DefaultIncrementalUpdateProcessor(GraphStore graphStore, ResultSetCache resultCache) {
        this.graphStore = graphStore;
        this.resultCache = resultCache;
    }
    
    @Override
    public Flux<ResultChange> processChange(ContinuousQuery query, ChangeEvent change) {
        return Flux.defer(() -> {
            try {
                // 1. Filter by source
                if (query.getSourceIds() != null && 
                    !query.getSourceIds().isEmpty() && 
                    !query.getSourceIds().contains(change.getSourceId())) {
                    return Flux.empty();
                }
                
                // 2. Update graph store
                graphStore.applyChange(change);
                
                // 3. Compute result changes based on change type
                List<ResultChange> changes = switch (change.getType()) {
                    case INSERT, SNAPSHOT -> handleInsert(query, change);
                    case UPDATE -> handleUpdate(query, change);
                    case DELETE -> handleDelete(query, change);
                };
                
                return Flux.fromIterable(changes);
                
            } catch (Exception e) {
                log.error("Error processing change for query {}: {}", 
                    query.getId(), e.getMessage(), e);
                return Flux.error(e);
            }
        });
    }
    
    private List<ResultChange> handleInsert(ContinuousQuery query, ChangeEvent change) {
        List<ResultChange> changes = new ArrayList<>();
        
        // Check if new entity matches query conditions
        if (!SimpleQueryEvaluator.evaluateConditions(change, query.getQuery())) {
            return changes;
        }
        
        // Create new result
        QueryResult newResult = SimpleQueryEvaluator.createQueryResult(
            change, query.getId(), query.getQuery());
        
        // Cache the result
        resultCache.put(newResult.getId(), newResult);
        
        // Emit ADDED change
        changes.add(ResultChange.builder()
            .queryId(query.getId())
            .type(ResultChange.ChangeType.ADDED)
            .before(null)
            .after(newResult)
            .timestamp(Instant.now())
            .sourceChange(ResultChange.SourceChangeInfo.builder()
                .sourceId(change.getSourceId())
                .changeType(change.getType())
                .entityType(change.getEntityType())
                .entityId(change.getEntityId())
                .build())
            .build());
        
        log.debug("Query {} - ADDED result for entity: {}", 
            query.getId(), change.getEntityId());
        
        return changes;
    }
    
    private List<ResultChange> handleUpdate(ContinuousQuery query, ChangeEvent change) {
        List<ResultChange> changes = new ArrayList<>();
        
        boolean beforeMatched = change.getBefore() != null && 
            evaluateConditionsOnData(query, change.getBefore());
        boolean afterMatched = change.getAfter() != null && 
            evaluateConditionsOnData(query, change.getAfter());
        
        // Find existing results that reference this entity
        Set<String> affectedResultIds = resultCache.findByEntity(
            change.getEntityType(), 
            change.getEntityId()
        );
        
        if (!beforeMatched && afterMatched) {
            // Entity now matches - treat as insert
            if (affectedResultIds.isEmpty()) {
                return handleInsert(query, change);
            }
        } else if (beforeMatched && !afterMatched) {
            // Entity no longer matches - treat as delete
            for (String resultId : affectedResultIds) {
                Optional<QueryResult> oldResult = resultCache.get(resultId);
                if (oldResult.isPresent()) {
                    resultCache.remove(resultId);
                    
                    changes.add(ResultChange.builder()
                        .queryId(query.getId())
                        .type(ResultChange.ChangeType.DELETED)
                        .before(oldResult.get())
                        .after(null)
                        .timestamp(Instant.now())
                        .sourceChange(createSourceChangeInfo(change))
                        .build());
                    
                    log.debug("Query {} - DELETED result: {}", query.getId(), resultId);
                }
            }
        } else if (beforeMatched && afterMatched) {
            // Entity still matches but properties changed
            for (String resultId : affectedResultIds) {
                Optional<QueryResult> oldResult = resultCache.get(resultId);
                if (oldResult.isEmpty()) continue;
                
                // Re-evaluate result with new data
                QueryResult newResult = SimpleQueryEvaluator.createQueryResult(
                    change, query.getId(), query.getQuery());
                
                // Check if result actually changed
                if (!oldResult.get().getData().equals(newResult.getData())) {
                    resultCache.put(resultId, newResult);
                    
                    changes.add(ResultChange.builder()
                        .queryId(query.getId())
                        .type(ResultChange.ChangeType.UPDATED)
                        .before(oldResult.get())
                        .after(newResult)
                        .timestamp(Instant.now())
                        .sourceChange(createSourceChangeInfo(change))
                        .build());
                    
                    log.debug("Query {} - UPDATED result: {}", query.getId(), resultId);
                }
            }
        }
        
        return changes;
    }
    
    private List<ResultChange> handleDelete(ContinuousQuery query, ChangeEvent change) {
        List<ResultChange> changes = new ArrayList<>();
        
        // Find all results that reference the deleted entity
        Set<String> affectedResultIds = resultCache.findByEntity(
            change.getEntityType(),
            change.getEntityId()
        );
        
        for (String resultId : affectedResultIds) {
            Optional<QueryResult> oldResult = resultCache.get(resultId);
            if (oldResult.isEmpty()) continue;
            
            // Remove result from cache
            resultCache.remove(resultId);
            
            // Emit DELETED change
            changes.add(ResultChange.builder()
                .queryId(query.getId())
                .type(ResultChange.ChangeType.DELETED)
                .before(oldResult.get())
                .after(null)
                .timestamp(Instant.now())
                .sourceChange(createSourceChangeInfo(change))
                .build());
            
            log.debug("Query {} - DELETED result: {} (entity deleted)", 
                query.getId(), resultId);
        }
        
        return changes;
    }
    
    private boolean evaluateConditionsOnData(ContinuousQuery query, Map<String, Object> data) {
        // Create a temporary change event for evaluation
        ChangeEvent tempEvent = ChangeEvent.builder()
            .type(ChangeEvent.ChangeType.UPDATE)
            .entityType("temp")
            .entityId("temp")
            .after(data)
            .build();
        
        return SimpleQueryEvaluator.evaluateConditions(tempEvent, query.getQuery());
    }
    
    private ResultChange.SourceChangeInfo createSourceChangeInfo(ChangeEvent change) {
        return ResultChange.SourceChangeInfo.builder()
            .sourceId(change.getSourceId())
            .changeType(change.getType())
            .entityType(change.getEntityType())
            .entityId(change.getEntityId())
            .build();
    }
    
    @Override
    public Flux<ResultChange> processSnapshot(ContinuousQuery query, Flux<ChangeEvent> snapshot) {
        return snapshot.flatMap(change -> processChange(query, change));
    }
    
    /**
     * Find candidate results that might be affected by a change.
     * Uses cache indexes for fast lookup.
     */
    private Set<String> findCandidateResults(ContinuousQuery query, ChangeEvent change) {
        Set<String> candidates = new HashSet<>();
        
        // Find by entity reference
        candidates.addAll(resultCache.findByEntity(
            change.getEntityType(), 
            change.getEntityId()
        ));
        
        // Find by field values that changed
        if (change.getBefore() != null && change.getAfter() != null) {
            for (Map.Entry<String, Object> entry : change.getAfter().entrySet()) {
                Object oldValue = change.getBefore().get(entry.getKey());
                Object newValue = entry.getValue();
                
                if (oldValue != null && !oldValue.equals(newValue)) {
                    // Field changed - find results with old value
                    candidates.addAll(resultCache.findByFieldValue(
                        entry.getKey(), 
                        oldValue
                    ));
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Get statistics about processing.
     */
    public ProcessorStats getStats() {
        return ProcessorStats.builder()
            .graphNodeCount(graphStore.getNodeCount())
            .graphRelationshipCount(graphStore.getRelationshipCount())
            .cacheStats(resultCache.stats())
            .build();
    }
    
    /**
     * Statistics for the processor.
     */
    @lombok.Value
    @lombok.Builder
    public static class ProcessorStats {
        long graphNodeCount;
        long graphRelationshipCount;
        io.openrudder.core.query.cache.CacheStats cacheStats;
    }
}
