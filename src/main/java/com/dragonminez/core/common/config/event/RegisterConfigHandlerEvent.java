package com.dragonminez.core.common.config.event;

import com.dragonminez.core.common.config.ConfigManager;
import net.minecraftforge.eventbus.api.Event;

/**
 * Event triggered to register configuration handlers within the {@link ConfigManager}. This event
 * provides information about the configuration type being registered.
 *
 * <p>
 * This event is posted during the mod loading process to enable mods to dynamically register their
 * configuration handlers. The registration is based on the configuration type, which can be either
 * STATIC or RUNTIME.
 * </p>
 *
 * <b>Important:</b>
 * <p>
 * Automatic event bus detection is not applicable in this case. You must manually register the
 * event listener using the following code:
 * </p>
 * <pre>
 * MinecraftForge.EVENT_BUS.addListener(YourClass::yourListenerFunction);
 * </pre>
 * This workaround is required due to specific implementation constraints.
 * </p>
 */
public class RegisterConfigHandlerEvent extends Event {

  private final ConfigManager dispatcher; // The instance of the ConfigManager handling the registration

  /**
   * Constructor for the RegisterConfigHandlerEvent.
   *
   * @param dispatcher The {@link ConfigManager} instance responsible for handling configurations.
   */
  public RegisterConfigHandlerEvent(ConfigManager dispatcher) {
    this.dispatcher = dispatcher;
  }

  /**
   * Retrieves the {@link ConfigManager} instance that will handle the configuration registration.
   *
   * @return The ConfigManager dispatcher.
   */
  public ConfigManager dispatcher() {
    return dispatcher;
  }
}
