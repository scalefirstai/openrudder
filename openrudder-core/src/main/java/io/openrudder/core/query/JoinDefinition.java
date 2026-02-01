package io.openrudder.core.query;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Defines a cross-source join by matching properties.
 */
@Value
@Builder
public class JoinDefinition {
    String joinId;
    List<JoinKey> keys;
    
    /**
     * A key participating in the join.
     */
    @Value
    @Builder
    public static class JoinKey {
        String label;
        String property;
    }
    
    /**
     * Validate that join has at least 2 keys.
     */
    public void validate() {
        if (keys == null || keys.size() < 2) {
            throw new IllegalArgumentException(
                "Join must have at least 2 keys, got: " + (keys != null ? keys.size() : 0));
        }
    }
}
