# papiflyfx-docking-settings-api

The public SPI for PapiflyFX settings. This module contains only interfaces, records, and enums used by settings contributors and runtime integrations.

## Scope

- `SettingsCategory` and `SettingsContributor` for ServiceLoader-based category registration
- `SettingsContext` for access to storage, secrets, theme state, and active scope
- `SettingsStorage` for non-secret settings persistence
- `SettingsServicesProvider` for optional ServiceLoader-based access to the default settings-backed storage services
- `SecretStore` and `SecretKeyNames` for shared secret management
- typed setting metadata via `SettingDefinition`, `SettingType`, `SettingScope`, `SettingsValidator`, `ValidationResult`, and `SettingsAction`

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-settings-api</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## ServiceLoader

Contributing modules register `SettingsCategory` or `SettingsContributor` implementations in their own `META-INF/services` resources. This module does not ship providers.
