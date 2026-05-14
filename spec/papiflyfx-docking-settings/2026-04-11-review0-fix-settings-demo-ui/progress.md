# Progress — Fix Settings Demo UI Regression

**Priority:** P1
**Lead Agent:** @ops-engineer
**Status:** PR #10 Review Remediation Complete (incl. multi-reviewer follow-up 2026-04-13)

## PR #10 Review Remediation

**Date:** 2026-04-13

### Accepted Items
- **Item 1 (P1):** `EncryptedFileSecretStore` now distinguishes malformed/corrupted envelopes from decrypt/authentication failures. Malformed primary content can still recover from `.bak`, but unrecoverable decrypt/tamper failures now throw `IllegalStateException` instead of resetting to an empty store. Regression tests now cover malformed fallback, tampered-envelope failure, and the "later write must not erase secrets after failed decrypt" path.
- **Item 2 (P2):** `AtomicFileWriter` now catches `AtomicMoveNotSupportedException`, retries with `REPLACE_EXISTING`, and guarantees `*.tmp` cleanup on failure paths. `AtomicFileWriterTest` now exercises the fallback move path and temp-file cleanup after a forced rename failure.
- **Item 3 (P2):** `AppearanceCategory` now restores mode-aware light/dark defaults for `appearance.background` and `appearance.border` when those keys are unset, both after `binder.load(context)` and during `buildTheme()`. `SettingsPanelFxTest` now proves light-mode defaults survive load, reset, and apply.
- **Item 4 (P2):** `SettingsPanel` now suppresses scope-change re-entrancy while `showCategory(...)` normalizes supported scopes, clears cached panes only after normalization, and keeps dirty-property binding one-in/one-out. `SettingsPanelFxTest` now covers category switches with incompatible scope sets and asserts dirty-listener ownership stays stable.
- **Item 5 (P2):** `SettingsPanel.buildPalette(...)` now maps `surfaceControl` to `UiCommonThemeSupport.background(resolved)` instead of the border token. `SettingsPanelFxTest` now verifies the shared search-field surface resolves to a real surface color, not the border color.
- **Item 6 (P3):** `SettingsCategoryList` no longer applies the generic `pf-settings-list` chrome path. The regression suite now asserts the category list keeps only its dedicated `pf-settings-category-list` styling path.
- **Item 7 (P3):** Fixed the broken samples-doc reference inside this task pack by redirecting the stale `papiflyfx-docking-samples/README.md` references to `spec/papiflyfx-docking-samples/README.md`.

### Reviewer Handoff Status
- `@auth-specialist`: ready to review the fail-closed secret-store behavior and the undecryptable/tampered regression coverage in `EncryptedFileSecretStoreTest`.
- `@qa-engineer`: ready to review the new regression coverage in `EncryptedFileSecretStoreTest`, `AtomicFileWriterTest`, and `SettingsPanelFxTest`, plus the green targeted and module-wide headless runs.
- `@ui-ux-designer`: ready to review the `surfaceControl` palette correction and the category-list selector isolation in `SettingsPanel` / `SettingsCategoryList`.

## Phase Tracking

### Phase 1: SettingsPanel Token Injection
- **Status:** Complete
- **Target files:** `SettingsPanel.java`
- **Summary:** Added `applyThemeTokens(Theme)` and `buildPalette(Theme)` methods. The constructor now calls `applyThemeTokens()` after loading stylesheets, and the `themeListener` triggers `applyThemeTokens(newTheme)` on every theme change. The palette is built from `UiCommonThemeSupport` helper methods (same pattern as `TreeSearchOverlay` and `GitHubToolbar`).

### Phase 2: SettingsToolbar and SettingsSearchBar Alignment
- **Status:** Complete
- **Target files:** `SettingsToolbar.java`, `SettingsSearchBar.java`
- **Summary:** Replaced ad-hoc `Button` instances with `UiPillButton`. Wrapped dirty/status labels in `UiStatusSlot`. Added `pf-ui-compact-field` style class to `SettingsSearchBar` search field. Action buttons in `setActions()` also use `UiPillButton`.

### Phase 3: SamplesApp Theme-Awareness
- **Status:** Complete
- **Target files:** `SamplesApp.java`
- **Summary:** Removed static `NAVIGATION_THEME` constant (19 hardcoded dark colors). Added `applyTheme(Theme)` method that derives all colors from `UiCommonThemeSupport` and applies them to top bar, content area, placeholder label, buttons, and navigation tree. Navigation tree theme now uses `TreeViewThemeMapper.map(theme)`. Category cell renderer uses `context.theme().background()` and `context.theme().connectingLineColor()` instead of hardcoded hex values. Theme change listener updates all surfaces on toggle.

## Validation Log

| Check | Result | Date |
|-------|--------|------|
| Compile (settings + samples) | PASS — 14/14 modules | 2026-04-12 |
| Headless tests (settings) | PASS — 17/17 | 2026-04-12 |
| Headless tests (samples) | PASS — 12/12 | 2026-04-12 |
| Full build | Not run (media fork issue pre-existing) | — |
| Manual visual verification | Pending reviewer | — |
| Session restore round-trip | Pending reviewer | — |

## Multi-Reviewer Follow-up (2026-04-13)

### Finding 1 (P1) — Category-list still carried generic `pf-settings-list` class: FIXED
- `SettingsCategoryList` constructor was passing both `"pf-settings-category-list"` and `"pf-settings-list"` to `SettingsUiStyles.apply(...)`. Removed `"pf-settings-list"` so the control is styled only by its dedicated selector.
- `SettingsPanelFxTest.categoryListUsesDedicatedSelectorOnly()` now passes (was failing before the fix).

### Finding 2 (P1) — Stale validation record: FIXED
- Reran all three validation commands after the code fix. Updated `validation.md` with actual 2026-04-13 rerun results.

### Validation

| Check | Result | Date |
|-------|--------|------|
| `./mvnw -pl papiflyfx-docking-settings -am compile` | PASS — 5/5 modules | 2026-04-13 |
| `./mvnw -pl papiflyfx-docking-settings -am -Dtest=EncryptedFileSecretStoreTest,AtomicFileWriterTest,SettingsPanelFxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` | PASS — 21/21 | 2026-04-13 |
| `./mvnw -pl papiflyfx-docking-settings -am -Dtestfx.headless=true test` | PASS — 28/28 | 2026-04-13 |

## Notes

- The `papiflyfx-docking-media` module has a pre-existing Surefire fork startup error unrelated to this task. Running tests on the target modules individually confirms no regressions.
- No new `-pf-ui-*` tokens were introduced; only the existing vocabulary is consumed.
- Settings runtime, persistence, and session restore contracts are untouched.

## @ui-ux-designer Review Follow-up

**Date:** 2026-04-12

### Finding 1 (Medium) — Accent button text contrast: FIXED
- In `SamplesApp.applyTheme(...)`, replaced `textPrimaryCss` with a contrast-on-accent color computed via `UiCommonThemeSupport.isDark(accent) ? "white" : "black"` for `loginDemoButton` and `themeToggle`.
- This ensures WCAG AA contrast (white text on dark accent, black text on light accent).

### Finding 2 (Low) — Toolbar buttons lack hover/focus states: ACCEPTED
- Accepted as a known demo-app limitation. Inline `setStyle(...)` overrides CSS pseudo-class rules. This is not a regression — the prior hardcoded buttons had the same behavior.

### Finding 3 (Low) — Fully-qualified `Color.BLACK` in `buildPalette`: FIXED
- Added `import javafx.scene.paint.Color;` to `SettingsPanel.java` and replaced `javafx.scene.paint.Color.BLACK` with `Color.BLACK`.

### Validation

| Check | Result | Date |
|-------|--------|------|
| Compile (settings + samples) | PASS | 2026-04-12 |
| Headless tests (settings) | PASS — 17/17 | 2026-04-12 |
| Headless tests (samples) | PASS | 2026-04-12 |

## Dark-Mode Follow-up

**Date:** 2026-04-12

### Finding 1 (Medium) — Dark-mode text/password inputs still using default chrome: FIXED
- Extended `SettingsUiStyles` with a shared field/dropdown/textarea/checkbox/list/button styling surface and reused the existing `pf-ui-compact-field` / `pf-ui-field` family instead of adding per-control dark overrides.
- Applied shared field chrome to the remaining text/password editors in `AuthenticationCategory`, `SecurityCategory`, `GitHubCategory`, `EditorCategory`, `HugoCategory`, and `ProfilesCategory`, aligning them with the preference search field for background, border, prompt text, text fill, and focus-ring behavior.

### Finding 2 (Medium) — Dark-mode dropdowns still falling back to Modena rendering: FIXED
- Added token-driven combo-box rules in `settings.css` for the closed control, arrow-button region, text fill, prompt text, focus ring, and popup cells.
- Reused the shared field treatment by tagging selectors in `SettingsToolbar`, `EnumSettingControl`, and category-specific combo boxes such as `AuthenticationCategory` with the compact field path plus a shared settings combo-box class.

### Finding 3 (Medium) — Dark-mode textareas do not match shared settings field chrome: FIXED
- Added shared textarea styling in `settings.css` for both the outer `TextArea` chrome and its inner `.content` region using existing `-pf-ui-*` tokens.
- Wired multiline editors such as `ProfilesCategory` to the shared textarea style so the dark-theme background, border, text, prompt text, selection, and focus treatment now match the rest of the settings input family without changing sizing behavior.

### Finding 4 (Medium) — Dark-mode checkboxes still use raw Modena visuals: FIXED
- Added token-driven checkbox styling in `settings.css` covering label text, box chrome, hover, focus, selected, disabled, and checkmark states.
- Applied the shared checkbox class to `BooleanSettingControl` and the category-specific toggles in `AuthenticationCategory`, `GitHubCategory`, `EditorCategory`, `HugoCategory`, and `McpServersCategory`.

### Finding 5 (Medium) — Dark-mode listboxes/list views still have readability gaps: FIXED
- Added shared settings list styling in `settings.css` for list background, border, hover, selected, and selected-but-unfocused states using the existing token vocabulary.
- Applied the shared list treatment to `SettingsCategoryList`, `SecurityCategory`, `McpServersCategory`, and the token/session list in `AuthenticationCategory` so dark-mode list surfaces no longer fall back to raw Modena chrome.

### Finding 6 (Medium) — Dark-mode settings buttons still use default button chrome: FIXED
- Reused the shared compact action button primitives instead of adding a parallel settings-button implementation.
- Converted raw settings buttons in `ProfilesCategory`, `SecurityCategory`, `McpServersCategory`, `PathSettingControl`, `SecretSettingControl`, and supporting toolbar/category actions to the shared compact action treatment so default, hover, focused, pressed, and disabled states remain token-driven in both light and dark themes.

### Color Popup Follow-up — Dark-mode color picker popup still using default palette chrome: FIXED
- `ColorSettingControl` now applies the same shared compact field treatment to `ColorPicker` that the other settings selectors use, keeping the closed control aligned with the shared settings field family.
- `settings.css` now adds token-driven styling for the color picker label/arrow region plus the `.color-palette` popup surface, separator, link text, hover square, and color swatch border so the appearance-category color popup remains readable in dark mode without introducing hardcoded dark-only values.

### Regression Coverage
- `SettingsPanelFxTest.selectedCategoryUsesExplicitInactiveTokensWhenListLosesFocus()` remains in place to guard inactive dark-theme selection behavior for the category list.
- `SettingsPanelFxTest.settingsEditorsReuseSharedCompactFieldStyleClass()` continues to protect the generic settings editor path from dropping shared field chrome.
- `SettingsPanelFxTest.colorPickerPopupUsesTokenDrivenDarkThemeSurface()` opens the appearance color picker popup in dark mode and verifies the popup surface, border, and action link resolve to the expected token-driven theme colors.
- Added `SettingsDemoStyleIntegrationTest` in `papiflyfx-docking-samples` to load the actual demo settings panel in dark mode and audit the required categories for shared styling on text/password fields, combo boxes, text areas, checkboxes, list views, and buttons.
- `SettingsDemoStyleIntegrationTest` now also audits the appearance-category `ColorPicker` controls for the shared compact field styling path.

### Validation

| Check | Result | Date |
|-------|--------|------|
| `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-login,papiflyfx-docking-github,papiflyfx-docking-code,papiflyfx-docking-hugo,papiflyfx-docking-samples -am compile` | PASS | 2026-04-12 |
| `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SettingsDemoStyleIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` | PASS | 2026-04-12 |
| `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-login,papiflyfx-docking-github,papiflyfx-docking-code,papiflyfx-docking-hugo,papiflyfx-docking-samples -am -Dtestfx.headless=true test` | PASS | 2026-04-12 |
| `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am compile` | PASS | 2026-04-12 |
| `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am -Dtest=SettingsPanelFxTest,SettingsDemoStyleIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` | PASS | 2026-04-12 |
| `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am -Dtestfx.headless=true test` | FAIL — pre-existing `papiflyfx-docking-media` Surefire fork crash stops the reactor before downstream modules | 2026-04-12 |
