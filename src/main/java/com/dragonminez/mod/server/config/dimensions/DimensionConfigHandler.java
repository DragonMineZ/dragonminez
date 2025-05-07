package com.dragonminez.mod.server.config.dimensions;

import com.dragonminez.mod.common.registry.ConfigRegistry;
import com.dragonminez.mod.core.common.config.model.ConfigDist;
import com.dragonminez.mod.core.common.config.model.ConfigType;
import com.dragonminez.mod.core.common.config.model.IConfigHandler;

public class DimensionConfigHandler implements IConfigHandler<DimensionConfig> {

    @Override
    public String identifier() {
        return ConfigRegistry.DIMENSION;
    }

    @Override
    public Class<DimensionConfig> getClazz() {
        return DimensionConfig.class;
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
        return "dimensions";
    }

    @Override
    public void onLoaded(String key, DimensionConfig data) {
        DimensionsConfigManager.INSTANCE.add(key, data);
    }
}
