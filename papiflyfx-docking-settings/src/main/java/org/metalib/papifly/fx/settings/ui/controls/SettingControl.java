package org.metalib.papifly.fx.settings.ui.controls;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.ValidationResult;

public abstract class SettingControl<T> extends VBox {

    private final SettingDefinition<T> definition;
    private final Label titleLabel;
    private final Label descriptionLabel;
    private final Label validationLabel;
    private Runnable changeListener;
    private ValidationResult validationResult = ValidationResult.OK;

    protected SettingControl(SettingDefinition<T> definition) {
        super(4);
        this.definition = definition;
        this.titleLabel = new Label(definition.label());
        this.descriptionLabel = new Label(definition.description());
        this.validationLabel = new Label();

        getStyleClass().add("pf-settings-control");
        titleLabel.getStyleClass().add("pf-settings-control-title");
        descriptionLabel.getStyleClass().add("pf-settings-control-description");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setManaged(!definition.description().isBlank());
        descriptionLabel.setVisible(!definition.description().isBlank());
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);

        getChildren().addAll(titleLabel, descriptionLabel, validationLabel);
        setFillWidth(true);
        setMaxWidth(Double.MAX_VALUE);
    }

    protected final void setEditor(Node editor) {
        VBox.setVgrow(editor, Priority.NEVER);
        getChildren().add(2, editor);
    }

    protected final void onValueChanged() {
        validationResult = validateCurrentValue();
        if (validationResult.level() == ValidationResult.Level.OK || validationResult.message().isBlank()) {
            validationLabel.setText("");
            validationLabel.setManaged(false);
            validationLabel.setVisible(false);
            validationLabel.getStyleClass().removeAll("pf-settings-control-validation-error", "pf-settings-control-validation-warning");
        } else {
            validationLabel.setText(validationResult.message());
            validationLabel.setManaged(true);
            validationLabel.setVisible(true);
            validationLabel.getStyleClass().removeAll("pf-settings-control-validation-error", "pf-settings-control-validation-warning");
            validationLabel.getStyleClass().add(validationResult.level() == ValidationResult.Level.ERROR
                ? "pf-settings-control-validation-error"
                : "pf-settings-control-validation-warning");
        }
        if (changeListener != null) {
            changeListener.run();
        }
    }

    public SettingDefinition<T> definition() {
        return definition;
    }

    public void setOnChange(Runnable changeListener) {
        this.changeListener = changeListener;
    }

    public ValidationResult validationResult() {
        return validationResult;
    }

    public ValidationResult validateCurrentValue() {
        if (definition.validator() == null) {
            return ValidationResult.OK;
        }
        return definition.validator().validate(getValue());
    }

    public abstract T getValue();

    public abstract void setValue(T value);
}
