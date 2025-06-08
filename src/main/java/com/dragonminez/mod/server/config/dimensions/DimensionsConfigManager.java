package com.dragonminez.mod.server.config.dimensions;

import com.dragonminez.mod.common.registry.ConfigRegistry;
import com.dragonminez.core.common.manager.ListManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class DimensionsConfigManager extends ListManager<String, DimensionConfig> {

  public static final DimensionsConfigManager INSTANCE = new DimensionsConfigManager();

  private DimensionsConfigManager() {
  }

  /**
   * Get the dimension config for the given location.
   *
   * @param location The location of the dimension.
   * @return The dimension config.
   */
  public DimensionConfig get(ResourceLocation location) {
    return this.get(location.toString()).stream()
        .findFirst()
        .orElse(null);
  }

  /**
   * Get the dimension config for the given key.
   *
   * @param key The key of the dimension.
   * @return The dimension config.
   */
  public DimensionConfig get(ResourceKey<Level> key) {
    return this.get(key.location());
  }

  @Override
  public String identifier() {
    return ConfigRegistry.DIMENSION;
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
