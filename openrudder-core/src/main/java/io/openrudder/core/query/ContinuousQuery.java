package io.openrudder.core.query;

import io.openrudder.core.model.ChangeEvent;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Value
@Builder
public class ContinuousQuery {
    String id;
    String name;
    String query;
    Set<String> sourceIds;
    QueryConfig config;

    public Flux<ResultUpdate> evaluate(Flux<ChangeEvent> changeStream) {
        return changeStream
            .filter(event -> sourceIds == null || sourceIds.isEmpty() || sourceIds.contains(event.getSourceId()))
            .filter(event -> SimpleQueryEvaluator.evaluateConditions(event, query))
            .map(event -> {
                log.debug("Query '{}' matched event: {}", name, event.getId());
                QueryResult result = SimpleQueryEvaluator.createQueryResult(event, id, query);
                
                ResultUpdate.UpdateType updateType = switch (event.getType()) {
                    case INSERT, SNAPSHOT -> ResultUpdate.UpdateType.ADDED;
                    case UPDATE -> ResultUpdate.UpdateType.UPDATED;
                    case DELETE -> ResultUpdate.UpdateType.REMOVED;
                };
                
                return ResultUpdate.builder()
                    .type(updateType)
                    .queryId(id)
                    .after(updateType != ResultUpdate.UpdateType.REMOVED ? result : null)
                    .before(updateType == ResultUpdate.UpdateType.REMOVED ? result : null)
                    .timestamp(Instant.now())
                    .build();
            })
            .doOnError(error -> log.error("Error evaluating query '{}': {}", name, error.getMessage(), error));
    }

    public Flux<QueryResult> initialEvaluation() {
        log.debug("Initial evaluation not implemented for query '{}'", name);
        return Flux.empty();
    }

    @Value
    @Builder
    public static class QueryConfig {
        boolean enableIncremental;
        int maxResultSetSize;
        long resultTtlSeconds;
    }
}
