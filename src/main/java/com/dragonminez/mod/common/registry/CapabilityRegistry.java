package com.dragonminez.mod.common.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.Reference.CapType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityRegistry {

  @SubscribeEvent
  public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (!(event.getObject() instanceof Player)) {
      return;
    }
    CapType.holders().forEach(event::addCapability);
  }
}
