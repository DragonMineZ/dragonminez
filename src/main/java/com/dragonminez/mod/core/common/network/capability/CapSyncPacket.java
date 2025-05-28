package com.dragonminez.mod.core.common.network.capability;

import com.dragonminez.mod.core.common.network.Packet;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public abstract class CapSyncPacket<V extends CapDataHolder> extends Packet {

  private CompoundTag encodedValue;
  private Integer playerId;

  public CapSyncPacket(V rawValue, Integer playerId) {
    this.encodedValue = rawValue.serialize(new CompoundTag());
    this.playerId = playerId;
  }

  public CapSyncPacket(V rawValue) {
    this(rawValue, null);
  }

  public CapSyncPacket(FriendlyByteBuf buf) {
    super(buf);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeNbt(this.encodedValue);
    if (!serializePlayerId()) {
      return;
    }
    buffer.writeInt(this.playerId);
  }

  @Override
  public void decode(FriendlyByteBuf buf) {
    this.encodedValue = buf.readNbt();
    if (!serializePlayerId()) {
      return;
    }
    this.playerId = buf.readInt();
  }

  public abstract V createInstance();

  public V data() {
    final V emptyData = this.createInstance();
    emptyData.deserialize(this.encodedValue);
    return emptyData;
  }

  public int playerId() {
    return this.playerId;
  }

  @SuppressWarnings("all")
  public abstract boolean serializePlayerId();
}