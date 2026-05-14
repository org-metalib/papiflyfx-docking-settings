# PapiflyFX Docking Settings Progress

## Status

- Overall: completed
- Phase 1: completed
- Phase 2: completed
- Phase 3: completed
- Phase 4: completed
- Phase 5: completed
- Phase 6: completed

## Delivered

- Added `papiflyfx-docking-settings-api` with the shared settings SPI, storage contracts, secret-store contracts, README, and parent Maven wiring.
- Added `papiflyfx-docking-settings` with JSON persistence, migration support, runtime wiring, dock integration, reusable controls, built-in categories, advanced categories, and ServiceLoader registration.
- Added native-backed secret store integrations for macOS Keychain, Windows Credential Manager, Linux `libsecret`, plus encrypted-file and in-memory fallbacks.
- Added contributing settings categories and integration points in `papiflyfx-docking-github`, `papiflyfx-docking-code`, `papiflyfx-docking-hugo`, and the new `papiflyfx-docking-login` module.
- Added `papiflyfx-docking-login` with `SecureSecretStore`, `SecretStoreSecureAdapter`, a lightweight `AuthSessionBroker` runtime, `AuthenticationCategory`, and ServiceLoader registration.
- Added the settings sample to `papiflyfx-docking-samples` and wired the samples runtime so contributed categories are visible in the assembled app.
- Added/updated module READMEs for `papiflyfx-docking-settings-api` and `papiflyfx-docking-settings`.

## Verification

- `./mvnw -pl papiflyfx-docking-settings -am -Dtestfx.headless=true test`
- `./mvnw -pl papiflyfx-docking-settings,papiflyfx-docking-login,papiflyfx-docking-samples -am -DskipTests compile`
- `./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false test`
