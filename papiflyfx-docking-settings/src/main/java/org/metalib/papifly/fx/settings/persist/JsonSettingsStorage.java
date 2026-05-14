package org.metalib.papifly.fx.settings.persist;

import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsMigrator;
import org.metalib.papifly.fx.settings.api.SettingsStorage;
import org.metalib.papifly.fx.settings.internal.AtomicFileWriter;
import org.metalib.papifly.fx.settings.internal.SettingsJsonCodec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class JsonSettingsStorage implements SettingsStorage {

    private static final Logger LOG = Logger.getLogger(JsonSettingsStorage.class.getName());

    public static final int CURRENT_VERSION = 1;

    private static final String VERSION_KEY = "version";
    private static final String SETTINGS_FILE_NAME = "settings.json";

    private final Map<SettingScope, Path> scopePaths = new EnumMap<>(SettingScope.class);
    private final Map<SettingScope, Map<String, Object>> scopeData = new EnumMap<>(SettingScope.class);
    private final NavigableMap<Integer, SettingsMigrator> migrators = new TreeMap<>();
    private final SettingsJsonCodec codec = new SettingsJsonCodec();
    private int currentVersion;

    public JsonSettingsStorage(Path applicationSettingsDir) {
        this(applicationSettingsDir, null);
    }

    public JsonSettingsStorage(Path applicationSettingsDir, Path workspaceSettingsDir) {
        this(applicationSettingsDir, workspaceSettingsDir, CURRENT_VERSION);
    }

    public JsonSettingsStorage(Path applicationSettingsDir, Path workspaceSettingsDir, int currentVersion) {
        this.currentVersion = Math.max(1, currentVersion);
        scopePaths.put(SettingScope.APPLICATION, applicationSettingsDir.resolve(SETTINGS_FILE_NAME));
        if (workspaceSettingsDir != null) {
            scopePaths.put(SettingScope.WORKSPACE, workspaceSettingsDir.resolve(SETTINGS_FILE_NAME));
        }
        scopeData.put(SettingScope.SESSION, new LinkedHashMap<>());
        reload();
    }

    @Override
    public synchronized String getString(SettingScope scope, String key, String defaultValue) {
        return getValue(scope, key)
            .map(value -> value instanceof String string ? string : String.valueOf(value))
            .orElse(defaultValue);
    }

    @Override
    public synchronized boolean getBoolean(SettingScope scope, String key, boolean defaultValue) {
        return getValue(scope, key)
            .map(value -> {
                if (value instanceof Boolean bool) {
                    return bool;
                }
                return Boolean.parseBoolean(String.valueOf(value));
            })
            .orElse(defaultValue);
    }

    @Override
    public synchronized int getInt(SettingScope scope, String key, int defaultValue) {
        return getValue(scope, key)
            .map(value -> {
                if (value instanceof Number number) {
                    return number.intValue();
                }
                return Integer.parseInt(String.valueOf(value));
            })
            .orElse(defaultValue);
    }

    @Override
    public synchronized double getDouble(SettingScope scope, String key, double defaultValue) {
        return getValue(scope, key)
            .map(value -> {
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
                return Double.parseDouble(String.valueOf(value));
            })
            .orElse(defaultValue);
    }

    @Override
    public synchronized Optional<String> getRaw(SettingScope scope, String key) {
        return getValue(scope, key).map(String::valueOf);
    }

    @Override
    public synchronized void putString(SettingScope scope, String key, String value) {
        if (value == null) {
            getScopeMap(scope).remove(key);
            return;
        }
        getScopeMap(scope).put(key, value);
    }

    @Override
    public synchronized void putBoolean(SettingScope scope, String key, boolean value) {
        getScopeMap(scope).put(key, value);
    }

    @Override
    public synchronized void putInt(SettingScope scope, String key, int value) {
        getScopeMap(scope).put(key, value);
    }

    @Override
    public synchronized void putDouble(SettingScope scope, String key, double value) {
        getScopeMap(scope).put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized Map<String, Object> getMap(SettingScope scope, String key) {
        Object value = getScopeMap(scope).get(key);
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>();
    }

    @Override
    public synchronized void putMap(SettingScope scope, String key, Map<String, Object> value) {
        getScopeMap(scope).put(key, new LinkedHashMap<>(value));
    }

    @Override
    public synchronized void save() {
        writeScope(SettingScope.APPLICATION);
        if (scopePaths.containsKey(SettingScope.WORKSPACE)) {
            writeScope(SettingScope.WORKSPACE);
        }
    }

    @Override
    public synchronized void reload() {
        scopeData.put(SettingScope.APPLICATION, readScope(SettingScope.APPLICATION));
        if (scopePaths.containsKey(SettingScope.WORKSPACE)) {
            scopeData.put(SettingScope.WORKSPACE, readScope(SettingScope.WORKSPACE));
        } else {
            scopeData.remove(SettingScope.WORKSPACE);
        }
        scopeData.put(SettingScope.SESSION, new LinkedHashMap<>());
    }

    public synchronized void registerMigrator(int fromVersion, SettingsMigrator migrator) {
        migrators.put(fromVersion, migrator);
    }

    public synchronized Map<String, Object> snapshot(SettingScope scope) {
        return new LinkedHashMap<>(getScopeMap(scope));
    }

    public synchronized void replaceScope(SettingScope scope, Map<String, Object> values) {
        Map<String, Object> next = new LinkedHashMap<>(values);
        next.remove(VERSION_KEY);
        scopeData.put(scope, next);
    }

    public synchronized int currentVersion() {
        return currentVersion;
    }

    public synchronized void setCurrentVersion(int currentVersion) {
        this.currentVersion = Math.max(1, currentVersion);
    }

    public synchronized Path pathFor(SettingScope scope) {
        return scopePaths.get(scope);
    }

    private Optional<Object> getValue(SettingScope scope, String key) {
        return Optional.ofNullable(getScopeMap(scope).get(key));
    }

    private Map<String, Object> getScopeMap(SettingScope scope) {
        return scopeData.computeIfAbsent(scope, ignored -> new LinkedHashMap<>());
    }

    private Map<String, Object> readScope(SettingScope scope) {
        Path path = scopePaths.get(scope);
        if (path == null || !Files.exists(path)) {
            return new LinkedHashMap<>();
        }
        try {
            return parseSettingsFile(path);
        } catch (Exception primary) {
            LOG.log(Level.WARNING, "Corrupted settings file " + path + ", attempting .bak recovery", primary);
            Path bakFile = path.resolveSibling(path.getFileName() + ".bak");
            if (Files.exists(bakFile)) {
                try {
                    Map<String, Object> recovered = parseSettingsFile(bakFile);
                    LOG.info("Recovered settings from backup " + bakFile);
                    return recovered;
                } catch (Exception secondary) {
                    LOG.log(Level.SEVERE, "Backup file " + bakFile + " is also corrupted, resetting to defaults", secondary);
                }
            } else {
                LOG.severe("No backup file found at " + bakFile + ", resetting to defaults");
            }
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> parseSettingsFile(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Map<String, Object> fileData = codec.fromJson(json);
        int version = 1;
        Object versionValue = fileData.remove(VERSION_KEY);
        if (versionValue instanceof Number number) {
            version = number.intValue();
        } else if (versionValue != null) {
            version = Integer.parseInt(String.valueOf(versionValue));
        }
        return migrate(fileData, version);
    }

    private Map<String, Object> migrate(Map<String, Object> data, int version) {
        Map<String, Object> migrated = new LinkedHashMap<>(data);
        int current = Math.max(1, version);
        while (current < currentVersion) {
            SettingsMigrator migrator = migrators.get(current);
            if (migrator == null) {
                current++;
                continue;
            }
            migrated = new LinkedHashMap<>(migrator.migrate(new LinkedHashMap<>(migrated), current));
            current++;
        }
        migrated.remove(VERSION_KEY);
        return migrated;
    }

    private void writeScope(SettingScope scope) {
        Path path = scopePaths.get(scope);
        if (path == null) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>(getScopeMap(scope));
        data.remove(VERSION_KEY);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(VERSION_KEY, currentVersion);
        payload.putAll(data);
        try {
            AtomicFileWriter.writeAtomically(path, codec.toJson(payload));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write settings to " + path, exception);
        }
    }
}
