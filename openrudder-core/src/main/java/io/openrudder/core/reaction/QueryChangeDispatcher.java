package io.openrudder.core.reaction;

import io.openrudder.core.query.ContinuousQuery;
import io.openrudder.core.query.ResultChange;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class QueryChangeDispatcher {
    
    private final ReactionRegistry reactionRegistry;
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    
    public QueryChangeDispatcher(ReactionRegistry reactionRegistry) {
        this.reactionRegistry = reactionRegistry;
    }
    
    public void subscribe(String queryId, String reactionId) {
        subscriptions
            .computeIfAbsent(queryId, k -> ConcurrentHashMap.newKeySet())
            .add(reactionId);
        log.debug("Subscribed reaction {} to query {}", reactionId, queryId);
    }
    
    public void unsubscribe(String queryId, String reactionId) {
        Set<String> reactionIds = subscriptions.get(queryId);
        if (reactionIds != null) {
            reactionIds.remove(reactionId);
            log.debug("Unsubscribed reaction {} from query {}", reactionId, queryId);
        }
    }
    
    public Flux<Void> connectToQuery(ContinuousQuery query) {
        return query.initialEvaluation()
            .flatMap(result -> {
                ResultChange change = ResultChange.builder()
                    .queryId(query.getId())
                    .type(ResultChange.ChangeType.ADDED)
                    .before(null)
                    .after(result)
                    .timestamp(Instant.now())
                    .sourceChange(null)
                    .build();
                return dispatchChange(change).flux();
            });
    }
    
    public Mono<Void> dispatchChange(ResultChange change) {
        String queryId = change.getQueryId();
        Set<String> reactionIds = subscriptions.get(queryId);
        
        if (reactionIds == null || reactionIds.isEmpty()) {
            return Mono.empty();
        }
        
        return Flux.fromIterable(reactionIds)
            .flatMap(reactionId -> 
                reactionRegistry.get(reactionId)
                    .flatMap(reaction -> 
                        reaction.processChange(change)
                            .onErrorResume(error -> {
                                log.error("Error in reaction {}: {}", reactionId, error.getMessage(), error);
                                return Mono.empty();
                            })
                    )
            )
            .then();
    }
}
