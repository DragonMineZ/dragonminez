package com.dragonminez.core.server.registry;

import com.dragonminez.core.common.keybind.KeybindManager;
import com.dragonminez.core.common.registry.KeybindRegistry;
import net.minecraftforge.api.distmarker.Dist;

public class ServerKeybindRegistry {

  public static void init() {
    KeybindManager.INSTANCE.register(Dist.DEDICATED_SERVER, KeybindRegistry.DEBUG_RANDOM_DATA.id(),
        KeybindRegistry.DEBUG_RANDOM_DATA);
  }

}
