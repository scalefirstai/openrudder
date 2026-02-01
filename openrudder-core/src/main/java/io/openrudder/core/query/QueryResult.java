package io.openrudder.core.query;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class QueryResult {
    String id;
    String queryId;
    Map<String, Object> data;
    @Builder.Default
    long version = 1;
    Instant timestamp;
    Map<String, Object> metadata;

    public Object get(String key) {
        return data != null ? data.get(key) : null;
    }

    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
    
    /**
     * Get result ID (alias for id).
     */
    public String resultId() {
        return id;
    }
    
    /**
     * Get result data (accessor method).
     */
    public Map<String, Object> data() {
        return data;
    }
    
    /**
     * Check if this result has a specific field.
     */
    public boolean hasField(String fieldName) {
        return data != null && data.containsKey(fieldName);
    }
}
