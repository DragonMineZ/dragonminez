package com.dragonminez.core.common.util;

import com.dragonminez.core.common.network.NetworkManager;
import com.dragonminez.core.common.network.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Utility class for sending packets through the mod's network channel.
 * <p>
 * Provides convenience methods for targeting common recipient scopes such as the server,
 * a specific player, all players, or players tracking a given entity.
 */
public class NetUtil {

  /**
   * Sends a packet from client to server.
   *
   * @param packet the packet to send
   */
  public static void sendToServer(Packet packet) {
    if (packet == null) {
      return;
    }
    NetworkManager.INSTANCE.retrieveChannel(
        simpleChannel -> simpleChannel.sendToServer(packet));
  }

  /**
   * Sends a packet from server to a specific player.
   *
   * @param message the packet to send
   * @param player the player to send the packet to
   */
  public static void sendToPlayer(Packet message, ServerPlayer player) {
    if (message == null) {
      return;
    }
    NetworkManager.INSTANCE.retrieveChannel(
        simpleChannel -> simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), message));
  }

  /**
   * Sends a packet from server to all players.
   *
   * @param message the packet to broadcast
   */
  public static void sendToAll(Packet message) {
    if (message == null) {
      return;
    }
    NetworkManager.INSTANCE.retrieveChannel(
        simpleChannel -> simpleChannel.send(PacketDistributor.ALL.noArg(), message));
  }

  /**
   * Sends a packet to all players tracking the given player (e.g., those in the same chunk area).
   *
   * @param player the player being tracked
   * @param message the packet to send
   */
  public static void sendToTracking(ServerPlayer player, Packet message) {
    if (message == null) {
      return;
    }
    NetworkManager.INSTANCE.retrieveChannel(
        simpleChannel -> simpleChannel.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), message));
  }

  /**
   * Sends a packet to all players tracking the given player, including the player themselves.
   *
   * @param player the player being tracked and included
   * @param message the packet to send
   */
  public static void sendToTrackingAndSelf(ServerPlayer player, Packet message) {
    if (message == null) {
      return;
    }
    NetworkManager.INSTANCE.retrieveChannel(
        simpleChannel -> simpleChannel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message));
  }
}
