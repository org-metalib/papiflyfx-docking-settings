package org.metalib.papifly.fx.settings.categories;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeyboardShortcutsCategory implements SettingsCategory {

    private static final List<SettingDefinition<?>> DEFINITIONS = List.of(
        SettingDefinition.of("keyboard.shortcuts", "Keyboard Shortcuts", SettingType.CUSTOM, "shortcuts")
            .withDescription("Edit common shortcuts and detect duplicate assignments.")
    );

    private final Map<String, TextField> shortcutFields = new LinkedHashMap<>();
    private VBox pane;
    private Label conflictLabel;
    private boolean dirty;

    @Override
    public String id() {
        return "keyboard-shortcuts";
    }

    @Override
    public String displayName() {
        return "Keyboard Shortcuts";
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return DEFINITIONS;
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            conflictLabel = new Label();
            conflictLabel.getStyleClass().add("pf-settings-control-validation-warning");
            GridPane grid = new GridPane();
            grid.setVgap(8);
            grid.setHgap(12);

            addShortcutRow(grid, 0, "editor.search", "Editor Search");
            addShortcutRow(grid, 1, "editor.replace", "Editor Replace");
            addShortcutRow(grid, 2, "workspace.saveSession", "Save Session");
            addShortcutRow(grid, 3, "dock.toggleMaximize", "Toggle Maximize");

            pane = new VBox(12, grid, conflictLabel);
            pane.setPadding(new Insets(8));
        }
        reset(context);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        Map<String, Object> shortcuts = new LinkedHashMap<>();
        shortcutFields.forEach((actionId, field) -> shortcuts.put(actionId, field.getText()));
        context.storage().putMap(SettingScope.APPLICATION, "keyboard.shortcuts", shortcuts);
        context.storage().save();
        dirty = false;
        updateConflicts();
    }

    @Override
    public void reset(SettingsContext context) {
        Map<String, Object> shortcuts = context.storage().getMap(SettingScope.APPLICATION, "keyboard.shortcuts");
        shortcutFields.forEach((actionId, field) -> field.setText(String.valueOf(shortcuts.getOrDefault(actionId, defaultShortcut(actionId)))));
        dirty = false;
        updateConflicts();
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void addShortcutRow(GridPane grid, int row, String actionId, String labelText) {
        Label label = new Label(labelText);
        TextField field = SettingsUiStyles.applyCompactField(new TextField());
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            dirty = true;
            updateConflicts();
        });
        shortcutFields.put(actionId, field);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private void updateConflicts() {
        Map<String, String> reverse = new LinkedHashMap<>();
        for (Map.Entry<String, TextField> entry : shortcutFields.entrySet()) {
            String shortcut = entry.getValue().getText();
            if (shortcut == null || shortcut.isBlank()) {
                continue;
            }
            if (reverse.containsKey(shortcut)) {
                conflictLabel.setText("Conflict: " + shortcut + " is assigned to both " + reverse.get(shortcut) + " and " + entry.getKey() + ".");
                return;
            }
            reverse.put(shortcut, entry.getKey());
        }
        conflictLabel.setText("");
    }

    private String defaultShortcut(String actionId) {
        return switch (actionId) {
            case "editor.search" -> "Cmd+F";
            case "editor.replace" -> "Cmd+Alt+F";
            case "workspace.saveSession" -> "Cmd+Shift+S";
            case "dock.toggleMaximize" -> "Cmd+Enter";
            default -> "";
        };
    }
}
