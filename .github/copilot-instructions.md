# Copilot instructions for this repository

Purpose
- Keep Copilot completions accurate for the split `papiflyfx-docking-settings` repository and avoid cross-repo drift.

Repository at a glance
- Multi-module Maven project with the parent POM at the repository root.
- This repository was extracted from the PapiflyFX Docking monorepo. Keep changes scoped to this repository's modules.
- Main Java package root: `org.metalib.papifly.fx.settings`.
- Modules:
  - `papiflyfx-docking-settings-api` - public settings SPI and shared contracts such as `SettingsCategory`, `SettingsContext`, `SettingsStorage`, `SecretStore`, `SettingDefinition`, `SettingScope`, `SettingsServicesProvider`, `SettingsMigrator`, and `ValidationResult`.
  - `papiflyfx-docking-settings` - JavaFX settings runtime and UI: `SettingsRuntime`, `SettingsPanel`, `DefinitionFormBinder`, built-in categories, JSON persistence, secret-store implementations, and docking integration via `SettingsContentFactory` / `SettingsStateAdapter`.
- Project docs, plans, and review notes live under `spec/papiflyfx-docking-settings/`.

Lead roles
- `@ops-engineer` - Maven build structure, dependency management, settings runtime, release configuration, and build validation.
- `@ui-ux-designer` - theme primitives, CSS, shared UI polish, accessibility-sensitive states, and layout ergonomics.
- `@qa-engineer` - test strategy, headless profiles, regression coverage, and deterministic validation.

Hard rules
- Do not change Maven `groupId`, module `artifactId`, or Java package names.
- For repository split maintenance, do not change Java source, public APIs, ServiceLoader descriptors, persistence formats, or theme assets.
- For feature or bug-fix work, preserve public APIs, ServiceLoader descriptors, persistence formats, and theme assets unless the task explicitly requires changing them.
- Same-repository PapiflyFX dependencies may use `${project.version}`.
- Cross-repository PapiflyFX dependencies must use `${papiflyfx.version}` or BOM management.
- Do not push split repositories until remotes are created explicitly by the project owner.

Build and validation
- Use the Maven Wrapper: `./mvnw` on macOS/Linux, `mvnw.cmd` on Windows.
- Preferred validation command:
  - `./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true clean verify`
- Common focused commands:
  - `./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split clean package`
  - `./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true test`
  - `./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -pl papiflyfx-docking-settings-api -am clean package`
  - `./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -pl papiflyfx-docking-settings -am -Dtestfx.headless=true test`
  - `./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -pl papiflyfx-docking-settings -am -Dtest=SettingsPanelFxTest test`
- CI uses Zulu JDK+FX `25.0.1`.
- When reproducing CI behavior for JavaFX/TestFX runs, add `-Djava.awt.headless=true`.

Architecture boundaries
- External consumers should depend on `papiflyfx-docking-settings-api` only. Keep UI, storage, secret implementations, and docking wiring in `papiflyfx-docking-settings`.
- The settings system assumes a single shared `SettingsRuntime`. Hosts must register it before ServiceLoader-based consumers restore docking content:
  - `SettingsStateAdapter.setSharedRuntime(runtime)`
  - `DefaultSettingsServicesProvider.setSharedRuntime(runtime)`
- ServiceLoader descriptors live in `papiflyfx-docking-settings/src/main/resources/META-INF/services/`.
- Preserve `APPLICATION`, `WORKSPACE`, and `SESSION` scope semantics when editing storage, toolbar, or category behavior.
- Preserve atomic write and corruption-recovery behavior when editing `persist/`, `secret/`, `internal/`, or `runtime/`.
- Secret storage is platform-aware: `KeychainSecretStore`, `LibsecretSecretStore`, and `WinCredSecretStore` wrap encrypted-file fallback behavior.
- `SettingsPanel` loads `/org/metalib/papifly/fx/settings/ui/settings.css` and relies on `-pf-ui-*` theme tokens. Preserve token-driven styling and theme-aware behavior.
- Prefer the existing `DefinitionFormBinder` + typed `SettingDefinition<?>` pattern for settings UI instead of ad hoc form logic.

Testing guidance
- Tests currently live in `papiflyfx-docking-settings/src/test/java`.
- Main regression anchors:
  - `JsonSettingsStorageTest`
  - `EncryptedFileSecretStoreTest`
  - `AtomicFileWriterTest`
  - `SettingsRuntimeTest`
  - `SettingsPanelFxTest`
- Check `target/surefire-reports` after Maven test runs.
- Prefer focused regression tests near the behavior being changed. UI changes should keep TestFX coverage current.

Useful search anchors
- Core runtime/UI:
  - `SettingsRuntime`
  - `SettingsPanel`
  - `DefinitionFormBinder`
  - `SettingsToolbar`
  - `SettingsSearchBar`
  - `SettingsCategoryList`
  - `ThemeStyleSupport`
  - `SettingsUiStyles`
- Docking and service wiring:
  - `SettingsContentFactory`
  - `SettingsStateAdapter`
  - `DefaultSettingsServicesProvider`
  - `SettingsServicesProvider`
- Persistence and secrets:
  - `JsonSettingsStorage`
  - `JsonSettingsStorageFactory`
  - `SettingsJsonCodec`
  - `AtomicFileWriter`
  - `EncryptedFileSecretStore`
  - `KeychainSecretStore`
  - `LibsecretSecretStore`
  - `WinCredSecretStore`
  - `SecretStoreFactory`
- API contracts:
  - `SettingsCategory`
  - `SettingsContributor`
  - `SettingsContext`
  - `SettingDefinition`
  - `SettingScope`
  - `SettingsAction`
  - `SettingsMigrator`
  - `SettingsValidator`
  - `ValidationResult`
- Built-in category entry points:
  - `AppearanceCategory`
  - `SecurityCategory`
  - `WorkspaceCategory`
  - `AiModelsCategory`
  - `McpServersCategory`
  - `NetworkCategory`
  - `ProfilesCategory`
  - `KeyboardShortcutsCategory`

Where to look first
- Root `pom.xml` for module structure and artifact coordinates.
- `papiflyfx-docking-settings-api/README.md` for SPI boundaries.
- `papiflyfx-docking-settings/README.md` for runtime, storage, and UI behavior.
- `papiflyfx-docking-settings/src/main/resources/META-INF/services/` for ServiceLoader wiring.
- `spec/papiflyfx-docking-settings/plan.md` and `spec/papiflyfx-docking-settings/plan-api.md` for design intent.

Quick checklist
- Build or verify with the split-local Maven repo.
- Run settings-module tests with `-Dtestfx.headless=true`.
- Search the API module first before adding implementation coupling.
- Inspect matching tests before editing persistence, secret storage, or docking restoration behavior.
- Keep styling changes aligned with `settings.css` and the existing `-pf-ui-*` token vocabulary.

End of instructions
