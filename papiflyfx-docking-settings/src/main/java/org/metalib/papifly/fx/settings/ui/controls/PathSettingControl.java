package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

public class PathSettingControl extends SettingControl<String> {

    private final TextField textField;

    public PathSettingControl(SettingDefinition<String> definition) {
        super(definition);
        this.textField = SettingsUiStyles.applyCompactField(new TextField());
        this.textField.textProperty().addListener((obs, oldValue, newValue) -> onValueChanged());
        Button clearButton = SettingsUiStyles.applySecondaryActionButton(new Button("Clear"));
        clearButton.setOnAction(event -> setValue(""));
        HBox row = new HBox(8, textField, clearButton);
        setEditor(row);
    }

    @Override
    public String getValue() {
        return textField.getText();
    }

    @Override
    public void setValue(String value) {
        textField.setText(value == null ? "" : value);
        onValueChanged();
    }
}
