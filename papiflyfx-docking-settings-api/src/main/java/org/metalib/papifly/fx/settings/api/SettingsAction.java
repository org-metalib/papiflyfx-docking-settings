package org.metalib.papifly.fx.settings.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public record SettingsAction(
    String label,
    String description,
    Function<SettingsContext, CompletableFuture<ValidationResult>> handler
) {
}
