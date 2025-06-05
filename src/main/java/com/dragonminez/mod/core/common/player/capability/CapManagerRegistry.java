package com.dragonminez.mod.core.common.player.capability;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.client.registry.ClientCapNetHandlerRegistry;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Registers and manages custom player capabilities for both client and server environments.
 * <p>
 * This class maps capability IDs to their corresponding {@link CapDataManager} instances, enabling
 * automatic attachment to {@link Player} entities. It handles environment-sensitive registration by
 * detecting the distribution (client/server) and applying the correct set of managers accordingly.
 * <p>
 * Capabilities are attached to entities during the {@link AttachCapabilitiesEvent}, and this
 * listener ensures that only player entities receive them.
 * <p>
 * Managers must implement {@link CapDataManager} and be registered in the static block using a
 * unique {@link ResourceLocation} ID.
 *
 * @see CapDataManager
 * @see AttachCapabilitiesEvent
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapManagerRegistry {

  /**
   * Stores a mapping between capability {@link ResourceLocation} IDs and their corresponding
   * {@link CapDataManager} instances.
   * <p>
   * This map serves as the central registry for all managed capabilities in the mod.
   */
  private static final HashMap<ResourceLocation, CapDataManager<?>> HOLDERS = new HashMap<>();

  /**
   * A flag to ensure that the client-side capability network handler registration is only run once.
   * <p>
   * This is shit and needs to be redone in the future.
   */
  private static boolean ran = false;

  public static void register(ResourceLocation id, CapDataManager<?> manager) {
    if (!ran && FMLEnvironment.dist == Dist.CLIENT) {
      ClientCapNetHandlerRegistry.init();
      ran = true;
    }
    CapManagerRegistry.HOLDERS.put(id, manager);
  }

  /**
   * Attaches capabilities to {@link Player} entities when the {@link AttachCapabilitiesEvent} is
   * fired by Forge.
   * <p>
   * This method filters out non-player entities and ensures all registered capabilities are
   * properly attached using their associated {@link CapDataManager} instances.
   *
   * @param event the capability attachment event fired for entities
   */
  @SubscribeEvent
  public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (!(event.getObject() instanceof Player)) {
      return;
    }
    CapManagerRegistry.holders().forEach(event::addCapability);
  }

  /**
   * Retrieves a capability holder by its {@link ResourceLocation} ID.
   *
   * @param id the {@link ResourceLocation} ID of the capability
   * @return the {@link CapDataManager} instance associated with the given ID, or null if not found
   */
  public static CapDataManager<?> holder(ResourceLocation id) {
    return CapManagerRegistry.HOLDERS.get(id);
  }

  /**
   * Returns a map of all registered capability holders.
   *
   * @return a {@link HashMap} mapping {@link ResourceLocation} capability IDs to their respective
   * {@link CapDataManager} instances
   */
  public static HashMap<ResourceLocation, CapDataManager<?>> holders() {
    return HOLDERS;
  }
}
