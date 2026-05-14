package org.metalib.papifly.fx.settings.ui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Border;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.categories.KeyboardShortcutsCategory;
import org.metalib.papifly.fx.settings.categories.McpServersCategory;
import org.metalib.papifly.fx.settings.categories.SecurityCategory;
import org.metalib.papifly.fx.settings.persist.JsonSettingsStorage;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.secret.InMemorySecretStore;
import org.metalib.papifly.fx.settings.ui.controls.SettingControl;
import org.metalib.papifly.fx.settings.ui.controls.NumberSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.PathSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.SecretSettingControl;
import org.metalib.papifly.fx.settings.ui.controls.StringSettingControl;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class SettingsPanelFxTest {

    @TempDir
    Path tempDir;

    private SettingsRuntime runtime;
    private SettingsPanel shownPanel;

    @Start
    void start(Stage stage) {
        runtime = new SettingsRuntime(
            tempDir.resolve("app"),
            tempDir.resolve("workspace"),
            new JsonSettingsStorage(tempDir.resolve("app"), tempDir.resolve("workspace")),
            new InMemorySecretStore(),
            new SimpleObjectProperty<>(Theme.dark())
        );
        shownPanel = new SettingsPanel(runtime);
        stage.setScene(new Scene(shownPanel, 960, 720));
        stage.show();
    }

    @Test
    void searchFiltersVisibleCategories() {
        SettingsPanel panel = callFx(() -> new SettingsPanel(runtime));

        runFx(() -> panel.searchBar().getSearchField().setText("layout preset"));
        List<String> visible = callFx(panel::visibleCategoryIds);

        assertEquals(List.of("workspace"), visible);
    }

    @Test
    void applyAndResetUpdateStorageAndTheme() {
        SettingsPanel panel = callFx(() -> new SettingsPanel(runtime));

        runFx(() -> {
            panel.selectCategory("appearance");
            panel.searchBar().getSearchField().clear();
            panel.applyActiveCategory();
        });

        assertTrue(callFx(() ->
            runtime.storage().getString(SettingScope.APPLICATION, "appearance.theme", "dark").equals("dark")
        ));
    }

    @Test
    void lightModeAppearanceDefaultsSurviveLoadResetAndApplyWhenOverridesAreUnset() {
        SettingsRuntime lightRuntime = newRuntime("appearance");
        lightRuntime.storage().putString(SettingScope.APPLICATION, "appearance.theme", "light");
        lightRuntime.storage().putString(SettingScope.APPLICATION, "appearance.background", null);
        lightRuntime.storage().putString(SettingScope.APPLICATION, "appearance.border", null);

        SettingsPanel panel = callFx(() -> new SettingsPanel(lightRuntime));
        runFx(() -> panel.selectCategory("appearance"));

        ColorPicker backgroundPicker = colorPickerFor(panel, "appearance.background");
        ColorPicker borderPicker = colorPickerFor(panel, "appearance.border");
        Theme expectedTheme = Theme.light();

        assertColorEquals(UiCommonThemeSupport.background(expectedTheme), callFx(backgroundPicker::getValue));
        assertColorEquals(UiCommonThemeSupport.border(expectedTheme), callFx(borderPicker::getValue));

        runFx(() -> {
            backgroundPicker.setValue(Color.BLACK);
            borderPicker.setValue(Color.BLACK);
            panel.resetActiveCategory();
        });

        assertColorEquals(UiCommonThemeSupport.background(expectedTheme), callFx(backgroundPicker::getValue));
        assertColorEquals(UiCommonThemeSupport.border(expectedTheme), callFx(borderPicker::getValue));

        runFx(panel::applyActiveCategory);

        Theme appliedTheme = callFx(() -> lightRuntime.themeProperty().get());
        assertColorEquals(UiCommonThemeSupport.background(expectedTheme), UiCommonThemeSupport.background(appliedTheme));
        assertColorEquals(UiCommonThemeSupport.border(expectedTheme), UiCommonThemeSupport.border(appliedTheme));
    }

    @Test
    void selectedCategoryUsesExplicitInactiveTokensWhenListLosesFocus() {
        SettingsCategoryList list = callFx(this::categoryList);
        Theme theme = Theme.dark();
        Color expectedText = UiCommonThemeSupport.textPrimary(theme);
        Color expectedFocusedBackground = UiCommonThemeSupport.alpha(UiCommonThemeSupport.accent(theme), 0.16);
        Color expectedInactiveBackground = UiCommonThemeSupport.alpha(UiCommonThemeSupport.accent(theme), 0.10);

        runFx(() -> {
            shownPanel.selectCategory("appearance");
            list.requestFocus();
            shownPanel.applyCss();
            shownPanel.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callFx(list::isFocused));
        ListCell<?> focusedCell = selectedCategoryCell();
        Label focusedLabel = selectedCategoryLabel(focusedCell);
        assertColorEquals(expectedText, callFx(() -> requireColor(focusedLabel.getTextFill())));
        assertColorEquals(expectedFocusedBackground, callFx(() -> backgroundColor(focusedCell)));

        runFx(() -> {
            shownPanel.searchBar().getSearchField().requestFocus();
            shownPanel.applyCss();
            shownPanel.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(callFx(list::isFocused));
        assertTrue(callFx(() -> shownPanel.searchBar().getSearchField().isFocused()));
        ListCell<?> inactiveCell = selectedCategoryCell();
        Label inactiveLabel = selectedCategoryLabel(inactiveCell);
        assertColorEquals(expectedText, callFx(() -> requireColor(inactiveLabel.getTextFill())));
        assertColorEquals(expectedInactiveBackground, callFx(() -> backgroundColor(inactiveCell)));
    }

    @Test
    void switchingCategoriesWithDifferentScopesNormalizesOnceWithoutDuplicateDirtyBindings() {
        CountingBooleanProperty workspaceDirty = new CountingBooleanProperty();
        CountingBooleanProperty applicationDirty = new CountingBooleanProperty();
        TestCategory workspaceCategory = new TestCategory(
            "workspace-test",
            "Workspace Test",
            10,
            Set.of(SettingScope.APPLICATION, SettingScope.WORKSPACE),
            workspaceDirty
        );
        TestCategory applicationCategory = new TestCategory(
            "application-test",
            "Application Test",
            20,
            Set.of(SettingScope.APPLICATION),
            applicationDirty
        );
        SettingsPanel panel = callFx(() -> new SettingsPanel(
            newRuntime("scopes"),
            "workspace-test",
            List.of(workspaceCategory, applicationCategory)
        ));

        runFx(() -> panel.toolbar().activeScopeProperty().set(SettingScope.WORKSPACE));
        assertEquals(2, workspaceCategory.buildCount(), "Scope switch should rebuild the active category once");
        assertEquals(1, workspaceDirty.activeListenerCount(), "Active category should keep a single dirty listener");

        runFx(() -> panel.selectCategory("application-test"));

        assertEquals("application-test", callFx(panel::getActiveCategoryId));
        assertEquals(SettingScope.APPLICATION, callFx(() -> panel.toolbar().getActiveScope()));
        assertEquals(1, applicationCategory.buildCount(),
            "Category switch should render the application-only category exactly once");
        assertEquals(0, workspaceDirty.activeListenerCount(),
            "Previous category listener should be removed during the switch");
        assertEquals(1, applicationDirty.activeListenerCount(),
            "New category listener should be attached exactly once");
    }

    @Test
    void settingsEditorsReuseSharedCompactFieldStyleClass() {
        CompactFieldAudit audit = callFx(() -> {
            List<Node> roots = List.of(
                shownPanel.searchBar(),
                new StringSettingControl(SettingDefinition.of("test.string", "String", SettingType.STRING, "")),
                new NumberSettingControl<>(SettingDefinition.of("test.number", "Number", SettingType.INTEGER, 1)),
                new PathSettingControl(SettingDefinition.of("test.path", "Path", SettingType.FILE_PATH, "")),
                new SecretSettingControl(SettingDefinition.of("test.secret", "Secret", SettingType.SECRET, "")),
                new KeyboardShortcutsCategory().buildSettingsPane(runtime.context(SettingScope.APPLICATION)),
                new McpServersCategory().buildSettingsPane(runtime.context(SettingScope.WORKSPACE)),
                new SecurityCategory().buildSettingsPane(runtime.context(SettingScope.APPLICATION))
            );

            List<TextInputControl> fields = new ArrayList<>();
            for (Node root : roots) {
                fields.addAll(collectNodes(root, TextInputControl.class));
            }
            List<String> missing = fields.stream()
                .filter(field -> !field.getStyleClass().contains(SettingsUiStyles.COMPACT_FIELD))
                .map(this::describeField)
                .toList();
            return new CompactFieldAudit(fields.size(), missing);
        });

        assertTrue(audit.fieldCount() >= 12, () -> "Expected multiple compact fields but found " + audit.fieldCount());
        assertTrue(audit.missing().isEmpty(), () -> "Inputs missing compact field styling: " + audit.missing());
    }

    @Test
    void colorPickerPopupUsesTokenDrivenDarkThemeSurface() {
        Theme theme = Theme.dark();
        Color expectedBackground = UiCommonThemeSupport.headerBackground(theme);
        Color expectedBorder = UiCommonThemeSupport.border(theme);
        Color expectedAccent = UiCommonThemeSupport.accent(theme);

        runFx(() -> {
            shownPanel.selectCategory("appearance");
            shownPanel.applyCss();
            shownPanel.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        ColorPicker colorPicker = appearanceColorPicker();
        assertTrue(callFx(() -> colorPicker.getStyleClass().contains(SettingsUiStyles.COMPACT_FIELD)));
        assertTrue(callFx(() -> colorPicker.getStyleClass().contains(SettingsUiStyles.COMBO_BOX)));

        runFx(() -> {
            colorPicker.show();
            shownPanel.applyCss();
            shownPanel.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Region palette = colorPalettePopup();
        Hyperlink customColorLink = popupHyperlink(palette);
        assertColorEquals(expectedBackground, callFx(() -> backgroundColor(palette)));
        assertColorEquals(expectedBorder, callFx(() -> borderColor(palette)));
        assertColorEquals(expectedAccent, callFx(() -> requireColor(customColorLink.getTextFill())));

        runFx(colorPicker::hide);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void searchFieldUsesSurfaceColorInsteadOfBorderColor() {
        Theme theme = Theme.dark();

        runFx(() -> {
            shownPanel.applyCss();
            shownPanel.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Region searchField = callFx(() -> shownPanel.searchBar().getSearchField());
        Color actual = callFx(() -> backgroundColor(searchField));

        assertColorEquals(UiCommonThemeSupport.background(theme), actual);
        assertColorNotEquals(UiCommonThemeSupport.border(theme), actual);
    }

    @Test
    void categoryListUsesDedicatedSelectorOnly() {
        SettingsCategoryList list = callFx(this::categoryList);

        assertTrue(callFx(() -> list.getStyleClass().contains("pf-settings-category-list")));
        assertFalse(callFx(() -> list.getStyleClass().contains(SettingsUiStyles.LIST)));
    }

    private void runFx(Runnable action) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        future.join();
    }

    private <T> T callFx(java.util.concurrent.Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future.join();
    }

    private SettingsCategoryList categoryList() {
        SettingsCategoryList list = (SettingsCategoryList) shownPanel.lookup(".pf-settings-category-list");
        assertNotNull(list);
        return list;
    }

    private SettingsRuntime newRuntime(String prefix) {
        return new SettingsRuntime(
            tempDir.resolve(prefix + "-app"),
            tempDir.resolve(prefix + "-workspace"),
            new JsonSettingsStorage(tempDir.resolve(prefix + "-app"), tempDir.resolve(prefix + "-workspace")),
            new InMemorySecretStore(),
            new SimpleObjectProperty<>(Theme.dark())
        );
    }

    private ColorPicker appearanceColorPicker() {
        return callFx(() -> {
            shownPanel.selectCategory("appearance");
            shownPanel.applyCss();
            shownPanel.layout();
            return collectNodes(shownPanel, ColorPicker.class).stream()
                .findFirst()
                .orElseThrow();
        });
    }

    private ColorPicker colorPickerFor(SettingsPanel panel, String key) {
        runFx(() -> {
            panel.selectCategory("appearance");
            panel.applyCss();
            panel.layout();
        });
        return callFx(() -> collectNodes(panel.contentArea(), SettingControl.class).stream()
            .filter(control -> key.equals(control.definition().key()))
            .map(control -> collectNodes(control, ColorPicker.class).stream().findFirst().orElseThrow())
            .findFirst()
            .orElseThrow());
    }

    private ListCell<?> selectedCategoryCell() {
        return callFx(() -> {
            shownPanel.applyCss();
            shownPanel.layout();
            SettingsCategoryList list = categoryList();
            list.applyCss();
            list.layout();
            return list.lookupAll(".list-cell").stream()
                .filter(ListCell.class::isInstance)
                .map(ListCell.class::cast)
                .filter(ListCell::isSelected)
                .filter(cell -> cell.getGraphic() != null)
                .findFirst()
                .orElseThrow();
        });
    }

    private Region colorPalettePopup() {
        return callFx(() -> Window.getWindows().stream()
            .filter(Window::isShowing)
            .filter(window -> window != shownPanel.getScene().getWindow())
            .map(Window::getScene)
            .filter(scene -> scene != null)
            .map(Scene::getRoot)
            .map(root -> findNodeWithStyleClass(root, "color-palette"))
            .filter(Region.class::isInstance)
            .map(Region.class::cast)
            .findFirst()
            .orElseThrow());
    }

    private Hyperlink popupHyperlink(Region palette) {
        return callFx(() -> collectNodes(palette, Hyperlink.class).stream()
            .findFirst()
            .orElseThrow());
    }

    private Label selectedCategoryLabel(ListCell<?> cell) {
        return callFx(() -> {
            cell.applyCss();
            Label label = (Label) cell.lookup(".pf-settings-category-label");
            assertNotNull(label);
            return label;
        });
    }

    private <T> List<T> collectNodes(Node root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collectNodes(root, type, matches);
        return matches;
    }

    private <T> void collectNodes(Node node, Class<T> type, List<T> matches) {
        if (type.isInstance(node)) {
            matches.add(type.cast(node));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectNodes(child, type, matches);
            }
        }
    }

    private Node findNodeWithStyleClass(Node node, String styleClass) {
        if (node.getStyleClass().contains(styleClass)) {
            return node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = findNodeWithStyleClass(child, styleClass);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private String describeField(TextInputControl field) {
        String prompt = field.getPromptText();
        if (prompt != null && !prompt.isBlank()) {
            return field.getClass().getSimpleName() + "[" + prompt + "]";
        }
        return field.getClass().getSimpleName();
    }

    private static Color backgroundColor(Region region) {
        Background background = region.getBackground();
        assertNotNull(background);
        assertFalse(background.getFills().isEmpty());
        assertTrue(background.getFills().getFirst().getFill() instanceof Color);
        return (Color) background.getFills().getFirst().getFill();
    }

    private static Color borderColor(Region region) {
        Border border = region.getBorder();
        assertNotNull(border);
        assertFalse(border.getStrokes().isEmpty());
        Paint stroke = border.getStrokes().getFirst().getTopStroke();
        assertTrue(stroke instanceof Color);
        return (Color) stroke;
    }

    private static Color requireColor(Paint paint) {
        assertTrue(paint instanceof Color);
        return (Color) paint;
    }

    private static void assertColorEquals(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), 0.01);
        assertEquals(expected.getGreen(), actual.getGreen(), 0.01);
        assertEquals(expected.getBlue(), actual.getBlue(), 0.01);
        assertEquals(expected.getOpacity(), actual.getOpacity(), 0.01);
    }

    private static void assertColorNotEquals(Color unexpected, Color actual) {
        boolean same = Math.abs(unexpected.getRed() - actual.getRed()) < 0.01
            && Math.abs(unexpected.getGreen() - actual.getGreen()) < 0.01
            && Math.abs(unexpected.getBlue() - actual.getBlue()) < 0.01
            && Math.abs(unexpected.getOpacity() - actual.getOpacity()) < 0.01;
        assertFalse(same, () -> "Did not expect color " + unexpected + " but found " + actual);
    }

    private record CompactFieldAudit(int fieldCount, List<String> missing) {
    }

    private static final class CountingBooleanProperty extends SimpleBooleanProperty {

        private int activeListenerCount;

        @Override
        public void addListener(javafx.beans.value.ChangeListener<? super Boolean> listener) {
            super.addListener(listener);
            activeListenerCount++;
        }

        @Override
        public void removeListener(javafx.beans.value.ChangeListener<? super Boolean> listener) {
            super.removeListener(listener);
            activeListenerCount--;
        }

        private int activeListenerCount() {
            return activeListenerCount;
        }
    }

    private static final class TestCategory implements SettingsCategory {

        private final String id;
        private final String displayName;
        private final int order;
        private final Set<SettingScope> supportedScopes;
        private final CountingBooleanProperty dirtyProperty;
        private int buildCount;

        private TestCategory(
            String id,
            String displayName,
            int order,
            Set<SettingScope> supportedScopes,
            CountingBooleanProperty dirtyProperty
        ) {
            this.id = id;
            this.displayName = displayName;
            this.order = order;
            this.supportedScopes = supportedScopes;
            this.dirtyProperty = dirtyProperty;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public Set<SettingScope> supportedScopes() {
            return supportedScopes;
        }

        @Override
        public Node buildSettingsPane(SettingsContext context) {
            buildCount++;
            return new Label(displayName + ':' + context.activeScope().name());
        }

        @Override
        public void apply(SettingsContext context) {
        }

        @Override
        public void reset(SettingsContext context) {
        }

        @Override
        public boolean isDirty() {
            return dirtyProperty.get();
        }

        @Override
        public ReadOnlyBooleanProperty dirtyProperty() {
            return dirtyProperty;
        }

        private int buildCount() {
            return buildCount;
        }
    }
}
