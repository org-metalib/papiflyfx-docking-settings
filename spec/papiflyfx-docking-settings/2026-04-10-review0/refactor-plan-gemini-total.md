# PapiflyFX Settings Refactor Plan: Unified Total Review

- Date: 2026-04-10
- Review batch: `review0`
- Authoring agent: `@core-architect`
- Proposed implementation lead: `@ops-engineer`
- Required reviewers: `@ui-ux-designer`, `@auth-specialist`, `@spec-steward`, `@qa-engineer`
- Primary modules: `papiflyfx-docking-settings`, `papiflyfx-docking-settings-api`
- Related modules: `papiflyfx-docking-login`, `papiflyfx-docking-github`, `papiflyfx-docking-samples`
- Status: accepted

## Context & Problem Statement

This plan captures a comprehensive, unified refactor initiative for the settings runtime after a thorough review of the current implementation in `papiflyfx-docking-settings` and `papiflyfx-docking-settings-api`. 

While functional and covered by passing tests, the current architecture suffers from several internal inconsistencies and rigid design choices that hinder composability and security:

1. **Rigid, Hardcoded UI:** Many categories bypass definition-driven metadata and use handwritten forms, duplicating logic. The settings module lacks a composable UI structure, making it difficult to adapt to applications that do not fit the hardcoded setting structure.
2. **Fragmented Runtime Ownership:** Runtime ownership is split between host-owned and hidden default bootstraps, leading to hidden coupling between settings, login, and docking restore flows.
3. **Inconsistent Scope Semantics:** The scope selector advertises dynamic scope switching, but multiple categories ignore it and hard-code reads/writes to fixed scopes.
4. **Weak Secret Handling:** Secret administration and fallback storage are weaker than the intended security model, sometimes re-exposing raw stored values.
5. **Polling-Based State:** UI dirty-state tracking is polled on a timer rather than being event-driven.
6. **Out-of-Sync Artifacts:** Spec and progress artifacts overstate completion and drift from implementation reality.

## Goals

1. **Enable Composable UI:** Transition from hardcoded setting structures to a fully composable, definition-driven UI that can easily adapt to varying application needs.
2. **Establish Single Runtime Ownership:** Enforce a single host-owned settings runtime model across settings UI, docking restore, and login integration.
3. **Clarify Scope Behavior:** Make scope behavior explicit, truthful, and predictable for every category.
4. **Harden Secret & Storage Handling:** Improve persistence safety (e.g., atomic saves) and redesign secret handling so raw values are never re-exposed.
5. **Modernize UI State & Styling:** Replace polling-based UI state refresh with deterministic event propagation, and align the settings shell with the shared `-pf-ui-*` token vocabulary.
6. **Sync Documentation:** Bring spec, progress, and validation artifacts back in sync with implementation reality.

## Non-goals

1. No broad redesign of public docking APIs outside narrow seams strictly required by the settings runtime and UI composability.
2. No new built-in settings categories in this phase.
3. No migration to external or cloud-backed settings storage.
4. No cosmetic redesign beyond aligning with UI standards and supporting composability.

## Key Invariants

1. One application instance must own one logical `SettingsRuntime`. Session restore must not silently create a second runtime.
2. The UI must be highly composable, driven by `SettingDefinition` and standard metadata, rather than hardcoded layouts.
3. The UI must never claim scope behavior that the active category does not support.
4. Secret administration must operate on aliases and lifecycle actions (rotate/replace/clear), never redisplaying stored secret values.
5. Apply operations are only enabled when the category is deterministically marked as both dirty and valid.

## Findings Summary

### 1. UI Infrastructure is Rigid and Hardcoded
- Categories (appearance, network, AI models, MCP servers) build most forms manually instead of leaning on `SettingDefinition`, `SettingsValidator`, and typed controls.
- This rigidity prevents the settings module from supporting a composable UI that adapts to different application configurations.

### 2. Runtime Ownership is Split
- `SettingsStateAdapter` and `DefaultSettingsServicesProvider` create their own default runtimes silently.
- Login flows can inadvertently bind to a different runtime than the settings panel.

### 3. Scope Selection is Dishonest
- The toolbar allows selecting `APPLICATION`, `WORKSPACE`, and `SESSION`, but many categories ignore the active scope and directly read/write hardcoded scopes like `SettingScope.APPLICATION`.

### 4. Persistence and Secret Handling Need Hardening
- JSON settings are written directly without an atomic swap strategy.
- `SecurityCategory` reloads stored secret values back into the UI.
- Fallback encrypted-file keys are derived from weak machine metadata.

### 5. UI State is Polling-Based and Styles are Inline
- `SettingsPanel` refreshes toolbar state on a timer instead of reacting to state changes.
- The module bypasses shared UI tokens in favor of inline styles.

## Target Architecture

### 1. Composable, Definition-Driven UI
Refactor the settings module to fully support composable UI. Move away from hardcoded forms.
- `SettingDefinition` and standard metadata must act as the source of truth.
- Introduce a shared form binder/presenter layer around `SettingControl` implementations.
- Custom UI should be strictly reserved for genuinely complex composite workflows (like secret management or profile import/export).

### 2. Single Runtime Ownership Model
The host application must explicitly own settings runtime construction and inject it into all integration points.
- Keep `SettingsRuntime` as the composition root.
- Remove hidden default bootstraps in favor of explicit host wiring or a bridge that fails closed.

### 3. Explicit Category Scope Contracts
Every category must explicitly declare its scope model (fixed, definition-driven, or custom).
- The toolbar must dynamically reflect only the scopes valid for the active category.
- Reads and writes must be derived from `SettingDefinition.scope()`.

### 4. Hardened Storage and Secret Admin
- Redesign secret management around alias inventory and lifecycle actions (replace/clear/delete). Do not expose raw values.
- Storage implementations must own persistence, introducing atomic JSON writes and clear corruption recovery paths.

## Workstreams

### Workstream A — Composable UI & Shared Validation
**Objective:** Eliminate hardcoded settings structures and introduce a composable, definition-driven form infrastructure.
- **Tasks:**
  1. Introduce a shared form binder around existing `SettingControl` implementations to support composability.
  2. Refactor built-in categories to use `SettingControlFactory` and declarative definitions.
  3. Centralize validation aggregation and replace ad-hoc dirty tracking with observable/property-backed state.
- **Acceptance:** Settings UI is generated compositionally from definitions where possible; manual form code is drastically reduced.

### Workstream B — Runtime Ownership Consolidation
**Objective:** Guarantee a single, host-injected runtime.
- **Tasks:**
  1. Refactor `SettingsStateAdapter` and `DefaultSettingsServicesProvider` to remove hidden default runtime creation.
  2. Verify login and docking restore paths inject and reuse the exact same runtime instance.
- **Acceptance:** No restore or `ServiceLoader` path silently creates a secondary runtime.

### Workstream C — Scope Semantics Cleanup
**Objective:** Align selected scope, declared scope, and persistence targets.
- **Tasks:**
  1. Classify all categories by scope policy.
  2. Remove hard-coded scope usage in favor of `SettingDefinition.scope()`.
  3. Ensure the UI toolbar only presents valid scope options for the active category.
- **Acceptance:** Categories respect scope choices, and the UI never over-promises scope capabilities.

### Workstream D — Secret Handling & Persistence Hardening
**Objective:** Secure secret administration and make file writes resilient.
- **Tasks:**
  1. Redesign `SecurityCategory` to never reload stored secret values. Implement lifecycle actions (rotate/delete).
  2. Implement atomic save behavior for JSON settings and define a corruption recovery strategy.
  3. Revisit the encrypted-file fallback threat model and document constraints.
- **Acceptance:** Atomic writes prevent partial file corruption. Stored secrets cannot be viewed via the UI.

### Workstream E — UI State and Standards Alignment
**Objective:** Remove polling and adopt shared design tokens.
- **Tasks:**
  1. Remove periodic timer loops for UI refresh; adopt event-driven dirty/valid state propagation.
  2. Replace inline styles with `-pf-ui-*` tokens and shared CSS classes.
- **Acceptance:** UI updates are deterministic and instantaneous; styling is consistent with global themes.

### Workstream F — Spec and Validation Repair
**Objective:** Restore accuracy to project documentation.
- **Tasks:**
  1. Overhaul `README.md`, `plan.md`, and `progress.md` to reflect current reality and this new plan.
  2. Maintain a clear `validation.md` logging residual risks.

## Proposed Sequence

1. **Phase 1 - Runtime & Scope Safety:** Lock down single runtime ownership. Fix misleading scope behavior.
2. **Phase 2 - Composable UI Refactor:** Migrate standard categories to the new composable, definition-driven UI binder.
3. **Phase 3 - Security & Storage Hardening:** Redesign the security page (no raw values). Add atomic file writes.
4. **Phase 4 - State & Styling Cleanup:** Remove polling timers. Apply shared UI tokens.
5. **Phase 5 - Documentation Sync:** Update all spec and progress artifacts.

## Validation Strategy

- **Automated:** Run focused module tests (`./mvnw -pl papiflyfx-docking-settings -am test`). Add regression tests for injected runtime reuse, composable form generation, scope switching, atomic JSON writes, and secret fallback paths.
- **Manual:** Verify category switching dynamically updates scope options. Validate that custom applications can compose settings without modifying hardcoded layouts. Exercise secret replacement flows without revealing values. Ensure theme updates apply without timer polling.

## Risks and Mitigations

- **Risk:** Runtime bootstrap changes break login or sample integration.
  - *Mitigation:* Implement robust integration tests before merging ownership refactors.

## Definition of Done
- The settings module supports a composable UI driven by definitions, minimizing hardcoded layouts.
- Hidden default-runtime creation is eliminated.
- Scope behavior is truthful for every category.
- Timer-based toolbar refresh is removed.
- Secret admin flows no longer re-expose stored values.
- Atomic file writes are implemented.
- Review gates are satisfied and spec artifacts are fully updated.