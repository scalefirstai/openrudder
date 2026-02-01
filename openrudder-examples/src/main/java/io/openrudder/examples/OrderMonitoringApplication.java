package io.openrudder.examples;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.openrudder.core.query.ContinuousQuery;
import io.openrudder.reactions.ai.LangChain4jReaction;
import io.openrudder.sources.postgres.PostgresSource;
import io.openrudder.spring.boot.autoconfigure.EnableOpenRudder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.Set;

@Slf4j
@SpringBootApplication
@EnableOpenRudder
public class OrderMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderMonitoringApplication.class, args);
    }

    @Bean
    public PostgresSource ordersSource(
            @Value("${postgres.host:localhost}") String host,
            @Value("${postgres.port:5432}") int port,
            @Value("${postgres.database:orders}") String database,
            @Value("${postgres.username:postgres}") String username,
            @Value("${postgres.password:postgres}") String password) {
        
        return PostgresSource.builder()
            .name("orders-source")
            .host(host)
            .port(port)
            .database(database)
            .username(username)
            .password(password)
            .schema("public")
            .table("orders")
            .cdcEnabled(true)
            .build();
    }

    @Bean
    public ContinuousQuery readyOrdersQuery(PostgresSource ordersSource) {
        return ContinuousQuery.builder()
            .id("ready-orders")
            .name("Ready Orders Query")
            .query("""
                MATCH (o:Order)
                WHERE o.status = 'READY_FOR_PICKUP'
                  AND NOT EXISTS(o.driverAssigned)
                RETURN o.id, o.customer, o.location
                """)
            .sourceIds(Set.of(ordersSource.getId()))
            .build();
    }

    @Bean
    public ChatLanguageModel chatModel(@Value("${openai.api.key:demo}") String apiKey) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-3.5-turbo")
            .temperature(0.7)
            .build();
    }

    @Bean
    public LangChain4jReaction orderDispatcherAgent(
            ContinuousQuery readyOrdersQuery,
            ChatLanguageModel chatModel) {
        
        return LangChain4jReaction.builder()
            .name("order-dispatcher")
            .queryId(readyOrdersQuery.getId())
            .model(chatModel)
            .systemPrompt("""
                You are an intelligent order dispatcher AI.
                Your job is to analyze new orders and determine the best driver assignment strategy.
                Consider factors like location, driver availability, and order priority.
                """)
            .userPromptTemplate("""
                New order ready for pickup:
                Order ID: {id}
                Customer: {customer}
                Location: {location}
                
                Analyze this order and suggest the optimal driver assignment.
                """)
            .onResponse(aiMessage -> {
                log.info("AI Dispatcher Response: {}", aiMessage.text());
                return Mono.empty();
            })
            .build();
    }
}
