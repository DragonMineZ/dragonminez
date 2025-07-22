package com.dragonminez.core.common.config;

import com.dragonminez.core.common.config.event.RegisterConfigHandlerEvent;
import com.dragonminez.core.common.config.model.ConfigDist;
import com.dragonminez.core.common.config.model.ConfigType;
import com.dragonminez.core.common.config.model.IConfigHandler;
import com.dragonminez.core.common.config.util.JacksonUtil;
import com.dragonminez.core.common.config.util.ModLoadUtil;
import com.dragonminez.core.common.util.LogUtil;
import com.dragonminez.mod.common.Reference;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Manages configuration handlers for static and runtime configurations. Responsible for loading
 * configurations from mod assets and runtime config folders.
 */
public final class ConfigManager {

  /**
   * Singleton instance of ConfigManager.
   */
  public static final ConfigManager INSTANCE = new ConfigManager();

  /**
   * Directory path to static configuration files shared across all environments.
   */
  public static final String STATIC_COMMON_DIR = "assets" + File.separator + Reference.MOD_ID
      + File.separator + "config" + File.separator + "common";

  /**
   * Directory path to static configuration files specific to the current distribution side.
   */
  public static final String STATIC_SIDE_DIR = "assets" + File.separator + Reference.MOD_ID
      + File.separator + "config" + File.separator + "%s";

  /**
   * Directory path to runtime-generated configuration files shared across all environments.
   */
  public static final String RUNTIME_COMMON_DIR =
      "assets" + File.separator + Reference.MOD_ID
          + File.separator + "config" + File.separator + "runtime" + File.separator + "common";

  /**
   * Directory path to runtime-generated configuration files specific to the current distribution
   * side.
   */
  public static final String RUNTIME_SIDE_DIR =
      "assets" + File.separator + Reference.MOD_ID
          + File.separator + "config" + File.separator + "runtime" + File.separator
          + "%s";

  /**
   * Map storing registered configuration handlers by their identifier.
   */
  private final HashMap<String, IConfigHandler<?>> handlers = new HashMap<>();

  /**
   * List of paths that do not exist. This is used to avoid duplicate loading of static
   * configurations.
   */
  private final List<String> pathsToSkip = new ArrayList<>();

  /**
   * Private constructor to enforce singleton pattern.
   */
  private ConfigManager() {
  }

  /**
   * Initializes the configuration manager by clearing handlers and loading configurations.
   */
  public void init() {
    this.fireDispatcher();
    this.loadStaticConfigs();
    this.loadRuntimeConfigs();
    this.pathsToSkip.clear();
  }

  /**
   * Registers a configuration handler.
   *
   * @param handler the configuration handler to register.
   */
  public void register(IConfigHandler<?> handler) {
    if (this.handlers.containsKey(handler.identifier())) {
      LogUtil.crash(
          "Configuration Handler with identifier " + handler.identifier() + " already exists.");
      return;
    }
    this.handlers.put(handler.identifier(), handler);
  }

  /**
   * Loads static and runtime configuration files from all loaded mods.
   * <p>
   * Static configs are loaded from the assets folder, and runtime configs are processed if they
   * have default files. This method delegates scanning logic by mod and config type.
   */
  private void loadStaticConfigs() {
    LogUtil.info("Scanning all mods for static DMZ configurations...");
    ModLoadUtil.forEachMod((mods, mod) -> {
      final String modId = mod.getModId();
      LogUtil.info("Scanning mod " + modId + " for static DMZ configurations.");
      final Path modPath = ModLoadUtil.getModPath(mods, modId);
      if (modPath == null) {
        return;
      }
      final boolean loadClientFiles = FMLEnvironment.dist != Dist.DEDICATED_SERVER;
      this.processRuntimeDefaults(modPath, loadClientFiles);
      this.processStaticConfigs(modPath, loadClientFiles);
    });
  }

  /**
   * Processes all runtime configuration handlers that have default files, scanning both common and
   * side-specific folders.
   *
   * @param modPath the root path to the mod being scanned
   */
  private void processRuntimeDefaults(Path modPath, boolean loadClientFiles) {
    this.handlers(handler -> handler.getType() == ConfigType.RUNTIME && handler.hasDefault())
        .forEach(handler -> {
          if (handler.getDist() == ConfigDist.CLIENT && !loadClientFiles) {
            LogUtil.info("Skipping client-side runtime configs for " + handler.identifier());
            return;
          }
          this.fetchModFolder(handler, modPath, handler.getStaticDataDir(),
              RUNTIME_COMMON_DIR, this::processRuntimeHandlerDefaultFiles);
          if (handler.getDist() != ConfigDist.BOTH) {
            this.fetchModFolder(handler, modPath, handler.getStaticDataDir(),
                RUNTIME_SIDE_DIR.formatted(handler.getDist().id()), this::processRuntimeHandlerDefaultFiles);
          }
        });
  }

  /**
   * Processes all static configuration handlers, scanning both common and side-specific folders.
   *
   * @param modPath the root path to the mod being scanned
   */
  private void processStaticConfigs(Path modPath, boolean loadClientFiles) {
    this.handlers(handler -> handler.getType() == ConfigType.STATIC)
        .forEach(handler -> {
          if (handler.getDist() == ConfigDist.CLIENT && !loadClientFiles) {
            LogUtil.info("Skipping client-side static configs for " + handler.identifier());
            return;
          }
          this.fetchModFolder(handler, modPath, handler.getDataDir(),
              STATIC_COMMON_DIR, this::processStaticHandlerFiles);
          if (handler.getDist() != ConfigDist.BOTH) {
            this.fetchModFolder(handler, modPath, handler.getDataDir(),
                STATIC_SIDE_DIR.formatted(handler.getDist().id()), this::processStaticHandlerFiles);
          }
        });
  }

  /**
   * Generic method to get a mod folder and process JSON files within.
   *
   * @param handler      the configuration handler.
   * @param modPath      the mod file system path.
   * @param dataDir      the data directory inside the mod.
   * @param baseDir      the base configuration directory (static or default).
   * @param fileConsumer the consumer that processes each JSON file.
   * @param <T>          the type of configuration object.
   */
  private <T> void fetchModFolder(IConfigHandler<T> handler, Path modPath, String dataDir,
      String baseDir, FileProcessor<T> fileConsumer) {
    if (!modPath.toString().endsWith("jar")) {
      final Path folder = modPath.resolve(baseDir);
      if (this.pathsToSkip.contains(folder.toString())) {
        return;
      }
      if (Files.exists(folder) && Files.isDirectory(folder)) {
        fileConsumer.process(handler, folder, dataDir);
      } else {
        if (!this.pathsToSkip.contains(folder.toString())) {
          this.pathsToSkip.add(folder.toString());
        }
        LogUtil.error("Folder doesn't exist: " + folder);
      }
    } else {
      try (FileSystem fileSystem = FileSystems.newFileSystem(modPath, new HashMap<>())) {
        final Path folder = fileSystem.getPath(baseDir);
        if (this.pathsToSkip.contains(folder.toString())) {
          return;
        }
        if (Files.exists(folder) && Files.isDirectory(folder)) {
          fileConsumer.process(handler, folder, dataDir);
        } else {
          if (!this.pathsToSkip.contains(folder.toString())) {
            this.pathsToSkip.add(folder.toString());
          }
          LogUtil.error(
              "Folder '%s' doesn't exist in JAR. This is normal on DEV ENV.".formatted(folder));
        }
      } catch (Exception exception) {
        LogUtil.crash("Error processing JAR file: " + modPath, exception);
      }
    }
  }


  /**
   * Processes JSON files for runtime handler default configurations by copying them to the runtime
   * folder.
   *
   * @param handler the configuration handler.
   * @param folder  the folder containing JSON files.
   * @param dataDir the data directory identifier.
   * @param <T>     the type of configuration object.
   */
  private <T> void processRuntimeHandlerDefaultFiles(IConfigHandler<T> handler, Path folder,
      String dataDir) {
    this.processJsonFiles(handler, folder, dataDir, (Path path) -> {
      final String dataIdentifier = path.getFileName().toString()
          .replace(JacksonUtil.FILE_EXTENSION, "");
      final String destinationPath = Paths.get(handler.getDataDir(),
          dataIdentifier + JacksonUtil.FILE_EXTENSION).toString();

      if (Files.exists(Paths.get(destinationPath))) {
        LogUtil.info(
            "Skipping default config '" + path + "' as it already exists in '" + destinationPath
                + "'");
        return;
      }

      try (InputStream stream = Files.newInputStream(path)) {
        JacksonUtil.copyStreamToFile(stream, destinationPath);
        LogUtil.info("Copied default config '" + path + "' to '" + destinationPath + "'");
      } catch (IOException e) {
        LogUtil.crash("Error copying default config '" + path + "'. " +
            "Did you add the file on the assets folder?", e);
      }
    });
  }

  /**
   * Processes JSON files for static handler configurations by loading their content. It avoids
   * duplicate loading for the main mod.
   *
   * @param handler the configuration handler.
   * @param folder  the folder containing JSON files.
   * @param dataDir the data directory identifier.
   * @param <T>     the type of configuration object.
   */
  private <T> void processStaticHandlerFiles(IConfigHandler<T> handler, Path folder,
      String dataDir) {
    final List<String> visitedConfigs = new ArrayList<>();
    this.processJsonFiles(handler, folder, dataDir, (Path path) -> {
      final String dataIdentifier = path.getFileName().toString()
          .replace(JacksonUtil.FILE_EXTENSION, "");
      if (visitedConfigs.contains(dataIdentifier) && Reference.MOD_ID.equals(
          handler.identifier())) {
        LogUtil.info("Skipping " + Reference.MOD_ID + " static config '" + dataIdentifier +
            "' as it has already been loaded by another mod.");
        return;
      }
      try (InputStream inputStream = Files.newInputStream(path)) {
        JacksonUtil.loadJsonFromStream(handler.getClazz(), inputStream, (object) -> {
          visitedConfigs.add(dataIdentifier);
          handler.onLoaded(dataIdentifier, object);
        });
      } catch (IOException e) {
        LogUtil.crash("Error loading static config: " + path, e);
      }
    });
  }

  /**
   * Processes JSON configuration files in a folder using a provided file consumer.
   *
   * @param handler      the configuration handler.
   * @param folder       the folder containing JSON configuration files.
   * @param dataDir      the data directory identifier.
   * @param fileConsumer a consumer that processes each JSON file.
   * @param <T>          the type of configuration object.
   */
  private <T> void processJsonFiles(IConfigHandler<T> handler, Path folder, String dataDir,
      Consumer<Path> fileConsumer) {
    try {
      if (!dataDir.isEmpty()) {
        folder = folder.resolve(dataDir);
      }
      if (Files.exists(folder) && Files.isDirectory(folder)) {
        try (var paths = Files.walk(folder, 1)) {
          List<Path> jsonPaths = paths.filter(Files::isRegularFile)
              .filter((Path path) -> path.toString().endsWith(JacksonUtil.FILE_EXTENSION))
              .toList();
          if (dataDir.isEmpty()) {
            jsonPaths = jsonPaths.stream()
                .filter((Path path) -> path.toString().contains(handler.identifier()))
                .toList();
          }
          if (jsonPaths.isEmpty()) {
            return;
          }
          jsonPaths.forEach(fileConsumer);
        }
      }
    } catch (Exception exception) {
      LogUtil.crash("Error processing folder: " + folder, exception);
    }
  }

  /**
   * Loads runtime configurations from the configuration folder.
   */
  private void loadRuntimeConfigs() {
    LogUtil.info("Scanning config folder for runtime DMZ configurations...");
    this.handlers(handler -> handler.getType() == ConfigType.RUNTIME)
        .forEach((IConfigHandler<?> handler) ->
            JacksonUtil.getFilesInDirectory(handler.getDataDir(), JacksonUtil.FILE_EXTENSION)
                .forEach((File file) -> this.processRuntimeFile(handler, file))
        );
  }

  /**
   * Processes a single runtime configuration file.
   *
   * @param handler the configuration handler.
   * @param file    the configuration file.
   * @param <T>     the type of configuration object.
   */
  private <T> void processRuntimeFile(IConfigHandler<T> handler, File file) {
    final String identifier = file.getName().replaceFirst("[.][^.]+$", "");
    JacksonUtil.loadJsonFromFile(handler.getClazz(), file, (object) -> {
      handler.onLoaded(identifier, object);
      LogUtil.info(
          "Loaded runtime config '" + identifier + "' from file '" + file.getAbsolutePath() + "'");
    });
  }

  /**
   * Saves runtime configuration data to a file.
   *
   * @param handlerID  the identifier of the configuration handler.
   * @param identifier the identifier for the configuration data.
   * @param data       the data to save.
   * @param log        whether to log the save operation.
   * @param <T>        the type of configuration object.
   */
  public <T> void saveRuntime(String handlerID, String identifier, T data, boolean log) {
    final IConfigHandler<?> handler = this.handler(handlerID);
    if (handler == null) {
      LogUtil.crash("Configuration Handler with identifier " + handlerID
          + " does not exist. Cannot be saved.");
      return;
    }
    if (handler.getType() != ConfigType.RUNTIME) {
      LogUtil.crash("Cannot save static configuration data for " + handlerID + " with identifier "
          + identifier);
      return;
    }
    try {
      final String dataDir = handler.getDataDir();
      JacksonUtil.saveJson(data, dataDir, identifier);
      if (log) {
        LogUtil.info("Saved data for {} in {}!", identifier, dataDir);
      }
    } catch (IOException e) {
      LogUtil.error("Could not save data for {}!", identifier);
      e.printStackTrace(System.out);
    }
  }

  /**
   * Saves runtime configuration data to a file (with logging enabled by default).
   *
   * @param handlerID  the identifier of the configuration handler.
   * @param identifier the identifier for the configuration data.
   * @param data       the data to save.
   * @param <T>        the type of configuration object.
   */
  public <T> void saveRuntime(String handlerID, String identifier, T data) {
    this.saveRuntime(handlerID, identifier, data, true);
  }

  /**
   * Deletes runtime configuration data from a file.
   *
   * @param handlerID  the identifier of the configuration handler.
   * @param identifier the identifier for the configuration data to delete.
   * @param log        whether to log the deletion.
   */
  public void deleteRuntime(String handlerID, String identifier, boolean log) {
    final IConfigHandler<?> handler = this.handler(handlerID);
    if (handler == null) {
      LogUtil.crash("Configuration Handler with identifier " + handlerID
          + " does not exist. Cannot be deleted.");
      return;
    }
    if (handler.getType() != ConfigType.RUNTIME) {
      LogUtil.crash("Cannot delete static configuration data for " + handlerID + " with identifier "
          + identifier);
      return;
    }
    final String dataDir = handler.getDataDir();
    if (log) {
      final boolean result = JacksonUtil.deleteJson(dataDir, identifier);
      if (!result) {
        LogUtil.error("Could not delete data for {} in {}!", identifier, dataDir);
      } else {
        LogUtil.info("Deleted data for {} in {}!", identifier, dataDir);
      }
    }
  }

  /**
   * Fires an event to register configuration handlers.
   */
  private void fireDispatcher() {
    MinecraftForge.EVENT_BUS.start();
    MinecraftForge.EVENT_BUS.post(new RegisterConfigHandlerEvent(this));
  }

  /**
   * Returns a list of configuration handlers filtered by a given predicate.
   *
   * @param predicate the filter condition.
   * @return a sorted list of configuration handlers.
   */
  public List<IConfigHandler<?>> handlers(Predicate<IConfigHandler<?>> predicate) {
    final List<IConfigHandler<?>> list = new ArrayList<>(this.handlers.values().stream()
        .filter(predicate)
        .filter(IConfigHandler::isCorrectSide)
        .toList());
    list.sort(Comparator.comparingInt(IConfigHandler::getPriority));
    return list;
  }

  /**
   * Retrieves a configuration handler by its identifier.
   *
   * @param identifier the handler identifier.
   * @return the corresponding configuration handler or null if not found.
   */
  public IConfigHandler<?> handler(String identifier) {
    return this.handlers(handler -> handler.identifier().equals(identifier))
        .stream().findFirst().orElse(null);
  }

  /**
   * Functional interface for processing JSON configuration files.
   *
   * @param <T> the type of configuration object.
   */
  @FunctionalInterface
  private interface FileProcessor<T> {

    void process(IConfigHandler<T> handler, Path folder, String dataDir);
  }
}
