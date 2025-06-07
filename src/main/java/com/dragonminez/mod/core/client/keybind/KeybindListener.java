package com.dragonminez.mod.core.client.keybind;

import com.dragonminez.mod.common.Reference;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Client-side event listener for keybind registration and input handling.
 * <p>
 * Registers all keybinds managed by {@link KeybindManager} during
 * the key mapping registration event. Tracks key press states each client tick,
 * invoking keybind actions accordingly.
 * <p>
 * Debug keys are excluded in production environment.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeybindListener {

  private static final Map<Keybind, Boolean> keyStateMap = new HashMap<>();

  /**
   * Handles key input state each client tick,
   * firing keybind actions on press or hold as appropriate.
   *
   * @param event the client tick event
   */
  @SubscribeEvent
  public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END) {
      return;
    }

    final LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      return;
    }

    for (Keybind keybind : KeybindManager.INSTANCE.values()) {
      final boolean isActive = keybind.isActive();
      final boolean wasActive = keyStateMap.getOrDefault(keybind, false);

      if (isActive) {
        if (keybind.canBeHeldDown()) {
          keybind.onPress(player);
          continue;
        }
        if (!wasActive) {
          keybind.onPress(player);
        }
      }

      keyStateMap.put(keybind, isActive);
    }
  }
}
