package io.openrudder.core.reaction;

import java.util.*;

public class ReactionBuilder {
    
    private String id;
    private String name;
    private String kind;
    private Set<String> queryIds = new HashSet<>();
    private Map<String, ReactionConfig.QueryConfig> queryConfigs = new HashMap<>();
    private Map<String, Object> properties = new HashMap<>();
    private ReactionConfig.ExecutionConfig executionConfig = ReactionConfig.ExecutionConfig.defaults();
    private ReactionConfig.RetryConfig retryConfig = ReactionConfig.RetryConfig.defaults();
    private ReactionConfig.SecurityConfig securityConfig = new ReactionConfig.SecurityConfig(Map.of(), Map.of());
    
    public ReactionBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    public ReactionBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public ReactionBuilder kind(String kind) {
        this.kind = kind;
        return this;
    }
    
    public ReactionBuilder addQueryId(String queryId) {
        this.queryIds.add(queryId);
        return this;
    }
    
    public ReactionBuilder addQueryConfig(String queryId, ReactionConfig.QueryConfig config) {
        this.queryConfigs.put(queryId, config);
        return this;
    }
    
    public ReactionBuilder property(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }
    
    public ReactionBuilder executionConfig(ReactionConfig.ExecutionConfig config) {
        this.executionConfig = config;
        return this;
    }
    
    public ReactionBuilder retryConfig(ReactionConfig.RetryConfig config) {
        this.retryConfig = config;
        return this;
    }
    
    public ReactionBuilder securityConfig(ReactionConfig.SecurityConfig config) {
        this.securityConfig = config;
        return this;
    }
    
    public ReactionConfig build() {
        Objects.requireNonNull(id, "Reaction ID is required");
        Objects.requireNonNull(kind, "Reaction kind is required");
        if (queryIds.isEmpty()) {
            throw new IllegalStateException("At least one query ID is required");
        }
        
        return new ReactionConfig(
            id, name, kind, queryIds, queryConfigs,
            properties, executionConfig, retryConfig, securityConfig
        );
    }
}
