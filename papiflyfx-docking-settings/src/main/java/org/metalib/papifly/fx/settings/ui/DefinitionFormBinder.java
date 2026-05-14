package org.metalib.papifly.fx.settings.ui;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.api.SettingsStorage;
import org.metalib.papifly.fx.settings.api.ValidationResult;
import org.metalib.papifly.fx.settings.ui.controls.SettingControl;
import org.metalib.papifly.fx.settings.ui.controls.SettingControlFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generates a composable settings form from a list of {@link SettingDefinition}s.
 *
 * <p>The binder creates typed {@link SettingControl} instances via {@link SettingControlFactory},
 * manages load/save against {@link SettingsStorage} and {@link SecretStore}, and exposes
 * observable dirty and valid properties for deterministic UI state binding.
 */
public final class DefinitionFormBinder {

    private final List<SettingDefinition<?>> definitions;
    private final Map<String, SettingControl<?>> controlsByKey = new LinkedHashMap<>();
    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper valid = new ReadOnlyBooleanWrapper(true);
    private final VBox pane;

    public DefinitionFormBinder(List<SettingDefinition<?>> definitions) {
        this.definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
        this.pane = new VBox(12);
        this.pane.setPadding(new Insets(8));
        this.pane.setFillWidth(true);
        buildControls();
    }

    public VBox pane() {
        return pane;
    }

    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty.getReadOnlyProperty();
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public ReadOnlyBooleanProperty validProperty() {
        return valid.getReadOnlyProperty();
    }

    public boolean isValid() {
        return valid.get();
    }

    @SuppressWarnings("unchecked")
    public <T> SettingControl<T> control(String key) {
        return (SettingControl<T>) controlsByKey.get(key);
    }

    /**
     * Loads values from storage/secret store into controls.
     */
    public void load(SettingsContext context) {
        for (SettingDefinition<?> definition : definitions) {
            SettingControl<?> control = controlsByKey.get(definition.key());
            if (control != null) {
                loadControl(definition, control, context);
            }
        }
        dirty.set(false);
        revalidate();
    }

    /**
     * Saves values from controls to storage/secret store.
     */
    public void save(SettingsContext context) {
        for (SettingDefinition<?> definition : definitions) {
            SettingControl<?> control = controlsByKey.get(definition.key());
            if (control != null) {
                saveControl(definition, control, context);
            }
        }
        dirty.set(false);
    }

    public void clearDirty() {
        dirty.set(false);
    }

    private void buildControls() {
        List<SettingControl<?>> controls = new ArrayList<>();
        for (SettingDefinition<?> definition : definitions) {
            if (definition.type() == SettingType.CUSTOM) {
                continue;
            }
            SettingControl<?> control = SettingControlFactory.createControl(definition);
            control.setOnChange(this::onControlChanged);
            controlsByKey.put(definition.key(), control);
            controls.add(control);
        }
        pane.getChildren().setAll(controls);
    }

    private void onControlChanged() {
        dirty.set(true);
        revalidate();
    }

    private void revalidate() {
        boolean allValid = true;
        for (SettingControl<?> control : controlsByKey.values()) {
            if (control.validationResult().level() == ValidationResult.Level.ERROR) {
                allValid = false;
                break;
            }
        }
        valid.set(allValid);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadControl(SettingDefinition<?> definition, SettingControl<?> control, SettingsContext context) {
        SettingScope scope = definition.scope();
        SettingsStorage storage = context.storage();
        SecretStore secretStore = context.secretStore();
        String key = definition.key();

        if (definition.secret() || definition.type() == SettingType.SECRET) {
            // Never reload stored secret values into UI controls.
            // Show empty field; the user must enter a new value to replace.
            ((SettingControl<String>) control).setValue("");
            return;
        }

        switch (definition.type()) {
            case BOOLEAN -> ((SettingControl<Boolean>) control).setValue(
                storage.getBoolean(scope, key, (Boolean) definition.defaultValue())
            );
            case STRING, FONT, FILE_PATH, DIRECTORY_PATH -> ((SettingControl<String>) control).setValue(
                storage.getString(scope, key, (String) definition.defaultValue())
            );
            case INTEGER -> ((SettingControl) control).setValue(
                storage.getInt(scope, key, ((Number) definition.defaultValue()).intValue())
            );
            case DOUBLE -> ((SettingControl) control).setValue(
                storage.getDouble(scope, key, ((Number) definition.defaultValue()).doubleValue())
            );
            case COLOR -> ((SettingControl<String>) control).setValue(
                storage.getString(scope, key, (String) definition.defaultValue())
            );
            case ENUM -> loadEnum((SettingDefinition) definition, (SettingControl) control, storage);
            default -> {}
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E extends Enum<E>> void loadEnum(SettingDefinition<E> definition, SettingControl<E> control, SettingsStorage storage) {
        String stored = storage.getString(definition.scope(), definition.key(), null);
        if (stored == null || stored.isBlank()) {
            control.setValue(definition.defaultValue());
            return;
        }
        Class<E> enumClass = (Class<E>) definition.defaultValue().getClass();
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(stored)) {
                control.setValue(constant);
                return;
            }
        }
        control.setValue(definition.defaultValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void saveControl(SettingDefinition<?> definition, SettingControl<?> control, SettingsContext context) {
        SettingScope scope = definition.scope();
        SettingsStorage storage = context.storage();
        SecretStore secretStore = context.secretStore();
        String key = definition.key();
        Object value = control.getValue();

        if (definition.secret() || definition.type() == SettingType.SECRET) {
            // Only update the secret if the user entered a new non-empty value.
            // An empty field means "no change", not "clear the secret".
            String secretValue = value == null ? "" : String.valueOf(value);
            if (!secretValue.isEmpty()) {
                secretStore.setSecret(key, secretValue);
            }
            return;
        }

        switch (definition.type()) {
            case BOOLEAN -> storage.putBoolean(scope, key, Boolean.TRUE.equals(value));
            case STRING, FONT, FILE_PATH, DIRECTORY_PATH, COLOR ->
                storage.putString(scope, key, value == null ? "" : String.valueOf(value));
            case INTEGER -> storage.putInt(scope, key, value == null ? 0 : ((Number) value).intValue());
            case DOUBLE -> storage.putDouble(scope, key, value == null ? 0.0 : ((Number) value).doubleValue());
            case ENUM -> storage.putString(scope, key, value == null ? "" : ((Enum) value).name().toLowerCase());
            default -> {}
        }
    }
}
