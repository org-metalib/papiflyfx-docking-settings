package org.metalib.papifly.fx.settings.api;

/**
 * ServiceLoader SPI exposing the default settings-backed storage services.
 *
 * <p>Optional consumers can use this seam to integrate with the settings
 * runtime without taking a direct dependency on the settings implementation
 * module.</p>
 */
public interface SettingsServicesProvider {

    SettingsStorage storage();

    SecretStore secretStore();
}
