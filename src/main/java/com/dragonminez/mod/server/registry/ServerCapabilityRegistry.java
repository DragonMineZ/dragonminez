package com.dragonminez.mod.server.registry;

import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataType;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataType;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataType;
import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;
import com.dragonminez.mod.server.player.combat.ServerCombatDataManager;
import com.dragonminez.mod.server.player.genetic.ServerGeneticDataManager;
import com.dragonminez.mod.server.player.stat.ServerStatDataManager;

public class ServerCapabilityRegistry {

  public static void init() {
    CapManagerRegistry.register(GeneticDataType.ID, ServerGeneticDataManager.INSTANCE);
    CapManagerRegistry.register(StatDataType.ID, ServerStatDataManager.INSTANCE);
    CapManagerRegistry.register(CombatDataType.ID, ServerCombatDataManager.INSTANCE);
  }
}
