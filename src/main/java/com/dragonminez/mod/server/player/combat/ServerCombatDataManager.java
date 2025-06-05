package com.dragonminez.mod.server.player.combat;

import com.dragonminez.mod.common.network.player.cap.combat.s2c.PacketS2CSyncCombatData;
import com.dragonminez.mod.common.player.cap.combat.CombatData;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataType;
import com.dragonminez.mod.common.player.cap.combat.CombatDataManager;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData;
import com.dragonminez.mod.common.player.cap.genetic.GeneticDataManager;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.core.server.player.capability.IServerCapDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

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
    this.setStatInternal(player, CombatDataType.COMBAT_MODE, combat,
        data -> data.setCombatMode(combat), log);
  }

  /**
   * Sets the player's blocking state and synchronizes the change.
   *
   * @param player   The target server player.
   * @param blocking The new blocking state to assign.
   * @param log      Whether to log the change.
   */
  public void setBlocking(ServerPlayer player, boolean blocking, boolean log) {
    this.setStatInternal(player, CombatDataType.BLOCKING, blocking,
        data -> data.setBlocking(blocking), log);
  }

  @Override
  public CombatDataManager manager() {
    return this;
  }

  @Override
  public PacketS2CCapSync<CombatData> buildSyncPacket(@Nullable Player player,
      @Nullable CombatData data, @Nullable Boolean isPublic) {
    return new PacketS2CSyncCombatData(data, player == null ? null : player.getUUID());
  }
}
