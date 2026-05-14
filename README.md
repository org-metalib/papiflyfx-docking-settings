# papiflyfx-docking-settings

Extracted from the PapiflyFX Docking monorepo.

## Modules

- `papiflyfx-docking-settings-api`
- `papiflyfx-docking-settings`

## Build

Use the split-local Maven repository so cross-repo snapshots resolve from the extraction workspace:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true clean verify
```

Lead agent: `@ops-engineer`.

## Notes

- Core dependencies resolve from the split-local Maven repository through `${papiflyfx.version}`.
