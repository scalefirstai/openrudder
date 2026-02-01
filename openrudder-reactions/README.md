# OpenRudder Reactions

Reaction implementations for OpenRudder - the output layer that processes query result changes and executes configurable actions.

## Overview

Reactions subscribe to Continuous Queries and execute actions when results change. They support:

- ✅ Multiple reaction types (HTTP, Kafka, AI agents, Debug)
- ✅ Pluggable provider architecture via Java ServiceLoader
- ✅ Automatic retry with exponential backoff
- ✅ Health monitoring and metrics
- ✅ Per-query configuration
- ✅ Batching and throttling
- ✅ Secure credential management

## Architecture

```
Continuous Query → Result Changes → Reaction → Action
      ↓                  ↓              ↓          ↓
   MATCH o:Order    [ADDED o:42]   Process    HTTP POST
   WHERE ready         ↓           Change      Send Email
   RETURN o         [UPDATED]        ↓         Update DB
                    [DELETED]     Execute     Call AI Agent
```

## Built-in Reaction Types

### 1. HTTP Webhook Reaction

Sends HTTP requests when query results change.

```java
ReactionConfig config = new ReactionBuilder()
    .id("webhook-1")
    .name("Order Webhook")
    .kind("http")
    .addQueryId("ready-orders")
    .property("webhookUrl", "https://api.example.com/webhooks/orders")
    .property("method", "POST")
    .property("headers", Map.of(
        "Content-Type", "application/json",
        "X-API-Key", "secret-key"
    ))
    .build();

HttpWebhookReaction reaction = new HttpWebhookReaction(config);
reaction.start();
```

**Configuration:**
- `webhookUrl` (required): URL to send webhooks to
- `method` (optional): HTTP method (GET, POST, PUT, PATCH, DELETE), default: POST
- `headers` (optional): HTTP headers to include

### 2. Debug Reaction

Logs all changes to console for development and debugging.

```java
ReactionConfig config = new ReactionBuilder()
    .id("debug-1")
    .name("Debug Reaction")
    .kind("debug")
    .addQueryId("ready-orders")
    .property("verbose", true)
    .build();

DebugReaction reaction = new DebugReaction(config);
reaction.start();
```

**Configuration:**
- `verbose` (optional): Show full result data instead of just field names, default: false

### 3. Kafka Reaction

Publishes query results to Kafka topics.

```java
KafkaSender<String, String> sender = KafkaSender.create(senderOptions);

ReactionConfig config = new ReactionBuilder()
    .id("kafka-1")
    .name("Order Events")
    .kind("kafka")
    .addQueryId("ready-orders")
    .property("topic", "order-events")
    .property("keyField", "orderId")
    .build();

KafkaReaction reaction = new KafkaReaction(config, sender);
reaction.start();
```

**Configuration:**
- `topic` (required): Kafka topic to publish to
- `keyField` (optional): Field name to use as Kafka message key, default: "id"

### 4. LangChain4j AI Agent Reaction

Invokes AI models to process query results.

```java
ChatLanguageModel model = OpenAiChatModel.builder()
    .apiKey(apiKey)
    .modelName("gpt-3.5-turbo")
    .build();

LangChain4jReaction reaction = LangChain4jReaction.builder()
    .id("ai-1")
    .name("Order AI Agent")
    .queryId("ready-orders")
    .model(model)
    .systemPrompt("You are an order processing assistant.")
    .userPromptTemplate(
        "A new order is ready:\n" +
        "Order ID: {orderId}\n" +
        "Status: {status}\n" +
        "Total: ${total}\n\n" +
        "Please acknowledge this order."
    )
    .onResponse(aiMessage -> {
        System.out.println("AI Response: " + aiMessage.text());
        return Mono.empty();
    })
    .build();

reaction.start();
```

**Configuration:**
- `systemPrompt` (optional): System prompt for the AI model
- `userPromptTemplate` (optional): User prompt template with placeholders like `{fieldName}`, `{queryId}`, `{changeType}`

## Using the Reaction Registry

The `ReactionRegistry` manages reaction lifecycle and uses ServiceLoader for provider discovery:

```java
ReactionRegistry registry = new ReactionRegistry();

// List available providers
registry.getProviders().forEach((kind, provider) -> {
    System.out.println(kind + ": " + provider.configSchema().description());
});

// Create a reaction
ReactionConfig config = new ReactionBuilder()
    .id("my-reaction")
    .name("My Reaction")
    .kind("debug")
    .addQueryId("my-query")
    .build();

Reaction reaction = registry.create(config).block();

// Get reaction
Reaction retrieved = registry.get("my-reaction").block();

// List all reactions
List<Reaction> reactions = registry.list().collectList().block();

// Delete reaction
registry.delete("my-reaction").block();
```

## Retry Configuration

Configure automatic retry with exponential backoff:

```java
ReactionConfig.RetryConfig retryConfig = new ReactionConfig.RetryConfig(
    true,                           // enabled
    3,                              // maxAttempts
    1000,                           // initialBackoffMs
    30000,                          // maxBackoffMs
    2.0,                            // backoffMultiplier
    Set.of(IOException.class)       // retryableExceptions
);

ReactionConfig config = new ReactionConfig(
    "retry-1",
    "Retry Reaction",
    "http",
    Set.of("my-query"),
    Map.of(),
    Map.of("webhookUrl", "https://api.example.com/webhook"),
    ReactionConfig.ExecutionConfig.defaults(),
    retryConfig,
    new ReactionConfig.SecurityConfig(Map.of(), Map.of())
);
```

## Execution Modes

Configure how reactions execute:

```java
ReactionConfig.ExecutionConfig executionConfig = new ReactionConfig.ExecutionConfig(
    ExecutionMode.ASYNC,    // SYNC, ASYNC, or BATCH
    100,                    // batchSize (for BATCH mode)
    1000,                   // batchWindowMs (for BATCH mode)
    0,                      // throttleRateMs (min time between executions)
    10                      // maxConcurrency (max parallel executions)
);
```

## Health Monitoring

Check reaction health and statistics:

```java
// Health check
Reaction.ReactionHealth health = reaction.health();
System.out.println("Healthy: " + health.healthy());
System.out.println("Status: " + health.status());
System.out.println("Message: " + health.message());

// Statistics
Reaction.ReactionStats stats = reaction.stats();
System.out.println("Total Processed: " + stats.totalProcessed());
System.out.println("Total Errors: " + stats.totalErrors());
System.out.println("Added Count: " + stats.addedCount());
System.out.println("Updated Count: " + stats.updatedCount());
System.out.println("Deleted Count: " + stats.deletedCount());
System.out.println("Avg Processing Time: " + stats.avgProcessingTimeMs() + "ms");
```

## Creating Custom Reactions

### 1. Implement the Reaction

```java
public class CustomReaction extends AbstractReaction {
    
    public CustomReaction(ReactionConfig config) {
        super(config);
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            ReactionConfig.QueryConfig queryConfig) {
        // Handle ADDED results
        return Mono.fromRunnable(() -> {
            System.out.println("Added: " + result.data());
        });
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        // Handle UPDATED results
        return Mono.fromRunnable(() -> {
            System.out.println("Updated: " + after.data());
        });
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        // Handle DELETED results
        return Mono.fromRunnable(() -> {
            System.out.println("Deleted: " + result.data());
        });
    }
}
```

### 2. Implement the Provider

```java
public class CustomReactionProvider implements ReactionProvider {
    
    @Override
    public String kind() {
        return "custom";
    }
    
    @Override
    public Reaction create(ReactionConfig config) {
        return new CustomReaction(config);
    }
    
    @Override
    public ValidationResult validate(ReactionConfig config) {
        // Validate configuration
        return ValidationResult.ofValid();
    }
    
    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(
            "custom",
            "Custom reaction description",
            List.of(
                new ConfigSchema.PropertySchema(
                    "propertyName", 
                    "string", 
                    true, 
                    "Property description", 
                    null
                )
            )
        );
    }
}
```

### 3. Register via ServiceLoader

Create `META-INF/services/io.openrudder.core.reaction.spi.ReactionProvider`:

```
com.example.CustomReactionProvider
```

## Dependencies

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.openrudder</groupId>
    <artifactId>openrudder-reactions</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

For AI reactions:
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>
```

For Kafka reactions:
```xml
<dependency>
    <groupId>io.projectreactor.kafka</groupId>
    <artifactId>reactor-kafka</artifactId>
</dependency>
```

## License

Apache License 2.0
