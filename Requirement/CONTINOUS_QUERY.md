# Continuous Query Implementation Specification for Drasi-Java

**Document Version:** 1.0  
**Target Implementation:** Windsurf IDE  
**Language:** Java 21+  
**Framework:** Spring Boot 3.x  
**Based on:** https://drasi.io/concepts/continuous-queries/  

---

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Architecture](#architecture)
4. [Data Models](#data-models)
5. [Component Specifications](#component-specifications)
6. [Query Language Implementation](#query-language-implementation)
7. [Incremental Processing Algorithm](#incremental-processing-algorithm)
8. [API Specifications](#api-specifications)
9. [Configuration Schema](#configuration-schema)
10. [Test Specifications](#test-specifications)
11. [Implementation Checklist](#implementation-checklist)

---

## 1. Overview

### 1.1 Purpose
Implement a **Continuous Query Engine** in pure Java that maintains perpetually accurate query results by processing data changes incrementally, emitting precise change notifications (added/updated/deleted).

### 1.2 Key Requirements
- ✅ Support Cypher and GQL query languages
- ✅ Maintain accurate query results continuously
- ✅ Detect and emit precise result changes (added/updated/deleted)
- ✅ Subscribe to multiple sources
- ✅ Support cross-source joins without explicit join syntax
- ✅ Process source changes incrementally (not re-execute entire query)
- ✅ Support both query mode (detailed changes) and filter mode (simplified)
- ✅ Enable middleware pipelines for change processing
- ✅ Provide parameter support for query reusability
- ✅ Support result view caching with retention policies

---

## 2. Core Concepts

### 2.1 Continuous Query vs Instantaneous Query

**Instantaneous Query (Traditional):**
```java
// Runs once, returns snapshot at point in time
List<Order> results = database.query("SELECT * FROM orders WHERE status = 'READY'");
// Results become stale immediately as data changes
```

**Continuous Query (Drasi):**
```java
// Starts once, maintains accurate results continuously
ContinuousQuery query = ContinuousQuery.builder()
    .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
    .build();

// Emits changes as they occur:
// - ResultAdded: When new order becomes READY
// - ResultUpdated: When READY order properties change
// - ResultRemoved: When order no longer READY
```

### 2.2 Change Detection Flow

```
Source Change → Continuous Query → Result Change → Reactions
     ↓                ↓                   ↓              ↓
  INSERT          Evaluate           ADDED         Trigger
  UPDATE         Incremental        UPDATED        Actions
  DELETE          Update            REMOVED
```

### 2.3 Result Change Types

1. **Added**: New result appears that matches query criteria
2. **Updated**: Existing result properties change but still matches
3. **Deleted**: Result no longer matches query criteria

---

## 3. Architecture

### 3.1 System Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                   Continuous Query Engine                       │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌─────────────────┐ │
│  │Query Parser  │───▶│Query Planner │───▶│ Query Executor  │ │
│  │(ANTLR/Cypher)│    │(Graph-based) │    │  (Incremental)  │ │
│  └──────────────┘    └──────────────┘    └─────────────────┘ │
│                                                   │             │
│  ┌─────────────────────────────────────────────────┐          │
│  │          Source Subscription Manager            │          │
│  │  - Subscribe to multiple sources                │          │
│  │  - Map source labels to query labels            │          │
│  │  - Manage middleware pipelines                  │          │
│  └─────────────────────────────────────────────────┘          │
│                           │                                     │
│  ┌─────────────────────────────────────────────────┐          │
│  │         Incremental Update Processor             │          │
│  │  - Receive source change events                  │          │
│  │  - Apply middleware transformations              │          │
│  │  - Identify affected query patterns              │          │
│  │  - Compute result deltas                         │          │
│  │  - Emit result changes                           │          │
│  └─────────────────────────────────────────────────┘          │
│                           │                                     │
│  ┌─────────────────────────────────────────────────┐          │
│  │              Result Set Manager                  │          │
│  │  - Cache current results (in-memory/Redis)       │          │
│  │  - Index results for fast lookup                 │          │
│  │  - Manage retention policies                     │          │
│  │  - Support point-in-time queries                 │          │
│  └─────────────────────────────────────────────────┘          │
│                           │                                     │
│  ┌─────────────────────────────────────────────────┐          │
│  │              Graph Storage Layer                 │          │
│  │  - Store nodes and relationships                 │          │
│  │  - Support cross-source joins                    │          │
│  │  - Index by label and properties                 │          │
│  │  - Neo4j Embedded or custom graph store          │          │
│  └─────────────────────────────────────────────────┘          │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
   ┌─────────┐      ┌──────────┐      ┌──────────┐
   │Source 1 │      │Source 2  │      │Source 3  │
   │(Postgres│      │(MongoDB) │      │(Kafka)   │
   └─────────┘      └──────────┘      └──────────┘
```

### 3.2 Component Layers

1. **Query Definition Layer**: Parse and validate query
2. **Subscription Layer**: Connect to sources, apply middleware
3. **Processing Layer**: Incremental update computation
4. **Storage Layer**: Result caching and indexing
5. **Emission Layer**: Result change notifications

---

## 4. Data Models

### 4.1 ContinuousQuery Model

```java
package io.drasi.core.query;

import io.drasi.core.ChangeEvent;
import reactor.core.publisher.Flux;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Continuous Query that maintains accurate results
 * by processing source changes incrementally.
 */
public interface ContinuousQuery {
    
    /**
     * Unique identifier for this query.
     */
    String id();
    
    /**
     * Human-readable name.
     */
    String name();
    
    /**
     * Query mode: QUERY (detailed changes) or FILTER (simplified).
     */
    QueryMode mode();
    
    /**
     * Query language: Cypher or GQL.
     */
    QueryLanguage language();
    
    /**
     * The query string.
     */
    String query();
    
    /**
     * Source subscriptions.
     */
    Set<SourceSubscription> sources();
    
    /**
     * Cross-source join definitions.
     */
    Set<JoinDefinition> joins();
    
    /**
     * Middleware configurations.
     */
    List<MiddlewareConfig> middleware();
    
    /**
     * Query parameters.
     */
    Map<String, Object> parameters();
    
    /**
     * View configuration (caching, retention).
     */
    ViewConfig viewConfig();
    
    /**
     * Evaluate query against initial snapshot.
     * @param snapshot Initial data from sources
     * @return Initial result set
     */
    Flux<QueryResult> evaluateInitial(Flux<ChangeEvent> snapshot);
    
    /**
     * Process a source change event incrementally.
     * @param change Source change event
     * @return Result changes (added/updated/deleted)
     */
    Flux<ResultChange> processChange(ChangeEvent change);
    
    /**
     * Get current result set.
     */
    Flux<QueryResult> currentResults();
    
    /**
     * Get results as of a specific point in time (if view retention allows).
     */
    Flux<QueryResult> resultsAt(Instant timestamp);
    
    /**
     * Query statistics.
     */
    QueryStats stats();
    
    enum QueryMode {
        QUERY,   // Maintain full result set, emit detailed changes
        FILTER   // Simplified mode, only emit additions
    }
    
    enum QueryLanguage {
        CYPHER,
        GQL
    }
}
```

### 4.2 QueryResult Model

```java
package io.drasi.core.query;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single result row from a continuous query.
 */
public record QueryResult(
    String queryId,
    String resultId,  // Unique identifier for this result
    Map<String, Object> data,  // Result columns as defined in RETURN clause
    long version,     // Version number for this result
    Instant timestamp // When this result was created/updated
) {
    
    /**
     * Get a specific field value from the result.
     */
    public Object get(String fieldName) {
        return data.get(fieldName);
    }
    
    /**
     * Check if this result has a specific field.
     */
    public boolean hasField(String fieldName) {
        return data.containsKey(fieldName);
    }
}
```

### 4.3 ResultChange Model

```java
package io.drasi.core.query;

import java.time.Instant;

/**
 * Represents a change to the query result set.
 * Emitted when results are added, updated, or removed.
 */
public record ResultChange(
    String queryId,
    ChangeType type,
    QueryResult before,  // null for ADDED
    QueryResult after,   // null for DELETED
    Instant timestamp,
    SourceChangeInfo sourceChange  // Original source change that caused this
) {
    
    public enum ChangeType {
        ADDED,    // New result appeared
        UPDATED,  // Existing result changed
        DELETED   // Result no longer matches
    }
    
    /**
     * Get the result ID (from before or after depending on change type).
     */
    public String resultId() {
        return switch (type) {
            case ADDED -> after.resultId();
            case DELETED -> before.resultId();
            case UPDATED -> after.resultId(); // Should be same as before
        };
    }
    
    /**
     * Get changed fields (for UPDATE type).
     */
    public Map<String, Object> changedFields() {
        if (type != ChangeType.UPDATED || before == null || after == null) {
            return Map.of();
        }
        
        return after.data().entrySet().stream()
            .filter(entry -> !entry.getValue().equals(before.data().get(entry.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
```

### 4.4 SourceSubscription Model

```java
package io.drasi.core.query;

import java.util.List;
import java.util.Set;

/**
 * Defines how a continuous query subscribes to a source.
 */
public record SourceSubscription(
    String sourceId,
    Set<LabelMapping> nodeLabels,
    Set<LabelMapping> relationLabels,
    List<String> middlewarePipeline  // Ordered list of middleware names
) {
    
    /**
     * Mapping between source label and query label.
     */
    public record LabelMapping(
        String sourceLabel,      // Label name in source data
        String queryLabel,       // Label name used in query (optional, defaults to sourceLabel)
        boolean suppressIndex    // Don't cache/index this element type
    ) {
        public String effectiveQueryLabel() {
            return queryLabel != null ? queryLabel : sourceLabel;
        }
    }
}
```

### 4.5 JoinDefinition Model

```java
package io.drasi.core.query;

import java.util.List;

/**
 * Defines a cross-source join by matching properties.
 */
public record JoinDefinition(
    String joinId,
    List<JoinKey> keys
) {
    
    /**
     * A key participating in the join.
     */
    public record JoinKey(
        String label,     // Node label
        String property   // Property name to match
    ) {}
    
    /**
     * Validate that join has at least 2 keys.
     */
    public JoinDefinition {
        if (keys.size() < 2) {
            throw new IllegalArgumentException(
                "Join must have at least 2 keys, got: " + keys.size());
        }
    }
}
```

### 4.6 ViewConfig Model

```java
package io.drasi.core.query;

import java.time.Duration;

/**
 * Configuration for result view caching and retention.
 */
public record ViewConfig(
    boolean enabled,              // Enable result caching
    RetentionPolicy retentionPolicy
) {
    
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
        return new ViewConfig(true, new Latest());
    }
}
```

### 4.7 MiddlewareConfig Model

```java
package io.drasi.core.query.middleware;

import java.util.Map;

/**
 * Configuration for a middleware component.
 */
public record MiddlewareConfig(
    String name,
    String kind,  // Middleware type identifier
    Map<String, Object> properties
) {}

/**
 * Base interface for all middleware implementations.
 */
public interface Middleware {
    
    /**
     * Process a source change event.
     * Can transform, enrich, filter, or split the event.
     */
    Flux<ChangeEvent> process(ChangeEvent event);
    
    /**
     * Middleware initialization.
     */
    void initialize(Map<String, Object> config);
}
```

---

## 5. Component Specifications

### 5.1 ContinuousQueryBuilder

```java
package io.drasi.core.query;

import java.util.*;

/**
 * Builder for constructing ContinuousQuery instances.
 */
public class ContinuousQueryBuilder {
    
    private String id;
    private String name;
    private QueryMode mode = QueryMode.QUERY;
    private QueryLanguage language = QueryLanguage.CYPHER;
    private String query;
    private Set<SourceSubscription> sources = new HashSet<>();
    private Set<JoinDefinition> joins = new HashSet<>();
    private List<MiddlewareConfig> middleware = new ArrayList<>();
    private Map<String, Object> parameters = new HashMap<>();
    private ViewConfig viewConfig = ViewConfig.defaultConfig();
    private String container = "default";
    private String storageProfile = "redis";
    
    public ContinuousQueryBuilder id(String id) {
        this.id = id;
        return this;
    }
    
    public ContinuousQueryBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public ContinuousQueryBuilder mode(QueryMode mode) {
        this.mode = mode;
        return this;
    }
    
    public ContinuousQueryBuilder language(QueryLanguage language) {
        this.language = language;
        return this;
    }
    
    public ContinuousQueryBuilder query(String query) {
        this.query = query;
        return this;
    }
    
    public ContinuousQueryBuilder addSource(SourceSubscription source) {
        this.sources.add(source);
        return this;
    }
    
    public ContinuousQueryBuilder addJoin(JoinDefinition join) {
        this.joins.add(join);
        return this;
    }
    
    public ContinuousQueryBuilder addMiddleware(MiddlewareConfig middleware) {
        this.middleware.add(middleware);
        return this;
    }
    
    public ContinuousQueryBuilder parameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }
    
    public ContinuousQueryBuilder viewConfig(ViewConfig config) {
        this.viewConfig = config;
        return this;
    }
    
    public ContinuousQuery build() {
        // Validation
        Objects.requireNonNull(id, "Query ID is required");
        Objects.requireNonNull(query, "Query string is required");
        if (sources.isEmpty()) {
            throw new IllegalStateException("At least one source is required");
        }
        
        return new DefaultContinuousQuery(
            id, name, mode, language, query,
            sources, joins, middleware, parameters,
            viewConfig, container, storageProfile
        );
    }
}
```

### 5.2 Query Parser Interface

```java
package io.drasi.core.query.parser;

import io.drasi.core.query.*;

/**
 * Parses query strings into executable query plans.
 */
public interface QueryParser {
    
    /**
     * Parse a query string.
     * @param query Query text
     * @param language Query language
     * @param parameters Query parameters
     * @return Parsed query plan
     */
    QueryPlan parse(String query, QueryLanguage language, Map<String, Object> parameters);
    
    /**
     * Validate query syntax without full parsing.
     */
    ValidationResult validate(String query, QueryLanguage language);
}

/**
 * Executable query plan.
 */
public interface QueryPlan {
    
    /**
     * Get all patterns in this query.
     */
    List<Pattern> patterns();
    
    /**
     * Get WHERE clause predicates.
     */
    List<Predicate> predicates();
    
    /**
     * Get RETURN clause projections.
     */
    List<Projection> projections();
    
    /**
     * Get ORDER BY clause.
     */
    Optional<OrderBy> orderBy();
    
    /**
     * Get LIMIT clause.
     */
    Optional<Integer> limit();
    
    /**
     * Execute plan against initial data.
     */
    Flux<QueryResult> execute(GraphStore store);
    
    /**
     * Identify patterns affected by a change event.
     */
    Set<Pattern> affectedPatterns(ChangeEvent change);
}
```

### 5.3 Incremental Update Processor

```java
package io.drasi.core.query.processor;

import io.drasi.core.ChangeEvent;
import io.drasi.core.query.*;
import reactor.core.publisher.Flux;

/**
 * Processes source changes incrementally to compute result changes.
 */
public interface IncrementalUpdateProcessor {
    
    /**
     * Process a source change and compute result changes.
     * 
     * Algorithm:
     * 1. Apply middleware transformations
     * 2. Identify affected query patterns
     * 3. For each pattern:
     *    a. Find candidate results that might be impacted
     *    b. Re-evaluate only those candidates
     *    c. Compute delta (added/updated/deleted)
     * 4. Emit result changes
     * 
     * @param query The continuous query
     * @param change Source change event
     * @return Result changes
     */
    Flux<ResultChange> processChange(ContinuousQuery query, ChangeEvent change);
}

/**
 * Default implementation of incremental update processing.
 */
public class DefaultIncrementalUpdateProcessor implements IncrementalUpdateProcessor {
    
    private final GraphStore graphStore;
    private final ResultSetCache resultCache;
    private final MiddlewareExecutor middlewareExecutor;
    
    @Override
    public Flux<ResultChange> processChange(ContinuousQuery query, ChangeEvent change) {
        return Flux.defer(() -> {
            // 1. Apply middleware pipeline
            Flux<ChangeEvent> transformed = middlewareExecutor.execute(
                query.middleware(), change);
            
            // 2. For each transformed event
            return transformed.flatMap(event -> {
                // 3. Update graph store
                graphStore.applyChange(event);
                
                // 4. Identify affected patterns
                QueryPlan plan = parseQuery(query);
                Set<Pattern> affectedPatterns = plan.affectedPatterns(event);
                
                // 5. Compute result changes
                return computeResultChanges(query, plan, affectedPatterns, event);
            });
        });
    }
    
    private Flux<ResultChange> computeResultChanges(
            ContinuousQuery query,
            QueryPlan plan,
            Set<Pattern> affectedPatterns,
            ChangeEvent change) {
        
        List<ResultChange> changes = new ArrayList<>();
        
        for (Pattern pattern : affectedPatterns) {
            // Find existing results that might be affected
            Set<String> candidateResultIds = findCandidateResults(pattern, change);
            
            for (String resultId : candidateResultIds) {
                QueryResult oldResult = resultCache.get(resultId);
                QueryResult newResult = reevaluateResult(plan, resultId, change);
                
                // Determine change type
                if (oldResult == null && newResult != null) {
                    // Result added
                    changes.add(new ResultChange(
                        query.id(),
                        ResultChange.ChangeType.ADDED,
                        null,
                        newResult,
                        Instant.now(),
                        toSourceChangeInfo(change)
                    ));
                    resultCache.put(resultId, newResult);
                    
                } else if (oldResult != null && newResult == null) {
                    // Result deleted
                    changes.add(new ResultChange(
                        query.id(),
                        ResultChange.ChangeType.DELETED,
                        oldResult,
                        null,
                        Instant.now(),
                        toSourceChangeInfo(change)
                    ));
                    resultCache.remove(resultId);
                    
                } else if (oldResult != null && newResult != null && 
                           !oldResult.equals(newResult)) {
                    // Result updated
                    changes.add(new ResultChange(
                        query.id(),
                        ResultChange.ChangeType.UPDATED,
                        oldResult,
                        newResult,
                        Instant.now(),
                        toSourceChangeInfo(change)
                    ));
                    resultCache.put(resultId, newResult);
                }
            }
        }
        
        return Flux.fromIterable(changes);
    }
    
    private Set<String> findCandidateResults(Pattern pattern, ChangeEvent change) {
        // Use index to quickly find results that reference the changed entity
        return resultCache.findByEntity(change.entityType(), change.entityId());
    }
    
    private QueryResult reevaluateResult(
            QueryPlan plan, 
            String resultId, 
            ChangeEvent change) {
        // Re-execute query for just this result
        // Uses indexed graph store for efficient lookup
        return plan.evaluateForResult(resultId, graphStore);
    }
}
```

### 5.4 Result Set Cache

```java
package io.drasi.core.query.cache;

import io.drasi.core.query.QueryResult;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Cache for query results with indexing capabilities.
 */
public interface ResultSetCache {
    
    /**
     * Store a result.
     */
    void put(String resultId, QueryResult result);
    
    /**
     * Retrieve a result.
     */
    Optional<QueryResult> get(String resultId);
    
    /**
     * Remove a result.
     */
    void remove(String resultId);
    
    /**
     * Find all result IDs for a specific query.
     */
    Set<String> findByQuery(String queryId);
    
    /**
     * Find results that reference a specific entity.
     * Used to quickly identify results affected by entity changes.
     */
    Set<String> findByEntity(String entityType, Object entityId);
    
    /**
     * Find results by field value.
     */
    Set<String> findByFieldValue(String fieldName, Object value);
    
    /**
     * Get results as of a specific point in time.
     * Requires appropriate retention policy.
     */
    Set<QueryResult> getResultsAt(String queryId, Instant timestamp);
    
    /**
     * Clear all results for a query.
     */
    void clearQuery(String queryId);
    
    /**
     * Get cache statistics.
     */
    CacheStats stats();
}

/**
 * Redis-backed implementation of result set cache.
 */
public class RedisResultSetCache implements ResultSetCache {
    
    private final RedisTemplate<String, QueryResult> redisTemplate;
    
    // Index structures in Redis:
    // - query:{queryId}:results -> Set of result IDs
    // - result:{resultId} -> QueryResult object
    // - entity:{entityType}:{entityId}:results -> Set of result IDs
    // - field:{fieldName}:{value}:results -> Set of result IDs
    // - result:{resultId}:history -> Sorted Set (timestamp -> QueryResult)
    
    @Override
    public void put(String resultId, QueryResult result) {
        String queryId = result.queryId();
        
        // Store result object
        redisTemplate.opsForValue().set("result:" + resultId, result);
        
        // Add to query index
        redisTemplate.opsForSet().add("query:" + queryId + ":results", resultId);
        
        // Add to entity indexes (extract entities from result data)
        indexByEntities(result);
        
        // Add to field indexes
        indexByFields(result);
        
        // Store in history if retention policy requires it
        storeHistory(result);
    }
    
    // Implementation details...
}
```

### 5.5 Graph Store

```java
package io.drasi.core.query.graph;

import io.drasi.core.ChangeEvent;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Graph storage for nodes and relationships across sources.
 * Supports cross-source joins via synthetic relationships.
 */
public interface GraphStore {
    
    /**
     * Apply a change event to the graph.
     */
    void applyChange(ChangeEvent change);
    
    /**
     * Get a node by ID.
     */
    Optional<Node> getNode(Object nodeId);
    
    /**
     * Get nodes by label.
     */
    Set<Node> getNodesByLabel(String label);
    
    /**
     * Get nodes by label and property value.
     */
    Set<Node> getNodesByProperty(String label, String property, Object value);
    
    /**
     * Get all relationships of a specific type.
     */
    Set<Relationship> getRelationshipsByType(String type);
    
    /**
     * Get relationships connected to a node.
     */
    Set<Relationship> getNodeRelationships(Object nodeId);
    
    /**
     * Execute a graph pattern match.
     */
    List<PatternMatch> match(Pattern pattern);
    
    /**
     * Create synthetic relationships for cross-source joins.
     */
    void createJoinRelationships(JoinDefinition join);
    
    /**
     * Clear all data for a specific source.
     */
    void clearSource(String sourceId);
}

/**
 * Node in the graph.
 */
public record Node(
    Object id,
    Set<String> labels,
    Map<String, Object> properties,
    String sourceId
) {}

/**
 * Relationship in the graph.
 */
public record Relationship(
    Object id,
    String type,
    Object startNodeId,
    Object endNodeId,
    Map<String, Object> properties,
    String sourceId,
    boolean synthetic  // true if created by join
) {}
```

---

## 6. Query Language Implementation

### 6.1 Cypher Grammar (ANTLR4)

```antlr4
grammar DrasiCypher;

// Parser rules
query
    : matchClause whereClause? returnClause orderClause? limitClause? EOF
    ;

matchClause
    : MATCH pattern (',' pattern)*
    ;

pattern
    : nodePattern (relationshipPattern nodePattern)*
    ;

nodePattern
    : '(' variable? labelExpression? properties? ')'
    ;

relationshipPattern
    : '-[' variable? labelExpression? properties? ']->'
    | '<-[' variable? labelExpression? properties? ']-'
    | '-[' variable? labelExpression? properties? ']-'
    ;

labelExpression
    : ':' labelName ('|' labelName)*
    ;

properties
    : '{' propertyKeyValue (',' propertyKeyValue)* '}'
    ;

propertyKeyValue
    : propertyKey ':' expression
    ;

whereClause
    : WHERE expression
    ;

returnClause
    : RETURN DISTINCT? returnItem (',' returnItem)*
    ;

returnItem
    : expression (AS variable)?
    ;

orderClause
    : ORDER BY sortItem (',' sortItem)*
    ;

sortItem
    : expression (ASC | DESC)?
    ;

limitClause
    : LIMIT INTEGER_LITERAL
    ;

expression
    : literal                                           # literalExpression
    | variable                                          # variableExpression
    | variable '.' propertyKey                          # propertyExpression
    | functionName '(' expression? (',' expression)* ')' # functionExpression
    | expression op=('*'|'/') expression                # multiplicativeExpression
    | expression op=('+'|'-') expression                # additiveExpression
    | expression op=('='|'<>'|'<'|'<='|'>'|'>=') expression # comparisonExpression
    | expression AND expression                         # andExpression
    | expression OR expression                          # orExpression
    | NOT expression                                    # notExpression
    | '(' expression ')'                                # parenthesizedExpression
    | expression IN expression                          # inExpression
    | expression IS NULL                                # isNullExpression
    | expression IS NOT NULL                            # isNotNullExpression
    ;

// Lexer rules
MATCH : 'MATCH' ;
WHERE : 'WHERE' ;
RETURN : 'RETURN' ;
DISTINCT : 'DISTINCT' ;
AS : 'AS' ;
ORDER : 'ORDER' ;
BY : 'BY' ;
ASC : 'ASC' ;
DESC : 'DESC' ;
LIMIT : 'LIMIT' ;
AND : 'AND' ;
OR : 'OR' ;
NOT : 'NOT' ;
IN : 'IN' ;
IS : 'IS' ;
NULL : 'NULL' ;

variable : IDENTIFIER ;
labelName : IDENTIFIER ;
propertyKey : IDENTIFIER ;
functionName : IDENTIFIER ;

literal
    : STRING_LITERAL
    | INTEGER_LITERAL
    | FLOAT_LITERAL
    | BOOLEAN_LITERAL
    ;

IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* ;
STRING_LITERAL : '\'' (~['])* '\'' ;
INTEGER_LITERAL : [0-9]+ ;
FLOAT_LITERAL : [0-9]+ '.' [0-9]+ ;
BOOLEAN_LITERAL : 'true' | 'false' ;

WS : [ \t\r\n]+ -> skip ;
```

### 6.2 Query Parser Implementation

```java
package io.drasi.core.query.parser.cypher;

import io.drasi.core.query.parser.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

/**
 * Cypher query parser implementation using ANTLR4.
 */
public class CypherQueryParser implements QueryParser {
    
    @Override
    public QueryPlan parse(String query, QueryLanguage language, Map<String, Object> parameters) {
        if (language != QueryLanguage.CYPHER) {
            throw new IllegalArgumentException("This parser only supports Cypher");
        }
        
        // Create ANTLR lexer and parser
        CharStream input = CharStreams.fromString(query);
        DrasiCypherLexer lexer = new DrasiCypherLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DrasiCypherParser parser = new DrasiCypherParser(tokens);
        
        // Parse query
        ParseTree tree = parser.query();
        
        // Build query plan using visitor
        CypherQueryPlanBuilder builder = new CypherQueryPlanBuilder(parameters);
        return builder.visit(tree);
    }
    
    @Override
    public ValidationResult validate(String query, QueryLanguage language) {
        try {
            parse(query, language, Map.of());
            return ValidationResult.valid();
        } catch (Exception e) {
            return ValidationResult.invalid(e.getMessage());
        }
    }
}

/**
 * Builds query plan from ANTLR parse tree.
 */
class CypherQueryPlanBuilder extends DrasiCypherBaseVisitor<QueryPlan> {
    
    private final Map<String, Object> parameters;
    private List<Pattern> patterns = new ArrayList<>();
    private List<Predicate> predicates = new ArrayList<>();
    private List<Projection> projections = new ArrayList<>();
    private Optional<OrderBy> orderBy = Optional.empty();
    private Optional<Integer> limit = Optional.empty();
    
    public CypherQueryPlanBuilder(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    @Override
    public QueryPlan visitQuery(DrasiCypherParser.QueryContext ctx) {
        // Visit match clause
        visit(ctx.matchClause());
        
        // Visit where clause
        if (ctx.whereClause() != null) {
            visit(ctx.whereClause());
        }
        
        // Visit return clause
        visit(ctx.returnClause());
        
        // Visit order clause
        if (ctx.orderClause() != null) {
            visit(ctx.orderClause());
        }
        
        // Visit limit clause
        if (ctx.limitClause() != null) {
            visit(ctx.limitClause());
        }
        
        return new DefaultQueryPlan(
            patterns, predicates, projections, orderBy, limit
        );
    }
    
    @Override
    public QueryPlan visitMatchClause(DrasiCypherParser.MatchClauseContext ctx) {
        for (var patternCtx : ctx.pattern()) {
            patterns.add(buildPattern(patternCtx));
        }
        return null;
    }
    
    private Pattern buildPattern(DrasiCypherParser.PatternContext ctx) {
        // Extract node patterns and relationship patterns
        // Build Pattern object
        // ...
    }
    
    // Additional visitor methods...
}
```

---

## 7. Incremental Processing Algorithm

### 7.1 Algorithm Overview

```java
/**
 * Incremental View Maintenance Algorithm
 * 
 * Given:
 * - Query Q
 * - Current result set R₀
 * - Source change Δ
 * 
 * Compute:
 * - New result set R₁ = Q(D₀ ⊕ Δ)
 * - Result delta ΔR = R₁ - R₀
 * 
 * Without re-executing entire query!
 */
public class IncrementalViewMaintenanceAlgorithm {
    
    /**
     * Core algorithm implementation.
     * 
     * Steps:
     * 1. Identify query patterns affected by the change
     * 2. For each affected pattern:
     *    a. Find existing results that reference changed entity
     *    b. Re-evaluate only those results
     *    c. Compare old vs new to determine change type
     * 3. Emit result changes (added/updated/deleted)
     */
    public Flux<ResultChange> process(
            ContinuousQuery query,
            ChangeEvent change,
            GraphStore store,
            ResultSetCache cache) {
        
        // 1. Update graph store with the change
        store.applyChange(change);
        
        // 2. Parse query to get patterns
        QueryPlan plan = parseQuery(query);
        
        // 3. Find patterns affected by this change
        Set<Pattern> affectedPatterns = plan.affectedPatterns(change);
        
        List<ResultChange> changes = new ArrayList<>();
        
        // 4. For each affected pattern
        for (Pattern pattern : affectedPatterns) {
            
            // 5. Handle based on change type
            switch (change.type()) {
                case INSERT -> {
                    // New entity might create new results
                    changes.addAll(handleInsert(pattern, change, plan, store, cache));
                }
                case UPDATE -> {
                    // Updated entity might affect existing results
                    changes.addAll(handleUpdate(pattern, change, plan, store, cache));
                }
                case DELETE -> {
                    // Deleted entity might remove results
                    changes.addAll(handleDelete(pattern, change, plan, store, cache));
                }
            }
        }
        
        return Flux.fromIterable(changes);
    }
    
    private List<ResultChange> handleInsert(
            Pattern pattern,
            ChangeEvent change,
            QueryPlan plan,
            GraphStore store,
            ResultSetCache cache) {
        
        List<ResultChange> changes = new ArrayList<>();
        
        // Check if new entity matches pattern criteria
        if (!pattern.matches(change.after(), store)) {
            return changes; // No match, no changes
        }
        
        // Find all combinations with other patterns
        List<PatternMatch> matches = expandToMatches(pattern, change.after(), plan, store);
        
        for (PatternMatch match : matches) {
            // Evaluate full query for this match
            Optional<QueryResult> result = evaluateMatch(match, plan, store);
            
            if (result.isPresent()) {
                QueryResult newResult = result.get();
                
                // This is a new result
                changes.add(new ResultChange(
                    plan.queryId(),
                    ResultChange.ChangeType.ADDED,
                    null,
                    newResult,
                    Instant.now(),
                    toSourceChangeInfo(change)
                ));
                
                // Cache the new result
                cache.put(newResult.resultId(), newResult);
            }
        }
        
        return changes;
    }
    
    private List<ResultChange> handleUpdate(
            Pattern pattern,
            ChangeEvent change,
            QueryPlan plan,
            GraphStore store,
            ResultSetCache cache) {
        
        List<ResultChange> changes = new ArrayList<>();
        
        boolean beforeMatched = pattern.matches(change.before(), store);
        boolean afterMatched = pattern.matches(change.after(), store);
        
        if (!beforeMatched && afterMatched) {
            // Entity now matches - treat as insert
            return handleInsert(pattern, change, plan, store, cache);
            
        } else if (beforeMatched && !afterMatched) {
            // Entity no longer matches - treat as delete
            return handleDelete(pattern, change, plan, store, cache);
            
        } else if (beforeMatched && afterMatched) {
            // Entity still matches but properties changed
            // Find all results that include this entity
            Set<String> affectedResultIds = cache.findByEntity(
                change.entityType(),
                change.entityId()
            );
            
            for (String resultId : affectedResultIds) {
                QueryResult oldResult = cache.get(resultId).orElse(null);
                if (oldResult == null) continue;
                
                // Re-evaluate this result
                QueryResult newResult = reevaluateResult(resultId, plan, store);
                
                if (newResult != null && !oldResult.equals(newResult)) {
                    // Result updated
                    changes.add(new ResultChange(
                        plan.queryId(),
                        ResultChange.ChangeType.UPDATED,
                        oldResult,
                        newResult,
                        Instant.now(),
                        toSourceChangeInfo(change)
                    ));
                    
                    cache.put(resultId, newResult);
                }
            }
        }
        
        return changes;
    }
    
    private List<ResultChange> handleDelete(
            Pattern pattern,
            ChangeEvent change,
            QueryPlan plan,
            GraphStore store,
            ResultSetCache cache) {
        
        List<ResultChange> changes = new ArrayList<>();
        
        // Find all results that reference the deleted entity
        Set<String> affectedResultIds = cache.findByEntity(
            change.entityType(),
            change.entityId()
        );
        
        for (String resultId : affectedResultIds) {
            QueryResult oldResult = cache.get(resultId).orElse(null);
            if (oldResult == null) continue;
            
            // Result no longer valid (entity deleted)
            changes.add(new ResultChange(
                plan.queryId(),
                ResultChange.ChangeType.DELETED,
                oldResult,
                null,
                Instant.now(),
                toSourceChangeInfo(change)
            ));
            
            cache.remove(resultId);
        }
        
        return changes;
    }
}
```

### 7.2 Pattern Matching

```java
package io.drasi.core.query.pattern;

import io.drasi.core.query.graph.*;
import java.util.Map;

/**
 * Represents a graph pattern from a MATCH clause.
 */
public interface Pattern {
    
    /**
     * Check if an entity matches this pattern.
     */
    boolean matches(Map<String, Object> entity, GraphStore store);
    
    /**
     * Get all matches for this pattern in the graph.
     */
    List<PatternMatch> findMatches(GraphStore store);
    
    /**
     * Check if a change affects this pattern.
     */
    boolean isAffectedBy(ChangeEvent change);
    
    /**
     * Get the labels involved in this pattern.
     */
    Set<String> labels();
    
    /**
     * Get the relationship types in this pattern.
     */
    Set<String> relationshipTypes();
}

/**
 * A concrete match of a pattern in the graph.
 */
public record PatternMatch(
    Pattern pattern,
    Map<String, Node> nodes,          // Variable name -> Node
    Map<String, Relationship> relationships  // Variable name -> Relationship
) {
    
    /**
     * Get a node by its variable name in the pattern.
     */
    public Node getNode(String variable) {
        return nodes.get(variable);
    }
    
    /**
     * Get a relationship by its variable name.
     */
    public Relationship getRelationship(String variable) {
        return relationships.get(variable);
    }
}
```

---

## 8. API Specifications

### 8.1 Query Management API

```java
package io.drasi.api;

import io.drasi.core.query.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST API for managing continuous queries.
 */
@RestController
@RequestMapping("/api/v1/queries")
public class QueryController {
    
    private final QueryRegistry queryRegistry;
    private final QueryExecutor queryExecutor;
    
    /**
     * Create a new continuous query.
     * POST /api/v1/queries
     */
    @PostMapping
    public Mono<QueryResponse> createQuery(@RequestBody QueryDefinition definition) {
        return queryRegistry.create(definition)
            .map(this::toResponse);
    }
    
    /**
     * Get query by ID.
     * GET /api/v1/queries/{queryId}
     */
    @GetMapping("/{queryId}")
    public Mono<QueryResponse> getQuery(@PathVariable String queryId) {
        return queryRegistry.get(queryId)
            .map(this::toResponse);
    }
    
    /**
     * List all queries.
     * GET /api/v1/queries
     */
    @GetMapping
    public Flux<QueryResponse> listQueries() {
        return queryRegistry.list()
            .map(this::toResponse);
    }
    
    /**
     * Delete a query.
     * DELETE /api/v1/queries/{queryId}
     */
    @DeleteMapping("/{queryId}")
    public Mono<Void> deleteQuery(@PathVariable String queryId) {
        return queryRegistry.delete(queryId);
    }
    
    /**
     * Get current results for a query.
     * GET /api/v1/queries/{queryId}/results
     */
    @GetMapping("/{queryId}/results")
    public Flux<QueryResult> getResults(@PathVariable String queryId) {
        return queryExecutor.getCurrentResults(queryId);
    }
    
    /**
     * Get results as of a specific time.
     * GET /api/v1/queries/{queryId}/results?timestamp={timestamp}
     */
    @GetMapping("/{queryId}/results")
    public Flux<QueryResult> getResultsAt(
            @PathVariable String queryId,
            @RequestParam Instant timestamp) {
        return queryExecutor.getResultsAt(queryId, timestamp);
    }
    
    /**
     * Subscribe to result changes.
     * GET /api/v1/queries/{queryId}/changes (Server-Sent Events)
     */
    @GetMapping(value = "/{queryId}/changes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResultChange> subscribeToChanges(@PathVariable String queryId) {
        return queryExecutor.subscribeToChanges(queryId);
    }
    
    /**
     * Get query statistics.
     * GET /api/v1/queries/{queryId}/stats
     */
    @GetMapping("/{queryId}/stats")
    public Mono<QueryStats> getStats(@PathVariable String queryId) {
        return queryRegistry.get(queryId)
            .map(ContinuousQuery::stats);
    }
}
```

### 8.2 WebSocket API for Real-time Changes

```java
package io.drasi.api;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for streaming query result changes.
 */
@Component
public class QueryResultWebSocketHandler extends TextWebSocketHandler {
    
    private final QueryExecutor queryExecutor;
    private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String queryId = extractQueryId(session);
        
        // Subscribe to query changes
        Disposable subscription = queryExecutor
            .subscribeToChanges(queryId)
            .subscribe(change -> {
                // Send change to client
                sendMessage(session, change);
            });
        
        subscriptions.put(session.getId(), subscription);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Disposable subscription = subscriptions.remove(session.getId());
        if (subscription != null) {
            subscription.dispose();
        }
    }
    
    private void sendMessage(WebSocketSession session, ResultChange change) {
        try {
            String json = objectMapper.writeValueAsString(change);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            // Handle error
        }
    }
}
```

---

## 9. Configuration Schema

### 9.1 YAML Configuration Format

```yaml
apiVersion: v1
kind: ContinuousQuery
name: incident-alerts
spec:
  # Query mode: query (default) or filter
  mode: query
  
  # Container to host the query
  container: default
  
  # Storage profile for caching
  storageProfile: redis
  
  # Query language: Cypher (default) or GQL
  queryLanguage: Cypher
  
  # Source subscriptions
  sources:
    subscriptions:
      # First source
      - id: human-resources
        nodes:
          - sourceLabel: Employee
            queryLabel: Employee    # Optional, defaults to sourceLabel
            suppressIndex: false    # Optional, defaults to false
          - sourceLabel: Team
          - sourceLabel: Building
          - sourceLabel: Region
          - sourceLabel: Incident
        relations:
          - sourceLabel: ASSIGNED_TO
          - sourceLabel: MANAGES
          - sourceLabel: LOCATED_IN
          - sourceLabel: OCCURS_IN
        pipeline:
          - enrichment-middleware
          - validation-middleware
      
      # Second source (if needed)
      - id: facilities
        nodes:
          - sourceLabel: Building
        relations:
          - sourceLabel: LOCATED_IN
    
    # Cross-source joins
    joins:
      - id: employee-building-join
        keys:
          - label: Employee
            property: building_id
          - label: Building
            property: id
    
    # Middleware definitions
    middleware:
      - name: enrichment-middleware
        kind: enrichment
        properties:
          enrichmentSource: external-api
          fields:
            - name: location
              apiUrl: https://api.example.com/locations/{id}
      
      - name: validation-middleware
        kind: validation
        properties:
          rules:
            - field: severity
              allowedValues: ['low', 'medium', 'high', 'critical', 'extreme']
  
  # View configuration
  view:
    enabled: true
    retentionPolicy:
      latest: {}  # Keep only latest version
      # OR
      # all: {}   # Keep all historical versions
      # OR
      # expire:
      #   afterSeconds: 86400  # Keep for 24 hours
  
  # Query parameters
  params:
    minSeverity: critical
    maxDistance: 10
  
  # The Cypher query
  query: |
    MATCH
      (e:Employee)-[:ASSIGNED_TO]->(t:Team),
      (m:Employee)-[:MANAGES]->(t:Team),
      (e:Employee)-[:LOCATED_IN]->(:Building)-[:LOCATED_IN]->(r:Region),
      (i:Incident {type:'environmental'})-[:OCCURS_IN]->(r:Region)
    WHERE
      elementId(e) <> elementId(m) 
      AND i.severity IN ['critical', 'extreme'] 
      AND i.endTimeMs IS NULL
    RETURN
      m.name AS ManagerName,
      m.email AS ManagerEmail,
      e.name AS EmployeeName,
      e.email AS EmployeeEmail,
      r.name AS RegionName,
      elementId(i) AS IncidentId,
      i.severity AS IncidentSeverity,
      i.description AS IncidentDescription
```

### 9.2 Configuration Loader

```java
package io.drasi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.drasi.core.query.*;

/**
 * Load continuous query definitions from YAML files.
 */
public class QueryConfigurationLoader {
    
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    /**
     * Load query definition from YAML file.
     */
    public ContinuousQuery loadFromFile(String filePath) throws IOException {
        QueryDefinitionYaml yaml = yamlMapper.readValue(
            new File(filePath),
            QueryDefinitionYaml.class
        );
        
        return convertToQuery(yaml);
    }
    
    /**
     * Load query definition from YAML string.
     */
    public ContinuousQuery loadFromString(String yamlContent) throws IOException {
        QueryDefinitionYaml yaml = yamlMapper.readValue(
            yamlContent,
            QueryDefinitionYaml.class
        );
        
        return convertToQuery(yaml);
    }
    
    private ContinuousQuery convertToQuery(QueryDefinitionYaml yaml) {
        ContinuousQueryBuilder builder = ContinuousQuery.builder()
            .id(yaml.name)
            .name(yaml.name)
            .query(yaml.spec.query);
        
        // Set mode
        if (yaml.spec.mode != null) {
            builder.mode(QueryMode.valueOf(yaml.spec.mode.toUpperCase()));
        }
        
        // Set language
        if (yaml.spec.queryLanguage != null) {
            builder.language(QueryLanguage.valueOf(yaml.spec.queryLanguage.toUpperCase()));
        }
        
        // Add sources
        if (yaml.spec.sources != null) {
            for (var sourceYaml : yaml.spec.sources.subscriptions) {
                builder.addSource(convertSource(sourceYaml));
            }
            
            // Add joins
            if (yaml.spec.sources.joins != null) {
                for (var joinYaml : yaml.spec.sources.joins) {
                    builder.addJoin(convertJoin(joinYaml));
                }
            }
            
            // Add middleware
            if (yaml.spec.sources.middleware != null) {
                for (var middlewareYaml : yaml.spec.sources.middleware) {
                    builder.addMiddleware(convertMiddleware(middlewareYaml));
                }
            }
        }
        
        // Add parameters
        if (yaml.spec.params != null) {
            yaml.spec.params.forEach(builder::parameter);
        }
        
        // Set view config
        if (yaml.spec.view != null) {
            builder.viewConfig(convertViewConfig(yaml.spec.view));
        }
        
        return builder.build();
    }
    
    // Conversion helper methods...
}

/**
 * YAML data structure matching configuration schema.
 */
class QueryDefinitionYaml {
    public String apiVersion;
    public String kind;
    public String name;
    public QuerySpecYaml spec;
    
    static class QuerySpecYaml {
        public String mode;
        public String container;
        public String storageProfile;
        public String queryLanguage;
        public SourcesYaml sources;
        public ViewYaml view;
        public Map<String, Object> params;
        public String query;
    }
    
    static class SourcesYaml {
        public List<SubscriptionYaml> subscriptions;
        public List<JoinYaml> joins;
        public List<MiddlewareYaml> middleware;
    }
    
    // Additional nested classes...
}
```

---

## 10. Test Specifications

### 10.1 Unit Tests

```java
package io.drasi.core.query;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * Unit tests for continuous query functionality.
 */
class ContinuousQueryTest {
    
    @Test
    void shouldEmitAddedResultWhenNewEntityMatchesQuery() {
        // Given: A query for orders with status 'READY'
        ContinuousQuery query = ContinuousQuery.builder()
            .id("ready-orders")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .addSource(testSource)
            .build();
        
        // When: New order with status 'READY' is inserted
        ChangeEvent insert = ChangeEvent.builder()
            .type(ChangeEvent.ChangeType.INSERT)
            .entityType("Order")
            .entityId(1)
            .after(Map.of("id", 1, "status", "READY", "customer", "John"))
            .build();
        
        // Then: Should emit ADDED result change
        StepVerifier.create(query.processChange(insert))
            .assertNext(change -> {
                assertThat(change.type()).isEqualTo(ResultChange.ChangeType.ADDED);
                assertThat(change.after()).isNotNull();
                assertThat(change.after().get("id")).isEqualTo(1);
                assertThat(change.before()).isNull();
            })
            .verifyComplete();
    }
    
    @Test
    void shouldEmitUpdatedResultWhenMatchingEntityChanges() {
        // Given: Query with existing result for order #1
        // When: Order #1 customer name changes
        // Then: Should emit UPDATED result change with before/after
    }
    
    @Test
    void shouldEmitDeletedResultWhenEntityNoLongerMatches() {
        // Given: Query with result for order #1 (status='READY')
        // When: Order #1 status changes to 'PREPARING'
        // Then: Should emit DELETED result change
    }
    
    @Test
    void shouldHandleMultiSourceJoin() {
        // Given: Query joining Orders and Drivers
        // When: Driver becomes available
        // Then: Should create synthetic relationship and emit matches
    }
    
    @Test
    void shouldApplyMiddlewarePipeline() {
        // Given: Query with enrichment and validation middleware
        // When: Change event received
        // Then: Should apply middleware in order before processing
    }
    
    @Test
    void shouldRespectRetentionPolicy() {
        // Given: Query with expire retention policy (1 hour)
        // When: Results older than 1 hour exist
        // Then: Should not return old results in point-in-time queries
    }
}
```

### 10.2 Integration Tests

```java
package io.drasi.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration tests with real PostgreSQL source.
 */
@SpringBootTest
class ContinuousQueryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldDetectPostgreSQLInsert() {
        // Given: Continuous query monitoring orders table
        // When: Row inserted into PostgreSQL
        // Then: Should receive change event and emit result
    }
    
    @Test
    void shouldHandlePostgreSQLUpdate() {
        // Given: Query with existing result
        // When: Row updated in PostgreSQL
        // Then: Should emit appropriate result change
    }
    
    @Test
    void endToEndIncidentAlertingScenario() {
        // Given: Incident alerting query from documentation
        // When: Series of changes (employee relocates, incident added, severity changes)
        // Then: Should emit correct sequence of result changes
    }
}
```

### 10.3 Performance Tests

```java
package io.drasi.performance;

/**
 * Performance benchmarks for continuous queries.
 */
class ContinuousQueryPerformanceTest {
    
    @Test
    void shouldProcess10KEventsPerSecond() {
        // Benchmark: Process 10,000 change events per second
        // Target: < 10ms p99 latency
    }
    
    @Test
    void shouldHandleLargeResultSet() {
        // Benchmark: Query with 1M results
        // Target: < 5GB memory, < 100ms incremental update
    }
    
    @Test
    void shouldScaleWithMultipleQueries() {
        // Benchmark: 100 concurrent queries
        // Target: Linear performance degradation
    }
}
```

---

## 11. Implementation Checklist

### Phase 1: Core Foundation (Week 1-2)
- [ ] Create data models (ChangeEvent, QueryResult, ResultChange)
- [ ] Implement ContinuousQuery interface and builder
- [ ] Create SourceSubscription and JoinDefinition models
- [ ] Implement ViewConfig with retention policies
- [ ] Write unit tests for all models

### Phase 2: Query Parser (Week 3-4)
- [ ] Define ANTLR4 grammar for Cypher subset
- [ ] Implement CypherQueryParser
- [ ] Create QueryPlan and Pattern abstractions
- [ ] Add query validation
- [ ] Write parser unit tests

### Phase 3: Graph Store (Week 5-6)
- [ ] Implement in-memory GraphStore
- [ ] Add node and relationship indexing
- [ ] Implement pattern matching
- [ ] Add cross-source join support (synthetic relationships)
- [ ] Write graph store unit tests

### Phase 4: Incremental Processing (Week 7-9)
- [ ] Implement IncrementalUpdateProcessor
- [ ] Create result change detection logic
- [ ] Add candidate result identification
- [ ] Implement INSERT/UPDATE/DELETE handlers
- [ ] Write incremental processing tests

### Phase 5: Result Set Cache (Week 10-11)
- [ ] Implement ResultSetCache interface
- [ ] Create RedisResultSetCache implementation
- [ ] Add multi-dimensional indexing
- [ ] Implement retention policy enforcement
- [ ] Write cache unit tests

### Phase 6: Middleware (Week 12-13)
- [ ] Define Middleware interface
- [ ] Implement MiddlewareExecutor
- [ ] Create example middleware (enrichment, validation)
- [ ] Add middleware pipeline processing
- [ ] Write middleware tests

### Phase 7: Source Integration (Week 14-15)
- [ ] Integrate with PostgresSource
- [ ] Implement source subscription management
- [ ] Add label mapping
- [ ] Handle initial snapshot loading
- [ ] Write source integration tests

### Phase 8: API Layer (Week 16-17)
- [ ] Create REST API controllers
- [ ] Implement WebSocket handler
- [ ] Add Server-Sent Events support
- [ ] Create YAML configuration loader
- [ ] Write API integration tests

### Phase 9: Spring Boot Integration (Week 18-19)
- [ ] Create auto-configuration
- [ ] Add health indicators
- [ ] Implement metrics collection
- [ ] Add actuator endpoints
- [ ] Write Spring Boot integration tests

### Phase 10: Documentation & Examples (Week 20)
- [ ] Write API documentation
- [ ] Create usage examples
- [ ] Add architecture diagrams
- [ ] Write deployment guide
- [ ] Create tutorial for incident alerting scenario

---

## 12. Example Usage

### 12.1 Programmatic API

```java
// Create continuous query programmatically
ContinuousQuery query = ContinuousQuery.builder()
    .id("manager-alerts")
    .mode(QueryMode.QUERY)
    .language(QueryLanguage.CYPHER)
    .query("""
        MATCH
          (e:Employee)-[:ASSIGNED_TO]->(t:Team),
          (m:Employee)-[:MANAGES]->(t:Team),
          (e:Employee)-[:LOCATED_IN]->(:Building)-[:LOCATED_IN]->(r:Region),
          (i:Incident {type:'environmental'})-[:OCCURS_IN]->(r:Region)
        WHERE
          elementId(e) <> elementId(m) 
          AND i.severity IN ['critical', 'extreme']
        RETURN
          m.email AS managerEmail,
          e.email AS employeeEmail,
          i.description AS incident
        """)
    .addSource(SourceSubscription.builder()
        .sourceId("hr-system")
        .addNodeLabel("Employee")
        .addNodeLabel("Team")
        .addNodeLabel("Region")
        .addRelationLabel("ASSIGNED_TO")
        .addRelationLabel("MANAGES")
        .build())
    .viewConfig(new ViewConfig(
        true,
        new ViewConfig.Latest()
    ))
    .build();

// Subscribe to result changes
query.processChange(changeEvent)
    .subscribe(resultChange -> {
        switch (resultChange.type()) {
            case ADDED -> handleNewAlert(resultChange.after());
            case UPDATED -> handleUpdatedAlert(resultChange.before(), resultChange.after());
            case DELETED -> handleResolvedAlert(resultChange.before());
        }
    });
```

### 12.2 Spring Boot Configuration

```java
@Configuration
@EnableDrasi
public class QueryConfiguration {
    
    @Bean
    public ContinuousQuery incidentAlerts() {
        return ContinuousQuery.builder()
            .fromYaml("""
                apiVersion: v1
                kind: ContinuousQuery
                name: incident-alerts
                spec:
                  query: |
                    MATCH (e:Employee)-[:ASSIGNED_TO]->(t:Team)
                    WHERE e.atRisk = true
                    RETURN e.name, e.email
                """)
            .build();
    }
    
    @Bean
    public Reaction alertReaction() {
        return LangChain4jReaction.builder()
            .queryId("incident-alerts")
            .onResultAdded(result -> {
                // Send alert
                emailService.sendAlert(
                    result.get("email").toString(),
                    "You are at risk!"
                );
            })
            .build();
    }
}
```

---

## 13. Success Criteria

### Functional Requirements
✅ Support Cypher query language with MATCH, WHERE, RETURN
✅ Detect added/updated/deleted result changes
✅ Process source changes incrementally (not full re-execution)
✅ Support multiple source subscriptions
✅ Support cross-source joins
✅ Apply middleware pipelines
✅ Cache results with configurable retention

### Performance Requirements
✅ Process 100,000+ events/second
✅ <10ms p99 incremental update latency
✅ <2GB memory for 1M cached results
✅ Linear scaling with query count

### Quality Requirements
✅ 90%+ test coverage
✅ Zero-downtime deployment
✅ Comprehensive monitoring
✅ Complete API documentation

---

## 14. References

- Original Drasi Documentation: https://drasi.io/concepts/continuous-queries/
- Cypher Query Language: https://neo4j.com/docs/cypher-manual/
- Incremental View Maintenance: Academic research papers
- ANTLR4 Documentation: https://www.antlr.org/
- Spring Boot Reactive: https://spring.io/reactive
- Project Reactor: https://projectreactor.io/

---

**END OF SPECIFICATION**