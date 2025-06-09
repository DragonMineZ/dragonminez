package com.dragonminez.core.client.registry;

import com.dragonminez.core.client.keybind.IClientKeybind;
import com.dragonminez.core.client.registry.ClientKeybindMappingRegistry.ExtensiveKeyMapping;
import com.dragonminez.core.common.keybind.KeybindManager;
import com.dragonminez.core.common.keybind.model.Keybind;
import com.dragonminez.core.common.manager.ListManager;
import com.dragonminez.core.common.util.DebugUtil;
import com.dragonminez.mod.common.Reference;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and manages all client-side {@link KeyMapping}s for the mod.
 * <p>
 * This class integrates the keybind configuration from {@link IClientKeybind} and
 * {@link KeybindManager}, automatically filtering out debug-only keys in production. Key mappings
 * requiring Ctrl are wrapped in a specialized implementation to reflect that behavior.
 *
 * @see IClientKeybind
 * @see KeybindManager
 * @see KeyMapping
 * @see RegisterKeyMappingsEvent
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class ClientKeybindMappingRegistry extends ListManager<String, ExtensiveKeyMapping> {

  /**
   * Singleton instance of this registry.
   */
  public static final ClientKeybindMappingRegistry INSTANCE = new ClientKeybindMappingRegistry();

  /**
   * Automatically registers all valid client key mappings during the mod's initialization phase.
   * <p>
   * This method is triggered by {@link RegisterKeyMappingsEvent} and filters out debug keys if not
   * running in debug mode. Only keys that extend {@link IClientKeybind} are considered.
   *
   * @param event the registration event dispatched by Forge
   * @see KeybindManager#values(Dist)
   * @see DebugUtil#isDebug()
   */
  @SubscribeEvent
  public static void keyRegister(RegisterKeyMappingsEvent event) {
    for (Keybind value : KeybindManager.INSTANCE.values(Dist.CLIENT)) {
      if (!(value instanceof IClientKeybind clientKey)) {
        continue;
      }
      if (value.isDebugKey() && !DebugUtil.isDebug()) {
        continue;
      }

      final ExtensiveKeyMapping keyMapping = ClientKeybindMappingRegistry.INSTANCE
          .createMapping(value, clientKey);
      ClientKeybindMappingRegistry.INSTANCE.register(value.id(), keyMapping);
      event.register(keyMapping);
    }
  }

  /**
   * Creates a new {@link ExtensiveKeyMapping} for the given keybind.
   * <p>
   * This method uses the keybind's ID and category to build a localized translation key, ensuring
   * consistency with language files.
   *
   * @param keybind       the base keybind definition
   * @param clientKeybind the client-side extension of the keybind
   * @return a constructed {@link ExtensiveKeyMapping}
   * @see IClientKeybind#requiresCtrl()
   */
  private ExtensiveKeyMapping createMapping(Keybind keybind, IClientKeybind clientKeybind) {
    final String keyLang = "key.%s.%s".formatted(Reference.MOD_ID, keybind.id());
    final String category = "keys.%s.%s".formatted(Reference.MOD_ID, keybind.category());
    return new ExtensiveKeyMapping(
        keyLang,
        Type.KEYSYM,
        clientKeybind.key(),
        category,
        clientKeybind.requiresCtrl()
    );
  }

  /**
   * Returns the identifier used to name this manager in logs or systems that require tagging.
   *
   * @return the string identifier "keymappings"
   */
  @Override
  public String identifier() {
    return "keymappings";
  }

  /**
   * Indicates that the keys in this manager must be unique.
   *
   * @return true, since key mappings are registered by unique identifiers
   */
  @Override
  public boolean uniqueKeys() {
    return true;
  }

  /**
   * Specifies the logging behavior for this manager. Returning {@code null} disables automatic
   * logging.
   *
   * @return null to indicate no logging behavior
   */
  @Override
  public LogMode logMode() {
    return null;
  }

  /**
   * A specialized extension of {@link KeyMapping} that supports Ctrl-modified logic and prepends
   * "Ctrl + " to the translated key message when applicable.
   *
   * @see KeyMapping
   * @see GLFW
   */
  public static class ExtensiveKeyMapping extends KeyMapping {

    private final boolean ctrlRequired;

    /**
     * Constructs an {@link ExtensiveKeyMapping} with optional Ctrl modifier logic.
     *
     * @param keyLang      the translation key for this keybind
     * @param type         the input type (usually {@link Type#KEYSYM})
     * @param key          the GLFW key code
     * @param category     the translation key for the category
     * @param ctrlRequired whether this keybind requires Ctrl to activate
     */
    public ExtensiveKeyMapping(String keyLang, InputConstants.Type type, int key, String category,
        boolean ctrlRequired) {
      super(keyLang, type, key, category);
      this.ctrlRequired = ctrlRequired;
    }

    /**
     * Returns the display name for this keybind. Prepends "Ctrl + " if {@code ctrlRequired} is
     * true.
     *
     * @return a localized {@link Component} representing the key message
     */
    @Override
    public @NotNull Component getTranslatedKeyMessage() {
      Component base = super.getTranslatedKeyMessage();
      return ctrlRequired ? Component.literal("Ctrl + ").append(base) : base;
    }

    /**
     * Checks if this keybind is currently active (pressed and, if required, Ctrl held).
     *
     * @return true if active, false otherwise
     * @see #isCtrlDown()
     */
    public boolean isActive() {
      return this.isDown() && (!ctrlRequired || isCtrlDown());
    }

    /**
     * Checks whether either the left or right Ctrl key is currently pressed.
     *
     * @return true if Ctrl is pressed, false otherwise
     */
    private boolean isCtrlDown() {
      long window = Minecraft.getInstance().getWindow().getWindow();
      return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
          GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }
  }
}
