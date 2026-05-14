package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.ui.SettingsUiStyles;

public class SecretSettingControl extends SettingControl<String> {

    private final PasswordField passwordField;
    private final TextField plainTextField;
    private final Button revealButton;
    private final Button clearButton;
    private boolean revealed;

    public SecretSettingControl(SettingDefinition<String> definition) {
        super(definition);
        this.passwordField = SettingsUiStyles.applyCompactField(new PasswordField());
        this.plainTextField = SettingsUiStyles.applyCompactField(new TextField());
        this.revealButton = SettingsUiStyles.applySecondaryActionButton(new Button("Reveal"));
        this.clearButton = SettingsUiStyles.applySecondaryActionButton(new Button("Clear"));

        plainTextField.setManaged(false);
        plainTextField.setVisible(false);

        passwordField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!revealed) {
                plainTextField.setText(newValue);
                onValueChanged();
            }
        });
        plainTextField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (revealed) {
                passwordField.setText(newValue);
                onValueChanged();
            }
        });

        revealButton.setOnAction(event -> toggleReveal());
        clearButton.setOnAction(event -> setValue(""));
        setEditor(new HBox(8, passwordField, plainTextField, revealButton, clearButton));
    }

    @Override
    public String getValue() {
        return revealed ? plainTextField.getText() : passwordField.getText();
    }

    @Override
    public void setValue(String value) {
        String safe = value == null ? "" : value;
        passwordField.setText(safe);
        plainTextField.setText(safe);
        onValueChanged();
    }

    private void toggleReveal() {
        revealed = !revealed;
        revealButton.setText(revealed ? "Hide" : "Reveal");
        plainTextField.setManaged(revealed);
        plainTextField.setVisible(revealed);
        passwordField.setManaged(!revealed);
        passwordField.setVisible(!revealed);
        onValueChanged();
    }
}
