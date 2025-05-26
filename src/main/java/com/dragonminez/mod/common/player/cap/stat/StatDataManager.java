package com.dragonminez.mod.common.player.cap.stat;

import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central manager for accessing and updating {@link StatData} on players via Forge's capability
 * system.
 * <p>
 * Serves as a common superclass for both client- and server-specific stat managers. Intended to be
 * accessed statically via {@link StatDataManager#INSTANCE}.
 */
public class StatDataManager extends CapDataManager<StatData> {

  /**
   * Global shared instance of the stat manager.
   */
  public static StatDataManager INSTANCE = new StatDataManager();

  /**
   * Protected constructor to restrict instantiation (supports singleton use).
   */
  protected StatDataManager() {
    super(CapabilityManager.get(new CapabilityToken<>() {
    }));
  }

  @Override
  public StatData buildCap() {
    return new StatData();
  }
}
