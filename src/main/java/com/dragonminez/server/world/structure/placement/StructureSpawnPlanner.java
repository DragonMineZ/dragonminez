package com.dragonminez.server.world.structure.placement;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class StructureSpawnPlanner {
	private static final int EXCLUSION_CHUNK_RADIUS = 5;
	private static final int CENTER_CHUNK_X = 0;
	private static final int CENTER_CHUNK_Z = 0;

	private static final TreeMap<Integer, BiomeAwareUniquePlacement> REGISTERED = new TreeMap<>();
	private static final TreeMap<Integer, UniqueNearSpawnPlacement> NEAR_SPAWN_RESERVED = new TreeMap<>();

	private static long cachedSeed = Long.MIN_VALUE;
	private static BiomeSource cachedBiomeSource = null;
	private static Map<Integer, ChunkPos> cachedPlan = new HashMap<>();

	private static Field generatorField = null;
	private static Field biomeSourceField = null;

	private StructureSpawnPlanner() {}

	static synchronized void register(BiomeAwareUniquePlacement placement) {
		REGISTERED.put(placement.placementSalt(), placement);
		invalidate();
	}

	static synchronized void registerReservation(UniqueNearSpawnPlacement placement) {
		NEAR_SPAWN_RESERVED.put(placement.placementSalt(), placement);
		invalidate();
	}

	private static void invalidate() {
		cachedSeed = Long.MIN_VALUE;
		cachedBiomeSource = null;
	}

	static synchronized ChunkPos getPositionFor(BiomeAwareUniquePlacement placement, long worldSeed,
												BiomeSource biomeSource, RandomState randomState,
												ChunkGeneratorStructureState state) {
		if (biomeSource == null || randomState == null) return null;

		if (worldSeed != cachedSeed || biomeSource != cachedBiomeSource) {
			cachedPlan = computePlan(worldSeed, biomeSource, randomState, state);
			cachedSeed = worldSeed;
			cachedBiomeSource = biomeSource;
		}
		return cachedPlan.get(placement.placementSalt());
	}

	static synchronized void injectResolved(long worldSeed, BiomeSource biomeSource, int salt, ChunkPos pos) {
		if (worldSeed == cachedSeed && biomeSource == cachedBiomeSource && pos != null) {
			cachedPlan.put(salt, pos);
		}
	}

	private static Map<Integer, ChunkPos> computePlan(long worldSeed, BiomeSource biomeSource,
													  RandomState randomState, ChunkGeneratorStructureState state) {
		WorldGenSettings cfg = new WorldGenSettings();

		ChunkGenerator generator = getGeneratorReflection(state);
		LevelHeightAccessor heightAccessor = generator != null
				? LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth())
				: null;

		double minChunks = cfg.minDistanceFromSpawn / 16.0;
		double maxChunks = Math.max(minChunks + 1.0, cfg.maxDistanceFromSpawn / 16.0);
		double spacingChunks = cfg.minDistanceBetween / 16.0;
		double spacingSqr = spacingChunks * spacingChunks;

		int minRing = (int) Math.floor(minChunks);
		int maxRing = (int) Math.ceil(maxChunks);

		Map<Integer, HolderSet<Biome>> structureBiomes = buildStructureBiomes(state);
		List<Holder<StructureSet>> avoid = collectAvoidableSets(state);

		Map<Integer, ChunkPos> plan = new HashMap<>();
		List<ChunkPos> accepted = new ArrayList<>();

		for (UniqueNearSpawnPlacement reserved : NEAR_SPAWN_RESERVED.values()) {
			ChunkPos pos = reserved.getStructureChunk(worldSeed);
			if (pos != null) accepted.add(pos);
		}

		List<BiomeAwareUniquePlacement> pending = new ArrayList<>();

		for (Map.Entry<Integer, BiomeAwareUniquePlacement> entry : REGISTERED.entrySet()) {
			BiomeAwareUniquePlacement placement = entry.getValue();
			HolderSet<Biome> structBiomes = structureBiomes.get(placement.placementSalt());
			if (structBiomes == null) continue;

			ChunkPos found = searchNearest(placement, structBiomes, biomeSource, randomState, generator,
					heightAccessor, minRing, maxRing, accepted, spacingSqr, state, avoid);

			if (found != null) {
				plan.put(placement.placementSalt(), found);
				accepted.add(found);
			} else if (generator != null && heightAccessor != null) {
				pending.add(placement);
			}
		}

		if (!pending.isEmpty()) {
			StructureAsyncResolver.enqueue(worldSeed, biomeSource, randomState, generator, heightAccessor,
					maxRing, spacingSqr, new ArrayList<>(accepted), pending, structureBiomes, state, avoid);
		}

		return plan;
	}

	static ChunkPos searchNearest(BiomeAwareUniquePlacement placement, HolderSet<Biome> structureBiomes,
								  BiomeSource biomeSource, RandomState randomState, ChunkGenerator generator,
								  LevelHeightAccessor heightAccessor, int minRing, int maxRing,
								  List<ChunkPos> accepted, double spacingSqr,
								  ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid) {
		for (int ring = minRing; ring <= maxRing; ring++) {
			for (ChunkPos candidate : ringChunks(ring)) {
				if (tooClose(accepted, candidate.x, candidate.z, spacingSqr)) continue;
				if (!biomePrefilter(placement, biomeSource, randomState, candidate.x, candidate.z)) continue;
				if (!validCandidate(structureBiomes, biomeSource, randomState, generator,
						heightAccessor, candidate.x, candidate.z)) continue;
				if (overlapsOtherStructures(state, avoid, candidate.x, candidate.z)) continue;
				return candidate;
			}
		}
		return null;
	}

	static List<ChunkPos> ringChunks(int ring) {
		List<ChunkPos> out = new ArrayList<>();
		if (ring <= 0) {
			out.add(new ChunkPos(CENTER_CHUNK_X, CENTER_CHUNK_Z));
			return out;
		}
		for (int dx = -ring; dx <= ring; dx++) {
			out.add(new ChunkPos(CENTER_CHUNK_X + dx, CENTER_CHUNK_Z - ring));
			out.add(new ChunkPos(CENTER_CHUNK_X + dx, CENTER_CHUNK_Z + ring));
		}
		for (int dz = -ring + 1; dz <= ring - 1; dz++) {
			out.add(new ChunkPos(CENTER_CHUNK_X - ring, CENTER_CHUNK_Z + dz));
			out.add(new ChunkPos(CENTER_CHUNK_X + ring, CENTER_CHUNK_Z + dz));
		}
		out.sort((a, b) -> Long.compare(distSqrToCenter(a), distSqrToCenter(b)));
		return out;
	}

	private static long distSqrToCenter(ChunkPos pos) {
		long dx = (long) pos.x - CENTER_CHUNK_X;
		long dz = (long) pos.z - CENTER_CHUNK_Z;
		return dx * dx + dz * dz;
	}

	private static boolean biomePrefilter(BiomeAwareUniquePlacement placement, BiomeSource biomeSource,
										  RandomState randomState, int chunkX, int chunkZ) {
		int quartX = QuartPos.fromBlock(chunkX << 4);
		int quartZ = QuartPos.fromBlock(chunkZ << 4);
		for (int y = 200; y >= 56; y -= 16) {
			Holder<Biome> biome = biomeSource.getNoiseBiome(quartX, QuartPos.fromBlock(y), quartZ, randomState.sampler());
			if (placement.getValidBiomes().contains(biome)) return true;
		}
		return false;
	}

	static boolean validCandidate(HolderSet<Biome> structureBiomes,
								  BiomeSource biomeSource, RandomState randomState, ChunkGenerator generator,
								  LevelHeightAccessor heightAccessor, int chunkX, int chunkZ) {
		if (generator == null || heightAccessor == null) {
			return true;
		}
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;
		int surfaceY = generator.getFirstFreeHeight(startX, startZ, Heightmap.Types.WORLD_SURFACE_WG,
				heightAccessor, randomState);

		Holder<Biome> biome = biomeSource.getNoiseBiome(QuartPos.fromBlock(startX), QuartPos.fromBlock(surfaceY),
				QuartPos.fromBlock(startZ), randomState.sampler());

		if (!structureBiomes.contains(biome)) return false;

		if (!biome.is(BiomeTags.IS_OCEAN) && isFootprintSubmerged(generator, heightAccessor, randomState, startX, startZ)) {
			return false;
		}
		return true;
	}

	private static boolean isFootprintSubmerged(ChunkGenerator generator, LevelHeightAccessor heightAccessor,
												RandomState randomState, int baseX, int baseZ) {
		for (int dx = 0; dx <= 16; dx += 8) {
			for (int dz = 0; dz <= 16; dz += 8) {
				int floor = generator.getFirstFreeHeight(baseX + dx, baseZ + dz, Heightmap.Types.OCEAN_FLOOR_WG,
						heightAccessor, randomState);
				int surface = generator.getFirstFreeHeight(baseX + dx, baseZ + dz, Heightmap.Types.WORLD_SURFACE_WG,
						heightAccessor, randomState);
				if (surface > floor) return true;
			}
		}
		return false;
	}

	private static Map<Integer, HolderSet<Biome>> buildStructureBiomes(ChunkGeneratorStructureState state) {
		Map<Integer, HolderSet<Biome>> result = new HashMap<>();
		if (state == null) return result;
		for (Holder<StructureSet> holder : state.possibleStructureSets()) {
			StructureSet set = holder.value();
			if (!(set.placement() instanceof BiomeAwareUniquePlacement placement)) continue;
			if (set.structures().isEmpty()) continue;
			Structure structure = set.structures().get(0).structure().value();
			result.put(placement.placementSalt(), structure.biomes());
		}
		return result;
	}

	private static List<Holder<StructureSet>> collectAvoidableSets(ChunkGeneratorStructureState state) {
		if (state == null) return Collections.emptyList();

		List<Holder<StructureSet>> result = new ArrayList<>();
		for (Holder<StructureSet> holder : state.possibleStructureSets()) {
			StructurePlacement placement = holder.value().placement();
			if (placement instanceof BiomeAwareUniquePlacement
					|| placement instanceof UniqueNearSpawnPlacement
					|| placement instanceof FixedStructurePlacement
					|| placement instanceof ConcentricRingsStructurePlacement) {
				continue;
			}
			result.add(holder);
		}
		return result;
	}

	static boolean overlapsOtherStructures(ChunkGeneratorStructureState state,
										   List<Holder<StructureSet>> avoid, int chunkX, int chunkZ) {
		if (state == null || avoid.isEmpty()) return false;
		for (Holder<StructureSet> holder : avoid) {
			if (state.hasStructureChunkInRange(holder, chunkX, chunkZ, EXCLUSION_CHUNK_RADIUS)) {
				return true;
			}
		}
		return false;
	}

	static boolean tooClose(List<ChunkPos> accepted, int chunkX, int chunkZ, double spacingSqr) {
		if (spacingSqr <= 0) {
			return false;
		}
		for (ChunkPos other : accepted) {
			double dx = other.x - chunkX;
			double dz = other.z - chunkZ;
			if (dx * dx + dz * dz < spacingSqr) return true;
		}
		return false;
	}

	private static ChunkGenerator getGeneratorReflection(ChunkGeneratorStructureState state) {
		if (state == null) return null;
		try {
			if (generatorField == null) {
				for (Field f : ChunkGeneratorStructureState.class.getDeclaredFields()) {
					if (ChunkGenerator.class.isAssignableFrom(f.getType())) {
						f.setAccessible(true);
						generatorField = f;
						break;
					}
				}
			}
			if (generatorField != null) return (ChunkGenerator) generatorField.get(state);
		} catch (Exception e) {
			System.err.println("[DMZ] StructureSpawnPlanner could not reflect ChunkGenerator: " + e.getMessage());
		}
		return null;
	}

	static BiomeSource getBiomeSourceReflection(ChunkGeneratorStructureState state) {
		if (state == null) return null;
		try {
			if (biomeSourceField == null) {
				for (Field f : ChunkGeneratorStructureState.class.getDeclaredFields()) {
					if (BiomeSource.class.isAssignableFrom(f.getType())) {
						f.setAccessible(true);
						biomeSourceField = f;
						break;
					}
				}
			}
			if (biomeSourceField != null) return (BiomeSource) biomeSourceField.get(state);
		} catch (Exception e) {
			System.err.println("[DMZ] StructureSpawnPlanner could not reflect BiomeSource: " + e.getMessage());
		}
		return null;
	}

	private static final class WorldGenSettings {
		final int minDistanceFromSpawn;
		final int maxDistanceFromSpawn;
		final int minDistanceBetween;

		WorldGenSettings() {
			var worldGen = ConfigManager.getServerConfig().getWorldGen();
			this.minDistanceFromSpawn = worldGen.getStructureMinDistanceFromSpawn();
			this.maxDistanceFromSpawn = worldGen.getStructureMaxDistanceFromSpawn();
			this.minDistanceBetween = worldGen.getStructureMinDistanceBetween();
		}
	}
}
