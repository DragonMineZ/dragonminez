package com.dragonminez.mod.core.client;

import com.dragonminez.mod.core.client.registry.ClientNetHandlerRegistry;

public class DragonMineCoreClient {

  public static void init() {
    DragonMineCoreClient.registry();
  }

  private static void registry() {
    ClientNetHandlerRegistry.init();
  }
}
