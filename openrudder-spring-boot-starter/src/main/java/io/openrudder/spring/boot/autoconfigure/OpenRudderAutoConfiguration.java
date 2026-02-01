package io.openrudder.spring.boot.autoconfigure;

import io.openrudder.core.engine.RudderEngine;
import io.openrudder.core.query.ContinuousQuery;
import io.openrudder.core.reaction.Reaction;
import io.openrudder.core.source.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(OpenRudderProperties.class)
@ConditionalOnProperty(prefix = "openrudder", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenRudderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RudderEngine rudderEngine(ApplicationContext context, OpenRudderProperties properties) {
        log.info("Initializing OpenRudder Engine");

        Map<String, Source> sourceBeans = context.getBeansOfType(Source.class);
        List<Source<?>> sources = sourceBeans.values().stream()
            .map(s -> (Source<?>) s)
            .collect(Collectors.toList());
        List<ContinuousQuery> queries = new ArrayList<>(context.getBeansOfType(ContinuousQuery.class).values());
        List<Reaction> reactions = new ArrayList<>(context.getBeansOfType(Reaction.class).values());

        RudderEngine engine = RudderEngine.builder()
            .sources(sources)
            .queries(queries)
            .reactions(reactions)
            .build();

        if (properties.isAutoStart()) {
            log.info("Auto-starting OpenRudder Engine");
            engine.start();
        }

        return engine;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenRudderHealthIndicator rudderHealthIndicator(RudderEngine engine) {
        return new OpenRudderHealthIndicator(engine);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenRudderMetrics rudderMetrics(RudderEngine engine) {
        return new OpenRudderMetrics(engine);
    }
}
