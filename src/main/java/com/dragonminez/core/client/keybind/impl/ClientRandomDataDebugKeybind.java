package com.dragonminez.core.client.keybind.impl;

import com.dragonminez.core.client.keybind.IClientKeybind;
import com.dragonminez.core.common.keybind.impl.RandomDataDebugKeybind;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side definition for the {@link RandomDataDebugKeybind}, assigning a default key and
 * modifier behavior.
 * <p>
 * This implementation binds the debug key to {@code J} without requiring the Ctrl modifier.
 *
 * @see RandomDataDebugKeybind
 * @see IClientKeybind
 */
public class ClientRandomDataDebugKeybind extends RandomDataDebugKeybind implements IClientKeybind {

  /**
   * Returns the default GLFW key code used for this keybind on the client.
   * <p>
   * This is hardcoded to {@link GLFW#GLFW_KEY_J}, representing the "J" key.
   *
   * @return the key code for the "J" key
   */
  @Override
  public int key() {
    return GLFW.GLFW_KEY_J;
  }

  /**
   * Indicates whether the Ctrl key must be held to activate this keybind.
   * <p>
   * This implementation returns {@code false}, meaning no modifier is required.
   *
   * @return false, as Ctrl is not required
   */
  @Override
  public boolean requiresCtrl() {
    return false;
  }
}
