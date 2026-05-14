package org.metalib.papifly.fx.settings.runtime;

import org.metalib.papifly.fx.settings.api.SecretStore;
import org.metalib.papifly.fx.settings.api.SettingsServicesProvider;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultSettingsServicesProvider implements SettingsServicesProvider {

    private static final AtomicReference<SettingsRuntime> SHARED_RUNTIME = new AtomicReference<>();

    private final SettingsRuntime runtime;

    /**
     * Registers the host-owned runtime for ServiceLoader-based discovery.
     * Must be called once before any ServiceLoader consumer resolves this provider.
     */
    public static void setSharedRuntime(SettingsRuntime runtime) {
        SHARED_RUNTIME.set(Objects.requireNonNull(runtime, "runtime"));
    }

    /**
     * ServiceLoader entry point. Requires {@link #setSharedRuntime(SettingsRuntime)}
     * to have been called; throws if no shared runtime has been registered.
     */
    public DefaultSettingsServicesProvider() {
        this(requireSharedRuntime());
    }

    DefaultSettingsServicesProvider(SettingsRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public SettingsStorage storage() {
        return runtime.storage();
    }

    @Override
    public SecretStore secretStore() {
        return runtime.secretStore();
    }

    private static SettingsRuntime requireSharedRuntime() {
        SettingsRuntime runtime = SHARED_RUNTIME.get();
        if (runtime == null) {
            throw new IllegalStateException(
                "No shared SettingsRuntime has been registered. "
                + "Call DefaultSettingsServicesProvider.setSharedRuntime(runtime) before ServiceLoader discovery."
            );
        }
        return runtime;
    }
}
