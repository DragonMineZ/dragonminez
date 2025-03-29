package com.yuseix.dragonminez.common.config;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.config.event.RegisterConfigHandlerEvent;
import com.yuseix.dragonminez.common.config.model.ConfigType;
import com.yuseix.dragonminez.common.config.model.IConfigHandler;
import com.yuseix.dragonminez.common.config.util.GsonUtil;
import com.yuseix.dragonminez.common.config.util.ModLoadUtil;
import com.yuseix.dragonminez.common.util.LogUtil;
import net.minecraftforge.common.MinecraftForge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

/**
 * Manages configuration handlers for static and runtime configurations.
 * Responsible for loading configurations from mod assets and runtime config folders.
 */
public class ConfigManager {

    /**
     * Singleton instance of ConfigManager
     */
    public static ConfigManager INSTANCE = new ConfigManager();

    /**
     * Map storing registered configuration handlers by their identifier.
     */
    private final HashMap<String, IConfigHandler<?>> handlers = new HashMap<>();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ConfigManager() {
    }

    /**
     * Initializes the configuration manager by clearing handlers and loading configurations.
     */
    public void init() {
        this.handlers.clear();
        this.loadStaticConfigs();
        this.loadRuntimeConfigs();
    }

    /**
     * Registers a configuration handler.
     *
     * @param handler The configuration handler to register.
     */
    public void register(IConfigHandler<?> handler) {
        if (this.handlers.containsKey(handler.identifier())) {
            LogUtil.crash("Configuration Handler with identifier " + handler.identifier() + " already exists.");
            return;
        }
        this.handlers.put(handler.identifier(), handler);
    }

    /**
     * Loads static configurations from mod assets.
     */
    private void loadStaticConfigs() {
        LogUtil.info("Scanning all mods for static DMZ configurations...");
        this.fireDispatcher(ConfigType.STATIC);
        ModLoadUtil.forEachMod((mods, mod) -> {
            final String modId = mod.getModId();
            LogUtil.info("Scanning mod " + modId + " for static DMZ configurations.");

            final Path modPath = ModLoadUtil.getModPath(mods, modId);
            if (modPath == null) return;

            this.handlers(handler -> handler.getType() == ConfigType.STATIC)
                    .forEach(handler -> this.handleModFolder(handler, modId, modPath,
                            handler.getDataDir()));
        });
    }

    /**
     * Processes a mod folder to load static configurations.
     *
     * @param handler The configuration handler.
     * @param modID   The mod that is currently being loaded.
     * @param modPath The path to the mod files.
     * @param dataDir The data directory inside the mod.
     */
    private void handleModFolder(IConfigHandler<?> handler, String modID, Path modPath, String dataDir) {
        Path folder = null;
        if (!modPath.toString().endsWith("jar")) {
            folder = modPath.resolve("assets/" + Reference.MOD_ID).resolve(dataDir);
        } else {
            try (FileSystem fileSystem = FileSystems.newFileSystem(modPath, new HashMap<>())) {
                folder = fileSystem.getPath("/assets/" + Reference.MOD_ID).resolve(dataDir);
            } catch (Exception exception) {
                LogUtil.crash("Error processing JAR file: %s".formatted(modPath));
            }
        }
        this.processFolder(handler, modID, folder, dataDir);
    }

    /**
     * Processes JSON configuration files from a given folder.
     *
     * @param handler The configuration handler.
     * @param modID   The mod that is currently being loaded.
     * @param folder  The folder containing configuration files.
     * @param dataDir The configuration data directory.
     */
    private <T> void processFolder(IConfigHandler<T> handler, String modID, Path folder, String dataDir) {
        try {
            if (Files.exists(folder) && Files.isDirectory(folder)) {
                try (var paths = Files.walk(folder, 1)) {
                    List<Path> jsonPaths = paths
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".json"))
                            .toList();

                    if (jsonPaths.isEmpty()) {
                        return;
                    }

                    if (dataDir.isEmpty()) {
                        jsonPaths = jsonPaths.stream()
                                .filter(path -> path.toString().contains(handler.identifier()))
                                .toList();
                    }

                    final List<String> visitedConfigs = new ArrayList<>();
                    for (Path path : jsonPaths) {
                        final String dataIdentifier = path.getFileName().toString().replace(".json", "");
                        if (visitedConfigs.contains(dataIdentifier) && modID.equals(Reference.MOD_ID)) {
                            LogUtil.info("Skipping " + Reference.MOD_ID + " static config '" + dataIdentifier + "' " +
                                    "as it has already been loaded by another mod.");
                            continue;
                        }
                        try (InputStream inputStream = Files.newInputStream(path)) {
                            GsonUtil.loadJsonFromStream(handler.getClazz(), inputStream, object -> {
                                visitedConfigs.add(dataIdentifier);
                                handler.onLoaded(dataIdentifier, object);
                            });
                        }
                    }
                }
            }
        } catch (Exception exception) {
            LogUtil.crash("Error processing folder: %s".formatted(folder.toString()), exception);
        }
    }

    /**
     * Loads runtime configurations from the configuration folder.
     */
    private void loadRuntimeConfigs() {
        LogUtil.info("Scanning config folder for runtime DMZ configurations...");
        this.fireDispatcher(ConfigType.RUNTIME);
        this.handlers(handler -> handler.getType() == ConfigType.RUNTIME)
                .forEach(handler -> GsonUtil.getFilesInDirectory(handler.getDataDir(), ".json")
                        .forEach(file -> this.processFile(handler, file)));
    }

    /**
     * Processes a single configuration file.
     *
     * @param handler The configuration handler.
     * @param file    The file to process.
     */
    private <T> void processFile(IConfigHandler<T> handler, File file) {
        final String identifier = file.getName().replaceFirst("[.][^.]+$", "");
        GsonUtil.loadJsonFromFile(handler.getClazz(), file, object -> handler.onLoaded(identifier, object));
    }

    /**
     * Saves runtime configuration data to a file.
     *
     * @param handlerID  The identifier of the configuration handler.
     * @param identifier The identifier for the configuration data.
     * @param data       The data to save.
     * @param log       Whether to log the save operation.
     */
    public <T> void saveRuntime(String handlerID, String identifier, T data, boolean log) {
        final IConfigHandler<?> handler = this.handler(handlerID);
        if (handler == null) {
            LogUtil.crash("Configuration Handler with identifier " + handlerID + " does not exist. " +
                    "Cannot be saved.");
            return;
        }

        if (handler.getType() != ConfigType.RUNTIME) {
            LogUtil.crash("Cannot save static configuration data for " + handlerID + " with identifier "
                    + identifier);
            return;
        }

        try {
            final String dataDir = handler.getDataDir();
            GsonUtil.saveJson(data, handler.getDataDir(), identifier);
            if (log) {
                LogUtil.info("Saved data for {} in {}!", identifier, dataDir);
            }
        } catch (IOException e) {
            LogUtil.error("Could not save data for {}!", identifier);
            e.printStackTrace(System.out);
        }
    }

    /**
     * Saves runtime configuration data to a file.
     *
     * @param handlerID  The identifier of the configuration handler.
     * @param identifier The identifier for the configuration data.
     * @param data       The data to save.
     */
    public <T> void saveRuntime(String handlerID, String identifier, T data) {
        ConfigManager.INSTANCE.saveRuntime(handlerID, identifier, data, true);
    }

    /**
     * Deletes runtime configuration data from a file.
     *
     * @param handlerID  The identifier of the configuration handler.
     * @param identifier The identifier for the configuration data to delete.
     * @param log        Whether to log the deletion.
     */
    public void deleteRuntime(String handlerID, String identifier, boolean log) {
        final IConfigHandler<?> handler = this.handler(handlerID);
        if (handler == null) {
            LogUtil.crash("Configuration Handler with identifier " + handlerID + " does not exist. " +
                    "Cannot be deleted.");
            return;
        }

        if (handler.getType() != ConfigType.RUNTIME) {
            LogUtil.crash("Cannot delete static configuration data for " + handlerID + " with identifier "
                    + identifier);
            return;
        }

        final String dataDir = handler.getDataDir();
        GsonUtil.deleteJson(dataDir, identifier);
        if (log) {
            LogUtil.info("Deleted data for {} in {}!", identifier, dataDir);
        }
    }

    /**
     * Fires an event to register configuration handlers.
     *
     * @param type The type of configuration (STATIC or RUNTIME).
     */
    private void fireDispatcher(ConfigType type) {
        MinecraftForge.EVENT_BUS.start();
        MinecraftForge.EVENT_BUS.post(new RegisterConfigHandlerEvent(this, type));
    }

    /**
     * Returns a list of handlers filtered by a given predicate.
     *
     * @param predicate The filter condition.
     * @return A sorted list of configuration handlers.
     */
    public List<IConfigHandler<?>> handlers(Predicate<IConfigHandler<?>> predicate) {
        List<IConfigHandler<?>> handlers = new ArrayList<>(this.handlers.values().stream()
                .filter(predicate)
                .filter(IConfigHandler::isCorrectSide)
                .toList());
        handlers.sort(Comparator.comparingInt(IConfigHandler::getPriority));
        return handlers;
    }

    /**
     * Retrieves a configuration handler by its identifier.
     *
     * @param identifier The handler identifier.
     * @return The corresponding handler or null if not found.
     */
    public IConfigHandler<?> handler(String identifier) {
        return this.handlers(handler -> handler.identifier().equals(identifier))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
