package org.metalib.papifly.fx.settings.categories;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SecretKeyNames;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class McpServersCategory implements SettingsCategory {

    private static final SettingDefinition<String> SERVERS_DEFINITION = SettingDefinition
        .of("mcp.servers", "MCP Servers", SettingType.CUSTOM, "servers")
        .withScope(SettingScope.WORKSPACE)
        .withDescription("Manage trusted MCP server definitions for the current workspace.");

    private BorderPane pane;
    private ListView<String> serverList;
    private TextField nameField;
    private TextField transportField;
    private TextField endpointField;
    private CheckBox trustedField;
    private PasswordField authTokenField;
    private boolean dirty;

    @Override
    public String id() {
        return "mcp-servers";
    }

    @Override
    public String displayName() {
        return "MCP Servers";
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(SERVERS_DEFINITION);
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            serverList = SettingsUiStyles.applyList(new ListView<>());
            nameField = SettingsUiStyles.applyCompactField(new TextField());
            transportField = SettingsUiStyles.applyCompactField(new TextField());
            endpointField = SettingsUiStyles.applyCompactField(new TextField());
            trustedField = SettingsUiStyles.applyCheckBox(new CheckBox("Trusted"));
            authTokenField = SettingsUiStyles.applyCompactField(new PasswordField());

            nameField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            transportField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            endpointField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            trustedField.selectedProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            authTokenField.textProperty().addListener((obs, oldValue, newValue) -> dirty = true);
            serverList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> loadServer(context, newValue));

            Button newButton = SettingsUiStyles.applySecondaryActionButton(new Button("New"));
            newButton.setOnAction(event -> {
                serverList.getSelectionModel().clearSelection();
                nameField.clear();
                transportField.clear();
                endpointField.clear();
                trustedField.setSelected(false);
                authTokenField.clear();
                dirty = false;
            });

            Button saveButton = SettingsUiStyles.applyActionButton(new Button("Save"));
            saveButton.setOnAction(event -> saveServer(context));

            Button deleteButton = SettingsUiStyles.applySecondaryActionButton(new Button("Delete"));
            deleteButton.setOnAction(event -> deleteServer(context));

            VBox form = new VBox(
                12,
                field("Name", nameField),
                field("Transport", transportField),
                field("Endpoint / Command", endpointField),
                trustedField,
                field("Auth Token", authTokenField),
                new HBox(8, newButton, saveButton, deleteButton)
            );
            form.setPadding(new Insets(0, 0, 0, 12));

            pane = new BorderPane();
            pane.setLeft(serverList);
            pane.setCenter(form);
            serverList.setPrefWidth(220);
        }
        refreshServerList(context);
        dirty = false;
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        saveServer(context);
    }

    @Override
    public void reset(SettingsContext context) {
        refreshServerList(context);
        String selected = serverList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            loadServer(context, selected);
        }
        dirty = false;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void refreshServerList(SettingsContext context) {
        Map<String, Object> servers = context.storage().getMap(SettingScope.WORKSPACE, "mcp.servers");
        serverList.getItems().setAll(servers.keySet().stream().sorted().toList());
    }

    @SuppressWarnings("unchecked")
    private void loadServer(SettingsContext context, String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return;
        }
        Map<String, Object> servers = context.storage().getMap(SettingScope.WORKSPACE, "mcp.servers");
        Object raw = servers.get(serverName);
        if (!(raw instanceof Map<?, ?> server)) {
            return;
        }
        nameField.setText(serverName);
        transportField.setText(stringValue(server, "transport"));
        endpointField.setText(stringValue(server, "endpoint"));
        trustedField.setSelected(booleanValue(server, "trusted"));
        authTokenField.setText(context.secretStore().getSecret(SecretKeyNames.mcpAuthToken(serverName)).orElse(""));
        dirty = false;
    }

    private void saveServer(SettingsContext context) {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        Map<String, Object> servers = new LinkedHashMap<>(context.storage().getMap(SettingScope.WORKSPACE, "mcp.servers"));
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("transport", transportField.getText());
        server.put("endpoint", endpointField.getText());
        server.put("trusted", trustedField.isSelected());
        servers.put(name, server);
        context.storage().putMap(SettingScope.WORKSPACE, "mcp.servers", servers);
        context.secretStore().setSecret(SecretKeyNames.mcpAuthToken(name), authTokenField.getText());
        context.storage().save();
        refreshServerList(context);
        serverList.getSelectionModel().select(name);
        dirty = false;
    }

    private void deleteServer(SettingsContext context) {
        String selected = serverList.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBlank()) {
            return;
        }
        Map<String, Object> servers = new LinkedHashMap<>(context.storage().getMap(SettingScope.WORKSPACE, "mcp.servers"));
        servers.remove(selected);
        context.storage().putMap(SettingScope.WORKSPACE, "mcp.servers", servers);
        context.secretStore().clearSecret(SecretKeyNames.mcpAuthToken(selected));
        context.storage().save();
        refreshServerList(context);
        nameField.clear();
        transportField.clear();
        endpointField.clear();
        trustedField.setSelected(false);
        authTokenField.clear();
        dirty = false;
    }

    private VBox field(String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("pf-settings-control-title");
        return new VBox(4, label, field);
    }

    private String stringValue(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private boolean booleanValue(Map<?, ?> values, String key) {
        Object value = values.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
