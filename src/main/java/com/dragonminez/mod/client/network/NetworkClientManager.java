package com.dragonminez.mod.client.network;

import com.dragonminez.mod.client.network.player.stat.handler.PacketHandlerS2CSyncStat;
import com.dragonminez.mod.common.network.NetworkManager;
import com.dragonminez.mod.common.network.player.stat.s2c.PacketS2CSyncStat;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkClientManager {

  public static final NetworkClientManager INSTANCE = new NetworkClientManager();

  public void init(SimpleChannel channel) {
    channel.messageBuilder(PacketS2CSyncStat.class, NetworkManager.INSTANCE.assignId(),
            NetworkDirection.PLAY_TO_CLIENT)
        .decoder(PacketS2CSyncStat::new)
        .encoder(PacketS2CSyncStat::encode)
        .consumerMainThread(PacketHandlerS2CSyncStat::handle)
        .add();
  }
}
