package com.dragonminez.mod.client.network.player.stat.handler;

import com.dragonminez.mod.common.network.player.cap.stat.s2c.PacketS2CSyncStat;
import com.dragonminez.mod.common.player.cap.stat.StatDataManager;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

public class PacketHandlerS2CSyncStat {

  public static void handle(PacketS2CSyncStat packet, Supplier<NetworkEvent.Context> ctx) {
    final NetworkEvent.Context context = ctx.get();
    context.enqueueWork(() -> {
      final Level level = Minecraft.getInstance().level;
      if (level == null) {
        return;
      }

      final Player player = Minecraft.getInstance().player;
      StatDataManager.INSTANCE.update(player, packet.data());
    });
    context.setPacketHandled(true);
  }
}
