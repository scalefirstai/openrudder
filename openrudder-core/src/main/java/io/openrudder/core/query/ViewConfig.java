package io.openrudder.core.query;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Configuration for result view caching and retention.
 */
@Value
@Builder
public class ViewConfig {
    @Builder.Default
    boolean enabled = true;
    RetentionPolicy retentionPolicy;
    
    public sealed interface RetentionPolicy permits Latest, All, Expire {}
    
    /**
     * Keep only the latest version of results.
     */
    public record Latest() implements RetentionPolicy {}
    
    /**
     * Keep all historical versions (point-in-time queries).
     */
    public record All() implements RetentionPolicy {}
    
    /**
     * Keep non-current results for a limited time.
     */
    public record Expire(Duration ttl) implements RetentionPolicy {}
    
    public static ViewConfig defaultConfig() {
        return ViewConfig.builder()
            .enabled(true)
            .retentionPolicy(new Latest())
            .build();
    }
}
