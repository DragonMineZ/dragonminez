package com.dragonminez.mod.core.client.keybind;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.manager.ListManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Singleton manager for handling keybind registrations.
 * <p>
 * Extends {@link ListManager} with keys of type {@link String} and values of type {@link Keybind}.
 * Ensures unique keys and logs all operations.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeybindManager extends ListManager<String, Keybind> {

  /**
   * The singleton instance of the KeybindManager.
   */
  public static final KeybindManager INSTANCE = new KeybindManager();

  /**
   * Registers key mappings for all managed keybinds,
   * excluding debug keys in production.
   *
   * @param event the key mapping registration event
   */
  @SubscribeEvent
  public static void keyRegister(RegisterKeyMappingsEvent event) {
    for (Keybind value : KeybindManager.INSTANCE.values()) {
      if (value.isDebugKey() && FMLEnvironment.production) {
        continue;
      }
      event.register(value.mapping());
    }
  }

  /**
   * Returns the identifier string used by this manager.
   *
   * @return the manager's identifier, "keybinds"
   */
  @Override
  public String identifier() {
    return "keybinds";
  }

  /**
   * Indicates whether keys managed by this manager must be unique.
   *
   * @return true, enforcing unique keys
   */
  @Override
  public boolean uniqueKeys() {
    return true;
  }

  /**
   * Specifies the logging mode for this manager.
   *
   * @return {@link LogMode#LOG_ALL}, indicating all operations should be logged
   */
  @Override
  public LogMode logMode() {
    return LogMode.LOG_ALL;
  }
}
