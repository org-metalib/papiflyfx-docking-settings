package org.metalib.papifly.fx.settings.api;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;

public record SettingsContext(
    SettingsStorage storage,
    SecretStore secretStore,
    ObjectProperty<Theme> themeProperty,
    SettingScope activeScope
) {

    public String getString(String key, String defaultValue) {
        return storage.getString(activeScope, key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return storage.getBoolean(activeScope, key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return storage.getInt(activeScope, key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        return storage.getDouble(activeScope, key, defaultValue);
    }
}
