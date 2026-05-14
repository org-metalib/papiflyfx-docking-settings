package org.metalib.papifly.fx.settings.api;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;

/**
 * Owns the interactive view and lifecycle for a settings category.
 */
public interface SettingsCategoryUI {

    Node buildSettingsPane(SettingsContext context);

    void apply(SettingsContext context);

    void reset(SettingsContext context);

    default boolean isDirty() {
        return false;
    }

    /**
     * Observable dirty state for deterministic UI binding.
     *
     * <p>Categories that return a non-null property enable the settings panel to
     * react to edits instantly without polling. Categories that return {@code null}
     * fall back to {@link #isDirty()} at natural action points.
     *
     * @return observable dirty property, or {@code null} if not supported
     */
    default ReadOnlyBooleanProperty dirtyProperty() {
        return null;
    }
}
