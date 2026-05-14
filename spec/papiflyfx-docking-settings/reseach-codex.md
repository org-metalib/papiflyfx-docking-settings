# PapiflyFX Docking Settings Component Research (Codex)

## Objective

Create a framework-level settings component that is extensible by PapiflyFX Docking modules and supports both simple preferences and complex secure configuration such as themes, MCP server definitions, LangChain model profiles, and GitHub personal access tokens.

## Why A Shared Settings Component Makes Sense

PapiflyFX Docking already has reusable framework concepts such as `Theme`, `ContentFactory`, and `ContentStateAdapter`. A settings component should follow the same direction:

1. framework-owned shell
2. module-owned setting contributions
3. versioned persistence
4. explicit secret handling
5. UI and validation that stay consistent across modules

Without this, each module will likely create its own ad hoc dialog, persistence format, validation rules, and secret storage behavior.

## Core Requirements

1. The settings component must be extensible so each module can contribute its own pages, groups, and fields.
2. The component must support both primitive values and structured values.
3. Secrets must be handled separately from normal persisted settings.
4. Settings must support multiple scopes: application, workspace, project, and session where needed.
5. The UI must support validation, defaults, reset, and test/verify actions.
6. Complex settings such as theme editing or MCP server lists must allow custom editors, not only simple text fields.
7. The framework should support search and categorization so settings remain usable as more modules are added.
8. Settings definitions should be versioned to support schema evolution and migration.

## Use Cases

| ID | Area | Use case | Scope | Secret |
|---|---|---|---|---|
| S1 | Appearance | Switch between built-in light and dark themes for the entire docking framework. | Application | No |
| S2 | Appearance | Create and save custom theme presets by editing colors, fonts, border width, corner radius, tab height, and drag-drop hint colors. | Application | No |
| S3 | Appearance | Apply a global accent color that propagates to code, tree, media, and docking headers where theme mapping exists. | Application | No |
| S4 | Appearance | Override theme behavior per component type, for example one theme for code editor and another for tree or media viewer. | Workspace | No |
| S5 | Docking | Enable or disable restore-last-session on startup. | Application | No |
| S6 | Docking | Configure auto-save of layout state and choose where layout/session files are persisted. | Application or Workspace | No |
| S7 | Docking | Tune framework interaction preferences such as drag hint visibility, animation duration, minimum tab width, or minimized bar behavior. | Application | No |
| S8 | Docking | Reset docking appearance and layout behavior back to framework defaults. | Application | No |
| S9 | LangChain | Choose the active LLM provider profile such as OpenAI, Anthropic, Gemini, or Ollama. | Workspace | No |
| S10 | LangChain | Store API keys for cloud model providers without leaking them into session or layout JSON. | Application or Workspace | Yes |
| S11 | LangChain | Configure model defaults such as model id, temperature, max tokens, streaming, timeout, and retry policy. | Workspace | No |
| S12 | LangChain | Configure local model endpoints such as Ollama base URL and default local models. | Workspace | No |
| S13 | LangChain | Run a "test connection" action from the settings page and show health status for the selected provider profile. | Session or Workspace | No |
| S14 | MCP | Register one or more MCP servers with transport settings such as stdio command, environment variables, HTTP URL, or SSE endpoint. | Workspace | Mixed |
| S15 | MCP | Enable, disable, or reorder MCP servers per workspace depending on which tools should be available in that context. | Workspace | No |
| S16 | MCP | Define trust policy for MCP tools, for example disabled by default, ask before first use, or always allowed for trusted servers. | Workspace | No |
| S17 | MCP | Store tokens or auth headers needed by specific MCP servers in secure storage. | Workspace | Yes |
| S18 | GitHub | Store a GitHub PAT for the GitHub toolbar/component separately from normal persisted settings. | Application or Workspace | Yes |
| S19 | GitHub | Configure GitHub host settings for public GitHub or GitHub Enterprise, including API base URL and default repository owner. | Workspace | No |
| S20 | GitHub | Choose repository-specific behavior such as auto-fetch interval, preferred default branch handling, or pull request target branch defaults. | Workspace or Project | No |
| S21 | Hugo / External Tools | Configure executable path, preview behavior, and default site directory for Hugo integration. | Workspace | No |
| S22 | Hugo / External Tools | Validate external tool availability and show actionable error messages in the settings UI. | Session | No |
| S23 | Code Editor | Configure editor defaults such as font size, tab width, soft wrap, line numbers, current line highlight, and search behavior. | Application or Workspace | No |
| S24 | Tree | Configure tree component behavior such as row height, icon size, search matching mode, keyboard navigation options, and hover details. | Application or Workspace | No |
| S25 | Media | Configure media defaults such as autoplay, loop, mute-on-hidden, default zoom behavior, or playback resume policy. | Application or Workspace | No |
| S26 | Security | Offer a page that lists stored secrets by alias and allows update, revoke, or delete without exposing raw values. | Application | Yes |
| S27 | Profiles | Allow export/import of non-secret settings profiles to move preferences across machines or sample apps. | Application | No |
| S28 | Profiles | Support multiple named profiles, such as "work", "demo", "ai-experiments", and "minimal". | Application or Workspace | No |
| S29 | Diagnostics | Provide a settings search bar so a user can quickly find "theme", "pat", "mcp", or "ollama" without knowing the owning module. | Session | No |
| S30 | Diagnostics | Show validation state, dirty state, and whether changes require restart, reconnect, or live rebind. | Session | No |

## High-Value Scenarios

### 1. Framework Theme Management

A developer opens `Settings`, changes from dark to light theme, edits accent and drop hint colors, and applies the result live. The framework updates all theme-bound components consistently.

This is likely the first visible use case because the framework already exposes a shared `Theme` model.

### 2. LangChain Provider Setup

A developer adds two model profiles:

1. OpenAI for cloud usage
2. Ollama for local usage

They store the OpenAI API key securely, configure default model ids, and test both profiles from the settings screen.

### 3. MCP Workspace Setup

A developer defines several MCP servers for a workspace:

1. local filesystem tools
2. git tools
3. issue tracker tools

The settings component lets the developer enable only the trusted servers for that workspace and store any required tokens securely.

### 4. GitHub Component Authentication

A developer configures the GitHub toolbar with:

1. a PAT
2. GitHub Enterprise API base URL
3. default repository owner

The toolbar can then connect without embedding credentials into the docking session state.

### 5. Per-Workspace Experience

A project can load workspace-specific settings such as:

1. a light theme for demos
2. a local Ollama endpoint
3. GitHub Enterprise host
4. MCP servers relevant to that repository

This avoids forcing one global configuration for every project.

## Extensibility Model

The settings component should be designed around contributions rather than a fixed hardcoded form.

### Recommended extension points

1. `SettingsContributor`
   Contributes one or more categories, groups, and setting definitions.
2. `SettingDefinition<T>`
   Declares stable key, label, description, type, scope, default value, validation rules, and whether the value is secret.
3. `SettingsEditorFactory`
   Creates custom editors for complex values such as a theme designer, model profile table, or MCP server list.
4. `SettingsValidator`
   Performs synchronous or asynchronous validation and returns errors, warnings, or info messages.
5. `SettingsAction`
   Allows page-level actions such as `Test Connection`, `Reset To Defaults`, `Generate Token Help`, or `Reload Theme`.
6. `SettingsMigration`
   Migrates versioned settings data when keys or structures change.

### Recommended category model

1. `Appearance`
2. `Docking`
3. `AI / LangChain`
4. `MCP`
5. `GitHub`
6. `External Tools`
7. `Editors`
8. `Security`
9. `Advanced`

### Recommended data types

1. boolean
2. integer / double
3. string
4. enum
5. file/path
6. color/font/theme
7. list/table of structured records
8. secure secret reference

## Persistence Model

The most important design rule is to separate secrets from normal settings.

### Non-secret settings

Persist in a normal application/workspace settings store, for example JSON or properties-based storage.

Typical examples:

1. selected theme id
2. Ollama base URL
3. preferred model name
4. GitHub Enterprise host URL
5. docking animation settings

### Secret settings

Persist in a dedicated secret store abstraction. The normal settings store should keep only a stable alias or key reference when necessary.

Typical examples:

1. OpenAI API key
2. Anthropic API key
3. GitHub PAT
4. MCP auth token

### Scope model

Recommended scopes:

1. `Application`
   Shared across all projects on the machine.
2. `Workspace`
   Tied to a specific repository or working directory.
3. `Project`
   Optional finer-grained override if a workspace contains multiple projects.
4. `Session`
   Temporary UI state, not meant for long-term persistence.

## UI Expectations

The settings UI should support both simple and advanced modules without becoming cluttered.

Recommended layout:

1. category navigation on the left
2. searchable settings list
3. page content in the center
4. inline validation and help text
5. `Apply`, `Save`, `Cancel`, `Reset`, and page-specific action buttons
6. status area for validation, connectivity tests, and restart requirements

## Recommended v1 Scope

For a first implementation, the settings component should probably deliver:

1. a reusable settings shell UI
2. a contributor API for modules
3. built-in support for primitive fields plus one complex custom editor path
4. application and workspace scopes
5. a non-secret settings store
6. a secret store abstraction
7. initial pages for:
   - appearance/theme
   - LangChain provider profiles
   - MCP server definitions
   - GitHub credentials and host settings

## Open Questions

1. Should settings open as a dockable content pane, a modal dialog, or both?
2. Should workspace settings automatically override application settings, or should the user see merge precedence explicitly?
3. What secret store should back secure values in v1?
4. Should theme editing stay framework-generic, or should feature modules be allowed to expose extra theme tokens?
5. Do MCP server definitions belong in framework settings, or should they live in an AI-specific module that plugs into the shared shell?

## Recommendation

Treat the settings component as framework infrastructure, not as a single page. The framework should own the shell, contributor API, persistence contracts, validation flow, and secret separation. Feature modules should only contribute their own settings definitions and optional custom editors.

That design will scale cleanly from simple theme toggles to complex cases such as MCP registries, LangChain model profiles, and GitHub authentication.
