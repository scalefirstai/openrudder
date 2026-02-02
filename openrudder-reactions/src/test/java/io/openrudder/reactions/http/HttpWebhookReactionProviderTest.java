package io.openrudder.reactions.http;

import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HttpWebhookReactionProviderTest {

    @Test
    void shouldReturnCorrectKind() {
        HttpWebhookReactionProvider provider = new HttpWebhookReactionProvider();
        assertThat(provider.kind()).isEqualTo("http");
    }

    @Test
    void shouldCreateHttpWebhookReaction() {
        HttpWebhookReactionProvider provider = new HttpWebhookReactionProvider();
        ReactionConfig config = createConfig("https://example.com/webhook", "POST");

        Reaction reaction = provider.create(config);

        assertThat(reaction).isInstanceOf(HttpWebhookReaction.class);
        assertThat(reaction.id()).isEqualTo("http-1");
    }

    @Test
    void shouldValidateSuccessfullyWithValidConfig() {
        HttpWebhookReactionProvider provider = new HttpWebhookReactionProvider();
        ReactionConfig config = createConfig("https://example.com/webhook", "POST");

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldFailValidationWhenWebhookUrlMissing() {
        HttpWebhookReactionProvider provider = new HttpWebhookReactionProvider();
        ReactionConfig config = new ReactionConfig(
            "http-1",
            "HTTP Webhook",
            "http",
            Set.of("query-1"),
            Map.of(),
            Map.of(),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("webhookUrl is required");
    }

    @Test
    void shouldReturnConfigSchema() {
        HttpWebhookReactionProvider provider = new HttpWebhookReactionProvider();

        ConfigSchema schema = provider.configSchema();

        assertThat(schema.kind()).isEqualTo("http");
        assertThat(schema.description()).contains("HTTP webhook");
        assertThat(schema.properties()).hasSize(3);
        assertThat(schema.properties()).anyMatch(p -> p.name().equals("webhookUrl") && p.required());
        assertThat(schema.properties()).anyMatch(p -> p.name().equals("method") && !p.required());
        assertThat(schema.properties()).anyMatch(p -> p.name().equals("headers") && !p.required());
    }

    private ReactionConfig createConfig(String webhookUrl, String method) {
        return new ReactionConfig(
            "http-1",
            "HTTP Webhook",
            "http",
            Set.of("query-1"),
            Map.of(),
            Map.of("webhookUrl", webhookUrl, "method", method),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );
    }
}
