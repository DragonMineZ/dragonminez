package com.dragonminez.server.world.dimension;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

public class OtherworldRegionLoader {

    private static final Set<String> loadedWorlds = new HashSet<>();
    private static final String RESOURCE_PATH = "/data/dragonminez/regions/otherworld/";

    public static void loadPreGeneratedRegions(ServerLevel level) {
        if (level == null || level.dimension() != OtherworldDimension.OTHERWORLD_KEY) return;

        String worldId = level.getServer().getWorldPath(LevelResource.ROOT).toString();

        if (loadedWorlds.contains(worldId)) return;

        try {
            Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
            Path regionDestPath = getOtherworldRegionPath(worldPath);

            if (!Files.exists(regionDestPath)) {
                LogUtil.info(Env.SERVER, "Creating Otherworld region directory at: {}", regionDestPath);
                Files.createDirectories(regionDestPath);
            }

            if (!Files.isWritable(regionDestPath)) {
                LogUtil.error(Env.SERVER, "Cannot write to Otherworld region directory: {}. Check file permissions!", regionDestPath);
                return;
            }

            String[] regionFiles = {
                "r.0.0.mca",
                "r.0.1.mca",
                "r.0.2.mca",
				"r.0.-1.mca",
                "r.-1.0.mca",
                "r.-1.1.mca",
                "r.-1.2.mca",
                "r.-1.-1.mca"
            };

            int copiedFiles = 0;
            int skippedFiles = 0;

            for (String fileName : regionFiles) {
                Path destFile = regionDestPath.resolve(fileName);

                if (!Files.exists(destFile)) {
                    if (copyRegionFile(fileName, destFile)) {
                        copiedFiles++;
                    }
                } else {
                    skippedFiles++;
                    LogUtil.debug(Env.SERVER, "Region file {} already exists, skipping.", fileName);
                }
            }

            LogUtil.info(Env.SERVER, "Otherworld region loader finished: {} copied, {} skipped", copiedFiles, skippedFiles);
            loadedWorlds.add(worldId);
        } catch (IOException e) {
			LogUtil.error(Env.SERVER, "Failed to load pre-generated regions for Otherworld: {}", e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
            LogUtil.error(Env.SERVER, "Unexpected error loading Otherworld regions: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private static Path getOtherworldRegionPath(Path worldPath) {
        Path forgePath = worldPath.resolve("dimensions").resolve(Reference.MOD_ID).resolve("otherworld").resolve("region");
        Path arclightPath = worldPath.resolve(Reference.MOD_ID + "_otherworld").resolve("region");

        if (Files.exists(arclightPath)) {
            LogUtil.info(Env.SERVER, "Using Arclight/Bukkit dimension path: {}", arclightPath);
            return arclightPath;
        }

        return forgePath;
    }

    private static boolean copyRegionFile(String fileName, Path destFile) {
        String resourcePath = RESOURCE_PATH + fileName;

        InputStream inputStream = OtherworldRegionLoader.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            LogUtil.debug(Env.SERVER, "Trying context ClassLoader for {}", fileName);
            inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        }

        if (inputStream == null) {
            String altPath = resourcePath.substring(1);
            LogUtil.debug(Env.SERVER, "Trying alternative path {} for {}", altPath, fileName);
            inputStream = OtherworldRegionLoader.class.getClassLoader().getResourceAsStream(altPath);
        }

        if (inputStream == null) {
            LogUtil.error(Env.SERVER, "Could not find region file {} in mod resources (tried multiple paths)", fileName);
            return false;
        }

        try {
            Files.copy(inputStream, destFile, StandardCopyOption.REPLACE_EXISTING);
            LogUtil.info(Env.SERVER, "Successfully copied region file: {}", fileName);
            return true;
        } catch (IOException e) {
            LogUtil.error(Env.SERVER, "Failed to copy region file {}: {}", fileName, e.getMessage());
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                LogUtil.warn(Env.SERVER, "Failed to close input stream for {}: {}", fileName, e.getMessage());
            }
        }
    }
}

