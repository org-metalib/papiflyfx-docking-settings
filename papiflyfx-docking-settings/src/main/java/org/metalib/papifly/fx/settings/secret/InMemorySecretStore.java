package org.metalib.papifly.fx.settings.secret;

import org.metalib.papifly.fx.settings.api.SecretStore;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySecretStore implements SecretStore {

    private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();

    @Override
    public Optional<String> getSecret(String key) {
        return Optional.ofNullable(values.get(key)).filter(value -> !value.isBlank());
    }

    @Override
    public void setSecret(String key, String value) {
        if (value == null || value.isBlank()) {
            clearSecret(key);
            return;
        }
        values.put(key, value);
    }

    @Override
    public void clearSecret(String key) {
        values.remove(key);
    }

    @Override
    public Set<String> listKeys() {
        return new LinkedHashSet<>(values.keySet());
    }

    @Override
    public String backendName() {
        return "In Memory";
    }
}
