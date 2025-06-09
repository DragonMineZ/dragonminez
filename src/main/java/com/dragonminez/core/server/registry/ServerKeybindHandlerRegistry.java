package com.dragonminez.core.server.registry;

import com.dragonminez.core.common.keybind.KeybindHandlerManager;
import com.dragonminez.core.common.registry.KeybindRegistry;
import com.dragonminez.core.server.keybind.ServerKeybindHandlerDebugRandomData;
import net.minecraftforge.api.distmarker.Dist;

public class ServerKeybindHandlerRegistry {

  public static void init() {
    KeybindHandlerManager.INSTANCE.register(Dist.DEDICATED_SERVER,
        KeybindRegistry.DEBUG_RANDOM_DATA.id(), new ServerKeybindHandlerDebugRandomData());
  }

}
