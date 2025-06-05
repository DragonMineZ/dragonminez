package com.dragonminez.mod.client.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.network.event.RegisterNetHandlerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.simple.SimpleChannel;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = Bus.FORGE)
public class ClientNetHandlerRegistry {

  public static void init() {
    MinecraftForge.EVENT_BUS.addListener(ClientNetHandlerRegistry::onClientRegistry);
  }

  @SubscribeEvent
  public static void onClientRegistry(RegisterNetHandlerEvent event) {
    if (!event.isEarly()) {
      return;
    }
    final SimpleChannel channel = event.channel();
  }
}
