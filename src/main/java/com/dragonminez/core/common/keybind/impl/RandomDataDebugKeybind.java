package com.dragonminez.core.common.keybind.impl;

import com.dragonminez.core.common.keybind.model.Keybind;

/**
 * A debug-only keybind that triggers randomization of server-side capability data.
 * <p>
 * This keybind is used for development purposes to test dynamic data initialization logic.
 * It notifies the server on press and is excluded from production environments.
 *
 * @see Keybind
 */
public class RandomDataDebugKeybind extends Keybind {

  /**
   * Returns the unique identifier for this keybind.
   * This will be used to construct the translation key (e.g., {@code key.dragonminez.random_data}).
   *
   * @return the keybind ID string
   */
  @Override
  public String id() {
    return "random_data";
  }

  /**
   * Returns the category under which this keybind is grouped in the controls menu.
   *
   * @return the category name string (e.g., "debug")
   */
  @Override
  public String category() {
    return "debug";
  }

  /**
   * Specifies whether this keybind supports being held down for repeated activation.
   * This keybind is press-only, so holding will have no effect.
   *
   * @return false, as holding is not supported
   */
  @Override
  public boolean canBeHeldDown() {
    return false;
  }

  /**
   * Indicates whether the server should be notified when this keybind is pressed.
   * This is required for capability randomization to be triggered server-side.
   *
   * @return true to enable server notification
   */
  @Override
  public boolean notifyServer() {
    return true;
  }

  /**
   * Flags this keybind as a debug-only binding.
   * It will be excluded from production builds unless explicitly enabled.
   *
   * @return true to mark as debug-only
   */
  @Override
  public boolean isDebugKey() {
    return true;
  }
}
