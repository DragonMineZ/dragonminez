package com.dragonminez.mod.common.registry;

import com.dragonminez.mod.client.player.cap.combat.ClientCombatDataManager;
import com.dragonminez.mod.client.player.cap.genetic.ClientGeneticDataManager;
import com.dragonminez.mod.client.player.cap.stat.ClientStatDataManager;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataType;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataType;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataType;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import com.dragonminez.mod.server.player.combat.ServerCombatDataManager;
import com.dragonminez.mod.server.player.genetic.ServerGeneticDataManager;
import com.dragonminez.mod.server.player.stat.ServerStatDataManager;
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
 * Handles registration and attachment of custom player capabilities. This class maps capability IDs
 * to their respective managers, and ensures they are attached to player entities when appropriate.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityRegistry {

  // Stores a mapping between capability IDs and their corresponding managers.
  private static final HashMap<ResourceLocation, CapDataManager<?>> HOLDERS = new HashMap<>();

  // Static initializer to register all capability types.
  static {
    if (FMLEnvironment.dist == Dist.CLIENT) {
      HOLDERS.put(GeneticDataType.ID, ClientGeneticDataManager.INSTANCE);
      HOLDERS.put(StatDataType.ID, ClientStatDataManager.INSTANCE);
      HOLDERS.put(CombatDataType.ID, ClientCombatDataManager.INSTANCE);
    } else {
      HOLDERS.put(GeneticDataType.ID, ServerGeneticDataManager.INSTANCE);
      HOLDERS.put(StatDataType.ID, ServerStatDataManager.INSTANCE);
      HOLDERS.put(CombatDataType.ID, ServerCombatDataManager.INSTANCE);
    }
  }

  /**
   * Event handler that attaches capabilities to entities. Only attaches capabilities to player
   * entities.
   */
  @SubscribeEvent
  public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (!(event.getObject() instanceof Player)) {
      return;
    }

    // Attach each capability to the player entity.
    CapabilityRegistry.holders().forEach(event::addCapability);
  }

  /**
   * Returns the registered capability holders.
   *
   * @return a map of capability IDs to their data managers
   */
  public static HashMap<ResourceLocation, CapDataManager<?>> holders() {
    return HOLDERS;
  }
}
