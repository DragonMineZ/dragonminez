package com.dragonminez.mod.core.client.keybind;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.keybind.KeyHandlerManager;
import com.dragonminez.mod.core.common.keybind.KeybindManager;
import com.dragonminez.mod.core.common.keybind.model.Keybind;
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
      if (!(keybind instanceof ClientKeybind clientKeybind)) {
        continue;
      }
      final boolean isActive = clientKeybind.isActive();
      final boolean wasActive = keyStateMap.getOrDefault(keybind, false);

      if (isActive) {
        if (keybind.canBeHeldDown()) {
          KeybindListener.onPress(player, clientKeybind, true);
          continue;
        }
        if (!wasActive) {
          KeybindListener.onPress(player, clientKeybind, false);
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
    KeyHandlerManager.INSTANCE.onPress(player, keybind.id(), heldDown, false);
  }
}
