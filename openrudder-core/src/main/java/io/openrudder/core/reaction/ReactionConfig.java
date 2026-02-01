package io.openrudder.core.reaction;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public record ReactionConfig(
    String id,
    String name,
    String kind,
    Set<String> queryIds,
    Map<String, QueryConfig> queryConfigs,
    Map<String, Object> properties,
    ExecutionConfig executionConfig,
    RetryConfig retryConfig,
    SecurityConfig securityConfig
) {
    
    public record QueryConfig(
        String queryId,
        Map<String, Object> config
    ) {}
    
    public record ExecutionConfig(
        ExecutionMode mode,
        int batchSize,
        long batchWindowMs,
        long throttleRateMs,
        int maxConcurrency
    ) {
        public static ExecutionConfig defaults() {
            return new ExecutionConfig(
                ExecutionMode.ASYNC,
                100,
                1000,
                0,
                10
            );
        }
    }
    
    public record RetryConfig(
        boolean enabled,
        int maxAttempts,
        long initialBackoffMs,
        long maxBackoffMs,
        double backoffMultiplier,
        Set<Class<? extends Exception>> retryableExceptions
    ) {
        public static RetryConfig defaults() {
            return new RetryConfig(
                true,
                3,
                1000,
                30000,
                2.0,
                Set.of(IOException.class, TimeoutException.class)
            );
        }
    }
    
    public record SecurityConfig(
        Map<String, SecretReference> secrets,
        Map<String, String> environmentVariables
    ) {}
    
    public sealed interface SecretReference 
        permits KubernetesSecret, EnvironmentVariable, InlineValue {}
    
    public record KubernetesSecret(
        String secretName,
        String key
    ) implements SecretReference {}
    
    public record EnvironmentVariable(
        String variableName
    ) implements SecretReference {}
    
    public record InlineValue(
        String value
    ) implements SecretReference {}
    
    public enum ExecutionMode {
        SYNC,
        ASYNC,
        BATCH
    }
}
