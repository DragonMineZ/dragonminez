package com.yuseix.dragonminez.common.config;

import com.yuseix.dragonminez.common.registry.ConfigRegistry;
import com.yuseix.dragonminez.core.common.config.model.ConfigDist;
import com.yuseix.dragonminez.core.common.config.model.ConfigType;
import com.yuseix.dragonminez.core.common.config.model.IConfigHandler;

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
    public void onLoaded(String key, GeneralConfig data) {
        GeneralConfig.INSTANCE = data;
    }
}
