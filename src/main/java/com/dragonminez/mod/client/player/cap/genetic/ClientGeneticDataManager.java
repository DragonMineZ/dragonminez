package com.dragonminez.mod.client.player.cap.genetic;

import com.dragonminez.mod.common.player.cap.genetic.GeneticData;
import com.dragonminez.mod.common.player.cap.genetic.GeneticDataManager;

/**
 * Client-side manager for accessing and updating {@link GeneticData} via Forge's capability system.
 * <p>
 * This subclass of {@link GeneticDataManager} is intended specifically for use on the client side.
 * It allows access to genetic-related player data and follows the singleton pattern for centralized access.
 * Use {@link ClientGeneticDataManager#INSTANCE} to retrieve the instance.
 */
public class ClientGeneticDataManager extends GeneticDataManager {

  /**
   * Singleton instance of the client-side genetic data manager.
   */
  public static final ClientGeneticDataManager INSTANCE = new ClientGeneticDataManager();

  /**
   * Private constructor to enforce singleton pattern.
   */
  private ClientGeneticDataManager() {
    super();
  }
}
