# Fix Settings Demo UI — Template-Aligned Prompt Set

This prompt pack follows `spec/agents/prompts.md` and the single-lead routing rule in `spec/agents/README.md`.

## Problem Summary

The settings refactor introduced `settings.css` rules that depend on `-pf-ui-*` tokens, but `SettingsPanel` never injects those variables on its root. `SamplesApp` also still uses hardcoded dark-only colors and a static dark-only navigation theme. The fix should restore token-driven styling in `papiflyfx-docking-settings` and make the demo sample theme-aware without changing settings runtime or persistence behavior.

## Prompt Flow

| File | Template | Agent | Purpose | When |
|------|----------|-------|---------|------|
| `.prompt-0.md` | Intake Prompt | `@spec-steward` | Route the task, confirm lead/reviewers, and produce the first handoff block | First |
| `.prompt-1.md` | Lead Prompt | `@ops-engineer` | Implement the fix across `papiflyfx-docking-settings` and `papiflyfx-docking-samples` | After intake |
| `.prompt-2.md` | Review Prompt | `@ui-ux-designer` | Review token usage, theme behavior, spacing, and interaction polish | After implementation |
| `.prompt-3.md` | Validation Prompt | `@qa-engineer` | Validate regression risk, automated checks, and remaining manual verification | After implementation |

## Key References

- `spec/agents/prompts.md`
- `spec/agents/README.md`
- `papiflyfx-docking-settings/README.md`
- `spec/papiflyfx-docking-samples/README.md`

If you need closure after validation, use the `@spec-steward` closure template from `spec/agents/prompts.md` with the same task context.
