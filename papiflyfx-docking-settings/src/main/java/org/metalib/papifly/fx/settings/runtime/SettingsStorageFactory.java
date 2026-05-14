package org.metalib.papifly.fx.settings.runtime;

import org.metalib.papifly.fx.settings.api.SettingsStorage;

import java.nio.file.Path;

/**
 * Creates runtime storage implementations from resolved application and workspace paths.
 */
@FunctionalInterface
public interface SettingsStorageFactory {

    SettingsStorage create(Path applicationDir, Path workspaceRoot);
}
