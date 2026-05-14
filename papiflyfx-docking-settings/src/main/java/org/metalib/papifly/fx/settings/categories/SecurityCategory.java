package org.metalib.papifly.fx.settings.categories;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.secret.EncryptedFileSecretStore;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

import java.util.List;
import java.util.Set;

/**
 * Secret administration category.
 *
 * <p>This category never reloads stored secret values into the UI.
 * It shows whether each key is "Set" or "Not Set" and provides
 * lifecycle actions: <b>Replace</b> (set a new value) and <b>Clear</b>
 * (delete the stored value).
 */
public class SecurityCategory implements SettingsCategory {

    private BorderPane pane;
    private ListView<String> keysView;
    private TextField keyField;
    private PasswordField newValueField;
    private Label statusLabel;
    private Label backendLabel;
    private Label warningLabel;
    private Button replaceButton;
    private Button clearButton;
    private Button newButton;
    private final ReadOnlyBooleanWrapper dirty = new ReadOnlyBooleanWrapper(false);

    @Override
    public String id() {
        return "security";
    }

    @Override
    public String displayName() {
        return "Security";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of();
    }

    @Override
    public Set<SettingScope> supportedScopes() {
        return Set.of(SettingScope.APPLICATION);
    }

    @Override
    public ReadOnlyBooleanProperty dirtyProperty() {
        return dirty.getReadOnlyProperty();
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (pane == null) {
            keysView = SettingsUiStyles.applyList(new ListView<>());
            keyField = SettingsUiStyles.applyCompactField(new TextField());
            keyField.setPromptText("secret:key:name");
            newValueField = SettingsUiStyles.applyCompactField(new PasswordField());
            newValueField.setPromptText("Enter new secret value");
            statusLabel = new Label();
            backendLabel = new Label();
            backendLabel.getStyleClass().add("pf-settings-control-description");
            warningLabel = new Label();
            warningLabel.getStyleClass().add("pf-settings-control-validation-warning");
            warningLabel.setWrapText(true);

            keyField.textProperty().addListener((obs, oldValue, newValue) -> dirty.set(true));
            newValueField.textProperty().addListener((obs, oldValue, newValue) -> dirty.set(true));
            keysView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldValue, newValue) -> showKeyStatus(context, newValue));

            newButton = SettingsUiStyles.applySecondaryActionButton(new Button("New"));
            newButton.setOnAction(event -> {
                keysView.getSelectionModel().clearSelection();
                keyField.clear();
                newValueField.clear();
                statusLabel.setText("");
                dirty.set(false);
            });

            replaceButton = SettingsUiStyles.applyActionButton(new Button("Save Secret"));
            replaceButton.setOnAction(event -> {
                String key = keyField.getText() == null ? "" : keyField.getText().trim();
                String value = newValueField.getText();
                if (key.isEmpty() || value == null || value.isEmpty()) {
                    return;
                }
                context.secretStore().setSecret(key, value);
                newValueField.clear();
                refreshKeys(context);
                keysView.getSelectionModel().select(key);
                dirty.set(false);
            });

            clearButton = SettingsUiStyles.applySecondaryActionButton(new Button("Clear Secret"));
            clearButton.setOnAction(event -> {
                String key = keyField.getText() == null ? "" : keyField.getText().trim();
                if (key.isEmpty()) {
                    return;
                }
                context.secretStore().clearSecret(key);
                refreshKeys(context);
                keyField.clear();
                newValueField.clear();
                statusLabel.setText("");
                dirty.set(false);
            });

            HBox buttons = new HBox(8, newButton, replaceButton, clearButton);
            VBox form = new VBox(12, backendLabel, warningLabel, keyField, statusLabel, newValueField, buttons);
            form.setPadding(new Insets(0, 0, 0, 12));
            VBox.setVgrow(newValueField, Priority.NEVER);

            pane = new BorderPane();
            pane.setLeft(keysView);
            pane.setCenter(form);
            keysView.setPrefWidth(220);
        }
        SecretStore secretStore = context.secretStore();
        backendLabel.setText("Secret backend: " + secretStore.backendName());
        boolean usingFallback = secretStore instanceof EncryptedFileSecretStore;
        warningLabel.setText(usingFallback
            ? "Secrets are stored in the encrypted-file backend. OS keychain integration is not active."
            : "");
        refreshKeys(context);
        dirty.set(false);
        return pane;
    }

    @Override
    public void apply(SettingsContext context) {
        String key = keyField.getText() == null ? "" : keyField.getText().trim();
        String value = newValueField.getText();
        if (!key.isEmpty() && value != null && !value.isEmpty()) {
            context.secretStore().setSecret(key, value);
            newValueField.clear();
        }
        refreshKeys(context);
        dirty.set(false);
    }

    @Override
    public void reset(SettingsContext context) {
        refreshKeys(context);
        String selected = keysView == null ? null : keysView.getSelectionModel().getSelectedItem();
        showKeyStatus(context, selected);
        newValueField.clear();
        dirty.set(false);
    }

    @Override
    public boolean isDirty() {
        return dirty.get();
    }

    private void refreshKeys(SettingsContext context) {
        List<String> keys = context.secretStore().listKeys().stream().sorted().toList();
        keysView.getItems().setAll(keys);
    }

    /**
     * Shows whether the selected key has a stored value, without revealing it.
     */
    private void showKeyStatus(SettingsContext context, String key) {
        if (key == null || key.isBlank()) {
            statusLabel.setText("");
            statusLabel.getStyleClass().removeAll("pf-settings-status-label", "pf-settings-control-description");
            return;
        }
        keyField.setText(key);
        newValueField.clear();
        boolean hasValue = context.secretStore().hasSecret(key);
        statusLabel.setText(hasValue ? "Status: Set" : "Status: Not Set");
        statusLabel.getStyleClass().removeAll("pf-settings-status-label", "pf-settings-control-description");
        statusLabel.getStyleClass().add(hasValue ? "pf-settings-status-label" : "pf-settings-control-description");
        dirty.set(false);
    }
}
