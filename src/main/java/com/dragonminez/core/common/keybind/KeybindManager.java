package com.dragonminez.core.common.keybind;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.util.NetUtil;
import com.dragonminez.core.common.keybind.model.Keybind;
import com.dragonminez.core.common.manager.DistListManager;
import com.dragonminez.core.common.manager.ListManager;
import com.dragonminez.core.common.network.keybind.PacketC2SKeyPressed;
import net.minecraftforge.fml.common.Mod;

/**
 * Singleton manager for handling keybind registrations.
 * <p>
 * Extends {@link ListManager} with keys of type {@link String} and values of type {@link Keybind}.
 * Ensures unique keys and logs all operations.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeybindManager extends DistListManager<String, Keybind> {

  /**
   * The singleton instance of the {@code KeybindManager}.
   */
  public static final KeybindManager INSTANCE = new KeybindManager();

  /**
   * Notifies the server when a keybind is pressed or released.
   * <p>
   * Only sends a packet if the keybind is flagged to notify the server.
   *
   * @param keybind  the keybind being triggered
   * @param heldDown whether the key is currently held down
   */
  public void notifyServer(Keybind keybind, boolean heldDown) {
    if (!keybind.notifyServer()) {
      return;
    }
    NetUtil.sendToServer(new PacketC2SKeyPressed(keybind.id(), heldDown));
  }

  /**
   * Returns the identifier for this manager.
   *
   * @return the string {@code "keybinds"}
   */
  @Override
  public String identifier() {
    return "keybinds";
  }

  /**
   * Specifies whether keys must be unique in this manager.
   *
   * @return {@code true}, indicating keys must be unique
   */
  @Override
  public boolean uniqueKeys() {
    return true;
  }

  /**
   * Specifies the logging mode used by this manager.
   *
   * @return {@link LogMode#LOG_ALL}, enabling logging for all operations
   */
  @Override
  public LogMode logMode() {
    return LogMode.LOG_ALL;
  }
}
