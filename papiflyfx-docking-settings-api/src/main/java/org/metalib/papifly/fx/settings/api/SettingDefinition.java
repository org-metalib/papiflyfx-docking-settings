package org.metalib.papifly.fx.settings.api;

public record SettingDefinition<T>(
    String key,
    String label,
    String description,
    SettingType type,
    SettingScope scope,
    T defaultValue,
    boolean secret,
    SettingsValidator<T> validator
) {

    public static <T> SettingDefinition<T> of(String key, String label, SettingType type, T defaultValue) {
        return new SettingDefinition<>(key, label, "", type, SettingScope.APPLICATION, defaultValue, false, null);
    }

    public SettingDefinition<T> withScope(SettingScope scope) {
        return new SettingDefinition<>(key, label, description, type, scope, defaultValue, secret, validator);
    }

    public SettingDefinition<T> asSecret() {
        return new SettingDefinition<>(key, label, description, type, scope, defaultValue, true, validator);
    }

    public SettingDefinition<T> withDescription(String description) {
        return new SettingDefinition<>(key, label, description, type, scope, defaultValue, secret, validator);
    }

    public SettingDefinition<T> withValidator(SettingsValidator<T> validator) {
        return new SettingDefinition<>(key, label, description, type, scope, defaultValue, secret, validator);
    }
}
