package com.dragonminez.mod.common.player.cap.combat;

import com.dragonminez.mod.common.player.cap.stat.StatData;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Central manager for accessing and updating {@link StatData} on players via Forge's capability
 * system.
 * <p>
 * Serves as a common superclass for both client- and server-specific stat managers. Intended to be
 * accessed statically via {@link CombatDataManager#INSTANCE}.
 */
public class CombatDataManager extends CapDataManager<CombatData> {

  /**
   * Global shared instance of the stat manager.
   */
  public static CombatDataManager INSTANCE = new CombatDataManager();

  /**
   * Protected constructor to restrict instantiation (supports singleton use).
   */
  protected CombatDataManager() {
    super(CapabilityManager.get(new CapabilityToken<>() {
    }));
  }

  @Override
  public CombatData buildCap() {
    return new CombatData();
  }
}
