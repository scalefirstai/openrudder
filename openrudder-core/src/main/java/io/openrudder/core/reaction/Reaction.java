package io.openrudder.core.reaction;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.query.ResultChange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface Reaction {
    
    String id();
    
    String name();
    
    String kind();
    
    Set<String> queryIds();
    
    Map<String, Object> properties();
    
    Map<String, ReactionConfig.QueryConfig> queryConfigs();
    
    Mono<Void> processChange(ResultChange change);
    
    default Mono<Void> onResultAdded(QueryResult result, String queryId) {
        ResultChange change = ResultChange.builder()
            .queryId(queryId)
            .type(ResultChange.ChangeType.ADDED)
            .before(null)
            .after(result)
            .timestamp(Instant.now())
            .sourceChange(null)
            .build();
        return processChange(change);
    }
    
    default Mono<Void> onResultUpdated(
            QueryResult before, 
            QueryResult after, 
            String queryId) {
        ResultChange change = ResultChange.builder()
            .queryId(queryId)
            .type(ResultChange.ChangeType.UPDATED)
            .before(before)
            .after(after)
            .timestamp(Instant.now())
            .sourceChange(null)
            .build();
        return processChange(change);
    }
    
    default Mono<Void> onResultDeleted(QueryResult result, String queryId) {
        ResultChange change = ResultChange.builder()
            .queryId(queryId)
            .type(ResultChange.ChangeType.DELETED)
            .before(result)
            .after(null)
            .timestamp(Instant.now())
            .sourceChange(null)
            .build();
        return processChange(change);
    }
    
    void start();
    
    void stop();
    
    boolean isRunning();
    
    ReactionHealth health();
    
    ReactionStats stats();
    
    record ReactionHealth(
        boolean healthy,
        String status,
        String message
    ) {}
    
    record ReactionStats(
        long totalProcessed,
        long totalErrors,
        long addedCount,
        long updatedCount,
        long deletedCount,
        double avgProcessingTimeMs,
        Instant lastProcessedAt
    ) {}
}
