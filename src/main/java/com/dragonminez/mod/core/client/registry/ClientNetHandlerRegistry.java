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
 * Client-side network handler registry.
 * <p>
 * This class is responsible for registering all client-side network handlers during the
 * {@link RegisterNetHandlerEvent}. In particular, it automatically registers handlers for
 * packets related to client-visible capability synchronization, but may also register any
 * additional client-only packets in the future.
 * <p>
 * Call {@link #init()} during client mod initialization to activate.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public class ClientNetHandlerRegistry {

  /**
   * Subscribes this registry to the Forge event bus.
   * <p>
   * Should be called from client mod initialization to ensure all relevant handlers are registered.
   */
  public static void init() {
    MinecraftForge.EVENT_BUS.addListener(ClientNetHandlerRegistry::onClientRegistry);
  }

  /**
   * Invoked during {@link RegisterNetHandlerEvent}. If the event is in the late phase,
   * registers all necessary client-side packet handlers, including capability sync packets
   * and other types.
   *
   * @param event the network handler registration event
   */
  @SubscribeEvent
  public static void onClientRegistry(RegisterNetHandlerEvent event) {
    if (!event.isLate()) {
      return;
    }

    final SimpleChannel channel = event.channel();
    registerAllClientHandlers(channel);
  }

  /**
   * Registers all known client-side packet handlers to the given channel.
   * <p>
   * This includes dynamic capability sync packets as well as other static or manually
   * defined packets that need to be handled on the client.
   *
   * @param channel the network channel to register to
   */
  public static void registerAllClientHandlers(SimpleChannel channel) {
    registerCapHandlers(channel);

    // Register additional client-side packets here as needed:
    // channel.messageBuilder(...).decoder(...).encoder(...).consumerMainThread(...).add();
  }

  /**
   * Registers dynamic handlers for all capabilities that provide a {@link PacketS2CCapSync}
   * implementation on the client side.
   *
   * @param channel the network channel to register capability sync packets to
   */
  @SuppressWarnings("unchecked")
  public static void registerCapHandlers(SimpleChannel channel) {
    CapManagerRegistry.INSTANCE.values(Dist.CLIENT).forEach(capDataManager -> {
      final PacketS2CCapSync<?> privatePacket = capDataManager.buildMockSyncPacket(false);
      if (privatePacket != null) {
        registerCapHandler(channel, capDataManager.id(),
            (Class<PacketS2CCapSync<?>>) privatePacket.getClass(), privatePacket);
        return;
      }

      final PacketS2CCapSync<?> publicPacket = capDataManager.buildMockSyncPacket(true);
      if (publicPacket != null) {
        registerCapHandler(channel, capDataManager.id(),
            (Class<PacketS2CCapSync<?>>) publicPacket.getClass(), publicPacket);
        return;
      }

      LogUtil.debug("Skipping network registration for {} as it has no sync packet",
          capDataManager.id());
    });
  }

  /**
   * Registers a client-side capability sync handler.
   *
   * @param channel  the network channel
   * @param id       the capability manager ID
   * @param clazz    the packet class
   * @param instance a mock packet instance to provide the decoder
   */
  public static void registerCapHandler(SimpleChannel channel,
      ResourceLocation id,
      Class<PacketS2CCapSync<?>> clazz,
      PacketS2CCapSync<?> instance) {
    channel.messageBuilder(clazz, NetworkManager.INSTANCE.assignId(), NetworkDirection.PLAY_TO_CLIENT)
        .decoder(instance::decodeAndReturn)
        .encoder(PacketS2CCapSync::encode)
        .consumerMainThread((packet, context) ->
            PacketHandlerS2CCapSync.handle(id, packet, context))
        .add();

    LogUtil.info("Registered capability network handler {}", clazz.getSimpleName());
  }
}
