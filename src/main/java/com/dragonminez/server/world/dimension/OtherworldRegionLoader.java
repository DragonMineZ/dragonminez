package com.dragonminez.server.world.dimension;

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
        if (level == null || level.dimension() != OtherworldDimension.OTHERWORLD_KEY) {
            return;
        }

        String worldId = level.getServer().getWorldPath(LevelResource.ROOT).toString();

        if (loadedWorlds.contains(worldId)) return;

        try {
            Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
            Path regionDestPath = worldPath.resolve("dimensions").resolve(Reference.MOD_ID).resolve("otherworld").resolve("region");
            if (!Files.exists(regionDestPath)) Files.createDirectories(regionDestPath);

            String[] regionFiles = {
                "r.0.0.mca",
                "r.0.1.mca",
                "r.0.2.mca",
                "r.0.3.mca",
                "r.1.0.mca",
                "r.1.1.mca",
                "r.1.2.mca",
                "r.1.3.mca"
            };

            for (String fileName : regionFiles) {
                String resourcePath = RESOURCE_PATH + fileName;
                Path destFile = regionDestPath.resolve(fileName);

                if (!Files.exists(destFile)) {
                    try (InputStream inputStream = OtherworldRegionLoader.class.getResourceAsStream(resourcePath)) {
                        if (inputStream != null) Files.copy(inputStream, destFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignore) {}
                }
            }

            loadedWorlds.add(worldId);

        } catch (IOException ignore) {}
    }
}

