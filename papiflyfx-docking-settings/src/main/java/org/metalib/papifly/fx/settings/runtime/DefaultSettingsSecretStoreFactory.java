package org.metalib.papifly.fx.settings.runtime;

import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.secret.SecretStoreFactory;

import java.nio.file.Path;

/**
 * Default secret-store factory backed by the platform-aware {@link SecretStoreFactory}.
 */
public final class DefaultSettingsSecretStoreFactory implements SettingsSecretStoreFactory {

    @Override
    public SecretStore create(Path applicationDir) {
        return SecretStoreFactory.createDefault(applicationDir);
    }
}
