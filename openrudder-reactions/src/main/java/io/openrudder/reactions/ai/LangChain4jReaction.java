package io.openrudder.reactions.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.AbstractReaction;
import io.openrudder.core.reaction.ReactionConfig;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.function.Function;

@Slf4j
public class LangChain4jReaction extends AbstractReaction {

    private final ChatLanguageModel model;
    private final String systemPrompt;
    private final String userPromptTemplate;
    private final Function<AiMessage, Mono<Void>> responseHandler;

    public LangChain4jReaction(
            ReactionConfig config,
            ChatLanguageModel model,
            String systemPrompt,
            String userPromptTemplate,
            Function<AiMessage, Mono<Void>> responseHandler) {
        super(config);
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.userPromptTemplate = userPromptTemplate;
        this.responseHandler = responseHandler;
    }

    public static LangChain4jReactionBuilder builder() {
        return new LangChain4jReactionBuilder();
    }

    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        return invokeAI(result, queryId, "added");
    }

    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        return invokeAI(after, queryId, "updated");
    }

    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        return invokeAI(result, queryId, "deleted");
    }

    private Mono<Void> invokeAI(
            QueryResult result,
            String queryId,
            String changeType) {
        return Mono.fromCallable(() -> {
            String prompt = formatPrompt(result, queryId, changeType);
            log.debug("Sending prompt to AI: {}", prompt);
            
            Response<AiMessage> response = model.generate(UserMessage.from(prompt));
            return response.content();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(aiMessage -> {
            log.info("AI response: {}", aiMessage.text());
            if (responseHandler != null) {
                return responseHandler.apply(aiMessage);
            }
            return Mono.empty();
        })
        .doOnError(e -> log.error("Error processing AI reaction", e))
        .then();
    }

    private String formatPrompt(
            QueryResult result,
            String queryId,
            String changeType) {
        StringBuilder prompt = new StringBuilder();
        
        if (systemPrompt != null) {
            prompt.append(systemPrompt).append("\n\n");
        }
        
        if (userPromptTemplate != null) {
            String formatted = userPromptTemplate;
            formatted = formatted.replace("{queryId}", queryId);
            formatted = formatted.replace("{changeType}", changeType);
            
            for (Map.Entry<String, Object> entry : result.data().entrySet()) {
                formatted = formatted.replace(
                    "{" + entry.getKey() + "}",
                    String.valueOf(entry.getValue())
                );
            }
            
            prompt.append(formatted);
        } else {
            prompt.append(buildDefaultPrompt(result, queryId, changeType));
        }
        
        return prompt.toString();
    }

    private String buildDefaultPrompt(
            QueryResult result,
            String queryId,
            String changeType) {
        return String.format(
            "Change Type: %s\nQuery ID: %s\nData: %s",
            changeType,
            queryId,
            result.data()
        );
    }

    public static class LangChain4jReactionBuilder {
        private String id;
        private String name;
        private java.util.Set<String> queryIds = new java.util.HashSet<>();
        private String systemPrompt = "You are a helpful assistant.";
        private String userPromptTemplate;
        private ChatLanguageModel model;
        private Function<AiMessage, Mono<Void>> responseHandler;
        private java.util.Map<String, Object> properties = new java.util.HashMap<>();

        public LangChain4jReactionBuilder id(String id) {
            this.id = id;
            return this;
        }

        public LangChain4jReactionBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LangChain4jReactionBuilder queryId(String queryId) {
            this.queryIds.add(queryId);
            return this;
        }

        public LangChain4jReactionBuilder queryIds(java.util.Set<String> queryIds) {
            this.queryIds = queryIds;
            return this;
        }

        public LangChain4jReactionBuilder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public LangChain4jReactionBuilder userPromptTemplate(String template) {
            this.userPromptTemplate = template;
            return this;
        }

        public LangChain4jReactionBuilder model(ChatLanguageModel model) {
            this.model = model;
            return this;
        }

        public LangChain4jReactionBuilder onResponse(Function<AiMessage, Mono<Void>> handler) {
            this.responseHandler = handler;
            return this;
        }

        public LangChain4jReactionBuilder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public LangChain4jReaction build() {
            if (id == null) {
                id = java.util.UUID.randomUUID().toString();
            }
            if (name == null) {
                name = "LangChain4j Reaction";
            }
            
            properties.put("systemPrompt", systemPrompt);
            properties.put("userPromptTemplate", userPromptTemplate);
            
            ReactionConfig config = new ReactionConfig(
                id,
                name,
                "langchain4j",
                queryIds,
                java.util.Map.of(),
                properties,
                ReactionConfig.ExecutionConfig.defaults(),
                ReactionConfig.RetryConfig.defaults(),
                new ReactionConfig.SecurityConfig(java.util.Map.of(), java.util.Map.of())
            );
            
            return new LangChain4jReaction(config, model, systemPrompt, userPromptTemplate, responseHandler);
        }
    }
}
