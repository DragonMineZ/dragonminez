package com.dragonminez.mod.common.player.cap.genetic;

import com.dragonminez.mod.common.player.cap.stat.StatData;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central manager for accessing and updating {@link StatData} on players via Forge's capability
 * system.
 * <p>
 * Serves as a common superclass for both client- and server-specific stat managers. Intended to be
 * accessed statically via {@link GeneticDataManager#INSTANCE}.
 */
public class GeneticDataManager extends CapDataManager<GeneticData> {

  /**
   * Global shared instance of the stat manager.
   */
  public static GeneticDataManager INSTANCE = new GeneticDataManager();

  /**
   * Protected constructor to restrict instantiation (supports singleton use).
   */
  protected GeneticDataManager() {
    super(CapabilityManager.get(new CapabilityToken<>() {
    }));
  }

  @Override
  public GeneticData buildCap() {
    return new GeneticData();
  }
}
