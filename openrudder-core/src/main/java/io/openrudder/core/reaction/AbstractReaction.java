package io.openrudder.core.reaction;

import io.openrudder.core.query.QueryResult;
import io.openrudder.core.query.ResultChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractReaction implements Reaction {
    
    protected static final Logger log = LoggerFactory.getLogger(AbstractReaction.class);
    protected final ReactionConfig config;
    protected final AtomicLong totalProcessed = new AtomicLong(0);
    protected final AtomicLong totalErrors = new AtomicLong(0);
    protected final AtomicLong addedCount = new AtomicLong(0);
    protected final AtomicLong updatedCount = new AtomicLong(0);
    protected final AtomicLong deletedCount = new AtomicLong(0);
    protected volatile Instant lastProcessedAt;
    protected volatile boolean running = false;

    protected AbstractReaction(ReactionConfig config) {
        this.config = config;
    }

    @Override
    public String id() {
        return config.id();
    }

    @Override
    public String name() {
        return config.name();
    }

    @Override
    public String kind() {
        return config.kind();
    }

    @Override
    public Set<String> queryIds() {
        return config.queryIds();
    }

    @Override
    public Map<String, Object> properties() {
        return config.properties();
    }

    @Override
    public Map<String, ReactionConfig.QueryConfig> queryConfigs() {
        return config.queryConfigs();
    }

    @Override
    public Mono<Void> processChange(ResultChange change) {
        return Mono.defer(() -> {
            ReactionConfig.QueryConfig queryConfig = config.queryConfigs().get(change.getQueryId());
            
            return switch (change.getType()) {
                case ADDED -> {
                    addedCount.incrementAndGet();
                    yield doProcessAdded(change.getAfter(), change.getQueryId(), queryConfig);
                }
                case UPDATED -> {
                    updatedCount.incrementAndGet();
                    yield doProcessUpdated(
                        change.getBefore(), 
                        change.getAfter(), 
                        change.getQueryId(), 
                        queryConfig
                    );
                }
                case DELETED -> {
                    deletedCount.incrementAndGet();
                    yield doProcessDeleted(change.getBefore(), change.getQueryId(), queryConfig);
                }
            };
        })
        .doOnSuccess(v -> {
            totalProcessed.incrementAndGet();
            lastProcessedAt = Instant.now();
        })
        .doOnError(error -> {
            totalErrors.incrementAndGet();
            handleError(change, error);
        })
        .retryWhen(buildRetrySpec());
    }

    protected abstract Mono<Void> doProcessAdded(
        QueryResult result, 
        String queryId, 
        ReactionConfig.QueryConfig queryConfig);

    protected Mono<Void> doProcessUpdated(
        QueryResult before,
        QueryResult after,
        String queryId,
        ReactionConfig.QueryConfig queryConfig) {
        return doProcessAdded(after, queryId, queryConfig);
    }

    protected Mono<Void> doProcessDeleted(
        QueryResult result,
        String queryId,
        ReactionConfig.QueryConfig queryConfig) {
        return Mono.empty();
    }

    protected Retry buildRetrySpec() {
        if (!config.retryConfig().enabled()) {
            return Retry.max(0);
        }
        
        return Retry.backoff(
            config.retryConfig().maxAttempts(),
            Duration.ofMillis(config.retryConfig().initialBackoffMs())
        )
        .maxBackoff(Duration.ofMillis(config.retryConfig().maxBackoffMs()))
        .filter(this::isRetryable)
        .doBeforeRetry(signal -> 
            logRetry(signal.totalRetries(), signal.failure())
        );
    }

    protected boolean isRetryable(Throwable error) {
        Set<Class<? extends Exception>> retryable = 
            config.retryConfig().retryableExceptions();
        
        return retryable.stream()
            .anyMatch(exClass -> exClass.isInstance(error));
    }

    protected void handleError(ResultChange change, Throwable error) {
        log.error("Error processing change in reaction {}: {}", id(), error.getMessage(), error);
    }

    protected void logRetry(long attempt, Throwable error) {
        log.warn("Retrying reaction {} (attempt {}): {}", id(), attempt, error.getMessage());
    }

    @Override
    public void start() {
        running = true;
        log.info("Reaction started: {}", name());
    }

    @Override
    public void stop() {
        running = false;
        log.info("Reaction stopped: {}", name());
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public ReactionHealth health() {
        if (!running) {
            return new ReactionHealth(false, "STOPPED", "Reaction is not running");
        }
        
        long total = totalProcessed.get();
        long errors = totalErrors.get();
        if (total > 100 && errors > total * 0.1) {
            return new ReactionHealth(
                false, 
                "UNHEALTHY", 
                String.format("High error rate: %d/%d", errors, total)
            );
        }
        
        return new ReactionHealth(true, "HEALTHY", "Reaction is running");
    }

    @Override
    public ReactionStats stats() {
        long total = totalProcessed.get();
        double avgTime = 0.0;
        
        return new ReactionStats(
            total,
            totalErrors.get(),
            addedCount.get(),
            updatedCount.get(),
            deletedCount.get(),
            avgTime,
            lastProcessedAt
        );
    }
}
