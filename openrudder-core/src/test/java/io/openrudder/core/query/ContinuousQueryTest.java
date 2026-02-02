package io.openrudder.core.query;

import io.openrudder.core.model.ChangeEvent;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContinuousQueryTest {

    @Test
    void shouldCreateContinuousQuery() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .sourceIds(Set.of("source-1"))
            .build();

        assertEquals("query-1", query.getId());
        assertEquals("Test Query", query.getName());
        assertNotNull(query.getSourceIds());
        assertTrue(query.getSourceIds().contains("source-1"));
    }

    @Test
    void shouldFilterEventsBySourceId() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .sourceIds(Set.of("source-1"))
            .build();

        ChangeEvent event1 = createTestEvent("evt-1", "source-1", "READY");
        ChangeEvent event2 = createTestEvent("evt-2", "source-2", "READY");

        Flux<ChangeEvent> changeStream = Flux.just(event1, event2);
        Flux<ResultUpdate> results = query.evaluate(changeStream);

        StepVerifier.create(results)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldEvaluateInsertEvents() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .sourceIds(Set.of("source-1"))
            .build();

        ChangeEvent event = createTestEvent("evt-1", "source-1", "READY");

        Flux<ChangeEvent> changeStream = Flux.just(event);
        Flux<ResultUpdate> results = query.evaluate(changeStream);

        StepVerifier.create(results)
            .assertNext(update -> {
                assertEquals(ResultUpdate.UpdateType.ADDED, update.getType());
                assertEquals("query-1", update.getQueryId());
                assertNotNull(update.getAfter());
                assertNull(update.getBefore());
            })
            .verifyComplete();
    }

    @Test
    void shouldEvaluateUpdateEvents() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .sourceIds(Set.of("source-1"))
            .build();

        ChangeEvent event = ChangeEvent.builder()
            .id("evt-1")
            .type(ChangeEvent.ChangeType.UPDATE)
            .entityType("Order")
            .entityId(1)
            .before(Map.of("id", 1, "status", "PENDING"))
            .after(Map.of("id", 1, "status", "READY"))
            .timestamp(Instant.now())
            .sourceId("source-1")
            .build();

        Flux<ChangeEvent> changeStream = Flux.just(event);
        Flux<ResultUpdate> results = query.evaluate(changeStream);

        StepVerifier.create(results)
            .assertNext(update -> {
                assertEquals(ResultUpdate.UpdateType.UPDATED, update.getType());
                assertEquals("query-1", update.getQueryId());
                assertNotNull(update.getAfter());
            })
            .verifyComplete();
    }

    @Test
    void shouldEvaluateDeleteEvents() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .sourceIds(Set.of("source-1"))
            .build();

        ChangeEvent event = ChangeEvent.builder()
            .id("evt-1")
            .type(ChangeEvent.ChangeType.DELETE)
            .entityType("Order")
            .entityId(1)
            .before(Map.of("id", 1, "status", "READY"))
            .timestamp(Instant.now())
            .sourceId("source-1")
            .build();

        Flux<ChangeEvent> changeStream = Flux.just(event);
        Flux<ResultUpdate> results = query.evaluate(changeStream);

        StepVerifier.create(results)
            .assertNext(update -> {
                assertEquals(ResultUpdate.UpdateType.REMOVED, update.getType());
                assertEquals("query-1", update.getQueryId());
                assertNull(update.getAfter());
                assertNotNull(update.getBefore());
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleEmptySourceIds() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .sourceIds(Set.of())
            .build();

        ChangeEvent event = createTestEvent("evt-1", "any-source", "READY");

        Flux<ChangeEvent> changeStream = Flux.just(event);
        Flux<ResultUpdate> results = query.evaluate(changeStream);

        StepVerifier.create(results)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldHandleNullSourceIds() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .sourceIds(null)
            .build();

        ChangeEvent event = createTestEvent("evt-1", "any-source", "READY");

        Flux<ChangeEvent> changeStream = Flux.just(event);
        Flux<ResultUpdate> results = query.evaluate(changeStream);

        StepVerifier.create(results)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyFluxForInitialEvaluation() {
        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .build();

        Flux<QueryResult> results = query.initialEvaluation();

        StepVerifier.create(results)
            .verifyComplete();
    }

    @Test
    void shouldCreateQueryWithConfig() {
        ContinuousQuery.QueryConfig config = ContinuousQuery.QueryConfig.builder()
            .enableIncremental(true)
            .maxResultSetSize(1000)
            .resultTtlSeconds(3600)
            .build();

        ContinuousQuery query = ContinuousQuery.builder()
            .id("query-1")
            .name("Test Query")
            .query("MATCH (o:Order) RETURN o")
            .config(config)
            .build();

        assertNotNull(query.getConfig());
        assertTrue(query.getConfig().isEnableIncremental());
        assertEquals(1000, query.getConfig().getMaxResultSetSize());
        assertEquals(3600, query.getConfig().getResultTtlSeconds());
    }

    private ChangeEvent createTestEvent(String id, String sourceId, String status) {
        return ChangeEvent.builder()
            .id(id)
            .type(ChangeEvent.ChangeType.INSERT)
            .entityType("Order")
            .entityId(1)
            .after(Map.of("id", 1, "status", status))
            .timestamp(Instant.now())
            .sourceId(sourceId)
            .build();
    }
}
