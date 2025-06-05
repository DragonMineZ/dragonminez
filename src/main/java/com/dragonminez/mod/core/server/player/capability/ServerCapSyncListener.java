package com.dragonminez.mod.core.server.player.capability;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.player.capability.CapManagerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.BiConsumer;

/**
 * Handles server-side capability synchronization for {@link ServerPlayer} entities.
 * This listener ensures all registered capabilities are kept in sync during key lifecycle events.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerCapSyncListener {

  @SubscribeEvent
  public static void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      syncSelf(player, player);
    }
  }

  @SubscribeEvent
  public static void onPlayerCloned(PlayerEvent.Clone event) {
    if (event.isWasDeath() &&
        event.getOriginal() instanceof ServerPlayer original &&
        event.getEntity() instanceof ServerPlayer clone) {
      original.reviveCaps();
      syncSelf(original, clone);
      original.invalidateCaps();
    }
  }

  @SubscribeEvent
  public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      syncSelf(player, player);
    }
  }

  @SubscribeEvent
  public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      syncSelf(player, player);
    }
  }

  @SubscribeEvent
  public static void onPlayerTrack(PlayerEvent.StartTracking event) {
    if (event.getTarget() instanceof ServerPlayer target &&
        event.getEntity() instanceof ServerPlayer tracker) {
      syncOthers(target, tracker);
    }
  }

  /**
   * Syncs all capability data from one player to another.
   * Used for login, respawn, and dimension change events.
   */
  private static void syncSelf(ServerPlayer from, ServerPlayer to) {
    updateEachManager(to, from, (id, manager)
        -> manager.manager().update(to, from));
  }

  /**
   * Sends public capability data from one player to another during tracking.
   */
  private static void syncOthers(ServerPlayer target, ServerPlayer tracker) {
    updateEachManager(tracker, target, (id, manager) -> {});
  }

  /**
   * Iterates over all registered capability managers and applies a sync operation.
   */
  private static void updateEachManager(ServerPlayer receiver, ServerPlayer reference,
      BiConsumer<ResourceLocation, IServerCapDataManager<?, ?>> consumer) {
    CapManagerRegistry.holders().forEach((id, manager) -> {
      if (manager instanceof IServerCapDataManager<?, ?> serverManager) {
        serverManager.sendUpdate(receiver, reference);
        consumer.accept(id, serverManager);
      }
    });
  }
}