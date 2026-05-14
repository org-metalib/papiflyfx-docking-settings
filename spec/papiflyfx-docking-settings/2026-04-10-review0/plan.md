# Implementation Plan: PapiflyFX Settings Refactor (review0)

- **Lead Agent:** `@ops-engineer`
- **Reference:** [refactor-plan-gemini-total.md](/refactor-plan-gemini-total.md)
- **Status:** completed

## Phase 1: Runtime & Scope Safety
- [x] B.1: Refactor `SettingsStateAdapter` to remove hidden default runtime creation.
- [x] B.2: Refactor `DefaultSettingsServicesProvider` to remove hidden default runtime creation.
- [x] B.3: Verify login and docking restore paths inject and reuse the exact same runtime instance.
- [x] C.1: Classify all categories by scope policy.
- [x] C.2: Remove hard-coded scope usage in favor of `SettingDefinition.scope()`.
- [x] C.3: Ensure the UI toolbar only presents valid scope options for the active category.

## Phase 2: Composable UI Refactor
- [x] A.1: Introduce a shared form binder around existing `SettingControl` implementations.
- [x] A.2: Refactor built-in categories (Appearance, Network, AI Models, Workspace) to use `SettingControlFactory` and declarative definitions.
- [x] A.3: Centralize validation aggregation and replace ad-hoc dirty tracking with observable/property-backed state.

## Phase 3: Security & Storage Hardening
- [x] D.1: Redesign `SecurityCategory` to never reload stored secret values.
- [x] D.2: Implement lifecycle actions (rotate/delete) for secrets.
- [x] D.3: Implement atomic save behavior for JSON settings (atomic swap).
- [x] D.4: Define and document corruption recovery strategy.
- [x] D.5: Revisit encrypted-file fallback threat model and document constraints.

## Phase 4: State & Styling Cleanup
- [x] E.1: Remove periodic timer loops for UI refresh (event-driven dirty/valid state).
- [x] E.2: Replace inline styles with `-pf-ui-*` tokens and shared CSS classes.

## Phase 5: Documentation Sync
- [x] F.1: Overhaul root `README.md`, module `README.md`, and existing `plan.md`/`progress.md` (if any) to reflect the new reality.
- [x] F.2: Maintain `validation.md` logging residual risks.
