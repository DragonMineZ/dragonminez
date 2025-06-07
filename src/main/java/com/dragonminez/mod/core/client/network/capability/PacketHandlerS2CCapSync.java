package com.dragonminez.mod.core.client.network.capability;

import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataHolder;
import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Handles the processing of capability synchronization packets sent from server to client.
 * <p>
 * This handler updates the local client-side capability data for the target player using the
 * deserialized data from the packet.
 */
public class PacketHandlerS2CCapSync {

  /**
   * Processes a server-to-client capability sync packet.
   *
   * @param id     the capability data manager responsible for applying updates
   * @param packet the packet containing the serialized capability data
   * @param ctx    the context supplier for network threading
   */
  @SuppressWarnings("all")
  public static void handle(ResourceLocation id, PacketS2CCapSync<?> packet,
      Supplier<Context> ctx) {
    final NetworkEvent.Context context = ctx.get();
    context.enqueueWork(() -> {
      final Level level = Minecraft.getInstance().level;
      if (level == null) {
        return;
      }

      Player player = Minecraft.getInstance().player;

      // If a specific player ID is included in the packet, get the correct player
      if (packet.serializePlayerId() && !packet.playerId().equals(player.getUUID())) {
        player = level.getPlayerByUUID(packet.playerId());
      }

      if (player == null) {
        LogUtil.info("Player with id {} not found for packet {}", packet.playerId(),
            packet.getClass().getSimpleName());
        return;
      }

      final CapDataManager<?> manager = CapManagerRegistry.manager(id, Dist.CLIENT);
      if (manager == null) {
        LogUtil.warn("Could not find manager with id %s. Discarding update.".formatted(id.toString()));
        return;
      }
      manager.update(player, packet.data());
    });
    context.setPacketHandled(true);
  }
}
