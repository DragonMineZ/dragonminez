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
		if (level == null || !level.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return;

		String worldId = level.getServer().getWorldPath(LevelResource.ROOT).toString();
		if (loadedWorlds.contains(worldId)) return;

		try {
			Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);

			Path regionDestPath = worldPath
					.resolve("dimensions")
					.resolve(Reference.MOD_ID)
					.resolve("otherworld")
					.resolve("region");

			if (!Files.exists(regionDestPath)) {
				LogUtil.info(Env.SERVER, "Creating region directory at: {}", regionDestPath);
				Files.createDirectories(regionDestPath);
			}

			String[] regionFiles = {"r.0.0.mca", "r.0.1.mca", "r.0.2.mca", "r.0.-1.mca", "r.-1.0.mca", "r.-1.1.mca", "r.-1.2.mca", "r.-1.-1.mca"};

			int copiedFiles = 0;
			int replacedFiles = 0;

			for (String fileName : regionFiles) {
				Path destFile = regionDestPath.resolve(fileName);
				boolean shouldCopy = false;

				if (!Files.exists(destFile)) shouldCopy = true;
				else {
					long fileSize = Files.size(destFile);
					if (fileSize < (1024 * 1024)) {
						LogUtil.warn(Env.SERVER, "Detected empty/header-only region file {} ({} bytes). Overwriting...", fileName, fileSize);
						shouldCopy = true;
						replacedFiles++;
					}
				}

				if (shouldCopy) if (copyRegionFile(fileName, destFile)) copiedFiles++;
			}

			LogUtil.info(Env.SERVER, "Region loader finished. New: {}, Replaced (Empty): {}", copiedFiles, replacedFiles);
			LogUtil.info(Env.SERVER, "Initializing Otherworld NPCs spawns...");
			OtherworldNPCSpawner.spawnNPCs(level);
			loadedWorlds.add(worldId);

		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Fatal IO error loading regions: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	private static boolean copyRegionFile(String fileName, Path destFile) {
		String resourcePath = RESOURCE_PATH + fileName;
		InputStream inputStream = OtherworldRegionLoader.class.getResourceAsStream(resourcePath);
		if (inputStream == null) inputStream = OtherworldRegionLoader.class.getResourceAsStream(resourcePath.substring(1));
		if (inputStream == null) inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);

		if (inputStream == null) {
			LogUtil.error(Env.SERVER, "FATAL: Could not find {} in JAR resources at {}", fileName, resourcePath);
			return false;
		}

		try (InputStream stream = inputStream) {
			Files.copy(stream, destFile, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Failed to copy {}: {}", fileName, e.getMessage());
			return false;
		}
	}
}

