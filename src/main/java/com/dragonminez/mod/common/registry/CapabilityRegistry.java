package com.dragonminez.mod.common.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.player.cap.combat.CombatData.CombatDataType;
import com.dragonminez.mod.common.player.cap.combat.CombatDataManager;
import com.dragonminez.mod.common.player.cap.genetic.GeneticData.GeneticDataType;
import com.dragonminez.mod.common.player.cap.genetic.GeneticDataManager;
import com.dragonminez.mod.common.player.cap.stat.StatData.StatDataType;
import com.dragonminez.mod.common.player.cap.stat.StatDataManager;
import com.dragonminez.mod.core.common.player.capability.CapDataManager;
import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles registration and attachment of custom player capabilities.
 * This class maps capability IDs to their respective managers, and ensures
 * they are attached to player entities when appropriate.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityRegistry {

  // Stores a mapping between capability IDs and their corresponding managers.
  private static final HashMap<ResourceLocation, CapDataManager<?>> HOLDERS = new HashMap<>();

  // Static initializer to register all capability types.
  static {
    HOLDERS.put(GeneticDataType.ID, GeneticDataManager.INSTANCE);
    HOLDERS.put(StatDataType.ID, StatDataManager.INSTANCE);
    HOLDERS.put(CombatDataType.ID, CombatDataManager.INSTANCE);
  }

  /**
   * Event handler that attaches capabilities to entities.
   * Only attaches capabilities to player entities.
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
