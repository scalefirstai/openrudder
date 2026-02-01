package io.openrudder.core.query.cache;

import io.openrudder.core.query.QueryResult;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Cache for query results with indexing capabilities.
 */
public interface ResultSetCache {
    
    /**
     * Store a result.
     */
    void put(String resultId, QueryResult result);
    
    /**
     * Retrieve a result.
     */
    Optional<QueryResult> get(String resultId);
    
    /**
     * Remove a result.
     */
    void remove(String resultId);
    
    /**
     * Find all result IDs for a specific query.
     */
    Set<String> findByQuery(String queryId);
    
    /**
     * Find results that reference a specific entity.
     * Used to quickly identify results affected by entity changes.
     */
    Set<String> findByEntity(String entityType, Object entityId);
    
    /**
     * Find results by field value.
     */
    Set<String> findByFieldValue(String fieldName, Object value);
    
    /**
     * Get results as of a specific point in time.
     * Requires appropriate retention policy.
     */
    Set<QueryResult> getResultsAt(String queryId, Instant timestamp);
    
    /**
     * Clear all results for a query.
     */
    void clearQuery(String queryId);
    
    /**
     * Get cache statistics.
     */
    CacheStats stats();
    
    /**
     * Clear all cached data.
     */
    void clear();
}
