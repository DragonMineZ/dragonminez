package com.dragonminez.mod.client.player.cap.power;

import com.dragonminez.core.server.player.capability.IServerCapDataManager;
import com.dragonminez.mod.common.player.cap.power.PowerData;
import com.dragonminez.mod.common.player.cap.power.PowerDataManager;

/**
 * Client-side manager for {@link PowerData}, allowing access to power-related capability data
 * locally on the client.
 * <p>
 * This class extends {@link PowerDataManager} to provide read-only or cached access to power
 * capabilities for the local player. It implements {@link IServerCapDataManager} only to
 * conform to a common interface; synchronization should be handled elsewhere via packets.
 */
public class ClientPowerDataManager extends PowerDataManager implements
    IServerCapDataManager<PowerDataManager, PowerData> {

  /**
   * Singleton instance of the client-side power data manager.
   */
  public static final ClientPowerDataManager INSTANCE = new ClientPowerDataManager();

  /**
   * Private constructor to enforce singleton usage. Delegates to the base constructor.
   */
  private ClientPowerDataManager() {
    super();
  }

  /**
   * Returns the associated data manager instance (this).
   *
   * @return the current {@link PowerDataManager} instance
   */
  @Override
  public PowerDataManager manager() {
    return this;
  }
}
