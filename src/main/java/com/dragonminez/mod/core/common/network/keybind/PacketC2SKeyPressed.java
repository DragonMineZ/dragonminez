package com.dragonminez.mod.core.common.network.keybind;

import com.dragonminez.mod.core.common.keybind.model.Keybind;
import com.dragonminez.mod.core.common.network.Packet;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client-to-server packet used to notify the server that a keybind was pressed.
 * <p>
 * Carries the keybind's unique identifier and whether it was a held-down repeat or a fresh press.
 * This enables server-side logic to respond to keybind actions initiated by the client.
 */
public class PacketC2SKeyPressed extends Packet {

  private String identifier;
  private boolean heldDown;

  /**
   * Constructs a new key press packet.
   *
   * @param identifier the unique keybind ID string (matches {@link Keybind#id()})
   * @param heldDown   true if the keybind is being held down (repeated activation), false if it's
   *                   an initial press
   */
  public PacketC2SKeyPressed(String identifier, boolean heldDown) {
    this.identifier = identifier;
    this.heldDown = heldDown;
  }

  public PacketC2SKeyPressed(FriendlyByteBuf buf) {
    super(buf);
  }

  /**
   * Serializes the packet data to the given buffer.
   *
   * @param buf the buffer to write to
   */
  @Override
  public void encode(FriendlyByteBuf buf) {
    buf.writeUtf(this.identifier);
    buf.writeBoolean(this.heldDown);
  }

  /**
   * Deserializes the packet data from the given buffer.
   *
   * @param buf the buffer to read from
   */
  @Override
  public void decode(FriendlyByteBuf buf) {
    this.identifier = buf.readUtf();
    this.heldDown = buf.readBoolean();
  }

  /**
   * Returns the keybind identifier sent in this packet.
   *
   * @return the keybind ID string
   */
  public String id() {
    return identifier;
  }

  /**
   * Indicates whether the key was being held down (repeated activation).
   *
   * @return true if the key was held down, false if it was a new press
   */
  public boolean isHeldDown() {
    return this.heldDown;
  }
}
