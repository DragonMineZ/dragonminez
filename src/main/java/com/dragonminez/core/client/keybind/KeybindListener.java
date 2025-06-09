package com.dragonminez.core.client.keybind;

import com.dragonminez.core.client.registry.ClientKeybindMappingRegistry;
import com.dragonminez.core.client.registry.ClientKeybindMappingRegistry.ExtensiveKeyMapping;
import com.dragonminez.core.common.keybind.KeybindHandlerManager;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.keybind.KeybindManager;
import com.dragonminez.core.common.keybind.model.Keybind;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event listener for keybind registration and input handling.
 * <p>
 * Registers all keybinds managed by {@link KeybindManager} during the key mapping registration
 * event. Tracks key press states each client tick, invoking keybind actions accordingly.
 * <p>
 * Debug keys are excluded in production environment.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeybindListener {

  private static final Map<Keybind, Boolean> keyStateMap = new HashMap<>();

  /**
   * Handles key input state each client tick, firing keybind actions on press or hold as
   * appropriate.
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

    for (Keybind keybind : KeybindManager.INSTANCE.values(Dist.CLIENT)) {
      final ExtensiveKeyMapping mapping = ClientKeybindMappingRegistry.INSTANCE.value(keybind.id());
      if (mapping == null) {
        continue;
      }

      final boolean isActive = mapping.isActive();
      final boolean wasActive = keyStateMap.getOrDefault(keybind, false);

      if (isActive) {
        if (keybind.canBeHeldDown()) {
          KeybindListener.onPress(player, keybind, true);
          continue;
        }
        if (!wasActive) {
          KeybindListener.onPress(player, keybind, false);
        }
      }

      keyStateMap.put(keybind, isActive);
    }
  }

  private static void onPress(Player player, Keybind keybind, boolean heldDown) {
    if (keybind.notifyServer()) {
      KeybindManager.INSTANCE.notifyServer(keybind, heldDown);
      return;
    }
    KeybindHandlerManager.INSTANCE.onPress(player, keybind.id(), heldDown, false);
  }
}
