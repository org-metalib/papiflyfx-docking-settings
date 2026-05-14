package org.metalib.papifly.fx.settings.categories;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;
import org.metalib.papifly.fx.settings.api.SecretKeyNames;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.DefinitionFormBinder;

import java.util.List;

public class AiModelsCategory implements SettingsCategory {

    private enum Provider {
        OPENAI,
        ANTHROPIC,
        GOOGLE
    }

    private static final SettingDefinition<Provider> DEFAULT_PROVIDER = SettingDefinition
        .of("ai.defaultProvider", "Default Provider", SettingType.ENUM, Provider.OPENAI)
        .withDescription("Provider used for default AI operations.");
    private static final SettingDefinition<String> DEFAULT_MODEL = SettingDefinition
        .of("ai.defaultModel", "Default Model", SettingType.STRING, "gpt-5.4")
        .withDescription("Default model identifier.");
    private static final SettingDefinition<String> OPENAI_BASE_URL = SettingDefinition
        .of("ai.openai.baseUrl", "OpenAI Base URL", SettingType.STRING, "https://api.openai.com/v1")
        .withDescription("Base URL for the OpenAI-compatible API.");
    private static final SettingDefinition<String> OPENAI_MODEL = SettingDefinition
        .of("ai.openai.model", "OpenAI Model", SettingType.STRING, "gpt-5.4")
        .withDescription("Model identifier for OpenAI requests.");
    private static final SettingDefinition<String> OPENAI_API_KEY = SettingDefinition
        .of(SecretKeyNames.settingsKey("openai", "api-key"), "OpenAI API Key", SettingType.SECRET, "")
        .withDescription("API key for OpenAI.");
    private static final SettingDefinition<String> ANTHROPIC_MODEL = SettingDefinition
        .of("ai.anthropic.model", "Anthropic Model", SettingType.STRING, "claude-sonnet-4")
        .withDescription("Model identifier for Anthropic requests.");
    private static final SettingDefinition<String> ANTHROPIC_API_KEY = SettingDefinition
        .of(SecretKeyNames.settingsKey("anthropic", "api-key"), "Anthropic API Key", SettingType.SECRET, "")
        .withDescription("API key for Anthropic.");
    private static final SettingDefinition<String> GOOGLE_MODEL = SettingDefinition
        .of("ai.google.model", "Google Model", SettingType.STRING, "gemini-2.5-pro")
        .withDescription("Model identifier for Google AI requests.");
    private static final SettingDefinition<String> GOOGLE_API_KEY = SettingDefinition
        .of(SecretKeyNames.settingsKey("google", "api-key"), "Google API Key", SettingType.SECRET, "")
        .withDescription("API key for Google AI.");

    private DefinitionFormBinder binder;

    @Override
    public String id() {
        return "ai-models";
    }

    @Override
    public String displayName() {
        return "AI Models";
    }

    @Override
    public int order() {
        return 85;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(
            DEFAULT_PROVIDER,
            DEFAULT_MODEL,
            OPENAI_BASE_URL,
            OPENAI_MODEL,
            OPENAI_API_KEY,
            ANTHROPIC_MODEL,
            ANTHROPIC_API_KEY,
            GOOGLE_MODEL,
            GOOGLE_API_KEY
        );
    }

    @Override
    public Node buildSettingsPane(SettingsContext context) {
        if (binder == null) {
            binder = new DefinitionFormBinder(definitions());
        }
        binder.load(context);
        return binder.pane();
    }

    @Override
    public void apply(SettingsContext context) {
        binder.save(context);
        context.storage().save();
    }

    @Override
    public void reset(SettingsContext context) {
        binder.load(context);
    }

    @Override
    public boolean isDirty() {
        return binder != null && binder.isDirty();
    }

    @Override
    public ReadOnlyBooleanProperty dirtyProperty() {
        return binder == null ? null : binder.dirtyProperty();
    }
}
