package org.metalib.papifly.fx.settings.api;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;

public interface SecretStore {

    Optional<String> getSecret(String key);

    void setSecret(String key, String value);

    default void putBytes(String key, byte[] secret) {
        setSecret(key, Base64.getEncoder().encodeToString(secret));
    }

    default Optional<byte[]> getBytes(String key) {
        return getSecret(key).map(value -> Base64.getDecoder().decode(value));
    }

    void clearSecret(String key);

    Set<String> listKeys();

    default boolean hasSecret(String key) {
        return getSecret(key).isPresent();
    }

    default String backendName() {
        return getClass().getSimpleName();
    }
}
