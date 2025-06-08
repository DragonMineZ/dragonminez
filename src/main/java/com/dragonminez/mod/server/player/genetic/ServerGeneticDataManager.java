package com.dragonminez.mod.server.player.genetic;

import com.dragonminez.mod.common.player.cap.genetic.GeneticData;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataHolder;
import com.dragonminez.mod.common.player.cap.genetic.GeneticDataManager;
import com.dragonminez.core.server.player.capability.IServerCapDataManager;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side manager for {@link GeneticData}, handling updates and synchronization of genetic
 * traits such as race and form for players.
 * <p>
 * This class extends {@link GeneticDataManager} to provide mutation and retrieval functionality and
 * implements {@link IServerCapDataManager} to support synchronization with connected clients. It
 * includes logging support for mutation changes.
 */
public class ServerGeneticDataManager extends GeneticDataManager implements
    IServerCapDataManager<GeneticDataManager, GeneticData> {

  /**
   * Singleton instance of the server-side genetic data manager.
   */
  public static final ServerGeneticDataManager INSTANCE = new ServerGeneticDataManager();

  private ServerGeneticDataManager() {
    super();
  }

  /**
   * Sets the player's genetic race and synchronizes the change.
   *
   * @param player The target server player.
   * @param race   The new race to assign.
   * @param log    Whether to log the change.
   */
  public void setRace(ServerPlayer player, String race, boolean log) {
    this.set(player, GeneticDataHolder.RACE.id(), race, log);
  }

  /**
   * Sets the player's genetic form and synchronizes the change.
   *
   * @param player The target server player.
   * @param form   The new form to assign.
   * @param log    Whether to log the change.
   */
  public void setForm(ServerPlayer player, String form, boolean log) {
    this.set(player, GeneticDataHolder.RACE.id(), form, log);
  }

  @Override
  public GeneticDataManager manager() {
    return this;
  }
}
