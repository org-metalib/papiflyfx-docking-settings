# @qa-engineer Validation — Fix Settings Demo UI Regression

**Reviewer:** @qa-engineer
**Status:** Approve with findings
**Date:** 2026-04-12

## Automated Check Results

| Check | Command | Result |
|-------|---------|--------|
| Settings compile + test (headless) | `./mvnw -pl papiflyfx-docking-settings -am -Dtestfx.headless=true test` | PASS — 17/17 tests, 0 failures |
| Samples compile + test (headless) | `./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test` | PASS — 12/12 tests, 0 failures |

Monocle/TestFX initialization noise (stack traces for `BufferOverflowException`, `IllegalArgumentException: Too small int buffer size`) appears in stderr but is pre-existing headless infrastructure noise — no test failures result.

## 1. Missing Regression Coverage

### 1a. No theme-toggle test for SettingsPanel (Medium)

`SettingsPanelFxTest` constructs the panel with `Theme.dark()` only and never toggles to `Theme.light()`. The core value proposition of this change — that `applyThemeTokens()` fires on theme change and the panel re-styles — is **not exercised by any automated test**.

The `SamplesSmokeTest` has a theme-toggle test but only for `LoginSample` (`loginSampleThemeToggleDoesNotThrow`). There is no equivalent for the settings panel or for the `SamplesApp` shell itself.

**Recommendation:** Add a test to `SettingsPanelFxTest` that:
1. Constructs the panel with `Theme.dark()`.
2. Toggles `runtime.themeProperty().set(Theme.light())`.
3. Asserts no exception and optionally checks that the panel's inline style contains a light-mode token (e.g., a background color that differs from the dark-mode value).

### 1b. No `SamplesApp` theme-toggle smoke test (Low)

`SamplesSmokeTest.allSamplesLoadWithoutException()` loads every catalog sample but only under `Theme.dark()`. The `SamplesApp.applyTheme()` method — including the new contrast-on-accent logic — is only reachable through the `SamplesApp.start()` path, which is not tested headlessly. The smoke test exercises individual `SampleScene.build()` calls, not the `SamplesApp` shell.

**Recommendation:** Consider adding a test that instantiates `SamplesApp` components (top bar + content area) and toggles the theme property, verifying no exception. This is lower priority since `SamplesApp` is a demo shell, not a framework component.

### 1c. `allSamplesLoadWithoutException` only tests dark mode (Low)

The existing smoke test iterates all samples with `Theme.dark()`. No sample is built under `Theme.light()`. This means any theme-specific rendering issue in light mode (the mode this fix primarily targets) has no automated coverage.

**Recommendation:** Add a second pass through `SampleCatalog.all()` using `Theme.light()` to the existing test, or add a parameterized variant.

## 2. Narrowest Relevant Automated Checks

The minimum validation set for this change:

```bash
# 1. Compile both affected modules and their dependencies
./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am compile

# 2. Run settings headless tests (17 tests — covers panel construction, search, apply/reset, persistence)
./mvnw -pl papiflyfx-docking-settings -am -Dtestfx.headless=true test

# 3. Run samples headless tests (12 tests — covers all catalog samples, login flows, persist, float)
./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test
```

Both passed successfully on the `settings` branch as of 2026-04-12.

## 3. Manual Verification Still Needed

| Scenario | Why automated coverage is insufficient | Priority |
|----------|----------------------------------------|----------|
| **Light/dark toggle on SettingsPanel** | No headless test toggles the settings panel theme. Visual token injection correctness (colors, borders, text fill) can only be verified visually or via style string inspection. | High |
| **Light/dark toggle on SamplesApp shell** | Top bar, placeholder, accent buttons, and navigation tree theme changes are not tested. The contrast-on-accent fix (Finding 1) specifically targets light mode. | High |
| **Accent button text contrast (WCAG AA)** | The `isDark(accent) ? "white" : "black"` logic is correct for the default `#007acc` accent, but edge cases (very light or very dark custom accents) can only be spot-checked visually or with a contrast ratio tool. | Medium |
| **Session restore round-trip** | Open settings, toggle theme, close/reopen — verify the settings panel re-renders correctly with the restored theme. Not covered by any test. | Medium |
| **Navigation tree category rendering** | Category cells use `context.theme().background()` and `context.theme().connectingLineColor()` — these are canvas-rendered, not CSS-styled, so headless tests cannot verify visual correctness. | Low |

**Recommended manual flow:**
```bash
./mvnw javafx:run -pl papiflyfx-docking-samples
```
1. Verify settings panel renders correctly in dark mode.
2. Toggle to light mode — verify all text, backgrounds, and borders update.
3. Toggle back to dark mode — verify no artifacts.
4. Click "Auth Settings" and "Login Demo" buttons — verify text is readable on accent background in both themes.
5. Verify navigation tree categories render with appropriate colors in both themes.

## 4. Residual Risk If Merged As-Is

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Theme toggle on SettingsPanel has no automated coverage — a future refactor could silently break `applyThemeTokens()` | Medium | Low | The pattern is well-established (same as `TreeSearchOverlay`, `GitHubToolbar`), but the gap exists |
| Hardcoded hex colors remain in `KeyboardShortcutsCategory` and `SecurityCategory` warning/status labels | Low | N/A | Out of scope for this task — these are category-internal inline styles, not the panel/toolbar/search chrome targeted by this fix |
| `SamplesApp` inline `setStyle()` overrides prevent CSS pseudo-class states (hover/focus) on top bar buttons | Low | N/A | Accepted as known demo-app limitation (Finding 2 in ui-ux-findings.md) — not a regression |
| Pre-existing Monocle `BufferOverflowException` noise in headless tests could mask a real rendering failure | Very Low | Very Low | The noise is infrastructure-level and predates this change; test assertions still pass |

## Summary

The change is safe to merge. Both affected modules compile and pass all headless tests. The primary residual risk is the absence of a theme-toggle regression test for `SettingsPanel` — the exact behavior this fix introduces. Adding a single test that toggles `runtime.themeProperty()` to `Theme.light()` and back would close the most significant coverage gap.

No new regressions detected. No shared contracts or session formats were changed. Settings runtime, persistence, and restore behavior are untouched.

## Handoff

```
Lead Agent: @qa-engineer
Priority: P1
Task Scope: Regression validation for settings demo UI fix
Impacted Modules: papiflyfx-docking-settings, papiflyfx-docking-samples
Files Changed: None (review-only)
Key Invariants: Settings runtime/persistence untouched, session restore unaffected, no new tokens introduced
Validation Performed: Headless test runs for settings (17/17) and samples (12/12), code review for coverage gaps
Open Risks / Follow-ups: (1) No theme-toggle test for SettingsPanel — recommend adding before next settings change (2) Manual visual verification pending
Required Reviewer: N/A — validation complete
```

## Dark-Mode Settings Follow-up Addendum

**Reviewer:** @qa-engineer
**Date:** 2026-04-12
**Status:** Approve with low residual risk

### 1. Remaining regression coverage gaps

1. **ColorPicker custom-color dialog is still untested (Low).**
   `SettingsPanelFxTest.colorPickerPopupUsesTokenDrivenDarkThemeSurface()` verifies the closed control plus the `.color-palette` popup surface, border, and action link, but it does not follow the `Custom Color...` path into JavaFX's deeper custom-color dialog. The dark-mode popup surface requested in this follow-up is covered; the secondary dialog path still needs manual verification.
2. **Demo-style coverage is wiring-focused, not full computed-skin coverage (Low).**
   `SettingsDemoStyleIntegrationTest` correctly audits the real demo categories for the shared style-class path across text/password fields, combo boxes, text areas, check boxes, list views, buttons, and appearance color pickers. It does not assert resolved background/text/border colors for every one of those control families, so a future selector drift inside `settings.css` could still require visual review even when style classes remain attached.
3. **Cross-theme toggle coverage remains outside this narrow follow-up (Low).**
   Both focused regression tests execute in `Theme.dark()` only. That is sufficient for the dark-mode/settings-style finding set under review here, but it still leaves broader light-mode/theme-toggle regression coverage to the existing future-work bucket.

### 2. The narrowest relevant automated checks

| Check | Command | Result |
|---|---|---|
| Touched-module compile | `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-login,papiflyfx-docking-github,papiflyfx-docking-code,papiflyfx-docking-hugo,papiflyfx-docking-samples -am compile` | PASS |
| Focused dark-mode regression tests | `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am -Dtest=SettingsPanelFxTest,SettingsDemoStyleIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` | PASS — `SettingsPanelFxTest` 5/5, `SettingsDemoStyleIntegrationTest` 1/1 |

**Additional scoped confirmation:** `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-login,papiflyfx-docking-github,papiflyfx-docking-code,papiflyfx-docking-hugo,papiflyfx-docking-samples -am -Dtestfx.headless=true test` also passed in the current repo state. The previously logged broader reactor failure in `papiflyfx-docking-media` did **not** reproduce here, so I did not find an out-of-scope reactor blocker to attribute separately.

### 3. Manual verification still needed

1. Open the samples app in dark mode and inspect the appearance-category `ColorPicker` end to end: closed control, palette popup, and the `Custom Color...` dialog path.
2. In the settings panel, move focus away from the selected category row and confirm the inactive-selected text remains readable on the target platform theme/rendering stack.
3. If release sign-off requires dual-theme confidence rather than dark-mode-only closure, do one interactive light/dark toggle pass through the settings demo shell.

### 4. Residual risk if merged as-is

**Low.** The shared class-wiring path is covered in the actual demo settings panel, the dark-mode color-picker popup surface is covered in `SettingsPanelFxTest`, the touched modules compile cleanly, and the scoped headless reactor now passes. The remaining risk is limited to visual subtleties that are awkward to assert headlessly, primarily the deeper custom-color dialog path and general cross-theme inspection outside this dark-mode-focused follow-up.

### 5. Whether this follow-up closes the dark-mode/settings-style findings

**Yes.** For the findings tracked in the dark-mode follow-up, this implementation closes the token-driven styling gaps for the remaining settings controls and adds the expected narrow regression coverage for both the shared style-class path and the dark-mode popup surface path. The open items above are follow-on hardening opportunities, not blockers for closing the dark-mode/settings-style finding set.
