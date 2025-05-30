package com.dragonminez.mod.common.player.cap.combat;

import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central manager for accessing and updating {@link CombatData} on players via Forge's capability
 * system.
 * <p>
 * This class serves as the main point of interaction for combat-related capability data. It is
 * designed as a singleton to be accessed statically via {@link CombatDataManager#INSTANCE}.
 */
public class CombatDataManager extends CapDataManager<CombatData> {

  /**
   * Singleton instance of the combat data manager.
   */
  public static final CombatDataManager INSTANCE = new CombatDataManager();

  /**
   * Protected constructor to enforce singleton usage. Registers the capability using Forge's
   * {@link CapabilityManager} with a generic {@link CapabilityToken}.
   */
  protected CombatDataManager() {
    super(CapabilityManager.get(new CapabilityToken<>() {
    }));
  }

  /**
   * Checks if the specified player is currently in combat mode.
   *
   * @param player The player to check.
   * @return true if the player is in combat mode, false otherwise.
   */
  public boolean isInCombatMode(Player player) {
    final CombatData combatData = this.retrieveData(player);
    return combatData != null && combatData.isInCombatMode();
  }

  /**
   * Checks if the specified player is currently blocking.
   *
   * @param player The player to check.
   * @return true if the player is blocking, false otherwise.
   */
  public boolean isBlocking(Player player) {
    final CombatData combatData = this.retrieveData(player);
    return combatData != null && combatData.isBlocking();
  }

  /**
   * Creates a new instance of {@link CombatData}.
   *
   * @return a new {@link CombatData} object.
   */
  @Override
  public CombatData buildCap() {
    return new CombatData();
  }
}
