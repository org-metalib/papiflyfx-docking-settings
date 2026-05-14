# PapiflyFX Settings Refactor Plan

- Date: 2026-04-10
- Review batch: `review0`
- Authoring agent: `@core-architect`
- Proposed implementation lead: `@ops-engineer`
- Required reviewers: `@ui-ux-designer`, `@auth-specialist`, `@spec-steward`, `@qa-engineer`
- Primary modules: `papiflyfx-docking-settings`, `papiflyfx-docking-settings-api`
- Related modules: `papiflyfx-docking-login`, `papiflyfx-docking-github`, `papiflyfx-docking-samples`
- Status: proposed

## Context

This plan captures the Codex review of the current settings stack and turns the review findings into a focused refactor initiative.

The current implementation is functional and the module test suite is green, but the architecture is not internally consistent in several areas:

1. runtime ownership is split between host-owned and hidden default bootstraps;
2. the scope selector advertises behavior that many categories do not actually honor;
3. secret administration and secret fallback storage are weaker than the intended security model;
4. the settings shell bypasses the shared UI token vocabulary and still relies on inline styling;
5. the spec and progress artifacts overstate completion and do not match the current behavioral gaps.

## Problem Statement

The settings stack currently mixes three different responsibilities too loosely:

1. composition and runtime ownership;
2. user-facing scope semantics and form behavior;
3. secret administration and storage hardening.

That leads to hidden coupling between settings, login, and docking restore flows, and it weakens confidence in both the UI contract and the security contract.

## Goals

1. Establish a single host-owned settings runtime model across settings UI, docking restore, and login integration.
2. Make scope behavior explicit and truthful for every category.
3. Harden secret handling so the UI never re-exposes raw stored values and the fallback backend has a defensible threat model.
4. Align the settings shell and controls with the shared `-pf-ui-*` token vocabulary and remove timer-driven state refresh.
5. Bring spec, progress, and validation artifacts back in sync with implementation reality.

## Non-goals

1. No new settings categories in this review batch.
2. No broad redesign of docking APIs outside narrow seams required by settings runtime ownership.
3. No external/cloud-backed settings storage.
4. No cosmetic redesign beyond UI standards alignment and accessibility/polish fixes needed by the refactor.

## Key Invariants

1. One application instance should own one logical `SettingsRuntime`.
2. Session restore must not silently create a second runtime with different storage or secret-store state.
3. The UI must never claim scope behavior that the active category does not support.
4. Secret administration must operate on aliases and lifecycle actions, not on redisplaying stored secret values.
5. Any `settings-api` change must remain narrowly scoped and reviewed before implementation.

## Findings Summary

### 1. Runtime ownership is inconsistent

- `SettingsStateAdapter` has a no-arg path that creates a fresh default runtime.
- `DefaultSettingsServicesProvider` also creates its own default runtime.
- Login consumes `SettingsServicesProvider` through `ServiceLoader`, which means login can bind to a different runtime than the settings panel if the host app configured custom paths or factories.

### 2. Scope selection is not an honest contract

- The toolbar exposes `APPLICATION`, `WORKSPACE`, and `SESSION`.
- `SettingsPanel` rebuilds category content for scope changes.
- Many categories still read and write hard-coded scopes instead of honoring either `activeScope` or their own `SettingDefinition.scope()`.

### 3. Secret handling needs redesign

- `SecurityCategory` reloads stored secret values back into the UI.
- OS-backed secret stores maintain plaintext alias/index sidecars.
- The encrypted-file fallback derives keys from machine and account metadata, which is convenient but not strong enough to call a secure default.

### 4. UI standards are not applied consistently

- The settings shell and control layer still use inline styles and hard-coded colors.
- Dirty-state refresh is timer-driven instead of event-driven.
- Validation infrastructure exists but is not consistently aggregated into one category-level apply gate.

### 5. Spec and delivery artifacts drift from reality

- `spec/papiflyfx-docking-settings/README.md` is still prompt-like.
- `progress.md` marks the initiative complete.
- There is no dedicated validation artifact that records current limitations and residual risks.

## Target Architecture

## 1. Single runtime ownership model

The host application should explicitly own settings runtime construction and pass that runtime into all settings integration points.

Preferred direction:

1. keep `SettingsRuntime` as the composition root;
2. require explicit runtime injection for `SettingsContentFactory` and `SettingsStateAdapter`;
3. replace hidden default-runtime creation with either:
   - explicit host wiring; or
   - a host-initialized runtime bridge that fails closed when not initialized.

The system should not silently create a second runtime just because a `ServiceLoader` path or restore path was used.

## 2. Explicit scope policy per category

Every category must fall into one of these modes:

1. fixed-scope category;
2. definition-driven scoped category;
3. custom multi-record category with an explicit scope policy.

The toolbar should reflect only the scopes that are valid for the active category. If the current API cannot express that clearly, introduce the smallest viable API addition and review it before implementation.

## 3. Secret admin separated from secret value display

Secret management should expose:

1. alias inventory, when supported;
2. backend diagnostics and fallback warnings;
3. rotate/replace/clear/delete actions;
4. optional metadata such as last-updated state if it can be tracked safely.

It should not load previously stored raw values back into revealable controls.

## Workstreams

### Workstream A - Runtime Ownership Consolidation

#### Objective

Remove hidden default bootstraps and make runtime ownership consistent across settings, docking restore, and login.

#### Tasks

1. Audit every code path that creates or discovers a `SettingsRuntime`.
2. Refactor `SettingsStateAdapter` away from implicit default runtime creation.
3. Refactor `DefaultSettingsServicesProvider` so it does not manufacture an unrelated runtime.
4. Verify login bootstrap paths reuse the same storage and secret store as the settings panel.
5. Add regression tests covering host-injected runtime reuse across:
   - settings panel creation;
   - settings dock restore;
   - login broker bootstrap.

#### Acceptance Criteria

1. No restore or `ServiceLoader` path silently creates a second runtime.
2. Sample/runtime wiring and restored settings content observe the same underlying settings and secrets state.

### Workstream B - Scope Semantics Cleanup

#### Objective

Make selected scope, declared scope, and actual persistence target agree.

#### Tasks

1. Classify all built-in and contributing categories as fixed-scope, scoped-definition, or custom-scope.
2. For simple categories, derive reads and writes from `SettingDefinition.scope()`.
3. For custom categories, define explicit scope policy and reflect it in the UI.
4. Narrow or disable toolbar scope options when the active category cannot support them.
5. Add focused tests for:
   - scope switching;
   - scope-aware reset/apply behavior;
   - categories that intentionally remain fixed-scope.

#### Acceptance Criteria

1. No category silently ignores the chosen scope.
2. The toolbar only presents scopes that are valid for the active category.

### Workstream C - Secret Handling Hardening

#### Objective

Bring secret admin flows and fallback storage behavior up to the intended security model.

#### Tasks

1. Redesign `SecurityCategory` around aliases and lifecycle actions instead of value redisplay.
2. Remove raw-value reload from the existing secret-management flow.
3. Replace plaintext alias/index sidecars with a safer metadata strategy, or explicitly constrain/disable inventory when a backend cannot support it safely.
4. Revisit encrypted-file fallback key derivation and document the supported threat model.
5. Add negative-path tests for:
   - fallback backend activation;
   - alias deletion/rotation;
   - malformed encrypted payloads;
   - unavailable OS secret backends.

#### Acceptance Criteria

1. Stored secret values are never rehydrated into revealable controls for browsing.
2. Secret backend behavior and limitations are explicitly documented and covered by tests.

### Workstream D - UI and Validation Alignment

#### Objective

Remove polling, centralize validation behavior, and align the settings shell with shared UI standards.

#### Tasks

1. Replace timer-driven dirty-state refresh with event-driven updates.
2. Aggregate category validity so apply/reset enablement is deterministic.
3. Move inline styles to shared CSS classes and `-pf-ui-*` tokens where available.
4. Review the shell, toolbar, list, search bar, and shared controls for spacing, focus states, and theme propagation.
5. Use `SettingControlFactory` more consistently for straightforward categories.

#### Acceptance Criteria

1. No polling timer is required for normal dirty/valid/status updates.
2. The settings shell uses shared tokens and behaves consistently across theme changes.

### Workstream E - Spec and Validation Repair

#### Objective

Restore trust in the settings planning and delivery artifacts.

#### Tasks

1. Replace the prompt-style settings spec README with a real initiative summary.
2. Update `plan.md` and `progress.md` to match the current implementation and remaining gaps.
3. Add `validation.md` for this review batch with commands run, manual checks, and residual risks.
4. Record ownership-sensitive decisions:
   - runtime ownership seam;
   - scope policy model;
   - secret inventory policy.

#### Acceptance Criteria

1. Spec artifacts describe the codebase as it is, not as originally intended.
2. The review batch leaves a clear trail for the next implementation pass.

## Proposed Sequence

### Phase 1 - Runtime and scope safety

1. Lock down runtime ownership with tests.
2. Fix misleading scope behavior before deeper UI refactors.

### Phase 2 - Secret admin and storage hardening

1. Redesign the security page.
2. Strengthen backend fallback and metadata handling.

### Phase 3 - UI state and validation cleanup

1. Remove timer polling.
2. Introduce event-driven dirty/valid propagation.
3. Align styling with shared UI tokens.

### Phase 4 - Spec closure

1. Update plan/progress docs.
2. Add validation notes and unresolved risks.

## Validation Strategy

### Automated

- `./mvnw -pl papiflyfx-docking-settings -am test -Dtestfx.headless=true`
- Add targeted tests for:
  - runtime reuse across settings/login/restore;
  - scope switching semantics;
  - security-page alias operations;
  - fallback secret-store failure paths;
  - category-level validation gating.

### Manual

1. Open the settings panel from samples and verify scope options change correctly per category.
2. Restore a saved layout containing the settings panel and confirm it binds to the same runtime as the live app shell.
3. Exercise security-page create/replace/delete flows without ever revealing existing secret values.
4. Verify theme changes, focus states, and status messaging after inline styles are removed.

## Risks and Mitigations

### Risk: Narrow API changes still ripple into contributing modules

Mitigation:

1. keep API changes minimal;
2. prefer implementation-side policy objects first;
3. require explicit reviewer sign-off before widening `settings-api`.

### Risk: Secret inventory becomes less convenient when hardened

Mitigation:

1. favor safe alias lifecycle over convenient raw browsing;
2. document backend limitations clearly;
3. provide explicit replace/delete flows so usability does not depend on redisplay.

### Risk: Runtime bootstrap refactor breaks login/sample integration

Mitigation:

1. add integration-style tests before refactoring;
2. validate sample runtime wiring manually after changes.

## Definition of Done

This review batch is done when:

1. hidden default-runtime creation is removed or replaced by an explicit host-initialized seam;
2. scope behavior is truthful for every category;
3. secret admin flows no longer re-expose stored values;
4. settings shell state updates are event-driven;
5. spec and validation artifacts are current and reviewable.
