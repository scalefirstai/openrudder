package io.openrudder.reactions.webhook;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.ReactionConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookReactionTest {

    @Test
    void shouldCreateWebhookReactionWithConfig() {
        ReactionConfig config = createConfig("https://example.com/webhook");
        WebhookReaction reaction = new WebhookReaction(config);

        assertThat(reaction.id()).isEqualTo("webhook-1");
        assertThat(reaction.name()).isEqualTo("Webhook Reaction");
        assertThat(reaction.kind()).isEqualTo("http");
    }

    @Test
    void shouldBuildWebhookReactionWithBuilder() {
        WebhookReaction reaction = WebhookReaction.builder()
            .id("custom-id")
            .name("Custom Webhook")
            .queryId("query-1")
            .webhookUrl("https://example.com/webhook")
            .method("POST")
            .timeoutSeconds(60)
            .build();

        assertThat(reaction.id()).isEqualTo("custom-id");
        assertThat(reaction.name()).isEqualTo("Custom Webhook");
        assertThat(reaction.queryIds()).containsExactly("query-1");
    }

    @Test
    void shouldGenerateIdWhenNotProvided() {
        WebhookReaction reaction = WebhookReaction.builder()
            .webhookUrl("https://example.com/webhook")
            .build();

        assertThat(reaction.id()).isNotNull();
        assertThat(reaction.name()).isEqualTo("Webhook Reaction");
    }

    @Test
    void shouldSupportMultipleQueryIds() {
        WebhookReaction reaction = WebhookReaction.builder()
            .webhookUrl("https://example.com/webhook")
            .queryIds(Set.of("query-1", "query-2", "query-3"))
            .build();

        assertThat(reaction.queryIds()).containsExactlyInAnyOrder("query-1", "query-2", "query-3");
    }

    @Test
    void shouldUseDefaultMethod() {
        WebhookReaction reaction = WebhookReaction.builder()
            .webhookUrl("https://example.com/webhook")
            .build();

        assertThat(reaction.properties().get("method")).isEqualTo("POST");
    }

    @Test
    void shouldUseDefaultTimeout() {
        WebhookReaction reaction = WebhookReaction.builder()
            .webhookUrl("https://example.com/webhook")
            .build();

        assertThat(reaction.properties().get("timeoutSeconds")).isEqualTo(30);
    }

    @Test
    void shouldSupportCustomHeaders() {
        Map<String, String> headers = Map.of("Authorization", "Bearer token", "X-Custom", "value");
        
        WebhookReaction reaction = WebhookReaction.builder()
            .webhookUrl("https://example.com/webhook")
            .headers(headers)
            .build();

        assertThat(reaction.properties().get("headers")).isEqualTo(headers);
    }

    private ReactionConfig createConfig(String webhookUrl) {
        return new ReactionConfig(
            "webhook-1",
            "Webhook Reaction",
            "http",
            Set.of("query-1"),
            Map.of(),
            Map.of(
                "webhookUrl", webhookUrl,
                "method", "POST",
                "timeoutSeconds", 30
            ),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );
    }

    private QueryResult createQueryResult(String resultId, Map<String, Object> data) {
        return QueryResult.builder()
            .id(resultId)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
}
