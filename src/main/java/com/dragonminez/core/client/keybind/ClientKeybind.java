package com.dragonminez.core.client.keybind;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.keybind.model.Keybind;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Defines a client-side keybind built directly on top of Minecraft's {@link KeyMapping} system.
 * <p>
 * Each instance represents a full keybind definition, responsible for creating and configuring
 * the underlying {@link KeyMapping} during client initialization.
 */
public abstract class ClientKeybind extends Keybind {

  private KeyMapping mapping;

  /**
   * Returns the default GLFW key code for this keybind.
   * This value is passed directly into the constructed {@link KeyMapping}.
   *
   * @return the GLFW key code (e.g., {@code GLFW.GLFW_KEY_R})
   */
  public abstract int key();

  /**
   * Indicates whether this keybind requires Ctrl to be held down for activation.
   * This overrides the base method to provide client-specific behavior.
   *
   * @return true if Ctrl is required, false otherwise
   */
  public abstract boolean requiresCtrl();

  /**
   * Constructs and returns the {@link KeyMapping} instance associated with this keybind.
   * <p>
   * This method ensures that the {@link KeyMapping} is only created once, and uses
   * localized translation keys derived from the keybind's {@code id()} and {@code category()}.
   *
   * @return the initialized {@link KeyMapping}
   */
  public KeyMapping mapping() {
    if (this.mapping == null) {
      final String keyLang = "key.%s.%s".formatted(Reference.MOD_ID, this.id());
      final String category = "keys.%s.%s".formatted(Reference.MOD_ID, this.category());
      this.mapping = new ExtensiveKeyMapping(
          keyLang,
          InputConstants.Type.KEYSYM,
          this.key(),
          category,
          this.requiresCtrl()
      );
    }
    return this.mapping;
  }

  /**
   * Checks if either Ctrl key (left or right) is currently being held down.
   *
   * @return true if Ctrl is down, false otherwise
   */
  public boolean isCtrlDown() {
    long window = Minecraft.getInstance().getWindow().getWindow();
    return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
  }

  /**
   * Checks whether this keybind is actively being pressed, and all modifier conditions are met.
   *
   * @return true if the keybind is currently active, false otherwise
   */
  public boolean isActive() {
    return this.mapping().isDown() && (!requiresCtrl() || isCtrlDown());
  }

  /**
   * A specialized {@link KeyMapping} implementation that prepends "Ctrl + "
   * to the translated key message if the keybind requires Ctrl.
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
      Component base = super.getTranslatedKeyMessage();
      return ctrlRequired ? Component.literal("Ctrl + ").append(base) : base;
    }
  }
}
