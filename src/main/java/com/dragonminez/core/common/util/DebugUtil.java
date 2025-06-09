package com.dragonminez.core.common.util;

import net.minecraftforge.fml.loading.FMLEnvironment;

public class DebugUtil {

  public static boolean isDebug() {
    return !FMLEnvironment.production;
  }
}
