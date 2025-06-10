package com.dragonminez.mod.client.player.cap.progress;

import com.dragonminez.mod.common.player.cap.progression.ProgressDataManager;
import com.dragonminez.mod.common.player.cap.progression.ProgressData;

/**
 * Client-side manager for accessing and updating {@link ProgressData}
 * via Forge's capability system.
 * <p>
 * This subclass of {@link ProgressDataManager} is specifically intended for use on the client side.
 * It provides access to progression-related player data and is designed as a singleton for centralized usage.
 * Use {@link ClientProgressDataManager#INSTANCE} to retrieve the instance.
 */
public class ClientProgressDataManager extends ProgressDataManager {

  /**
   * Singleton instance of the client-side progression data manager.
   */
  public static final ClientProgressDataManager INSTANCE = new ClientProgressDataManager();

  /**
   * Private constructor to enforce singleton pattern.
   */
  private ClientProgressDataManager() {
    super();
  }
}
