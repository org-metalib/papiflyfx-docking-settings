package org.metalib.papifly.fx.settings.categories;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.scene.Node;
import org.metalib.papifly.fx.settings.api.SettingDefinition;
import org.metalib.papifly.fx.settings.api.SettingType;
import org.metalib.papifly.fx.settings.api.SettingsCategory;
import org.metalib.papifly.fx.settings.api.SettingsContext;
import org.metalib.papifly.fx.settings.ui.DefinitionFormBinder;

import java.util.List;

public class NetworkCategory implements SettingsCategory {

    private static final SettingDefinition<Boolean> PROXY_ENABLED = SettingDefinition
        .of("network.proxy.enabled", "Proxy Enabled", SettingType.BOOLEAN, false)
        .withDescription("Enable outbound proxy routing.");
    private static final SettingDefinition<String> PROXY_HOST = SettingDefinition
        .of("network.proxy.host", "Proxy Host", SettingType.STRING, "")
        .withDescription("Proxy host name.");
    private static final SettingDefinition<Integer> PROXY_PORT = SettingDefinition
        .of("network.proxy.port", "Proxy Port", SettingType.INTEGER, 8080)
        .withDescription("Proxy port.");
    private static final SettingDefinition<Boolean> TLS_VERIFY = SettingDefinition
        .of("network.tls.verify", "Verify TLS", SettingType.BOOLEAN, true)
        .withDescription("Verify TLS certificates for outbound requests.");
    private static final SettingDefinition<Integer> CONNECT_TIMEOUT = SettingDefinition
        .of("network.timeout.connect", "Connect Timeout (ms)", SettingType.INTEGER, 5000)
        .withDescription("Socket connect timeout.");
    private static final SettingDefinition<Integer> READ_TIMEOUT = SettingDefinition
        .of("network.timeout.read", "Read Timeout (ms)", SettingType.INTEGER, 15000)
        .withDescription("Socket read timeout.");

    private DefinitionFormBinder binder;

    @Override
    public String id() {
        return "network";
    }

    @Override
    public String displayName() {
        return "Network";
    }

    @Override
    public int order() {
        return 75;
    }

    @Override
    public List<SettingDefinition<?>> definitions() {
        return List.of(PROXY_ENABLED, PROXY_HOST, PROXY_PORT, TLS_VERIFY, CONNECT_TIMEOUT, READ_TIMEOUT);
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
