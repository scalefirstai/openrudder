package io.openrudder.core.query.cache;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.query.ViewConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory implementation of result set cache with multi-dimensional indexing.
 * Thread-safe using concurrent data structures.
 */
@Slf4j
public class InMemoryResultSetCache implements ResultSetCache {
    
    // Primary storage: resultId -> QueryResult
    private final Map<String, QueryResult> results = new ConcurrentHashMap<>();
    
    // Index: queryId -> Set<resultId>
    private final Map<String, Set<String>> resultsByQuery = new ConcurrentHashMap<>();
    
    // Index: entityType:entityId -> Set<resultId>
    private final Map<String, Set<String>> resultsByEntity = new ConcurrentHashMap<>();
    
    // Index: fieldName:value -> Set<resultId>
    private final Map<String, Set<String>> resultsByFieldValue = new ConcurrentHashMap<>();
    
    // Historical results: resultId -> List<QueryResult> (sorted by timestamp)
    private final Map<String, List<QueryResult>> resultHistory = new ConcurrentHashMap<>();
    
    // Retention policy per query
    private final Map<String, ViewConfig.RetentionPolicy> retentionPolicies = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    
    @Override
    public void put(String resultId, QueryResult result) {
        QueryResult oldResult = results.put(resultId, result);
        
        // Add to query index
        resultsByQuery.computeIfAbsent(result.getQueryId(), k -> ConcurrentHashMap.newKeySet())
            .add(resultId);
        
        // Index by entities referenced in result data
        indexByEntities(result);
        
        // Index by field values
        indexByFields(result);
        
        // Handle retention policy
        ViewConfig.RetentionPolicy policy = retentionPolicies.get(result.getQueryId());
        if (policy != null) {
            handleRetentionPolicy(resultId, result, oldResult, policy);
        }
        
        log.debug("Cached result: {} for query: {}", resultId, result.getQueryId());
    }
    
    private void indexByEntities(QueryResult result) {
        if (result.getData() == null) return;
        
        // Index by id field if present
        Object id = result.getData().get("id");
        if (id != null) {
            String entityKey = "id:" + id;
            resultsByEntity.computeIfAbsent(entityKey, k -> ConcurrentHashMap.newKeySet())
                .add(result.getId());
        }
        
        // Index by other entity references (fields ending with _id or Id)
        for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
            String fieldName = entry.getKey();
            if ((fieldName.endsWith("_id") || fieldName.endsWith("Id")) && entry.getValue() != null) {
                String entityType = fieldName.replace("_id", "").replace("Id", "");
                String entityKey = entityType + ":" + entry.getValue();
                resultsByEntity.computeIfAbsent(entityKey, k -> ConcurrentHashMap.newKeySet())
                    .add(result.getId());
            }
        }
    }
    
    private void indexByFields(QueryResult result) {
        if (result.getData() == null) return;
        
        for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
            if (entry.getValue() != null) {
                String fieldKey = entry.getKey() + ":" + entry.getValue();
                resultsByFieldValue.computeIfAbsent(fieldKey, k -> ConcurrentHashMap.newKeySet())
                    .add(result.getId());
            }
        }
    }
    
    private void handleRetentionPolicy(
            String resultId, 
            QueryResult newResult, 
            QueryResult oldResult,
            ViewConfig.RetentionPolicy policy) {
        
        if (policy instanceof ViewConfig.Latest) {
            // Keep only latest - no history
            if (oldResult != null) {
                resultHistory.remove(resultId);
            }
            
        } else if (policy instanceof ViewConfig.All) {
            // Keep all versions
            resultHistory.computeIfAbsent(resultId, k -> new ArrayList<>())
                .add(newResult);
            
        } else if (policy instanceof ViewConfig.Expire expire) {
            // Keep with TTL
            List<QueryResult> history = resultHistory.computeIfAbsent(resultId, k -> new ArrayList<>());
            history.add(newResult);
            
            // Remove expired results
            Instant cutoff = Instant.now().minus(expire.ttl());
            history.removeIf(r -> r.getTimestamp().isBefore(cutoff));
            
            if (history.isEmpty()) {
                resultHistory.remove(resultId);
            }
        }
    }
    
    @Override
    public Optional<QueryResult> get(String resultId) {
        QueryResult result = results.get(resultId);
        if (result != null) {
            hitCount.incrementAndGet();
            return Optional.of(result);
        } else {
            missCount.incrementAndGet();
            return Optional.empty();
        }
    }
    
    @Override
    public void remove(String resultId) {
        QueryResult result = results.remove(resultId);
        if (result == null) return;
        
        // Remove from query index
        Set<String> queryResults = resultsByQuery.get(result.getQueryId());
        if (queryResults != null) {
            queryResults.remove(resultId);
        }
        
        // Remove from entity indexes
        removeFromEntityIndexes(result);
        
        // Remove from field indexes
        removeFromFieldIndexes(result);
        
        // Remove history
        resultHistory.remove(resultId);
        
        evictionCount.incrementAndGet();
        log.debug("Removed result: {}", resultId);
    }
    
    private void removeFromEntityIndexes(QueryResult result) {
        if (result.getData() == null) return;
        
        Object id = result.getData().get("id");
        if (id != null) {
            String entityKey = "id:" + id;
            Set<String> entityResults = resultsByEntity.get(entityKey);
            if (entityResults != null) {
                entityResults.remove(result.getId());
            }
        }
        
        for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
            String fieldName = entry.getKey();
            if ((fieldName.endsWith("_id") || fieldName.endsWith("Id")) && entry.getValue() != null) {
                String entityType = fieldName.replace("_id", "").replace("Id", "");
                String entityKey = entityType + ":" + entry.getValue();
                Set<String> entityResults = resultsByEntity.get(entityKey);
                if (entityResults != null) {
                    entityResults.remove(result.getId());
                }
            }
        }
    }
    
    private void removeFromFieldIndexes(QueryResult result) {
        if (result.getData() == null) return;
        
        for (Map.Entry<String, Object> entry : result.getData().entrySet()) {
            if (entry.getValue() != null) {
                String fieldKey = entry.getKey() + ":" + entry.getValue();
                Set<String> fieldResults = resultsByFieldValue.get(fieldKey);
                if (fieldResults != null) {
                    fieldResults.remove(result.getId());
                }
            }
        }
    }
    
    @Override
    public Set<String> findByQuery(String queryId) {
        Set<String> resultIds = resultsByQuery.get(queryId);
        return resultIds != null ? new HashSet<>(resultIds) : Set.of();
    }
    
    @Override
    public Set<String> findByEntity(String entityType, Object entityId) {
        String entityKey = entityType + ":" + entityId;
        Set<String> resultIds = resultsByEntity.get(entityKey);
        return resultIds != null ? new HashSet<>(resultIds) : Set.of();
    }
    
    @Override
    public Set<String> findByFieldValue(String fieldName, Object value) {
        String fieldKey = fieldName + ":" + value;
        Set<String> resultIds = resultsByFieldValue.get(fieldKey);
        return resultIds != null ? new HashSet<>(resultIds) : Set.of();
    }
    
    @Override
    public Set<QueryResult> getResultsAt(String queryId, Instant timestamp) {
        Set<String> resultIds = findByQuery(queryId);
        
        return resultIds.stream()
            .map(resultId -> {
                List<QueryResult> history = resultHistory.get(resultId);
                if (history == null || history.isEmpty()) {
                    // No history, return current if timestamp is after it
                    QueryResult current = results.get(resultId);
                    if (current != null && !current.getTimestamp().isAfter(timestamp)) {
                        return current;
                    }
                    return null;
                }
                
                // Find the latest result at or before the timestamp
                return history.stream()
                    .filter(r -> !r.getTimestamp().isAfter(timestamp))
                    .max(Comparator.comparing(QueryResult::getTimestamp))
                    .orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    @Override
    public void clearQuery(String queryId) {
        Set<String> resultIds = resultsByQuery.remove(queryId);
        if (resultIds != null) {
            resultIds.forEach(this::remove);
        }
        retentionPolicies.remove(queryId);
        log.info("Cleared {} results for query: {}", 
            resultIds != null ? resultIds.size() : 0, queryId);
    }
    
    @Override
    public CacheStats stats() {
        long total = hitCount.get() + missCount.get();
        double hitRate = total > 0 ? (double) hitCount.get() / total : 0.0;
        
        return CacheStats.builder()
            .totalResults(results.size())
            .totalQueries(resultsByQuery.size())
            .hitCount(hitCount.get())
            .missCount(missCount.get())
            .evictionCount(evictionCount.get())
            .hitRate(hitRate)
            .memoryUsageBytes(estimateMemoryUsage())
            .build();
    }
    
    private long estimateMemoryUsage() {
        // Rough estimation: 1KB per result + indexes
        return results.size() * 1024L + 
               resultsByQuery.size() * 100L +
               resultsByEntity.size() * 100L +
               resultsByFieldValue.size() * 100L;
    }
    
    @Override
    public void clear() {
        results.clear();
        resultsByQuery.clear();
        resultsByEntity.clear();
        resultsByFieldValue.clear();
        resultHistory.clear();
        retentionPolicies.clear();
        hitCount.set(0);
        missCount.set(0);
        evictionCount.set(0);
        log.info("Result cache cleared");
    }
    
    /**
     * Set retention policy for a query.
     */
    public void setRetentionPolicy(String queryId, ViewConfig.RetentionPolicy policy) {
        retentionPolicies.put(queryId, policy);
    }
    
    /**
     * Get all results for a query (for testing).
     */
    public List<QueryResult> getAllResultsForQuery(String queryId) {
        Set<String> resultIds = findByQuery(queryId);
        return resultIds.stream()
            .map(results::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
