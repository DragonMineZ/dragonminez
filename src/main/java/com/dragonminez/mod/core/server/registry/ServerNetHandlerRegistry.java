package com.dragonminez.mod.core.server.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.network.NetworkManager;
import com.dragonminez.mod.core.common.network.event.RegisterNetHandlerEvent;
import com.dragonminez.mod.core.common.network.keybind.PacketC2SKeyPressed;
import com.dragonminez.mod.core.server.network.PacketHandlerC2SKeyPressed;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server-side registry responsible for handling the registration of network packet handlers during
 * the late phase of {@link RegisterNetHandlerEvent}.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class ServerNetHandlerRegistry {

  /**
   * Initializes this registry by subscribing to the Forge event bus.
   */
  public static void init() {
    MinecraftForge.EVENT_BUS.addListener(ServerNetHandlerRegistry::onServerRegistry);
  }

  /**
   * Handles the {@link RegisterNetHandlerEvent} during its late phase. Registers all server-side
   * packet handlers.
   *
   * @param event the network handler registration event
   */
  @SubscribeEvent
  public static void onServerRegistry(RegisterNetHandlerEvent event) {
    if (!event.isLate()) {
      return;
    }

    final SimpleChannel channel = event.channel();
    channel.messageBuilder(PacketC2SKeyPressed.class, NetworkManager.INSTANCE.assignId(),
            NetworkDirection.PLAY_TO_CLIENT)
        .decoder(PacketC2SKeyPressed::new)
        .encoder(PacketC2SKeyPressed::encode)
        .consumerMainThread(PacketHandlerC2SKeyPressed::handle)
        .add();
  }
}
