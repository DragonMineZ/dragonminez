package com.dragonminez.mod.server.player.stat;

import com.dragonminez.mod.common.network.player.cap.stat.s2c.PacketS2CSyncStat;
import com.dragonminez.mod.common.player.cap.stat.StatData;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataType;
import com.dragonminez.mod.common.player.cap.stat.StatDataManager;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.core.server.player.capability.IServerCapDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side manager for player {@link StatData}, providing methods to update and sync individual
 * stat fields.
 * <p>
 * This class handles stat mutation and optionally logs changes. It extends {@link StatDataManager}
 * and implements {@link IServerCapDataManager} to support syncing with clients.
 */
public class ServerStatDataManager extends StatDataManager implements
    IServerCapDataManager<StatDataManager, StatData> {

  /**
   * Singleton instance of the server stat manager.
   */
  public static final ServerStatDataManager INSTANCE = new ServerStatDataManager();

  private ServerStatDataManager() {
    super();
  }

  /**
   * Sets the player's Strength stat.
   *
   * @param player   The player whose stat is being updated.
   * @param strength The new strength value.
   * @param log      Whether to log this change on the server.
   */
  public void setStrength(ServerPlayer player, int strength, boolean log) {
    this.setStatInternal(player, StatDataType.STRENGTH, strength,
        data -> data.setStrength(strength), log);
  }

  /**
   * Sets the player's Strike Power stat.
   *
   * @param player      The player whose stat is being updated.
   * @param strikePower The new strike power value.
   * @param log         Whether to log this change on the server.
   */
  public void setStrikePower(ServerPlayer player, int strikePower, boolean log) {
    this.setStatInternal(player, StatDataType.STRIKE_POWER, strikePower,
        data -> data.setStrikePower(strikePower), log);
  }

  /**
   * Sets the player's Energy stat.
   *
   * @param player The player whose stat is being updated.
   * @param energy The new energy value.
   * @param log    Whether to log this change on the server.
   */
  public void setEnergy(ServerPlayer player, int energy, boolean log) {
    this.setStatInternal(player, StatDataType.ENERGY, energy,
        data -> data.setEnergy(energy), log);
  }

  /**
   * Sets the player's Vitality stat.
   *
   * @param player   The player whose stat is being updated.
   * @param vitality The new vitality value.
   * @param log      Whether to log this change on the server.
   */
  public void setVitality(ServerPlayer player, int vitality, boolean log) {
    this.setStatInternal(player, StatDataType.VITALITY, vitality,
        data -> data.setVitality(vitality), log);
  }

  /**
   * Sets the player's Resistance stat.
   *
   * @param player     The player whose stat is being updated.
   * @param resistance The new resistance value.
   * @param log        Whether to log this change on the server.
   */
  public void setResistance(ServerPlayer player, int resistance, boolean log) {
    this.setStatInternal(player, StatDataType.RESISTANCE, resistance,
        data -> data.setResistance(resistance), log);
  }

  /**
   * Sets the player's Ki Power stat.
   *
   * @param player  The player whose stat is being updated.
   * @param kiPower The new ki power value.
   * @param log     Whether to log this change on the server.
   */
  public void setKiPower(ServerPlayer player, int kiPower, boolean log) {
    this.setStatInternal(player, StatDataType.KI_POWER, kiPower,
        data -> data.setKiPower(kiPower), log);
  }

  /**
   * Sets the player's Alignment stat.
   *
   * @param player    The player whose stat is being updated.
   * @param alignment The new alignment value.
   * @param log       Whether to log this change on the server.
   */
  public void setAlignment(ServerPlayer player, int alignment, boolean log) {
    this.setStatInternal(player, StatDataType.ALIGNMENT, alignment,
        data -> data.setAlignment(alignment), log);
  }

  @Override
  public StatDataManager manager() {
    return this;
  }

  @Override
  public PacketS2CCapSync<StatData> buildSyncPacket(@Nullable Player player,
      @Nullable StatData data, @Nullable Boolean isPublic) {
    return new PacketS2CSyncStat(data);
  }
}
