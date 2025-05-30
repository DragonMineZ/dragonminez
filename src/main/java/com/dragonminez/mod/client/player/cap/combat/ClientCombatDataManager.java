package com.dragonminez.mod.client.player.cap.combat;

import com.dragonminez.mod.common.player.cap.combat.CombatData;
import com.dragonminez.mod.common.player.cap.combat.CombatDataManager;

/**
 * Client-side manager for accessing and updating {@link CombatData} via Forge's capability system.
 * <p>
 * This subclass of {@link CombatDataManager} is specifically intended for use on the client side.
 * It provides access to combat-related player data and is designed as a singleton for centralized usage.
 * Use {@link ClientCombatDataManager#INSTANCE} to retrieve the instance.
 */
public class ClientCombatDataManager extends CombatDataManager {

  /**
   * Singleton instance of the client-side combat data manager.
   */
  public static final ClientCombatDataManager INSTANCE = new ClientCombatDataManager();

  /**
   * Private constructor to enforce singleton pattern.
   */
  private ClientCombatDataManager() {
    super();
  }
}
