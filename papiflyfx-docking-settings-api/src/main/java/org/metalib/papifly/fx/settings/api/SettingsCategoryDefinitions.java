package org.metalib.papifly.fx.settings.api;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Exposes searchable definitions and toolbar actions for a settings category.
 */
public interface SettingsCategoryDefinitions {

    default List<SettingDefinition<?>> definitions() {
        return List.of();
    }

    default List<SettingsAction> actions() {
        return List.of();
    }

    /**
     * Returns the set of scopes this category actively supports.
     *
     * <p>The default implementation derives the set from the scopes declared on
     * {@link #definitions()}. Categories with no definitions or custom scope
     * logic should override this method.
     *
     * <p>The toolbar uses this to show only valid scope options for the active category.
     *
     * @return non-empty set of supported scopes
     */
    default Set<SettingScope> supportedScopes() {
        List<SettingDefinition<?>> defs = definitions();
        if (defs == null || defs.isEmpty()) {
            return Set.of(SettingScope.APPLICATION);
        }
        EnumSet<SettingScope> scopes = EnumSet.noneOf(SettingScope.class);
        for (SettingDefinition<?> def : defs) {
            scopes.add(def.scope());
        }
        return Set.copyOf(scopes);
    }
}
