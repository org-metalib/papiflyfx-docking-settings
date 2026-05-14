package org.metalib.papifly.fx.settings.docking;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.ui.SettingsPanel;

public class SettingsContentFactory implements ContentFactory {

    public static final String FACTORY_ID = "settings-panel";

    private final SettingsRuntime runtime;

    public SettingsContentFactory(SettingsRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) {
            return null;
        }
        return new SettingsPanel(runtime);
    }
}
