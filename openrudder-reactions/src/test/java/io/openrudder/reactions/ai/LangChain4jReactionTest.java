package io.openrudder.reactions.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.ReactionConfig;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LangChain4jReactionTest {

    @Test
    void shouldProcessAddedResult() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiMessage aiMessage = AiMessage.from("AI response");
        Response<AiMessage> response = Response.from(aiMessage);
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        ReactionConfig config = createConfig();
        LangChain4jReaction reaction = new LangChain4jReaction(
            config, model, "System prompt", "User: {name}", null
        );

        QueryResult result = createQueryResult("result-1", Map.of("name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldProcessUpdatedResult() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiMessage aiMessage = AiMessage.from("AI response");
        Response<AiMessage> response = Response.from(aiMessage);
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        ReactionConfig config = createConfig();
        LangChain4jReaction reaction = new LangChain4jReaction(
            config, model, "System prompt", null, null
        );

        QueryResult before = createQueryResult("result-1", Map.of("name", "Old"));
        QueryResult after = createQueryResult("result-1", Map.of("name", "New"));

        StepVerifier.create(reaction.doProcessUpdated(before, after, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldProcessDeletedResult() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiMessage aiMessage = AiMessage.from("AI response");
        Response<AiMessage> response = Response.from(aiMessage);
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        ReactionConfig config = createConfig();
        LangChain4jReaction reaction = new LangChain4jReaction(
            config, model, "System prompt", null, null
        );

        QueryResult result = createQueryResult("result-1", Map.of("name", "Test"));

        StepVerifier.create(reaction.doProcessDeleted(result, "query-1", null))
            .verifyComplete();
    }

    @Test
    void shouldInvokeResponseHandler() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiMessage aiMessage = AiMessage.from("AI response");
        Response<AiMessage> response = Response.from(aiMessage);
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        AtomicReference<AiMessage> capturedMessage = new AtomicReference<>();
        
        ReactionConfig config = createConfig();
        LangChain4jReaction reaction = new LangChain4jReaction(
            config, 
            model, 
            "System prompt", 
            null, 
            msg -> {
                capturedMessage.set(msg);
                return Mono.empty();
            }
        );

        QueryResult result = createQueryResult("result-1", Map.of("name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();

        assertThat(capturedMessage.get()).isNotNull();
        assertThat(capturedMessage.get().text()).isEqualTo("AI response");
    }

    @Test
    void shouldBuildReactionWithBuilder() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        
        LangChain4jReaction reaction = LangChain4jReaction.builder()
            .id("custom-id")
            .name("Custom AI Reaction")
            .queryId("query-1")
            .systemPrompt("Custom system prompt")
            .userPromptTemplate("Custom template")
            .model(model)
            .build();

        assertThat(reaction.id()).isEqualTo("custom-id");
        assertThat(reaction.name()).isEqualTo("Custom AI Reaction");
        assertThat(reaction.queryIds()).containsExactly("query-1");
    }

    @Test
    void shouldGenerateIdWhenNotProvided() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        
        LangChain4jReaction reaction = LangChain4jReaction.builder()
            .model(model)
            .build();

        assertThat(reaction.id()).isNotNull();
        assertThat(reaction.name()).isEqualTo("LangChain4j Reaction");
    }

    @Test
    void shouldFormatPromptWithPlaceholders() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        AiMessage aiMessage = AiMessage.from("AI response");
        Response<AiMessage> response = Response.from(aiMessage);
        when(model.generate(any(UserMessage.class))).thenReturn(response);

        ReactionConfig config = createConfig();
        LangChain4jReaction reaction = new LangChain4jReaction(
            config, 
            model, 
            "System", 
            "Query: {queryId}, Type: {changeType}, Name: {name}", 
            null
        );

        QueryResult result = createQueryResult("result-1", Map.of("name", "TestName"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-123", null))
            .verifyComplete();
    }

    private ReactionConfig createConfig() {
        return new ReactionConfig(
            "ai-1",
            "AI Reaction",
            "langchain4j",
            Set.of("query-1"),
            Map.of(),
            Map.of(
                "systemPrompt", "You are a helpful assistant.",
                "userPromptTemplate", "Process this data"
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
