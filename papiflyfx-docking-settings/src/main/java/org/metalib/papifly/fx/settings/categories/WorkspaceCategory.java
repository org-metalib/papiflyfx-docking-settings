package org.metalib.papifly.fx.settings.categories;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.DefinitionFormBinder;

import java.util.List;

public class WorkspaceCategory implements SettingsCategory {

    private enum LayoutPreset {
        IDE,
        DOCUMENT,
        REVIEW
    }

    private static final SettingDefinition<Boolean> RESTORE_DEFINITION = SettingDefinition
        .of("workspace.restoreOnStartup", "Restore On Startup", SettingType.BOOLEAN, true)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Restores the last saved workspace layout on startup.");
    private static final SettingDefinition<Boolean> ANIMATION_DEFINITION = SettingDefinition
        .of("workspace.animations", "Enable Animations", SettingType.BOOLEAN, true)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Enables docking transitions and panel animations.");
    private static final SettingDefinition<LayoutPreset> PRESET_DEFINITION = SettingDefinition
        .of("workspace.layoutPreset", "Layout Preset", SettingType.ENUM, LayoutPreset.IDE)
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Selects the preferred workspace layout preset.");

    private DefinitionFormBinder binder;

    @Override
    public String id() {
        return "workspace";
    }

    @Override
    public String displayName() {
        return "Workspace";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(RESTORE_DEFINITION, ANIMATION_DEFINITION, PRESET_DEFINITION);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (binder == null) {
            binder = new DefinitionFormBinder(definitions());
        }
        binder.load(context);
        return binder.pane();
    }

    @Override
    public void apply(SettingsContext context) {
        binder.save(context);
        context.storage().save();
    }

    @Override
    public void reset(SettingsContext context) {
        binder.load(context);
    }

    @Override
    public boolean isDirty() {
        return binder != null && binder.isDirty();
    }

    @Override
    public ReadOnlyBooleanProperty dirtyProperty() {
        return binder == null ? null : binder.dirtyProperty();
    }
}
