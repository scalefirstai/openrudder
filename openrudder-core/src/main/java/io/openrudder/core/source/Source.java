package io.openrudder.core.source;

import io.openrudder.core.model.ChangeEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Source<C extends SourceConfig> {
    
    String getId();
    
    String getName();
    
    C getConfig();
    
    Flux<ChangeEvent> start();
    
    Mono<Void> stop();
    
    Flux<ChangeEvent> snapshot();
    
    SourceStatus getStatus();
    
    enum SourceStatus {
        CREATED,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        ERROR
    }
}
