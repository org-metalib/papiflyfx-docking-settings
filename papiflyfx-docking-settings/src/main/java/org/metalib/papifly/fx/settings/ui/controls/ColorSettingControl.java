package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

public class ColorSettingControl extends SettingControl<String> {

    private final ColorPicker colorPicker;

    public ColorSettingControl(SettingDefinition<String> definition) {
        super(definition);
        this.colorPicker = SettingsUiStyles.applyCompactField(new ColorPicker());
        this.colorPicker.valueProperty().addListener((obs, oldValue, newValue) -> onValueChanged());
        setEditor(colorPicker);
    }

    @Override
    public String getValue() {
        return toHex(colorPicker.getValue());
    }

    @Override
    public void setValue(String value) {
        colorPicker.setValue(parse(value));
        onValueChanged();
    }

    private Color parse(String value) {
        if (value == null || value.isBlank()) {
            return Color.BLACK;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException exception) {
            return Color.BLACK;
        }
    }

    private String toHex(Color color) {
        int red = (int) Math.round(color.getRed() * 255.0);
        int green = (int) Math.round(color.getGreen() * 255.0);
        int blue = (int) Math.round(color.getBlue() * 255.0);
        return String.format("#%02x%02x%02x", red, green, blue);
    }
}
