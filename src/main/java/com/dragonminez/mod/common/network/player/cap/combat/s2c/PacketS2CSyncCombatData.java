package com.dragonminez.mod.common.network.player.cap.combat.s2c;

import com.dragonminez.mod.common.player.cap.combat.CombatData;
import com.dragonminez.mod.core.common.network.capability.CapSyncPacket;
import net.minecraft.network.FriendlyByteBuf;

public class PacketS2CSyncCombatData extends CapSyncPacket<CombatData> {

  public PacketS2CSyncCombatData(CombatData rawValue, int playerId) {
    super(rawValue, playerId);
  }

  public PacketS2CSyncCombatData(FriendlyByteBuf buf) {
    super(buf);
  }

  @Override
  public CombatData createInstance() {
    return new CombatData();
  }

  @Override
  public boolean serializePlayerId() {
    return true;
  }
}
