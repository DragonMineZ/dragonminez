package com.dragonminez.core.server;

import com.dragonminez.core.server.registry.ServerNetHandlerRegistry;

public class DragonMineCoreServer {

  public static void init() {
    DragonMineCoreServer.registry();
  }

  private static void registry() {
    ServerNetHandlerRegistry.init();
  }
}
