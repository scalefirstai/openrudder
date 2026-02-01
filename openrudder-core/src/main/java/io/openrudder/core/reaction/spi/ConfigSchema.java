package io.openrudder.core.reaction.spi;

import java.util.List;

public record ConfigSchema(
    String kind,
    String description,
    List<PropertySchema> properties
) {
    
    public record PropertySchema(
        String name,
        String type,
        boolean required,
        String description,
        Object defaultValue
    ) {}
}
