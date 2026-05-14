package org.metalib.papifly.fx.settings.persist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.settings.api.SettingScope;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSettingsStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndReloadsApplicationAndWorkspaceScopes() {
        JsonSettingsStorage storage = new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace"));

        storage.putString(SettingScope.APPLICATION, "appearance.theme", "dark");
        storage.putBoolean(SettingScope.WORKSPACE, "workspace.restore", true);
        storage.putMap(SettingScope.APPLICATION, "network.proxy", Map.of("host", "localhost", "port", 8080));
        storage.save();

        JsonSettingsStorage reloaded = new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace"));

        assertEquals("dark", reloaded.getString(SettingScope.APPLICATION, "appearance.theme", "light"));
        assertTrue(reloaded.getBoolean(SettingScope.WORKSPACE, "workspace.restore", false));
        assertEquals("localhost", reloaded.getMap(SettingScope.APPLICATION, "network.proxy").get("host"));
    }

    @Test
    void keepsSessionScopeInMemoryOnly() {
        JsonSettingsStorage storage = new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace"));

        storage.putBoolean(SettingScope.SESSION, "panel.open", true);
        storage.save();
        storage.reload();

        assertFalse(storage.getBoolean(SettingScope.SESSION, "panel.open", false));
    }

    @Test
    void createsBackupFileOnSave() throws Exception {
        Path appDir = tempDir.resolve("app");
        JsonSettingsStorage storage = new JsonSettingsStorage(appDir);
        storage.putString(SettingScope.APPLICATION, "key", "first");
        storage.save();

        storage.putString(SettingScope.APPLICATION, "key", "second");
        storage.save();

        Path bakFile = appDir.resolve("settings.json.bak");
        assertTrue(Files.exists(bakFile), "Backup file should exist after second save");
        String bakContent = Files.readString(bakFile, StandardCharsets.UTF_8);
        assertTrue(bakContent.contains("first"), "Backup should contain old value");
    }

    @Test
    void recoversFromCorruptedFileUsingBackup() throws Exception {
        Path appDir = tempDir.resolve("app");
        JsonSettingsStorage storage = new JsonSettingsStorage(appDir);
        storage.putString(SettingScope.APPLICATION, "theme", "dark");
        storage.save();

        // Save again to create a .bak with "dark"
        storage.putString(SettingScope.APPLICATION, "theme", "light");
        storage.save();

        // Corrupt the primary file
        Files.writeString(appDir.resolve("settings.json"), "NOT VALID JSON {{{{", StandardCharsets.UTF_8);

        // Reload — should recover from backup
        JsonSettingsStorage recovered = new JsonSettingsStorage(appDir);
        assertEquals("dark", recovered.getString(SettingScope.APPLICATION, "theme", "default"));
    }

    @Test
    void resetsToDefaultsWhenBothPrimaryAndBackupCorrupted() throws Exception {
        Path appDir = tempDir.resolve("app");
        Files.createDirectories(appDir);
        Files.writeString(appDir.resolve("settings.json"), "CORRUPT", StandardCharsets.UTF_8);
        Files.writeString(appDir.resolve("settings.json.bak"), "ALSO CORRUPT", StandardCharsets.UTF_8);

        JsonSettingsStorage storage = new JsonSettingsStorage(appDir);
        assertEquals("fallback", storage.getString(SettingScope.APPLICATION, "missing", "fallback"));
    }

    @Test
    void migratesOlderFilesWhenMigratorIsRegistered() {
        JsonSettingsStorage writer = new JsonSettingsStorage(tempDir.resolve("app"), null);
        writer.putString(SettingScope.APPLICATION, "appearance.colorMode", "dark");
        writer.save();

        JsonSettingsStorage storage = new JsonSettingsStorage(tempDir.resolve("app"), null, 2);
        storage.registerMigrator(1, (data, version) -> {
            Object value = data.remove("appearance.colorMode");
            if (value != null) {
                data.put("appearance.theme", value);
            }
            return data;
        });
        storage.reload();

        assertEquals("dark", storage.getString(SettingScope.APPLICATION, "appearance.theme", "light"));
    }
}
