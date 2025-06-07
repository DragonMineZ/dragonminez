package com.dragonminez.mod.core.common.player.capability;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.client.registry.ClientCapNetHandlerRegistry;
import com.dragonminez.mod.core.common.manager.DistListManager;
import com.dragonminez.mod.core.common.player.capability.CapDataManager.CapInstanceProvider;
import com.google.common.collect.HashBasedTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

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
public class CapManagerRegistry extends DistListManager<ResourceLocation, CapDataManager<?>> {

  public static final CapManagerRegistry INSTANCE = new CapManagerRegistry();

  static {
    if (FMLEnvironment.dist == Dist.CLIENT) {
      ClientCapNetHandlerRegistry.init();
    }
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
    if (!(event.getObject() instanceof Player player)) {
      return;
    }

    final boolean isClientSide = player.level().isClientSide;
    for (CapDataManager<?> value :
        INSTANCE.values(isClientSide ? Dist.CLIENT : Dist.DEDICATED_SERVER)) {
      final CapInstanceProvider<?> provider = new CapInstanceProvider<>(value);
      event.addCapability(value.id(), provider);
    }
  }

  @Override
  public String identifier() {
    return "capability";
  }

  @Override
  public boolean uniqueKeys() {
    return true;
  }

  @Override
  public LogMode logMode() {
    return LogMode.LOG_ALL;
  }
}
