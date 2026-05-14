# PapiflyFX Settings Refactor Plan

- Date: 2026-04-10
- Review batch: `review0`
- Lead agent: `@ops-engineer`
- Required reviewers: `@qa-engineer`, `@ui-ux-designer`, `@auth-specialist`, `@spec-steward`
- Primary modules: `papiflyfx-docking-settings`, `papiflyfx-docking-settings-api`
- Status: proposed

## Context

This plan captures a targeted refactor of the settings runtime after revisiting the current implementation in
`papiflyfx-docking-settings` and `papiflyfx-docking-settings-api`.

The current module is functional, but several behaviors are inconsistent or overly coupled:

1. The scope selector in the UI suggests dynamic scope switching, while multiple categories hard-code reads and writes to fixed scopes.
2. Dirty-state tracking is polled on a timer instead of being driven by state changes.
3. Persistence and secret storage are serviceable but need stronger failure handling and clearer boundaries.
4. Definition-driven settings metadata exists in the API, but many categories bypass it with handwritten forms and duplicate logic.
5. Tests cover the happy path well enough, but they do not yet lock down the riskiest regression areas.

## Goals

1. Make scope behavior explicit, predictable, and definition-driven.
2. Reduce category duplication by leaning on shared setting metadata and controls.
3. Improve persistence and secret-storage safety without widening module coupling.
4. Replace polling-based UI state refresh with deterministic state propagation.
5. Add regression coverage around the highest-risk workflows before deeper structural changes.

## Non-goals

1. No redesign of the public docking contracts outside what the settings API strictly requires.
2. No new settings categories.
3. No visual redesign beyond what is needed to align the settings UI with shared UI token usage.
4. No migration to external infrastructure or cloud-backed settings storage in this phase.

## Findings Summary

### 1. Scope model is internally inconsistent

- `SettingsContext` exposes `activeScope`, but many categories ignore it and directly read/write `SettingScope.APPLICATION` or `SettingScope.WORKSPACE`.
- `SettingDefinition.scope()` exists, but the UI currently does not consistently use it to decide persistence targets.
- The toolbar allows selecting `APPLICATION`, `WORKSPACE`, and `SESSION`, yet category behavior does not always reflect that choice.

### 2. Form infrastructure is underused

- The settings API already provides `SettingDefinition`, `SettingsValidator`, `ValidationResult`, and typed controls.
- Categories such as appearance, network, AI models, keyboard shortcuts, and MCP servers still build most of their forms manually.
- This duplicates validation, dirty tracking, labeling, and scope decisions.

### 3. UI state management is polling-based

- `SettingsPanel` refreshes toolbar state on a repeating timer.
- This introduces avoidable churn and makes correctness depend on polling instead of the actual control/category lifecycle.

### 4. Persistence and secret backends need hardening

- JSON settings are written directly to the destination file without an atomic swap strategy.
- Corrupted settings files currently escalate to failure instead of offering recovery behavior.
- The encrypted-file secret store uses a machine-derived seed that is convenient but weak and brittle.

### 5. A few category flows are structurally fragile

- MCP server editing does not clearly separate rename vs update behavior.
- Profiles import/export is tightly coupled to `JsonSettingsStorage`.
- Some categories save immediately without a shared validation gate or transactional apply flow.

## Target Architecture

## 1. Make definitions the source of truth

Refactor settings categories so that:

- `SettingDefinition.scope()` determines where a setting is stored unless a category explicitly documents custom persistence.
- categories describe settings declaratively whenever possible;
- shared controls own validation and dirty notifications;
- custom categories are reserved for genuinely composite workflows such as secret management, profile import/export, and MCP server collections.

## 2. Introduce explicit category state contracts

Refactor `SettingsCategoryUI` implementations toward an internal model with:

- `dirty` as an observable/property-backed state;
- `valid` as an observable/property-backed state;
- `apply` only enabled when the current category is both dirty and valid;
- `reset` restoring the currently selected scope view without depending on timer refreshes.

This may be implemented without changing the public SPI in the first pass, but if the current contract becomes too limiting,
any API update must be isolated, justified, and reviewed before implementation.

## 3. Separate storage concerns more cleanly

Keep the current runtime model, but tighten responsibilities:

- `SettingsRuntime` remains the composition root;
- storage implementations own persistence, migration, and corruption handling;
- secret-store implementations own backend-specific behavior and fallback;
- UI categories stop making assumptions about concrete storage implementations where avoidable.

## Workstreams

### Workstream A — Scope and settings-definition consistency

#### Objective

Unify reads/writes so UI scope selection, setting metadata, and actual persistence targets agree.

#### Tasks

1. Audit each built-in category and classify it as:
   - definition-driven;
   - custom multi-record;
   - secret-oriented.
2. For definition-driven categories, remove hard-coded scope usage and derive scope from `SettingDefinition.scope()`.
3. Decide and document the toolbar behavior:
   - either it controls the active scope for compatible categories;
   - or categories expose fixed scopes and the toolbar is narrowed to only applicable contexts.
4. Ensure `SESSION` settings are only offered where they are meaningfully supported.
5. Add regression tests for scope switching and persistence per category type.

#### Acceptance criteria

- No built-in category silently ignores the active scope when its definitions declare scope-aware behavior.
- Scope behavior is documented in the settings spec and module README if user-facing behavior changes.

### Workstream B — Shared form and validation model

#### Objective

Reduce manual form code and standardize validation, dirty-state tracking, and apply/reset behavior.

#### Tasks

1. Introduce a shared form binder or presenter layer around existing `SettingControl` implementations.
2. Use `SettingControlFactory` for simple categories instead of hand-building duplicated controls.
3. Centralize category-level validation aggregation so the toolbar can reflect current validity.
4. Standardize dirty tracking on change listeners/property bindings instead of ad hoc booleans where practical.
5. Keep handwritten UI only for:
   - secrets management;
   - MCP server collections;
   - profile import/export;
   - other multi-entity editors that do not map cleanly to flat definitions.

#### Acceptance criteria

- Validation errors surface consistently.
- Apply/reset button enablement is deterministic.
- At least the straightforward categories rely primarily on shared controls instead of custom field plumbing.

### Workstream C — UI state and styling cleanup

#### Objective

Make the settings shell align better with shared UI standards and eliminate timer-driven refresh.

#### Tasks

1. Remove the periodic toolbar refresh loop in favor of event-driven updates.
2. Move inline styling toward shared CSS classes and `-pf-ui-*` token usage where the shared API already provides equivalents.
3. Review category list, search bar, toolbar, and validation message styling for consistency.
4. Ensure theme changes still propagate immediately without reconstructing unnecessary UI nodes.

#### Acceptance criteria

- No polling timer is needed for ordinary dirty/valid/status updates.
- Visual styling relies less on inline strings and more on shared theme/token patterns.

### Workstream D — Persistence and secret-storage hardening

#### Objective

Improve resilience and security posture while preserving the module’s current runtime role.

#### Tasks

1. Add atomic save behavior for JSON settings files.
2. Define how corrupted settings files are handled:
   - fail fast with a clear message;
   - or quarantine/backup and continue with defaults.
3. Expand migration-path tests to include malformed version data and missing migrators.
4. Revisit encrypted-file secret key derivation and document the intended threat model.
5. If secret backend behavior changes, require `@auth-specialist` review before merge.

#### Acceptance criteria

- Settings file writes avoid partial-write corruption.
- Secret-storage behavior is explicitly documented, including fallback semantics and limitations.

### Workstream E — Category-specific fixes

#### Objective

Close known correctness gaps before or during broader refactoring.

#### Tasks

1. Fix MCP server rename/update semantics so editing an existing server does not leave stale entries behind.
2. Verify delete flow clears associated secrets for removed or renamed MCP server identities.
3. Decouple profiles import/export from `JsonSettingsStorage` where feasible, or clearly document the limitation.
4. Review AI-model settings and network settings for missing validation and scope consistency.

#### Acceptance criteria

- MCP server CRUD behavior is covered by regression tests.
- Category-specific edge cases are either fixed or explicitly documented as deferred.

## Proposed Sequence

### Phase 1 — Safety first

1. Add tests for scope behavior, MCP CRUD flows, invalid numeric input, and persistence failure handling.
2. Fix category-level correctness issues that would make later refactors risky.

### Phase 2 — State model cleanup

1. Remove timer-based refresh.
2. Introduce event-driven dirty/valid status propagation.
3. Stabilize toolbar behavior around the new model.

### Phase 3 — Definition-driven refactor

1. Refactor straightforward categories onto shared controls/binders.
2. Normalize scope handling around `SettingDefinition.scope()`.

### Phase 4 — Persistence and security hardening

1. Add atomic JSON writes and recovery behavior.
2. Improve secret-store documentation and internals as approved.

### Phase 5 — UI standards alignment

1. Replace inline styles with shared tokens/classes where practical.
2. Perform a final accessibility and consistency review.

## Validation Strategy

### Automated

- Focused module tests:
  - `./mvnw -pl papiflyfx-docking-settings -am test`
- Additional targeted tests for:
  - scope switching behavior;
  - category dirty/valid transitions;
  - MCP server rename/delete flows;
  - JSON migration and corruption handling;
  - secret-store fallback behavior.

### Manual

1. Open the settings panel from samples and verify category switching, search, and scope behavior.
2. Change appearance settings and confirm theme updates remain immediate.
3. Edit MCP server entries, including rename and delete operations.
4. Exercise profile import/export with valid and invalid payloads.
5. Confirm headless UI tests remain deterministic after event-driven refresh changes.

## Risks and Mitigations

### Risk: API drift in `papiflyfx-docking-settings-api`

Mitigation:

- Keep the first pass internal to the runtime module where possible.
- Any SPI change must be reviewed for compatibility and accompanied by updated documentation.

### Risk: Secret-storage changes can affect login/runtime integrations

Mitigation:

- Keep backend changes isolated behind the existing `SecretStore` contract unless a broader change is approved.
- Validate downstream usage in login-related consumers before closing the workstream.

### Risk: UI cleanup introduces regressions in theme responsiveness

Mitigation:

- Add or extend TestFX coverage before removing the existing refresh timer.

## Deliverables

1. Refactored settings module implementation with clarified scope behavior.
2. Stronger test coverage for the riskiest settings workflows.
3. Reduced manual form duplication in built-in categories.
4. Hardened persistence and documented secret backend behavior.
5. Updated spec/progress/validation notes for completed phases.

## Definition of Done

- The chosen scope model is documented and consistently implemented.
- Timer-based toolbar refresh is removed.
- High-risk category workflows have regression tests.
- Persistence writes are more resilient than the current direct-write behavior.
- Review gates are satisfied for QA, UI, security, and spec updates.
