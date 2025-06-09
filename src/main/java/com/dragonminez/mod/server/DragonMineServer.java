package com.dragonminez.mod.server;

import com.dragonminez.mod.common.DragonMineZ;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.server.registry.ServerCapabilityRegistry;
import com.dragonminez.mod.server.registry.ServerKeybindRegistry;
import com.dragonminez.mod.server.registry.ServerNetHandlerRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

/**
 * The main server class for the DragonMineZ mod.
 *
 * @see DragonMineZ
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class DragonMineServer {

  public static void init() {
    DragonMineServer.registry();
  }

  @SubscribeEvent
  public static void onServerStarting(FMLDedicatedServerSetupEvent event) {
  }

  private static void registry() {
    ServerKeybindRegistry.init();
    ServerCapabilityRegistry.init();
    ServerNetHandlerRegistry.init();
  }
}