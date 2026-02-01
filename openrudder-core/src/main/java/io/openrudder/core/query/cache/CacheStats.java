package io.openrudder.core.query.cache;

import lombok.Builder;
import lombok.Value;

/**
 * Statistics for result set cache.
 */
@Value
@Builder
public class CacheStats {
    long totalResults;
    long totalQueries;
    long hitCount;
    long missCount;
    long evictionCount;
    double hitRate;
    long memoryUsageBytes;
}
