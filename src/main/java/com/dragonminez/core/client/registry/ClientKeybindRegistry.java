package com.dragonminez.core.client.registry;

import com.dragonminez.core.client.keybind.impl.ClientRandomDataDebugKeybind;
import com.dragonminez.core.common.keybind.KeybindManager;
import com.dragonminez.core.common.registry.KeybindRegistry;
import net.minecraftforge.api.distmarker.Dist;

public class ClientKeybindRegistry {

  public static void init() {
    KeybindManager.INSTANCE.register(Dist.CLIENT,
        KeybindRegistry.DEBUG_RANDOM_DATA.id(), new ClientRandomDataDebugKeybind());
  }
}
