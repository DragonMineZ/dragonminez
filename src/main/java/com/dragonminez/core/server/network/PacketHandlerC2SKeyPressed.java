package com.dragonminez.core.server.network;

import com.dragonminez.core.common.keybind.KeybindHandlerManager;
import com.dragonminez.core.common.network.keybind.PacketC2SKeyPressed;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketHandlerC2SKeyPressed {

  public static void handle(PacketC2SKeyPressed packet, Supplier<Context> supplier) {
    final NetworkEvent.Context context = supplier.get();
    context.enqueueWork(() ->
        KeybindHandlerManager.INSTANCE.onPress(supplier.get().getSender(), packet.id(),
            packet.isHeldDown(), true));
    context.setPacketHandled(true);
  }
}
