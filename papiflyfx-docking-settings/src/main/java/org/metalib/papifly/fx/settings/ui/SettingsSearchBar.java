package org.metalib.papifly.fx.settings.ui;

import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class SettingsSearchBar extends HBox {

    private final TextField searchField;

    public SettingsSearchBar() {
        getStyleClass().add("pf-settings-search-bar");
        this.searchField = SettingsUiStyles.applyCompactField(new TextField());
        SettingsUiStyles.apply(searchField, "pf-settings-search-field");
        this.searchField.setPromptText("Search settings...");
        getChildren().add(searchField);
    }

    public TextField getSearchField() {
        return searchField;
    }
}
