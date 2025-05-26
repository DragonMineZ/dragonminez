package com.dragonminez.mod.common.player.cap.genetic;

import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central manager for accessing and updating {@link GeneticData} on players via Forge's capability system.
 * <p>
 * Acts as the main point of interaction for genetic data capabilities, serving as a singleton.
 * This class extends {@link CapDataManager} to provide capability management specific to {@link GeneticData}.
 * <p>
 * Access the global instance statically via {@link GeneticDataManager#INSTANCE}.
 */
public class GeneticDataManager extends CapDataManager<GeneticData> {

  /**
   * Singleton instance of the genetic data manager.
   */
  public static final GeneticDataManager INSTANCE = new GeneticDataManager();

  /**
   * Protected constructor to enforce singleton pattern.
   * Registers the capability using Forge's {@link CapabilityManager} with a generic {@link CapabilityToken}.
   */
  protected GeneticDataManager() {
    super(CapabilityManager.get(new CapabilityToken<GeneticData>() {}));
  }

  /**
   * Builds a new instance of {@link GeneticData}.
   *
   * @return a new {@link GeneticData} object.
   */
  @Override
  public GeneticData buildCap() {
    return new GeneticData();
  }
}
