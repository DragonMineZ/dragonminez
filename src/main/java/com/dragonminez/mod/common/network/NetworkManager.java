package com.dragonminez.mod.common.network;

import com.dragonminez.mod.client.network.NetworkClientManager;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.server.network.NetworkServerManager;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkManager {

  public static final NetworkManager INSTANCE = new NetworkManager();
  private SimpleChannel channel;
  private int packetId = 0;

  public void init() {
    this.channel = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(Reference.MOD_ID))
        .networkProtocolVersion(() -> Reference.VERSION)
        .clientAcceptedVersions(s -> true)
        .serverAcceptedVersions(s -> true)
        .simpleChannel();
    NetworkServerManager.INSTANCE.init(channel);
    NetworkClientManager.INSTANCE.init(channel);
  }

  public <MSG> void sendToServer(MSG message) {
    if (message == null) {
      return;
    }
    this.retrieveChannel(simpleChannel -> simpleChannel.sendToServer(message));
  }

  public <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
    if (message == null) {
      return;
    }
    this.retrieveChannel(simpleChannel -> simpleChannel.send(PacketDistributor.PLAYER.with(()
        -> player), message));
  }

  public <MSG> void sendToAll(MSG message) {
    if (message == null) {
      return;
    }
    this.retrieveChannel(
        simpleChannel -> simpleChannel.send(PacketDistributor.ALL.noArg(), message));
  }

  public <MSG> void sendToTracking(ServerPlayer player, MSG message) {
    if (message == null) {
      return;
    }
    this.retrieveChannel(simpleChannel ->
        simpleChannel.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), message));
  }

  public <MSG> void sendToTrackingAndSelf(ServerPlayer player, MSG message) {
    if (message == null) {
      return;
    }
    this.retrieveChannel(simpleChannel ->
        simpleChannel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message));
  }

  private void retrieveChannel(Consumer<SimpleChannel> consumer) {
    if (this.channel == null) {
      LogUtil.error(
          "Network channel is not registered. Please register the channel before using it.");
      return;
    }
    consumer.accept(this.channel);
  }

  public int assignId() {
    final int id = this.packetId;
    this.packetId++;
    return id;
  }
}
