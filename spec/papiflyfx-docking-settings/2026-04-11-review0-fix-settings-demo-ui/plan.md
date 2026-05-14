# Plan — Fix Settings Demo UI Regression

**Priority:** P1 (High)
**Lead Agent:** @ops-engineer
**Required Reviewers:** @ui-ux-designer, @qa-engineer
**Workflow:** Fast path with review (no separate research.md — problem is well-characterized)

## Goal

Restore token-driven styling in `papiflyfx-docking-settings` and make the `SamplesApp` demo theme-aware, without changing settings runtime or persistence behavior.

## Scope

### In Scope

1. **SettingsPanel token injection** — Derive a `UiCommonPalette` from the active `Theme`, inject `UiCommonThemeSupport.themeVariables(...)` and `UiStyleSupport.metricVariables()` on the `SettingsPanel` root via `setStyle(...)`, and refresh when the theme changes. This is the pattern already used by `GitHubToolbar`, `TreeSearchOverlay`, `GoToLineController`, and `SearchController`.

2. **SettingsToolbar and SettingsSearchBar alignment** — Adopt shared UI primitives and classes from `papiflyfx-docking-api`: `UiCommonStyles.ensureLoaded(...)`, `UiPillButton`, `UiStatusSlot`, `pf-ui-field`, `pf-ui-compact-field`, `UiMetrics`. Replace any ad-hoc styling with shared token classes.

3. **SamplesApp theme-awareness** — Replace hardcoded dark-only colors in the top bar (`-fx-background-color: #3c3c3c`), content area (`-fx-background-color: #1e1e1e`), and navigation cell rendering (`Color.web("#1e1e1e")`, `Color.web("#888888")`) with values derived from the active `Theme` via `UiCommonThemeSupport`. Replace the static `NAVIGATION_THEME` constant with a theme-aware builder using `TreeViewThemeMapper` or equivalent.

### Out of Scope (Non-Goals)

- Settings runtime, persistence, or secret-store behavior changes
- `SettingsContentFactory` or `SettingsStateAdapter` contract changes
- New `-pf-ui-*` tokens in `papiflyfx-docking-api` (consume existing vocabulary only)
- Changes to `settings.css` rules (unless strictly necessary for correctness)
- Changes to other content modules

## Impacted Modules

| Module | Files Expected to Change |
|--------|--------------------------|
| `papiflyfx-docking-settings` | `SettingsPanel.java`, `SettingsToolbar.java`, `SettingsSearchBar.java` |
| `papiflyfx-docking-samples` | `SamplesApp.java` |

## Key Invariants

1. Settings runtime ownership model unchanged — single shared `SettingsRuntime`
2. JSON persistence format unchanged — backward compatibility preserved
3. Session restore round-trip works — `SettingsContentFactory` + `SettingsStateAdapter` contracts untouched
4. `-pf-ui-*` token vocabulary consumed, not extended
5. Theme property binding chain propagates both light and dark themes end-to-end
6. `settings.css` rules remain authoritative for settings panel styling — Java code only injects the variables they reference

## Implementation Phases

### Phase 1: SettingsPanel Token Injection

1. Import `UiCommonThemeSupport`, `UiCommonPalette`, `UiStyleSupport` into `SettingsPanel`.
2. In the constructor, after loading stylesheets, build and apply `setStyle(...)` with `UiCommonThemeSupport.themeVariables(palette) + UiStyleSupport.metricVariables()`.
3. In the `themeListener`, rebuild the palette from the new theme and re-apply `setStyle(...)`.
4. Derive `UiCommonPalette` from `Theme` using the same approach as `GitHubToolbar` (background, header, accent, text colors from the theme object).

### Phase 2: SettingsToolbar and SettingsSearchBar Alignment

1. Replace ad-hoc button creation in `SettingsToolbar` with `UiPillButton` or shared style classes.
2. Replace ad-hoc status labels with `UiStatusSlot` where applicable.
3. Ensure `SettingsSearchBar` text field uses `pf-ui-field` or `pf-ui-compact-field` style class.
4. Confirm `UiCommonStyles.ensureLoaded(...)` is called on these components (or inherited from `SettingsPanel`).

### Phase 3: SamplesApp Theme-Awareness

1. Replace `NAVIGATION_THEME` static constant with a method that builds a `TreeViewTheme` from the active `Theme` using `UiCommonThemeSupport` or `TreeViewThemeMapper.map(theme)`.
2. Listen to `themeProperty` changes and update `sampleTree.setTreeViewTheme(...)`.
3. Replace hardcoded top bar style (`#3c3c3c`) with theme-derived header background.
4. Replace hardcoded content area style (`#1e1e1e`) with theme-derived canvas background.
5. Replace hardcoded navigation cell colors with theme-derived text/background colors.
6. Update `styleTopBarButton(...)` to use theme-aware colors.
7. Update `themeToggle` button text fill to follow theme.

## Acceptance Criteria

- [ ] `SettingsPanel` injects theme and metric CSS variables on its root and refreshes them on theme change
- [ ] `SettingsToolbar` and `SettingsSearchBar` use shared UI primitives and style classes from `ui-common.css`
- [ ] `SamplesApp` top bar, content area, navigation rendering, and tree theme derive colors from the active `Theme`
- [ ] No task-specific hardcoded hex colors remain in the affected settings/sample UI surfaces
- [ ] `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am compile` succeeds
- [ ] `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am -Dtestfx.headless=true test` passes
- [ ] `./mvnw clean package` passes (no regressions in other modules)
- [ ] Manual visual verification: light/dark toggle restyles settings panel, sample top bar, content area, and navigation tree

## Validation Strategy

1. **Compile check:** `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am compile`
2. **Headless tests:** `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-samples -am -Dtestfx.headless=true test`
3. **Full build:** `./mvnw clean package`
4. **Manual visual verification (interactive):**
   - `./mvnw javafx:run -pl papiflyfx-docking-samples`
   - Toggle light/dark — verify settings panel text, backgrounds, controls render correctly in both themes
   - Verify navigation sidebar follows theme
   - Verify toolbar/search bar spacing and styling consistency
5. **Session restore round-trip:** open settings, switch themes, close and reopen — verify settings state persists

## Risks

| Risk | Mitigation |
|------|------------|
| `UiCommonPalette` construction from `Theme` may not cover all tokens `settings.css` uses | Check `settings.css` token list against `UiCommonThemeSupport.themeVariables()` output before implementation |
| `TreeViewThemeMapper` may not exist or may produce unexpected results for navigation sizing | Fall back to manual palette construction if needed; preserve current row height and indent |
| Headless tests may not catch visual regressions | Manual visual verification is part of the validation plan |