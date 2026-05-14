package org.metalib.papifly.fx.settings.ui;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsCategoryDefinitions;
import org.metalib.papifly.fx.settings.api.SettingsCategoryMetadata;

import java.util.List;

public class SettingsCategoryList extends ListView<SettingsCategory> {

    private final ObservableList<SettingsCategory> categories = FXCollections.observableArrayList();
    private final FilteredList<SettingsCategory> filteredCategories = new FilteredList<>(categories, item -> true);

    public SettingsCategoryList() {
        SettingsUiStyles.apply(this, "pf-settings-category-list");
        setItems(filteredCategories);
        setPrefWidth(220);
        setMinWidth(180);
        setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(SettingsCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label label = SettingsUiStyles.apply(new Label(displayName(item)), SettingsUiStyles.CATEGORY_LABEL);
                HBox row = SettingsUiStyles.apply(new HBox(8), SettingsUiStyles.CATEGORY_ROW);
                Node icon = icon(item);
                if (icon != null) {
                    row.getChildren().add(icon);
                }
                row.getChildren().add(label);
                row.setPadding(new Insets(4, 8, 4, 8));
                HBox.setHgrow(label, Priority.ALWAYS);
                setGraphic(row);
                setText(null);
            }
        });
    }

    public void setCategories(List<SettingsCategory> items) {
        categories.setAll(items);
    }

    public void filter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        filteredCategories.setPredicate(category -> {
            if (normalized.isEmpty()) {
                return true;
            }
            return displayName(category).toLowerCase().contains(normalized)
                || id(category).toLowerCase().contains(normalized)
                || definitions(category).stream().anyMatch(definition ->
                    definition.label().toLowerCase().contains(normalized)
                        || definition.description().toLowerCase().contains(normalized)
                        || definition.key().toLowerCase().contains(normalized)
                );
        });
    }

    public ReadOnlyObjectProperty<SettingsCategory> selectedCategoryProperty() {
        return getSelectionModel().selectedItemProperty();
    }

    public void selectById(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        for (SettingsCategory category : filteredCategories) {
            if (id.equals(category.id())) {
                getSelectionModel().select(category);
                scrollTo(category);
                return;
            }
        }
    }

    public List<String> visibleCategoryIds() {
        return filteredCategories.stream().map(this::id).toList();
    }

    private String id(SettingsCategoryMetadata category) {
        return category.id();
    }

    private String displayName(SettingsCategoryMetadata category) {
        return category.displayName();
    }

    private Node icon(SettingsCategoryMetadata category) {
        return category.icon();
    }

    private List<org.metalib.papifly.fx.settings.api.SettingDefinition<?>> definitions(SettingsCategoryDefinitions category) {
        return category.definitions();
    }
}
