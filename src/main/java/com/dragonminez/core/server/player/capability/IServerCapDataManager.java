package com.dragonminez.core.server.player.capability;

import com.dragonminez.core.common.util.LogUtil;
import com.dragonminez.core.common.util.NetUtil;
import com.dragonminez.core.common.player.capability.CapDataHolder;
import com.dragonminez.core.common.player.capability.CapDataManager;
import com.dragonminez.core.common.player.capability.CapData;
import com.dragonminez.core.common.player.capability.ICap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side interface to manage and synchronize player capability data.
 * <p>
 * Provides methods for setting capability values, copying data between players, and sending data
 * updates to clients and players tracking them.
 *
 * @param <M> the type of capability manager used
 * @param <D> the type of capability data handled
 */
public interface IServerCapDataManager<M extends CapDataManager<D>, D extends ICap> {

  /**
   * Sets a capability value on a player and optionally logs the update.
   *
   * @param player the player whose capability is updated
   * @param key    the capability key identifying the data field
   * @param value  the new value to assign
   * @param log    whether to log the update action
   */
  default void set(ServerPlayer player, String key, Object value, boolean log) {
    final D cap = this.manager().retrieveData(player);
    if (cap == null) {
      return;
    }

    final CapDataHolder holder = cap.holder();
    final CapData<?, ?> data = holder.datas().get(key);
    if (data == null) {
      return;
    }

    data.set(cap, value);
    this.sendUpdate(player, cap, data.isPublic());
    if (log) {
      LogUtil.info("{} set to {} for player {}", data.legibleId(), value,
          player.getName().getString());
    }
  }

  /**
   * Copies all capability data from a reference player to a target player and syncs the target.
   *
   * @param reference the player to copy data from
   * @param target    the player to copy data to
   */
  default void update(ServerPlayer reference, ServerPlayer target) {
    this.manager().update(reference, target);
    this.sendUpdate(target, target);
  }

  /**
   * Sends the full capability data of a target player to a receiver player. If the receiver is
   * null, sends the data to the target player.
   *
   * @param receiver the player receiving the data, or null to send to the target
   * @param target   the player whose data is sent
   */
  default void sendUpdate(ServerPlayer receiver, ServerPlayer target) {
    final D data = this.manager().retrieveData(target);
    if (receiver == null) {
      receiver = target;
    }

    boolean isSamePlayer = receiver.getUUID().equals(target.getUUID());
    NetUtil.sendToPlayer(this.manager().buildSyncPacket(isSamePlayer ? receiver : target, data,
        false), receiver);
  }

  /**
   * Sends an update packet for capability data to the player, and if public, also sends to players
   * tracking them.
   *
   * @param player   the player whose data changed
   * @param data     the updated capability data
   * @param isPublic whether to send to tracking players as well
   */
  default void sendUpdate(ServerPlayer player, D data, boolean isPublic) {
    if (isPublic) {
      NetUtil.sendToTracking(player, this.manager().buildSyncPacket(player, data, true));
    }
    NetUtil.sendToPlayer(this.manager().buildSyncPacket(player, data, false), player);
  }

  /**
   * Gets the capability manager responsible for managing this data.
   *
   * @return the capability manager instance
   */
  M manager();
}
