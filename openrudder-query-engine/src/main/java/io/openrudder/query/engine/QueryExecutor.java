package io.openrudder.query.engine;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.ContinuousQuery;
import io.openrudder.core.query.QueryResult;
import io.openrudder.core.query.ResultUpdate;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class QueryExecutor {

    private final ContinuousQuery query;
    private final Map<String, QueryResult> resultCache;
    private final QueryMatcher matcher;

    public QueryExecutor(ContinuousQuery query) {
        this.query = query;
        this.resultCache = new ConcurrentHashMap<>();
        this.matcher = new QueryMatcher(query.getQuery());
    }

    public Flux<ResultUpdate> execute(Flux<ChangeEvent> changeStream) {
        return changeStream
            .filter(event -> query.getSourceIds().contains(event.getSourceId()))
            .flatMap(this::evaluateIncremental);
    }

    private Flux<ResultUpdate> evaluateIncremental(ChangeEvent changeEvent) {
        List<ResultUpdate> updates = new ArrayList<>();

        if (changeEvent.isInsert() || changeEvent.isUpdate()) {
            if (matcher.matches(changeEvent)) {
                String resultId = generateResultId(changeEvent);
                QueryResult oldResult = resultCache.get(resultId);
                QueryResult newResult = createQueryResult(changeEvent);

                if (oldResult == null) {
                    resultCache.put(resultId, newResult);
                    updates.add(ResultUpdate.builder()
                        .type(ResultUpdate.UpdateType.ADDED)
                        .after(newResult)
                        .queryId(query.getId())
                        .timestamp(Instant.now())
                        .build());
                } else if (!oldResult.equals(newResult)) {
                    resultCache.put(resultId, newResult);
                    updates.add(ResultUpdate.builder()
                        .type(ResultUpdate.UpdateType.UPDATED)
                        .before(oldResult)
                        .after(newResult)
                        .queryId(query.getId())
                        .timestamp(Instant.now())
                        .build());
                }
            } else {
                String resultId = generateResultId(changeEvent);
                QueryResult oldResult = resultCache.remove(resultId);
                if (oldResult != null) {
                    updates.add(ResultUpdate.builder()
                        .type(ResultUpdate.UpdateType.REMOVED)
                        .before(oldResult)
                        .queryId(query.getId())
                        .timestamp(Instant.now())
                        .build());
                }
            }
        } else if (changeEvent.isDelete()) {
            String resultId = generateResultId(changeEvent);
            QueryResult oldResult = resultCache.remove(resultId);
            if (oldResult != null) {
                updates.add(ResultUpdate.builder()
                    .type(ResultUpdate.UpdateType.REMOVED)
                    .before(oldResult)
                    .queryId(query.getId())
                    .timestamp(Instant.now())
                    .build());
            }
        }

        return Flux.fromIterable(updates);
    }

    private QueryResult createQueryResult(ChangeEvent changeEvent) {
        return QueryResult.builder()
            .id(generateResultId(changeEvent))
            .queryId(query.getId())
            .data(changeEvent.getData())
            .timestamp(Instant.now())
            .metadata(Map.of(
                "entityType", changeEvent.getEntityType(),
                "entityId", changeEvent.getEntityId(),
                "sourceId", changeEvent.getSourceId()
            ))
            .build();
    }

    private String generateResultId(ChangeEvent changeEvent) {
        return String.format("%s_%s_%s", 
            query.getId(), 
            changeEvent.getEntityType(), 
            changeEvent.getEntityId());
    }

    public Collection<QueryResult> getCurrentResults() {
        return Collections.unmodifiableCollection(resultCache.values());
    }
}
