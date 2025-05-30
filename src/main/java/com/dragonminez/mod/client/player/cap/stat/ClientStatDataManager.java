package com.dragonminez.mod.client.player.cap.stat;

import com.dragonminez.mod.common.player.cap.stat.StatData;
import com.dragonminez.mod.common.player.cap.stat.StatDataManager;

/**
 * Client-side manager for accessing and updating {@link StatData} via Forge's capability system.
 * <p>
 * This subclass of {@link StatDataManager} is intended specifically for use on the client side.
 * It allows access to stat-related player data and follows the singleton pattern for centralized access.
 * Use {@link ClientStatDataManager#INSTANCE} to retrieve the instance.
 */
public class ClientStatDataManager extends StatDataManager {

  /**
   * Singleton instance of the client-side stat data manager.
   */
  public static final ClientStatDataManager INSTANCE = new ClientStatDataManager();

  /**
   * Private constructor to enforce singleton pattern.
   */
  private ClientStatDataManager() {
    super();
  }
}
