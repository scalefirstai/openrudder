package io.openrudder.core.reaction.spi;

import java.util.List;

public record ValidationResult(
    boolean valid,
    List<String> errors
) {
    public static ValidationResult ofValid() {
        return new ValidationResult(true, List.of());
    }
    
    public static ValidationResult ofInvalid(String... errors) {
        return new ValidationResult(false, List.of(errors));
    }
}
