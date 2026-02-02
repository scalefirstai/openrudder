package io.openrudder.core.engine;

import io.openrudder.core.model.ChangeEvent;
import io.openrudder.core.query.ContinuousQuery;
import io.openrudder.core.query.ResultChange;
import io.openrudder.core.query.ResultUpdate;
import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.source.Source;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class RudderEngine {
    
    private static final Logger log = LoggerFactory.getLogger(RudderEngine.class);
    
    private final List<Source<?>> sources;
    private final List<ContinuousQuery> queries;
    private final List<Reaction> reactions;
    private final Map<String, Disposable> sourceSubscriptions;
    private final Sinks.Many<ChangeEvent> changeEventBus;
    private final Sinks.Many<ResultUpdate> resultUpdateBus;
    private EngineStatus status;

    @Builder
    public RudderEngine(List<Source<?>> sources, 
                        List<ContinuousQuery> queries, 
                        List<Reaction> reactions) {
        this.sources = new CopyOnWriteArrayList<>(sources != null ? sources : List.of());
        this.queries = new CopyOnWriteArrayList<>(queries != null ? queries : List.of());
        this.reactions = new CopyOnWriteArrayList<>(reactions != null ? reactions : List.of());
        this.sourceSubscriptions = new ConcurrentHashMap<>();
        this.changeEventBus = Sinks.many().multicast().onBackpressureBuffer();
        this.resultUpdateBus = Sinks.many().multicast().onBackpressureBuffer();
        this.status = EngineStatus.CREATED;
    }

    public void start() {
        if (status != EngineStatus.CREATED && status != EngineStatus.STOPPED) {
            throw new IllegalStateException("Engine already started");
        }

        log.info("Starting RudderEngine with {} sources, {} queries, {} reactions",
                sources.size(), queries.size(), reactions.size());

        status = EngineStatus.STARTING;

        startSources();
        startQueries();
        startReactions();

        status = EngineStatus.RUNNING;
        log.info("RudderEngine started successfully");
    }

    private void startSources() {
        for (Source<?> source : sources) {
            Disposable subscription = source.start()
                .subscribe(
                    event -> {
                        log.debug("Change event from source {}: {}", source.getName(), event);
                        changeEventBus.tryEmitNext(event);
                    },
                    error -> log.error("Source error: {}", source.getName(), error),
                    () -> log.info("Source completed: {}", source.getName())
                );
            
            sourceSubscriptions.put(source.getId(), subscription);
        }
    }

    private void startQueries() {
        Flux<ChangeEvent> changeStream = changeEventBus.asFlux();
        
        for (ContinuousQuery query : queries) {
            query.evaluate(changeStream)
                .subscribe(
                    update -> {
                        log.debug("Query result update: {}", update);
                        resultUpdateBus.tryEmitNext(update);
                    },
                    error -> log.error("Query error: {}", query.getName(), error)
                );
        }
    }

    private void startReactions() {
        Flux<ResultUpdate> resultStream = resultUpdateBus.asFlux();
        
        for (Reaction reaction : reactions) {
            resultStream
                .filter(update -> reaction.queryIds().contains(update.getQueryId()))
                .map(this::convertToResultChange)
                .flatMap(reaction::processChange)
                .subscribe(
                    v -> {},
                    error -> log.error("Reaction error: {}", reaction.name(), error)
                );
            
            reaction.start();
        }
    }
    
    private ResultChange convertToResultChange(ResultUpdate update) {
        ResultChange.ChangeType changeType = switch (update.getType()) {
            case ADDED -> ResultChange.ChangeType.ADDED;
            case UPDATED -> ResultChange.ChangeType.UPDATED;
            case REMOVED -> ResultChange.ChangeType.DELETED;
        };
        
        return ResultChange.builder()
            .queryId(update.getQueryId())
            .type(changeType)
            .before(update.getBefore())
            .after(update.getAfter())
            .timestamp(update.getTimestamp())
            .sourceChange(null)
            .build();
    }

    public void stop() {
        if (status != EngineStatus.RUNNING) {
            return;
        }

        log.info("Stopping RudderEngine");
        status = EngineStatus.STOPPING;

        reactions.forEach(Reaction::stop);

        sourceSubscriptions.values().forEach(Disposable::dispose);
        sourceSubscriptions.clear();

        sources.forEach(source -> source.stop().subscribe());

        changeEventBus.tryEmitComplete();
        resultUpdateBus.tryEmitComplete();

        status = EngineStatus.STOPPED;
        log.info("RudderEngine stopped");
    }

    public void addSource(Source<?> source) {
        sources.add(source);
        if (status == EngineStatus.RUNNING) {
            Disposable subscription = source.start()
                .subscribe(changeEventBus::tryEmitNext);
            sourceSubscriptions.put(source.getId(), subscription);
        }
    }

    public void addQuery(ContinuousQuery query) {
        queries.add(query);
    }

    public void addReaction(Reaction reaction) {
        reactions.add(reaction);
    }

    public enum EngineStatus {
        CREATED,
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        ERROR
    }
}
