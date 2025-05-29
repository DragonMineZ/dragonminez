package com.dragonminez.mod.common.network.player.cap.stat.s2c;

import com.dragonminez.mod.common.player.cap.stat.StatData;
import com.dragonminez.mod.core.common.network.capability.CapSyncPacket;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Full stat synchronization packet sent from server to client.
 * <p>
 * This includes values from {@link StatData}. This packet is
 * only sent to the owning client and not to nearby players.
 */
public class PacketS2CSyncStat extends CapSyncPacket<StatData> {
  public PacketS2CSyncStat(StatData data) {
    super(data);
  }

  public PacketS2CSyncStat(FriendlyByteBuf buf){
    super(buf);
  }

  @Override
  public StatData createInstance() {
    return new StatData();
  }

  @Override
  public boolean serializePlayerId() {
    return false;
  }
}
