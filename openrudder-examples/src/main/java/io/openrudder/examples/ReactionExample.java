package io.openrudder.examples;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.openrudder.core.query.QueryResult;
import io.openrudder.core.query.ResultChange;
import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionBuilder;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.ReactionRegistry;
import io.openrudder.reactions.ai.LangChain4jReaction;
import io.openrudder.reactions.debug.DebugReaction;
import io.openrudder.reactions.http.HttpWebhookReaction;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class ReactionExample {
    
    public static void main(String[] args) {
        demoDebugReaction();
        demoHttpWebhookReaction();
        demoLangChain4jReaction();
        demoReactionRegistry();
    }
    
    public static void demoDebugReaction() {
        System.out.println("\n=== Debug Reaction Demo ===\n");
        
        ReactionConfig config = new ReactionBuilder()
            .id("debug-1")
            .name("Debug Reaction")
            .kind("debug")
            .addQueryId("ready-orders")
            .property("verbose", true)
            .build();
        
        DebugReaction reaction = new DebugReaction(config);
        reaction.start();
        
        QueryResult result = QueryResult.builder()
            .id("order-123")
            .queryId("ready-orders")
            .data(Map.of(
                "orderId", "123",
                "status", "ready",
                "total", 99.99
            ))
            .timestamp(Instant.now())
            .build();
        
        reaction.onResultAdded(result, "ready-orders").block();
        
        System.out.println("\nReaction Stats:");
        System.out.println("  Total Processed: " + reaction.stats().totalProcessed());
        System.out.println("  Health: " + reaction.health().status());
        
        reaction.stop();
    }
    
    public static void demoHttpWebhookReaction() {
        System.out.println("\n=== HTTP Webhook Reaction Demo ===\n");
        
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
        
        System.out.println("Created HTTP webhook reaction: " + reaction.name());
        System.out.println("  ID: " + reaction.id());
        System.out.println("  Kind: " + reaction.kind());
        System.out.println("  Query IDs: " + reaction.queryIds());
        System.out.println("  Webhook URL: " + reaction.properties().get("webhookUrl"));
        
        reaction.stop();
    }
    
    public static void demoLangChain4jReaction() {
        System.out.println("\n=== LangChain4j AI Reaction Demo ===\n");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            System.out.println("Skipping AI demo - OPENAI_API_KEY not set");
            return;
        }
        
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
                return reactor.core.publisher.Mono.empty();
            })
            .build();
        
        reaction.start();
        
        QueryResult result = QueryResult.builder()
            .id("order-456")
            .queryId("ready-orders")
            .data(Map.of(
                "orderId", "456",
                "status", "ready",
                "total", 149.99
            ))
            .timestamp(Instant.now())
            .build();
        
        System.out.println("Processing order with AI...");
        reaction.onResultAdded(result, "ready-orders").block();
        
        reaction.stop();
    }
    
    public static void demoReactionRegistry() {
        System.out.println("\n=== Reaction Registry Demo ===\n");
        
        ReactionRegistry registry = new ReactionRegistry();
        
        System.out.println("Available Reaction Providers:");
        registry.getProviders().forEach((kind, provider) -> {
            System.out.println("  - " + kind + ": " + provider.configSchema().description());
        });
        
        ReactionConfig config = new ReactionBuilder()
            .id("debug-registry")
            .name("Registry Debug Reaction")
            .kind("debug")
            .addQueryId("test-query")
            .property("verbose", false)
            .build();
        
        Reaction reaction = registry.create(config).block();
        System.out.println("\nCreated reaction via registry: " + reaction.name());
        System.out.println("  Running: " + reaction.isRunning());
        
        registry.delete(reaction.id()).block();
        System.out.println("Deleted reaction from registry");
    }
    
    public static void demoReactionWithRetry() {
        System.out.println("\n=== Reaction with Retry Demo ===\n");
        
        ReactionConfig.RetryConfig retryConfig = new ReactionConfig.RetryConfig(
            true,
            3,
            1000,
            10000,
            2.0,
            Set.of(java.io.IOException.class)
        );
        
        ReactionConfig config = new ReactionConfig(
            "retry-1",
            "Retry Reaction",
            "debug",
            Set.of("test-query"),
            Map.of(),
            Map.of("verbose", false),
            ReactionConfig.ExecutionConfig.defaults(),
            retryConfig,
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );
        
        DebugReaction reaction = new DebugReaction(config);
        reaction.start();
        
        System.out.println("Created reaction with retry config:");
        System.out.println("  Max Attempts: " + retryConfig.maxAttempts());
        System.out.println("  Initial Backoff: " + retryConfig.initialBackoffMs() + "ms");
        System.out.println("  Max Backoff: " + retryConfig.maxBackoffMs() + "ms");
        
        reaction.stop();
    }
}
