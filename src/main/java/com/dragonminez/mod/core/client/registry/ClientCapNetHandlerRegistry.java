package com.dragonminez.mod.core.client.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.core.client.network.capability.PacketHandlerS2CCapSync;
import com.dragonminez.mod.core.common.network.NetworkManager;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.core.common.network.event.RegisterNetHandlerEvent;
import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Client-side registry responsible for handling the registration of capability synchronization
 * packets during the late phase of {@link RegisterNetHandlerEvent}.
 * <p>
 * This ensures that client-specific handlers are wired correctly to handle server-to-client sync
 * packets for various capabilities.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class ClientCapNetHandlerRegistry {

  /**
   * Initializes this registry by subscribing to the Forge event bus. Should be called during client
   * mod initialization.
   */
  public static void init() {
    MinecraftForge.EVENT_BUS.addListener(ClientCapNetHandlerRegistry::onClientRegistry);
  }

  /**
   * Listens to the {@link RegisterNetHandlerEvent} and registers network handlers for each
   * capability that supports server-to-client synchronization.
   *
   * @param event the network handler registration event
   */
  @SubscribeEvent
  @SuppressWarnings("unchecked")
  public static void onClientRegistry(RegisterNetHandlerEvent event) {
    if (!event.isLate()) {
      return;
    }

    final SimpleChannel channel = event.channel();
    CapManagerRegistry.managers(Dist.CLIENT).forEach((location,
        capDataManager) -> {
      // Try to register private sync packet
      final PacketS2CCapSync<?> privatePacket = capDataManager.buildMockSyncPacket(false);
      if (privatePacket != null) {
        register(channel, location, (Class<PacketS2CCapSync<?>>) privatePacket.getClass(),
            privatePacket);
        return;
      }

      // Fallback to public sync packet
      final PacketS2CCapSync<?> publicPacket = capDataManager.buildMockSyncPacket(true);
      if (publicPacket != null) {
        register(channel, location, (Class<PacketS2CCapSync<?>>) publicPacket.getClass(),
            publicPacket);
        return;
      }

      LogUtil.debug("Skipping network registration for {} as it has no sync packet",
          location.toString());
    });
  }

  /**
   * Registers a sync packet handler to the given network channel.
   *
   * @param channel  the channel to register to
   * @param id  the manager id associated with the packet
   * @param clazz    the packet class
   * @param instance an instance of the packet used for decoding
   */
  public static void register(SimpleChannel channel,
      ResourceLocation id,
      Class<PacketS2CCapSync<?>> clazz,
      PacketS2CCapSync<?> instance) {
    channel.messageBuilder(clazz, NetworkManager.INSTANCE.assignId(),
            NetworkDirection.PLAY_TO_CLIENT)
        .decoder(instance::decodeAndReturn)
        .encoder(PacketS2CCapSync::encode)
        .consumerMainThread((packetS2CCapSync, contextSupplier) ->
            PacketHandlerS2CCapSync.handle(id, packetS2CCapSync, contextSupplier))
        .add();

    LogUtil.info("Registered capability network handler {}", clazz.getSimpleName());
  }
}
