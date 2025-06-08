package com.dragonminez.mod.common.config;

import com.dragonminez.mod.common.registry.ConfigRegistry;
import com.dragonminez.core.common.config.model.ConfigDist;
import com.dragonminez.core.common.config.model.ConfigType;
import com.dragonminez.core.common.config.model.IConfigHandler;

public class GeneralConfigHandler implements IConfigHandler<GeneralConfig> {

  @Override
  public String identifier() {
    return ConfigRegistry.GENERAL;
  }

  @Override
  public Class<GeneralConfig> getClazz() {
    return GeneralConfig.class;
  }

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public ConfigDist getDist() {
    return ConfigDist.BOTH;
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
    return "";
  }

  @Override
  public void onLoaded(String key, GeneralConfig data) {
    GeneralConfig.INSTANCE = data;
  }
}
