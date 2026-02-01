package io.openrudder.spring.boot.autoconfigure;

import io.openrudder.core.engine.RudderEngine;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class OpenRudderHealthIndicator implements HealthIndicator {

    private final RudderEngine engine;

    public OpenRudderHealthIndicator(RudderEngine engine) {
        this.engine = engine;
    }

    @Override
    public Health health() {
        RudderEngine.EngineStatus status = engine.getStatus();
        
        if (status == RudderEngine.EngineStatus.RUNNING) {
            return Health.up()
                .withDetail("status", status)
                .withDetail("sources", engine.getSources().size())
                .withDetail("queries", engine.getQueries().size())
                .withDetail("reactions", engine.getReactions().size())
                .build();
        } else if (status == RudderEngine.EngineStatus.ERROR) {
            return Health.down()
                .withDetail("status", status)
                .build();
        } else {
            return Health.unknown()
                .withDetail("status", status)
                .build();
        }
    }
}
