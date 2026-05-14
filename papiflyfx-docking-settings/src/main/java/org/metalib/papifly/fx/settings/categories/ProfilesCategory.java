package org.metalib.papifly.fx.settings.categories;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.internal.SettingsJsonCodec;
import org.metalib.papifly.fx.settings.persist.JsonSettingsStorage;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

import java.util.Map;
import java.util.Set;

public class ProfilesCategory implements SettingsCategory {

    private final SettingsJsonCodec codec = new SettingsJsonCodec();
    private TextArea payloadArea;
    private Label statusLabel;

    @Override
    public String id() {
        return "profiles";
    }

    @Override
    public String displayName() {
        return "Profiles";
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public Set<SettingScope> supportedScopes() {
        return Set.of(SettingScope.APPLICATION, SettingScope.WORKSPACE);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (payloadArea == null) {
            payloadArea = SettingsUiStyles.applyTextArea(new TextArea());
            payloadArea.setPrefRowCount(18);
            statusLabel = new Label();
            statusLabel.setWrapText(true);
            statusLabel.getStyleClass().add("pf-settings-control-description");

            Button exportApplication = SettingsUiStyles.applySecondaryActionButton(new Button("Export Application"));
            exportApplication.setOnAction(event -> exportScope(context, SettingScope.APPLICATION));

            Button exportWorkspace = SettingsUiStyles.applySecondaryActionButton(new Button("Export Workspace"));
            exportWorkspace.setOnAction(event -> exportScope(context, SettingScope.WORKSPACE));

            Button importApplication = SettingsUiStyles.applyActionButton(new Button("Import Application"));
            importApplication.setOnAction(event -> importScope(context, SettingScope.APPLICATION));

            Button importWorkspace = SettingsUiStyles.applyActionButton(new Button("Import Workspace"));
            importWorkspace.setOnAction(event -> importScope(context, SettingScope.WORKSPACE));

            HBox buttons = new HBox(8, exportApplication, exportWorkspace, importApplication, importWorkspace);
            VBox box = new VBox(12, buttons, payloadArea, statusLabel);
            box.setPadding(new Insets(8));
            VBox.setVgrow(payloadArea, Priority.ALWAYS);
            return box;
        }
        return (Node) payloadArea.getParent();
    }

    @Override
    public void apply(SettingsContext context) {
    }

    @Override
    public void reset(SettingsContext context) {
        statusLabel.setText("");
    }

    private void exportScope(SettingsContext context, SettingScope scope) {
        if (!(context.storage() instanceof JsonSettingsStorage storage)) {
            statusLabel.setText("Export is available only for JsonSettingsStorage.");
            return;
        }
        payloadArea.setText(codec.toJson(storage.snapshot(scope)));
        statusLabel.setText("Exported " + scope.name().toLowerCase() + " settings.");
    }

    private void importScope(SettingsContext context, SettingScope scope) {
        if (!(context.storage() instanceof JsonSettingsStorage storage)) {
            statusLabel.setText("Import is available only for JsonSettingsStorage.");
            return;
        }
        try {
            Map<String, Object> imported = codec.fromJson(payloadArea.getText());
            storage.replaceScope(scope, imported);
            storage.save();
            storage.reload();
            statusLabel.setText("Imported " + scope.name().toLowerCase() + " settings.");
        } catch (RuntimeException exception) {
            statusLabel.setText("Import failed: " + exception.getMessage());
        }
    }
}
