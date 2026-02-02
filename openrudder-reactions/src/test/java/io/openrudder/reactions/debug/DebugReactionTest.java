package io.openrudder.reactions.debug;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.ReactionConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DebugReactionTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    void shouldLogAddedResultInNonVerboseMode() {
        ReactionConfig config = createConfig(false);
        DebugReaction reaction = new DebugReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();

        String output = outContent.toString();
        assertThat(output).contains("ADDED result in query: query-1");
        assertThat(output).contains("Result ID: result-1");
        assertThat(output).contains("Fields:");
        assertThat(output).contains("id");
        assertThat(output).contains("name");
        assertThat(output).doesNotContain("Data:");
    }

    @Test
    void shouldLogAddedResultInVerboseMode() {
        ReactionConfig config = createConfig(true);
        DebugReaction reaction = new DebugReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessAdded(result, "query-1", null))
            .verifyComplete();

        String output = outContent.toString();
        assertThat(output).contains("ADDED result in query: query-1");
        assertThat(output).contains("Data:");
        assertThat(output).contains("id=1");
        assertThat(output).contains("name=Test");
    }

    @Test
    void shouldLogUpdatedResultInNonVerboseMode() {
        ReactionConfig config = createConfig(false);
        DebugReaction reaction = new DebugReaction(config);

        QueryResult before = createQueryResult("result-1", Map.of("id", 1, "name", "Old"));
        QueryResult after = createQueryResult("result-1", Map.of("id", 1, "name", "New"));

        StepVerifier.create(reaction.doProcessUpdated(before, after, "query-1", null))
            .verifyComplete();

        String output = outContent.toString();
        assertThat(output).contains("UPDATED result in query: query-1");
        assertThat(output).contains("Changed fields:");
    }

    @Test
    void shouldLogUpdatedResultInVerboseMode() {
        ReactionConfig config = createConfig(true);
        DebugReaction reaction = new DebugReaction(config);

        QueryResult before = createQueryResult("result-1", Map.of("id", 1, "name", "Old"));
        QueryResult after = createQueryResult("result-1", Map.of("id", 1, "name", "New"));

        StepVerifier.create(reaction.doProcessUpdated(before, after, "query-1", null))
            .verifyComplete();

        String output = outContent.toString();
        assertThat(output).contains("BEFORE:");
        assertThat(output).contains("name=Old");
        assertThat(output).contains("AFTER:");
        assertThat(output).contains("name=New");
    }

    @Test
    void shouldLogDeletedResultInNonVerboseMode() {
        ReactionConfig config = createConfig(false);
        DebugReaction reaction = new DebugReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessDeleted(result, "query-1", null))
            .verifyComplete();

        String output = outContent.toString();
        assertThat(output).contains("DELETED result in query: query-1");
        assertThat(output).contains("Result ID: result-1");
        assertThat(output).doesNotContain("Data:");
    }

    @Test
    void shouldLogDeletedResultInVerboseMode() {
        ReactionConfig config = createConfig(true);
        DebugReaction reaction = new DebugReaction(config);

        QueryResult result = createQueryResult("result-1", Map.of("id", 1, "name", "Test"));

        StepVerifier.create(reaction.doProcessDeleted(result, "query-1", null))
            .verifyComplete();

        String output = outContent.toString();
        assertThat(output).contains("DELETED result in query: query-1");
        assertThat(output).contains("Data:");
        assertThat(output).contains("id=1");
        assertThat(output).contains("name=Test");
    }

    @Test
    void shouldReturnCorrectMetadata() {
        ReactionConfig config = createConfig(false);
        DebugReaction reaction = new DebugReaction(config);

        assertThat(reaction.id()).isEqualTo("debug-1");
        assertThat(reaction.name()).isEqualTo("Debug Reaction");
        assertThat(reaction.kind()).isEqualTo("debug");
        assertThat(reaction.queryIds()).containsExactly("query-1");
    }

    private ReactionConfig createConfig(boolean verbose) {
        return new ReactionConfig(
            "debug-1",
            "Debug Reaction",
            "debug",
            Set.of("query-1"),
            Map.of(),
            Map.of("verbose", verbose),
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
