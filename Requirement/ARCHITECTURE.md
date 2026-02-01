# OpenRudder Architecture & Implementation Guide

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Component Deep Dive](#component-deep-dive)
3. [Query Language Design](#query-language-design)
4. [Incremental Query Processing](#incremental-query-processing)
5. [AI Agent Integration Patterns](#ai-agent-integration-patterns)
6. [Performance Optimization](#performance-optimization)
7. [Deployment Architectures](#deployment-architectures)
8. [Implementation Roadmap](#implementation-roadmap)

---

## System Architecture

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        OpenRudder Platform                        │
└──────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │   RudderEngine Core    │
                    │   (Orchestrator)      │
                    └───────────┬───────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼────────┐     ┌───────▼────────┐     ┌───────▼────────┐
│  Source Layer  │     │  Query Engine  │     │ Reaction Layer │
│  (CDC/Events)  │     │  (Continuous)  │     │  (AI Agents)   │
└────────────────┘     └────────────────┘     └────────────────┘
        │                       │                       │
        │                       │                       │
┌───────▼────────────────────────▼───────────────────────▼───────┐
│              Change Event Store & Streaming Bus                │
│         (Reactor Flux / Kafka / Redis Streams)                 │
└────────────────────────────────────────────────────────────────┘
```

### Component Layer Breakdown

#### 1. Source Layer

**Purpose**: Capture data changes from various sources

**Components**:
- `Source<T>` interface - Abstract source definition
- `ChangeEventEmitter` - Publishes change events
- CDC Connectors:
  - `PostgresSource` - PostgreSQL logical replication
  - `MongoSource` - MongoDB change streams
  - `KafkaSource` - Kafka consumer
  - `JdbcPollingSource` - Generic JDBC polling
  - `FileSystemSource` - File system watcher

**Implementation Details**:

```java
// Source Architecture
interface Source<C extends SourceConfig> {
    Flux<ChangeEvent> start();  // Returns reactive stream
    void stop();
    Flux<ChangeEvent> snapshot();  // Initial state
}

// PostgresSource Implementation Strategy
class PostgresSource {
    // Two modes:
    // 1. CDC Mode: Uses PostgreSQL logical replication (pg_logical)
    // 2. Polling Mode: Fallback using timestamp columns
    
    private void setupLogicalReplication() {
        // Create replication slot
        // Configure publication
        // Start WAL streaming
    }
    
    private Flux<ChangeEvent> streamWalChanges() {
        // Stream Write-Ahead Log changes
        // Parse pgoutput format
        // Convert to ChangeEvent
    }
}
```

**Key Technologies**:
- Debezium Embedded Engine for robust CDC
- Project Reactor for reactive streams
- PostgreSQL pgoutput / wal2json plugins
- MongoDB reactive streams driver

#### 2. Query Engine Layer

**Purpose**: Continuously evaluate queries and detect meaningful changes

**Components**:
- `QueryParser` - Parse query DSL
- `QueryPlanner` - Generate execution plan
- `IncrementalExecutor` - Execute incremental updates
- `ResultSetManager` - Manage query result sets
- `GraphStore` - Internal graph database for joins

**Architecture**:

```
┌────────────────────────────────────────────────────────────┐
│                    Query Engine                            │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────┐    ┌──────────┐    ┌─────────────┐        │
│  │  Parser  │───▶│ Planner  │───▶│  Executor   │        │
│  │ (ANTLR4) │    │ (Graph)  │    │(Incremental)│        │
│  └──────────┘    └──────────┘    └─────────────┘        │
│                                          │                │
│                                          ▼                │
│                                   ┌─────────────┐        │
│                                   │ ResultSet   │        │
│                                   │  Manager    │        │
│                                   └─────────────┘        │
│                                                            │
│  ┌────────────────────────────────────────────────────┐  │
│  │         Internal Graph Store (Neo4j Embedded)      │  │
│  │  - Nodes: Entities from all sources                │  │
│  │  - Edges: Relationships from queries               │  │
│  │  - Properties: Entity attributes                   │  │
│  └────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

**Incremental Update Algorithm**:

```java
class IncrementalQueryExecutor {
    
    // Core algorithm: Given a change event, update only affected results
    Flux<ResultUpdate> evaluateIncremental(ChangeEvent change) {
        // 1. Identify affected query patterns
        Set<QueryPattern> affected = findAffectedPatterns(change);
        
        // 2. For each affected pattern:
        for (QueryPattern pattern : affected) {
            // 3. Find existing results that might be impacted
            Set<ResultId> candidates = findCandidateResults(pattern, change);
            
            // 4. Re-evaluate only those candidates
            for (ResultId resultId : candidates) {
                QueryResult oldResult = resultCache.get(resultId);
                QueryResult newResult = reevaluate(pattern, change, oldResult);
                
                // 5. Emit update if result changed
                if (!Objects.equals(oldResult, newResult)) {
                    if (newResult == null) {
                        emit(new ResultUpdate(REMOVED, oldResult, null));
                    } else if (oldResult == null) {
                        emit(new ResultUpdate(ADDED, null, newResult));
                    } else {
                        emit(new ResultUpdate(UPDATED, oldResult, newResult));
                    }
                }
            }
        }
    }
    
    // Example: For query "MATCH (o:Order) WHERE o.status = 'READY'"
    // If change updates Order #42 status from PREPARING → READY:
    // - Pattern affected: Order node with status filter
    // - Candidate results: None (new result)
    // - Emit: ResultUpdate(ADDED, null, {order_id: 42, ...})
}
```

#### 3. Reaction Layer

**Purpose**: Execute actions in response to query updates

**Components**:
- `Reaction` interface - Abstract reaction definition
- `AIAgentReaction` - AI-powered reactions
- `WebhookReaction` - HTTP callback reactions
- `MessageQueueReaction` - Kafka/RabbitMQ publishers
- `DatabaseReaction` - Write results to DB

**AI Agent Integration**:

```java
interface AIAgentReaction extends Reaction {
    
    // Called when new result appears
    Mono<Void> onResultAdded(QueryResult result);
    
    // Called when result changes
    Mono<Void> onResultUpdated(QueryResult before, QueryResult after);
    
    // Called when result no longer matches
    Mono<Void> onResultRemoved(QueryResult result);
    
    // Multi-agent workflow
    Mono<Void> executeWorkflow(ResultUpdate update);
}

// LangChain4j Integration
class LangChain4jReaction implements AIAgentReaction {
    private final ChatLanguageModel model;
    private final String promptTemplate;
    
    @Override
    public Mono<Void> onResultAdded(QueryResult result) {
        return Mono.fromCallable(() -> {
            String prompt = formatPrompt(promptTemplate, result);
            return model.generate(prompt);
        })
        .flatMap(this::processAiResponse)
        .subscribeOn(Schedulers.boundedElastic());
    }
}
```

---

## Query Language Design

### DSL Syntax (Cypher-Inspired)

```cypher
// Basic pattern matching
MATCH (o:Order)
WHERE o.status = 'READY'
RETURN o.id, o.customer

// Multi-source join
MATCH (o:Order), (d:Driver)
WHERE o.status = 'READY'
  AND d.available = true
  AND distance(o.location, d.location) < 5.0
RETURN o.id, d.id, distance(o.location, d.location) AS dist

// Aggregation with window functions
MATCH (t:Transaction)
WHERE t.timestamp > now() - interval '1 hour'
WITH t.account_id AS account,
     COUNT(*) AS txCount,
     SUM(t.amount) AS totalAmount
WHERE txCount > 10 AND totalAmount > 10000
RETURN account, txCount, totalAmount

// Temporal conditions
MATCH (s:Sensor)
WHERE s.value > s.threshold
  AND s.timestamp > now() - interval '5 minutes'
WITH s,
     AVG(s.value) OVER (
         PARTITION BY s.device_id
         ORDER BY s.timestamp
         ROWS BETWEEN 10 PRECEDING AND CURRENT ROW
     ) AS movingAvg
WHERE movingAvg > s.threshold * 1.2
RETURN s.device_id, s.value, movingAvg

// Subqueries
MATCH (order:Order)
WHERE order.status = 'PENDING'
  AND EXISTS {
      MATCH (order)-[:CONTAINS]->(item:Item)
      WHERE item.out_of_stock = true
  }
RETURN order.id, order.customer

// Graph traversal
MATCH (product:Product)<-[:BOUGHT]-(customer:Customer)-[:BOUGHT]->(other:Product)
WHERE product.id = $targetProduct
  AND other.id != $targetProduct
GROUP BY other.id
RETURN other.id, other.name, COUNT(*) AS frequency
ORDER BY frequency DESC
LIMIT 10
```

### Grammar Definition (ANTLR4)

```antlr4
grammar RudderQuery;

query
    : matchClause whereClause? withClause? returnClause orderClause? limitClause?
    ;

matchClause
    : MATCH pattern (',' pattern)*
    ;

pattern
    : '(' variable ':' label ')' relationship*
    ;

relationship
    : '-[' variable? ':' type ']->(' variable ':' label ')'
    ;

whereClause
    : WHERE expression
    ;

returnClause
    : RETURN returnItem (',' returnItem)*
    ;

// Lexer rules
MATCH : 'MATCH' ;
WHERE : 'WHERE' ;
RETURN : 'RETURN' ;
AND : 'AND' ;
OR : 'OR' ;
```

---

## Incremental Query Processing

### Mathematical Foundation

Rudder uses **Incremental View Maintenance (IVM)** theory:

Given:
- Query Q
- Database state D₀
- Change Δ (insert/update/delete)
- Result R₀ = Q(D₀)

Compute:
- R₁ = Q(D₀ ⊕ Δ) incrementally
- ΔR = R₁ - R₀ (result delta)

### Implementation Strategy

```java
class IncrementalViewMaintenance {
    
    /**
     * Core IVM algorithm implementation.
     */
    public ResultDelta maintainView(
            Query query,
            ViewState currentView,
            ChangeEvent change) {
        
        // Step 1: Identify which query patterns are affected
        Set<Pattern> affected = query.patternsAffecting(change.entityType());
        
        // Step 2: For each affected pattern, compute delta
        ResultDelta totalDelta = new ResultDelta();
        
        for (Pattern pattern : affected) {
            switch (change.type()) {
                case INSERT -> totalDelta.merge(handleInsert(pattern, change));
                case UPDATE -> totalDelta.merge(handleUpdate(pattern, change));
                case DELETE -> totalDelta.merge(handleDelete(pattern, change));
            }
        }
        
        return totalDelta;
    }
    
    private ResultDelta handleInsert(Pattern pattern, ChangeEvent change) {
        // For INSERT: Check if new entity matches pattern
        if (pattern.matches(change.after())) {
            // Find all combinations with other patterns
            List<QueryResult> newResults = pattern.expandToResults(change.after());
            return ResultDelta.added(newResults);
        }
        return ResultDelta.empty();
    }
    
    private ResultDelta handleUpdate(Pattern pattern, ChangeEvent change) {
        boolean beforeMatched = pattern.matches(change.before());
        boolean afterMatched = pattern.matches(change.after());
        
        if (!beforeMatched && afterMatched) {
            // Entity now matches - treat as insert
            return handleInsert(pattern, change);
        } else if (beforeMatched && !afterMatched) {
            // Entity no longer matches - treat as delete
            return handleDelete(pattern, change);
        } else if (beforeMatched && afterMatched) {
            // Entity still matches but values changed
            List<QueryResult> updated = pattern.updateResults(change);
            return ResultDelta.updated(updated);
        }
        
        return ResultDelta.empty();
    }
    
    private ResultDelta handleDelete(Pattern pattern, ChangeEvent change) {
        if (pattern.matches(change.before())) {
            List<QueryResult> removed = pattern.findResultsWith(change.entityId());
            return ResultDelta.removed(removed);
        }
        return ResultDelta.empty();
    }
}
```

### Optimization: Indexing for Fast Lookups

```java
class QueryResultIndex {
    // Multi-dimensional index structure
    
    // Index by entity ID: O(1) lookup for affected results
    private final Map<Object, Set<ResultId>> entityIndex;
    
    // Index by pattern: O(1) lookup for pattern matches
    private final Map<Pattern, Set<ResultId>> patternIndex;
    
    // Index by field value: O(log n) range queries
    private final NavigableMap<String, NavigableMap<Object, Set<ResultId>>> fieldIndex;
    
    public Set<ResultId> findAffectedResults(ChangeEvent change) {
        // Fast lookup using entity ID
        return entityIndex.getOrDefault(change.entityId(), Set.of());
    }
    
    public Set<ResultId> findByFieldValue(String field, Object value) {
        return fieldIndex
            .getOrDefault(field, new TreeMap<>())
            .getOrDefault(value, Set.of());
    }
}
```

---

## AI Agent Integration Patterns

### Pattern 1: Single Agent per Query

```java
// One AI agent processes all updates from a specific query
ContinuousQuery criticalOrders = /* ... */;

LangChain4jReaction agent = LangChain4jReaction.builder()
    .queryId(criticalOrders.id())
    .model(chatModel)
    .onResultUpdate(update -> {
        // AI agent decides action for each update
    })
    .build();
```

### Pattern 2: Multi-Agent Workflow (LangGraph)

```java
// Multiple specialized agents collaborate
class OrderProcessingWorkflow {
    
    private final LangGraph workflow;
    
    public OrderProcessingWorkflow() {
        this.workflow = LangGraph.builder()
            .addNode("analyzer", new OrderAnalyzerAgent())
            .addNode("prioritizer", new PriorityAgent())
            .addNode("router", new RoutingAgent())
            .addNode("notifier", new NotificationAgent())
            
            .addEdge("analyzer", "prioritizer")
            .addConditionalEdge("prioritizer", this::routeByPriority)
            .addEdge("router", "notifier")
            
            .build();
    }
    
    private String routeByPriority(WorkflowState state) {
        int priority = (int) state.get("priority");
        return priority > 5 ? "urgent_router" : "normal_router";
    }
    
    public Mono<Void> processUpdate(ResultUpdate update) {
        return workflow.execute(Map.of("update", update));
    }
}
```

### Pattern 3: RAG-Enhanced Agents

```java
// Agent with retrieval-augmented generation
class RAGEnhancedReaction implements Reaction {
    
    private final ChatLanguageModel model;
    private final EmbeddingStore<TextSegment> knowledgeBase;
    
    @Override
    public Mono<Void> onResultAdded(QueryResult result) {
        return Mono.fromCallable(() -> {
            // 1. Retrieve relevant context
            List<TextSegment> context = retrieveContext(result);
            
            // 2. Build prompt with context
            String prompt = buildPromptWithContext(result, context);
            
            // 3. Generate response
            return model.generate(prompt);
        })
        .flatMap(this::executeAction);
    }
    
    private List<TextSegment> retrieveContext(QueryResult result) {
        Embedding queryEmbedding = embeddingModel.embed(result.toString()).content();
        return knowledgeBase.findRelevant(queryEmbedding, 5);
    }
}
```

---

## Performance Optimization

### 1. Query Optimization

```java
class QueryOptimizer {
    
    public Query optimize(Query original) {
        Query optimized = original;
        
        // Predicate pushdown
        optimized = pushdownPredicates(optimized);
        
        // Join reordering
        optimized = reorderJoins(optimized);
        
        // Index selection
        optimized = selectIndexes(optimized);
        
        return optimized;
    }
    
    private Query pushdownPredicates(Query query) {
        // Move WHERE conditions closer to MATCH patterns
        // Example: MATCH (o:Order), (d:Driver) WHERE o.id = 123
        // Becomes: MATCH (o:Order {id: 123}), (d:Driver)
    }
}
```

### 2. Caching Strategies

```java
class ResultSetCache {
    
    // L1: In-memory LRU cache
    private final Cache<QueryId, ResultSet> l1Cache;
    
    // L2: Redis distributed cache
    private final RedisTemplate<String, ResultSet> l2Cache;
    
    public Mono<ResultSet> get(QueryId queryId) {
        // Try L1 first
        ResultSet cached = l1Cache.getIfPresent(queryId);
        if (cached != null) {
            return Mono.just(cached);
        }
        
        // Try L2
        return l2Cache.opsForValue()
            .get(queryId.toString())
            .doOnNext(result -> l1Cache.put(queryId, result));
    }
}
```

### 3. Parallel Processing

```java
class ParallelQueryExecutor {
    
    public Flux<ResultUpdate> evaluateParallel(
            ContinuousQuery query,
            Flux<ChangeEvent> changes) {
        
        return changes
            .parallel(8)  // Process 8 changes concurrently
            .runOn(Schedulers.parallel())
            .flatMap(change -> query.evaluateIncremental(change))
            .sequential();
    }
}
```

---

## Deployment Architectures

### Architecture 1: Single-Node (Development)

```
┌──────────────────────────────────┐
│     Spring Boot Application       │
├──────────────────────────────────┤
│  RudderEngine (Embedded)          │
│  - In-memory change store         │
│  - Embedded query executor        │
│  - All sources/reactions local    │
└──────────────────────────────────┘
```

### Architecture 2: Distributed (Production)

```
                    ┌─────────────┐
                    │   Kafka     │
                    │ (Change Bus)│
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼────────┐  ┌──────▼──────┐  ┌───────▼────────┐
│ Source Workers │  │   Query     │  │   Reaction     │
│  (CDC → Kafka) │  │  Executors  │  │   Workers      │
│                │  │ (Stateless) │  │  (AI Agents)   │
└────────────────┘  └─────────────┘  └────────────────┘
                           │
                    ┌──────▼──────┐
                    │    Redis    │
                    │ (Result Set)│
                    └─────────────┘
```

### Architecture 3: Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: Rudder-query-executor
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: query-executor
        image: OpenRudder/query-executor:1.0
        env:
        - name: KAFKA_BROKERS
          value: kafka:9092
        - name: REDIS_URL
          value: redis://redis:6379
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: Rudder-ai-agents
spec:
  replicas: 5
  template:
    spec:
      containers:
      - name: ai-agent
        image: OpenRudder/ai-agents:1.0
        env:
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secrets
              key: openai-key
```

---

## Implementation Roadmap

### Phase 1: Core Engine (Months 1-2)
- [x] ChangeEvent model
- [x] Source interface & PostgresSource
- [x] Basic query parser (ANTLR4)
- [ ] In-memory query executor
- [ ] Reaction interface

### Phase 2: AI Integration (Month 3)
- [x] LangChain4j reaction
- [ ] Spring AI reaction
- [ ] Multi-agent workflow support
- [ ] RAG integration

### Phase 3: Advanced Sources (Month 4)
- [ ] MongoDB source
- [ ] Kafka source
- [ ] Debezium integration
- [ ] File system source

### Phase 4: Query Engine (Months 5-6)
- [ ] Full query language support
- [ ] Incremental view maintenance
- [ ] Graph store (Neo4j embedded)
- [ ] Query optimization

### Phase 5: Enterprise Features (Months 7-8)
- [ ] Distributed deployment
- [ ] Redis change store
- [ ] Kafka change bus
- [ ] Metrics & observability

### Phase 6: Developer Experience (Month 9)
- [x] Spring Boot starter
- [ ] CLI tools
- [ ] Web console
- [ ] IDE plugins

---

## Getting Started with Development

```bash
# Clone repository
git clone https://github.com/OpenRudder/Rudder-ai-agents.git
cd Rudder-ai-agents

# Build all modules
mvn clean install

# Run tests
mvn test

# Start example application
cd examples/restaurant-delivery
mvn spring-boot:run
```

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## License

Apache 2.0 - see [LICENSE](LICENSE)
