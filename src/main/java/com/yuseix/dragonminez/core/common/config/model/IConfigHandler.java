package com.yuseix.dragonminez.core.common.config.model;

import com.yuseix.dragonminez.common.Reference;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
     * <b>Determines whether the mod should search for a default configuration file</b>
     * inside the <b>assets folder</b>.
     * <p>
     * <u>Use this with caution!</u> This process can be <b>resource-intensive</b>
     * and may impact <b>performance</b> if used improperly.
     * <p>
     * <i><b>This method is for runtime configurations only.</b></i>
     */
    boolean hasDefault();

    /**
     * Called when the configuration is loaded.
     *
     * @param key  The key associated with the configuration.
     * @param data The loaded configuration data.
     */
    void onLoaded(String key, T data);

    /**
     * Gets the directory path where configuration files are stored.
     *
     * @return The path of the configuration file.
     */
    default String getDataDir() {
        if (this.getType() == ConfigType.RUNTIME) {
            return this.getStaticDataDir();
        }
        return new File(FMLPaths.CONFIGDIR.get().toString() + File.separator + Reference.MOD_ID)
                .getAbsolutePath();
    }

    /**
     * Build the static directory path where configuration files are stored.
     *
     * @return The static directory path.
     */
    default String getStaticDataDir() {
        return this.identifier();
    }

    /**
     * Used to determine if the configuration is loaded on the correct side.
     *
     * @return True if the configuration is loaded on the correct side, false otherwise.
     */
    default boolean isCorrectSide() {
        final Dist currentDist = FMLEnvironment.dist;
        final ConfigDist currentConfigDist = ConfigDist.valueOf(currentDist.name());
        return currentConfigDist == this.getDist();
    }
}
