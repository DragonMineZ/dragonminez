package com.dragonminez.server.world.dimension;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class OtherworldRegionLoader {

	private static final String RESOURCE_PATH = "/data/dragonminez/regions/otherworld/";
	private static final long MIN_VALID_REGION_BYTES = 1024 * 1024;
	private static final Set<String> GENERATED_TERRAIN_BLOCKS = Set.of(
			"minecraft:air",
			"minecraft:bedrock",
			Reference.MOD_ID + ":otherworld_cloud"
	);

	public static void loadPreGeneratedRegions(MinecraftServer server) {
		try {
			Path worldPath = server.getWorldPath(LevelResource.ROOT);

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
			int repairedFiles = 0;

			for (String fileName : regionFiles) {
				Path destFile = regionDestPath.resolve(fileName);
				RegionAction action = inspectExistingRegion(fileName, destFile);

				if (action == RegionAction.COPY_MISSING && copyRegionFile(fileName, destFile)) {
					copiedFiles++;
				} else if (action.shouldRepair()) {
					backupRegionFile(destFile, action);
					if (copyRegionFile(fileName, destFile)) repairedFiles++;
				}
			}

			LogUtil.info(Env.SERVER, "Region loader finished. New: {}, Repaired: {}", copiedFiles, repairedFiles);

		} catch (IOException e) {
			LogUtil.error(Env.SERVER, "Fatal IO error loading regions: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	static RegionAction inspectExistingRegion(String fileName, Path destFile) throws IOException {
		if (!Files.exists(destFile)) return RegionAction.COPY_MISSING;

		long fileSize = Files.size(destFile);
		if (fileSize < MIN_VALID_REGION_BYTES) {
			return RegionAction.REPAIR_EMPTY;
		}

		Set<String> requiredStructureBlocks = readBundledStructureBlocks(fileName);
		if (requiredStructureBlocks.isEmpty()) {
			return RegionAction.KEEP;
		}

		Set<String> existingBlocks = readRegionPaletteBlocks(destFile, fileName);
		return existingBlocks.containsAll(requiredStructureBlocks) ? RegionAction.KEEP : RegionAction.REPAIR_STALE;
	}

	private static Set<String> readBundledStructureBlocks(String fileName) throws IOException {
		Path tempFile = Files.createTempFile("dmz-otherworld-" + fileName, ".mca");
		try {
			if (!copyRegionFile(fileName, tempFile)) {
				throw new IOException("Could not load bundled Otherworld region " + fileName);
			}
			Set<String> blocks = readRegionPaletteBlocks(tempFile, fileName);
			blocks.removeAll(GENERATED_TERRAIN_BLOCKS);
			return blocks;
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	private static Set<String> readRegionPaletteBlocks(Path regionFilePath, String fileName) throws IOException {
		Set<String> blockNames = new HashSet<>();
		RegionCoordinates coordinates = RegionCoordinates.parse(fileName);

		try (RegionFile regionFile = new RegionFile(regionFilePath, regionFilePath.getParent(), false)) {
			for (int localX = 0; localX < 32; localX++) {
				for (int localZ = 0; localZ < 32; localZ++) {
					ChunkPos chunkPos = new ChunkPos(coordinates.regionX() * 32 + localX, coordinates.regionZ() * 32 + localZ);
					try (DataInputStream stream = regionFile.getChunkDataInputStream(chunkPos)) {
						if (stream == null) continue;
						collectChunkPaletteBlocks(NbtIo.read(stream), blockNames);
					}
				}
			}
		}

		return blockNames;
	}

	private static void collectChunkPaletteBlocks(CompoundTag chunkTag, Set<String> blockNames) {
		ListTag sections = chunkTag.getList("sections", Tag.TAG_COMPOUND);
		for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
			CompoundTag section = sections.getCompound(sectionIndex);
			CompoundTag blockStates = section.getCompound("block_states");
			ListTag palette = blockStates.getList("palette", Tag.TAG_COMPOUND);
			for (int paletteIndex = 0; paletteIndex < palette.size(); paletteIndex++) {
				String blockName = palette.getCompound(paletteIndex).getString("Name");
				if (!blockName.isBlank()) {
					blockNames.add(blockName);
				}
			}
		}
	}

	private static void backupRegionFile(Path destFile, RegionAction action) throws IOException {
		Path backupFile = nextBackupPath(destFile);
		Files.copy(destFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
		LogUtil.warn(Env.SERVER, "Repairing Otherworld region file {} ({}). Backup saved as {}",
				destFile.getFileName(), action.logReason(), backupFile.getFileName());
	}

	private static Path nextBackupPath(Path destFile) {
		Path parent = destFile.getParent();
		String fileName = destFile.getFileName().toString();

		for (int index = 0; index < 1000; index++) {
			String suffix = index == 0 ? ".dmzbak" : ".dmzbak." + index;
			Path backupFile = parent.resolve(fileName + suffix);
			if (!Files.exists(backupFile)) {
				return backupFile;
			}
		}

		return parent.resolve(fileName + ".dmzbak.latest");
	}

	private static boolean copyRegionFile(String fileName, Path destFile) {
		String resourcePath = RESOURCE_PATH + fileName;
		InputStream inputStream = OtherworldRegionLoader.class.getResourceAsStream(resourcePath);
		if (inputStream == null) inputStream = OtherworldRegionLoader.class.getResourceAsStream(resourcePath.substring(1));
		if (inputStream == null) inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.substring(1));

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

	enum RegionAction {
		COPY_MISSING(null),
		REPAIR_EMPTY("empty or header-only"),
		REPAIR_STALE("missing bundled structure blocks"),
		KEEP(null);

		@Nullable
		private final String logReason;

		RegionAction(@Nullable String logReason) {
			this.logReason = logReason;
		}

		boolean shouldRepair() {
			return this == REPAIR_EMPTY || this == REPAIR_STALE;
		}

		String logReason() {
			return logReason == null ? name().toLowerCase(Locale.ROOT) : logReason;
		}
	}

	private record RegionCoordinates(int regionX, int regionZ) {
		private static RegionCoordinates parse(String fileName) throws IOException {
			if (!fileName.startsWith("r.") || !fileName.endsWith(".mca")) {
				throw new IOException("Invalid region file name " + fileName);
			}

			String[] parts = fileName.substring(2, fileName.length() - 4).split("\\.");
			if (parts.length != 2) {
				throw new IOException("Invalid region file name " + fileName);
			}

			try {
				return new RegionCoordinates(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			} catch (NumberFormatException e) {
				throw new IOException("Invalid region file name " + fileName, e);
			}
		}
	}
}
