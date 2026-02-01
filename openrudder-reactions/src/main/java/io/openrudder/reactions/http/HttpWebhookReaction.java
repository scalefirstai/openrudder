package io.openrudder.reactions.http;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.AbstractReaction;
import io.openrudder.core.reaction.ReactionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Slf4j
public class HttpWebhookReaction extends AbstractReaction {
    
    private final WebClient webClient;
    private final String webhookUrl;
    private final HttpMethod method;
    private final Map<String, String> headers;
    
    public HttpWebhookReaction(ReactionConfig config) {
        super(config);
        
        this.webhookUrl = (String) config.properties().get("webhookUrl");
        String methodStr = (String) config.properties().getOrDefault("method", "POST");
        this.method = HttpMethod.valueOf(methodStr.toUpperCase());
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
            ReactionConfig.QueryConfig queryConfig) {
        
        return webClient
            .method(method)
            .uri("")
            .bodyValue(createPayload(result, queryId, "added"))
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Sent webhook for ADDED result: {}", result.resultId()))
            .doOnError(e -> log.error("Failed to send webhook: {}", e.getMessage()));
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        
        return webClient
            .method(method)
            .uri("")
            .bodyValue(createUpdatePayload(before, after, queryId))
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Sent webhook for UPDATED result: {}", after.resultId()))
            .doOnError(e -> log.error("Failed to send webhook: {}", e.getMessage()));
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        
        return webClient
            .method(method)
            .uri("")
            .bodyValue(createPayload(result, queryId, "deleted"))
            .retrieve()
            .bodyToMono(Void.class)
            .doOnSuccess(v -> log.debug("Sent webhook for DELETED result: {}", result.resultId()))
            .doOnError(e -> log.error("Failed to send webhook: {}", e.getMessage()));
    }
    
    private Map<String, Object> createPayload(
            QueryResult result, 
            String queryId, 
            String changeType) {
        return Map.of(
            "queryId", queryId,
            "changeType", changeType,
            "result", result.data(),
            "timestamp", result.getTimestamp() != null ? result.getTimestamp() : Instant.now()
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
            "timestamp", after.getTimestamp() != null ? after.getTimestamp() : Instant.now()
        );
    }
}
