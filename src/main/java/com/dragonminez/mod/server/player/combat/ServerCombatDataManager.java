package com.dragonminez.mod.server.player.combat;

import com.dragonminez.mod.common.player.cap.combat.CombatData;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataHolder;
import com.dragonminez.mod.common.player.cap.combat.CombatDataManager;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData;
import com.dragonminez.mod.common.player.cap.genetic.GeneticDataManager;
import com.dragonminez.mod.core.server.player.capability.IServerCapDataManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side manager for {@link GeneticData}, handling updates and synchronization
 * of genetic traits such as race and form for players.
 * <p>
 * This class extends {@link GeneticDataManager} to provide mutation and retrieval functionality
 * and implements {@link IServerCapDataManager} to support synchronization with connected clients.
 * It includes logging support for mutation changes.
 */
public class ServerCombatDataManager extends CombatDataManager implements
    IServerCapDataManager<CombatDataManager, CombatData> {

  /**
   * Singleton instance of the server-side genetic data manager.
   */
  public static final ServerCombatDataManager INSTANCE = new ServerCombatDataManager();

  private ServerCombatDataManager() {
    super();
  }

  /**
   * Sets the player's combat mode and synchronizes the change.
   *
   * @param player The target server player.
   * @param combat The new combat mode to assign.
   * @param log    Whether to log the change.
   */
  public void setCombatMode(ServerPlayer player, boolean combat, boolean log) {
    this.set(player, CombatDataHolder.COMBAT_MODE.id(), combat, log);
  }

  /**
   * Sets the player's blocking state and synchronizes the change.
   *
   * @param player   The target server player.
   * @param blocking The new blocking state to assign.
   * @param log      Whether to log the change.
   */
  public void setBlocking(ServerPlayer player, boolean blocking, boolean log) {
    this.set(player, CombatDataHolder.BLOCKING.id(), blocking, log);
  }

  @Override
  public CombatDataManager manager() {
    return this;
  }
}
