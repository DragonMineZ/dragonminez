package com.dragonminez.mod.client.registry;

import com.dragonminez.mod.client.player.cap.combat.ClientCombatDataManager;
import com.dragonminez.mod.client.player.cap.genetic.ClientGeneticDataManager;
import com.dragonminez.mod.client.player.cap.stat.ClientStatDataManager;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataType;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataType;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataType;
import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;

public class ClientCapabilityRegistry {

  public static void init() {
    CapManagerRegistry.register(GeneticDataType.ID, ClientGeneticDataManager.INSTANCE);
    CapManagerRegistry.register(StatDataType.ID, ClientStatDataManager.INSTANCE);
    CapManagerRegistry.register(CombatDataType.ID, ClientCombatDataManager.INSTANCE);
  }
}
