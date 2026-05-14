package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.control.CheckBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;

public class BooleanSettingControl extends SettingControl<Boolean> {

    private final CheckBox checkBox;

    public BooleanSettingControl(SettingDefinition<Boolean> definition) {
        super(definition);
        this.checkBox = org.metalib.papifly.fx.settings.ui.SettingsUiStyles.applyCheckBox(new CheckBox());
        this.checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> onValueChanged());
        setEditor(checkBox);
    }

    @Override
    public Boolean getValue() {
        return checkBox.isSelected();
    }

    @Override
    public void setValue(Boolean value) {
        checkBox.setSelected(Boolean.TRUE.equals(value));
        onValueChanged();
    }
}
