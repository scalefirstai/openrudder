package io.openrudder.reactions.debug;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.reaction.AbstractReaction;
import io.openrudder.core.reaction.ReactionConfig;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DebugReaction extends AbstractReaction {
    
    private final boolean verbose;
    
    public DebugReaction(ReactionConfig config) {
        super(config);
        this.verbose = (Boolean) config.properties()
            .getOrDefault("verbose", false);
    }
    
    @Override
    protected Mono<Void> doProcessAdded(
            QueryResult result, 
            String queryId, 
            ReactionConfig.QueryConfig queryConfig) {
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("ADDED result in query: " + queryId);
        System.out.println("Result ID: " + result.resultId());
        if (verbose) {
            System.out.println("Data: " + result.data());
        } else {
            System.out.println("Fields: " + result.data().keySet());
        }
        System.out.println("═══════════════════════════════════════");
        
        return Mono.empty();
    }
    
    @Override
    protected Mono<Void> doProcessUpdated(
            QueryResult before,
            QueryResult after,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("UPDATED result in query: " + queryId);
        System.out.println("Result ID: " + after.resultId());
        
        if (verbose) {
            System.out.println("BEFORE: " + before.data());
            System.out.println("AFTER:  " + after.data());
        } else {
            Map<String, Object> changed = getChangedFields(before, after);
            System.out.println("Changed fields: " + changed);
        }
        
        System.out.println("═══════════════════════════════════════");
        
        return Mono.empty();
    }
    
    @Override
    protected Mono<Void> doProcessDeleted(
            QueryResult result,
            String queryId,
            ReactionConfig.QueryConfig queryConfig) {
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("DELETED result in query: " + queryId);
        System.out.println("Result ID: " + result.resultId());
        if (verbose) {
            System.out.println("Data: " + result.data());
        }
        System.out.println("═══════════════════════════════════════");
        
        return Mono.empty();
    }
    
    private Map<String, Object> getChangedFields(
            QueryResult before, 
            QueryResult after) {
        return after.data().entrySet().stream()
            .filter(e -> !e.getValue().equals(before.data().get(e.getKey())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
