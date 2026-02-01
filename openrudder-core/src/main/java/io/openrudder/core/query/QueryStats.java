package io.openrudder.core.query;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Statistics for a continuous query.
 */
@Value
@Builder
public class QueryStats {
    long totalEventsProcessed;
    long totalResultsAdded;
    long totalResultsUpdated;
    long totalResultsDeleted;
    long currentResultCount;
    Instant lastProcessedEventTime;
    Instant createdAt;
    double avgProcessingTimeMs;
    long errorCount;
}
