package com.dragonminez.mod.common.network.player.cap.genetic.s2c;

import com.dragonminez.mod.common.player.cap.genetic.GeneticData;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public class PacketS2CSyncGenetic extends PacketS2CCapSync<GeneticData> {
  public PacketS2CSyncGenetic(GeneticData rawValue, UUID playerId) {
    super(rawValue, playerId);
  }

  public PacketS2CSyncGenetic(FriendlyByteBuf buf){
    super(buf);
  }

  @Override
  public GeneticData createDataInstance() {
    return new GeneticData();
  }

  @Override
  public boolean serializePlayerId() {
    return true;
  }
}
