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

public class LibsecretSecretStore implements SecretStore {

    private final SecretStore fallback;
    private final Path indexFile;

    public LibsecretSecretStore(SecretStore fallback, Path indexFile) {
        this.fallback = fallback;
        this.indexFile = indexFile;
    }

    @Override
    public synchronized Optional<String> getSecret(String key) {
        if (!isLinux()) {
            return fallback.getSecret(key);
        }
        CommandResult result = run("secret-tool", "lookup", "service", "PapiflyFX-Docking", "account", key);
        if (!result.success()) {
            return fallback.getSecret(key);
        }
        String value = result.output().trim();
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    @Override
    public synchronized void setSecret(String key, String value) {
        if (value == null || value.isBlank()) {
            clearSecret(key);
            return;
        }
        if (!isLinux()) {
            fallback.setSecret(key, value);
            return;
        }
        CommandResult result = runWithInput(value, "secret-tool", "store", "--label=PapiflyFX Docking Secret", "service", "PapiflyFX-Docking", "account", key);
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
        if (!isLinux()) {
            return;
        }
        run("secret-tool", "clear", "service", "PapiflyFX-Docking", "account", key);
    }

    @Override
    public synchronized Set<String> listKeys() {
        Set<String> keys = new LinkedHashSet<>(fallback.listKeys());
        keys.addAll(readIndex());
        return keys;
    }

    @Override
    public String backendName() {
        return "libsecret";
    }

    private boolean isLinux() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("linux");
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
        } catch (IOException ignored) {
        }
        keys.removeIf(String::isBlank);
        return keys;
    }

    private void writeIndex(Set<String> keys) {
        try {
            Path parent = indexFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(indexFile, keys, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to update libsecret index", exception);
        }
    }

    private CommandResult run(String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            int exitCode = process.waitFor();
            return new CommandResult(exitCode == 0, read(process));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new CommandResult(false, "");
        }
    }

    private CommandResult runWithInput(String input, String... command) {
        try {
            Process process = new ProcessBuilder(command).start();
            process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
            int exitCode = process.waitFor();
            return new CommandResult(exitCode == 0, read(process));
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
