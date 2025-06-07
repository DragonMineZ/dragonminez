package com.dragonminez.mod.core.common.network.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Custom Forge event used to register network handlers to a {@link SimpleChannel}.
 * <p>
 * This event is fired during the mod loading lifecycle to allow different parts of the mod to
 * register their packet handlers to the shared network channel. It provides phase control to
 * distinguish between early and late registrations.
 */
public class RegisterNetHandlerEvent extends Event {

  private final SimpleChannel channel;
  private final Phase phase;

  /**
   * Constructs a new RegisterNetHandlerEvent.
   *
   * @param channel the {@link SimpleChannel} where handlers will be registered
   * @param phase   the registration {@link Phase}, indicating if it’s early or late
   */
  public RegisterNetHandlerEvent(SimpleChannel channel, Phase phase) {
    this.channel = channel;
    this.phase = phase;
  }

  /**
   * Returns the network channel this event is targeting.
   *
   * @return the {@link SimpleChannel} used for registration
   */
  public SimpleChannel channel() {
    return this.channel;
  }

  /**
   * Returns true if the event is in the early (START) phase.
   *
   * @return true if registration is happening early
   */
  public boolean isEarly() {
    return this.phase == Phase.START;
  }

  /**
   * Returns true if the event is in the late (END) phase.
   *
   * @return true if registration is happening late
   */
  public boolean isLate() {
    return this.phase == Phase.END;
  }

  /**
   * Indicates the phase of the network registration lifecycle.
   */
  public enum Phase {
    START, END
  }
}
