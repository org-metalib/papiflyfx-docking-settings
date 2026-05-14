package org.metalib.papifly.fx.settings.secret;

import org.metalib.papifly.fx.settings.api.SecretStore;

import java.nio.file.Path;

public final class SecretStoreFactory {

    private SecretStoreFactory() {
    }

    public static SecretStore createDefault() {
        return createDefault(Path.of(System.getProperty("user.home"), ".papiflyfx"));
    }

    public static SecretStore createDefault(Path applicationDir) {
        String os = System.getProperty("os.name", "").toLowerCase();
        SecretStore encryptedFallback = new EncryptedFileSecretStore(applicationDir.resolve("secrets.enc"));
        if (os.contains("mac")) {
            return new KeychainSecretStore(KeychainSecretStore.DEFAULT_SERVICE_NAME, encryptedFallback, applicationDir.resolve("keychain-index"));
        }
        if (os.contains("win")) {
            return new WinCredSecretStore(encryptedFallback, applicationDir.resolve("wincred-index"));
        }
        if (os.contains("linux")) {
            return new LibsecretSecretStore(encryptedFallback, applicationDir.resolve("libsecret-index"));
        }
        return encryptedFallback;
    }

    public static String backendName(SecretStore secretStore) {
        return secretStore.backendName();
    }
}
