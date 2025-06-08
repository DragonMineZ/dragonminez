package com.dragonminez.mod.server.config.food;

import com.dragonminez.mod.common.registry.ConfigRegistry;
import com.dragonminez.core.common.config.model.ConfigDist;
import com.dragonminez.core.common.config.model.ConfigType;
import com.dragonminez.core.common.config.model.IConfigHandler;

public class FoodConfigHandler implements IConfigHandler<FoodConfig> {

  @Override
  public String identifier() {
    return ConfigRegistry.FOOD;
  }

  @Override
  public Class<FoodConfig> getClazz() {
    return FoodConfig.class;
  }

  @Override
  public int getPriority() {
    return 1;
  }

  @Override
  public ConfigDist getDist() {
    return ConfigDist.SERVER;
  }

  @Override
  public ConfigType getType() {
    return ConfigType.RUNTIME;
  }

  @Override
  public boolean hasDefault() {
    return true;
  }

  @Override
  public String getStaticDataDir() {
    return "foods";
  }

  @Override
  public void onLoaded(String key, FoodConfig data) {
    FoodConfigManager.INSTANCE.register(key, data);
  }
}
