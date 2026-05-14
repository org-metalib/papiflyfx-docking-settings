package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.control.TextField;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

public class StringSettingControl extends SettingControl<String> {

    private final TextField textField;

    public StringSettingControl(SettingDefinition<String> definition) {
        super(definition);
        this.textField = SettingsUiStyles.applyCompactField(new TextField());
        this.textField.textProperty().addListener((obs, oldValue, newValue) -> onValueChanged());
        setEditor(textField);
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
