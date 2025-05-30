package com.dragonminez.mod.common.player.cap.stat;

import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central manager for accessing {@link StatData} on players via Forge's capability system.
 * <p>
 * Serves as a common superclass for both client- and server-specific stat manager. Intended to be
 * accessed statically via {@link StatDataManager#INSTANCE}.
 */
public class StatDataManager extends CapDataManager<StatData> {

  /**
   * Global shared instance of the stat manager.
   */
  public static StatDataManager INSTANCE = new StatDataManager();

  /**
   * Protected constructor to restrict instantiation (supports singleton use).
   * Registers the capability using Forge's {@link CapabilityManager}.
   */
  protected StatDataManager() {
    super(CapabilityManager.get(new CapabilityToken<>() {
    }));
  }

  /**
   * Gets the strength stat value for the given player.
   *
   * @param player The player whose strength stat to retrieve.
   * @return The player's strength value, or 0 if data is unavailable.
   */
  public int getStrength(Player player) {
    final StatData data = this.retrieveData(player);
    return data != null ? data.getStrength() : 0;
  }

  /**
   * Gets the strike power stat value for the given player.
   *
   * @param player The player whose strike power stat to retrieve.
   * @return The player's strike power value, or 0 if data is unavailable.
   */
  public int getStrikePower(Player player) {
    final StatData data = this.retrieveData(player);
    return data != null ? data.getStrikePower() : 0;
  }

  /**
   * Gets the energy stat value for the given player.
   *
   * @param player The player whose energy stat to retrieve.
   * @return The player's energy value, or 0 if data is unavailable.
   */
  public int getEnergy(Player player) {
    final StatData data = this.retrieveData(player);
    return data != null ? data.getEnergy() : 0;
  }

  /**
   * Gets the vitality stat value for the given player.
   *
   * @param player The player whose vitality stat to retrieve.
   * @return The player's vitality value, or 0 if data is unavailable.
   */
  public int getVitality(Player player) {
    final StatData data = this.retrieveData(player);
    return data != null ? data.getVitality() : 0;
  }

  /**
   * Gets the resistance stat value for the given player.
   *
   * @param player The player whose resistance stat to retrieve.
   * @return The player's resistance value, or 0 if data is unavailable.
   */
  public int getResistance(Player player) {
    final StatData data = this.retrieveData(player);
    return data != null ? data.getResistance() : 0;
  }

  /**
   * Gets the ki power stat value for the given player.
   *
   * @param player The player whose ki power stat to retrieve.
   * @return The player's ki power value, or 0 if data is unavailable.
   */
  public int getKiPower(Player player) {
    final StatData data = this.retrieveData(player);
    return data != null ? data.getKiPower() : 0;
  }

  /**
   * Gets the alignment stat value for the given player.
   *
   * @param player The player whose alignment stat to retrieve.
   * @return The player's alignment value, or 0 if data is unavailable.
   */
  public int getAlignment(Player player) {
    final StatData data = this.retrieveData(player);
    return data != null ? data.getAlignment() : 0;
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
