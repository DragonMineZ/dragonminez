package com.dragonminez.mod.client.registry;

import com.dragonminez.mod.client.player.cap.combat.ClientCombatDataManager;
import com.dragonminez.mod.client.player.cap.genetic.ClientGeneticDataManager;
import com.dragonminez.mod.client.player.cap.power.ClientPowerDataManager;
import com.dragonminez.mod.client.player.cap.progress.ClientProgressDataManager;
import com.dragonminez.mod.client.player.cap.stat.ClientStatDataManager;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataHolder;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataHolder;
import com.dragonminez.mod.common.player.cap.power.PowerData.PowerDataHolder;
import com.dragonminez.mod.common.player.cap.progress.ProgressData.ProgressDataHolder;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataHolder;
import com.dragonminez.core.common.player.capability.CapManagerRegistry;
import net.minecraftforge.api.distmarker.Dist;

public class ClientCapabilityRegistry {

  public static void init() {
    CapManagerRegistry.INSTANCE.register(Dist.CLIENT, GeneticDataHolder.ID,
        ClientGeneticDataManager.INSTANCE);
    CapManagerRegistry.INSTANCE.register(Dist.CLIENT, StatDataHolder.ID,
        ClientStatDataManager.INSTANCE);
    CapManagerRegistry.INSTANCE.register(Dist.CLIENT, CombatDataHolder.ID,
        ClientCombatDataManager.INSTANCE);
    CapManagerRegistry.INSTANCE.register(Dist.CLIENT, ProgressDataHolder.ID,
        ClientProgressDataManager.INSTANCE);
    CapManagerRegistry.INSTANCE.register(Dist.CLIENT, PowerDataHolder.ID,
        ClientPowerDataManager.INSTANCE);
  }
}
