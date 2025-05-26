package com.dragonminez.mod.common.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataType;
import com.dragonminez.mod.common.player.cap.combat.CombatDataManager;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataType;
import com.dragonminez.mod.common.player.cap.genetic.GeneticDataManager;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataType;
import com.dragonminez.mod.common.player.cap.stat.StatDataManager;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityRegistry {

  private static final HashMap<ResourceLocation, CapDataManager<?>> HOLDERS = new HashMap<>();

  static {
    HOLDERS.put(GeneticDataType.ID, GeneticDataManager.INSTANCE);
    HOLDERS.put(StatDataType.ID, StatDataManager.INSTANCE);
    HOLDERS.put(CombatDataType.ID, CombatDataManager.INSTANCE);
  }

  @SubscribeEvent
  public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (!(event.getObject() instanceof Player)) {
      return;
    }
    CapabilityRegistry.holders().forEach(event::addCapability);
  }


  public static HashMap<ResourceLocation, CapDataManager<?>> holders() {
    return HOLDERS;
  }
}
