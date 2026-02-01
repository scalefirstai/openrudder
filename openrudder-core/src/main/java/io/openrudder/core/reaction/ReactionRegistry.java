package io.openrudder.core.reaction;

import io.openrudder.core.reaction.spi.ReactionProvider;
import io.openrudder.core.reaction.spi.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ReactionRegistry {
    
    private final Map<String, Reaction> reactions = new ConcurrentHashMap<>();
    private final Map<String, ReactionProvider> providers = new ConcurrentHashMap<>();
    
    public ReactionRegistry() {
        ServiceLoader.load(ReactionProvider.class)
            .forEach(provider -> {
                providers.put(provider.kind(), provider);
                log.info("Registered reaction provider: {}", provider.kind());
            });
    }
    
    public Mono<Reaction> create(ReactionConfig config) {
        return Mono.fromCallable(() -> {
            ReactionProvider provider = providers.get(config.kind());
            if (provider == null) {
                throw new IllegalArgumentException(
                    "No provider found for reaction kind: " + config.kind());
            }
            
            ValidationResult validation = provider.validate(config);
            if (!validation.valid()) {
                throw new IllegalArgumentException(
                    "Invalid configuration: " + String.join(", ", validation.errors()));
            }
            
            Reaction reaction = provider.create(config);
            
            reactions.put(reaction.id(), reaction);
            reaction.start();
            
            log.info("Created and started reaction: {} ({})", reaction.name(), reaction.kind());
            
            return reaction;
        });
    }
    
    public Mono<Reaction> get(String id) {
        return Mono.justOrEmpty(reactions.get(id));
    }
    
    public Flux<Reaction> list() {
        return Flux.fromIterable(reactions.values());
    }
    
    public Mono<Void> delete(String id) {
        return Mono.fromRunnable(() -> {
            Reaction reaction = reactions.remove(id);
            if (reaction != null) {
                reaction.stop();
                log.info("Deleted reaction: {}", reaction.name());
            }
        });
    }
    
    public Map<String, ReactionProvider> getProviders() {
        return Map.copyOf(providers);
    }
}
