package com.dragonminez.mod.common.util;

import com.dragonminez.mod.common.registry.NetworkRegistry;
import com.dragonminez.mod.core.common.network.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class NetUtil {

  public static void sendToServer(Packet packet) {
    if (packet == null) {
      return;
    }
    NetworkRegistry.retrieveChannel(simpleChannel -> simpleChannel.sendToServer(packet));
  }

  public static void sendToPlayer(Packet message, ServerPlayer player) {
    if (message == null) {
      return;
    }
    NetworkRegistry.retrieveChannel(
        simpleChannel -> simpleChannel.send(PacketDistributor.PLAYER.with(()
            -> player), message));
  }

  public static void sendToAll(Packet message) {
    if (message == null) {
      return;
    }
    NetworkRegistry.retrieveChannel(
        simpleChannel -> simpleChannel.send(PacketDistributor.ALL.noArg(), message));
  }

  public static void sendToTracking(ServerPlayer player, Packet message) {
    if (message == null) {
      return;
    }
    NetworkRegistry.retrieveChannel(simpleChannel ->
        simpleChannel.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), message));
  }

  public void sendToTrackingAndSelf(ServerPlayer player, Packet message) {
    if (message == null) {
      return;
    }
    NetworkRegistry.retrieveChannel(simpleChannel ->
        simpleChannel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message));
  }
}
