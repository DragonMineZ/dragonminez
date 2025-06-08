package com.dragonminez.mod.common.network.player.cap.combat.s2c;

import com.dragonminez.mod.common.player.cap.combat.CombatData;
import com.dragonminez.core.common.network.capability.PacketS2CCapSync;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public class PacketS2CSyncCombatData extends PacketS2CCapSync<CombatData> {

  public PacketS2CSyncCombatData(CombatData rawValue, UUID playerId) {
    super(rawValue, playerId);
  }

  public PacketS2CSyncCombatData(FriendlyByteBuf buf) {
    super(buf);
  }

  @Override
  public CombatData createDataInstance() {
    return new CombatData();
  }

  @Override
  public boolean serializePlayerId() {
    return true;
  }
}
