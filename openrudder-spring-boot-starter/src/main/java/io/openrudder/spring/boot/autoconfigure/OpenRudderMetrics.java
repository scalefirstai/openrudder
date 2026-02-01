package io.openrudder.spring.boot.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.openrudder.core.engine.RudderEngine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpenRudderMetrics {

    private final RudderEngine engine;

    public OpenRudderMetrics(RudderEngine engine) {
        this.engine = engine;
    }

    public void registerMetrics(MeterRegistry registry) {
        registry.gauge("openrudder.sources.count", Tags.empty(), engine.getSources().size());
        registry.gauge("openrudder.queries.count", Tags.empty(), engine.getQueries().size());
        registry.gauge("openrudder.reactions.count", Tags.empty(), engine.getReactions().size());
    }
}
