package io.openrudder.core.query;

import io.openrudder.core.model.ChangeEvent;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a change to the query result set.
 * Emitted when results are added, updated, or removed.
 */
@Value
@Builder
public class ResultChange {
    String queryId;
    ChangeType type;
    QueryResult before;
    QueryResult after;
    Instant timestamp;
    SourceChangeInfo sourceChange;
    
    public enum ChangeType {
        ADDED,
        UPDATED,
        DELETED
    }
    
    /**
     * Get the result ID (from before or after depending on change type).
     */
    public String resultId() {
        return switch (type) {
            case ADDED -> after.resultId();
            case DELETED -> before.resultId();
            case UPDATED -> after.resultId();
        };
    }
    
    /**
     * Get changed fields (for UPDATE type).
     */
    public Map<String, Object> changedFields() {
        if (type != ChangeType.UPDATED || before == null || after == null) {
            return Map.of();
        }
        
        Map<String, Object> changed = new HashMap<>();
        for (Map.Entry<String, Object> entry : after.data().entrySet()) {
            Object oldValue = before.data().get(entry.getKey());
            Object newValue = entry.getValue();
            if (oldValue == null && newValue != null || 
                oldValue != null && !oldValue.equals(newValue)) {
                changed.put(entry.getKey(), newValue);
            }
        }
        return changed;
    }
    
    /**
     * Information about the source change that caused this result change.
     */
    @Value
    @Builder
    public static class SourceChangeInfo {
        String sourceId;
        ChangeEvent.ChangeType changeType;
        String entityType;
        Object entityId;
    }
}
