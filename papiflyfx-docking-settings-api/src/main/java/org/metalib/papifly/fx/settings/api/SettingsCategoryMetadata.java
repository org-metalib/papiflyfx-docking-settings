package org.metalib.papifly.fx.settings.api;

import javafx.scene.Node;

/**
 * Describes how a settings category is identified and presented in the shell UI.
 */
public interface SettingsCategoryMetadata {

    String id();

    String displayName();

    default Node icon() {
        return null;
    }

    default int order() {
        return 100;
    }
}
