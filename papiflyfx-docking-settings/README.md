# papiflyfx-docking-settings

The dockable settings shell for PapiflyFX applications. This module provides the settings panel UI, JSON settings persistence, secure secret storage implementations, built-in categories, and docking integration.

## Features

- **Composable UI architecture:** simplified category implementation via `DefinitionFormBinder` and typed `SettingDefinition` list.
- **Event-driven state logic:** removed all polling loops; UI state (dirty/valid/apply) is driven by property bindings.
- **Tokenized styling:** unified visual identity using `-pf-ui-*` CSS tokens for instant, consistent theme switching.
- **Secure secret handling:** re-exposure prevention (set-only UI), atomic persistence, and corruption recovery.
- **Multi-scope support:** native handling of APPLICATION, WORKSPACE, and SESSION scopes with dynamic toolbar updates.
- **Docking integration:** through `SettingsContentFactory` and `SettingsStateAdapter` with single shared runtime ownership.

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-settings</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Runtime

The settings system uses a single shared `SettingsRuntime` model. 

1. **Host initialization:** The application host (e.g., `SamplesRuntimeSupport`) must call `SettingsStateAdapter.setSharedRuntime(runtime)` and `DefaultSettingsServicesProvider.setSharedRuntime(runtime)` before any consumers attempt to access settings via ServiceLoader.
2. **Binding:** The `SettingsPanel` automatically binds to the runtime's theme property and applies styles via `settings.css` using the standard PapiflyFX token vocabulary.
3. **Composability:** New categories should implement `SettingsCategory` and use `DefinitionFormBinder` to generate their UI from a list of definitions, ensuring consistent layout and automatic dirty/valid state propagation.

## SPI Boundary

Content modules should depend on `papiflyfx-docking-settings-api` only. This implementation module stays on the application/runtime side.
