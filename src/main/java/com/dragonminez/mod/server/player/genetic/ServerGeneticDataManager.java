package com.dragonminez.mod.server.player.genetic;

import com.dragonminez.mod.common.network.player.cap.genetic.s2c.PacketS2CSyncGenetic;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataType;
import com.dragonminez.mod.common.player.cap.genetic.GeneticDataManager;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.core.server.player.capability.IServerCapDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

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
    this.setStatInternal(player, GeneticDataType.RACE, race,
        data -> data.setRace(race), log);
  }

  /**
   * Sets the player's genetic form and synchronizes the change.
   *
   * @param player The target server player.
   * @param form   The new form to assign.
   * @param log    Whether to log the change.
   */
  public void setForm(ServerPlayer player, String form, boolean log) {
    this.setStatInternal(player, GeneticDataType.RACE, form,
        data -> data.setForm(form), log);
  }

  @Override
  public GeneticDataManager manager() {
    return this;
  }

  @Override
  public PacketS2CCapSync<GeneticData> buildSyncPacket(@Nullable Player player,
      @Nullable GeneticData data, @Nullable Boolean isPublic) {
    return new PacketS2CSyncGenetic(data, player == null ? null : player.getUUID());
  }
}
