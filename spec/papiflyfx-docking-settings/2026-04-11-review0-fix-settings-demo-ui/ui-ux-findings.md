# @ui-ux-designer Review — Fix Settings Demo UI Regression

**Reviewer:** @ui-ux-designer
**Status:** Approve with findings
**Date:** 2026-04-12

## Overall Assessment

The change correctly restores token-driven styling and eliminates all hardcoded dark-only hex values. The token injection pattern matches established usage (`TreeSearchOverlay`, `GitHubToolbar`). Shared primitives (`UiPillButton`, `UiStatusSlot`, `pf-ui-compact-field`) are correctly adopted.

## Findings (ordered by severity)

### Finding 1 — Medium: SamplesApp accent-on-accent text readability

**File:** `SamplesApp.java:281-282`

```java
loginDemoButton.setStyle("-fx-background-color: " + accentCss + "; -fx-text-fill: " + textPrimaryCss + ";");
themeToggle.setStyle("-fx-background-color: " + accentCss + "; -fx-text-fill: " + textPrimaryCss + ";");
```

`textPrimary` is derived from `Theme.textColor()` — light grey in dark mode, dark grey in light mode. When buttons use `accent` background (`#007acc` blue), the text should use a high-contrast-on-accent color (white in most cases), not the general `textPrimary`. In light mode, `textPrimary` is approximately `rgb(50, 50, 50)` on a `#007acc` background — the contrast ratio may fall below WCAG AA (4.5:1).

**Recommendation:** Use `UiCommonThemeSupport.contrastOn(accent)` or hardcode `Color.WHITE` for text on accent-filled buttons, consistent with how `UiCommonThemeSupport.themeVariables()` emits `-pf-ui-text-on-accent`. The token `themeVariables()` already computes `contrastOn(accent)` — but it's private. A simple fix:

```java
Color textOnAccent = UiCommonThemeSupport.isDark(accent) ? Color.WHITE : Color.BLACK;
```

**Severity:** Medium — accessibility / contrast regression in light mode.

---

### Finding 2 — Low: Toolbar buttons lack hover/focus state indication

**File:** `SamplesApp.java:280-282`

The top bar buttons (`authSettingsButton`, `loginDemoButton`, `themeToggle`) use plain inline `setStyle(...)` for background and text fill, but don't define `:hover`, `:pressed`, or `:focused` pseudo-class styles. Inline styles in JavaFX override CSS pseudo-class rules, so hovering these buttons will produce no visual change.

**Recommendation:** Either:
- (a) Switch these buttons to use style classes (e.g., add a `pf-samples-topbar-button` class and define it in a samples CSS), or
- (b) Accept this as a demo-app limitation and note it as a known gap for the interactive verification.

This is not a regression (the old hardcoded buttons had the same limitation), but worth noting for future polish.

**Severity:** Low — ergonomics, not a regression.

---

### Finding 3 — Low: SettingsPanel `buildPalette` fully-qualified Color reference

**File:** `SettingsPanel.java:163`

```java
UiCommonThemeSupport.alpha(javafx.scene.paint.Color.BLACK, 0.25)
```

Cosmetic only — `Color` is already used transitively through `UiCommonThemeSupport`, but not imported in `SettingsPanel`. This works but is a minor code style inconsistency with the rest of the file.

**Recommendation:** Add `import javafx.scene.paint.Color;` and use `Color.BLACK`.

**Severity:** Low — code style, no functional impact.

---

### Finding 4 — Info: Token coverage is complete

Cross-referencing `settings.css` token usage against `UiCommonThemeSupport.themeVariables()` output:

| Token in `settings.css` | Emitted by `themeVariables()`? |
|---|---|
| `-pf-ui-surface-panel` | Yes |
| `-pf-ui-surface-panel-subtle` | Yes |
| `-pf-ui-border-default` | Yes |
| `-pf-ui-text-primary` | Yes |
| `-pf-ui-surface-selected` | Yes |
| `-pf-ui-surface-control-hover` | Yes |
| `-pf-ui-accent` | Yes |
| `-pf-ui-warning` | Yes |
| `-pf-ui-success` | Yes |
| `-pf-ui-text-muted` | Yes |
| `-pf-ui-danger` | Yes |

All 11 unique token references in `settings.css` are covered. `UiStyleSupport.metricVariables()` also covers the spacing/sizing tokens used by `ui-common.css` primitives. No gaps.

---

### Finding 5 — Info: Shared primitive adoption is correct

| Component | Primitive | CSS class | Verified |
|---|---|---|---|
| Apply/Reset buttons | `UiPillButton` | `pf-ui-pill` | Yes — `ui-common.css` defines `:hover`, `:armed`, `:focused` |
| Dirty + status labels | `UiStatusSlot` | `pf-ui-status-slot` | Yes — alignment and spacing from `ui-common.css` |
| Search field | `TextField` | `pf-ui-compact-field` | Yes — `ui-common.css` defines field sizing, border, focus ring |
| Action buttons | `UiPillButton` | `pf-ui-pill` | Yes |

## Summary

| # | Severity | Finding | Action |
|---|---|---|---|
| 1 | **Medium** | Accent button text may fail contrast in light mode | Fix: use contrast-on-accent color for `-fx-text-fill` on accent-background buttons |
| 2 | Low | Top bar buttons have no hover/focus states (inline style override) | Accept as demo limitation or add style class |
| 3 | Low | Fully-qualified `javafx.scene.paint.Color.BLACK` in `buildPalette` | Add import |
| 4 | Info | Token coverage is complete | No action |
| 5 | Info | Shared primitive adoption is correct | No action |

## Required UX Follow-up

Finding 1 should be addressed before close — the accent-on-primary contrast in light mode is an accessibility concern. Findings 2-3 are optional improvements.
