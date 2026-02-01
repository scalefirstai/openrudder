# OpenRudder API Reference

This document describes the external interfaces (input and output) of OpenRudder components.

## Table of Contents

- [Core Interfaces](#core-interfaces)
- [Data Sources](#data-sources)
- [Query Engine](#query-engine)
- [Reactions](#reactions)
- [Spring Boot Configuration](#spring-boot-configuration)

---

## Core Interfaces

### Source Interface

**Package:** `io.openrudder.core.source`

#### Input

```java
public interface Source {
    String getId();
    SourceConfig getConfig();
    Mono<Void> start();
    Mono<Void> stop();
    Flux<ChangeEvent> getChangeStream();
}
```

**Configuration:**
- `id` (String): Unique identifier for the source
- `name` (String): Human-readable name
- `enabled` (boolean): Whether the source is enabled

#### Output

Returns `Flux<ChangeEvent>` containing:
- `type` (ChangeType): INSERT, UPDATE, DELETE
- `entityType` (String): Type of entity changed
- `entityId` (String): Unique identifier
- `before` (Map<String, Object>): Previous state (for UPDATE/DELETE)
- `after` (Map<String, Object>): New state (for INSERT/UPDATE)
- `timestamp` (Instant): When the change occurred
- `metadata` (Map<String, Object>): Additional metadata

---

### ContinuousQuery Interface

**Package:** `io.openrudder.core.query`

#### Input

```java
public interface ContinuousQuery {
    String getId();
    String getName();
    String getQuery();
    Set<String> getSourceIds();
    Flux<ResultUpdate> execute(Flux<ChangeEvent> changeStream);
}
```

**Query Language:** Cypher-like syntax

**Example Query:**
```cypher
MATCH (o:Order)
WHERE o.status = 'READY' AND o.amount > 100
RETURN o
```

#### Output

Returns `Flux<ResultUpdate>` containing:
- `queryId` (String): Query identifier
- `changeType` (ResultChange): ADDED, MODIFIED, REMOVED
- `result` (QueryResult): The query result
- `timestamp` (Instant): When the update occurred

**QueryResult Structure:**
- `id` (String): Result identifier
- `data` (Map<String, Object>): Result data
- `metadata` (Map<String, Object>): Additional metadata

---

### Reaction Interface

**Package:** `io.openrudder.core.reaction`

#### Input

```java
public interface Reaction {
    String getId();
    String getName();
    String getQueryId();
    Mono<Void> start();
    Mono<Void> stop();
    Mono<Void> onResultUpdate(ResultUpdate update);
}
```

**ResultUpdate Input:**
- `queryId` (String): Source query identifier
- `changeType` (ResultChange): Type of change
- `result` (QueryResult): The result data

#### Output

Reactions produce side effects (no direct return value):
- HTTP webhooks
- AI agent invocations
- Kafka messages
- Custom actions

---

## Data Sources

### PostgreSQL CDC Source

**Class:** `io.openrudder.sources.postgres.PostgresSource`

#### Configuration

```java
PostgresSource.builder()
    .name("orders-db")
    .host("localhost")
    .port(5432)
    .database("mydb")
    .username("user")
    .password("pass")
    .table("orders")
    .cdcEnabled(true)
    .slotName("openrudder_slot")
    .publicationName("openrudder_pub")
    .build();
```

**Required Parameters:**
- `host` (String): PostgreSQL host
- `port` (int): PostgreSQL port (default: 5432)
- `database` (String): Database name
- `username` (String): Database user
- `password` (String): Database password
- `table` (String): Table to monitor

**Optional Parameters:**
- `cdcEnabled` (boolean): Enable CDC (default: true)
- `slotName` (String): Replication slot name
- `publicationName` (String): Publication name
- `snapshotMode` (String): initial, never, always

#### Output

Emits `ChangeEvent` for each database change:
```json
{
  "type": "INSERT",
  "entityType": "Order",
  "entityId": "123",
  "after": {
    "id": 123,
    "customer_id": 456,
    "status": "PENDING",
    "amount": 99.99,
    "created_at": "2024-02-01T10:00:00Z"
  },
  "timestamp": "2024-02-01T10:00:00.123Z",
  "metadata": {
    "table": "orders",
    "database": "mydb",
    "lsn": "0/1234567"
  }
}
```

---

### MongoDB Change Streams Source

**Class:** `io.openrudder.sources.mongodb.MongoSource`

#### Configuration

```java
MongoSource.builder()
    .name("orders-mongo")
    .connectionString("mongodb://localhost:27017")
    .database("mydb")
    .collection("orders")
    .build();
```

**Required Parameters:**
- `connectionString` (String): MongoDB connection URI
- `database` (String): Database name
- `collection` (String): Collection to monitor

#### Output

Similar to PostgreSQL CDC, emits `ChangeEvent` for each document change.

---

### Kafka Source

**Class:** `io.openrudder.sources.kafka.KafkaSource`

#### Configuration

```java
KafkaSource.builder()
    .name("events-topic")
    .bootstrapServers("localhost:9092")
    .topic("order-events")
    .groupId("openrudder-consumer")
    .build();
```

**Required Parameters:**
- `bootstrapServers` (String): Kafka broker addresses
- `topic` (String): Topic to consume
- `groupId` (String): Consumer group ID

#### Output

Emits `ChangeEvent` for each Kafka message.

---

## Query Engine

### Query Language Syntax

OpenRudder uses a Cypher-like query language.

#### Pattern Matching

```cypher
MATCH (variable:EntityType)
WHERE variable.property = value
RETURN variable
```

#### Supported Operators

**Comparison:**
- `=` Equal
- `!=` Not equal
- `>` Greater than
- `<` Less than
- `>=` Greater than or equal
- `<=` Less than or equal

**Logical:**
- `AND` Logical AND
- `OR` Logical OR
- `NOT` Logical NOT

**String:**
- `CONTAINS` String contains
- `STARTS WITH` String starts with
- `ENDS WITH` String ends with

#### Examples

**Simple Filter:**
```cypher
MATCH (o:Order)
WHERE o.status = 'READY'
RETURN o
```

**Complex Conditions:**
```cypher
MATCH (o:Order)
WHERE o.status = 'READY' 
  AND o.amount > 100 
  AND o.customer_id IS NOT NULL
RETURN o
```

**Joins:**
```cypher
MATCH (o:Order)-[:BELONGS_TO]->(c:Customer)
WHERE c.tier = 'PREMIUM'
RETURN o, c
```

---

## Reactions

### LangChain4j AI Reaction

**Class:** `io.openrudder.reactions.ai.LangChain4jReaction`

#### Configuration

```java
LangChain4jReaction.builder()
    .name("order-processor")
    .queryId("ready-orders")
    .model(chatLanguageModel)
    .systemPrompt("You are an order processing AI assistant")
    .userPromptTemplate("Process this order: {order}")
    .onResponse(response -> {
        // Handle AI response
        return Mono.empty();
    })
    .build();
```

**Required Parameters:**
- `name` (String): Reaction name
- `queryId` (String): Query to react to
- `model` (ChatLanguageModel): LangChain4j model instance
- `systemPrompt` (String): System prompt for AI
- `onResponse` (Function): Response handler

#### Input

Receives `ResultUpdate` from query engine.

#### Output

Invokes AI model and executes response handler with:
- `text` (String): AI response text
- `metadata` (Map): Response metadata

---

### HTTP Webhook Reaction

**Class:** `io.openrudder.reactions.webhook.WebhookReaction`

#### Configuration

```java
WebhookReaction.builder()
    .name("order-webhook")
    .queryId("ready-orders")
    .url("https://api.example.com/webhooks/orders")
    .method("POST")
    .headers(Map.of("Authorization", "Bearer token"))
    .build();
```

**Required Parameters:**
- `url` (String): Webhook URL
- `method` (String): HTTP method (GET, POST, PUT, DELETE)

**Optional Parameters:**
- `headers` (Map<String, String>): HTTP headers
- `timeout` (Duration): Request timeout

#### Output

Sends HTTP request with JSON body:
```json
{
  "queryId": "ready-orders",
  "changeType": "ADDED",
  "result": {
    "id": "123",
    "data": { ... }
  },
  "timestamp": "2024-02-01T10:00:00Z"
}
```

---

## Spring Boot Configuration

### Application Properties

```yaml
openrudder:
  enabled: true
  auto-start: true
  
  sources:
    postgres:
      host: localhost
      port: 5432
      database: mydb
      username: ${DB_USER}
      password: ${DB_PASSWORD}
      
  queries:
    - id: ready-orders
      name: Ready Orders
      query: |
        MATCH (o:Order)
        WHERE o.status = 'READY'
        RETURN o
        
  reactions:
    - type: webhook
      name: order-webhook
      query-id: ready-orders
      url: https://api.example.com/webhooks
```

### Java Configuration

```java
@Configuration
@EnableOpenRudder
public class OpenRudderConfig {
    
    @Bean
    public PostgresSource ordersSource() {
        return PostgresSource.builder()
            .name("orders")
            .host("localhost")
            .database("mydb")
            .table("orders")
            .build();
    }
    
    @Bean
    public ContinuousQuery readyOrders() {
        return ContinuousQuery.builder()
            .id("ready-orders")
            .query("MATCH (o:Order) WHERE o.status = 'READY' RETURN o")
            .build();
    }
}
```

---

## Error Handling

### Exception Types

- `SourceException`: Source-related errors
- `QueryException`: Query parsing or execution errors
- `ReactionException`: Reaction execution errors
- `ConfigurationException`: Configuration errors

### Error Response Format

```json
{
  "error": "QueryException",
  "message": "Invalid query syntax",
  "details": {
    "query": "MATCH (o:Order WHERE ...",
    "position": 20,
    "expected": ")"
  },
  "timestamp": "2024-02-01T10:00:00Z"
}
```

---

## Health Checks

### Endpoint

`GET /actuator/health/openrudder`

### Response

```json
{
  "status": "UP",
  "components": {
    "sources": {
      "status": "UP",
      "details": {
        "orders-db": "UP",
        "events-kafka": "UP"
      }
    },
    "queries": {
      "status": "UP",
      "details": {
        "ready-orders": "RUNNING"
      }
    },
    "reactions": {
      "status": "UP",
      "details": {
        "order-webhook": "ACTIVE"
      }
    }
  }
}
```

---

## Metrics

### Available Metrics

- `openrudder.source.events.total`: Total events processed
- `openrudder.query.executions.total`: Total query executions
- `openrudder.query.latency`: Query execution latency
- `openrudder.reaction.invocations.total`: Total reaction invocations
- `openrudder.reaction.errors.total`: Total reaction errors

### Prometheus Format

```
# HELP openrudder_source_events_total Total events processed by source
# TYPE openrudder_source_events_total counter
openrudder_source_events_total{source="orders-db",type="INSERT"} 1234
openrudder_source_events_total{source="orders-db",type="UPDATE"} 567
```

---

## Version Information

**Current Version:** 1.0.0-SNAPSHOT  
**API Stability:** Development (subject to change)  
**Java Version:** 21+  
**Spring Boot Version:** 3.2+

---

## Support

- **Documentation:** https://openrudder.io/docs
- **API Reference:** https://openrudder.io/api
- **GitHub Issues:** https://github.com/scalefirstai/openrudder/issues
- **Discord:** https://discord.gg/openrudder
