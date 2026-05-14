package org.metalib.papifly.fx.settings.api;

import java.util.Map;

@FunctionalInterface
public interface SettingsMigrator {

    Map<String, Object> migrate(Map<String, Object> data, int fromVersion);
}
