package io.openrudder.core.source;

import io.openrudder.core.model.ChangeEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public abstract class AbstractSource<C extends SourceConfig> implements Source<C> {
    
    protected final String id;
    protected final String name;
    protected final C config;
    protected final AtomicReference<SourceStatus> statusRef;
    protected final Sinks.Many<ChangeEvent> eventSink;

    protected AbstractSource(C config) {
        this.id = UUID.randomUUID().toString();
        this.name = config.getName();
        this.config = config;
        this.statusRef = new AtomicReference<>(SourceStatus.CREATED);
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public C getConfig() {
        return config;
    }

    @Override
    public SourceStatus getStatus() {
        return statusRef.get();
    }

    @Override
    public Flux<ChangeEvent> start() {
        if (!statusRef.compareAndSet(SourceStatus.CREATED, SourceStatus.STARTING) &&
            !statusRef.compareAndSet(SourceStatus.STOPPED, SourceStatus.STARTING)) {
            return Flux.error(new IllegalStateException("Source already started or starting"));
        }

        log.info("Starting source: {}", name);

        return doStart()
            .doOnSubscribe(s -> {
                statusRef.set(SourceStatus.RUNNING);
                log.info("Source started: {}", name);
            })
            .doOnError(e -> {
                statusRef.set(SourceStatus.ERROR);
                log.error("Source error: {}", name, e);
            })
            .doFinally(signal -> {
                if (statusRef.get() == SourceStatus.RUNNING) {
                    statusRef.set(SourceStatus.STOPPED);
                    log.info("Source stopped: {}", name);
                }
            });
    }

    @Override
    public Mono<Void> stop() {
        if (!statusRef.compareAndSet(SourceStatus.RUNNING, SourceStatus.STOPPING)) {
            return Mono.empty();
        }

        log.info("Stopping source: {}", name);
        
        return doStop()
            .doFinally(signal -> {
                statusRef.set(SourceStatus.STOPPED);
                eventSink.tryEmitComplete();
            });
    }

    @Override
    public Flux<ChangeEvent> snapshot() {
        return doSnapshot();
    }

    protected abstract Flux<ChangeEvent> doStart();
    
    protected abstract Mono<Void> doStop();
    
    protected abstract Flux<ChangeEvent> doSnapshot();

    protected void emitEvent(ChangeEvent event) {
        eventSink.tryEmitNext(event);
    }
}
