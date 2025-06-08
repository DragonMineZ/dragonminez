package com.dragonminez.mod.server.config.food;

import com.dragonminez.mod.common.registry.ConfigRegistry;
import com.dragonminez.core.common.manager.ListManager;

public class FoodConfigManager extends ListManager<String, FoodConfig> {

  public static final FoodConfigManager INSTANCE = new FoodConfigManager();

  private FoodConfigManager() {
  }

  @Override
  public String identifier() {
    return ConfigRegistry.FOOD;
  }

  @Override
  public boolean uniqueKeys() {
    return true;
  }

  @Override
  public LogMode logMode() {
    return LogMode.LOG_ALL;
  }
}
