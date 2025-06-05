package com.dragonminez.mod.core.common.network.capability;

import com.dragonminez.mod.core.common.network.Packet;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Abstract base class for server-to-client capability synchronization packets.
 * <p>
 * This packet is used to transmit serialized capability data from the server to the client,
 * typically for syncing player-related data such as abilities, stats, or status effects.
 *
 * @param <V> the type of {@link CapDataHolder} being synchronized
 */
public abstract class PacketS2CCapSync<V extends CapDataHolder> extends Packet {

  private CompoundTag encodedValue;
  private UUID playerId;

  /**
   * Constructs a packet with the specified capability data and optional player ID.
   *
   * @param rawValue the capability data to serialize and send
   * @param playerId the UUID of the player this data is associated with (can be null)
   */
  public PacketS2CCapSync(V rawValue, UUID playerId) {
    if (rawValue == null) {
      return;
    }
    this.encodedValue = rawValue.serialize(new CompoundTag());
    this.playerId = playerId;
  }

  /**
   * Constructs a packet with capability data but no specific player ID.
   *
   * @param rawValue the capability data to serialize and send
   */
  public PacketS2CCapSync(V rawValue) {
    this(rawValue, null);
  }

  /**
   * Constructs a packet by deserializing data from the buffer.
   *
   * @param buf the buffer containing serialized packet data
   */
  public PacketS2CCapSync(FriendlyByteBuf buf) {
    super(buf);
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeNbt(this.encodedValue);
    if (!serializePlayerId()) {
      return;
    }
    buffer.writeUUID(this.playerId);
  }

  @Override
  public void decode(FriendlyByteBuf buf) {
    this.encodedValue = buf.readNbt();
    if (!serializePlayerId()) {
      return;
    }
    this.playerId = buf.readUUID();
  }

  /**
   * Decodes this packet and returns itself for chaining.
   *
   * @param buf the buffer containing serialized data
   * @return this packet instance with data populated
   */
  public PacketS2CCapSync<V> decodeAndReturn(FriendlyByteBuf buf) {
    this.decode(buf);
    return this;
  }

  /**
   * Creates an empty data instance of the capability type being synced.
   * <p>
   * This instance will be filled using {@link #encodedValue}.
   *
   * @return an empty instance of the capability data holder
   */
  public abstract V createDataInstance();

  /**
   * Reconstructs the capability data from the received tag.
   *
   * @return a populated instance of the capability data
   */
  public V data() {
    final V emptyData = this.createDataInstance();
    emptyData.deserialize(this.encodedValue, false);
    return emptyData;
  }

  /**
   * Returns the UUID of the player this capability data belongs to.
   *
   * @return the player UUID, or null if not present
   */
  public UUID playerId() {
    return this.playerId;
  }

  /**
   * Determines whether to include the player UUID in the encoded packet.
   *
   * @return true if the UUID should be serialized; false otherwise
   */
  @SuppressWarnings("all")
  public abstract boolean serializePlayerId();
}
