package org.metalib.papifly.fx.settings.api;

@FunctionalInterface
public interface SettingsValidator<T> {

    ValidationResult validate(T value);
}
