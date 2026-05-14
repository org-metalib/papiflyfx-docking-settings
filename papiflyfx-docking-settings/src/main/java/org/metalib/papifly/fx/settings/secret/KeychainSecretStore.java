package org.metalib.papifly.fx.settings.secret;

import org.metalib.papifly.fx.settings.api.SecretStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class KeychainSecretStore implements SecretStore {

    public static final String DEFAULT_SERVICE_NAME = "PapiflyFX-Docking";

    private final String serviceName;
    private final SecretStore fallback;
    private final Path indexFile;

    public KeychainSecretStore() {
        this(DEFAULT_SERVICE_NAME, new EncryptedFileSecretStore(), Path.of(System.getProperty("user.home"), ".papiflyfx", "keychain-index"));
    }

    public KeychainSecretStore(SecretStore fallback) {
        this(DEFAULT_SERVICE_NAME, fallback, Path.of(System.getProperty("user.home"), ".papiflyfx", "keychain-index"));
    }

    public KeychainSecretStore(String serviceName, SecretStore fallback, Path indexFile) {
        this.serviceName = serviceName;
        this.fallback = fallback;
        this.indexFile = indexFile;
    }

    @Override
    public synchronized Optional<String> getSecret(String key) {
        if (!isMacOs()) {
            return fallback.getSecret(key);
        }
        CommandResult result = run("security", "find-generic-password", "-s", serviceName, "-a", key, "-w");
        if (!result.success()) {
            return fallback.getSecret(key);
        }
        String value = result.output().trim();
        if (value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @Override
    public synchronized void setSecret(String key, String value) {
        if (value == null || value.isBlank()) {
            clearSecret(key);
            return;
        }
        if (!isMacOs()) {
            fallback.setSecret(key, value);
            return;
        }
        CommandResult result = run(
            "security",
            "add-generic-password",
            "-U",
            "-s",
            serviceName,
            "-a",
            key,
            "-w",
            value
        );
        if (result.success()) {
            rememberKey(key);
            fallback.clearSecret(key);
            return;
        }
        fallback.setSecret(key, value);
    }

    @Override
    public synchronized void clearSecret(String key) {
        fallback.clearSecret(key);
        forgetKey(key);
        if (!isMacOs()) {
            return;
        }
        run("security", "delete-generic-password", "-s", serviceName, "-a", key);
    }

    @Override
    public synchronized Set<String> listKeys() {
        Set<String> keys = new LinkedHashSet<>(fallback.listKeys());
        keys.addAll(readIndex());
        return keys;
    }

    @Override
    public String backendName() {
        return "macOS Keychain";
    }

    private boolean isMacOs() {
        String name = System.getProperty("os.name", "").toLowerCase();
        return name.contains("mac");
    }

    private void rememberKey(String key) {
        Set<String> keys = readIndex();
        if (keys.add(key)) {
            writeIndex(keys);
        }
    }

    private void forgetKey(String key) {
        Set<String> keys = readIndex();
        if (keys.remove(key)) {
            writeIndex(keys);
        }
    }

    private Set<String> readIndex() {
        Set<String> keys = new LinkedHashSet<>();
        if (!Files.exists(indexFile)) {
            return keys;
        }
        try {
            keys.addAll(Files.readAllLines(indexFile, StandardCharsets.UTF_8));
            keys.removeIf(String::isBlank);
            return keys;
        } catch (IOException exception) {
            return keys;
        }
    }

    private void writeIndex(Set<String> keys) {
        try {
            Path parent = indexFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(indexFile, keys, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to update keychain secret index " + indexFile, exception);
        }
    }

    private CommandResult run(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            int exitCode = process.waitFor();
            String output = read(process);
            return new CommandResult(exitCode == 0, output);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new CommandResult(false, "");
        }
    }

    private String read(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private record CommandResult(boolean success, String output) {
    }
}
