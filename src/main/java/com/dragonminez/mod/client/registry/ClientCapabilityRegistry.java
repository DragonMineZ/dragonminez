package com.dragonminez.mod.client.registry;

import com.dragonminez.mod.client.player.cap.combat.ClientCombatDataManager;
import com.dragonminez.mod.client.player.cap.genetic.ClientGeneticDataManager;
import com.dragonminez.mod.client.player.cap.stat.ClientStatDataManager;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataHolder;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataHolder;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;
import net.minecraftforge.api.distmarker.Dist;

public class ClientCapabilityRegistry {

  public static void init() {
    CapManagerRegistry.register(Dist.CLIENT, GeneticDataHolder.ID, ClientGeneticDataManager.INSTANCE);
    CapManagerRegistry.register(Dist.CLIENT, StatDataHolder.ID, ClientStatDataManager.INSTANCE);
    CapManagerRegistry.register(Dist.CLIENT, CombatDataHolder.ID, ClientCombatDataManager.INSTANCE);
  }
}
