# PR Review Fix Suggestions — PR #10

- **Date:** 2026-04-13
- **Lead Agent:** `@spec-steward`
- **Primary implementation owner:** `@ops-engineer`
- **Required reviewers:** `@auth-specialist`, `@qa-engineer`, `@ui-ux-designer`
- **Source:** inline review comments on `https://github.com/org-metalib/papiflyfx-docking/pull/10`

## Summary

After deduplicating the PR review, there are **7 actionable items**:

1. one security/data-loss issue in encrypted secret recovery
2. one portability/cleanup issue in atomic file writes
3. one appearance reset regression
4. one `SettingsPanel` scope-change re-entrancy bug
5. one incorrect theme-token palette mapping
6. one category-list CSS class collision
7. one broken spec link

The two review-body overview comments from Codex/Copilot are summaries only and do not require changes.

## Recommended Actions

### 1. P1 — Accept: fail closed when encrypted secrets cannot be decrypted

- **Review source:** `EncryptedFileSecretStore.java`
- **Why this is valid:** `loadSecrets()` currently catches every failure, logs, and returns an empty map. If both `secrets.enc` and `secrets.enc.bak` are undecryptable, the next `setSecret(...)` or `clearSecret(...)` rewrites the store from empty state and silently destroys previously persisted secrets.
- **Suggested fix:**
  - Keep backup recovery for malformed/corrupted file content.
  - Do **not** convert unrecoverable decryption/authentication failures into an empty store.
  - If both primary and backup fail to decrypt, throw `IllegalStateException` with the original cause chain and leave the on-disk files untouched.
  - Log corruption-recovery attempts at `WARNING`, but treat unrecoverable crypto/auth failures as hard errors.
- **Suggested code shape:**
  - Split `loadSecrets()` into:
    - primary read
    - backup recovery attempt
    - final `throw` path for unrecoverable decrypt/auth failures
  - Distinguish "recoverable parse/corruption" from "cannot decrypt/authenticate existing secret store".
- **Validation:**
  - Replace `resetsToEmptyWhenBothFilesCorrupted()` with two tests:
    - both files malformed JSON/base64 -> empty fallback still allowed
    - both files valid envelope but undecryptable/tampered -> read path throws
  - Add a regression proving failed decrypt does not let a later write erase existing secret state silently.

### 2. P2 — Accept: add `ATOMIC_MOVE` fallback and guaranteed temp cleanup

- **Review source:** `AtomicFileWriter.java`
- **Deduped from:** Codex + Copilot comments on the same line range
- **Why this is valid:** `Files.move(..., ATOMIC_MOVE)` can fail on filesystems that do not support atomic rename. The current code aborts the write entirely and can leave `*.tmp` behind.
- **Suggested fix:**
  - Catch `AtomicMoveNotSupportedException`.
  - Retry the rename with `REPLACE_EXISTING` only.
  - Ensure `*.tmp` is deleted in failure paths.
  - Keep the backup step as-is.
- **Suggested code shape:**
  - Write temp file.
  - Try atomic move.
  - On `AtomicMoveNotSupportedException`, log one `WARNING` and retry with non-atomic replace.
  - In a `finally` or guarded cleanup block, delete leftover temp file if it still exists.
- **Validation:**
  - Extend `AtomicFileWriterTest` with coverage for temp-file cleanup on move failure.
  - If direct filesystem simulation is awkward, extract the move step behind a package-private helper so the fallback path can be exercised deterministically.

### 3. P2 — Accept: restore mode-aware appearance defaults on load/reset

- **Review source:** `AppearanceCategory.java`
- **Why this is valid:** `DefinitionFormBinder.load(...)` falls back to static definition defaults. `appearance.background` and `appearance.border` are hardcoded dark defaults, so a light-theme selection with missing explicit overrides reloads dark chrome values and can produce a light theme with dark surfaces after `apply()`.
- **Suggested fix:**
  - Reintroduce mode-aware fallback resolution for `appearance.background` and `appearance.border`.
  - Do not rely on static dark defaults for those two fields when the current theme mode is `LIGHT`.
- **Suggested code shape:**
  - Add helper methods that resolve background/border from:
    - explicit stored value if present
    - otherwise `Theme.light()` or `Theme.dark()` defaults based on the selected `ThemeMode`
  - Call that helper after `binder.load(context)` in both `buildSettingsPane(...)` and `reset(...)`.
  - Mirror the same fallback in `buildTheme()` so blank/unset values cannot rebuild a light theme with dark overrides.
- **Validation:**
  - Add a regression where storage contains `appearance.theme=light` but no explicit background/border, then `reset()` and `apply()` must produce light-mode background/border values.

### 4. P2 — Accept: prevent `SettingsPanel` scope-change re-entrancy during category refresh

- **Review source:** `SettingsPanel.java`
- **Why this is valid:** `showCategory(...)` calls `toolbar.setSupportedScopes(...)`. That method can change the selected scope, which fires the toolbar scope listener and re-enters `onScopeChanged()` before the original `showCategory(...)` call finishes. The current sequence can clear the pane cache mid-render and bind the same dirty property listener twice.
- **Suggested fix:**
  - Suppress scope-change callbacks while the panel is normalizing supported scopes for the newly selected category.
  - Rebuild the pane at most once after scope normalization completes.
- **Suggested code shape:**
  - Add a panel-local guard such as `suppressScopeRefresh`.
  - In the toolbar scope listener, return early while suppression is active.
  - In `showCategory(...)`, capture the old scope, update supported scopes under the guard, then rebuild once if the effective scope changed.
  - Keep `unbindDirtyProperty()` and `bindDirtyProperty(...)` strictly one-in/one-out.
- **Validation:**
  - Add a focused `SettingsPanelFxTest` with categories that expose different scope sets and observable dirty properties.
  - Assert that switching categories does not attach duplicate dirty listeners or trigger duplicate toolbar refreshes.

### 5. P2 — Accept: map `surfaceControl` to a real surface color, not a border color

- **Review source:** `SettingsPanel.java`
- **Why this is valid:** `UiCommonPalette` expects the 6th constructor argument to be `surfaceControl`, but `SettingsPanel.buildPalette(...)` currently passes `UiCommonThemeSupport.border(resolved)`. That makes `-pf-ui-surface-control` and `-pf-ui-surface-panel-subtle` border-colored fills.
- **Suggested fix:**
  - Replace the `surfaceControl` argument with an actual surface fill.
  - Recommended mapping: use `UiCommonThemeSupport.background(resolved)` for `surfaceControl` so controls remain inset against the panel shell.
  - Keep `UiCommonThemeSupport.border(resolved)` confined to `borderDefault`.
- **Validation:**
  - Extend `SettingsPanelFxTest` to assert that a shared field/list/combo background resolves to the expected surface token, not the border token.

### 6. P3 — Accept: remove generic list chrome from `SettingsCategoryList`

- **Review source:** `SettingsCategoryList.java`
- **Why this is valid:** `SettingsCategoryList` applies both `pf-settings-category-list` and the generic `pf-settings-list` class. In `settings.css`, the generic list rules come later with matching specificity, so they can override category-list-specific border, padding, and radius decisions.
- **Suggested fix:**
  - Remove `SettingsUiStyles.applyList(this)` from `SettingsCategoryList`.
  - Keep only the dedicated `pf-settings-category-list` styling path there.
  - Reuse `pf-settings-list` only for actual content/list-editor controls.
- **Validation:**
  - Add a small regression in `SettingsPanelFxTest` that the category list does not carry `pf-settings-list`, or assert the category list border/padding matches the dedicated selector.

## Suggested Execution Order

1. Fix `EncryptedFileSecretStore` and its tests first. This is the only review item with real data-loss risk.
2. Fix `AtomicFileWriter` next, because both settings JSON and encrypted secrets depend on it.
3. Fix `SettingsPanel` re-entrancy and palette mapping together, then cover them in `SettingsPanelFxTest`.
4. Fix `AppearanceCategory` mode-aware reset behavior and add the regression.
5. Remove the category-list generic class collision.
6. Fix the broken doc link last.

## Done Criteria For This Review Pass

- No secret-store path rewrites encrypted secrets from an unrecoverable decrypt failure into an empty store.
- Atomic writes succeed on filesystems without `ATOMIC_MOVE` support and do not leave orphaned `*.tmp` files behind.
- Appearance reset/apply preserves light-mode defaults when background/border are unset.
- Scope changes no longer re-enter `showCategory(...)` and duplicate dirty listeners.
- Settings token mapping uses a surface color for `surfaceControl`.
- Category list styling is controlled only by its dedicated selector.
