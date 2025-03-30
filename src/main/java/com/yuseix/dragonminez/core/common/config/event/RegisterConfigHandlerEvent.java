package com.yuseix.dragonminez.core.common.config.event;

import com.yuseix.dragonminez.core.common.config.ConfigManager;
import com.yuseix.dragonminez.core.common.config.model.ConfigType;
import net.minecraftforge.eventbus.api.Event;

/**
 * Event triggered to register configuration handlers in the {@link ConfigManager}.
 * This event carries information about the configuration type being registered.
 * <p>
 * This event is posted during the mod loading process to allow mods to register their
 * configuration handlers dynamically, based on the configuration type (STATIC or RUNTIME).
 * </p>
 */
public class RegisterConfigHandlerEvent extends Event {

    private final ConfigManager dispatcher; // The instance of the ConfigManager handling the registration
    private final ConfigType type; // The configuration type (STATIC or RUNTIME) being registered

    /**
     * Constructor for the RegisterConfigHandlerEvent.
     *
     * @param dispatcher The {@link ConfigManager} instance responsible for handling configurations.
     * @param type The {@link ConfigType} (STATIC or RUNTIME) indicating which type of configuration
     *             handlers are being registered.
     */
    public RegisterConfigHandlerEvent(ConfigManager dispatcher, ConfigType type) {
        this.dispatcher = dispatcher;
        this.type = type;
    }

    /**
     * Retrieves the {@link ConfigManager} instance that will handle the configuration registration.
     *
     * @return The ConfigManager dispatcher.
     */
    public ConfigManager dispatcher() {
        return dispatcher;
    }

    /**
     * Retrieves the {@link ConfigType} associated with this event, indicating which configuration
     * type is being registered (STATIC or RUNTIME).
     *
     * @return The ConfigType (STATIC or RUNTIME).
     */
    public ConfigType type() {
        return type;
    }
}
