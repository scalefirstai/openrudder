package io.openrudder.reactions.webhook;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.AbstractReaction;
import io.openrudder.core.reaction.ReactionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WebhookReaction extends AbstractReaction {

    private final WebClient webClient;
    private final String webhookUrl;
    private final String method;
    private final Map<String, String> headers;
    private final int timeoutSeconds;

    public WebhookReaction(ReactionConfig config) {
        super(config);
        this.webhookUrl = (String) config.properties().get("webhookUrl");
        this.method = (String) config.properties().getOrDefault("method", "POST");
        this.headers = (Map<String, String>) config.properties().getOrDefault("headers", Map.of());
        this.timeoutSeconds = ((Number) config.properties().getOrDefault("timeoutSeconds", 30)).intValue();
        this.webClient = WebClient.builder()
            .baseUrl(webhookUrl)
            .build();
    }

    public static WebhookReactionBuilder builder() {
        return new WebhookReactionBuilder();
    }

    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        return sendWebhook(result, queryId, "added");
    }

    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        return sendWebhook(after, queryId, "updated");
    }

    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        return sendWebhook(result, queryId, "deleted");
    }

    private Mono<Void> sendWebhook(QueryResult result, String queryId, String changeType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("queryId", queryId);
        payload.put("changeType", changeType);
        payload.put("data", result.data());
        payload.put("timestamp", result.getTimestamp());

        return webClient.method(org.springframework.http.HttpMethod.valueOf(method.toUpperCase()))
            .uri("")
            .contentType(MediaType.APPLICATION_JSON)
            .headers(httpHeaders -> headers.forEach(httpHeaders::add))
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .doOnSuccess(response -> log.info("Webhook called successfully for query {}: {}", queryId, response))
            .doOnError(e -> log.error("Webhook call failed for query {}", queryId, e))
            .then();
    }

    public static class WebhookReactionBuilder {
        private String id;
        private String name;
        private java.util.Set<String> queryIds = new java.util.HashSet<>();
        private String webhookUrl;
        private String method = "POST";
        private Map<String, String> headers = new HashMap<>();
        private int timeoutSeconds = 30;

        public WebhookReactionBuilder id(String id) {
            this.id = id;
            return this;
        }

        public WebhookReactionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public WebhookReactionBuilder queryId(String queryId) {
            this.queryIds.add(queryId);
            return this;
        }

        public WebhookReactionBuilder queryIds(java.util.Set<String> queryIds) {
            this.queryIds = queryIds;
            return this;
        }

        public WebhookReactionBuilder webhookUrl(String url) {
            this.webhookUrl = url;
            return this;
        }

        public WebhookReactionBuilder method(String method) {
            this.method = method;
            return this;
        }

        public WebhookReactionBuilder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public WebhookReactionBuilder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public WebhookReaction build() {
            if (id == null) {
                id = java.util.UUID.randomUUID().toString();
            }
            if (name == null) {
                name = "Webhook Reaction";
            }

            Map<String, Object> properties = new HashMap<>();
            properties.put("webhookUrl", webhookUrl);
            properties.put("method", method);
            properties.put("headers", headers);
            properties.put("timeoutSeconds", timeoutSeconds);

            ReactionConfig config = new ReactionConfig(
                id,
                name,
                "http",
                queryIds,
                Map.of(),
                properties,
                ReactionConfig.ExecutionConfig.defaults(),
                ReactionConfig.RetryConfig.defaults(),
                new ReactionConfig.SecurityConfig(Map.of(), Map.of())
            );
            
            return new WebhookReaction(config);
        }
    }
}
