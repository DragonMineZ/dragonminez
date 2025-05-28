package com.dragonminez.mod.client.network;

import com.dragonminez.mod.client.network.player.stat.handler.PacketHandlerS2CSyncStat;
import com.dragonminez.mod.common.registry.NetworkRegistry;
import com.dragonminez.mod.common.network.player.cap.stat.s2c.PacketS2CSyncStat;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public class ClientNetHandlerRegistry {

  public static void init(SimpleChannel channel) {
    channel.messageBuilder(PacketS2CSyncStat.class, NetworkRegistry.assignId(),
            NetworkDirection.PLAY_TO_CLIENT)
        .decoder(PacketS2CSyncStat::new)
        .encoder(PacketS2CSyncStat::encode)
        .consumerMainThread(PacketHandlerS2CSyncStat::handle)
        .add();
  }
}
