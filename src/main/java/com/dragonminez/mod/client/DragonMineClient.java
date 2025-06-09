package com.dragonminez.mod.client;

import com.dragonminez.mod.client.registry.ClientCapabilityRegistry;
import com.dragonminez.mod.client.registry.ClientNetHandlerRegistry;
import com.dragonminez.mod.client.registry.ClientKeybindRegistry;
import com.dragonminez.mod.common.Reference;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.dragonminez.mod.common.DragonMineZ;

/**
 * The main client class for the DragonMineZ mod.
 *
 * @see DragonMineZ
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DragonMineClient {

  public static void init() {
    DragonMineClient.registry();
  }

  @SubscribeEvent
  public static void onClientStarting(FMLClientSetupEvent event) {
  }

  private static void registry() {
    ClientCapabilityRegistry.init();
    ClientNetHandlerRegistry.init();
    ClientKeybindRegistry.init();
  }
}