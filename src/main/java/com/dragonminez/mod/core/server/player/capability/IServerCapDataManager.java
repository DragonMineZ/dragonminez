package com.dragonminez.mod.core.server.player.capability;

import com.dragonminez.mod.common.network.NetworkManager;
import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.core.common.network.IPacket;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import com.dragonminez.mod.core.common.player.capability.CapDataType;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IServerCapDataManager<M extends CapDataManager<D>, D extends CapDataHolder> {

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

  default void modifyStat(M manager, ServerPlayer player, CapDataType type, Consumer<D> consumer) {
    manager.retrieveStatData(player, consumer);
    consumer.andThen(data -> this.sendUpdate(player, data, type.isPublic()));
  }

  default void sendUpdate(ServerPlayer player, D data, boolean isPublic) {
    if (isPublic) {
      NetworkManager.INSTANCE.sendToTracking(player,
          this.buildSyncPacket(player, data, true));
    }
    NetworkManager.INSTANCE.sendToPlayer(this.buildSyncPacket(player, data, false), player);
  }

  default IPacket buildSyncPacket(Player player, D data, boolean isPublic) {
    return null;
  }
}
