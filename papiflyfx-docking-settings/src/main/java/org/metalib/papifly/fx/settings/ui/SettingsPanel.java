package org.metalib.papifly.fx.settings.ui;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.paint.Color;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsCategoryMetadata;
import org.metalib.papifly.fx.settings.api.SettingsCategoryUI;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.api.SettingsContributor;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.ui.UiCommonPalette;
import org.metalib.papifly.fx.ui.UiCommonStyles;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class SettingsPanel extends BorderPane implements DisposableContent {

    public static final String SETTINGS_STYLESHEET = "/org/metalib/papifly/fx/settings/ui/settings.css";

    private final SettingsRuntime runtime;
    private final SettingsSearchBar searchBar;
    private final SettingsCategoryList categoryList;
    private final ScrollPane contentScroll;
    private final VBox contentArea;
    private final SettingsToolbar toolbar;
    private final Map<String, SettingsCategory> categoriesById = new LinkedHashMap<>();
    private final Map<String, Node> paneCache = new LinkedHashMap<>();
    private final List<SettingsCategory> providedCategories;
    private final ChangeListener<Theme> themeListener;
    private final ChangeListener<Boolean> dirtyBindingListener = (obs, oldValue, newValue) -> refreshToolbarState();

    private SettingsCategory activeCategory;
    private ReadOnlyBooleanProperty activeDirtyProperty;
    private String initialCategoryId;
    private boolean suppressScopeRefresh;

    public SettingsPanel(SettingsRuntime runtime) {
        this(runtime, null, null);
    }

    public SettingsPanel(SettingsRuntime runtime, String initialCategoryId) {
        this(runtime, initialCategoryId, null);
    }

    SettingsPanel(SettingsRuntime runtime, String initialCategoryId, List<SettingsCategory> providedCategories) {
        this.runtime = runtime;
        this.initialCategoryId = initialCategoryId;
        this.providedCategories = providedCategories == null ? null : List.copyOf(providedCategories);

        UiCommonStyles.ensureLoaded(this);
        URL settingsUrl = getClass().getResource(SETTINGS_STYLESHEET);
        if (settingsUrl != null) {
            getStylesheets().add(settingsUrl.toExternalForm());
        }
        getStyleClass().add("pf-settings-panel");

        this.searchBar = new SettingsSearchBar();
        this.categoryList = new SettingsCategoryList();
        this.contentArea = new VBox();
        this.contentScroll = new ScrollPane(contentArea);
        this.toolbar = new SettingsToolbar();

        contentArea.getStyleClass().add("pf-settings-content-area");
        contentScroll.getStyleClass().add("pf-settings-content-scroll");

        VBox.setVgrow(contentScroll, Priority.ALWAYS);
        contentScroll.setFitToWidth(true);
        contentScroll.setFitToHeight(true);
        contentScroll.setContent(contentArea);

        setTop(searchBar);
        setLeft(categoryList);
        setCenter(contentScroll);
        setBottom(toolbar);

        searchBar.getSearchField().textProperty().addListener((obs, oldValue, newValue) -> {
            categoryList.filter(newValue);
            ensureSelectedCategoryVisible();
        });
        categoryList.selectedCategoryProperty().addListener((obs, oldValue, newValue) -> showCategory(newValue));
        toolbar.onApply(this::applyActiveCategory);
        toolbar.onReset(this::resetActiveCategory);
        toolbar.activeScopeProperty().addListener((obs, oldValue, newValue) -> onScopeChanged(newValue));

        themeListener = (obs, oldTheme, newTheme) -> applyThemeTokens(newTheme);
        runtime.themeProperty().addListener(themeListener);
        applyThemeTokens(runtime.themeProperty().get());

        loadCategories();
    }

    public void applyActiveCategory() {
        if (activeCategory == null) {
            return;
        }
        activeCategory.apply(currentContext());
        refreshToolbarState();
    }

    public void resetActiveCategory() {
        if (activeCategory == null) {
            return;
        }
        activeCategory.reset(currentContext());
        refreshToolbarState();
    }

    public String getActiveCategoryId() {
        return activeCategory == null ? null : activeCategory.id();
    }

    public void selectCategory(String categoryId) {
        categoryList.selectById(categoryId);
    }

    public SettingsSearchBar searchBar() {
        return searchBar;
    }

    public List<String> visibleCategoryIds() {
        return categoryList.visibleCategoryIds();
    }

    SettingsToolbar toolbar() {
        return toolbar;
    }

    VBox contentArea() {
        return contentArea;
    }

    @Override
    public void dispose() {
        runtime.themeProperty().removeListener(themeListener);
        unbindDirtyProperty();
        runtime.storage().save();
    }

    private void applyThemeTokens(Theme theme) {
        UiCommonPalette palette = buildPalette(theme);
        setStyle(UiCommonThemeSupport.themeVariables(palette) + UiStyleSupport.metricVariables());
    }

    private static UiCommonPalette buildPalette(Theme theme) {
        Theme resolved = UiCommonThemeSupport.resolvedTheme(theme);
        return new UiCommonPalette(
            UiCommonThemeSupport.headerBackground(resolved),
            UiCommonThemeSupport.border(resolved),
            UiCommonThemeSupport.textPrimary(resolved),
            UiCommonThemeSupport.alpha(UiCommonThemeSupport.textPrimary(resolved), 0.66),
            UiCommonThemeSupport.alpha(UiCommonThemeSupport.textPrimary(resolved), 0.50),
            UiCommonThemeSupport.background(resolved),
            UiCommonThemeSupport.hover(resolved),
            UiCommonThemeSupport.pressed(resolved),
            UiCommonThemeSupport.accent(resolved),
            UiCommonThemeSupport.accent(resolved),
            UiCommonThemeSupport.success(resolved),
            UiCommonThemeSupport.warning(resolved),
            UiCommonThemeSupport.danger(resolved),
            UiCommonThemeSupport.dropHint(resolved),
            UiCommonThemeSupport.alpha(Color.BLACK, 0.25)
        );
    }

    private void loadCategories() {
        List<SettingsCategory> categories = discoverCategories();
        categories.sort(Comparator.comparingInt(SettingsCategoryMetadata::order).thenComparing(SettingsCategoryMetadata::displayName));
        categoriesById.clear();
        for (SettingsCategory category : categories) {
            categoriesById.put(category.id(), category);
        }
        categoryList.setCategories(categories);
        if (initialCategoryId != null && categoriesById.containsKey(initialCategoryId)) {
            categoryList.selectById(initialCategoryId);
            initialCategoryId = null;
        } else if (!categories.isEmpty()) {
            categoryList.getSelectionModel().select(categories.getFirst());
        }
    }

    private List<SettingsCategory> discoverCategories() {
        if (providedCategories != null) {
            return new ArrayList<>(providedCategories);
        }
        Map<String, SettingsCategory> loaded = new LinkedHashMap<>();
        ServiceLoader.load(SettingsCategory.class).forEach(category -> loaded.put(category.id(), category));
        ServiceLoader.load(SettingsContributor.class).forEach(contributor ->
            contributor.getCategories().forEach(category -> loaded.put(category.id(), category))
        );
        return new ArrayList<>(loaded.values());
    }

    private void ensureSelectedCategoryVisible() {
        if (activeCategory != null && visibleCategoryIds().contains(activeCategory.id())) {
            return;
        }
        List<String> visibleIds = visibleCategoryIds();
        if (!visibleIds.isEmpty()) {
            categoryList.selectById(visibleIds.getFirst());
        } else {
            contentArea.getChildren().clear();
            activeCategory = null;
        }
    }

    private void showCategory(SettingsCategory category) {
        if (category == null) {
            unbindDirtyProperty();
            contentArea.getChildren().clear();
            activeCategory = null;
            refreshToolbarState();
            return;
        }
        SettingScope previousScope = toolbar.getActiveScope();
        normalizeSupportedScopes(category);

        unbindDirtyProperty();
        activeCategory = category;
        if (toolbar.getActiveScope() != previousScope) {
            paneCache.clear();
        }
        displayActiveCategory(category);
    }

    private void bindDirtyProperty(SettingsCategoryUI category) {
        ReadOnlyBooleanProperty dp = category.dirtyProperty();
        if (dp != null) {
            activeDirtyProperty = dp;
            dp.addListener(dirtyBindingListener);
        }
    }

    private void unbindDirtyProperty() {
        if (activeDirtyProperty != null) {
            activeDirtyProperty.removeListener(dirtyBindingListener);
            activeDirtyProperty = null;
        }
    }

    private void onScopeChanged(SettingScope scope) {
        if (suppressScopeRefresh) {
            return;
        }
        paneCache.clear();
        if (activeCategory != null) {
            unbindDirtyProperty();
            displayActiveCategory(activeCategory);
            return;
        }
        refreshToolbarState();
    }

    private SettingsContext currentContext() {
        return runtime.context(toolbar.getActiveScope());
    }

    private void refreshToolbarState() {
        toolbar.setDirty(activeCategory != null && isDirty(activeCategory));
        toolbar.setActions(activeCategory == null ? List.of() : activeCategory.actions(), this::currentContext);
    }

    private void normalizeSupportedScopes(SettingsCategory category) {
        suppressScopeRefresh = true;
        try {
            toolbar.setSupportedScopes(category.supportedScopes());
        } finally {
            suppressScopeRefresh = false;
        }
    }

    private void displayActiveCategory(SettingsCategory category) {
        Node pane = paneCache.computeIfAbsent(category.id(), ignored -> buildPane(category));
        contentArea.getChildren().setAll(pane);
        bindDirtyProperty(category);
        refreshToolbarState();
    }

    private Node buildPane(SettingsCategoryUI category) {
        return category.buildSettingsPane(currentContext());
    }

    private boolean isDirty(SettingsCategoryUI category) {
        return category.isDirty();
    }
}
