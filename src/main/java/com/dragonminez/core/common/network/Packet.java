package com.dragonminez.core.common.network;

import net.minecraft.network.FriendlyByteBuf;

public abstract class Packet {

  public Packet() {
  }

  public Packet(FriendlyByteBuf buf) {
    this.decode(buf);
  }

  public abstract void encode(FriendlyByteBuf buf);

  public abstract void decode(FriendlyByteBuf buf);
}