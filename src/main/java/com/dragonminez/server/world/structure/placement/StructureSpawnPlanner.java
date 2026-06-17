package com.dragonminez.server.world.structure.placement;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.structure.TallJigsawStructure;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StructureSpawnPlanner {
	private static final int EXCLUSION_CHUNK_RADIUS = 5;
	private static final int CENTER_CHUNK_X = 0;
	private static final int CENTER_CHUNK_Z = 0;

	private static final int RING_STEP = 64;
	private static final int TAIL_EXTRA_RINGS = 1000;
	private static final long BUILD_AWAIT_SECONDS = 5L;
	private static final int FLATNESS_MAX_SPREAD = 12;

	private static final TreeMap<Integer, BiomeAwareUniquePlacement> REGISTERED = new TreeMap<>();
	private static final TreeMap<Integer, UniqueNearSpawnPlacement> NEAR_SPAWN_RESERVED = new TreeMap<>();

	private static final ConcurrentHashMap<PlanKey, PlanHolder> PLANS = new ConcurrentHashMap<>();
	private static volatile PlanHolder lastHolder = null;
	private static volatile int planEpoch = 0;

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

	public static void reset() {
		invalidate();
	}

	private static synchronized void invalidate() {
		planEpoch++;
		PLANS.clear();
		lastHolder = null;
	}

	private static boolean isStale(int buildEpoch) {
		return buildEpoch != planEpoch;
	}

	public static void prewarm(long worldSeed, RandomState randomState, ChunkGeneratorStructureState state) {
		if (randomState == null || state == null) return;
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return;
		BiomeSource biomeSource = getBiomeSourceReflection(state);
		if (biomeSource == null) return;
		PlanHolder holder = obtainHolder(worldSeed, biomeSource, randomState, state);
		ensureBuildStarted(holder);
	}

	static ChunkPos getPositionFor(BiomeAwareUniquePlacement placement, long worldSeed,
								   BiomeSource biomeSource, RandomState randomState,
								   ChunkGeneratorStructureState state) {
		if (biomeSource == null || randomState == null) return null;

		PlanHolder holder = obtainHolder(worldSeed, biomeSource, randomState, state);
		ensureBuildStarted(holder);

		Map<Integer, ChunkPos> positions = holder.positions;
		if (positions == null) {
			holder.awaitReady(BUILD_AWAIT_SECONDS);
			positions = holder.positions;
			if (positions == null) return null;
		}
		return positions.get(placement.placementSalt());
	}

	private static PlanHolder obtainHolder(long worldSeed, BiomeSource biomeSource,
										   RandomState randomState, ChunkGeneratorStructureState state) {
		PlanHolder cached = lastHolder;
		if (cached != null && cached.seed == worldSeed && cached.biomeSource == biomeSource) {
			return cached;
		}
		PlanKey key = new PlanKey(worldSeed, biomeSource);
		PlanHolder holder = PLANS.computeIfAbsent(key,
				k -> new PlanHolder(worldSeed, biomeSource, randomState, state));
		lastHolder = holder;
		return holder;
	}

	private static void ensureBuildStarted(PlanHolder holder) {
		if (holder.started.compareAndSet(false, true)) {
			StructureAsyncResolver.buildPlan(holder);
		}
	}

	static void injectResolved(PlanHolder holder, int salt, ChunkPos pos) {
		if (holder == null || pos == null) return;
		synchronized (holder.writeLock) {
			Map<Integer, ChunkPos> current = holder.positions;
			Map<Integer, ChunkPos> next = current == null ? new HashMap<>() : new HashMap<>(current);
			next.put(salt, pos);
			holder.positions = Collections.unmodifiableMap(next);
		}
	}

	static List<BiomeAwareUniquePlacement> runBuild(PlanHolder holder, ForkJoinPool searchPool) {
		final int epoch = planEpoch;
		final long worldSeed = holder.seed;
		final BiomeSource biomeSource = holder.biomeSource;
		final RandomState randomState = holder.randomState;
		final ChunkGeneratorStructureState state = holder.state;

		WorldGenSettings cfg = new WorldGenSettings();

		final ChunkGenerator generator = getGeneratorReflection(state);
		final LevelHeightAccessor heightAccessor = generator != null
				? LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth())
				: null;

		double minChunks = cfg.minDistanceFromSpawn / 16.0;
		double maxChunks = Math.max(minChunks + 1.0, cfg.maxDistanceFromSpawn / 16.0);
		double spacingChunks = cfg.minDistanceBetween / 16.0;
		final double spacingSqr = spacingChunks * spacingChunks;

		final int minRing = (int) Math.floor(minChunks);
		final int maxRing = (int) Math.ceil(maxChunks);

		final Map<Integer, HolderSet<Biome>> structureBiomes = buildStructureBiomes(state);
		final Map<Integer, Integer> structureMinHeights = buildStructureMinHeights(state);
		final List<Holder<StructureSet>> avoid = collectAvoidableSets(state);

		final List<ChunkPos> reservedBaseline = new ArrayList<>();
		for (UniqueNearSpawnPlacement reserved : NEAR_SPAWN_RESERVED.values()) {
			ChunkPos pos = reserved.getStructureChunk(worldSeed);
			if (pos != null) reservedBaseline.add(pos);
		}

		final List<BiomeAwareUniquePlacement> targets = new ArrayList<>();
		for (BiomeAwareUniquePlacement placement : REGISTERED.values()) {
			if (structureBiomes.get(placement.placementSalt()) != null) targets.add(placement);
		}

		final Map<Integer, ChunkPos> independent = new ConcurrentHashMap<>();
		searchIndependent(targets, structureBiomes, structureMinHeights, biomeSource, randomState, generator, heightAccessor,
				minRing, maxRing, reservedBaseline, spacingSqr, state, avoid, independent, searchPool, epoch);

		Map<Integer, ChunkPos> plan = new HashMap<>();
		List<ChunkPos> accepted = new ArrayList<>(reservedBaseline);
		List<BiomeAwareUniquePlacement> notFound = new ArrayList<>();

		for (BiomeAwareUniquePlacement placement : targets) {
			if (isStale(epoch)) break;
			int salt = placement.placementSalt();
			ChunkPos candidate = independent.get(salt);
			if (candidate != null && !tooClose(accepted, candidate.x, candidate.z, spacingSqr)) {
				plan.put(salt, candidate);
				accepted.add(candidate);
				continue;
			}
			ChunkPos reconciled = searchNearest(placement, structureBiomes.get(salt), biomeSource, randomState,
					generator, heightAccessor, minRing, maxRing, accepted, spacingSqr, state, avoid,
					structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE), epoch);
			if (reconciled != null) {
				plan.put(salt, reconciled);
				accepted.add(reconciled);
			} else if (generator != null && heightAccessor != null && !isStale(epoch)) {
				notFound.add(placement);
			}
		}

		holder.publish(plan);

		if (!notFound.isEmpty() && !isStale(epoch)) {
			resolveTail(holder, notFound, structureBiomes, structureMinHeights, biomeSource, randomState, generator, heightAccessor,
					maxRing, spacingSqr, accepted, state, avoid, epoch);
		}
		return notFound;
	}

	private static void searchIndependent(List<BiomeAwareUniquePlacement> targets,
										  Map<Integer, HolderSet<Biome>> structureBiomes,
										  Map<Integer, Integer> structureMinHeights, BiomeSource biomeSource,
										  RandomState randomState, ChunkGenerator generator,
										  LevelHeightAccessor heightAccessor, int minRing, int maxRing,
										  List<ChunkPos> reservedBaseline, double spacingSqr,
										  ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid,
										  Map<Integer, ChunkPos> out, ForkJoinPool searchPool, int epoch) {
		Runnable work = () -> targets.parallelStream().forEach(placement -> {
			if (isStale(epoch)) return;
			int salt = placement.placementSalt();
			ChunkPos found = searchNearest(placement, structureBiomes.get(salt), biomeSource,
					randomState, generator, heightAccessor, minRing, maxRing, reservedBaseline, spacingSqr, state, avoid,
					structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE), epoch);
			if (found != null) out.put(salt, found);
		});

		try {
			searchPool.submit(work).get();
		} catch (Exception e) {
			out.clear();
			for (BiomeAwareUniquePlacement placement : targets) {
				if (isStale(epoch)) return;
				int salt = placement.placementSalt();
				ChunkPos found = searchNearest(placement, structureBiomes.get(salt), biomeSource,
						randomState, generator, heightAccessor, minRing, maxRing, reservedBaseline, spacingSqr, state, avoid,
						structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE), epoch);
				if (found != null) out.put(salt, found);
			}
		}
	}

	private static void resolveTail(PlanHolder holder, List<BiomeAwareUniquePlacement> notFound,
									Map<Integer, HolderSet<Biome>> structureBiomes,
									Map<Integer, Integer> structureMinHeights, BiomeSource biomeSource,
									RandomState randomState, ChunkGenerator generator, LevelHeightAccessor heightAccessor,
									int maxRing, double spacingSqr, List<ChunkPos> accepted,
									ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid, int epoch) {
		int absoluteCap = maxRing + TAIL_EXTRA_RINGS;

		for (BiomeAwareUniquePlacement placement : notFound) {
			if (isStale(epoch)) return;
			int salt = placement.placementSalt();
			HolderSet<Biome> structBiomes = structureBiomes.get(salt);
			if (structBiomes == null) continue;
			int minHeight = structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE);

			ChunkPos found = null;
			int from = maxRing + 1;
			while (found == null && from <= absoluteCap) {
				if (isStale(epoch)) return;
				int to = Math.min(from + RING_STEP - 1, absoluteCap);
				found = searchNearest(placement, structBiomes, biomeSource, randomState, generator,
						heightAccessor, from, to, accepted, spacingSqr, state, avoid, minHeight, epoch);
				from = to + 1;
			}

			if (found == null) {
				System.err.println("[DMZ] StructureSpawnPlanner: no valid placement found for salt "
						+ placement.placementSalt() + " within " + absoluteCap + " chunks of spawn.");
				continue;
			}

			accepted.add(found);
			injectResolved(holder, placement.placementSalt(), found);
			StructureAsyncResolver.forceGenerate(generator, found);
		}
	}

	static ChunkPos searchNearest(BiomeAwareUniquePlacement placement, HolderSet<Biome> structureBiomes,
								  BiomeSource biomeSource, RandomState randomState, ChunkGenerator generator,
								  LevelHeightAccessor heightAccessor, int minRing, int maxRing,
								  List<ChunkPos> accepted, double spacingSqr,
								  ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid,
								  int minHeight, int buildEpoch) {
		if (structureBiomes == null) return null;
		ChunkPos flattestFallback = null;
		int flattestSpread = Integer.MAX_VALUE;
		for (int ring = minRing; ring <= maxRing; ring++) {
			if (isStale(buildEpoch)) return flattestFallback;
			for (ChunkPos candidate : ringChunks(ring)) {
				if (tooClose(accepted, candidate.x, candidate.z, spacingSqr)) continue;
				if (!biomePrefilter(placement, biomeSource, randomState, candidate.x, candidate.z)) continue;
				int spread = evaluateCandidate(structureBiomes, biomeSource, randomState, generator,
						heightAccessor, candidate.x, candidate.z, minHeight);
				if (spread < 0) continue;
				if (overlapsOtherStructures(state, avoid, candidate.x, candidate.z)) continue;
				if (spread <= FLATNESS_MAX_SPREAD) return candidate;
				if (spread < flattestSpread) {
					flattestSpread = spread;
					flattestFallback = candidate;
				}
			}
		}
		return flattestFallback;
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
		for (int y = 160; y >= 64; y -= 48) {
			Holder<Biome> biome = biomeSource.getNoiseBiome(quartX, QuartPos.fromBlock(y), quartZ, randomState.sampler());
			if (placement.getValidBiomes().contains(biome)) return true;
		}
		return false;
	}

	static int evaluateCandidate(HolderSet<Biome> structureBiomes,
								 BiomeSource biomeSource, RandomState randomState, ChunkGenerator generator,
								 LevelHeightAccessor heightAccessor, int chunkX, int chunkZ, int minHeight) {
		if (generator == null || heightAccessor == null) {
			return 0;
		}
		int startX = chunkX << 4;
		int startZ = chunkZ << 4;

		int minSurface = Integer.MAX_VALUE;
		int maxSurface = Integer.MIN_VALUE;
		int cornerSurface = 0;
		int centerSurface = 0;
		for (int dx = 0; dx <= 16; dx += 8) {
			for (int dz = 0; dz <= 16; dz += 8) {
				int h = generator.getFirstFreeHeight(startX + dx, startZ + dz, Heightmap.Types.WORLD_SURFACE_WG,
						heightAccessor, randomState);
				if (h < minSurface) minSurface = h;
				if (h > maxSurface) maxSurface = h;
				if (dx == 0 && dz == 0) cornerSurface = h;
				if (dx == 8 && dz == 8) centerSurface = h;
			}
		}

		Holder<Biome> biome = biomeSource.getNoiseBiome(QuartPos.fromBlock(startX), QuartPos.fromBlock(cornerSurface),
				QuartPos.fromBlock(startZ), randomState.sampler());

		if (!structureBiomes.contains(biome)) return -1;

		if (minHeight > Integer.MIN_VALUE && maxSurface < minHeight) return -1;

		if (!biome.is(BiomeTags.IS_OCEAN)) {
			int floor = generator.getFirstFreeHeight(startX + 8, startZ + 8, Heightmap.Types.OCEAN_FLOOR_WG,
					heightAccessor, randomState);
			if (centerSurface > floor) return -1;
		}
		return maxSurface - minSurface;
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

	private static Map<Integer, Integer> buildStructureMinHeights(ChunkGeneratorStructureState state) {
		Map<Integer, Integer> result = new HashMap<>();
		if (state == null) return result;
		for (Holder<StructureSet> holder : state.possibleStructureSets()) {
			StructureSet set = holder.value();
			if (!(set.placement() instanceof BiomeAwareUniquePlacement placement)) continue;
			if (set.structures().isEmpty()) continue;
			Structure structure = set.structures().get(0).structure().value();
			if (structure instanceof TallJigsawStructure tall) {
				result.put(placement.placementSalt(), tall.getMinStartY());
			}
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

	static final class PlanHolder {
		final long seed;
		final BiomeSource biomeSource;
		final RandomState randomState;
		final ChunkGeneratorStructureState state;
		final AtomicBoolean started = new AtomicBoolean(false);
		final CountDownLatch ready = new CountDownLatch(1);
		final Object writeLock = new Object();
		volatile Map<Integer, ChunkPos> positions = null;

		PlanHolder(long seed, BiomeSource biomeSource, RandomState randomState, ChunkGeneratorStructureState state) {
			this.seed = seed;
			this.biomeSource = biomeSource;
			this.randomState = randomState;
			this.state = state;
		}

		void publish(Map<Integer, ChunkPos> plan) {
			synchronized (writeLock) {
				if (positions == null) {
					positions = Collections.unmodifiableMap(new HashMap<>(plan));
				} else {
					Map<Integer, ChunkPos> merged = new HashMap<>(plan);
					merged.putAll(positions);
					positions = Collections.unmodifiableMap(merged);
				}
			}
			ready.countDown();
		}

		boolean awaitReady(long seconds) {
			try {
				return ready.await(seconds, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
	}

	private record PlanKey(long seed, BiomeSource src) {
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof PlanKey other)) return false;
			return seed == other.seed && src == other.src;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(seed) * 31 + System.identityHashCode(src);
		}
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
