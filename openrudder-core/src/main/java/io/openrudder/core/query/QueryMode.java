package io.openrudder.core.query;

/**
 * Query execution mode.
 */
public enum QueryMode {
    /**
     * Maintain full result set, emit detailed changes (added/updated/deleted).
     */
    QUERY,
    
    /**
     * Simplified mode, only emit additions.
     */
    FILTER
}
