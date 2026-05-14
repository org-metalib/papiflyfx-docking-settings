# PapiflyFX Docking Settings — Implementation Plan

## 1. Problem Statement

PapiflyFX Docking currently has no unified mechanism for managing application settings. Each module handles configuration ad hoc (e.g., the GitHub module stores PATs via `CredentialStore` but there is no discoverable UI for it). Users need a centralized, extensible, dockable settings component where they can configure themes, secrets (API keys, PATs), MCP servers, editor preferences, and more — all with a consistent UI, validation, and persistence model.

## 2. Proposed Approach

Build a new `papiflyfx-docking-settings` module that follows the established content-module pattern (`ContentFactory` + `ContentStateAdapter` + ServiceLoader). The module provides:

1. **A settings shell UI** — dockable two-pane panel (category navigation on the left, active category pane on the right).
2. **A contributor SPI** — `SettingsCategory` + `SettingsContributor` interfaces registered via ServiceLoader so any module can inject its own settings pages.
3. **A tiered persistence layer** — plain JSON for non-secret settings, `SecretStore` abstraction (OS keychain / encrypted file) for secrets.
4. **Built-in categories for v1** — Appearance, GitHub, AI Models, MCP Servers, Editor, Workspace.

The design is grounded in the Claude Opus research document (UC-1 through UC-10) and enriched with:
- **Codex**: scoped settings (Application / Workspace), versioned schema migration, settings search, validation lifecycle, profiles.
- **Grok**: hierarchical category tree, i18n readiness, debug/diagnostics panel.
- **Gemini**: dynamic plugin injection, encrypted storage layer, density/scaling.
- **Login specs** (`spec/papiflyfx-docking-login/`): unified secret store architecture, identity provider configuration, session metadata persistence, memory hygiene for sensitive values, and secret key naming conventions.

---

## 3. Module Structure

```
papiflyfx-docking-settings/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/org/metalib/papifly/fx/settings/
│   │   │   ├── api/                    # Public SPI & DTOs
│   │   │   │   ├── SettingsCategory.java
│   │   │   │   ├── SettingsContributor.java
│   │   │   │   ├── SettingsContext.java
│   │   │   │   ├── SettingsAction.java
│   │   │   │   ├── SettingsValidator.java
│   │   │   │   ├── SettingDefinition.java
│   │   │   │   ├── SettingScope.java
│   │   │   │   ├── SettingType.java
│   │   │   │   └── ValidationResult.java
│   │   │   ├── secret/                 # Secret storage abstraction
│   │   │   │   ├── SecretStore.java
│   │   │   │   ├── SecretKeyNames.java
│   │   │   │   ├── SecretStoreFactory.java
│   │   │   │   ├── KeychainSecretStore.java
│   │   │   │   ├── EncryptedFileSecretStore.java
│   │   │   │   ├── InMemorySecretStore.java
│   │   │   │   ├── SecretStoreCredentialAdapter.java
│   │   │   │   └── SecretStoreSecureAdapter.java
│   │   │   ├── persist/               # JSON persistence
│   │   │   │   ├── SettingsStorage.java
│   │   │   │   ├── JsonSettingsStorage.java
│   │   │   │   └── SettingsMigrator.java
│   │   │   ├── ui/                    # UI components
│   │   │   │   ├── SettingsPanel.java
│   │   │   │   ├── SettingsCategoryList.java
│   │   │   │   ├── SettingsSearchBar.java
│   │   │   │   ├── SettingsToolbar.java
│   │   │   │   └── controls/          # Reusable setting controls
│   │   │   │       ├── BooleanSettingControl.java
│   │   │   │       ├── StringSettingControl.java
│   │   │   │       ├── NumberSettingControl.java
│   │   │   │       ├── EnumSettingControl.java
│   │   │   │       ├── ColorSettingControl.java
│   │   │   │       ├── PathSettingControl.java
│   │   │   │       ├── SecretSettingControl.java
│   │   │   │       └── SettingControlFactory.java
│   │   │   ├── categories/            # Built-in category implementations
│   │   │   │   ├── AppearanceCategory.java
│   │   │   │   ├── WorkspaceCategory.java
│   │   │   │   └── SecurityCategory.java
│   │   │   └── docking/               # Docking integration
│   │   │       ├── SettingsContentFactory.java
│   │   │       └── SettingsStateAdapter.java
│   │   └── resources/
│   │       └── META-INF/services/
│   │           └── org.metalib.papifly.fx.docking.api.ContentStateAdapter
│   └── test/
│       └── java/org/metalib/papifly/fx/settings/
│           ├── api/
│           ├── secret/
│           ├── persist/
│           └── ui/
```

---

## 4. Public SPI — Interfaces & Records

### 4.1 SettingsCategory

The primary extension point. Each module implements this to contribute a settings page.

```java
package org.metalib.papifly.fx.settings.api;

import javafx.scene.Node;

/**
 * A single page in the settings panel. Discovered via ServiceLoader
 * or registered programmatically through SettingsContributor.
 */
public interface SettingsCategory {

    /** Stable identifier for persistence (e.g., "appearance", "github"). */
    String id();

    /** Human-readable name shown in the category list. */
    String displayName();

    /** Optional icon node for the category list (16x16 recommended). */
    default Node icon() { return null; }

    /** Sort order within the category list. Lower values appear first. */
    default int order() { return 100; }

    /**
     * Builds the settings pane for this category.
     * Called once when the category is first selected.
     *
     * @param context provides access to storage, secret store, and theme
     * @return the settings pane node
     */
    Node buildSettingsPane(SettingsContext context);

    /**
     * Applies pending changes. Called when the user clicks "Apply" or "Save".
     * Implementations should write values to SettingsContext.storage()
     * and SettingsContext.secretStore() as appropriate.
     *
     * @param context settings context
     */
    void apply(SettingsContext context);

    /**
     * Resets all fields to their persisted (or default) values.
     * Called when the user clicks "Reset" or switches away without saving.
     *
     * @param context settings context
     */
    void reset(SettingsContext context);

    /**
     * Returns true if any field has been modified since the last apply/reset.
     */
    default boolean isDirty() { return false; }
}
```

### 4.2 SettingsContext

Shared context passed to every category. Provides access to storage, secrets, theme, and validation.

```java
package org.metalib.papifly.fx.settings.api;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.persist.SettingsStorage;
import org.metalib.papifly.fx.settings.secret.SecretStore;

/**
 * Context object provided to SettingsCategory implementations.
 */
public record SettingsContext(
    SettingsStorage storage,
    SecretStore secretStore,
    ObjectProperty<Theme> themeProperty,
    SettingScope activeScope
) {

    /**
     * Convenience: read a string setting with default.
     */
    public String getString(String key, String defaultValue) {
        return storage.getString(activeScope, key, defaultValue);
    }

    /**
     * Convenience: read a boolean setting with default.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return storage.getBoolean(activeScope, key, defaultValue);
    }

    /**
     * Convenience: read an int setting with default.
     */
    public int getInt(String key, int defaultValue) {
        return storage.getInt(activeScope, key, defaultValue);
    }

    /**
     * Convenience: read a double setting with default.
     */
    public double getDouble(String key, double defaultValue) {
        return storage.getDouble(activeScope, key, defaultValue);
    }
}
```

### 4.3 SettingScope

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Determines where a setting is persisted and how scopes override each other.
 * Resolution order: SESSION > WORKSPACE > APPLICATION > DEFAULT.
 */
public enum SettingScope {
    /** Machine-wide, shared across all projects. */
    APPLICATION,
    /** Tied to a specific repository or working directory. */
    WORKSPACE,
    /** Temporary, not persisted beyond the current session. */
    SESSION
}
```

### 4.4 SettingDefinition

Typed setting descriptor for automatic UI generation (from Codex research).

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Declares a single setting field. Used for automatic control generation
 * and settings search indexing.
 *
 * @param <T> the value type
 */
public record SettingDefinition<T>(
    String key,
    String label,
    String description,
    SettingType type,
    SettingScope scope,
    T defaultValue,
    boolean secret,
    SettingsValidator<T> validator
) {
    public static <T> SettingDefinition<T> of(
            String key, String label, SettingType type, T defaultValue) {
        return new SettingDefinition<>(key, label, "", type,
                SettingScope.APPLICATION, defaultValue, false, null);
    }

    public SettingDefinition<T> withScope(SettingScope scope) {
        return new SettingDefinition<>(key, label, description, type,
                scope, defaultValue, secret, validator);
    }

    public SettingDefinition<T> asSecret() {
        return new SettingDefinition<>(key, label, description, type,
                scope, defaultValue, true, validator);
    }

    public SettingDefinition<T> withDescription(String description) {
        return new SettingDefinition<>(key, label, description, type,
                scope, defaultValue, secret, validator);
    }

    public SettingDefinition<T> withValidator(SettingsValidator<T> validator) {
        return new SettingDefinition<>(key, label, description, type,
                scope, defaultValue, secret, validator);
    }
}
```

### 4.5 SettingType

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Supported setting value types. Used by SettingControlFactory to create
 * the appropriate UI control.
 */
public enum SettingType {
    BOOLEAN,
    STRING,
    INTEGER,
    DOUBLE,
    ENUM,
    COLOR,
    FONT,
    FILE_PATH,
    DIRECTORY_PATH,
    SECRET,
    /** Complex custom editor (theme designer, MCP server list, etc.) */
    CUSTOM
}
```

### 4.6 SettingsValidator and ValidationResult

```java
package org.metalib.papifly.fx.settings.api;

@FunctionalInterface
public interface SettingsValidator<T> {
    ValidationResult validate(T value);
}
```

```java
package org.metalib.papifly.fx.settings.api;

public record ValidationResult(Level level, String message) {

    public enum Level { OK, INFO, WARNING, ERROR }

    public static final ValidationResult OK = new ValidationResult(Level.OK, "");

    public static ValidationResult error(String message) {
        return new ValidationResult(Level.ERROR, message);
    }

    public static ValidationResult warning(String message) {
        return new ValidationResult(Level.WARNING, message);
    }

    public boolean isValid() {
        return level != Level.ERROR;
    }
}
```

### 4.7 SettingsAction

Page-level action buttons (e.g., "Test Connection", "Reset to Defaults") — from Codex research.

```java
package org.metalib.papifly.fx.settings.api;

import java.util.concurrent.CompletableFuture;

/**
 * An action button displayed in a settings category page.
 * Actions can be synchronous or async (e.g., network calls).
 */
public record SettingsAction(
    String label,
    String description,
    java.util.function.Function<SettingsContext, CompletableFuture<ValidationResult>> handler
) {}
```

### 4.8 SettingsContributor

Alternative to direct `SettingsCategory` ServiceLoader registration. Allows a module to contribute multiple categories and register programmatically.

```java
package org.metalib.papifly.fx.settings.api;

import java.util.List;

/**
 * A module-level contributor that provides one or more settings categories.
 * Discovered via ServiceLoader.
 */
public interface SettingsContributor {
    List<SettingsCategory> getCategories();
}
```

---

## 5. Secret Storage Layer

### 5.1 Unified SecretStore — Shared with Login Module

The `papiflyfx-docking-login` module (see `spec/papiflyfx-docking-login/plan.md`) defines its own `SecureSecretStore` interface for OAuth refresh tokens. To avoid parallel secret store hierarchies, the settings module's `SecretStore` is the **single shared secret abstraction** for the entire framework. The login module's `SecureSecretStore` and the GitHub module's `CredentialStore` both delegate to it.

> **Placement decision**: The `SecretStore` interface should ultimately live in `papiflyfx-docking-api` so both `papiflyfx-docking-settings` and `papiflyfx-docking-login` can depend on it without depending on each other. For v1 bootstrapping it can start in the settings module and be promoted later.

### 5.2 SecretStore Interface

Generalizes the existing `CredentialStore` pattern from the GitHub module and the `SecureSecretStore` contract from the login module into a unified multi-key secret store. Provides both `String` and `byte[]` APIs since login tokens are strings but the login spec's vault cipher operates on byte arrays.

```java
package org.metalib.papifly.fx.settings.secret;

import java.util.Optional;
import java.util.Set;

/**
 * Abstraction for secure secret storage. Implementations must never
 * expose raw secret values in logs, toString(), or serialized JSON.
 *
 * Shared by: settings module (API keys, PATs), login module (OAuth
 * refresh tokens), and any future module needing secret persistence.
 */
public interface SecretStore {

    // --- String API (settings, PATs, API keys) ---

    /**
     * Retrieves a secret by key.
     * @param key stable secret identifier (e.g., "github.pat", "openai.api-key")
     * @return the secret value, or empty if not stored
     */
    Optional<String> getSecret(String key);

    /**
     * Stores or updates a secret.
     * @param key   stable secret identifier
     * @param value the secret value (null or blank clears the secret)
     */
    void setSecret(String key, String value);

    // --- Binary API (OAuth tokens, encryption keys) ---

    /**
     * Stores a binary secret (e.g., raw token bytes, vault master key).
     * Default implementation encodes to Base64 and delegates to setSecret().
     */
    default void putBytes(String key, byte[] secret) {
        setSecret(key, java.util.Base64.getEncoder().encodeToString(secret));
    }

    /**
     * Retrieves a binary secret.
     * Default implementation decodes from Base64 via getSecret().
     */
    default Optional<byte[]> getBytes(String key) {
        return getSecret(key).map(s -> java.util.Base64.getDecoder().decode(s));
    }

    // --- Management ---

    /**
     * Removes a secret.
     * @param key stable secret identifier
     */
    void clearSecret(String key);

    /**
     * Lists all stored secret keys (not values).
     * Used by the Security settings category to display stored secrets.
     */
    Set<String> listKeys();

    /**
     * Returns true if a secret exists for the given key.
     */
    default boolean hasSecret(String key) {
        return getSecret(key).isPresent();
    }
}
```

### 5.3 Secret Key Naming Convention

Aligned with the login module's `SecretKeyNames` pattern (from `plan-codex.md` §12.2). All secret keys use a hierarchical colon-separated namespace:

```
<module>:<category>:<identifier>
```

| Module | Key Pattern | Example |
|---|---|---|
| Settings (static secrets) | `settings:<provider>:<name>` | `settings:openai:api-key` |
| GitHub | `github:pat` | `github:pat` |
| Login (OAuth tokens) | `login:oauth:refresh:<providerId>:<subject>` | `login:oauth:refresh:google:user@gmail.com` |
| Login (vault key) | `login:vault:key` | `login:vault:key` |
| MCP | `mcp:<serverName>:auth-token` | `mcp:filesystem-tools:auth-token` |

```java
package org.metalib.papifly.fx.settings.secret;

/**
 * Standardized secret key naming to avoid collisions across modules.
 */
public final class SecretKeyNames {
    private SecretKeyNames() {}

    public static String settingsKey(String provider, String name) {
        return "settings:" + provider + ":" + name;
    }

    public static String githubPat() {
        return "github:pat";
    }

    public static String oauthRefreshToken(String providerId, String subject) {
        return "login:oauth:refresh:" + providerId + ":" + subject;
    }

    public static String vaultKey(String moduleId) {
        return moduleId + ":vault:key";
    }

    public static String mcpAuthToken(String serverName) {
        return "mcp:" + serverName + ":auth-token";
    }
}
```

### 5.4 Memory Hygiene

From login spec `plan-codex.md` §12.4: sensitive values held in mutable buffers must be zeroed after use. All `SecretStore`
implementations and consumers should follow this discipline:

```java
// Pattern: zero sensitive char/byte arrays after use
char[] secret = secretStore.getSecret(key)
        .map(String::toCharArray).orElse(new char[0]);
try {
    useSecret(secret);
} finally {
    java.util.Arrays.fill(secret, '\0');
}
```

Implementations of `EncryptedFileSecretStore` must zero plaintext byte buffers immediately after encryption and before returning from decryption callers.

### 5.5 Implementation Strategy

Three implementations, mirroring the existing `CredentialStore` hierarchy:

| Implementation | Platform | Backing | Security Level |
|---|---|---|---|
| `KeychainSecretStore` | macOS | `security` CLI (Keychain Services) | High — OS-managed encryption |
| `EncryptedFileSecretStore` | All | AES-256-GCM encrypted JSON file | Medium — key derived from machine ID |
| `InMemorySecretStore` | All | `ConcurrentHashMap` | None — testing / transient only |

**`KeychainSecretStore`** extends the existing pattern from `KeychainTokenStore` but supports multiple keys by using the secret key as the `account` name:

```java
package org.metalib.papifly.fx.settings.secret;

/**
 * macOS Keychain-backed secret store. Falls back to EncryptedFileSecretStore
 * on non-macOS platforms.
 */
public class KeychainSecretStore implements SecretStore {
    private static final String SERVICE_NAME = "PapiflyFX-Docking";
    private final SecretStore fallback;

    public KeychainSecretStore() {
        this(new EncryptedFileSecretStore());
    }

    public KeychainSecretStore(SecretStore fallback) {
        this.fallback = fallback;
    }

    @Override
    public Optional<String> getSecret(String key) {
        if (!isMacOs()) return fallback.getSecret(key);
        // security find-generic-password -s PapiflyFX-Docking -a <key> -w
        // ... same pattern as KeychainTokenStore
    }

    @Override
    public void setSecret(String key, String value) {
        if (value == null || value.isBlank()) { clearSecret(key); return; }
        if (!isMacOs()) { fallback.setSecret(key, value); return; }
        // security add-generic-password -U -s PapiflyFX-Docking -a <key> -w <value>
    }

    @Override
    public void clearSecret(String key) {
        fallback.clearSecret(key);
        if (!isMacOs()) return;
        // security delete-generic-password -s PapiflyFX-Docking -a <key>
    }
    // ...
}
```

**`EncryptedFileSecretStore`** uses JDK-bundled `javax.crypto` (AES-256-GCM) with a machine-derived key:

```java
package org.metalib.papifly.fx.settings.secret;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.file.Path;

/**
 * Encrypted JSON file-based secret store.
 * File location: ~/.papiflyfx/secrets.enc
 * Key derivation: PBKDF2 from machine-specific seed (hostname + user.name + os.arch).
 */
public class EncryptedFileSecretStore implements SecretStore {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private final Path secretsFile;
    private final SecretKey encryptionKey;

    public EncryptedFileSecretStore() {
        this(Path.of(System.getProperty("user.home"), ".papiflyfx", "secrets.enc"));
    }

    public EncryptedFileSecretStore(Path secretsFile) {
        this.secretsFile = secretsFile;
        this.encryptionKey = deriveKey();
    }

    // Loads all secrets from encrypted file, decrypts, returns map
    // On setSecret/clearSecret: decrypt, modify map, re-encrypt, write
    // ...
}
```

### 5.6 SecretStore Factory

```java
package org.metalib.papifly.fx.settings.secret;

/**
 * Creates the appropriate SecretStore for the current platform.
 */
public final class SecretStoreFactory {
    private SecretStoreFactory() {}

    public static SecretStore createDefault() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return new KeychainSecretStore();
        }
        // Windows: could add WinCred support in future
        // Linux: could add libsecret/GNOME Keyring in future
        return new EncryptedFileSecretStore();
    }
}
```

### 5.7 Bridge to Existing CredentialStore

The GitHub module's `CredentialStore` interface should be adapted to delegate to the new `SecretStore`:

```java
package org.metalib.papifly.fx.settings.secret;

import org.metalib.papifly.fx.github.ui.state.CredentialStore;

/**
 * Bridges the GitHub module's CredentialStore to the unified SecretStore.
 */
public class SecretStoreCredentialAdapter implements CredentialStore {
    private static final String GITHUB_PAT_KEY = "github.pat";
    private final SecretStore secretStore;

    public SecretStoreCredentialAdapter(SecretStore secretStore) {
        this.secretStore = secretStore;
    }

    @Override
    public Optional<String> getToken() {
        return secretStore.getSecret(GITHUB_PAT_KEY);
    }

    @Override
    public void setToken(String token) {
        secretStore.setSecret(GITHUB_PAT_KEY, token);
    }

    @Override
    public void clearToken() {
        secretStore.clearSecret(GITHUB_PAT_KEY);
    }
}
```

### 5.8 Bridge to Login Module's SecureSecretStore

The login module (`spec/papiflyfx-docking-login/plan.md` §7.4) defines `SecureSecretStore` with a `byte[]` API for OAuth refresh tokens. Rather than maintaining a separate store, the login module should delegate to the unified `SecretStore` via adapter:

```java
package org.metalib.papifly.fx.settings.secret;

import org.metalib.papifly.fx.login.security.SecureSecretStore;

/**
 * Bridges the login module's SecureSecretStore to the unified SecretStore.
 * Allows OAuth refresh tokens to be stored alongside other secrets.
 */
public class SecretStoreSecureAdapter implements SecureSecretStore {
    private final SecretStore secretStore;

    public SecretStoreSecureAdapter(SecretStore secretStore) {
        this.secretStore = secretStore;
    }

    @Override
    public void put(String key, byte[] secret) {
        secretStore.putBytes(key, secret);
    }

    @Override
    public Optional<byte[]> get(String key) {
        return secretStore.getBytes(key);
    }

    @Override
    public void delete(String key) {
        secretStore.clearSecret(key);
    }
}
```

### 5.9 Security Hardening Checklist (from Login Specs)

Derived from `plan-codex.md` §15 and `plan.md` §9.3, applicable to all secret store implementations:

1. **No secrets in logs** — structured logs must redact any field matching secret key patterns.
2. **No secrets in serialized state** — `ContentStateAdapter.saveState()` must never include secret values; only key references (e.g., `refreshTokenRef`).
3. **Memory zeroing** — mutable `byte[]`/`char[]` buffers holding secrets are zeroed in `finally` blocks.
4. **Encryption uses `SecureRandom`** — for IV generation, key derivation salts, PKCE verifiers.
5. **AES-GCM authenticated encryption** — the `EncryptedFileSecretStore` uses GCM (not CBC) to prevent tampering.
6. **OS keychain preferred** — `SecretStoreFactory` always prefers the OS keychain over file-based encryption.
7. **Fallback warning** — when falling back from OS keychain to encrypted file, surface a non-blocking warning in the Security settings category UI.

---

## 6. Settings Persistence (Non-Secret)

### 6.1 SettingsStorage Interface

```java
package org.metalib.papifly.fx.settings.persist;

import org.metalib.papifly.fx.settings.api.SettingScope;
import java.util.Map;
import java.util.Optional;

/**
 * Reads and writes non-secret settings values.
 * Scoped: application-level settings stored in ~/.papiflyfx/settings.json,
 * workspace-level in .papiflyfx/settings.json relative to workspace root.
 */
public interface SettingsStorage {

    // --- Typed getters with defaults ---
    String getString(SettingScope scope, String key, String defaultValue);
    boolean getBoolean(SettingScope scope, String key, boolean defaultValue);
    int getInt(SettingScope scope, String key, int defaultValue);
    double getDouble(SettingScope scope, String key, double defaultValue);
    Optional<String> getRaw(SettingScope scope, String key);

    // --- Typed setters ---
    void putString(SettingScope scope, String key, String value);
    void putBoolean(SettingScope scope, String key, boolean value);
    void putInt(SettingScope scope, String key, int value);
    void putDouble(SettingScope scope, String key, double value);

    // --- Structured data (maps/lists) ---
    Map<String, Object> getMap(SettingScope scope, String key);
    void putMap(SettingScope scope, String key, Map<String, Object> value);

    // --- Lifecycle ---
    void save();
    void reload();

    // --- Scope resolution: returns effective value across scopes ---
    default String getEffectiveString(String key, String defaultValue) {
        // SESSION > WORKSPACE > APPLICATION
        for (SettingScope scope : SettingScope.values()) {
            Optional<String> val = getRaw(scope, key);
            if (val.isPresent()) return val.get();
        }
        return defaultValue;
    }
}
```

### 6.2 JsonSettingsStorage

Uses the existing `java.util.Map`-based JSON approach (no external library), consistent with `DockSessionSerializer`.

```java
package org.metalib.papifly.fx.settings.persist;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON file-backed SettingsStorage.
 *
 * File layout:
 *   ~/.papiflyfx/settings.json         (APPLICATION scope)
 *   <workspace>/.papiflyfx/settings.json (WORKSPACE scope)
 *
 * JSON structure:
 * {
 *   "version": 1,
 *   "appearance.theme": "dark",
 *   "appearance.font.size": 14,
 *   "editor.tabSize": 4,
 *   ...
 * }
 *
 * Uses the flat dotted-key convention for simplicity and searchability.
 */
public class JsonSettingsStorage implements SettingsStorage {
    private final Map<SettingScope, Path> scopePaths;
    private final Map<SettingScope, Map<String, Object>> data = new LinkedHashMap<>();

    public JsonSettingsStorage(Path applicationSettingsDir) {
        this(applicationSettingsDir, null);
    }

    public JsonSettingsStorage(Path applicationSettingsDir, Path workspaceSettingsDir) {
        this.scopePaths = new LinkedHashMap<>();
        scopePaths.put(SettingScope.APPLICATION,
                applicationSettingsDir.resolve("settings.json"));
        if (workspaceSettingsDir != null) {
            scopePaths.put(SettingScope.WORKSPACE,
                    workspaceSettingsDir.resolve("settings.json"));
        }
        // SESSION scope is in-memory only
        data.put(SettingScope.SESSION, new LinkedHashMap<>());
        reload();
    }

    // ... implementation reads/writes JSON files using the
    // same Map-based serializer used by DockSessionPersistence
}
```

### 6.3 Persistence File Layout

```
~/.papiflyfx/                          # Application-scope directory
├── settings.json                       # Non-secret settings (flat key-value)
├── secrets.enc                         # Encrypted secrets (or OS keychain used)
└── keybindings.json                    # Custom keyboard shortcuts (future)

<workspace>/.papiflyfx/                 # Workspace-scope directory (optional)
├── settings.json                       # Workspace-specific overrides
└── mcp-servers.json                    # MCP server definitions for this workspace
```

### 6.4 Settings Versioning and Migration

From Codex: settings files carry a `version` field. `SettingsMigrator` upgrades old formats.

```java
package org.metalib.papifly.fx.settings.persist;

import java.util.Map;

/**
 * Migrates settings data from one schema version to the next.
 * Registered per category. Invoked on load when the stored version
 * is older than the current version.
 */
@FunctionalInterface
public interface SettingsMigrator {
    /**
     * Transforms settings data from fromVersion to fromVersion + 1.
     * @param data    mutable settings map
     * @param fromVersion the version of the stored data
     * @return the migrated data (may be the same map, modified in place)
     */
    Map<String, Object> migrate(Map<String, Object> data, int fromVersion);
}
```

---

## 7. UI Components

### 7.1 SettingsPanel — Main Layout

Standard two-pane settings layout (inspired by IntelliJ/VS Code, from all four research docs).

```
┌─────────────────────────────────────────────────────────────┐
│  🔍 Search settings...                                      │
├──────────────┬──────────────────────────────────────────────┤
│              │                                              │
│  Appearance  │   ← Active category pane content →           │
│  Workspace   │                                              │
│  Editor      │   (built by SettingsCategory                 │
│  GitHub      │    .buildSettingsPane())                     │
│  AI Models   │                                              │
│  MCP Servers │                                              │
│  Security    │                                              │
│              │                                              │
├──────────────┴──────────────────────────────────────────────┤
│  [Apply]  [Reset]  [Dirty indicator]    [page actions...]   │
└─────────────────────────────────────────────────────────────┘
```

```java
package org.metalib.papifly.fx.settings.ui;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.settings.api.*;

/**
 * Main settings panel. Implements DisposableContent for proper cleanup
 * when the dock tab is closed.
 */
public class SettingsPanel extends BorderPane implements DisposableContent {

    private final SettingsContext context;
    private final SettingsCategoryList categoryList;
    private final StackPane contentArea;
    private final SettingsToolbar toolbar;
    private final SettingsSearchBar searchBar;
    private SettingsCategory activeCategory;

    public SettingsPanel(SettingsContext context) {
        this.context = context;
        this.searchBar = new SettingsSearchBar();
        this.categoryList = new SettingsCategoryList();
        this.contentArea = new StackPane();
        this.toolbar = new SettingsToolbar();

        setTop(searchBar);
        setLeft(categoryList);
        setCenter(contentArea);
        setBottom(toolbar);

        categoryList.selectedCategoryProperty().addListener(
                (obs, old, selected) -> showCategory(selected));

        toolbar.onApply(() -> {
            if (activeCategory != null) activeCategory.apply(context);
        });
        toolbar.onReset(() -> {
            if (activeCategory != null) activeCategory.reset(context);
        });

        // Discover and register all categories
        loadCategories();
    }

    private void loadCategories() {
        // 1. Load SettingsCategory implementations via ServiceLoader
        // 2. Load SettingsContributor implementations via ServiceLoader
        // 3. Sort by order(), add to categoryList
        // 4. Select first category
    }

    private void showCategory(SettingsCategory category) {
        if (activeCategory != null && activeCategory.isDirty()) {
            // prompt: save or discard
        }
        activeCategory = category;
        contentArea.getChildren().setAll(
                category.buildSettingsPane(context));
    }

    @Override
    public void dispose() {
        context.storage().save();
    }
}
```

### 7.2 SettingsSearchBar

From Codex (S29): global search across all settings categories.

```java
package org.metalib.papifly.fx.settings.ui;

import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Search bar that filters categories and highlights matching settings.
 * Categories implement keyword matching by returning searchable terms
 * from their SettingDefinition labels and descriptions.
 */
public class SettingsSearchBar extends HBox {
    private final TextField searchField;

    public SettingsSearchBar() {
        searchField = new TextField();
        searchField.setPromptText("Search settings...");
        getChildren().add(searchField);
    }

    public TextField getSearchField() { return searchField; }
}
```

### 7.3 Reusable Setting Controls

`SettingControlFactory` creates the appropriate control for a `SettingDefinition`:

```java
package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.Node;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingType;

/**
 * Creates UI controls for setting definitions.
 */
public final class SettingControlFactory {
    private SettingControlFactory() {}

    public static <T> Node createControl(SettingDefinition<T> definition) {
        return switch (definition.type()) {
            case BOOLEAN   -> new BooleanSettingControl(definition);
            case STRING    -> new StringSettingControl(definition);
            case INTEGER   -> new NumberSettingControl(definition);
            case DOUBLE    -> new NumberSettingControl(definition);
            case ENUM      -> new EnumSettingControl(definition);
            case COLOR     -> new ColorSettingControl(definition);
            case SECRET    -> new SecretSettingControl(definition);
            case FILE_PATH, DIRECTORY_PATH -> new PathSettingControl(definition);
            case FONT      -> new StringSettingControl(definition); // v1 simplified
            case CUSTOM    -> throw new UnsupportedOperationException(
                    "CUSTOM type requires explicit editor via buildSettingsPane()");
        };
    }
}
```

**SecretSettingControl** — masked input with reveal toggle:

```java
package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Masked password field with reveal toggle and clear button.
 * Never exposes the raw value in tooltips, logs, or toString().
 */
public class SecretSettingControl extends HBox {
    private final PasswordField passwordField;
    private final TextField plainTextField;
    private final Button revealButton;
    private final Button clearButton;
    private boolean revealed = false;

    public SecretSettingControl(SettingDefinition<?> definition) {
        passwordField = new PasswordField();
        plainTextField = new TextField();
        plainTextField.setVisible(false);
        plainTextField.setManaged(false);

        revealButton = new Button("👁");
        revealButton.setOnAction(e -> toggleReveal());

        clearButton = new Button("✕");
        clearButton.setOnAction(e -> {
            passwordField.clear();
            plainTextField.clear();
        });

        getChildren().addAll(passwordField, plainTextField, revealButton, clearButton);
    }

    private void toggleReveal() {
        revealed = !revealed;
        if (revealed) {
            plainTextField.setText(passwordField.getText());
            passwordField.setVisible(false); passwordField.setManaged(false);
            plainTextField.setVisible(true); plainTextField.setManaged(true);
        } else {
            passwordField.setText(plainTextField.getText());
            plainTextField.setVisible(false); plainTextField.setManaged(false);
            passwordField.setVisible(true); passwordField.setManaged(true);
        }
    }

    public String getValue() { return revealed ? plainTextField.getText() : passwordField.getText(); }
    public void setValue(String value) { passwordField.setText(value); plainTextField.setText(value); }
}
```

---

## 8. Docking Integration

### 8.1 SettingsContentFactory

```java
package org.metalib.papifly.fx.settings.docking;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.SettingsPanel;

public class SettingsContentFactory implements ContentFactory {
    public static final String FACTORY_ID = "settings-panel";
    private final SettingsContext context;

    public SettingsContentFactory(SettingsContext context) {
        this.context = context;
    }

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) return null;
        return new SettingsPanel(context);
    }
}
```

### 8.2 SettingsStateAdapter

```java
package org.metalib.papifly.fx.settings.docking;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.settings.ui.SettingsPanel;

import java.util.Map;

/**
 * Persists which settings category was last selected so it can be
 * restored when the settings dock is reopened.
 */
public class SettingsStateAdapter implements ContentStateAdapter {
    public static final int VERSION = 1;

    @Override
    public String getTypeKey() { return SettingsContentFactory.FACTORY_ID; }

    @Override
    public int getVersion() { return VERSION; }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (content instanceof SettingsPanel panel) {
            return Map.of("activeCategory", panel.getActiveCategoryId());
        }
        return Map.of();
    }

    @Override
    public Node restore(LeafContentData content) {
        // Restoration handled by SettingsContentFactory; state only carries
        // the active category ID for re-selection after restore.
        return null;
    }
}
```

### 8.3 ServiceLoader Registration

**File:** `src/main/resources/META-INF/services/org.metalib.papifly.fx.docking.api.ContentStateAdapter`

```
org.metalib.papifly.fx.settings.docking.SettingsStateAdapter
```

**File:** `src/main/resources/META-INF/services/org.metalib.papifly.fx.settings.api.SettingsCategory`

```
org.metalib.papifly.fx.settings.categories.AppearanceCategory
org.metalib.papifly.fx.settings.categories.WorkspaceCategory
org.metalib.papifly.fx.settings.categories.SecurityCategory
```

---

## 9. Built-in Settings Categories (v1)

### 9.1 AppearanceCategory (UC-1)

```java
package org.metalib.papifly.fx.settings.categories;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.*;

/**
 * Built-in category for theme switching and visual customization.
 */
public class AppearanceCategory implements SettingsCategory {
    private ComboBox<String> themeSelector;
    private boolean dirty = false;

    @Override public String id() { return "appearance"; }
    @Override public String displayName() { return "Appearance"; }
    @Override public int order() { return 10; }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        VBox pane = new VBox(12);
        themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll("Dark", "Light");
        String current = context.getString("appearance.theme", "dark");
        themeSelector.setValue(current.substring(0, 1).toUpperCase() + current.substring(1));
        themeSelector.setOnAction(e -> dirty = true);
        // ... additional controls: font size, density, etc.
        pane.getChildren().addAll(/* labeled controls */);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        String selected = themeSelector.getValue().toLowerCase();
        context.storage().putString(SettingScope.APPLICATION, "appearance.theme", selected);
        Theme theme = "dark".equals(selected) ? Theme.dark() : Theme.light();
        context.themeProperty().set(theme);
        context.storage().save();
        dirty = false;
    }

    @Override
    public void reset(SettingsContext context) {
        String stored = context.getString("appearance.theme", "dark");
        themeSelector.setValue(stored.substring(0, 1).toUpperCase() + stored.substring(1));
        dirty = false;
    }

    @Override public boolean isDirty() { return dirty; }
}
```

### 9.2 WorkspaceCategory (UC-6)

Settings for layout persistence, animation, restore-on-startup. Reads/writes docking framework preferences.

### 9.3 SecurityCategory (UC from Codex S26)

Lists stored secret keys (not values), allows update/revoke/delete. Shows masked indicators for existing secrets.

### 9.4 Module-Contributed Categories (out-of-tree, registered via ServiceLoader)

These categories live in their respective modules and depend on `papiflyfx-docking-settings` (API only):

| Category | Module | Use Cases | ServiceLoader |
|---|---|---|---|
| `GitHubCategory` | `papiflyfx-docking-github` | UC-2: PAT management, host config, commit author | `SettingsCategory` |
| `AuthenticationCategory` | `papiflyfx-docking-login` | Identity provider configuration, active sessions, stored token management | `SettingsCategory` |
| `AiModelsCategory` | (future `papiflyfx-docking-langchain`) | UC-3: API keys, model defaults, provider profiles | `SettingsCategory` |
| `McpServersCategory` | (future `papiflyfx-docking-mcp` or `langchain`) | UC-4: Server CRUD, transport config, trust policy | `SettingsCategory` |
| `EditorCategory` | `papiflyfx-docking-code` | UC-5: Tab size, word wrap, font, auto-save | `SettingsCategory` |
| `HugoCategory` | `papiflyfx-docking-hugo` | UC-7: Binary path, port, build flags | `SettingsCategory` |

### 9.5 AuthenticationCategory — Login Module Integration (from login specs)

The `papiflyfx-docking-login` module contributes an **Authentication** settings category that provides:

1. **Identity provider configuration** — enable/disable providers, configure client IDs, custom scopes, and OIDC discovery endpoints for `GenericOidcProvider`.
2. **Active session overview** — display authenticated user (name, email, avatar), provider, granted scopes, token expiry countdown.
3. **Session management actions** — "Test Connection", "Refresh Token", "Logout", "Switch Account" as `SettingsAction` instances.
4. **Stored token inventory** — list OAuth refresh tokens by provider:subject (keys only, not values), with revoke/delete actions. Delegates to `SecretStore.listKeys()` filtered by `login:oauth:refresh:*` prefix.
5. **Provider-specific sub-settings** — e.g., GitHub Enterprise API URL, Google workspace domain restriction.

```java
package org.metalib.papifly.fx.login.settings;

import org.metalib.papifly.fx.settings.api.*;

/**
 * Settings category contributed by the login module.
 * Registered via ServiceLoader in papiflyfx-docking-login.
 */
public class AuthenticationCategory implements SettingsCategory {

    @Override public String id() { return "authentication"; }
    @Override public String displayName() { return "Authentication"; }
    @Override public int order() { return 25; }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        // Provider list with enable/disable toggles
        // Active session summary (bound to AuthSessionBroker.sessionProperty())
        // Stored refresh token list from context.secretStore().listKeys()
        // Action buttons: Sign In, Refresh, Logout
    }

    @Override
    public void apply(SettingsContext context) {
        // Persist provider enable/disable flags and custom endpoints
        context.storage().save();
    }

    @Override
    public void reset(SettingsContext context) { /* reload from storage */ }
}
```

This category bridges the login module's `AuthSessionBroker` (observable session state) with the settings module's `SettingsContext` (storage + secret store), giving users a single place to manage all authentication configuration.

---

## 10. Maven Module Configuration

### 10.1 `papiflyfx-docking-settings/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking</artifactId>
        <version>0.0.15-SNAPSHOT</version>
    </parent>

    <artifactId>papiflyfx-docking-settings</artifactId>
    <name>papiflyfx-docking-settings</name>
    <description>Extensible application settings component with secure secret storage for PapiflyFX Docking.</description>

    <dependencies>
        <!-- API module (compile scope) -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-api</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- JavaFX (compile scope) -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-base</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-graphics</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <classifier>${javafx.platform}</classifier>
        </dependency>

        <!-- Docks module (test scope only — for integration tests) -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-docks</artifactId>
            <version>${project.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>testfx-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testfx</groupId>
            <artifactId>openjfx-monocle</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <useModulePath>false</useModulePath>
                    <argLine>--enable-native-access=javafx.graphics
                        --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED</argLine>
                    <systemPropertyVariables>
                        <testfx.headless>${testfx.headless}</testfx.headless>
                        <testfx.robot>glass</testfx.robot>
                        <glass.platform>Monocle</glass.platform>
                        <monocle.platform>Headless</monocle.platform>
                        <prism.order>sw</prism.order>
                        <prism.text>t2k</prism.text>
                        <java.awt.headless>false</java.awt.headless>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 10.2 Parent POM Changes

Add the new module to the root `pom.xml`:

```xml
<modules>
    <module>papiflyfx-docking-api</module>
    <module>papiflyfx-docking-settings</module>  <!-- NEW -->
    <module>papiflyfx-docking-docks</module>
    <module>papiflyfx-docking-code</module>
    <module>papiflyfx-docking-tree</module>
    <module>papiflyfx-docking-media</module>
    <module>papiflyfx-docking-hugo</module>
    <module>papiflyfx-docking-github</module>
    <module>papiflyfx-docking-samples</module>
</modules>
```

---

## 11. Dependency Graph

```
papiflyfx-docking-api           (no deps; SecretStore interface moves here once login ships)
        ↑
papiflyfx-docking-settings      (depends on api at compile scope)
     ↑      ↑
     │   papiflyfx-docking-login  (depends on api + settings for SecretStore & SettingsCategory)
     │      ↑
papiflyfx-docking-docks         (depends on api; settings at test scope only)
        ↑
   ┌────┼────┬────┬────┬────┐
   code tree media hugo github   (depend on api; docks at test scope;
   │    │    │    │    │          optionally settings for SettingsCategory)
   └────┴────┴────┴────┘
        ↑
   samples                       (depends on everything)
```

Content modules that want to contribute settings categories add a **compile-scope** dependency on `papiflyfx-docking-settings` (API part) and register their `SettingsCategory` via ServiceLoader. The settings module itself does not depend on any content module.

The login module depends on the settings module for `SecretStore` (secret persistence) and `SettingsCategory` (to contribute the Authentication settings page). It also contributes `SecretStoreSecureAdapter` (§5.8) to bridge its `SecureSecretStore` interface to the unified store. When the `SecretStore` interface is promoted to `papiflyfx-docking-api`, the login module's dependency on the settings module becomes optional (only needed if it contributes a settings category).

---

## 12. Implementation Phases

Implementation status: all phases completed on 2026-03-17.

### Phase 1: Foundation (core SPI + persistence + secret store)

| # | Task | Description | Status |
|---|---|---|---|
| 1.1 | Create Maven module | `papiflyfx-docking-settings` and `papiflyfx-docking-settings-api` with POMs, add to parent | Completed |
| 1.2 | SPI interfaces | `SettingsCategory`, `SettingsContributor`, `SettingsContext`, `SettingScope`, `SettingDefinition`, `SettingType`, `ValidationResult`, `SettingsValidator`, `SettingsAction` | Completed |
| 1.3 | SecretStore | Interface + `KeychainSecretStore`, `EncryptedFileSecretStore`, `InMemorySecretStore`, `SecretStoreFactory` | Completed |
| 1.4 | SettingsStorage | Interface + `JsonSettingsStorage` with application and workspace scope support | Completed |
| 1.5 | SettingsMigrator | Schema versioning and migration contract | Completed |
| 1.6 | Unit tests | Test storage read/write/scope resolution, secret store encrypt/decrypt, migration | Completed |

### Phase 2: UI Shell

| # | Task | Description | Status |
|---|---|---|---|
| 2.1 | SettingsPanel | Two-pane layout: category list + content area + toolbar | Completed |
| 2.2 | SettingsCategoryList | ListView with icon + label, selection binding | Completed |
| 2.3 | SettingsSearchBar | Filter text field wired to category/definition matching | Completed |
| 2.4 | SettingsToolbar | Apply, Reset buttons + dirty indicator + page action slots | Completed |
| 2.5 | SettingControlFactory | Typed control builders for each SettingType | Completed |
| 2.6 | SecretSettingControl | Masked input with reveal toggle | Completed |
| 2.7 | Theme binding | `SettingsPanel` binds to `themeProperty` for live styling | Completed |
| 2.8 | UI tests | Headless TestFX tests for panel, search, apply/reset | Completed |

### Phase 3: Built-in Categories

| # | Task | Description | Status |
|---|---|---|---|
| 3.1 | AppearanceCategory | Theme switcher (Dark/Light), font size, density | Completed |
| 3.2 | WorkspaceCategory | Restore-on-startup, animation, layout presets | Completed |
| 3.3 | SecurityCategory | List stored secret keys, update/revoke UI | Completed |
| 3.4 | ServiceLoader registration | Register built-in categories | Completed |

### Phase 4: Docking Integration

| # | Task | Description | Status |
|---|---|---|---|
| 4.1 | SettingsContentFactory | Create SettingsPanel as dockable content | Completed |
| 4.2 | SettingsStateAdapter | Save/restore active category selection | Completed |
| 4.3 | ServiceLoader registration | Register ContentStateAdapter | Completed |
| 4.4 | Samples integration | Add settings dock to `papiflyfx-docking-samples` demo app | Completed |

### Phase 5: Module Category Contributions

| # | Task | Description | Status |
|---|---|---|---|
| 5.1 | GitHubCategory | PAT management, host config (in `papiflyfx-docking-github`) | Completed |
| 5.2 | EditorCategory | Tab size, font, wrap (in `papiflyfx-docking-code`) | Completed |
| 5.3 | SecretStore bridges | Adapt GitHub `CredentialStore` → `SecretStore`; adapt login `SecureSecretStore` → `SecretStore` | Completed |
| 5.4 | HugoCategory | Binary path, port, flags (in `papiflyfx-docking-hugo`) | Completed |
| 5.5 | AuthenticationCategory | Identity provider config, active sessions, stored tokens (in `papiflyfx-docking-login`) | Completed |

### Phase 6: Advanced Features (v1.1+)

| # | Task | Description | Status |
|---|---|---|---|
| 6.1 | Settings profiles | Export/import non-secret settings (Codex S27, S28) | Completed |
| 6.2 | Keyboard shortcuts category | Keybinding editor with conflict detection (UC-9) | Completed |
| 6.3 | Network/proxy category | HTTP proxy, TLS, timeouts (UC-10) | Completed |
| 6.4 | AI Models category | LangChain provider profiles, API keys (UC-3) | Completed |
| 6.5 | MCP Servers category | Server CRUD, transport config, trust policy (UC-4) | Completed |
| 6.6 | Custom theme editor | Color pickers for all Theme record fields (UC-1b) | Completed |
| 6.7 | Windows Credential Manager | `WinCredSecretStore` for Windows platform | Completed |
| 6.8 | Linux libsecret | `LibsecretSecretStore` for GNOME/KDE | Completed |

---

## 13. Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Settings panel type | Dockable content (not modal dialog) | Consistent with the docking framework; can be pinned, floated, or minimized like any other panel. From Grok: "dockable panel" is the natural fit. |
| JSON serialization | `java.util.Map`-based (no external library) | Consistent with `DockSessionSerializer`. Keeps the dependency footprint minimal. |
| Secret separation | Secrets never stored in `settings.json` | All four research docs + login specs agree: secrets must be in a separate, secure store. |
| Unified SecretStore | Single `SecretStore` interface shared by settings, login, and GitHub modules | Login spec (`plan-codex.md` §12) defines its own `SecureSecretStore`; settings defines `SecretStore`. Unifying avoids parallel hierarchies. Adapters bridge the two APIs. |
| SecretStore placement | Start in `papiflyfx-docking-settings`, promote to `papiflyfx-docking-api` when login module ships | Both settings and login need it. API module is the natural home once both consumers exist. |
| Secret key naming | Hierarchical colon-separated namespace: `<module>:<category>:<id>` | Login spec uses `appId:oauth:refresh:provider:subject`. Settings uses `settings:provider:name`. Unified convention prevents key collisions across modules. |
| Memory hygiene | Zero `byte[]`/`char[]` buffers in `finally` blocks | Login spec `plan-codex.md` §12.4 prescribes this. Critical for `EncryptedFileSecretStore` and any code handling decrypted secrets. |
| Scope resolution | SESSION > WORKSPACE > APPLICATION | Codex scope model. Narrower scope wins. Keeps global defaults overridable per workspace. |
| SPI discovery | ServiceLoader for both `SettingsCategory` and `SettingsContributor` | Established pattern (same as `ContentStateAdapter`). Zero coupling between modules. |
| Encryption algorithm | AES-256-GCM (JDK built-in) | No external crypto dependency. GCM provides authenticated encryption. Login spec's `SecretCipher` (`plan-codex.md` §12.3) uses the same algorithm. |
| Key derivation | PBKDF2 from machine-specific seed | Pragmatic for desktop app. Not a substitute for OS keychain but acceptable fallback. |
| Theme changes | Live via `ObjectProperty<Theme>` | Already implemented in framework. Settings panel just writes to the property. |
| v1 category set | Appearance, Workspace, Security (built-in); GitHub, Editor, Hugo, Authentication (module-contributed) | Focused on what the existing modules need. AI/MCP deferred to when those modules exist. Authentication category contributed by login module. |
| Login integration | Settings provides infrastructure; login contributes `AuthenticationCategory` | Login module's identity provider config, session overview, and stored token management all surface through the settings panel via `SettingsCategory` SPI. |

---

## 14. Open Questions

1. **Should the `SettingsCategory` SPI live in `papiflyfx-docking-api` or in `papiflyfx-docking-settings`?**
   Placing it in `api` avoids a compile dependency on `settings` from content modules. However, it widens the API module's surface. Recommendation: keep it in `settings` for now and move to `api` if multiple modules need it at compile time.

2. **Workspace path resolution** — How does the framework determine the current workspace root? Should it be set programmatically by the host application, or auto-detected from the working directory?

3. **Secret store key conventions** — ~~Should keys be namespaced by module (e.g., `github.pat`, `openai.api-key`) or use a flat namespace?~~ **Resolved**: Use hierarchical colon-separated namespace (`<module>:<category>:<id>`) as defined in §5.3, aligned with login module's `SecretKeyNames`.

4. **Settings as API or settings module?** — The `SettingsCategory`, `SettingsContext`, and `SecretStore` interfaces could live in a thin `papiflyfx-docking-settings-api` sub-module (like how `api` is separate from `docks`). This would allow content modules to declare settings categories without depending on the full settings UI.

5. **Shared SecretStore vs. module-private stores** (from login specs) — The login module defines `SecureSecretStore` and the GitHub module has `CredentialStore`. Should all modules be required to use the unified `SecretStore`, or should the adapters be optional? Recommendation: adapters are provided as convenience; modules may use `SecretStore` directly. The old interfaces remain for backward compatibility but delegate to `SecretStore` internally.

6. **Shared sessions across modules** (from `login-chatgpt.md` §18) — Should the login module provide a shared authenticated session that other modules (GitHub, Hugo, LangChain) can consume, or should each module manage its own credentials independently? This affects whether the Settings → Authentication category shows a single unified session or per-module sessions.

7. **Login module dependency direction** — The login module needs `SecretStore` (from settings). The settings module's `AuthenticationCategory` is contributed by the login module. Should the login module depend on the settings module, or should `SecretStore` be promoted to the API module first to keep the login module independent?

---

## 15. References

- [Claude Opus Research](reseach-claude-opus.md) — Foundation: 10 use cases, SPI design, persistence format
- [Codex Research](reseach-codex.md) — 30 scoped use cases, extensibility model, v1 scope, migration, profiles
- [Gemini Research](reaearch-gemini.md) — Use case categories, encrypted storage emphasis
- [Grok Research](research-grok.md) — Dockable panel recommendation, hierarchical categories, i18n, debug mode
- [Login ChatGPT Spec](../papiflyfx-docking-login/login-chatgpt.md) — Login component concept: provider adapters, session broker, SecureTokenStore, multi-account
- [Login Codex Plan](../papiflyfx-docking-login/plan-codex.md) — Detailed login implementation: SecureSecretStore byte[] API, SecretKeyNames, SecretCipher, memory hygiene, security hardening checklist
- [Login Unified Plan](../papiflyfx-docking-login/plan.md) — Merged login plan: session lifecycle, provider SPI, OsKeychainSecureTokenStore, FileVaultSecureTokenStore
- [Existing codebase patterns](../papiflyfx-docking-docks/README.md) — `ContentFactory`, `ContentStateAdapter`, `CredentialStore`, `DockSessionSerializer`
