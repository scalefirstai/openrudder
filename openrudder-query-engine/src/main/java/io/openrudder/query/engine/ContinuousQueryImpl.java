package io.openrudder.query.engine;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.ContinuousQuery;
import io.openrudder.core.query.QueryResult;
import io.openrudder.core.query.ResultUpdate;
import reactor.core.publisher.Flux;

public class ContinuousQueryImpl {

    public static Flux<ResultUpdate> evaluate(ContinuousQuery query, Flux<ChangeEvent> changeStream) {
        QueryExecutor executor = new QueryExecutor(query);
        return executor.execute(changeStream);
    }

    public static Flux<QueryResult> initialEvaluation(ContinuousQuery query) {
        return Flux.empty();
    }
}
