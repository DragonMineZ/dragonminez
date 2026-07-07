package com.dragonminez.server.world.structure.placement;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safety net for the unique-structure plan: once a planned chunk is fully
 * generated, verifies the structure actually exists there. If the chunk was
 * generated before the plan was ready (old worlds, async races) or vanilla
 * silently rejected the start (biome/height checks), the structure is placed
 * in-world directly, like /place does. If even that is impossible (e.g. the
 * terrain no longer satisfies the structure), the position is relocated.
 */
public final class StructureRepairManager {
	private static final int CHECK_INTERVAL_TICKS = 100;
	private static final int MAX_RELOCATIONS = 2;

	/** dimension#salt@chunk entries already verified/handled this session. */
	private static final Set<String> HANDLED = ConcurrentHashMap.newKeySet();
	/** dimension#salt → relocation attempts this session. */
	private static final Map<String, Integer> RELOCATIONS = new ConcurrentHashMap<>();

	private StructureRepairManager() {}

	public static void reset() {
		HANDLED.clear();
		RELOCATIONS.clear();
	}

	public static void tick(ServerLevel level) {
		if ((level.getGameTime() % CHECK_INTERVAL_TICKS) != 0) return;
		if (level.players().isEmpty()) return;
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return;

		Map<Integer, ChunkPos> positions = StructureSpawnPlanner.publishedPositions(level);
		if (positions.isEmpty()) return;

		Map<Integer, Holder<Structure>> structuresBySalt = null;
		for (Map.Entry<Integer, ChunkPos> entry : positions.entrySet()) {
			int salt = entry.getKey();
			ChunkPos pos = entry.getValue();
			String saltKey = level.dimension().location() + "#" + salt;
			String key = saltKey + "@" + pos.toLong();
			if (HANDLED.contains(key)) continue;

			// Only act once the chunk is fully generated and loaded (a player is
			// nearby); ungenerated chunks get the structure through normal worldgen.
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
			if (chunk == null) continue;

			if (structuresBySalt == null) structuresBySalt = structuresBySalt(level);
			Holder<Structure> structure = structuresBySalt.get(salt);
			if (structure == null) {
				HANDLED.add(key);
				continue;
			}

			StructureStart start = chunk.getStartForStructure(structure.value());
			if (start != null && start.isValid()) {
				HANDLED.add(key);
				continue;
			}

			String name = structure.unwrapKey().map(k -> k.location().toString()).orElse("salt:" + salt);
			if (forcePlace(level, structure.value(), pos)) {
				HANDLED.add(key);
				LogUtil.info(Env.SERVER, "[DMZ] Materialized missing structure " + name
						+ " at chunk " + pos.x + ", " + pos.z + " in " + level.dimension().location());
			} else {
				HANDLED.add(key);
				int attempts = RELOCATIONS.merge(saltKey, 1, Integer::sum);
				if (attempts <= MAX_RELOCATIONS) {
					LogUtil.info(Env.SERVER, "[DMZ] Could not materialize " + name
							+ " at chunk " + pos.x + ", " + pos.z + "; relocating.");
					StructureSpawnPlanner.relocate(level, salt);
				} else {
					LogUtil.error(Env.SERVER, "[DMZ] Giving up on relocating " + name
							+ " after " + MAX_RELOCATIONS + " attempts this session.");
				}
			}
		}
	}

	private static Map<Integer, Holder<Structure>> structuresBySalt(ServerLevel level) {
		Map<Integer, Holder<Structure>> result = new HashMap<>();
		for (Holder<StructureSet> holder : level.getChunkSource().getGeneratorState().possibleStructureSets()) {
			StructureSet set = holder.value();
			if (!(set.placement() instanceof BiomeAwareUniquePlacement placement)) continue;
			if (set.structures().isEmpty()) continue;
			result.put(placement.placementSalt(), set.structures().get(0).structure());
		}
		return result;
	}

	/**
	 * Generates and places the structure at the planned chunk, bypassing biome
	 * checks (the planner already picked the site), mirroring vanilla /place.
	 */
	private static boolean forcePlace(ServerLevel level, Structure structure, ChunkPos chunkPos) {
		try {
			ChunkGenerator generator = level.getChunkSource().getGenerator();
			StructureStart start = structure.generate(level.registryAccess(), generator,
					generator.getBiomeSource(), level.getChunkSource().randomState(),
					level.getStructureManager(), level.getSeed(), chunkPos, 0, level, biome -> true);
			if (!start.isValid()) return false;

			BoundingBox box = start.getBoundingBox();
			ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()),
					SectionPos.blockToSectionCoord(box.minZ()));
			ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()),
					SectionPos.blockToSectionCoord(box.maxZ()));

			ChunkPos.rangeClosed(min, max).forEach(p -> {
				level.getChunk(p.x, p.z);
				start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
						new BoundingBox(p.getMinBlockX(), level.getMinBuildHeight(), p.getMinBlockZ(),
								p.getMaxBlockX(), level.getMaxBuildHeight(), p.getMaxBlockZ()), p);
			});

			// Register the start and references so locate, maps and future
			// verification passes see the structure as properly generated.
			level.getChunk(chunkPos.x, chunkPos.z).setStartForStructure(structure, start);
			ChunkPos.rangeClosed(min, max).forEach(p ->
					level.getChunk(p.x, p.z).addReferenceForStructure(structure, chunkPos.toLong()));
			return true;
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "[DMZ] Force-placing structure at chunk " + chunkPos.x + ", "
					+ chunkPos.z + " failed: " + e.getMessage());
			return false;
		}
	}
}
