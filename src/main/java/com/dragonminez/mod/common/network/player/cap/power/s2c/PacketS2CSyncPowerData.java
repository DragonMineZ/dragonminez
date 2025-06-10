package com.dragonminez.mod.common.network.player.cap.power.s2c;

import com.dragonminez.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.common.player.cap.power.PowerData;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public class PacketS2CSyncPowerData extends PacketS2CCapSync<PowerData> {

  public PacketS2CSyncPowerData(PowerData rawValue, UUID playerId) {
    super(rawValue, playerId);
  }

  public PacketS2CSyncPowerData(FriendlyByteBuf buf) {
    super(buf);
  }

  @Override
  public PowerData createDataInstance() {
    return new PowerData();
  }

  @Override
  public boolean serializePlayerId() {
    return false;
  }
}
