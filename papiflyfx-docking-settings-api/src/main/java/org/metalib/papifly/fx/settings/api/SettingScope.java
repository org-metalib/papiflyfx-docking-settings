package org.metalib.papifly.fx.settings.api;

import java.util.List;

public enum SettingScope {
    APPLICATION,
    WORKSPACE,
    SESSION;

    public static List<SettingScope> resolutionOrder() {
        return List.of(SESSION, WORKSPACE, APPLICATION);
    }
}
