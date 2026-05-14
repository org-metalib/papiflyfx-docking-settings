package org.metalib.papifly.fx.settings.ui.controls;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumSettingControl<T extends Enum<T>> extends SettingControl<T> {

    private final ComboBox<T> comboBox;

    public EnumSettingControl(SettingDefinition<T> definition) {
        super(definition);
        T defaultValue = definition.defaultValue();
        if (defaultValue == null) {
            throw new IllegalArgumentException("ENUM controls require a non-null default value.");
        }
        T[] constants = (T[]) defaultValue.getDeclaringClass().getEnumConstants();
        this.comboBox = SettingsUiStyles.applyCompactField(new ComboBox<>(FXCollections.observableArrayList(constants)));
        this.comboBox.valueProperty().addListener((obs, oldValue, newValue) -> onValueChanged());
        setEditor(comboBox);
    }

    @Override
    public T getValue() {
        return comboBox.getValue();
    }

    @Override
    public void setValue(T value) {
        comboBox.setValue(value == null ? definition().defaultValue() : value);
        onValueChanged();
    }
}
