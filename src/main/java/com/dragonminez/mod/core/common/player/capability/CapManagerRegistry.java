package com.dragonminez.mod.core.common.player.capability;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.client.registry.ClientCapNetHandlerRegistry;
import com.google.common.collect.HashBasedTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.HashMap;

/**
 * Central registry and event handler for managing player capability systems.
 * <p>
 * This class maps {@link ResourceLocation} IDs to {@link CapDataManager} instances using a
 * {@link HashBasedTable} that separates registrations by distribution side ({@link Dist}).
 * <p>
 * It ensures capabilities are only attached to {@link Player} entities during
 * {@link AttachCapabilitiesEvent}, and that each side (client or server) receives only its relevant
 * capabilities.
 * <p>
 * Client-side capability synchronization is also initialized once on the first client-side
 * registration.
 *
 * @see CapDataManager
 * @see AttachCapabilitiesEvent
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapManagerRegistry {

  /**
   * Holds capability managers for each side (client or server) mapped by their unique capability
   * ID.
   * <p>
   * The outer map is keyed by {@link Dist}, and the inner map by the {@link ResourceLocation} of
   * the capability. This allows separate registration of client/server-only capabilities when
   * needed.
   */
  private static final HashBasedTable<Dist, ResourceLocation, CapDataManager<?>> MANAGERS =
      HashBasedTable.create();

  static {
    if (FMLEnvironment.dist == Dist.CLIENT) {
      ClientCapNetHandlerRegistry.init();
    }
  }

  /**
   * Registers a capability manager for the specified distribution (client or server).
   * <p>
   * On the client side, it also triggers the initialization of client capability networking (only
   * once).
   *
   * @param dist    the distribution (client or server)
   * @param id      the unique ID of the capability
   * @param manager the capability data manager instance
   */
  public static void register(Dist dist, ResourceLocation id, CapDataManager<?> manager) {

    MANAGERS.put(dist, id, manager);
  }

  /**
   * Automatically attaches all capability managers registered for the current distribution to
   * player entities.
   * <p>
   * This event is triggered by Forge during capability attachment and is filtered to apply only to
   * players.
   *
   * @param event the capability attachment event
   */
  @SubscribeEvent
  public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (!(event.getObject() instanceof Player)) {
      return;
    }

    Dist dist = FMLEnvironment.dist;
    MANAGERS.row(dist).forEach(event::addCapability);
  }

  /**
   * Retrieves a capability manager for the current distribution (client/server) by its ID.
   *
   * @param id the capability ID
   * @return the corresponding {@link CapDataManager} instance, or {@code null} if not found
   */
  public static CapDataManager<?> manager(ResourceLocation id) {
    return manager(id, FMLEnvironment.dist);
  }

  /**
   * Retrieves a capability manager for the given distribution (client/server) by its ID.
   *
   * @param id   the capability ID
   * @param dist the distribution (client or server)
   * @return the corresponding {@link CapDataManager} instance, or {@code null} if not found
   */
  public static CapDataManager<?> manager(ResourceLocation id, Dist dist) {
    return MANAGERS.get(dist, id);
  }

  /**
   * Returns all capability managers registered for the current distribution.
   * <p>
   * This returns a shallow copy to avoid exposing the internal mutable structure.
   *
   * @return a map of capability IDs to their respective managers for the current distribution
   */
  public static HashMap<ResourceLocation, CapDataManager<?>> managers(Dist dist) {
    return new HashMap<>(MANAGERS.row(dist));
  }

  /**
   * Returns all capability managers registered for the current distribution.
   * <p>
   * This returns a shallow copy to avoid exposing the internal mutable structure.
   *
   * @return a map of capability IDs to their respective managers for the current distribution
   */
  public static HashMap<ResourceLocation, CapDataManager<?>> managers() {
    return new HashMap<>(MANAGERS.row(FMLEnvironment.dist));
  }
}
