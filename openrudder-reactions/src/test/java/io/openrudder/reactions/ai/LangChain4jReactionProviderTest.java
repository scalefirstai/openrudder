package io.openrudder.reactions.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LangChain4jReactionProviderTest {

    @Test
    void shouldReturnCorrectKind() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        LangChain4jReactionProvider provider = new LangChain4jReactionProvider(model);
        
        assertThat(provider.kind()).isEqualTo("langchain4j");
    }

    @Test
    void shouldCreateLangChain4jReaction() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        LangChain4jReactionProvider provider = new LangChain4jReactionProvider(model);
        ReactionConfig config = createConfig();

        Reaction reaction = provider.create(config);

        assertThat(reaction).isInstanceOf(LangChain4jReaction.class);
        assertThat(reaction.id()).isEqualTo("ai-1");
    }

    @Test
    void shouldValidateSuccessfullyWithModel() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        LangChain4jReactionProvider provider = new LangChain4jReactionProvider(model);
        ReactionConfig config = createConfig();

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldFailValidationWhenModelIsNull() {
        LangChain4jReactionProvider provider = new LangChain4jReactionProvider(null);
        ReactionConfig config = createConfig();

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("ChatLanguageModel is required");
    }

    @Test
    void shouldReturnConfigSchema() {
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        LangChain4jReactionProvider provider = new LangChain4jReactionProvider(model);

        ConfigSchema schema = provider.configSchema();

        assertThat(schema.kind()).isEqualTo("langchain4j");
        assertThat(schema.description()).contains("LangChain4j");
        assertThat(schema.properties()).hasSize(2);
        assertThat(schema.properties()).anyMatch(p -> p.name().equals("systemPrompt"));
        assertThat(schema.properties()).anyMatch(p -> p.name().equals("userPromptTemplate"));
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
                "userPromptTemplate", "Process this: {data}"
            ),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );
    }
}
