package com.yuseix.dragonminez.common.config;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.config.model.ConfigType;
import com.yuseix.dragonminez.common.config.model.IConfigHandler;
import com.yuseix.dragonminez.common.config.util.GsonUtil;
import com.yuseix.dragonminez.common.config.util.ModLoadUtil;
import com.yuseix.dragonminez.common.util.LogUtil;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class ConfigManager {

    public static ConfigManager INSTANCE = new ConfigManager();
    private final HashMap<String, IConfigHandler<?>> handlers = new HashMap<>();

    private ConfigManager() {
    }

    public void init() {
        this.handlers.clear();
        this.loadStaticConfigs();
        this.loadRuntimeConfigs();
    }

    public void register(IConfigHandler<?> handler) {
        if (this.handlers.containsKey(handler.identifier())) {
            LogUtil.crash("Configuration Handler with identifier " + handler.identifier() + " " +
                    "already exists.");
            return;
        }
        this.handlers.put(handler.identifier(), handler);
    }

    private void loadStaticConfigs() {
        LogUtil.info("Scanning all mods for static DMZ configurations...");
        ModLoadUtil.forEachMod((mods, mod) -> {
            final String modId = mod.getModId();
            LogUtil.info("Scanning mod " + modId + " for static DMZ configurations.");

            final Path modPath = ModLoadUtil.getModPath(mods, modId);
            if (modPath == null) return;

            this.handlers.values().stream()
                    .filter(handler -> handler.getType() == ConfigType.STATIC)
                    .forEach(handler -> {
                        final String dataDir = handler.getDataDir();
                        LogUtil.info("Processing mod {} folder '{}'.", modId, dataDir);
                        this.handleModFolder(handler, modPath, dataDir);
                    });
        });
    }

    private void handleModFolder(IConfigHandler<?> handler, Path modPath, String dataDir) {
        Path folder = null;
        if (!modPath.toString().endsWith("jar")) {
            folder = modPath.resolve("assets/" + Reference.MOD_ID).resolve(dataDir);
        } else {
            try (FileSystem fileSystem = FileSystems.newFileSystem(modPath, new HashMap<>())) {
                folder = fileSystem.getPath("/assets/" + Reference.MOD_ID).resolve(dataDir);
            } catch (Exception exception) {
                LogUtil.error("Error processing JAR file: {}", modPath);
                exception.printStackTrace(System.out);
            }
        }
        this.processFolder(handler, folder, dataDir);
    }

    private <T> void processFolder(IConfigHandler<T> handler, Path folder, String dataDir) {
        try {
            if (Files.exists(folder) && Files.isDirectory(folder)) {
                List<Path> jsonPaths = Files.walk(folder, 1)
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

                LogUtil.info("Loading config handler: {}", handler.identifier());
                LogUtil.info("Found {} JSON files in folder: {}", jsonPaths.size(), dataDir);
                for (Path path : jsonPaths) {
                    LogUtil.info("Processing JSON file: {}", path.getFileName());
                    try (InputStream inputStream = Files.newInputStream(path)) {
                        GsonUtil.loadJsonFromStream(handler.getClazz(), inputStream, object -> {
                            final String dataIdentifier = path.getFileName().toString().replace(".json",
                                    "");
                            handler.onLoaded(dataIdentifier, object);
                        });
                    }
                    LogUtil.info("Config Handler {} loaded successfully!", handler.identifier());
                }
            }
        } catch (Exception exception) {
            LogUtil.error("Error processing folder: {}", folder.toString(), exception);
        }
    }

    private void loadRuntimeConfigs() {

    }
}
