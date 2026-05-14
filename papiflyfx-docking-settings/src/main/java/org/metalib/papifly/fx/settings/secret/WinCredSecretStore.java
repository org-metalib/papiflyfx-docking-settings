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

public class WinCredSecretStore implements SecretStore {

    private static final String RESOURCE_NAME = "PapiflyFX-Docking";

    private final SecretStore fallback;
    private final Path indexFile;

    public WinCredSecretStore(SecretStore fallback, Path indexFile) {
        this.fallback = fallback;
        this.indexFile = indexFile;
    }

    @Override
    public synchronized Optional<String> getSecret(String key) {
        if (!isWindows()) {
            return fallback.getSecret(key);
        }
        CommandResult result = run(script("""
            [Windows.Security.Credentials.PasswordVault,Windows.Security.Credentials,ContentType=WindowsRuntime] > $null
            $vault = [Windows.Security.Credentials.PasswordVault]::new()
            $credential = $vault.Retrieve('%s', '%s')
            $credential.RetrievePassword()
            Write-Output $credential.Password
            """.formatted(escaped(RESOURCE_NAME), escaped(key))));
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
        if (!isWindows()) {
            fallback.setSecret(key, value);
            return;
        }
        CommandResult result = run(script("""
            [Windows.Security.Credentials.PasswordVault,Windows.Security.Credentials,ContentType=WindowsRuntime] > $null
            $vault = [Windows.Security.Credentials.PasswordVault]::new()
            try {
                $existing = $vault.Retrieve('%s', '%s')
                $vault.Remove($existing)
            } catch {
            }
            $credential = [Windows.Security.Credentials.PasswordCredential]::new('%s', '%s', '%s')
            $vault.Add($credential)
            """.formatted(
            escaped(RESOURCE_NAME),
            escaped(key),
            escaped(RESOURCE_NAME),
            escaped(key),
            escaped(value)
        )));
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
        if (!isWindows()) {
            return;
        }
        run(script("""
            [Windows.Security.Credentials.PasswordVault,Windows.Security.Credentials,ContentType=WindowsRuntime] > $null
            $vault = [Windows.Security.Credentials.PasswordVault]::new()
            try {
                $credential = $vault.Retrieve('%s', '%s')
                $vault.Remove($credential)
            } catch {
            }
            """.formatted(escaped(RESOURCE_NAME), escaped(key))));
    }

    @Override
    public synchronized Set<String> listKeys() {
        Set<String> keys = new LinkedHashSet<>(fallback.listKeys());
        keys.addAll(readIndex());
        return keys;
    }

    @Override
    public String backendName() {
        return "Windows Credential Manager";
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
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
            throw new IllegalStateException("Unable to update Windows Credential Manager index", exception);
        }
    }

    private CommandResult run(String script) {
        try {
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).start();
            int exitCode = process.waitFor();
            return new CommandResult(exitCode == 0, read(process));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new CommandResult(false, "");
        }
    }

    private String script(String script) {
        return "$ErrorActionPreference = 'Stop'; " + script;
    }

    private String escaped(String value) {
        return value.replace("'", "''");
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
