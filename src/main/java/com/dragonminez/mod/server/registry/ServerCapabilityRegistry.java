package com.dragonminez.mod.server.registry;

import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataHolder;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataHolder;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataHolder;
import com.dragonminez.core.common.player.capability.CapManagerRegistry;
import com.dragonminez.mod.server.player.combat.ServerCombatDataManager;
import com.dragonminez.mod.server.player.genetic.ServerGeneticDataManager;
import com.dragonminez.mod.server.player.stat.ServerStatDataManager;
import net.minecraftforge.api.distmarker.Dist;

public class ServerCapabilityRegistry {

  public static void init() {
    CapManagerRegistry.INSTANCE.register(Dist.DEDICATED_SERVER, GeneticDataHolder.ID,
        ServerGeneticDataManager.INSTANCE);
    CapManagerRegistry.INSTANCE.register(Dist.DEDICATED_SERVER, StatDataHolder.ID,
        ServerStatDataManager.INSTANCE);
    CapManagerRegistry.INSTANCE.register(Dist.DEDICATED_SERVER, CombatDataHolder.ID,
        ServerCombatDataManager.INSTANCE);
  }
}
