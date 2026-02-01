package io.openrudder.core.query;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ResultUpdate {
    UpdateType type;
    QueryResult before;
    QueryResult after;
    String queryId;
    Instant timestamp;

    public enum UpdateType {
        ADDED,
        UPDATED,
        REMOVED
    }

    public boolean isAdded() {
        return type == UpdateType.ADDED;
    }

    public boolean isUpdated() {
        return type == UpdateType.UPDATED;
    }

    public boolean isRemoved() {
        return type == UpdateType.REMOVED;
    }

    public QueryResult getResult() {
        return after != null ? after : before;
    }
}
