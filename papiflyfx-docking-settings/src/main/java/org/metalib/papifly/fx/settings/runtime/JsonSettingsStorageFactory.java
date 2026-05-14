package org.metalib.papifly.fx.settings.runtime;

import org.metalib.papifly.fx.settings.api.SettingsStorage;
import org.metalib.papifly.fx.settings.persist.JsonSettingsStorage;

import java.nio.file.Path;

/**
 * Default storage factory backed by {@link JsonSettingsStorage}.
 */
public final class JsonSettingsStorageFactory implements SettingsStorageFactory {

    @Override
    public SettingsStorage create(Path applicationDir, Path workspaceRoot) {
        return new JsonSettingsStorage(
            applicationDir,
            workspaceRoot == null ? null : workspaceRoot.resolve(".papiflyfx")
        );
    }
}
