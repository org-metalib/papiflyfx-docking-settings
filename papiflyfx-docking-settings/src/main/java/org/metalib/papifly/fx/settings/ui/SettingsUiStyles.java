package org.metalib.papifly.fx.settings.ui;

import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;

public final class SettingsUiStyles {

    public static final String CATEGORY_ROW = "pf-settings-category-row";
    public static final String CATEGORY_LABEL = "pf-settings-category-label";
    public static final String COMPACT_FIELD = "pf-ui-compact-field";
    public static final String COMBO_BOX = "pf-settings-combo-box";
    public static final String TEXT_AREA = "pf-settings-text-area";
    public static final String CHECK_BOX = "pf-settings-check-box";
    public static final String LIST = "pf-settings-list";
    public static final String ACTION_BUTTON = "pf-ui-compact-action-button";
    public static final String ACTION_BUTTON_SECONDARY = "pf-ui-compact-action-button-secondary";
    public static final String SECTION_TITLE = "pf-settings-section-title";

    private SettingsUiStyles() {
    }

    public static <T extends Node> T apply(T node, String... styleClasses) {
        for (String styleClass : styleClasses) {
            if (styleClass != null
                && !styleClass.isBlank()
                && !node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
        }
        return node;
    }

    public static <T extends TextInputControl> T applyCompactField(T field) {
        return apply(field, COMPACT_FIELD);
    }

    public static <T extends ComboBoxBase<?>> T applyCompactField(T field) {
        return apply(field, COMPACT_FIELD, COMBO_BOX);
    }

    public static <T extends TextArea> T applyTextArea(T area) {
        return apply(area, TEXT_AREA);
    }

    public static <T extends CheckBox> T applyCheckBox(T checkBox) {
        return apply(checkBox, CHECK_BOX);
    }

    public static <T extends ListView<?>> T applyList(T listView) {
        return apply(listView, LIST);
    }

    public static <T extends ButtonBase> T applyActionButton(T button) {
        return apply(button, ACTION_BUTTON);
    }

    public static <T extends ButtonBase> T applySecondaryActionButton(T button) {
        return apply(button, ACTION_BUTTON, ACTION_BUTTON_SECONDARY);
    }
}
