package com.dragonminez.mod.core.common.keybind.model;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Abstract base class for handling keybind actions.
 * <p>
 * Implementations define behavior that should occur when a keybind is activated. This handler can
 * be invoked on both client and server sides, depending on the keybind's configuration.
 */
public abstract class KeybindHandler {

  /**
   * Called when the associated keybind is triggered.
   *
   * @param player   the player pressing the key
   * @param heldDown true if the key is being held down (for repeatable actions), false if just
   *                 pressed
   * @param dist the side where this code is being called.
   */
  public abstract void onPressed(Player player, boolean heldDown, Dist dist);
}
