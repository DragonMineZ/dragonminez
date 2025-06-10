package com.dragonminez.mod.common.network.player.cap.progress.s2c;

import com.dragonminez.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.common.player.cap.progression.ProgressData;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public class PacketS2CSyncProgressData extends PacketS2CCapSync<ProgressData> {

  public PacketS2CSyncProgressData(ProgressData rawValue, UUID playerId) {
    super(rawValue, playerId);
  }

  public PacketS2CSyncProgressData(FriendlyByteBuf buf) {
    super(buf);
  }

  @Override
  public ProgressData createDataInstance() {
    return new ProgressData();
  }

  @Override
  public boolean serializePlayerId() {
    return false;
  }
}
