package com.dragonminez.mod.core.server;

import com.dragonminez.mod.core.server.registry.ServerNetHandlerRegistry;

public class DragonMineCoreServer {

  public static void init() {
    DragonMineCoreServer.registry();
  }

  private static void registry() {
    ServerNetHandlerRegistry.init();
  }
}
