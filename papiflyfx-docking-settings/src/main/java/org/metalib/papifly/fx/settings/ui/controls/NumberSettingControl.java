package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.control.TextField;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.ValidationResult;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

@SuppressWarnings("unchecked")
public class NumberSettingControl<T extends Number> extends SettingControl<T> {

    private final TextField textField;

    public NumberSettingControl(SettingDefinition<T> definition) {
        super(definition);
        this.textField = SettingsUiStyles.applyCompactField(new TextField());
        this.textField.textProperty().addListener((obs, oldValue, newValue) -> onValueChanged());
        setEditor(textField);
    }

    @Override
    public T getValue() {
        String text = textField.getText();
        if (text == null || text.isBlank()) {
            return definition().defaultValue();
        }
        try {
            if (definition().type() == SettingType.INTEGER) {
                return (T) Integer.valueOf(Integer.parseInt(text.trim()));
            }
            return (T) Double.valueOf(Double.parseDouble(text.trim()));
        } catch (NumberFormatException exception) {
            return definition().defaultValue();
        }
    }

    @Override
    public ValidationResult validateCurrentValue() {
        String text = textField.getText();
        if (text != null && !text.isBlank()) {
            try {
                if (definition().type() == SettingType.INTEGER) {
                    Integer.parseInt(text.trim());
                } else {
                    Double.parseDouble(text.trim());
                }
            } catch (NumberFormatException exception) {
                return ValidationResult.error("Enter a valid number.");
            }
        }
        return super.validateCurrentValue();
    }

    @Override
    public void setValue(T value) {
        textField.setText(value == null ? "" : String.valueOf(value));
        onValueChanged();
    }
}
