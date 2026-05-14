package org.metalib.papifly.fx.settings.runtime;

import org.metalib.papifly.fx.settings.api.SecretStore;

import java.nio.file.Path;

/**
 * Creates the secret-store implementation used by a settings runtime.
 */
@FunctionalInterface
public interface SettingsSecretStoreFactory {

    SecretStore create(Path applicationDir);
}
