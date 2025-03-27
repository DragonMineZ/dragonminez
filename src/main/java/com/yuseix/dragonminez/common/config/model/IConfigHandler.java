package com.yuseix.dragonminez.common.config.model;

import com.yuseix.dragonminez.common.Reference;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

/**
 * Interface representing a configuration model for the mod.
 * Defines methods for managing configuration data.
 *
 * @param <T> The type of the configuration data.
 */
public interface IConfigHandler<T> {

    /**
     * Gets the unique identifier for this configuration.
     *
     * @return The configuration identifier.
     */
    String identifier();

    /**
     * Gets the class type of the configuration data.
     *
     * @return The class type of the configuration data.
     */
    Class<T> getClazz();

    /**
     * Gets the priority of this configuration.
     *
     * @return The priority level.
     */
    int getPriority();

    /**
     * Gets the distribution type of the configuration (client, server, or common).
     *
     * @return The distribution type.
     */
    ConfigDist getDist();

    /**
     * Gets the type of the configuration.
     *
     * @return The configuration type.
     */
    ConfigType getType();

    /**
     * Called when the configuration is loaded.
     *
     * @param key  The key associated with the configuration.
     * @param data The loaded configuration data.
     */
    void onLoaded(String key, T data);

    /**
     * Gets the default configuration instance if available.
     *
     * @return The default configuration instance, or {@code null} if not available.
     */
    default IConfigHandler<T> getDefault() {
        return null;
    }

    /**
     * Gets the directory path where configuration files are stored.
     *
     * @return The absolute path of the configuration directory.
     */
    default String getDataDir() {
        return new File(FMLPaths.CONFIGDIR.get().toString() + File.separator + Reference.MOD_ID)
                .getAbsolutePath();
    }
}
