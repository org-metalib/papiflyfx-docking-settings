# PapiflyFX Docking Settings Component — Use Cases

## Overview

A `papiflyfx-docking-settings` module providing an extensible, programmatic settings UI for the docking framework. The component follows the same content-module pattern as other modules (code, github, hugo): a main content `Node`, a `ContentFactory`, and a `ContentStateAdapter`. Settings are organized into categories, each contributed by its own `SettingsCategory` SPI, so any module can register its own settings page without modifying the core.

---

## Architecture Principles

- **Extensibility via SPI** — Each settings category is a `SettingsCategory` registered through `ServiceLoader`. Modules contribute their own settings pages independently.
- **No CSS, no FXML** — All UI is programmatic JavaFX, consistent with the rest of the framework.
- **Observable properties** — Settings values are exposed as JavaFX `Property` objects so that changes propagate live (e.g., theme switches apply immediately).
- **Persistence** — Settings are serialized to a JSON file using the existing `java.util.Map`-based JSON approach (no external JSON library), matching `DockSessionSerializer`.
- **Secure storage** — Secrets (API keys, PATs) are stored separately from plain settings, using OS keychain integration or at minimum an encrypted local file.

---

## Use Cases

### UC-1: Color Themes

| Field | Value |
|---|---|
| **Actor** | Application user |
| **Precondition** | Settings dock is open, "Appearance" category is selected |
| **Description** | User selects a built-in theme (Dark, Light) or configures a custom theme by adjusting individual color properties from the `Theme` record (background, accent, text, border, header colors, etc.) |
| **Postcondition** | `DockManager.themeProperty()` is updated; all docked content reflects the new theme live |

**Sub-cases:**
- UC-1a: Switch between built-in presets (Dark / Light)
- UC-1b: Customize individual theme colors via color pickers
- UC-1c: Adjust font family, size, and weight for header and content fonts
- UC-1d: Export / import custom theme as JSON
- UC-1e: Reset theme to defaults

---

### UC-2: GitHub PAT Management

| Field | Value |
|---|---|
| **Actor** | Developer using `papiflyfx-docking-github` |
| **Precondition** | Settings dock is open, "GitHub" category is selected |
| **Description** | User enters or updates a GitHub Personal Access Token. The token is validated against the GitHub API (`GET /user`) and stored securely. The `GitHubApiService.tokenSupplier` and `JGitRepository.credentialsProviderSupplier` read from this store. |
| **Postcondition** | GitHub operations (push, pull, PR creation) use the configured PAT without re-prompting |

**Sub-cases:**
- UC-2a: Enter a new PAT with scope validation feedback
- UC-2b: Revoke / clear a stored PAT
- UC-2c: Test connection (verify token validity and display authenticated username)
- UC-2d: Configure default commit author name and email

---

### UC-3: LangChain Model API Keys

| Field | Value |
|---|---|
| **Actor** | Developer using `papiflyfx-docking-langchain` |
| **Precondition** | Settings dock is open, "AI Models" category is selected |
| **Description** | User configures API keys for LLM providers (OpenAI, Anthropic, Ollama endpoint URL). Keys are stored securely and supplied to `PapiflyAiService` at runtime. |
| **Postcondition** | LangChain4j services can authenticate with the configured providers |

**Sub-cases:**
- UC-3a: Add / update API key per provider (OpenAI, Anthropic, Google, etc.)
- UC-3b: Configure local model endpoint (Ollama base URL)
- UC-3c: Select default model for chat, code analysis, and embedding tasks
- UC-3d: Set token budget / max-tokens per request
- UC-3e: Test provider connectivity and display model list

---

### UC-4: MCP Server Configuration

| Field | Value |
|---|---|
| **Actor** | Developer integrating MCP (Model Context Protocol) tools |
| **Precondition** | Settings dock is open, "MCP Servers" category is selected |
| **Description** | User adds, edits, or removes MCP server definitions. Each server entry specifies a transport type (stdio, SSE), command or URL, optional arguments, and environment variables. |
| **Postcondition** | MCP clients in the framework can connect to the configured servers |

**Sub-cases:**
- UC-4a: Add a new MCP server (name, transport type, command/URL, args, env vars)
- UC-4b: Edit an existing MCP server configuration
- UC-4c: Remove an MCP server
- UC-4d: Enable / disable individual servers without deleting them
- UC-4e: Test server connectivity and list available tools
- UC-4f: Import MCP server list from a `mcp.json` / `claude_desktop_config.json` file

---

### UC-5: Editor Preferences

| Field | Value |
|---|---|
| **Actor** | User of `papiflyfx-docking-code` |
| **Precondition** | Settings dock is open, "Editor" category is selected |
| **Description** | User configures code editor behavior: tab size, soft tabs, word wrap, line numbers, font, minimap visibility, bracket matching, auto-indent. Changes apply to all open `CodeEditor` instances. |
| **Postcondition** | All open and future `CodeEditor` instances reflect the new settings |

**Sub-cases:**
- UC-5a: Set tab width and soft tabs (spaces vs. tabs)
- UC-5b: Toggle line numbers, word wrap, bracket matching
- UC-5c: Set editor font family and size (separate from theme content font)
- UC-5d: Configure default language / lexer for new files
- UC-5e: Set auto-save interval or disable auto-save

---

### UC-6: Layout & Workspace Preferences

| Field | Value |
|---|---|
| **Actor** | Application user |
| **Precondition** | Settings dock is open, "Workspace" category is selected |
| **Description** | User configures global docking behavior: default dock state for new panels, animation duration, double-click header behavior (maximize vs. float), restore-on-startup behavior. |
| **Postcondition** | `DockManager` behavior reflects the configured preferences |

**Sub-cases:**
- UC-6a: Toggle "restore last session on startup"
- UC-6b: Set default dock state for new panels (docked / floating)
- UC-6c: Configure drag-and-drop sensitivity thresholds
- UC-6d: Toggle animation and set duration
- UC-6e: Manage saved layout presets (save current / load / delete)

---

### UC-7: Hugo Server Configuration

| Field | Value |
|---|---|
| **Actor** | User of `papiflyfx-docking-hugo` |
| **Precondition** | Settings dock is open, "Hugo" category is selected |
| **Description** | User configures the Hugo executable path, default port, auto-refresh behavior, and build flags used by the Hugo preview dock. |
| **Postcondition** | Hugo preview docks use the configured settings for server lifecycle management |

**Sub-cases:**
- UC-7a: Set Hugo binary path (or use system PATH)
- UC-7b: Configure default server port and bind address
- UC-7c: Set additional Hugo build flags (e.g., `--buildDrafts`, `--environment`)
- UC-7d: Toggle live-reload on file change

---

### UC-8: Plugin / Extension Management

| Field | Value |
|---|---|
| **Actor** | Framework developer or power user |
| **Precondition** | Settings dock is open, "Extensions" category is selected |
| **Description** | User views and manages registered content modules and their `ContentFactory` / `ContentStateAdapter` registrations. Allows enabling or disabling optional modules. |
| **Postcondition** | Only enabled modules contribute content factories and settings categories |

**Sub-cases:**
- UC-8a: List all registered content modules with version info
- UC-8b: Enable / disable individual modules
- UC-8c: View module dependencies
- UC-8d: Check for available updates (if a module registry is configured)

---

### UC-9: Keyboard Shortcuts

| Field | Value |
|---|---|
| **Actor** | Application user |
| **Precondition** | Settings dock is open, "Keybindings" category is selected |
| **Description** | User views and customizes keyboard shortcuts for framework actions (close tab, split, maximize, navigate tabs, editor actions). Conflict detection prevents duplicate bindings. |
| **Postcondition** | Key bindings are updated and persisted across sessions |

**Sub-cases:**
- UC-9a: View all registered shortcuts grouped by category
- UC-9b: Rebind a shortcut with conflict detection
- UC-9c: Reset shortcuts to defaults
- UC-9d: Import / export keybinding profiles

---

### UC-10: Proxy & Network Configuration

| Field | Value |
|---|---|
| **Actor** | User behind a corporate proxy |
| **Precondition** | Settings dock is open, "Network" category is selected |
| **Description** | User configures HTTP/HTTPS proxy settings, timeouts, and SSL certificate trust. These settings are consumed by `HttpClient` instances used by GitHub API, LangChain providers, and MCP transports. |
| **Postcondition** | All outbound HTTP requests use the configured proxy and TLS settings |

**Sub-cases:**
- UC-10a: Set HTTP/HTTPS proxy host, port, and optional auth credentials
- UC-10b: Define no-proxy host patterns
- UC-10c: Configure connection and read timeouts
- UC-10d: Import custom CA certificates for corporate TLS inspection

---

## SPI Extension Point

Any module can contribute a settings category by implementing:

```java
public interface SettingsCategory {
    String id();
    String displayName();
    Node icon();
    Node buildSettingsPane(SettingsContext context);
    void apply(SettingsContext context);
    void reset(SettingsContext context);
}
```

Registration via `META-INF/services/org.metalib.papifly.fx.docking.settings.SettingsCategory`.

The settings dock discovers all registered categories at startup and renders a navigation list on the left with the active category's pane on the right — a standard two-pane settings layout.

---

## Persistence Format

```
~/.papiflyfx/
  settings.json          # plain settings (theme, editor prefs, layout prefs)
  mcp-servers.json       # MCP server definitions
  keybindings.json       # custom keyboard shortcuts
  secrets/               # encrypted API keys and tokens (or OS keychain)
```

All JSON produced using the existing `java.util.Map`-based serialization approach — no external JSON library required.