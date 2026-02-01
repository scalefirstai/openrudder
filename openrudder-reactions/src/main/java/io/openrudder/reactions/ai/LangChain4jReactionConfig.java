package io.openrudder.reactions.ai;

import io.openrudder.core.reaction.ReactionConfig;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class LangChain4jReactionConfig implements ReactionConfig {
    String name;
    Set<String> queryIds;
    String systemPrompt;
    String userPromptTemplate;
    @Builder.Default
    boolean enabled = true;
    @Builder.Default
    double temperature = 0.7;
    @Builder.Default
    int maxTokens = 1000;
}
