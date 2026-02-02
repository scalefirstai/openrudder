package io.openrudder.reactions.http;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.ReactionConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HttpWebhookReactionTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldSendWebhookForAddedResult() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String webhookUrl = mockWebServer.url("/webhook").toString();
        ReactionConfig config = createConfig(webhookUrl, "POST");
        HttpWebhookReaction reaction = new HttpWebhookReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(body).contains("\"queryId\":\"query-1\"");
        assertThat(body).contains("\"changeType\":\"added\"");
    }

    @Test
    void shouldSendWebhookForUpdatedResult() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String webhookUrl = mockWebServer.url("/webhook").toString();
        ReactionConfig config = createConfig(webhookUrl, "POST");
        HttpWebhookReaction reaction = new HttpWebhookReaction(config);

        QueryResult before = createQueryResult("result-1", Map.of("id", 1, "name", "Old"));
        QueryResult after = createQueryResult("result-1", Map.of("id", 1, "name", "New"));

        StepVerifier.create(reaction.doProcessUpdated(before, after, "query-1", null))
            .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(body).contains("\"changeType\":\"updated\"");
    }

    @Test
    void shouldSendWebhookForDeletedResult() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String webhookUrl = mockWebServer.url("/webhook").toString();
        ReactionConfig config = createConfig(webhookUrl, "POST");
        HttpWebhookReaction reaction = new HttpWebhookReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessDeleted(result, "query-1", null))
            .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(body).contains("\"changeType\":\"deleted\"");
    }

    @Test
    void shouldUseCustomHttpMethod() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String webhookUrl = mockWebServer.url("/webhook").toString();
        ReactionConfig config = createConfig(webhookUrl, "PUT");
        HttpWebhookReaction reaction = new HttpWebhookReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("PUT");
    }

    @Test
    void shouldIncludeCustomHeaders() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String webhookUrl = mockWebServer.url("/webhook").toString();
        Map<String, String> headers = Map.of("X-Custom-Header", "CustomValue");
        ReactionConfig config = createConfigWithHeaders(webhookUrl, "POST", headers);
        HttpWebhookReaction reaction = new HttpWebhookReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("X-Custom-Header")).isEqualTo("CustomValue");
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

    private ReactionConfig createConfigWithHeaders(String webhookUrl, String method, Map<String, String> headers) {
        return new ReactionConfig(
            "http-1",
            "HTTP Webhook",
            "http",
            Set.of("query-1"),
            Map.of(),
            Map.of("webhookUrl", webhookUrl, "method", method, "headers", headers),
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
