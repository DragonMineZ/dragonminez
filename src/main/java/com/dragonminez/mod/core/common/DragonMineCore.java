package com.dragonminez.mod.core.common;

import com.dragonminez.mod.core.client.DragonMineCoreClient;
import com.dragonminez.mod.core.server.DragonMineCoreServer;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class DragonMineCore {

  public static void init() {
    DragonMineCore.instance();
  }

  private static void instance() {
    if (FMLEnvironment.dist.isClient()) {
      DragonMineCoreClient.init();
    }
    DragonMineCoreServer.init();
  }
}
