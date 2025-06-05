package com.dragonminez.mod.core.server.player.capability;

import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.common.util.NetUtil;
import com.dragonminez.mod.core.common.network.Packet;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import com.dragonminez.mod.core.common.player.capability.CapDataType;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Interface to assist in managing and syncing capability data for players on the server.
 * <p>
 * Implementations are responsible for handling data changes, syncing with the client, and
 * optionally logging changes.
 *
 * @param <M> The manager type for this capability
 * @param <D> The capability data holder type
 */
public interface IServerCapDataManager<M extends CapDataManager<D>, D extends CapDataHolder> {

  /**
   * Modifies the specified capability data for a player and optionally logs the change.
   *
   * @param player       The player whose data is being modified
   * @param type         The type of capability being modified
   * @param value        The new value being set (used for logging)
   * @param dataConsumer The operation to apply to the data
   * @param log          Whether to log this change to the server console
   */
  default void setStatInternal(ServerPlayer player, CapDataType type, Object value,
      Consumer<D> dataConsumer, boolean log) {
    this.modifyStat(player, type, data -> {
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
   * @param player   The player whose data is being modified
   * @param type     The type of capability being modified
   * @param consumer The modification logic to apply to the data
   */
  default void modifyStat(ServerPlayer player, CapDataType type, Consumer<D> consumer) {
    this.manager().retrieveData(player, consumer);
    consumer.andThen(data -> this.sendUpdate(player, data, type.isPublic()));
  }

  /**
   * Sends a capability data update packet to a player.
   * <p>
   * This method retrieves the capability data of the target player and sends it to the receiver. If
   * the receiver is {@code null}, the target is used as the receiver (i.e., the player will receive
   * their own data).
   *
   * @param receiver the player who will receive the update packet; if {@code null}, the target will
   *                 receive it instead
   * @param target   the player whose capability data will be extracted and sent
   */
  default void sendUpdate(ServerPlayer receiver, ServerPlayer target) {
    final D data = this.manager().retrieveData(target);
    if (receiver == null) {
      receiver = target;
    }
    final boolean isSamePlayer = receiver.getUUID().equals(target.getUUID());
    NetUtil.sendToPlayer(this.buildSyncPacket(isSamePlayer ? receiver : target, data, false), receiver);
  }

  /**
   * Sends a network update packet to the player and optionally to others tracking the player.
   *
   * @param player   The player whose data changed
   * @param data     The data to sync
   * @param isPublic Whether to send the update to tracking players
   */
  default void sendUpdate(ServerPlayer player, D data, boolean isPublic) {
    if (isPublic) {
      NetUtil.sendToTracking(player,
          this.buildSyncPacket(player, data, true));
    }
    NetUtil.sendToPlayer(this.buildSyncPacket(player, data, false), player);
  }

  /**
   * Provides the associated capability manager.
   * <p>
   * This has to be done this way because of the way wildcard generics work in Java.
   */
  M manager();

  /**
   * Constructs a sync packet to update client-side data.
   * <p>
   * All parameters and the return value can be {@code null}.
   *
   * @param player   The player whose data is being synced, or {@code null}
   * @param data     The capability data to send, or {@code null}
   * @param isPublic Whether the data is visible to others (can be {@code null})
   * @return An {@link Packet} ready to send to the client, or {@code null}
   */
  PacketS2CCapSync<D> buildSyncPacket(@Nullable Player player, @Nullable D data,
      @Nullable Boolean isPublic);


  /**
   * Builds a representative sync packet instance using all {@code null} parameters.
   * <p>
   * This method is used primarily during network registration to obtain a prototype
   * {@link PacketS2CCapSync} for setting up the decoder and encoder via
   * {@link net.minecraftforge.network.simple.SimpleChannel#messageBuilder}.
   * <p>
   * The returned packet does not represent actual data and is not intended for transmission, but
   * instead serves as a template to provide type and structure information.
   *
   * @return A {@link PacketS2CCapSync} with default {@code null} parameters, or {@code null} if the
   * capability does not support sync.
   */
  default PacketS2CCapSync<D> buildMockSyncPacket(boolean isPublic) {
    return this.buildSyncPacket(null, null, isPublic);
  }
}
