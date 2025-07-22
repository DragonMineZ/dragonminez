package com.dragonminez.core.common.config.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public enum ConfigDist {
  CLIENT,
  SERVER,
  BOTH;

  public String id() {
    if (this == BOTH) {
      return "common";
    }
    return this.name().toLowerCase();
  }

  public static String sideId() {
    final Dist dist = FMLEnvironment.dist;
    if (dist.isClient()) {
      return ConfigDist.CLIENT.id();
    }
    return ConfigDist.SERVER.id();
  }
}
