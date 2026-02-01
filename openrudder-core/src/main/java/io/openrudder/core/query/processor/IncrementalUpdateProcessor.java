package io.openrudder.core.query.processor;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.ContinuousQuery;
import io.openrudder.core.query.ResultChange;
import reactor.core.publisher.Flux;

/**
 * Processes source changes incrementally to compute result changes.
 * Implements incremental view maintenance algorithm.
 */
public interface IncrementalUpdateProcessor {
    
    /**
     * Process a source change and compute result changes.
     * 
     * Algorithm:
     * 1. Apply middleware transformations
     * 2. Update graph store with the change
     * 3. Identify affected query patterns
     * 4. For each pattern:
     *    a. Find candidate results that might be impacted
     *    b. Re-evaluate only those candidates
     *    c. Compute delta (added/updated/deleted)
     * 5. Emit result changes
     * 
     * @param query The continuous query
     * @param change Source change event
     * @return Result changes
     */
    Flux<ResultChange> processChange(ContinuousQuery query, ChangeEvent change);
    
    /**
     * Process initial snapshot data.
     */
    Flux<ResultChange> processSnapshot(ContinuousQuery query, Flux<ChangeEvent> snapshot);
}
