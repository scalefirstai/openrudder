# Reactions Implementation Specification for Drasi-Java

**Document Version:** 1.0  
**Target Implementation:** Windsurf IDE  
**Language:** Java 21+  
**Framework:** Spring Boot 3.x  
**Based on:** https://drasi.io/concepts/reactions/  

---

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Architecture](#architecture)
4. [Data Models](#data-models)
5. [Reaction Provider Framework](#reaction-provider-framework)
6. [Built-in Reaction Types](#built-in-reaction-types)
7. [AI Agent Reactions](#ai-agent-reactions)
8. [Configuration Schema](#configuration-schema)
9. [API Specifications](#api-specifications)
10. [Test Specifications](#test-specifications)
11. [Implementation Checklist](#implementation-checklist)

---

## 1. Overview

### 1.1 Purpose
Implement a **Reaction Framework** that processes query result changes from Continuous Queries and executes configurable actions. Reactions are the output layer of Drasi that connect query results to downstream systems and AI agents.

### 1.2 Key Requirements
- ✅ Subscribe to one or more Continuous Queries
- ✅ Process result changes (added/updated/deleted)
- ✅ Execute configurable actions (webhooks, message queues, databases, AI agents)
- ✅ Support pluggable reaction providers
- ✅ Handle per-query configuration
- ✅ Manage credentials securely (Kubernetes secrets, environment variables)
- ✅ Provide error handling and retry logic
- ✅ Support both synchronous and asynchronous execution
- ✅ Enable batching and throttling
- ✅ Integrate with Spring Boot ecosystem

### 1.3 Drasi Reaction Flow

```
Continuous Query → Result Changes → Reaction → Action
      ↓                  ↓              ↓          ↓
   MATCH o:Order    [ADDED o:42]   Process    HTTP POST
   WHERE ready         ↓           Change      Send Email
   RETURN o         [UPDATED]        ↓         Update DB
                    [DELETED]     Execute     Call AI Agent
```

---

## 2. Core Concepts

### 2.1 What is a Reaction?

A **Reaction** processes the stream of query result changes and takes action:

```java
// Reaction subscribes to query changes
Reaction reaction = HttpReaction.builder()
    .queryId("ready-orders")
    .webhookUrl("https://api.example.com/orders")
    .onResultAdded(result -> {
        // POST new order to webhook
        httpClient.post(webhookUrl, result);
    })
    .build();

// As query results change:
// ADDED → HTTP POST with new result
// UPDATED → HTTP PATCH with before/after
// DELETED → HTTP DELETE with removed result
```

### 2.2 Reaction Lifecycle

```
1. Creation → 2. Subscription → 3. Processing → 4. Action → 5. Deletion
      ↓              ↓               ↓             ↓           ↓
   Define        Subscribe       Receive       Execute      Clean
   Config        to Queries      Changes       Action        Up
```

### 2.3 Reaction Types

**Microsoft Drasi Reactions:**
1. **Event Grid** - Azure Event Grid integration
2. **Event Bridge** - AWS EventBridge integration
3. **Storage Queue** - Azure Storage Queue
4. **Debug** - Development/debugging tool
5. **SignalR** - Real-time web notifications
6. **Gremlin** - Graph database commands
7. **Stored Procedure** - SQL database procedures
8. **Dapr State Store** - Materialized views
9. **Dapr Pub/Sub** - Message publishing

**Drasi-Java Additional Reactions:**
1. **LangChain4j** - AI agent integration
2. **Spring AI** - Spring AI framework
3. **LangGraph** - Multi-agent workflows
4. **Kafka** - Apache Kafka producer
5. **RabbitMQ** - AMQP messaging
6. **MongoDB** - Document updates
7. **Elasticsearch** - Search indexing
8. **Redis** - Cache updates
9. **Email** - SMTP notifications
10. **Slack/Teams** - Chat notifications

---

## 3. Architecture

### 3.1 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Reaction Framework                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │         Reaction Registry & Lifecycle               │    │
│  │  - Create/Start/Stop reactions                      │    │
│  │  - Manage subscriptions to queries                  │    │
│  │  - Health monitoring                                │    │
│  └────────────────────────────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐   │
│  │         Query Change Dispatcher                      │   │
│  │  - Subscribe to continuous queries                   │   │
│  │  - Route changes to reactions                        │   │
│  │  - Filter by query-specific config                   │   │
│  └────────────────────────────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐   │
│  │         Reaction Executor                            │   │
│  │  - Execute reaction logic                            │   │
│  │  - Handle errors and retries                         │   │
│  │  - Apply batching and throttling                     │   │
│  │  - Metrics and observability                         │   │
│  └────────────────────────────────────────────────────┘    │
│                           │                                  │
│  ┌────────────────────────▼────────────────────────────┐   │
│  │         Reaction Provider SPI                        │   │
│  │  - Pluggable reaction implementations                │   │
│  │  - Standard interfaces                               │   │
│  │  - Configuration validation                          │   │
│  └────────────────────────────────────────────────────┘    │
│                           │                                  │
└───────────────────────────┼──────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   HTTP       │    │   AI Agents  │    │   Message    │
│   Webhooks   │    │  (LangChain) │    │   Queues     │
└──────────────┘    └──────────────┘    └──────────────┘
        ▼                   ▼                   ▼
   External API       OpenAI/Claude        Kafka/RabbitMQ
```

### 3.2 Component Layers

1. **Registry Layer**: Lifecycle management
2. **Dispatcher Layer**: Route changes to reactions
3. **Executor Layer**: Execute actions with retries
4. **Provider Layer**: Pluggable implementations
5. **Integration Layer**: External systems

---

## 4. Data Models

### 4.1 Reaction Interface

```java
package io.drasi.core.reaction;

import io.drasi.core.query.QueryResult;
import io.drasi.core.query.ResultChange;
import reactor.core.publisher.Mono;

/**
 * Base interface for all reactions.
 * Processes query result changes and executes actions.
 */
public interface Reaction {
    
    /**
     * Unique identifier for this reaction.
     */
    String id();
    
    /**
     * Human-readable name.
     */
    String name();
    
    /**
     * Type/kind of this reaction (e.g., "http", "kafka", "ai-agent").
     */
    String kind();
    
    /**
     * IDs of queries this reaction subscribes to.
     */
    Set<String> queryIds();
    
    /**
     * Configuration properties for this reaction.
     */
    Map<String, Object> properties();
    
    /**
     * Per-query configuration (optional).
     * Key: queryId, Value: query-specific config
     */
    Map<String, QueryConfig> queryConfigs();
    
    /**
     * Process a result change.
     * Called when a subscribed query emits a change.
     * 
     * @param change Result change event
     * @return Mono that completes when action is executed
     */
    Mono<Void> processChange(ResultChange change);
    
    /**
     * Called when a new result is added to a query.
     * Default implementation delegates to processChange.
     */
    default Mono<Void> onResultAdded(QueryResult result, String queryId) {
        ResultChange change = new ResultChange(
            queryId,
            ResultChange.ChangeType.ADDED,
            null,
            result,
            Instant.now(),
            null
        );
        return processChange(change);
    }
    
    /**
     * Called when an existing result is updated.
     */
    default Mono<Void> onResultUpdated(
            QueryResult before, 
            QueryResult after, 
            String queryId) {
        ResultChange change = new ResultChange(
            queryId,
            ResultChange.ChangeType.UPDATED,
            before,
            after,
            Instant.now(),
            null
        );
        return processChange(change);
    }
    
    /**
     * Called when a result is removed from a query.
     */
    default Mono<Void> onResultDeleted(QueryResult result, String queryId) {
        ResultChange change = new ResultChange(
            queryId,
            ResultChange.ChangeType.DELETED,
            result,
            null,
            Instant.now(),
            null
        );
        return processChange(change);
    }
    
    /**
     * Start the reaction.
     */
    void start();
    
    /**
     * Stop the reaction gracefully.
     */
    void stop();
    
    /**
     * Check if reaction is running.
     */
    boolean isRunning();
    
    /**
     * Health check.
     */
    ReactionHealth health();
    
    /**
     * Reaction statistics.
     */
    ReactionStats stats();
    
    record ReactionHealth(
        boolean healthy,
        String status,
        String message
    ) {}
    
    record ReactionStats(
        long totalProcessed,
        long totalErrors,
        long addedCount,
        long updatedCount,
        long deletedCount,
        double avgProcessingTimeMs,
        Instant lastProcessedAt
    ) {}
}
```

### 4.2 ReactionConfig Model

```java
package io.drasi.core.reaction;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for a reaction.
 */
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
    
    /**
     * Per-query configuration.
     */
    public record QueryConfig(
        String queryId,
        Map<String, Object> config
    ) {}
    
    /**
     * Execution configuration (batching, throttling, etc.).
     */
    public record ExecutionConfig(
        ExecutionMode mode,      // SYNC, ASYNC, BATCH
        int batchSize,           // For batch mode
        long batchWindowMs,      // For batch mode
        long throttleRateMs,     // Min time between executions
        int maxConcurrency       // Max parallel executions
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
    
    /**
     * Retry configuration.
     */
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
    
    /**
     * Security configuration (credentials, secrets).
     */
    public record SecurityConfig(
        Map<String, SecretReference> secrets,
        Map<String, String> environmentVariables
    ) {}
    
    /**
     * Reference to a Kubernetes secret or environment variable.
     */
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
        SYNC,   // Blocking execution
        ASYNC,  // Non-blocking, fire-and-forget
        BATCH   // Collect changes and process in batches
    }
}
```

### 4.3 ReactionProvider SPI

```java
package io.drasi.core.reaction.spi;

import io.drasi.core.reaction.Reaction;
import io.drasi.core.reaction.ReactionConfig;

/**
 * Service Provider Interface for creating reactions.
 * Implementations are discovered via Java ServiceLoader.
 */
public interface ReactionProvider {
    
    /**
     * The kind/type of reaction this provider creates.
     * Must match spec.kind in YAML configuration.
     */
    String kind();
    
    /**
     * Create a reaction instance from configuration.
     */
    Reaction create(ReactionConfig config);
    
    /**
     * Validate configuration before creating reaction.
     */
    ValidationResult validate(ReactionConfig config);
    
    /**
     * Get configuration schema for this reaction type.
     * Used for documentation and validation.
     */
    ConfigSchema configSchema();
}

/**
 * Validation result.
 */
public record ValidationResult(
    boolean valid,
    List<String> errors
) {
    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }
    
    public static ValidationResult invalid(String... errors) {
        return new ValidationResult(false, List.of(errors));
    }
}

/**
 * Configuration schema definition.
 */
public record ConfigSchema(
    String kind,
    String description,
    List<PropertySchema> properties
) {
    
    public record PropertySchema(
        String name,
        String type,
        boolean required,
        String description,
        Object defaultValue
    ) {}
}
```

### 4.4 ReactionBuilder

```java
package io.drasi.core.reaction;

import java.util.*;

/**
 * Fluent builder for creating reactions.
 */
public class ReactionBuilder {
    
    private String id;
    private String name;
    private String kind;
    private Set<String> queryIds = new HashSet<>();
    private Map<String, QueryConfig> queryConfigs = new HashMap<>();
    private Map<String, Object> properties = new HashMap<>();
    private ExecutionConfig executionConfig = ExecutionConfig.defaults();
    private RetryConfig retryConfig = RetryConfig.defaults();
    private SecurityConfig securityConfig = new SecurityConfig(Map.of(), Map.of());
    
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
    
    public ReactionBuilder addQueryConfig(String queryId, QueryConfig config) {
        this.queryConfigs.put(queryId, config);
        return this;
    }
    
    public ReactionBuilder property(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }
    
    public ReactionBuilder executionConfig(ExecutionConfig config) {
        this.executionConfig = config;
        return this;
    }
    
    public ReactionBuilder retryConfig(RetryConfig config) {
        this.retryConfig = config;
        return this;
    }
    
    public ReactionBuilder securityConfig(SecurityConfig config) {
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
```

---

## 5. Reaction Provider Framework

### 5.1 Base Reaction Implementation

```java
package io.drasi.core.reaction;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for reaction implementations.
 * Provides common functionality: retries, metrics, error handling.
 */
public abstract class AbstractReaction implements Reaction {
    
    protected final ReactionConfig config;
    protected final AtomicLong totalProcessed = new AtomicLong(0);
    protected final AtomicLong totalErrors = new AtomicLong(0);
    protected final AtomicLong addedCount = new AtomicLong(0);
    protected final AtomicLong updatedCount = new AtomicLong(0);
    protected final AtomicLong deletedCount = new AtomicLong(0);
    protected volatile Instant lastProcessedAt;
    protected volatile boolean running = false;
    
    protected AbstractReaction(ReactionConfig config) {
        this.config = config;
    }
    
    @Override
    public String id() {
        return config.id();
    }
    
    @Override
    public String name() {
        return config.name();
    }
    
    @Override
    public String kind() {
        return config.kind();
    }
    
    @Override
    public Set<String> queryIds() {
        return config.queryIds();
    }
    
    @Override
    public Map<String, Object> properties() {
        return config.properties();
    }
    
    @Override
    public Map<String, QueryConfig> queryConfigs() {
        return config.queryConfigs();
    }
    
    @Override
    public Mono<Void> processChange(ResultChange change) {
        return Mono.defer(() -> {
            // Get query-specific config if available
            QueryConfig queryConfig = config.queryConfigs().get(change.queryId());
            
            // Delegate to implementation
            return switch (change.type()) {
                case ADDED -> {
                    addedCount.incrementAndGet();
                    yield doProcessAdded(change.after(), change.queryId(), queryConfig);
                }
                case UPDATED -> {
                    updatedCount.incrementAndGet();
                    yield doProcessUpdated(
                        change.before(), 
                        change.after(), 
                        change.queryId(), 
                        queryConfig
                    );
                }
                case DELETED -> {
                    deletedCount.incrementAndGet();
                    yield doProcessDeleted(change.before(), change.queryId(), queryConfig);
                }
            };
        })
        .doOnSuccess(v -> {
            totalProcessed.incrementAndGet();
            lastProcessedAt = Instant.now();
        })
        .doOnError(error -> {
            totalErrors.incrementAndGet();
            handleError(change, error);
        })
        .retryWhen(buildRetrySpec());
    }
    
    /**
     * Process an ADDED result.
     * Subclasses must implement this.
     */
    protected abstract Mono<Void> doProcessAdded(
        QueryResult result, 
        String queryId, 
        QueryConfig queryConfig);
    
    /**
     * Process an UPDATED result.
     * Default implementation calls doProcessAdded.
     */
    protected Mono<Void> doProcessUpdated(
        QueryResult before,
        QueryResult after,
        String queryId,
        QueryConfig queryConfig) {
        return doProcessAdded(after, queryId, queryConfig);
    }
    
    /**
     * Process a DELETED result.
     * Default implementation does nothing.
     */
    protected Mono<Void> doProcessDeleted(
        QueryResult result,
        String queryId,
        QueryConfig queryConfig) {
        return Mono.empty();
    }
    
    /**
     * Build retry specification from config.
     */
    protected Retry buildRetrySpec() {
        if (!config.retryConfig().enabled()) {
            return Retry.max(0);
        }
        
        return Retry.backoff(
            config.retryConfig().maxAttempts(),
            Duration.ofMillis(config.retryConfig().initialBackoffMs())
        )
        .maxBackoff(Duration.ofMillis(config.retryConfig().maxBackoffMs()))
        .filter(this::isRetryable)
        .doBeforeRetry(signal -> 
            logRetry(signal.totalRetries(), signal.failure())
        );
    }
    
    /**
     * Check if exception is retryable.
     */
    protected boolean isRetryable(Throwable error) {
        Set<Class<? extends Exception>> retryable = 
            config.retryConfig().retryableExceptions();
        
        return retryable.stream()
            .anyMatch(exClass -> exClass.isInstance(error));
    }
    
    /**
     * Handle error.
     */
    protected void handleError(ResultChange change, Throwable error) {
        // Log error
        System.err.printf(
            "Error processing change in reaction %s: %s%n",
            id(), error.getMessage()
        );
    }
    
    /**
     * Log retry attempt.
     */
    protected void logRetry(long attempt, Throwable error) {
        System.out.printf(
            "Retrying reaction %s (attempt %d): %s%n",
            id(), attempt, error.getMessage()
        );
    }
    
    @Override
    public void start() {
        running = true;
    }
    
    @Override
    public void stop() {
        running = false;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public ReactionHealth health() {
        if (!running) {
            return new ReactionHealth(false, "STOPPED", "Reaction is not running");
        }
        
        // Check if too many errors
        long total = totalProcessed.get();
        long errors = totalErrors.get();
        if (total > 100 && errors > total * 0.1) {
            return new ReactionHealth(
                false, 
                "UNHEALTHY", 
                String.format("High error rate: %d/%d", errors, total)
            );
        }
        
        return new ReactionHealth(true, "HEALTHY", "Reaction is running");
    }
    
    @Override
    public ReactionStats stats() {
        long total = totalProcessed.get();
        double avgTime = 0.0; // TODO: Track processing times
        
        return new ReactionStats(
            total,
            totalErrors.get(),
            addedCount.get(),
            updatedCount.get(),
            deletedCount.get(),
            avgTime,
            lastProcessedAt
        );
    }
}
```

### 5.2 Reaction Registry

```java
package io.drasi.core.reaction;

import io.drasi.core.reaction.spi.ReactionProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing reaction instances.
 */
public class ReactionRegistry {
    
    private final Map<String, Reaction> reactions = new ConcurrentHashMap<>();
    private final Map<String, ReactionProvider> providers = new ConcurrentHashMap<>();
    
    public ReactionRegistry() {
        // Discover reaction providers via ServiceLoader
        ServiceLoader.load(ReactionProvider.class)
            .forEach(provider -> {
                providers.put(provider.kind(), provider);
            });
    }
    
    /**
     * Create and register a reaction.
     */
    public Mono<Reaction> create(ReactionConfig config) {
        return Mono.fromCallable(() -> {
            // Get provider for this kind
            ReactionProvider provider = providers.get(config.kind());
            if (provider == null) {
                throw new IllegalArgumentException(
                    "No provider found for reaction kind: " + config.kind());
            }
            
            // Validate configuration
            ValidationResult validation = provider.validate(config);
            if (!validation.valid()) {
                throw new IllegalArgumentException(
                    "Invalid configuration: " + String.join(", ", validation.errors()));
            }
            
            // Create reaction
            Reaction reaction = provider.create(config);
            
            // Register and start
            reactions.put(reaction.id(), reaction);
            reaction.start();
            
            return reaction;
        });
    }
    
    /**
     * Get a reaction by ID.
     */
    public Mono<Reaction> get(String id) {
        return Mono.justOrEmpty(reactions.get(id));
    }
    
    /**
     * List all reactions.
     */
    public Flux<Reaction> list() {
        return Flux.fromIterable(reactions.values());
    }
    
    /**
     * Delete a reaction.
     */
    public Mono<Void> delete(String id) {
        return Mono.fromRunnable(() -> {
            Reaction reaction = reactions.remove(id);
            if (reaction != null) {
                reaction.stop();
            }
        });
    }
    
    /**
     * Get available reaction providers.
     */
    public Map<String, ReactionProvider> getProviders() {
        return Map.copyOf(providers);
    }
}
```

### 5.3 Query Change Dispatcher

```java
package io.drasi.core.reaction;

import io.drasi.core.query.ContinuousQuery;
import io.drasi.core.query.ResultChange;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches query result changes to subscribed reactions.
 */
public class QueryChangeDispatcher {
    
    private final ReactionRegistry reactionRegistry;
    
    // queryId -> Set of reaction IDs
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    
    public QueryChangeDispatcher(ReactionRegistry reactionRegistry) {
        this.reactionRegistry = reactionRegistry;
    }
    
    /**
     * Subscribe a reaction to a query.
     */
    public void subscribe(String queryId, String reactionId) {
        subscriptions
            .computeIfAbsent(queryId, k -> ConcurrentHashMap.newKeySet())
            .add(reactionId);
    }
    
    /**
     * Unsubscribe a reaction from a query.
     */
    public void unsubscribe(String queryId, String reactionId) {
        Set<String> reactionIds = subscriptions.get(queryId);
        if (reactionIds != null) {
            reactionIds.remove(reactionId);
        }
    }
    
    /**
     * Connect to a continuous query and dispatch changes.
     */
    public Flux<Void> connectToQuery(ContinuousQuery query) {
        return query.currentResults()
            .flatMap(result -> {
                // Process initial results as ADDED
                ResultChange change = new ResultChange(
                    query.id(),
                    ResultChange.ChangeType.ADDED,
                    null,
                    result,
                    Instant.now(),
                    null
                );
                return dispatchChange(change);
            })
            .thenMany(
                // Subscribe to future changes
                subscribeToChanges(query)
            );
    }
    
    /**
     * Subscribe to query changes and dispatch to reactions.
     */
    private Flux<Void> subscribeToChanges(ContinuousQuery query) {
        // Get change stream from query
        return getQueryChangeStream(query)
            .flatMap(this::dispatchChange);
    }
    
    /**
     * Dispatch a change to all subscribed reactions.
     */
    private Flux<Void> dispatchChange(ResultChange change) {
        String queryId = change.queryId();
        Set<String> reactionIds = subscriptions.get(queryId);
        
        if (reactionIds == null || reactionIds.isEmpty()) {
            return Flux.empty();
        }
        
        return Flux.fromIterable(reactionIds)
            .flatMap(reactionId -> 
                reactionRegistry.get(reactionId)
                    .flatMap(reaction -> 
                        reaction.processChange(change)
                            .onErrorResume(error -> {
                                // Log but don't fail other reactions
                                System.err.printf(
                                    "Error in reaction %s: %s%n",
                                    reactionId, error.getMessage()
                                );
                                return Mono.empty();
                            })
                    )
            );
    }
    
    private Flux<ResultChange> getQueryChangeStream(ContinuousQuery query) {
        // TODO: Implement subscription to query change stream
        // This would connect to the query's result change publisher
        return Flux.empty();
    }
}
```

---

## 6. Built-in Reaction Types

### 6.1 HTTP Webhook Reaction

```java
package io.drasi.reaction.http;

import io.drasi.core.reaction.*;
import io.drasi.core.query.QueryResult;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * HTTP webhook reaction.
 * Sends HTTP requests when query results change.
 */
public class HttpWebhookReaction extends AbstractReaction {
    
    private final WebClient webClient;
    private final String webhookUrl;
    private final HttpMethod method;
    private final Map<String, String> headers;
    
    public HttpWebhookReaction(ReactionConfig config) {
        super(config);
        
        this.webhookUrl = (String) config.properties().get("webhookUrl");
        this.method = HttpMethod.valueOf(
            (String) config.properties().getOrDefault("method", "POST")
        );
        this.headers = (Map<String, String>) 
            config.properties().getOrDefault("headers", Map.of());
        
        this.webClient = WebClient.builder()
            .baseUrl(webhookUrl)
            .defaultHeaders(httpHeaders -> headers.forEach(httpHeaders::add))
            .build();
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            QueryConfig queryConfig) {
        
        return webClient
            .method(org.springframework.http.HttpMethod.valueOf(method.name()))
            .uri("")
            .bodyValue(createPayload(result, queryId, "added"))
            .retrieve()
            .bodyToMono(Void.class);
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            QueryConfig queryConfig) {
        
        return webClient
            .method(org.springframework.http.HttpMethod.valueOf(method.name()))
            .uri("")
            .bodyValue(createUpdatePayload(before, after, queryId))
            .retrieve()
            .bodyToMono(Void.class);
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            QueryConfig queryConfig) {
        
        return webClient
            .method(org.springframework.http.HttpMethod.valueOf(method.name()))
            .uri("")
            .bodyValue(createPayload(result, queryId, "deleted"))
            .retrieve()
            .bodyToMono(Void.class);
    }
    
    private Map<String, Object> createPayload(
            QueryResult result, 
            String queryId, 
            String changeType) {
        return Map.of(
            "queryId", queryId,
            "changeType", changeType,
            "result", result.data(),
            "timestamp", result.timestamp()
        );
    }
    
    private Map<String, Object> createUpdatePayload(
            QueryResult before,
            QueryResult after,
            String queryId) {
        return Map.of(
            "queryId", queryId,
            "changeType", "updated",
            "before", before.data(),
            "after", after.data(),
            "timestamp", after.timestamp()
        );
    }
    
    enum HttpMethod {
        GET, POST, PUT, PATCH, DELETE
    }
}

/**
 * Provider for HTTP webhook reactions.
 */
@AutoService(ReactionProvider.class)
public class HttpWebhookReactionProvider implements ReactionProvider {
    
    @Override
    public String kind() {
        return "http";
    }
    
    @Override
    public Reaction create(ReactionConfig config) {
        return new HttpWebhookReaction(config);
    }
    
    @Override
    public ValidationResult validate(ReactionConfig config) {
        List<String> errors = new ArrayList<>();
        
        if (!config.properties().containsKey("webhookUrl")) {
            errors.add("webhookUrl is required");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.valid() 
            : ValidationResult.invalid(errors.toArray(String[]::new));
    }
    
    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(
            "http",
            "HTTP webhook reaction",
            List.of(
                new PropertySchema("webhookUrl", "string", true, 
                    "URL to send webhooks to", null),
                new PropertySchema("method", "string", false, 
                    "HTTP method (default: POST)", "POST"),
                new PropertySchema("headers", "map", false, 
                    "HTTP headers", Map.of())
            )
        );
    }
}
```

### 6.2 Kafka Message Queue Reaction

```java
package io.drasi.reaction.kafka;

import io.drasi.core.reaction.*;
import io.drasi.core.query.QueryResult;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Kafka reaction.
 * Publishes query results to Kafka topics.
 */
public class KafkaReaction extends AbstractReaction {
    
    private final KafkaSender<String, String> sender;
    private final String topic;
    private final String keyField;
    
    public KafkaReaction(ReactionConfig config, KafkaSender<String, String> sender) {
        super(config);
        this.sender = sender;
        this.topic = (String) config.properties().get("topic");
        this.keyField = (String) config.properties().getOrDefault("keyField", "id");
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            QueryConfig queryConfig) {
        
        String key = extractKey(result);
        String value = serializeResult(result, queryId, "added");
        
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topic, key, value);
        
        return sender
            .send(Mono.just(SenderRecord.create(record, null)))
            .then();
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            QueryConfig queryConfig) {
        
        String key = extractKey(after);
        String value = serializeUpdate(before, after, queryId);
        
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topic, key, value);
        
        return sender
            .send(Mono.just(SenderRecord.create(record, null)))
            .then();
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            QueryConfig queryConfig) {
        
        String key = extractKey(result);
        
        // Send tombstone record (null value) for deletion
        ProducerRecord<String, String> record = 
            new ProducerRecord<>(topic, key, null);
        
        return sender
            .send(Mono.just(SenderRecord.create(record, null)))
            .then();
    }
    
    private String extractKey(QueryResult result) {
        Object keyValue = result.data().get(keyField);
        return keyValue != null ? keyValue.toString() : result.resultId();
    }
    
    private String serializeResult(QueryResult result, String queryId, String changeType) {
        // Serialize to JSON
        Map<String, Object> payload = Map.of(
            "queryId", queryId,
            "changeType", changeType,
            "result", result.data()
        );
        return toJson(payload);
    }
    
    private String serializeUpdate(QueryResult before, QueryResult after, String queryId) {
        Map<String, Object> payload = Map.of(
            "queryId", queryId,
            "changeType", "updated",
            "before", before.data(),
            "after", after.data()
        );
        return toJson(payload);
    }
    
    private String toJson(Object obj) {
        // Use Jackson or similar
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize", e);
        }
    }
}
```

### 6.3 Debug Reaction

```java
package io.drasi.reaction.debug;

import io.drasi.core.reaction.*;
import io.drasi.core.query.QueryResult;
import reactor.core.publisher.Mono;

/**
 * Debug reaction for development.
 * Logs all changes to console.
 */
public class DebugReaction extends AbstractReaction {
    
    private final boolean verbose;
    
    public DebugReaction(ReactionConfig config) {
        super(config);
        this.verbose = (Boolean) config.properties()
            .getOrDefault("verbose", false);
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            QueryConfig queryConfig) {
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("ADDED result in query: " + queryId);
        System.out.println("Result ID: " + result.resultId());
        if (verbose) {
            System.out.println("Data: " + result.data());
        } else {
            System.out.println("Fields: " + result.data().keySet());
        }
        System.out.println("═══════════════════════════════════════");
        
        return Mono.empty();
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            QueryConfig queryConfig) {
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("UPDATED result in query: " + queryId);
        System.out.println("Result ID: " + after.resultId());
        
        if (verbose) {
            System.out.println("BEFORE: " + before.data());
            System.out.println("AFTER:  " + after.data());
        } else {
            Map<String, Object> changed = getChangedFields(before, after);
            System.out.println("Changed fields: " + changed);
        }
        
        System.out.println("═══════════════════════════════════════");
        
        return Mono.empty();
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            QueryConfig queryConfig) {
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("DELETED result in query: " + queryId);
        System.out.println("Result ID: " + result.resultId());
        if (verbose) {
            System.out.println("Data: " + result.data());
        }
        System.out.println("═══════════════════════════════════════");
        
        return Mono.empty();
    }
    
    private Map<String, Object> getChangedFields(
            QueryResult before, 
            QueryResult after) {
        return after.data().entrySet().stream()
            .filter(e -> !e.getValue().equals(before.data().get(e.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
```

---

## 7. AI Agent Reactions

### 7.1 LangChain4j AI Agent Reaction

```java
package io.drasi.reaction.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.AiMessage;
import io.drasi.core.reaction.*;
import io.drasi.core.query.QueryResult;
import reactor.core.publisher.Mono;

/**
 * LangChain4j AI agent reaction.
 * Invokes AI models to process query results.
 */
public class LangChain4jReaction extends AbstractReaction {
    
    private final ChatLanguageModel model;
    private final String systemPrompt;
    private final String userPromptTemplate;
    private final Function<AiMessage, Mono<Void>> responseHandler;
    
    public LangChain4jReaction(
            ReactionConfig config,
            ChatLanguageModel model,
            String systemPrompt,
            String userPromptTemplate,
            Function<AiMessage, Mono<Void>> responseHandler) {
        super(config);
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.responseHandler = responseHandler;
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            QueryConfig queryConfig) {
        
        return invokeAI(result, queryId, "added");
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            QueryConfig queryConfig) {
        
        return invokeAI(after, queryId, "updated");
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            QueryConfig queryConfig) {
        
        return invokeAI(result, queryId, "deleted");
    }
    
    private Mono<Void> invokeAI(
            QueryResult result, 
            String queryId, 
            String changeType) {
        
        return Mono.fromCallable(() -> {
            // Build prompt
            String userPrompt = formatPrompt(result, queryId, changeType);
            
            // Invoke AI model
            AiMessage response = model.generate(userPrompt);
            
            return response;
        })
        .flatMap(responseHandler)
        .subscribeOn(Schedulers.boundedElastic()); // AI calls can be blocking
    }
    
    private String formatPrompt(
            QueryResult result, 
            String queryId, 
            String changeType) {
        
        String prompt = userPromptTemplate;
        
        // Replace placeholders
        prompt = prompt.replace("{queryId}", queryId);
        prompt = prompt.replace("{changeType}", changeType);
        
        // Replace field values
        for (Map.Entry<String, Object> entry : result.data().entrySet()) {
            prompt = prompt.replace(
                "{" + entry.getKey() + "}", 
                String.valueOf(entry.getValue())
            );
        }
        
        return prompt;
    }
}

/**
 * Builder for LangChain4j reactions.
 */
public class LangChain4jReactionBuilder extends ReactionBuilder {
    
    private ChatLanguageModel model;
    private String systemPrompt = "You are a helpful assistant.";
    private String userPromptTemplate;
    private Function<AiMessage, Mono<Void>> responseHandler = msg -> Mono.empty();
    
    public LangChain4jReactionBuilder model(ChatLanguageModel model) {
        this.model = model;
        return this;
    }
    
    public LangChain4jReactionBuilder systemPrompt(String prompt) {
        this.systemPrompt = prompt;
        return this;
    }
    
    public LangChain4jReactionBuilder userPromptTemplate(String template) {
        this.userPromptTemplate = template;
        return this;
    }
    
    public LangChain4jReactionBuilder onResponse(
            Function<AiMessage, Mono<Void>> handler) {
        this.responseHandler = handler;
        return this;
    }
    
    public LangChain4jReaction buildAI() {
        ReactionConfig config = this.kind("langchain4j").build();
        return new LangChain4jReaction(
            config, model, systemPrompt, userPromptTemplate, responseHandler
        );
    }
}
```

### 7.2 Multi-Agent Workflow Reaction (LangGraph)

```java
package io.drasi.reaction.ai;

import io.drasi.core.reaction.*;
import io.drasi.core.query.QueryResult;
import reactor.core.publisher.Mono;

/**
 * LangGraph multi-agent workflow reaction.
 * Orchestrates multiple AI agents to process results.
 */
public class LangGraphWorkflowReaction extends AbstractReaction {
    
    private final LangGraphWorkflow workflow;
    
    public LangGraphWorkflowReaction(
            ReactionConfig config,
            LangGraphWorkflow workflow) {
        super(config);
        this.workflow = workflow;
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            QueryConfig queryConfig) {
        
        return workflow.execute(Map.of(
            "result", result,
            "queryId", queryId,
            "changeType", "added"
        ));
    }
}

/**
 * LangGraph workflow definition.
 */
public interface LangGraphWorkflow {
    
    /**
     * Execute workflow with initial state.
     */
    Mono<Void> execute(Map<String, Object> initialState);
    
    /**
     * Builder for workflows.
     */
    static WorkflowBuilder builder() {
        return new WorkflowBuilder();
    }
    
    class WorkflowBuilder {
        private Map<String, AIAgent> agents = new HashMap<>();
        private Map<String, Set<String>> edges = new HashMap<>();
        
        public WorkflowBuilder addNode(String name, AIAgent agent) {
            agents.put(name, agent);
            return this;
        }
        
        public WorkflowBuilder addEdge(String from, String to) {
            edges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
            return this;
        }
        
        public WorkflowBuilder addConditionalEdge(
                String from, 
                Function<WorkflowState, String> condition) {
            // Implementation
            return this;
        }
        
        public LangGraphWorkflow build() {
            return new DefaultLangGraphWorkflow(agents, edges);
        }
    }
}

/**
 * Example: Order processing workflow.
 */
class OrderProcessingWorkflow {
    
    public static LangGraphWorkflow create() {
        return LangGraphWorkflow.builder()
            .addNode("analyzer", new OrderAnalyzerAgent())
            .addNode("prioritizer", new PriorityAgent())
            .addNode("router", new RoutingAgent())
            .addNode("notifier", new NotificationAgent())
            
            .addEdge("analyzer", "prioritizer")
            .addConditionalEdge("prioritizer", state -> {
                int priority = (int) state.get("priority");
                return priority > 5 ? "urgent_router" : "normal_router";
            })
            .addEdge("router", "notifier")
            
            .build();
    }
}
```

---

## 8. Configuration Schema

### 8.1 YAML Configuration Format

```yaml
apiVersion: v1
kind: Reaction
name: order-notification
spec:
  # Reaction type
  kind: http
  
  # Queries to subscribe to
  queries:
    # Simple subscription
    ready-orders: {}
    
    # With per-query config
    urgent-orders:
      priority: high
      batchSize: 10
  
  # Execution configuration
  execution:
    mode: async          # sync, async, batch
    batchSize: 100       # For batch mode
    batchWindowMs: 1000  # For batch mode
    throttleRateMs: 100  # Min time between executions
    maxConcurrency: 10   # Max parallel executions
  
  # Retry configuration
  retry:
    enabled: true
    maxAttempts: 3
    initialBackoffMs: 1000
    maxBackoffMs: 30000
    backoffMultiplier: 2.0
    retryableExceptions:
      - java.io.IOException
      - java.util.concurrent.TimeoutException
  
  # Security (credentials)
  security:
    secrets:
      apiKey:
        kind: Secret
        name: webhook-credentials
        key: api-key
      # OR
      # kind: EnvironmentVariable
      # name: WEBHOOK_API_KEY
      # OR
      # kind: InlineValue
      # value: "my-secret-key"
  
  # Reaction-specific properties
  properties:
    webhookUrl: https://api.example.com/orders
    method: POST
    headers:
      Content-Type: application/json
      Authorization: Bearer ${security.secrets.apiKey}
```

### 8.2 HTTP Webhook Reaction Config

```yaml
apiVersion: v1
kind: Reaction
name: webhook-orders
spec:
  kind: http
  queries:
    ready-orders: {}
  properties:
    webhookUrl: https://api.example.com/webhook
    method: POST  # GET, POST, PUT, PATCH, DELETE
    headers:
      Content-Type: application/json
      X-Custom-Header: value
    timeout: 30000  # milliseconds
```

### 8.3 Kafka Reaction Config

```yaml
apiVersion: v1
kind: Reaction
name: kafka-orders
spec:
  kind: kafka
  queries:
    ready-orders: {}
  properties:
    bootstrapServers: kafka:9092
    topic: order-events
    keyField: orderId  # Field to use as Kafka key
    # Kafka producer config
    producerConfig:
      acks: all
      retries: 3
      compression.type: gzip
```

### 8.4 LangChain4j AI Agent Config

```yaml
apiVersion: v1
kind: Reaction
name: ai-dispatcher
spec:
  kind: langchain4j
  queries:
    ready-orders: {}
  properties:
    modelProvider: openai
    modelName: gpt-4
    temperature: 0.7
    maxTokens: 1000
    systemPrompt: |
      You are an intelligent order dispatcher.
      Analyze orders and assign optimal drivers.
    userPromptTemplate: |
      New order ready:
      Order ID: {orderId}
      Customer: {customer}
      Location: {location}
      
      Assign the best available driver.
  security:
    secrets:
      apiKey:
        kind: Secret
        name: openai-credentials
        key: api-key
```

### 8.5 Debug Reaction Config

```yaml
apiVersion: v1
kind: Reaction
name: debug-logger
spec:
  kind: debug
  queries:
    ready-orders: {}
    urgent-orders: {}
  properties:
    verbose: true  # Log full result data
    logLevel: INFO  # DEBUG, INFO, WARN, ERROR
```

---

## 9. API Specifications

### 9.1 Reaction Management API

```java
package io.drasi.api;

import io.drasi.core.reaction.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST API for managing reactions.
 */
@RestController
@RequestMapping("/api/v1/reactions")
public class ReactionController {
    
    private final ReactionRegistry reactionRegistry;
    
    /**
     * Create a new reaction.
     * POST /api/v1/reactions
     */
    @PostMapping
    public Mono<ReactionResponse> createReaction(
            @RequestBody ReactionConfig config) {
        return reactionRegistry.create(config)
            .map(this::toResponse);
    }
    
    /**
     * Get reaction by ID.
     * GET /api/v1/reactions/{reactionId}
     */
    @GetMapping("/{reactionId}")
    public Mono<ReactionResponse> getReaction(
            @PathVariable String reactionId) {
        return reactionRegistry.get(reactionId)
            .map(this::toResponse);
    }
    
    /**
     * List all reactions.
     * GET /api/v1/reactions
     */
    @GetMapping
    public Flux<ReactionResponse> listReactions() {
        return reactionRegistry.list()
            .map(this::toResponse);
    }
    
    /**
     * Delete a reaction.
     * DELETE /api/v1/reactions/{reactionId}
     */
    @DeleteMapping("/{reactionId}")
    public Mono<Void> deleteReaction(
            @PathVariable String reactionId) {
        return reactionRegistry.delete(reactionId);
    }
    
    /**
     * Get reaction health.
     * GET /api/v1/reactions/{reactionId}/health
     */
    @GetMapping("/{reactionId}/health")
    public Mono<ReactionHealth> getHealth(
            @PathVariable String reactionId) {
        return reactionRegistry.get(reactionId)
            .map(Reaction::health);
    }
    
    /**
     * Get reaction statistics.
     * GET /api/v1/reactions/{reactionId}/stats
     */
    @GetMapping("/{reactionId}/stats")
    public Mono<ReactionStats> getStats(
            @PathVariable String reactionId) {
        return reactionRegistry.get(reactionId)
            .map(Reaction::stats);
    }
    
    /**
     * List available reaction providers.
     * GET /api/v1/reactions/providers
     */
    @GetMapping("/providers")
    public Flux<ProviderInfo> listProviders() {
        return Flux.fromIterable(
            reactionRegistry.getProviders().values()
        )
        .map(provider -> new ProviderInfo(
            provider.kind(),
            provider.configSchema()
        ));
    }
    
    private ReactionResponse toResponse(Reaction reaction) {
        return new ReactionResponse(
            reaction.id(),
            reaction.name(),
            reaction.kind(),
            reaction.queryIds(),
            reaction.isRunning(),
            reaction.health(),
            reaction.stats()
        );
    }
}

record ReactionResponse(
    String id,
    String name,
    String kind,
    Set<String> queryIds,
    boolean running,
    ReactionHealth health,
    ReactionStats stats
) {}

record ProviderInfo(
    String kind,
    ConfigSchema schema
) {}
```

### 9.2 Configuration Loader

```java
package io.drasi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.drasi.core.reaction.ReactionConfig;

/**
 * Load reaction configurations from YAML.
 */
public class ReactionConfigLoader {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    /**
     * Load from YAML file.
     */
    public ReactionConfig loadFromFile(String filePath) throws IOException {
        ReactionYaml yaml = yamlMapper.readValue(
            new File(filePath),
            ReactionYaml.class
        );
        return convertToConfig(yaml);
    }
    
    /**
     * Load from YAML string.
     */
    public ReactionConfig loadFromString(String yamlContent) throws IOException {
        ReactionYaml yaml = yamlMapper.readValue(
            yamlContent,
            ReactionYaml.class
        );
        return convertToConfig(yaml);
    }
    
    private ReactionConfig convertToConfig(ReactionYaml yaml) {
        // Convert YAML to ReactionConfig
        return ReactionConfig.builder()
            .id(yaml.name)
            .name(yaml.name)
            .kind(yaml.spec.kind)
            .queryIds(yaml.spec.queries.keySet())
            // ... convert other fields
            .build();
    }
}

/**
 * YAML data structure.
 */
class ReactionYaml {
    public String apiVersion;
    public String kind;
    public String name;
    public ReactionSpecYaml spec;
    
    static class ReactionSpecYaml {
        public String kind;
        public Map<String, Object> queries;
        public Map<String, Object> properties;
        public ExecutionYaml execution;
        public RetryYaml retry;
        public SecurityYaml security;
    }
    
    // Nested classes for execution, retry, security...
}
```

---

## 10. Test Specifications

### 10.1 Unit Tests

```java
package io.drasi.core.reaction;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ReactionTest {
    
    @Test
    void shouldProcessAddedResult() {
        // Given: HTTP webhook reaction
        HttpWebhookReaction reaction = new HttpWebhookReaction(
            ReactionConfig.builder()
                .id("test-reaction")
                .kind("http")
                .addQueryId("test-query")
                .property("webhookUrl", "http://localhost:8080/webhook")
                .build()
        );
        
        QueryResult result = new QueryResult(
            "test-query",
            "result-1",
            Map.of("orderId", 123, "status", "READY"),
            1,
            Instant.now()
        );
        
        // When: Process ADDED change
        ResultChange change = new ResultChange(
            "test-query",
            ResultChange.ChangeType.ADDED,
            null,
            result,
            Instant.now(),
            null
        );
        
        // Then: Should call webhook
        StepVerifier.create(reaction.processChange(change))
            .verifyComplete();
        
        // Verify stats updated
        assertThat(reaction.stats().addedCount()).isEqualTo(1);
        assertThat(reaction.stats().totalProcessed()).isEqualTo(1);
    }
    
    @Test
    void shouldRetryOnFailure() {
        // Given: Reaction with retry config
        // When: Processing fails with retryable exception
        // Then: Should retry according to config
    }
    
    @Test
    void shouldBatchChanges() {
        // Given: Reaction in batch mode
        // When: Multiple changes arrive
        // Then: Should collect and process as batch
    }
    
    @Test
    void shouldThrottleExecution() {
        // Given: Reaction with throttle rate
        // When: Changes arrive rapidly
        // Then: Should respect throttle rate
    }
}
```

### 10.2 Integration Tests

```java
package io.drasi.integration;

@SpringBootTest
class ReactionIntegrationTest {
    
    @Test
    void endToEndHttpWebhookFlow() {
        // Given: Continuous query and HTTP reaction
        // When: Query result changes
        // Then: Should call webhook with correct payload
    }
    
    @Test
    void multiQueryReactionProcessing() {
        // Given: Reaction subscribed to multiple queries
        // When: Changes from different queries
        // Then: Should process all correctly
    }
    
    @Test
    void aiAgentReactionWithLangChain4j() {
        // Given: AI agent reaction
        // When: Result added
        // Then: Should invoke AI model and execute action
    }
}
```

---

## 11. Implementation Checklist

### Phase 1: Core Framework (Week 1-2)
- [ ] Create Reaction interface and data models
- [ ] Implement AbstractReaction base class
- [ ] Create ReactionConfig and builders
- [ ] Implement ReactionProvider SPI
- [ ] Create ReactionRegistry
- [ ] Write unit tests for core models

### Phase 2: Dispatcher & Executor (Week 3-4)
- [ ] Implement QueryChangeDispatcher
- [ ] Add subscription management
- [ ] Create retry logic
- [ ] Add batching support
- [ ] Implement throttling
- [ ] Write dispatcher tests

### Phase 3: Built-in Reactions (Week 5-7)
- [ ] Implement HttpWebhookReaction
- [ ] Implement KafkaReaction
- [ ] Implement DebugReaction
- [ ] Create EmailReaction
- [ ] Create DatabaseReaction (MongoDB, PostgreSQL)
- [ ] Write reaction provider tests

### Phase 4: AI Agent Integration (Week 8-10)
- [ ] Implement LangChain4jReaction
- [ ] Create Spring AI reaction
- [ ] Implement LangGraphWorkflow reaction
- [ ] Add RAG-enhanced reactions
- [ ] Write AI agent tests

### Phase 5: Configuration & API (Week 11-12)
- [ ] Create YAML configuration loader
- [ ] Implement REST API controllers
- [ ] Add configuration validation
- [ ] Create CLI integration
- [ ] Write configuration tests

### Phase 6: Spring Boot Integration (Week 13-14)
- [ ] Create auto-configuration
- [ ] Add health indicators
- [ ] Implement metrics collection
- [ ] Create Spring Boot starter
- [ ] Write integration tests

### Phase 7: Security & Secrets (Week 15)
- [ ] Implement Kubernetes secret support
- [ ] Add environment variable support
- [ ] Create credential management
- [ ] Add encryption for sensitive data
- [ ] Write security tests

### Phase 8: Advanced Features (Week 16-17)
- [ ] Add conditional execution
- [ ] Implement result filtering
- [ ] Create transformation pipelines
- [ ] Add error recovery strategies
- [ ] Write advanced feature tests

### Phase 9: Documentation & Examples (Week 18)
- [ ] Write API documentation
- [ ] Create usage examples
- [ ] Add configuration guides
- [ ] Create tutorial scenarios
- [ ] Document best practices

### Phase 10: Testing & Polish (Week 19-20)
- [ ] Performance testing
- [ ] Load testing
- [ ] Security testing
- [ ] Documentation review
- [ ] Final polish

---

## 12. Example Usage

### 12.1 Programmatic API

```java
// Create HTTP webhook reaction
Reaction httpReaction = HttpWebhookReaction.builder()
    .id("order-webhook")
    .addQueryId("ready-orders")
    .webhookUrl("https://api.example.com/orders")
    .method("POST")
    .header("Authorization", "Bearer token123")
    .build();

// Create AI agent reaction
Reaction aiReaction = LangChain4jReaction.builder()
    .id("order-dispatcher")
    .addQueryId("ready-orders")
    .model(chatModel)
    .systemPrompt("You are an order dispatcher")
    .userPromptTemplate("""
        New order: {orderId}
        Customer: {customer}
        Assign best driver.
        """)
    .onResponse(aiResponse -> {
        String driverId = extractDriverId(aiResponse);
        return assignDriver(driverId);
    })
    .buildAI();

// Register reactions
reactionRegistry.create(httpReaction);
reactionRegistry.create(aiReaction);
```

### 12.2 Spring Boot Configuration

```java
@Configuration
@EnableDrasi
public class ReactionConfiguration {
    
    @Bean
    public Reaction orderWebhook() {
        return HttpWebhookReaction.builder()
            .id("order-webhook")
            .addQueryId("ready-orders")
            .fromYaml("""
                kind: http
                properties:
                  webhookUrl: ${webhook.url}
                  method: POST
                """)
            .build();
    }
    
    @Bean
    public Reaction aiDispatcher(ChatLanguageModel model) {
        return LangChain4jReaction.builder()
            .id("ai-dispatcher")
            .addQueryId("ready-orders")
            .model(model)
            .buildAI();
    }
}
```

---

**END OF SPECIFICATION**

