package io.openrudder.reactions.debug;

import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DebugReactionProviderTest {

    @Test
    void shouldReturnCorrectKind() {
        DebugReactionProvider provider = new DebugReactionProvider();
        assertThat(provider.kind()).isEqualTo("debug");
    }

    @Test
    void shouldCreateDebugReaction() {
        DebugReactionProvider provider = new DebugReactionProvider();
        ReactionConfig config = createConfig();

        Reaction reaction = provider.create(config);

        assertThat(reaction).isInstanceOf(DebugReaction.class);
        assertThat(reaction.id()).isEqualTo("debug-1");
    }

    @Test
    void shouldValidateSuccessfully() {
        DebugReactionProvider provider = new DebugReactionProvider();
        ReactionConfig config = createConfig();

        ValidationResult result = provider.validate(config);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void shouldReturnConfigSchema() {
        DebugReactionProvider provider = new DebugReactionProvider();

        ConfigSchema schema = provider.configSchema();

        assertThat(schema.kind()).isEqualTo("debug");
        assertThat(schema.description()).contains("Debug reaction");
        assertThat(schema.properties()).hasSize(1);
        assertThat(schema.properties().get(0).name()).isEqualTo("verbose");
    }

    private ReactionConfig createConfig() {
        return new ReactionConfig(
            "debug-1",
            "Debug Reaction",
            "debug",
            Set.of("query-1"),
            Map.of(),
            Map.of("verbose", false),
            ReactionConfig.ExecutionConfig.defaults(),
            ReactionConfig.RetryConfig.defaults(),
            new ReactionConfig.SecurityConfig(Map.of(), Map.of())
        );
    }
}
