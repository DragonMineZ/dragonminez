package com.dragonminez.mod.server.player.cap.power;

import com.dragonminez.core.server.player.capability.IServerCapDataManager;
import com.dragonminez.mod.common.player.cap.power.PowerData;
import com.dragonminez.mod.common.player.cap.power.PowerData.PowerDataHolder;
import com.dragonminez.mod.common.player.cap.power.PowerDataManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side manager for {@link PowerData}, handling updates and synchronization of
 * power-related data.
 * <p>
 * This class extends {@link PowerDataManager} to provide server-specific mutation and retrieval
 * functionality and implements {@link IServerCapDataManager} to support synchronization with
 * connected clients.
 */
public class ServerPowerDataManager extends PowerDataManager implements
    IServerCapDataManager<PowerDataManager, PowerData> {

  /**
   * Singleton instance of the server-side power data manager.
   */
  public static final ServerPowerDataManager INSTANCE = new ServerPowerDataManager();

  /**
   * Private constructor to enforce singleton usage. Delegates to the base constructor.
   */
  private ServerPowerDataManager() {
    super();
  }

  /**
   * Sets the player's Power Release (e.g., ZPoints) value and synchronizes the change with the client.
   *
   * @param player       the target server-side player
   * @param powerRelease the new Power Release value to set
   * @param log          whether to log this change for debugging/auditing
   */
  public void setPowerRelease(ServerPlayer player, int powerRelease, boolean log) {
    this.set(player, PowerDataHolder.POWER_RELEASE.id(), powerRelease, log);
  }

  /**
   * Sets the player's Form Release value and synchronizes the change with the client.
   *
   * @param player      the target server-side player
   * @param formRelease the new Form Release value to set
   * @param log         whether to log this change
   */
  public void setFormRelease(ServerPlayer player, int formRelease, boolean log) {
    this.set(player, PowerDataHolder.FORM_RELEASE.id(), formRelease, log);
  }

  /**
   * Sets the player's Aura state (on/off) and synchronizes the change with the client.
   *
   * @param player    the target server-side player
   * @param auraState the new aura state value to set
   * @param log       whether to log this change
   */
  public void setAuraState(ServerPlayer player, boolean auraState, boolean log) {
    this.set(player, PowerDataHolder.AURA_STATE.id(), auraState, log);
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
