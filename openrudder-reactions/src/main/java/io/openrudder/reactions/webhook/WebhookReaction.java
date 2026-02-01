package io.openrudder.reactions.webhook;

import io.openrudder.core.query.ResultUpdate;
import io.openrudder.core.reaction.AbstractReaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public class WebhookReaction extends AbstractReaction {

    private final WebhookReactionConfig reactionConfig;
    private final WebClient webClient;

    public WebhookReaction(WebhookReactionConfig config) {
        super(config);
        this.reactionConfig = config;
        this.webClient = WebClient.builder()
            .baseUrl(config.getUrl())
            .build();
    }

    public static WebhookReactionBuilder builder() {
        return new WebhookReactionBuilder();
    }

    @Override
    protected Mono<Void> doOnResultUpdate(ResultUpdate update) {
        return webClient.post()
            .uri("")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(update)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(reactionConfig.getTimeoutSeconds()))
            .doOnSuccess(response -> log.info("Webhook called successfully: {}", response))
            .doOnError(e -> log.error("Webhook call failed", e))
            .then();
    }

    public static class WebhookReactionBuilder {
        private String name;
        private java.util.Set<String> queryIds;
        private String url;
        private String method = "POST";

        public WebhookReactionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public WebhookReactionBuilder queryId(String queryId) {
            if (this.queryIds == null) {
                this.queryIds = new java.util.HashSet<>();
            }
            this.queryIds.add(queryId);
            return this;
        }

        public WebhookReactionBuilder url(String url) {
            this.url = url;
            return this;
        }

        public WebhookReaction build() {
            WebhookReactionConfig config = WebhookReactionConfig.builder()
                .name(name)
                .queryIds(queryIds)
                .url(url)
                .method(method)
                .build();
            
            return new WebhookReaction(config);
        }
    }
}
