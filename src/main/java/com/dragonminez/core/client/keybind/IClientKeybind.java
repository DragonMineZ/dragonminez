package com.dragonminez.core.client.keybind;

import net.minecraft.client.KeyMapping;
import com.dragonminez.core.client.registry.ClientKeybindMappingRegistry;

/**
 * Represents a client-specific keybind configuration that integrates with Minecraft's
 * {@link KeyMapping} system.
 * <p>
 * Implementations of this interface provide additional behavior specific to client input logic,
 * such as modifier key support and default key mappings used during registration.
 *
 * <p>This interface is typically used alongside systems like {@code ClientKeybindMappingRegistry}
 * to initialize and manage user keybinds during mod startup.
 *
 * @see KeyMapping
 * @see ClientKeybindMappingRegistry
 */
public interface IClientKeybind {

  /**
   * Gets the default GLFW key code for this keybind.
   * <p>
   * This value is used when registering the {@link KeyMapping} and determines
   * which physical key is initially bound (e.g., {@code GLFW.GLFW_KEY_R}).
   *
   * @return the default GLFW key code
   * @see org.lwjgl.glfw.GLFW
   */
  int key();

  /**
   * Checks whether this keybind requires the Ctrl key to be held in order to activate.
   * <p>
   * This is used to provide additional control granularity and prevent accidental triggering
   * of powerful or context-sensitive abilities.
   *
   * @return {@code true} if Ctrl must be pressed for activation, {@code false} otherwise
   * @see net.minecraft.client.Minecraft
   */
  boolean requiresCtrl();
}
