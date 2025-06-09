package com.dragonminez.core.server;

import com.dragonminez.core.server.registry.ServerKeybindHandlerRegistry;
import com.dragonminez.core.server.registry.ServerKeybindRegistry;
import com.dragonminez.core.server.registry.ServerNetHandlerRegistry;

public class DragonMineCoreServer {

  public static void init() {
    DragonMineCoreServer.registry();
  }

  private static void registry() {
    ServerKeybindRegistry.init();
    ServerKeybindHandlerRegistry.init();
    ServerNetHandlerRegistry.init();
  }
}
