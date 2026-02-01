package io.openrudder.core.query.parser;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Represents an ORDER BY clause.
 */
@Value
@Builder
public class OrderBy {
    List<SortItem> items;
    
    @Value
    @Builder
    public static class SortItem {
        String field;
        @Builder.Default
        Direction direction = Direction.ASC;
    }
    
    public enum Direction {
        ASC, DESC
    }
}
