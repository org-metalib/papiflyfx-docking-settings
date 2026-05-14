package org.metalib.papifly.fx.settings.runtime;

import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingsStorage;
import org.metalib.papifly.fx.settings.secret.InMemorySecretStore;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRuntimeTest {

    @Test
    void createUsesInjectedFactories() {
        AtomicBoolean storageFactoryCalled = new AtomicBoolean();
        AtomicBoolean secretStoreFactoryCalled = new AtomicBoolean();
        SettingsStorage storage = new StubSettingsStorage();
        SecretStore secretStore = new InMemorySecretStore();

        SettingsRuntime runtime = SettingsRuntime.create(
            Path.of("/tmp/app"),
            Path.of("/tmp/workspace"),
            new SimpleObjectProperty<>(Theme.dark()),
            (applicationDir, workspaceRoot) -> {
                storageFactoryCalled.set(true);
                return storage;
            },
            applicationDir -> {
                secretStoreFactoryCalled.set(true);
                return secretStore;
            }
        );

        assertTrue(storageFactoryCalled.get());
        assertTrue(secretStoreFactoryCalled.get());
        assertSame(storage, runtime.storage());
        assertSame(secretStore, runtime.secretStore());
    }

    private static final class StubSettingsStorage implements SettingsStorage {

        @Override
        public String getString(SettingScope scope, String key, String defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean getBoolean(SettingScope scope, String key, boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public int getInt(SettingScope scope, String key, int defaultValue) {
            return defaultValue;
        }

        @Override
        public double getDouble(SettingScope scope, String key, double defaultValue) {
            return defaultValue;
        }

        @Override
        public Optional<String> getRaw(SettingScope scope, String key) {
            return Optional.empty();
        }

        @Override
        public void putString(SettingScope scope, String key, String value) {
        }

        @Override
        public void putBoolean(SettingScope scope, String key, boolean value) {
        }

        @Override
        public void putInt(SettingScope scope, String key, int value) {
        }

        @Override
        public void putDouble(SettingScope scope, String key, double value) {
        }

        @Override
        public Map<String, Object> getMap(SettingScope scope, String key) {
            return Map.of();
        }

        @Override
        public void putMap(SettingScope scope, String key, Map<String, Object> value) {
        }

        @Override
        public void save() {
        }

        @Override
        public void reload() {
        }
    }
}
