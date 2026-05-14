# Validation — Fix Settings Demo UI Regression

**Priority:** P1
**Lead Agent:** @ops-engineer
**Status:** PR #10 Review Remediation Complete (incl. multi-reviewer follow-up 2026-04-13)

## 2026-04-13 Multi-Reviewer Follow-up Validation

### Context
The multi-reviewer pass found that `SettingsCategoryList` still carried the generic `pf-settings-list` class, causing `SettingsPanelFxTest.categoryListUsesDedicatedSelectorOnly()` to fail. The validation record was stale as it did not reflect this regression.

### Fix Applied
Removed `"pf-settings-list"` from the `SettingsUiStyles.apply(...)` call in `SettingsCategoryList` constructor (line 26). The control is now styled only by `pf-settings-category-list`.

### Automated Checks (rerun 2026-04-13)

| Check | Result |
|-------|--------|
| `./mvnw -pl papiflyfx-docking-settings -am compile` | PASS — 5/5 modules |
| `./mvnw -pl papiflyfx-docking-settings -am -Dtest=EncryptedFileSecretStoreTest,AtomicFileWriterTest,SettingsPanelFxTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test` | PASS — 21/21 tests |
| `./mvnw -pl papiflyfx-docking-settings -am -Dtestfx.headless=true test` | PASS — 28/28 tests |

### Manual Verification

- Not run in this follow-up pass.
- Interactive visual review and session round-trip checks from the original demo follow-up remain a reviewer/manual pass item.

### Residual Risks

| Risk | Severity | Notes |
|------|----------|-------|
| Secret-store logging is intentionally noisy on tamper/decrypt failure | Low | Expected for a fail-closed path; the regression tests confirm no on-disk rewrite happens before the exception escapes |
| Headless tests cover scope normalization and palette wiring but not interactive visual polish | Low | `@ui-ux-designer` review is still the right place for final visual confirmation |
| Category-list dark-mode styling now relies solely on `.pf-settings-category-list` rules in `settings.css` | Low | The dedicated selector already covers background, border, selection, hover, and inactive states — no generic fallback needed |

### Reviewer Status

| Reviewer | Focus | Status |
|----------|-------|--------|
| `@auth-specialist` | Fail-closed decrypt/auth behavior and on-disk preservation | Ready for review |
| `@qa-engineer` | Regression coverage and headless validation (updated with follow-up rerun) | Ready for review |
| `@ui-ux-designer` | `surfaceControl` palette mapping and category-list selector isolation | Ready for review |
| `@spec-steward` | `progress.md` / `validation.md` accuracy after follow-up | Ready for review |

## Automated Checks

### 1. Compile Check
```bash
./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am compile
```
- **Expected:** Clean compilation with no errors or warnings related to missing imports or unresolved symbols.
- **Result:** —

### 2. Headless Tests (Targeted)
```bash
./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am -Dtestfx.headless=true test
```
- **Expected:** All existing tests pass. `SettingsPanelFxTest` and `SamplesSmokeTest` remain green.
- **Result:** —

### 3. Full Build
```bash
./mvnw clean package
```
- **Expected:** No regressions in any module.
- **Result:** —

## Manual Verification

### 4. Visual Theme Toggle (Interactive)
```bash
./mvnw javafx:run -pl papiflyfx-docking-samples
```

#### Checks:
- [ ] **Settings panel (dark):** Background, text, borders, category list, scroll area, toolbar render correctly using dark-theme token values.
- [ ] **Settings panel (light):** Toggle to light — all surfaces, text, borders, and controls update to light-theme token values. No stale dark colors.
- [ ] **Search bar:** Input field uses `pf-ui-field` or `pf-ui-compact-field` styling. Placeholder text visible in both themes.
- [ ] **Toolbar buttons:** Apply/Reset buttons use `UiPillButton` styling or equivalent shared classes. Dirty label and status label use correct semantic colors.
- [ ] **Scope selector:** Scope chips/buttons visible and styled in both themes.
- [ ] **SamplesApp top bar (dark):** Background uses theme-derived header color. Title text, auth hint, and buttons are readable.
- [ ] **SamplesApp top bar (light):** Toggle to light — background, text, and button colors update. No stale `#3c3c3c` or hardcoded dark fills.
- [ ] **Content area (dark):** Background uses theme-derived canvas color.
- [ ] **Content area (light):** Background updates on toggle. No stale `#1e1e1e`.
- [ ] **Navigation tree (dark):** Category headers and sample items render with theme-derived text and background colors.
- [ ] **Navigation tree (light):** Tree theme updates on toggle. Category backgrounds, text fills, selection highlight, and hover state all follow the light theme.
- [ ] **No visual artifacts:** No flash of unstyled content, no color bleed, no mismatched borders on theme switch.

### 5. Session Restore Round-Trip
- [ ] Open `SamplesApp`, navigate to Settings Panel sample, switch theme, close app.
- [ ] Reopen — settings state (category selection, any applied values) persists correctly.
- [ ] Session restore does not produce errors or visual glitches.

## Residual Risks

| Risk | Severity | Notes |
|------|----------|-------|
| Headless tests cannot validate visual correctness | Low | Mitigated by manual visual verification step |
| Token coverage gaps in `settings.css` vs `UiCommonThemeSupport` output | Medium | Pre-check: compare token lists before implementation |
| Navigation tree custom cell renderer may not fully respond to theme change without tree rebuild | Low | Verify that `setTreeViewTheme(...)` triggers repaint |

## Review Assignments

| Reviewer | Focus | Status |
|----------|-------|--------|
| @ui-ux-designer | Token usage, theme behavior, spacing, interaction polish | Not Started |
| @qa-engineer | Regression risk, automated checks, manual verification gaps | Not Started |
