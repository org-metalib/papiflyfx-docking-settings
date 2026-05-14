package org.metalib.papifly.fx.settings.ui.controls;

import org.metalib.papifly.fx.settings.api.SettingDefinition;

public final class SettingControlFactory {

    private SettingControlFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> SettingControl<T> createControl(SettingDefinition<T> definition) {
        return switch (definition.type()) {
            case BOOLEAN -> (SettingControl<T>) new BooleanSettingControl((SettingDefinition<Boolean>) definition);
            case STRING, FONT -> (SettingControl<T>) new StringSettingControl((SettingDefinition<String>) definition);
            case INTEGER, DOUBLE -> (SettingControl<T>) new NumberSettingControl<>((SettingDefinition<? extends Number>) definition);
            case ENUM -> (SettingControl<T>) createEnumControl(definition);
            case COLOR -> (SettingControl<T>) new ColorSettingControl((SettingDefinition<String>) definition);
            case FILE_PATH, DIRECTORY_PATH -> (SettingControl<T>) new PathSettingControl((SettingDefinition<String>) definition);
            case SECRET -> (SettingControl<T>) new SecretSettingControl((SettingDefinition<String>) definition);
            case CUSTOM -> throw new UnsupportedOperationException("CUSTOM settings require explicit UI.");
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SettingControl<?> createEnumControl(SettingDefinition<?> definition) {
        return new EnumSettingControl((SettingDefinition) definition);
    }
}
