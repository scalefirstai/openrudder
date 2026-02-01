package io.openrudder.core.query;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * Defines how a continuous query subscribes to a source.
 */
@Value
@Builder
public class SourceSubscription {
    String sourceId;
    Set<LabelMapping> nodeLabels;
    Set<LabelMapping> relationLabels;
    List<String> middlewarePipeline;
    
    /**
     * Mapping between source label and query label.
     */
    @Value
    @Builder
    public static class LabelMapping {
        String sourceLabel;
        String queryLabel;
        @Builder.Default
        boolean suppressIndex = false;
        
        public String effectiveQueryLabel() {
            return queryLabel != null ? queryLabel : sourceLabel;
        }
    }
}
