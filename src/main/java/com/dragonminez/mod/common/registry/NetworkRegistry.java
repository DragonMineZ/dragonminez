package com.dragonminez.mod.common.registry;

import com.dragonminez.mod.client.network.ClientNetHandlerRegistry;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.server.registry.ServerNetHandlerRegistry;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkRegistry {

  private static SimpleChannel channel;
  private static int packetId = 0;

  public static void init() {
    NetworkRegistry.channel = net.minecraftforge.network.NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(Reference.MOD_ID, "channel"))
        .networkProtocolVersion(() -> Reference.VERSION)
        .clientAcceptedVersions(s -> true)
        .serverAcceptedVersions(s -> true)
        .simpleChannel();
    ServerNetHandlerRegistry.init(channel);
    ClientNetHandlerRegistry.init(channel);
  }

  public static void retrieveChannel(Consumer<SimpleChannel> consumer) {
    if (NetworkRegistry.channel == null) {
      LogUtil.error(
          "Network channel is not registered. Please register the channel before using it.");
      return;
    }
    consumer.accept(NetworkRegistry.channel);
  }

  public static int assignId() {
    final int id = NetworkRegistry.packetId;
    NetworkRegistry.packetId++;
    return id;
  }
}
