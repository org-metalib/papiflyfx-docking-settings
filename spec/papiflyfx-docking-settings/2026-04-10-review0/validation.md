# Validation & Threat Model: PapiflyFX Settings Security

Date: 2026-04-10
Phase: 3 — Security & Storage Hardening

## 1. Secret Administration — No Re-Exposure Invariant

### Requirement
Stored secret values must never be loaded back into UI controls. The settings UI
operates on aliases (key names) and lifecycle actions only.

### Implementation
- `SecurityCategory` uses `SecretStore.hasSecret(key)` to show "Set" / "Not Set"
  status. It never calls `getSecret(key)` for display.
- `DefinitionFormBinder.loadControl()` sets SECRET-type controls to empty string
  on load, rather than retrieving the stored value.
- `DefinitionFormBinder.saveControl()` only writes to the secret store when the
  user has entered a non-empty new value; an empty field means "no change".

### Residual Risk
- **In-process access:** Any code with a reference to `SecretStore` can call
  `getSecret()` programmatically. This is by design — the SPI is a runtime
  contract, not a sandbox. The invariant protects against UI-level re-exposure,
  not in-process information disclosure.
- **Memory residency:** Secret values exist in JVM heap between `getSecret()`
  and garbage collection. `EncryptedFileSecretStore` zeroes intermediate byte
  arrays but cannot control `String` interning or GC timing.

## 2. Atomic Persistence

### Requirement
Setting writes must survive process crashes and power loss without corrupting
the on-disk file.

### Implementation
`AtomicFileWriter.writeAtomically(target, content)`:
1. Creates parent directories if needed.
2. Copies existing `target` to `target.bak`.
3. Writes content to `target.tmp`.
4. Atomically renames `target.tmp` to `target` via `Files.move(..., ATOMIC_MOVE)`.

Used by:
- `JsonSettingsStorage.writeScope()` — application and workspace settings JSON.
- `EncryptedFileSecretStore.saveSecrets()` — encrypted secrets envelope.

### Residual Risk
- **ATOMIC_MOVE guarantee:** On most POSIX systems, `rename(2)` within the same
  filesystem is atomic. On Windows, `MoveFileEx` with `MOVEFILE_REPLACE_EXISTING`
  is atomic for NTFS. Cross-filesystem moves are not atomic; `AtomicFileWriter`
  writes the `.tmp` file in the same directory as the target, so this does not
  apply in practice.
- **Backup staleness:** The `.bak` file reflects the state before the most recent
  successful write. If two rapid writes occur, only the second-to-last state is
  preserved in `.bak`.

## 3. Corruption Recovery

### Requirement
The system must recover gracefully from corrupted settings or secrets files.

### Implementation
Both `JsonSettingsStorage.readScope()` and `EncryptedFileSecretStore.loadSecrets()`
follow the same recovery strategy:
1. Attempt to parse the primary file.
2. On failure (parse error, I/O error, decryption failure), attempt the `.bak` file.
3. If the backup also fails (or does not exist), reset to empty defaults.
4. All recovery steps are logged at WARNING/SEVERE level via `java.util.logging`.

### Residual Risk
- **Silent data loss:** If both primary and backup are corrupted, all settings
  revert to defaults with no user-facing prompt. The application functions
  correctly but user customizations are lost. This is acceptable for a desktop
  application where the alternative (crash loop) is worse.
- **No write-ahead log:** There is no WAL or multi-version history. The `.bak`
  file provides single-generation recovery only.

## 4. Encrypted-File Fallback Threat Model

### Context
When the OS keychain (macOS Keychain, Windows Credential Manager, Linux
libsecret) is unavailable, secrets fall back to `EncryptedFileSecretStore`:
AES-256-GCM encryption with PBKDF2-derived keys stored in `~/.papiflyfx/secrets.enc`.

### Key Derivation
The encryption key is derived from a machine-specific seed:
```
"{user.name}|{os.name}|{os.arch}|{hostname}"
```
with PBKDF2WithHmacSHA256 (65,536 iterations) and a random 16-byte salt.

### Threat Assessment

| Threat | Severity | Mitigation | Residual |
|--------|----------|------------|----------|
| File copied to another machine | Medium | Key derivation uses machine metadata — decryption fails on different host | Attacker on same OS/arch with same username and hostname can decrypt |
| Local privilege escalation | High | Secrets are accessible to any process running as the same user | File permissions should be restricted to owner-only (0600) |
| Machine metadata enumeration | Medium | Seed components are not secret (username, OS, hostname are public on most systems) | An attacker with read access to the file AND knowledge of the machine metadata can derive the key offline |
| Hostname change | Low | Existing secrets become inaccessible after hostname change | User must re-enter secrets; no migration path exists |
| Brute-force (offline) | Low | 65,536 PBKDF2 iterations raise cost but are below modern recommendations (600k+) | Sufficient for desktop secrets; not suitable for high-value credentials |

### Recommendations (Future Work)
1. **Increase PBKDF2 iterations** to 600,000+ per OWASP 2023 guidance, with a
   version migration to re-encrypt existing stores.
2. **Add file-permission enforcement** on creation: `PosixFilePermissions.asFileAttribute(Set.of(OWNER_READ, OWNER_WRITE))` on POSIX systems.
3. **Consider Argon2id** as a replacement KDF when Java 25+ makes it
   available via standard providers, for better resistance to GPU attacks.
4. **Prompt on hostname change:** Detect hostname mismatch and offer re-encryption
   with the new seed rather than silently failing.
5. **Prefer OS keychain:** The encrypted-file backend should remain a fallback.
   Encourage users to resolve keychain availability issues rather than relying
   on file-based encryption.
