package io.openrudder.core.query.cache;

import io.openrudder.core.query.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryResultSetCacheTest {

    private InMemoryResultSetCache cache;

    @BeforeEach
    void setUp() {
        cache = new InMemoryResultSetCache();
    }

    @Test
    void shouldStoreAndRetrieveResults() {
        String resultId = "result-1";
        QueryResult result = createTestResult(resultId, "query-1");

        cache.put(resultId, result);

        Optional<QueryResult> retrieved = cache.get(resultId);
        assertTrue(retrieved.isPresent());
        assertEquals(resultId, retrieved.get().getId());
    }

    @Test
    void shouldReturnEmptyForNonExistentResult() {
        Optional<QueryResult> result = cache.get("non-existent");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldClearQueryResults() {
        String queryId = "query-1";
        QueryResult result1 = createTestResult("result-1", queryId);
        QueryResult result2 = createTestResult("result-2", queryId);

        cache.put("result-1", result1);
        cache.put("result-2", result2);
        cache.clearQuery(queryId);

        assertFalse(cache.get("result-1").isPresent());
        assertFalse(cache.get("result-2").isPresent());
    }

    @Test
    void shouldClearAllResults() {
        cache.put("result-1", createTestResult("result-1", "query-1"));
        cache.put("result-2", createTestResult("result-2", "query-2"));

        cache.clear();

        assertFalse(cache.get("result-1").isPresent());
        assertFalse(cache.get("result-2").isPresent());
    }

    @Test
    void shouldRemoveSpecificResult() {
        String resultId = "result-1";
        QueryResult result = createTestResult(resultId, "query-1");

        cache.put(resultId, result);
        cache.remove(resultId);

        assertFalse(cache.get(resultId).isPresent());
    }

    @Test
    void shouldGetCacheStats() {
        cache.put("result-1", createTestResult("result-1", "query-1"));
        cache.put("result-2", createTestResult("result-2", "query-1"));
        cache.put("result-3", createTestResult("result-3", "query-2"));

        CacheStats stats = cache.stats();

        assertNotNull(stats);
        assertEquals(2, stats.getTotalQueries());
        assertEquals(3, stats.getTotalResults());
    }

    @Test
    void shouldFindResultsByQuery() {
        String queryId = "query-1";
        cache.put("result-1", createTestResult("result-1", queryId));
        cache.put("result-2", createTestResult("result-2", queryId));
        cache.put("result-3", createTestResult("result-3", "query-2"));

        Set<String> resultIds = cache.findByQuery(queryId);

        assertEquals(2, resultIds.size());
        assertTrue(resultIds.contains("result-1"));
        assertTrue(resultIds.contains("result-2"));
    }

    @Test
    void shouldFindResultsByEntity() {
        cache.put("result-1", createTestResultWithEntity("result-1", "query-1", "order", 123));
        cache.put("result-2", createTestResultWithEntity("result-2", "query-1", "order", 123));
        cache.put("result-3", createTestResultWithEntity("result-3", "query-1", "order", 456));

        Set<String> resultIds = cache.findByEntity("order", 123);

        assertEquals(2, resultIds.size());
        assertTrue(resultIds.contains("result-1"));
        assertTrue(resultIds.contains("result-2"));
    }

    @Test
    void shouldFindResultsByFieldValue() {
        cache.put("result-1", createTestResultWithField("result-1", "query-1", "status", "ACTIVE"));
        cache.put("result-2", createTestResultWithField("result-2", "query-1", "status", "ACTIVE"));
        cache.put("result-3", createTestResultWithField("result-3", "query-1", "status", "INACTIVE"));

        Set<String> resultIds = cache.findByFieldValue("status", "ACTIVE");

        assertEquals(2, resultIds.size());
        assertTrue(resultIds.contains("result-1"));
        assertTrue(resultIds.contains("result-2"));
    }

    @Test
    void shouldGetAllResultsForQuery() {
        String queryId = "query-1";
        cache.put("result-1", createTestResult("result-1", queryId));
        cache.put("result-2", createTestResult("result-2", queryId));

        List<QueryResult> results = cache.getAllResultsForQuery(queryId);

        assertEquals(2, results.size());
    }

    @Test
    void shouldTrackHitsAndMisses() {
        cache.put("result-1", createTestResult("result-1", "query-1"));

        cache.get("result-1");
        cache.get("non-existent");

        CacheStats stats = cache.stats();
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertTrue(stats.getHitRate() > 0);
    }

    @Test
    void shouldIndexByIdField() {
        QueryResult result = createTestResultWithField("result-1", "query-1", "id", 123);
        cache.put("result-1", result);

        Set<String> resultIds = cache.findByEntity("id", 123);
        assertEquals(1, resultIds.size());
        assertTrue(resultIds.contains("result-1"));
    }

    @Test
    void shouldReturnEmptySetForNonExistentQuery() {
        Set<String> resultIds = cache.findByQuery("non-existent");
        assertNotNull(resultIds);
        assertTrue(resultIds.isEmpty());
    }

    private QueryResult createTestResult(String id, String queryId) {
        return QueryResult.builder()
            .id(id)
            .queryId(queryId)
            .data(Map.of("id", id, "value", "test"))
            .timestamp(Instant.now())
            .build();
    }

    private QueryResult createTestResultWithEntity(String id, String queryId, String entityType, Object entityId) {
        return QueryResult.builder()
            .id(id)
            .queryId(queryId)
            .data(Map.of("id", id, entityType + "_id", entityId))
            .timestamp(Instant.now())
            .build();
    }

    private QueryResult createTestResultWithField(String id, String queryId, String fieldName, Object fieldValue) {
        Map<String, Object> data = fieldName.equals("id") 
            ? Map.of("id", fieldValue)
            : Map.of("id", id, fieldName, fieldValue);
        
        return QueryResult.builder()
            .id(id)
            .queryId(queryId)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
}
