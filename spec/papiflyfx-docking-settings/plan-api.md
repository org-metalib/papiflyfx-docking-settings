# papiflyfx-docking-settings-api — Module Plan

## 1. Problem

The current `spec/papiflyfx-docking-settings/plan.md` places all settings interfaces (`SettingsCategory`, `SettingsContext`, `SecretStore`, `SettingsStorage`, etc.) inside the `papiflyfx-docking-settings` module. This creates a tight coupling problem: any module that wants to contribute a settings category or use the secret store must take a compile-scope dependency on the full settings module — including its UI components, built-in categories, and control library.

The `papiflyfx-docking-login` module faces this directly: it needs `SecretStore` for token persistence and `SettingsCategory` to contribute an Authentication page, but should not depend on the settings UI.

## 2. Solution

Split the settings module into two modules following the same pattern as `papiflyfx-docking-api` / `papiflyfx-docking-docks`:

| Module | Contents | Depends on |
|---|---|---|
| `papiflyfx-docking-settings-api` | Pure interfaces, records, enums — zero UI | `papiflyfx-docking-api` only |
| `papiflyfx-docking-settings` | UI shell, built-in categories, storage implementations, controls | `papiflyfx-docking-settings-api` + `papiflyfx-docking-api` |

Content modules (`github`, `code`, `hugo`, `login`, future `langchain`/`mcp`) depend only on `settings-api` at compile scope. They never see the settings UI or its implementations.

## Status

- Completed: created `papiflyfx-docking-settings-api` with its README, Maven module, and the full public SPI/storage/secret contract set.
- Completed: rewired `papiflyfx-docking-settings` to consume `settings-api` and kept UI, persistence, docking integration, and native secret backends in the implementation module.
- Completed: updated the root module order to place `settings-api` after `papiflyfx-docking-api` and added `papiflyfx-docking-login` before samples.
- Completed: `github`, `code`, `hugo`, and `login` compile against `settings-api` only and register their `SettingsCategory` providers through `ServiceLoader`.
- Completed: kept bridge adapters in the contributing implementation modules (`github`, `login`) so the settings split remains acyclic.

## 3. What Lives in settings-api

Everything a module author needs to **declare** settings, **access** storage, and **store** secrets — but nothing that renders UI or implements persistence.

### 3.1 Package Layout

```
papiflyfx-docking-settings-api/
├── pom.xml
├── src/
│   └── main/
│       └── java/org/metalib/papifly/fx/settings/api/
│           ├── SettingsCategory.java
│           ├── SettingsContributor.java
│           ├── SettingsContext.java
│           ├── SettingsAction.java
│           ├── SettingsValidator.java
│           ├── SettingDefinition.java
│           ├── SettingScope.java
│           ├── SettingType.java
│           ├── ValidationResult.java
│           ├── SettingsStorage.java
│           ├── SecretStore.java
│           ├── SecretKeyNames.java
│           └── SettingsMigrator.java
```

### 3.2 Boundary Rule

**In settings-api:**
- Interfaces and records only (no concrete classes with behavior).
- No JavaFX UI imports (`javafx.scene.control.*`, `javafx.scene.layout.*`). Only `javafx.scene.Node` (for `SettingsCategory.buildSettingsPane()` return type and `icon()`), `javafx.scene.paint.Paint`/`javafx.scene.text.Font` (used by `Theme` in the existing API module), and `javafx.beans.property.*` (for observable properties in `SettingsContext`).
- No file I/O, no crypto, no process execution.

**In settings (implementation):**
- `JsonSettingsStorage` (implements `SettingsStorage`)
- `KeychainSecretStore`, `EncryptedFileSecretStore`, `InMemorySecretStore` (implement `SecretStore`)
- `SecretStoreFactory`
- `SettingsPanel`, `SettingsCategoryList`, `SettingsSearchBar`, `SettingsToolbar`
- All `*SettingControl` classes
- `SettingsContentFactory`, `SettingsStateAdapter`
- Built-in categories: `AppearanceCategory`, `WorkspaceCategory`, `SecurityCategory`

**In contributing implementation modules:**
- `SecretStoreCredentialAdapter` in `papiflyfx-docking-github`
- `SecretStoreSecureAdapter` in `papiflyfx-docking-login`

---

## 4. Interfaces

### 4.1 SettingsCategory

```java
package org.metalib.papifly.fx.settings.api;

import javafx.scene.Node;

/**
 * A single page in the settings panel. Discovered via ServiceLoader
 * or registered programmatically through {@link SettingsContributor}.
 *
 * <p>Implementations live in content modules (github, code, login, etc.)
 * and depend only on {@code papiflyfx-docking-settings-api}.
 */
public interface SettingsCategory {

    /** Stable identifier for persistence (e.g., "appearance", "github"). */
    String id();

    /** Human-readable name shown in the category list. */
    String displayName();

    /** Optional icon node for the category list (16×16 recommended). */
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
     * Applies pending changes.
     * Called when the user clicks "Apply" or "Save".
     */
    void apply(SettingsContext context);

    /**
     * Resets all fields to their persisted (or default) values.
     * Called when the user clicks "Reset" or switches away without saving.
     */
    void reset(SettingsContext context);

    /**
     * Returns true if any field has been modified since the last apply/reset.
     */
    default boolean isDirty() { return false; }
}
```

### 4.2 SettingsContributor

```java
package org.metalib.papifly.fx.settings.api;

import java.util.List;

/**
 * A module-level contributor that provides one or more settings categories.
 * Discovered via ServiceLoader. Allows a single module to register
 * multiple categories without a separate service file per category.
 */
public interface SettingsContributor {
    List<SettingsCategory> getCategories();
}
```

### 4.3 SettingsContext

```java
package org.metalib.papifly.fx.settings.api;

import javafx.beans.property.ObjectProperty;
import org.metalib.papifly.fx.docking.api.Theme;

/**
 * Context object provided to {@link SettingsCategory} implementations.
 * Gives read/write access to storage, secrets, and the live theme.
 *
 * <p>Constructed by the settings shell (implementation module) and
 * passed into each category's {@code buildSettingsPane}, {@code apply},
 * and {@code reset} methods.
 */
public record SettingsContext(
    SettingsStorage storage,
    SecretStore secretStore,
    ObjectProperty<Theme> themeProperty,
    SettingScope activeScope
) {
    /** Convenience: read a string setting with default. */
    public String getString(String key, String defaultValue) {
        return storage.getString(activeScope, key, defaultValue);
    }

    /** Convenience: read a boolean setting with default. */
    public boolean getBoolean(String key, boolean defaultValue) {
        return storage.getBoolean(activeScope, key, defaultValue);
    }

    /** Convenience: read an int setting with default. */
    public int getInt(String key, int defaultValue) {
        return storage.getInt(activeScope, key, defaultValue);
    }

    /** Convenience: read a double setting with default. */
    public double getDouble(String key, double defaultValue) {
        return storage.getDouble(activeScope, key, defaultValue);
    }
}
```

### 4.4 SettingScope

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Determines where a setting is persisted and how scopes layer.
 * Resolution order: SESSION > WORKSPACE > APPLICATION.
 */
public enum SettingScope {
    /** Machine-wide, shared across all projects. */
    APPLICATION,
    /** Tied to a specific repository or working directory. */
    WORKSPACE,
    /** Temporary UI state, not persisted beyond the current run. */
    SESSION
}
```

### 4.5 SettingDefinition

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Typed descriptor for a single setting field.
 * Used for automatic UI control generation and settings search indexing.
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

### 4.6 SettingType

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Supported setting value types. The settings implementation module
 * maps each type to an appropriate UI control.
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

### 4.7 SettingsValidator and ValidationResult

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Validates a setting value. Implementations may be synchronous
 * (format checks) or asynchronous (network connectivity tests).
 */
@FunctionalInterface
public interface SettingsValidator<T> {
    ValidationResult validate(T value);
}
```

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Result of a validation check.
 */
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

### 4.8 SettingsAction

```java
package org.metalib.papifly.fx.settings.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A page-level action button displayed in a settings category
 * (e.g., "Test Connection", "Reset to Defaults").
 */
public record SettingsAction(
    String label,
    String description,
    Function<SettingsContext, CompletableFuture<ValidationResult>> handler
) {}
```

### 4.9 SettingsStorage

The storage interface lives in the API module. The implementation (`JsonSettingsStorage`) lives in the settings module.

```java
package org.metalib.papifly.fx.settings.api;

import java.util.Map;
import java.util.Optional;

/**
 * Reads and writes non-secret settings values.
 * Scoped: application-level, workspace-level, and session-level.
 *
 * <p>Concrete implementations (JSON file, Preferences, etc.)
 * live in {@code papiflyfx-docking-settings}.
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

    // --- Scope resolution ---
    /**
     * Returns the effective value across scopes (SESSION > WORKSPACE > APPLICATION).
     */
    default String getEffectiveString(String key, String defaultValue) {
        for (SettingScope scope : SettingScope.values()) {
            Optional<String> val = getRaw(scope, key);
            if (val.isPresent()) return val.get();
        }
        return defaultValue;
    }
}
```

### 4.10 SecretStore

```java
package org.metalib.papifly.fx.settings.api;

import java.util.Optional;
import java.util.Set;

/**
 * Abstraction for secure secret storage. Implementations must never
 * expose raw secret values in logs, toString(), or serialized JSON.
 *
 * <p>Shared across the framework: settings module (API keys, PATs),
 * login module (OAuth refresh tokens), GitHub module (PAT),
 * and any future module needing secret persistence.
 *
 * <p>Concrete implementations (OS keychain, encrypted file, in-memory)
 * live in {@code papiflyfx-docking-settings}.
 */
public interface SecretStore {

    // --- String API (settings, PATs, API keys) ---

    /**
     * Retrieves a secret by key.
     * @param key stable secret identifier (e.g., "github:pat", "settings:openai:api-key")
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
     */
    void clearSecret(String key);

    /**
     * Lists all stored secret keys (not values).
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

### 4.11 SecretKeyNames

```java
package org.metalib.papifly.fx.settings.api;

/**
 * Standardized secret key naming to avoid collisions across modules.
 * Key format: {@code <module>:<category>:<identifier>}.
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

### 4.12 SettingsMigrator

```java
package org.metalib.papifly.fx.settings.api;

import java.util.Map;

/**
 * Migrates settings data from one schema version to the next.
 * Registered per category. Invoked on load when the stored version
 * is older than the current version.
 */
@FunctionalInterface
public interface SettingsMigrator {
    /**
     * Transforms settings data from {@code fromVersion} to {@code fromVersion + 1}.
     * @param data        mutable settings map
     * @param fromVersion the version of the stored data
     * @return the migrated data (may be the same map, modified in place)
     */
    Map<String, Object> migrate(Map<String, Object> data, int fromVersion);
}
```

---

## 5. Maven Configuration

### 5.1 `papiflyfx-docking-settings-api/pom.xml`

Follows the same minimal pattern as `papiflyfx-docking-api`:

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

    <artifactId>papiflyfx-docking-settings-api</artifactId>
    <name>papiflyfx-docking-settings-api</name>
    <description>Lightweight API module: interfaces and records for the settings SPI, storage, and secret management.</description>

    <dependencies>
        <!-- Only dependency: the core docking API (for Theme, Node references) -->
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-api</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

Note: `papiflyfx-docking-api` transitively brings in JavaFX base/graphics which provides `javafx.scene.Node`, `javafx.beans.property.*`, `javafx.scene.paint.Paint`, and `javafx.scene.text.Font` — exactly what the settings API interfaces need. No additional dependencies required.

### 5.2 Root `pom.xml` Module Order

```xml
<modules>
    <module>papiflyfx-docking-api</module>
    <module>papiflyfx-docking-settings-api</module>   <!-- NEW: after api, before settings -->
    <module>papiflyfx-docking-settings</module>
    <module>papiflyfx-docking-docks</module>
    <module>papiflyfx-docking-code</module>
    <module>papiflyfx-docking-tree</module>
    <module>papiflyfx-docking-media</module>
    <module>papiflyfx-docking-hugo</module>
    <module>papiflyfx-docking-github</module>
    <module>papiflyfx-docking-login</module>
    <module>papiflyfx-docking-samples</module>
</modules>
```

### 5.3 Updated `papiflyfx-docking-settings/pom.xml` Dependencies

The settings implementation module replaces its direct API types with a dependency on `settings-api`:

```xml
<dependencies>
    <!-- Settings API (compile scope) -->
    <dependency>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking-settings-api</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- Core docking API (transitive via settings-api, explicit for clarity) -->
    <dependency>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking-api</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- JavaFX controls for UI components -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <classifier>${javafx.platform}</classifier>
    </dependency>

    <!-- Test dependencies -->
    <!-- ... same as before ... -->
</dependencies>
```

### 5.4 Content Module Dependency Pattern

Every content module that contributes a settings category depends on `settings-api` only:

```xml
<!-- Example: papiflyfx-docking-github/pom.xml additions -->
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-settings-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

The full settings module (`papiflyfx-docking-settings`) is **never** a compile dependency of content modules. It is only pulled in by the application assembly (`samples` or host app) at runtime.

---

## 6. Updated Dependency Graph

```
papiflyfx-docking-api                  (no deps)
        ↑
papiflyfx-docking-settings-api         (depends on api)
     ↑           ↑           ↑
     │           │           │
  settings    login    code/github/hugo/tree/media
  (impl)     (impl)   (contribute SettingsCategory
     │                  via ServiceLoader)
     │
  docks  (depends on api; settings at test scope)
     ↑
  samples  (assembles everything at runtime)
```

Key properties:
- **Content modules** compile against `settings-api` only — they never see `SettingsPanel`, `JsonSettingsStorage`, `KeychainSecretStore`, or any UI control.
- **Login module** compiles against `settings-api` for `SecretStore` and `SettingsCategory`. It does NOT depend on `settings` (the UI module).
- **Settings implementation** module compiles against `settings-api` and provides all concrete classes.
- **Samples / host app** depends on both `settings-api` and `settings` (for runtime wiring).

---

## 7. What Moves Out of settings into settings-api

Compared to the original `spec/papiflyfx-docking-settings/plan.md`:

| Interface / Record | Original location | New location |
|---|---|---|
| `SettingsCategory` | `settings/api/` | **`settings-api/`** |
| `SettingsContributor` | `settings/api/` | **`settings-api/`** |
| `SettingsContext` | `settings/api/` | **`settings-api/`** |
| `SettingsAction` | `settings/api/` | **`settings-api/`** |
| `SettingsValidator` | `settings/api/` | **`settings-api/`** |
| `SettingDefinition` | `settings/api/` | **`settings-api/`** |
| `SettingScope` | `settings/api/` | **`settings-api/`** |
| `SettingType` | `settings/api/` | **`settings-api/`** |
| `ValidationResult` | `settings/api/` | **`settings-api/`** |
| `SettingsStorage` | `settings/persist/` | **`settings-api/`** (interface only) |
| `SecretStore` | `settings/secret/` | **`settings-api/`** (interface only) |
| `SecretKeyNames` | `settings/secret/` | **`settings-api/`** |
| `SettingsMigrator` | `settings/persist/` | **`settings-api/`** (interface only) |
| `JsonSettingsStorage` | `settings/persist/` | stays in `settings` |
| `KeychainSecretStore` | `settings/secret/` | stays in `settings` |
| `EncryptedFileSecretStore` | `settings/secret/` | stays in `settings` |
| `InMemorySecretStore` | `settings/secret/` | stays in `settings` |
| `SecretStoreFactory` | `settings/secret/` | stays in `settings` |
| `SecretStoreCredentialAdapter` | `settings/secret/` | implemented in `papiflyfx-docking-github` |
| `SecretStoreSecureAdapter` | `settings/secret/` | implemented in `papiflyfx-docking-login` |
| `SettingsPanel` | `settings/ui/` | stays in `settings` |
| All `*SettingControl` | `settings/ui/controls/` | stays in `settings` |
| `SettingsContentFactory` | `settings/docking/` | stays in `settings` |
| `SettingsStateAdapter` | `settings/docking/` | stays in `settings` |
| Built-in categories | `settings/categories/` | stays in `settings` |

---

## 8. ServiceLoader Registration

ServiceLoader files live in the **module that provides the implementation**, not in the API module.

| Service interface (in settings-api) | Provider (in contributing module) | Registration file location |
|---|---|---|
| `SettingsCategory` | `AppearanceCategory` | `settings/META-INF/services/...SettingsCategory` |
| `SettingsCategory` | `GitHubCategory` | `github/META-INF/services/...SettingsCategory` |
| `SettingsCategory` | `AuthenticationCategory` | `login/META-INF/services/...SettingsCategory` |
| `SettingsCategory` | `EditorCategory` | `code/META-INF/services/...SettingsCategory` |
| `SettingsContributor` | (any multi-category contributor) | respective module |

The `settings-api` module itself contains **no** `META-INF/services/` files — identical to how `papiflyfx-docking-api` works.

---

## 9. Package Name

All interfaces use the single package:

```
org.metalib.papifly.fx.settings.api
```

This mirrors the existing `org.metalib.papifly.fx.docking.api` package in the core API module. The implementation module keeps its deeper package hierarchy (`secret/`, `persist/`, `ui/`, `categories/`, `docking/`) but those packages import from `org.metalib.papifly.fx.settings.api` — never the other way around.

---

## 10. Impact on plan.md

The original `spec/papiflyfx-docking-settings/plan.md` remains the authoritative plan for the full settings feature. This document refines the module split only. Specific updates to plan.md:

1. **§3 Module Structure** — split tree into two: `settings-api/` and `settings/`.
2. **§5.1** — The `SecretStore` placement decision is resolved: it lives in `settings-api` from day one, not "start in settings and promote later."
3. **§10 Maven Configuration** — add `settings-api` POM; update `settings` POM to depend on `settings-api`.
4. **§11 Dependency Graph** — replace with the graph from §6 above.
5. **§12 Phase 1** — task 1.1 becomes "create both Maven modules" and task 1.2 becomes "implement SPI interfaces in `settings-api`."
6. **§14 Open Questions** — questions #1 (SPI placement) and #4 (settings-api sub-module) are now resolved by this plan.
