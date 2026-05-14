package org.metalib.papifly.fx.settings.docking;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentStateAdapter;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.metalib.papifly.fx.settings.runtime.SettingsRuntime;
import org.metalib.papifly.fx.settings.ui.SettingsPanel;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsStateAdapter implements ContentStateAdapter {

    public static final int VERSION = 1;

    private static final AtomicReference<SettingsRuntime> SHARED_RUNTIME = new AtomicReference<>();

    private final SettingsRuntime runtime;

    /**
     * Registers the host-owned runtime for ServiceLoader-based restoration.
     * Must be called once before any session restore or ServiceLoader discovery.
     */
    public static void setSharedRuntime(SettingsRuntime runtime) {
        SHARED_RUNTIME.set(Objects.requireNonNull(runtime, "runtime"));
    }

    /**
     * ServiceLoader entry point. Requires {@link #setSharedRuntime(SettingsRuntime)}
     * to have been called; throws if no shared runtime has been registered.
     */
    public SettingsStateAdapter() {
        this(requireSharedRuntime());
    }

    public SettingsStateAdapter(SettingsRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    private static SettingsRuntime requireSharedRuntime() {
        SettingsRuntime runtime = SHARED_RUNTIME.get();
        if (runtime == null) {
            throw new IllegalStateException(
                "No shared SettingsRuntime has been registered. "
                + "Call SettingsStateAdapter.setSharedRuntime(runtime) before session restore."
            );
        }
        return runtime;
    }

    @Override
    public String getTypeKey() {
        return SettingsContentFactory.FACTORY_ID;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, Object> saveState(String contentId, Node content) {
        if (content instanceof SettingsPanel panel && panel.getActiveCategoryId() != null) {
            return Map.of("activeCategory", panel.getActiveCategoryId());
        }
        return Map.of();
    }

    @Override
    public Node restore(LeafContentData content) {
        String activeCategory = null;
        if (content != null && content.state() != null) {
            Object value = content.state().get("activeCategory");
            if (value != null) {
                activeCategory = String.valueOf(value);
            }
        }
        return new SettingsPanel(runtime, activeCategory);
    }
}
