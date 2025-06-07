package com.dragonminez.mod.common.player.cap.stat;

import com.dragonminez.mod.common.network.player.cap.stat.s2c.PacketS2CSyncStat;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataHolder;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.jetbrains.annotations.Nullable;

/**
 * Central manager for retrieving {@link StatData} on players via Forge's capability system.
 * <p>
 * Serves as a common superclass for both client- and server-specific stat manager. Intended to be
 * accessed statically via {@link StatDataManager#INSTANCE}.
 */
public class StatDataManager extends CapDataManager<StatData> {

  /**
   * Global shared instance of the stat manager.
   */
  public static final StatDataManager INSTANCE = new StatDataManager();

  /**
   * Protected constructor to restrict instantiation (supports singleton use).
   * Registers the capability using Forge's {@link CapabilityManager}.
   */
  protected StatDataManager() {
    super(CapabilityManager.get(new CapabilityToken<>() {}));
  }

  /**
   * Retrieves the strength stat value for the given player.
   *
   * @param player The player whose strength stat to retrieve.
   * @return The retrieved strength value, or 0 if data is unavailable.
   */
  public int getStrength(Player player) {
    Object val = this.get(player, StatDataHolder.STRENGTH.id());
    return val instanceof Integer i ? i : 0;
  }

  /**
   * Retrieves the strike power stat value for the given player.
   *
   * @param player The player whose strike power stat to retrieve.
   * @return The retrieved strike power value, or 0 if data is unavailable.
   */
  public int getStrikePower(Player player) {
    Object val = this.get(player, StatDataHolder.STRIKE_POWER.id());
    return val instanceof Integer i ? i : 0;
  }

  /**
   * Retrieves the energy stat value for the given player.
   *
   * @param player The player whose energy stat to retrieve.
   * @return The retrieved energy value, or 0 if data is unavailable.
   */
  public int getEnergy(Player player) {
    Object val = this.get(player, StatDataHolder.ENERGY.id());
    return val instanceof Integer i ? i : 0;
  }

  /**
   * Retrieves the vitality stat value for the given player.
   *
   * @param player The player whose vitality stat to retrieve.
   * @return The retrieved vitality value, or 0 if data is unavailable.
   */
  public int getVitality(Player player) {
    Object val = this.get(player, StatDataHolder.VITALITY.id());
    return val instanceof Integer i ? i : 0;
  }

  /**
   * Retrieves the resistance stat value for the given player.
   *
   * @param player The player whose resistance stat to retrieve.
   * @return The retrieved resistance value, or 0 if data is unavailable.
   */
  public int getResistance(Player player) {
    Object val = this.get(player, StatDataHolder.RESISTANCE.id());
    return val instanceof Integer i ? i : 0;
  }

  /**
   * Retrieves the ki power stat value for the given player.
   *
   * @param player The player whose ki power stat to retrieve.
   * @return The retrieved ki power value, or 0 if data is unavailable.
   */
  public int getKiPower(Player player) {
    Object val = this.get(player, StatDataHolder.KI_POWER.id());
    return val instanceof Integer i ? i : 0;
  }

  /**
   * Retrieves the alignment stat value for the given player.
   *
   * @param player The player whose alignment stat to retrieve.
   * @return The retrieved alignment value, or 0 if data is unavailable.
   */
  public int getAlignment(Player player) {
    Object val = this.get(player, StatDataHolder.ALIGNMENT.id());
    return val instanceof Integer i ? i : 0;
  }

  @Override
  public PacketS2CCapSync<StatData> buildSyncPacket(@Nullable Player player,
      @Nullable StatData data, @Nullable Boolean isPublic) {
    return new PacketS2CSyncStat(data);
  }

  /**
   * Creates and returns a new instance of {@link StatData} for capability initialization.
   *
   * @return A new {@link StatData} instance.
   */
  @Override
  public StatData buildCap() {
    return new StatData();
  }
}
