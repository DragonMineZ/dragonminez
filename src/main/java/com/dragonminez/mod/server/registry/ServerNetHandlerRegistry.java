package com.dragonminez.mod.server.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.network.event.RegisterNetHandlerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.simple.SimpleChannel;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class ServerNetHandlerRegistry {

  public static void init() {
    MinecraftForge.EVENT_BUS.addListener(ServerNetHandlerRegistry::onServerRegistry);
  }

  @SubscribeEvent
  public static void onServerRegistry(RegisterNetHandlerEvent event) {
    if (!event.isEarly()) {
      return;
    }
    final SimpleChannel channel = event.channel();
  }
}
