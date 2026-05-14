package org.metalib.papifly.fx.settings.api;

import java.util.Map;
import java.util.Optional;

public interface SettingsStorage {

    String getString(SettingScope scope, String key, String defaultValue);

    boolean getBoolean(SettingScope scope, String key, boolean defaultValue);

    int getInt(SettingScope scope, String key, int defaultValue);

    double getDouble(SettingScope scope, String key, double defaultValue);

    Optional<String> getRaw(SettingScope scope, String key);

    void putString(SettingScope scope, String key, String value);

    void putBoolean(SettingScope scope, String key, boolean value);

    void putInt(SettingScope scope, String key, int value);

    void putDouble(SettingScope scope, String key, double value);

    Map<String, Object> getMap(SettingScope scope, String key);

    void putMap(SettingScope scope, String key, Map<String, Object> value);

    void save();

    void reload();

    default String getEffectiveString(String key, String defaultValue) {
        for (SettingScope scope : SettingScope.resolutionOrder()) {
            Optional<String> value = getRaw(scope, key);
            if (value.isPresent()) {
                return value.get();
            }
        }
        return defaultValue;
    }
}
