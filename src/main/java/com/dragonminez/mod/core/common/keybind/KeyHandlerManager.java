package com.dragonminez.mod.core.common.keybind;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.util.LogUtil;
import com.dragonminez.mod.core.common.keybind.model.Keybind;
import com.dragonminez.mod.core.common.keybind.model.KeybindHandler;
import com.dragonminez.mod.core.common.manager.DistListManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * Singleton manager for handling keybind press event routing across client and server.
 * <p>
 * This class extends {@link DistListManager} to register {@link KeybindHandler} instances
 * based on environment (client/server) and dispatches key press actions to them
 * in coordination with {@link KeybindManager}.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyHandlerManager extends DistListManager<String, KeybindHandler> {

  /**
   * The singleton instance of the {@code KeyHandlerManager}.
   */
  public static final KeyHandlerManager INSTANCE = new KeyHandlerManager();

  /**
   * Routes a key press event to the appropriate {@link KeybindHandler} for the given keybind ID.
   *
   * @param player   the player who triggered the keybind
   * @param id       the keybind ID
   * @param heldDown true if the key was held down, false if it was a new press
   * @param isServer true if this logic is being executed on the server, false if on client
   */
  public void onPress(Player player, String id, boolean heldDown, boolean isServer) {
    final Dist dist = isServer ? Dist.DEDICATED_SERVER : Dist.CLIENT;

    final Keybind keybind = KeybindManager.INSTANCE.get(dist, id);
    if (keybind == null) {
      LogUtil.warn("Could not find keybind attached to key '%s'".formatted(id));
      return;
    }

    final KeybindHandler handler = this.get(dist, id);
    if (handler == null) {
      LogUtil.warn("Could not find handler attached to key '%s'".formatted(id));
      return;
    }

    if (heldDown && !keybind.canBeHeldDown()) {
      return;
    }

    handler.onPressed(player, heldDown, dist);
  }

  /**
   * Returns the string identifier for this manager.
   *
   * @return the string {@code "keybinds"}
   */
  @Override
  public String identifier() {
    return "keybinds";
  }

  /**
   * Specifies that keys in this manager must be unique across the registered handlers.
   *
   * @return {@code true}, enforcing unique keybind identifiers
   */
  @Override
  public boolean uniqueKeys() {
    return true;
  }

  /**
   * Specifies the logging mode used by this manager.
   *
   * @return {@link LogMode#LOG_ALL}, logging all registration operations
   */
  @Override
  public LogMode logMode() {
    return LogMode.LOG_ALL;
  }
}
