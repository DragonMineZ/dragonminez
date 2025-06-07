package com.dragonminez.mod.core.client.keybind;

import com.dragonminez.mod.common.Reference;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Represents a custom keybind with optional Ctrl modifier support.
 * <p>
 * Manages key mapping registration, input state checking, and key press handling.
 */
public abstract class Keybind {

  private KeyMapping mapping;

  /**
   * Called when this keybind is pressed by the player.
   *
   * @param player the local player instance
   */
  public abstract void onPress(LocalPlayer player);

  /**
   * Unique identifier for this keybind (used in translation keys).
   *
   * @return the keybind ID string
   */
  public abstract String id();

  /**
   * Category name for grouping keybinds in controls settings.
   *
   * @return the category string
   */
  public abstract String category();

  /**
   * The default GLFW key code this keybind listens to.
   *
   * @return the GLFW key code
   */
  public abstract int key();

  /**
   * Whether this keybind action can be triggered repeatedly by holding the key.
   *
   * @return true if holdable, false if only on initial press
   */
  public abstract boolean canBeHeldDown();

  /**
   * Whether this keybind requires Ctrl to be held down to activate.
   *
   * @return true if Ctrl is required, false otherwise
   */
  public abstract boolean requiresCtrl();

  /**
   * Whether this keybind is considered a debug key (hidden in production).
   *
   * @return true if debug key, false by default
   */
  public boolean isDebugKey() {
    return false;
  }

  /**
   * Returns the {@link KeyMapping} instance for this keybind,
   * creating it if necessary.
   *
   * @return the KeyMapping object
   */
  public KeyMapping mapping() {
    if (this.mapping == null) {
      final String keyLang = "key.%s.%s".formatted(Reference.MOD_ID, this.id());
      final String category = "keys.%s.%s".formatted(Reference.MOD_ID, this.category());
      this.mapping = new ExtensiveKeyMapping(keyLang, InputConstants.Type.KEYSYM, this.key(),
          category,
          this.requiresCtrl());
    }
    return this.mapping;
  }

  /**
   * Checks whether the Ctrl key is currently pressed.
   *
   * @return true if left or right Ctrl is down, false otherwise
   */
  public boolean isCtrlDown() {
    long window = Minecraft.getInstance().getWindow().getWindow();
    return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
  }

  /**
   * Determines if this keybind is currently active (pressed and modifiers satisfied).
   *
   * @return true if the keybind is active, false otherwise
   */
  public boolean isActive() {
    return this.mapping().isDown() && (!requiresCtrl() || isCtrlDown());
  }

  /**
   * Extended KeyMapping that optionally displays "Ctrl + " prefix in its translated name.
   */
  public static class ExtensiveKeyMapping extends KeyMapping {

    private final boolean ctrlRequired;

    public ExtensiveKeyMapping(String keyLang, InputConstants.Type type, int key, String category,
        boolean ctrlRequired) {
      super(keyLang, type, key, category);
      this.ctrlRequired = ctrlRequired;
    }

    @Override
    public @NotNull Component getTranslatedKeyMessage() {
      if (this.ctrlRequired) {
        final String keyName = super.getTranslatedKeyMessage().getString();
        return Component.literal("Ctrl + " + keyName);
      } else {
        return super.getTranslatedKeyMessage();
      }
    }
  }
}
