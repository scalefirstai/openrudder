package io.openrudder.reactions.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.reaction.ReactionConfig;
import io.openrudder.core.reaction.spi.ConfigSchema;
import io.openrudder.core.reaction.spi.ReactionProvider;
import io.openrudder.core.reaction.spi.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class LangChain4jReactionProvider implements ReactionProvider {
    
    private final ChatLanguageModel model;
    
    public LangChain4jReactionProvider(ChatLanguageModel model) {
        this.model = model;
    }
    
    @Override
    public String kind() {
        return "langchain4j";
    }
    
    @Override
    public Reaction create(ReactionConfig config) {
        String systemPrompt = (String) config.properties().getOrDefault(
            "systemPrompt", "You are a helpful assistant.");
        String userPromptTemplate = (String) config.properties().get("userPromptTemplate");
        
        return new LangChain4jReaction(
            config,
            model,
            systemPrompt,
            userPromptTemplate,
            null
        );
    }
    
    @Override
    public ValidationResult validate(ReactionConfig config) {
        List<String> errors = new ArrayList<>();
        
        if (model == null) {
            errors.add("ChatLanguageModel is required");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.ofValid() 
            : ValidationResult.ofInvalid(errors.toArray(String[]::new));
    }
    
    @Override
    public ConfigSchema configSchema() {
        return new ConfigSchema(
            "langchain4j",
            "LangChain4j AI agent reaction - invokes AI models to process query results",
            List.of(
                new ConfigSchema.PropertySchema("systemPrompt", "string", false, 
                    "System prompt for the AI model", "You are a helpful assistant."),
                new ConfigSchema.PropertySchema("userPromptTemplate", "string", false, 
                    "User prompt template with placeholders like {fieldName}, {queryId}, {changeType}", null)
            )
        );
    }
}
