package com.dragonminez.mod.core.server.player.capability;

import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.common.util.NetUtil;
import com.dragonminez.mod.core.common.network.Packet;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import com.dragonminez.mod.core.common.player.capability.CapDataType;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Interface to assist in managing and syncing capability data for players on the server.
 * <p>
 * Implementations are responsible for handling data changes, syncing with the client,
 * and optionally logging changes.
 *
 * @param <M> The manager type for this capability
 * @param <D> The capability data holder type
 */
public interface IServerCapDataManager<M extends CapDataManager<D>, D extends CapDataHolder> {

  /**
   * Modifies the specified capability data for a player and optionally logs the change.
   *
   * @param manager      The capability manager
   * @param player       The player whose data is being modified
   * @param type         The type of capability being modified
   * @param value        The new value being set (used for logging)
   * @param dataConsumer The operation to apply to the data
   * @param log          Whether to log this change to the server console
   */
  default void setStatInternal(M manager, ServerPlayer player, CapDataType type, Object value,
      Consumer<D> dataConsumer, boolean log) {
    this.modifyStat(manager, player, type, data -> {
      dataConsumer.accept(data);
      if (log) {
        LogUtil.info("{} set to {} for player {}", type.legibleId(), value,
            player.getName().getString());
      }
    });
  }

  /**
   * Retrieves and modifies a player's capability data, then sends an update packet.
   *
   * @param manager  The capability manager
   * @param player   The player whose data is being modified
   * @param type     The type of capability being modified
   * @param consumer The modification logic to apply to the data
   */
  default void modifyStat(M manager, ServerPlayer player, CapDataType type, Consumer<D> consumer) {
    manager.retrieveStatData(player, consumer);
    consumer.andThen(data -> this.sendUpdate(player, data, type.isPublic()));
  }

  /**
   * Sends a network update packet to the player and optionally to others tracking the player.
   *
   * @param player    The player whose data changed
   * @param data      The data to sync
   * @param isPublic  Whether to send the update to tracking players
   */
  default void sendUpdate(ServerPlayer player, D data, boolean isPublic) {
    if (isPublic) {
      NetUtil.sendToTracking(player,
          this.buildSyncPacket(player, data, true));
    }
    NetUtil.sendToPlayer(this.buildSyncPacket(player, data, false), player);
  }

  /**
   * Constructs a sync packet to update client-side data.
   *
   * @param player    The player whose data is being synced
   * @param data      The capability data to send
   * @param isPublic  Whether the data is visible to others
   * @return An {@link Packet} ready to send to the client
   */
  default Packet buildSyncPacket(Player player, D data, boolean isPublic) {
    return null;
  }
}
