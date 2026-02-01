package io.openrudder.core.query.parser;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.QueryResult;
import io.openrudder.core.query.graph.GraphStore;
import io.openrudder.core.query.pattern.Pattern;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Executable query plan.
 */
public interface QueryPlan {
    
    /**
     * Get query ID.
     */
    String queryId();
    
    /**
     * Get all patterns in this query.
     */
    List<Pattern> patterns();
    
    /**
     * Get WHERE clause predicates.
     */
    List<Predicate> predicates();
    
    /**
     * Get RETURN clause projections.
     */
    List<Projection> projections();
    
    /**
     * Get ORDER BY clause.
     */
    Optional<OrderBy> orderBy();
    
    /**
     * Get LIMIT clause.
     */
    Optional<Integer> limit();
    
    /**
     * Execute plan against initial data.
     */
    Flux<QueryResult> execute(GraphStore store);
    
    /**
     * Identify patterns affected by a change event.
     */
    Set<Pattern> affectedPatterns(ChangeEvent change);
    
    /**
     * Evaluate query for a specific result ID.
     */
    QueryResult evaluateForResult(String resultId, GraphStore store);
}
