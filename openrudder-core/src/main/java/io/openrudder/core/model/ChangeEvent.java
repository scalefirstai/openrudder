package io.openrudder.core.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
@With
public class ChangeEvent {
    String id;
    ChangeType type;
    String entityType;
    Object entityId;
    Map<String, Object> before;
    Map<String, Object> after;
    Instant timestamp;
    String sourceId;
    Map<String, Object> metadata;

    public enum ChangeType {
        INSERT,
        UPDATE,
        DELETE,
        SNAPSHOT
    }

    public boolean isInsert() {
        return type == ChangeType.INSERT;
    }

    public boolean isUpdate() {
        return type == ChangeType.UPDATE;
    }

    public boolean isDelete() {
        return type == ChangeType.DELETE;
    }

    public boolean isSnapshot() {
        return type == ChangeType.SNAPSHOT;
    }

    public Map<String, Object> getData() {
        return after != null ? after : before;
    }

    public Object getFieldValue(String fieldName) {
        Map<String, Object> data = getData();
        return data != null ? data.get(fieldName) : null;
    }

    public Object getBeforeValue(String fieldName) {
        return before != null ? before.get(fieldName) : null;
    }

    public Object getAfterValue(String fieldName) {
        return after != null ? after.get(fieldName) : null;
    }

    public boolean hasFieldChanged(String fieldName) {
        if (!isUpdate()) {
            return false;
        }
        Object beforeVal = getBeforeValue(fieldName);
        Object afterVal = getAfterValue(fieldName);
        
        if (beforeVal == null && afterVal == null) {
            return false;
        }
        if (beforeVal == null || afterVal == null) {
            return true;
        }
        return !beforeVal.equals(afterVal);
    }
}
