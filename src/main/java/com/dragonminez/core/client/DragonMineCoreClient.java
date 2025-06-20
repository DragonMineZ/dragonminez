package com.dragonminez.core.client;

import com.dragonminez.core.client.registry.ClientKeybindRegistry;
import com.dragonminez.core.client.registry.ClientNetHandlerRegistry;

public class DragonMineCoreClient {

  public static void init() {
    DragonMineCoreClient.registry();
  }

  private static void registry() {
    ClientKeybindRegistry.init();
    ClientNetHandlerRegistry.init();
  }
}
