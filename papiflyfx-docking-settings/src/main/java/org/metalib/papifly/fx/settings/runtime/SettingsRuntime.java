package org.metalib.papifly.fx.settings.runtime;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.api.SettingsStorage;
import org.metalib.papifly.fx.settings.api.SecretStore;

import java.nio.file.Path;
import java.util.Objects;

public final class SettingsRuntime {

    private final Path applicationDir;
    private final Path workspaceRoot;
    private final SettingsStorage storage;
    private final SecretStore secretStore;
    private final ObjectProperty<Theme> themeProperty;

    public SettingsRuntime(
        Path applicationDir,
        Path workspaceRoot,
        SettingsStorage storage,
        SecretStore secretStore,
        ObjectProperty<Theme> themeProperty
    ) {
        this.applicationDir = Objects.requireNonNull(applicationDir, "applicationDir");
        this.workspaceRoot = workspaceRoot;
        this.storage = Objects.requireNonNull(storage, "storage");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
        this.themeProperty = Objects.requireNonNull(themeProperty, "themeProperty");
    }

    public static SettingsRuntime createDefault(ObjectProperty<Theme> themeProperty) {
        return createDefault(
            themeProperty,
            new JsonSettingsStorageFactory(),
            new DefaultSettingsSecretStoreFactory()
        );
    }

    public static SettingsRuntime createDefault(
        ObjectProperty<Theme> themeProperty,
        SettingsStorageFactory storageFactory,
        SettingsSecretStoreFactory secretStoreFactory
    ) {
        Path applicationDir = resolveApplicationDir();
        Path workspaceRoot = resolveWorkspaceRoot();
        return create(
            applicationDir,
            workspaceRoot,
            themeProperty,
            storageFactory,
            secretStoreFactory
        );
    }

    public static SettingsRuntime create(
        Path applicationDir,
        Path workspaceRoot,
        ObjectProperty<Theme> themeProperty,
        SettingsStorageFactory storageFactory,
        SettingsSecretStoreFactory secretStoreFactory
    ) {
        Objects.requireNonNull(storageFactory, "storageFactory");
        Objects.requireNonNull(secretStoreFactory, "secretStoreFactory");
        return new SettingsRuntime(
            applicationDir,
            workspaceRoot,
            storageFactory.create(applicationDir, workspaceRoot),
            secretStoreFactory.create(applicationDir),
            themeProperty
        );
    }

    public SettingsContext context(SettingScope activeScope) {
        return new SettingsContext(storage, secretStore, themeProperty, activeScope);
    }

    public Path applicationDir() {
        return applicationDir;
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public SettingsStorage storage() {
        return storage;
    }

    public SecretStore secretStore() {
        return secretStore;
    }

    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    private static Path resolveWorkspaceRoot() {
        String configured = System.getProperty("papiflyfx.workspace.root", "").trim();
        if (!configured.isEmpty()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    private static Path resolveApplicationDir() {
        String configured = System.getProperty("papiflyfx.app.dir", "").trim();
        if (!configured.isEmpty()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".papiflyfx");
    }
}
