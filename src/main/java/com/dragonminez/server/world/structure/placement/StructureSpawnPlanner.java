package com.dragonminez.server.world.structure.placement;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.data.StructurePlanSavedData;
import com.dragonminez.server.world.structure.TallJigsawStructure;
import com.dragonminez.server.world.structure.fitted.FittedTemplateStructure;
import com.dragonminez.server.world.structure.fitted.HeightSampler;
import com.dragonminez.server.world.structure.fitted.SitePlan;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class StructureSpawnPlanner {
	private static final int EXCLUSION_CHUNK_RADIUS = 3;

	private static final int MAX_CACHE_ENTRIES = 250_000;

	private static final int RING_STEP = 64;
	private static final int TAIL_EXTRA_RINGS = 256;
	private static final int TAIL_CHUNK_STRIDE = 3;
	private static final long STARTUP_WAIT_SECONDS = 30L;

	private static final int FLATNESS_MAX_SPREAD = 20;
	private static final int MAX_PROBES_PER_SEARCH = 24;
	private static final int MAX_PROBES_WITHOUT_SITE = 96;
	private static final int PROBE_ACCEPT_SCORE = 30;
	private static final float MIN_BIOME_COVERAGE = 0.6F;
	private static final float MAX_LAND_WATER = 0.35F;
	private static final float MIN_SEA_WATER = 0.6F;
	private static final int FLATNESS_SCAN_EXTRA_RINGS = 5;
	/** Per-call ring window. The tail resumes where the previous window stopped, so this bounds one call, not the search. */
	private static final int ABSOLUTE_SCAN_CAP_RINGS = 96;
	private static final long SEARCH_SAMPLE_BUDGET_MIN = 120_000L;
	private static final long SEARCH_SAMPLE_BUDGET_MAX = 1_500_000L;
	private static long sampleBudget(int maxRing) {
		long window = 8L * Math.max(1, maxRing) * (ABSOLUTE_SCAN_CAP_RINGS + 1L);
		return Math.max(SEARCH_SAMPLE_BUDGET_MIN, Math.min(window + (window >> 2), SEARCH_SAMPLE_BUDGET_MAX));
	}

	private static final TreeMap<Integer, BiomeAwareUniquePlacement> REGISTERED = new TreeMap<>();
	private static final TreeMap<Integer, UniqueNearSpawnPlacement> NEAR_SPAWN_RESERVED = new TreeMap<>();

	private static final ConcurrentHashMap<PlanKey, PlanHolder> PLANS = new ConcurrentHashMap<>();
	private static volatile PlanHolder lastHolder = null;
	private static volatile int planEpoch = 0;
	private static final AtomicInteger POSITIONS_VERSION = new AtomicInteger();

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

	static int currentEpoch() {
		return planEpoch;
	}

	static int positionsVersion() {
		return POSITIONS_VERSION.get();
	}

	public static void onLevelLoad(ServerLevel level) {
		if (level == null) return;
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return;

		var chunkSource = level.getChunkSource();
		ChunkGeneratorStructureState state = chunkSource.getGeneratorState();
		RandomState randomState = chunkSource.randomState();
		if (state == null || randomState == null) return;
		BiomeSource biomeSource = getBiomeSourceReflection(state);
		if (biomeSource == null) return;

		PlanHolder holder = obtainHolder(level.getSeed(), biomeSource, randomState, state);

		StructurePlanSavedData saved = StructurePlanSavedData.get(level);
		Map<Integer, ChunkPos> savedPositions = saved.getPositions();
		if (!savedPositions.isEmpty()) {
			holder.publish(savedPositions);
		}
		holder.exhausted.addAll(saved.getExhausted());

		if (saved.isResolved() && isSettled(holder, expectedSalts(state, biomeSource))) {
			holder.started.set(true);
			holder.primaryDone = true;
			return;
		}

		if (level.dimension().equals(Level.OVERWORLD)) {
			if (holder.started.compareAndSet(false, true)) {
				StructureAsyncResolver.buildPlanSync(holder);
			}
			return;
		}

		ensureBuildStarted(holder);
	}

	private static Set<Integer> expectedSalts(ChunkGeneratorStructureState state, BiomeSource biomeSource) {
		Map<Integer, HolderSet<Biome>> structureBiomes = buildStructureBiomes(state);
		Set<Integer> expected = new HashSet<>();
		synchronized (StructureSpawnPlanner.class) {
			for (BiomeAwareUniquePlacement placement : REGISTERED.values()) {
				int salt = placement.placementSalt();
				if (!structureBiomes.containsKey(salt)) continue;
				if (!biomeSourceHasAny(biomeSource, placement.getValidBiomes())) continue;
				expected.add(salt);
			}
		}
		return expected;
	}

	/**
	 * Currently published plan positions for this level's dimension, without
	 * triggering a plan build. Empty until the plan is available.
	 */
	public static Map<Integer, ChunkPos> publishedPositions(ServerLevel level) {
		PlanHolder holder = findExistingHolder(level);
		if (holder == null) return Collections.emptyMap();
		Map<Integer, ChunkPos> positions = holder.positions;
		return positions == null ? Collections.emptyMap() : positions;
	}

	public static void relocate(ServerLevel level, int salt) {
		PlanHolder holder = findExistingHolder(level);
		if (holder == null) return;
		if (StructurePlanSavedData.get(level).isBuilt(salt)) {
			LogUtil.info(Env.SERVER, "[DMZ] Refusing to relocate salt " + salt
					+ " in " + level.dimension().location() + "; it is already built.");
			return;
		}
		synchronized (holder.writeLock) {
			Map<Integer, ChunkPos> current = holder.positions;
			if (current == null || !current.containsKey(salt)) return;
			Map<Integer, ChunkPos> next = new HashMap<>(current);
			holder.excluded.add(next.remove(salt));
			holder.positions = Collections.unmodifiableMap(next);
			POSITIONS_VERSION.incrementAndGet();
		}
		StructurePlanSavedData.get(level).removePosition(salt);
		LogUtil.info(Env.SERVER, "[DMZ] Relocating structure with salt " + salt
				+ " in " + level.dimension().location() + "; previous site was unusable.");
		StructureAsyncResolver.buildPlan(holder);
	}

	private static PlanHolder findExistingHolder(ServerLevel level) {
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		BiomeSource biomeSource = getBiomeSourceReflection(state);
		if (biomeSource == null) return null;
		PlanHolder cached = lastHolder;
		if (cached != null && cached.seed == level.getSeed() && cached.biomeSource == biomeSource) {
			return cached;
		}
		return PLANS.get(new PlanKey(level.getSeed(), biomeSource));
	}

	public static void precomputeAndWait(MinecraftServer server) {
		if (server == null) return;
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return;


        List<PlanHolder> holders = new ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			try {
				var chunkSource = level.getChunkSource();
				ChunkGeneratorStructureState state = chunkSource.getGeneratorState();
				RandomState randomState = chunkSource.randomState();
				if (state == null || randomState == null) continue;
				BiomeSource biomeSource = getBiomeSourceReflection(state);
				if (biomeSource == null) continue;
				onLevelLoad(level);
				PlanHolder holder = obtainHolder(level.getSeed(), biomeSource, randomState, state);
				if (holder != null && !holders.contains(holder)) holders.add(holder);
			} catch (Throwable t) {
				LogUtil.error(Env.SERVER, "[DMZ] Structure precompute failed for a level: " + t.getMessage());
			}
		}

		// One shared deadline, so adding dimensions never multiplies the startup wait.
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(STARTUP_WAIT_SECONDS);
		for (PlanHolder holder : holders) {
			awaitComplete(holder, deadline);
		}
	}

	/**
	 * Blocks until every applicable structure has a position, or the timeout expires.
	 * Waiting for completeness (not just for the first publish) means maps, {@code /dmzlocate}
	 * and villager trades have real coordinates from the moment players can join. This runs
	 * during startup only — never while players are in the world. On timeout the deferred
	 * search simply keeps going in the background.
	 */
	private static void awaitComplete(PlanHolder holder, long deadlineNanos) {
		Set<Integer> expected;
		try {
			expected = expectedSalts(holder.state, holder.biomeSource);
		} catch (Exception e) {
			return;
		}
		// Dimensions with no unique structures of ours (e.g. the Otherworld) wait for nothing.
		if (expected.isEmpty()) return;

		while (System.nanoTime() < deadlineNanos) {
			if (holder.primaryDone || isSettled(holder, expected)) return;
			try {
				Thread.sleep(50L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}

		Map<Integer, ChunkPos> positions = holder.positions;
		int placed = positions == null ? 0 : positions.size();
		LogUtil.info(Env.SERVER, "[DMZ] Structure plan still incomplete at startup ("
				+ placed + "/" + expected.size() + " placed); the deep search continues in the background.");
	}

	private static boolean isSettled(PlanHolder holder, Set<Integer> expected) {
		Set<Integer> settled = new HashSet<>(holder.exhausted);
		Map<Integer, ChunkPos> positions = holder.positions;
		if (positions != null) settled.addAll(positions.keySet());
		return settled.containsAll(expected);
	}

	static ChunkPos getPositionFor(BiomeAwareUniquePlacement placement, long worldSeed,
	                               BiomeSource biomeSource, RandomState randomState,
	                               ChunkGeneratorStructureState state) {
		if (biomeSource == null || randomState == null) return null;

		PlanHolder holder = obtainHolder(worldSeed, biomeSource, randomState, state);
		ensureBuildStarted(holder);

		Map<Integer, ChunkPos> positions = holder.positions;
		if (positions == null) return null;
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
		if (holder.started.get()) return;
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
			POSITIONS_VERSION.incrementAndGet();
		}
	}

	static List<BiomeAwareUniquePlacement> runBuild(PlanHolder holder, ForkJoinPool searchPool, boolean deferTail) {
		final long buildStartNanos = System.nanoTime();
		final int epoch = planEpoch;
		final long worldSeed = holder.seed;
		final BiomeSource biomeSource = holder.biomeSource;
		final RandomState randomState = holder.randomState;
		final ChunkGeneratorStructureState state = holder.state;

		WorldGenSettings cfg = new WorldGenSettings();

		final ChunkGenerator generator = resolveGeneratorFromServer(state);
		final LevelHeightAccessor heightAccessor = generator != null
				? LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth())
				: null;

		final SampleCache cache = new SampleCache(biomeSource, randomState, generator, heightAccessor);

		double minChunks = cfg.minDistanceFromSpawn / 16.0;
		double maxChunks = Math.max(minChunks + 1.0, cfg.maxDistanceFromSpawn / 16.0);
		double spacingChunks = cfg.minDistanceBetween / 16.0;
		final double spacingSqr = spacingChunks * spacingChunks;

		final int minRing = (int) Math.floor(minChunks);
		final int maxRing = (int) Math.ceil(maxChunks);

		final Map<Integer, HolderSet<Biome>> structureBiomes = buildStructureBiomes(state);
		final Map<Integer, Integer> structureMinHeights = buildStructureMinHeights(state);
		final Map<Integer, String> structureNames = buildStructureNames(state);
		final List<Holder<StructureSet>> avoid = collectAvoidableSets(state);
		final Map<Integer, Probe> strict = buildProbes(state, cache, false);
		final Map<Integer, Probe> relaxed = buildProbes(state, cache, true);

		final List<ChunkPos> reservedBaseline = new ArrayList<>();
		for (UniqueNearSpawnPlacement reserved : NEAR_SPAWN_RESERVED.values()) {
			ChunkPos pos = reserved.getStructureChunk(worldSeed);
			if (pos != null) reservedBaseline.add(pos);
		}

		// Positions already resolved (loaded from the world save or from a previous
		// build) are kept as-is; only missing structures are searched for. Sites
		// that proved unusable (failed relocations) are avoided via spacing.
		final Map<Integer, ChunkPos> existing = holder.positions;
		if (existing != null) reservedBaseline.addAll(existing.values());
		synchronized (holder.excluded) {
			reservedBaseline.addAll(holder.excluded);
		}

		final List<BiomeAwareUniquePlacement> targets = new ArrayList<>();
		for (BiomeAwareUniquePlacement placement : REGISTERED.values()) {
			if (structureBiomes.get(placement.placementSalt()) == null) continue;
			if (!biomeSourceHasAny(biomeSource, placement.getValidBiomes())) continue;
			if (existing != null && existing.containsKey(placement.placementSalt())) continue;
			if (holder.exhausted.contains(placement.placementSalt())) continue;
			targets.add(placement);
		}

		final Map<Integer, ChunkPos> independent = new ConcurrentHashMap<>();
		searchIndependent(targets, structureBiomes, structureMinHeights, cache,
				minRing, maxRing, reservedBaseline, spacingSqr, state, avoid, independent, searchPool, epoch, strict);

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
			ChunkPos reconciled = searchNearest(placement, structureBiomes.get(salt), cache,
					minRingFor(placement, minRing, maxRing), maxRing, accepted, spacingSqr, state, avoid,
					structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE), epoch, 1,
					new AtomicLong(sampleBudget(maxRing)), strict.get(salt));
			if (reconciled != null) {
				plan.put(salt, reconciled);
				accepted.add(reconciled);
			} else if (generator != null && heightAccessor != null && !isStale(epoch)) {
				notFound.add(placement);
			}
		}

		holder.publish(plan);
		holder.primaryDone = true;

		long buildMs = (System.nanoTime() - buildStartNanos) / 1_000_000L;
		if (!targets.isEmpty()) {
			LogUtil.info(Env.SERVER, "[DMZ] Structure plan built in " + buildMs + "ms ("
					+ targets.size() + " targets, " + plan.size() + " placed, " + notFound.size() + " deferred to tail).");
			for (Map.Entry<Integer, ChunkPos> entry : plan.entrySet()) {
				logPlacement(structureNames, entry.getKey(), entry.getValue());
			}
		}

		if (!notFound.isEmpty() && !isStale(epoch)) {
			Runnable tailWork = () -> {
				resolveTail(holder, notFound, structureBiomes, structureMinHeights, structureNames, cache,
						minRing, maxRing, spacingSqr, accepted, state, avoid, epoch, strict, relaxed);
				persistPlan(holder, epoch);
			};

			if (deferTail) {
				persistPlan(holder, epoch);
				StructureAsyncResolver.submitTail(tailWork);
			} else {
				tailWork.run();
			}
			return notFound;
		}

		persistPlan(holder, epoch);
		return notFound;
	}
	private static void persistPlan(PlanHolder holder, int epoch) {
		if (isStale(epoch)) return;
		Map<Integer, ChunkPos> positions = holder.positions;
		if (positions == null) return;

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;
		ServerLevel level = null;
		for (ServerLevel candidate : server.getAllLevels()) {
			if (candidate.getChunkSource().getGeneratorState() == holder.state) {
				level = candidate;
				break;
			}
		}
		if (level == null) return;

		boolean complete = isSettled(holder, expectedSalts(holder.state, holder.biomeSource));
		final ServerLevel targetLevel = level;
		final Map<Integer, ChunkPos> snapshot = positions;
		final Set<Integer> exhausted = new HashSet<>(holder.exhausted);
		server.execute(() -> StructurePlanSavedData.get(targetLevel).setPositions(snapshot, exhausted, complete));
	}

	private static void searchIndependent(List<BiomeAwareUniquePlacement> targets,
	                                      Map<Integer, HolderSet<Biome>> structureBiomes,
	                                      Map<Integer, Integer> structureMinHeights, SampleCache cache,
	                                      int minRing, int maxRing,
	                                      List<ChunkPos> reservedBaseline, double spacingSqr,
	                                      ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid,
	                                      Map<Integer, ChunkPos> out, ForkJoinPool searchPool, int epoch,
	                                      Map<Integer, Probe> probes) {
		Runnable work = () -> targets.parallelStream().forEach(placement -> {
			if (isStale(epoch)) return;
			int salt = placement.placementSalt();
			ChunkPos found = searchNearest(placement, structureBiomes.get(salt), cache,
					minRingFor(placement, minRing, maxRing), maxRing, reservedBaseline, spacingSqr, state, avoid,
					structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE), epoch, 1,
					new AtomicLong(sampleBudget(maxRing)), probes.get(salt));
			if (found != null) out.put(salt, found);
		});

		try {
			searchPool.submit(work).get();
		} catch (Exception e) {
			out.clear();
			for (BiomeAwareUniquePlacement placement : targets) {
				if (isStale(epoch)) return;
				int salt = placement.placementSalt();
				ChunkPos found = searchNearest(placement, structureBiomes.get(salt), cache,
						minRingFor(placement, minRing, maxRing), maxRing, reservedBaseline, spacingSqr, state, avoid,
						structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE), epoch, 1,
						new AtomicLong(sampleBudget(maxRing)), probes.get(salt));
				if (found != null) out.put(salt, found);
			}
		}
	}

	private static void resolveTail(PlanHolder holder, List<BiomeAwareUniquePlacement> notFound,
	                                Map<Integer, HolderSet<Biome>> structureBiomes,
	                                Map<Integer, Integer> structureMinHeights, Map<Integer, String> structureNames,
	                                SampleCache cache,
	                                int minRing, int maxRing, double spacingSqr, List<ChunkPos> accepted,
	                                ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid, int epoch,
	                                Map<Integer, Probe> strict, Map<Integer, Probe> relaxed) {
		int absoluteCap = maxRing + TAIL_EXTRA_RINGS;

		for (BiomeAwareUniquePlacement placement : notFound) {
			if (isStale(epoch)) return;
			int salt = placement.placementSalt();
			HolderSet<Biome> structBiomes = structureBiomes.get(salt);
			if (structBiomes == null) continue;
			int minHeight = structureMinHeights.getOrDefault(salt, Integer.MIN_VALUE);

			AtomicLong budget = new AtomicLong(sampleBudget(maxRing));
			ChunkPos found = null;
			int from = Math.min(maxRing, minRingFor(placement, minRing, maxRing) + ABSOLUTE_SCAN_CAP_RINGS) + 1;
			while (found == null && from <= absoluteCap && budget.get() > 0) {
				if (isStale(epoch)) return;
				int to = Math.min(from + RING_STEP - 1, absoluteCap);
				int stride = from <= maxRing ? 1 : TAIL_CHUNK_STRIDE;
				found = searchNearest(placement, structBiomes, cache, from, to, accepted, spacingSqr,
						state, avoid, minHeight, epoch, stride, budget, strict.get(salt));
				from = to + 1;
			}

			String name = structureNames.getOrDefault(salt, "salt:" + salt);
			Probe fallback = relaxed.get(salt);
			if (found == null && fallback != null && !isStale(epoch)) {
				found = searchNearest(placement, structBiomes, cache, minRingFor(placement, minRing, maxRing), maxRing,
						accepted, spacingSqr, state, avoid, minHeight, epoch, 1, new AtomicLong(sampleBudget(maxRing)), fallback);
				if (found != null) {
					LogUtil.info(Env.SERVER, "[DMZ] No site in its own biomes for " + name
							+ " within " + absoluteCap + " chunks of spawn; using any biome it can generate in.");
				}
			}

			if (found == null) {
				holder.exhausted.add(salt);
				LogUtil.warn(Env.SERVER, "[DMZ] No valid placement found for " + name + " within "
						+ absoluteCap + " chunks of spawn; it will not be searched again in this world.");
				continue;
			}

			accepted.add(found);
			injectResolved(holder, placement.placementSalt(), found);
			logPlacement(structureNames, salt, found);
		}
	}

	private static void logPlacement(Map<Integer, String> structureNames, int salt, ChunkPos pos) {
		String name = structureNames.getOrDefault(salt, "salt:" + salt);
		LogUtil.info(Env.SERVER, "[DMZ]   placed " + name
				+ " at x=" + ((pos.x << 4) + 8) + ", z=" + ((pos.z << 4) + 8)
				+ " (chunk " + pos.x + ", " + pos.z + ")");
	}

	@FunctionalInterface
	interface SiteEvaluator {
		int score(ChunkPos candidate);
	}

	record Probe(HolderSet<Biome> biomes, SiteEvaluator evaluator) {}

	private static Map<Integer, Probe> buildProbes(ChunkGeneratorStructureState state, SampleCache cache, boolean relaxed) {
		Map<Integer, Probe> result = new HashMap<>();
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (state == null || server == null || cache.generator == null || cache.heightAccessor == null) return result;
		StructureTemplateManager templates = server.getStructureManager();
		for (Holder<StructureSet> holder : state.possibleStructureSets()) {
			StructureSet set = holder.value();
			if (!(set.placement() instanceof BiomeAwareUniquePlacement placement)) continue;
			if (set.structures().isEmpty()) continue;
			Structure structure = set.structures().get(0).structure().value();
			if (structure instanceof FittedTemplateStructure fitted) {
				HolderSet<Biome> validBiomes = placement.getValidBiomes();
				boolean sea = validBiomes.size() > 0 && validBiomes.stream().allMatch(biome -> biome.is(BiomeTags.IS_OCEAN));
				HolderSet<Biome> wanted = relaxed ? fitted.biomes() : validBiomes;
				result.put(placement.placementSalt(), new Probe(wanted, fittedEvaluator(fitted, wanted, sea, cache, templates)));
			} else if (!relaxed) {
				result.put(placement.placementSalt(), new Probe(placement.getValidBiomes(), null));
			}
		}
		return result;
	}

	private static SiteEvaluator fittedEvaluator(FittedTemplateStructure structure, HolderSet<Biome> validBiomes, boolean sea,
	                                             SampleCache cache, StructureTemplateManager templates) {
		HolderSet<Biome> structureBiomes = structure.biomes();
		HeightSampler sampler = cache.heights();
		return candidate -> {
			Optional<SitePlan> plan;
			try {
				plan = structure.estimate(templates, sampler, candidate);
			} catch (RuntimeException e) {
				return -1;
			}
			if (plan.isEmpty()) return -1;
			SitePlan site = plan.get();
			BlockPos center = site.center();
			if (!structureBiomes.contains(cache.biomeAt(center.getX(), center.getY(), center.getZ()))) return -1;
			int[] xs = site.sampleX();
			int[] zs = site.sampleZ();
			int[] tops = site.sampleTop();
			int inside = 0;
			for (int i = 0; i < xs.length; i++) {
				if (validBiomes.contains(cache.biomeAt(xs[i], tops[i] + 1, zs[i]))) inside++;
			}
			float coverage = inside / (float) Math.max(1, xs.length);
			if (coverage < MIN_BIOME_COVERAGE) return -1;
			if (sea ? site.water() < MIN_SEA_WATER : site.water() > MAX_LAND_WATER) return -1;
			double cost = site.deviation() * (sea ? 1.5D : 6.0D);
			return (int) Math.round(cost + (1.0F - coverage) * 40.0F);
		};
	}

	private static Map<Integer, String> buildStructureNames(ChunkGeneratorStructureState state) {
		Map<Integer, String> result = new HashMap<>();
		if (state == null) return result;
		for (Holder<StructureSet> holder : state.possibleStructureSets()) {
			StructureSet set = holder.value();
			if (!(set.placement() instanceof BiomeAwareUniquePlacement placement)) continue;
			if (set.structures().isEmpty()) continue;
			String name = set.structures().get(0).structure().unwrapKey()
					.map(key -> key.location().toString()).orElse("salt:" + placement.placementSalt());
			result.put(placement.placementSalt(), name);
		}
		return result;
	}

	static ChunkPos searchNearest(BiomeAwareUniquePlacement placement, HolderSet<Biome> structureBiomes,
	                              SampleCache cache, int minRing, int maxRing,
	                              List<ChunkPos> accepted, double spacingSqr,
	                              ChunkGeneratorStructureState state, List<Holder<StructureSet>> avoid,
	                              int minHeight, int buildEpoch, int chunkStride, AtomicLong budget,
	                              Probe probe) {
		if (structureBiomes == null) return null;
		HolderSet<Biome> wanted = probe != null ? probe.biomes() : placement.getValidBiomes();
		SiteEvaluator evaluator = probe != null ? probe.evaluator() : null;
		int probes = 0;
		ChunkPos bestNonOverlap = null;
		int bestNonOverlapSpread = Integer.MAX_VALUE;
		ChunkPos overlapFallback = null;
		int overlapFallbackSpread = Integer.MAX_VALUE;
		int firstValidRing = -1;
		for (int ring = minRing; ring <= maxRing; ring++) {
			if (isStale(buildEpoch)) break;
			if (ring - minRing > ABSOLUTE_SCAN_CAP_RINGS) break;
			if (firstValidRing >= 0 && ring - firstValidRing > FLATNESS_SCAN_EXTRA_RINGS) break;
			if (budget != null && budget.get() <= 0) break;
			List<ChunkPos> candidates = ringChunks(ring, cache.center);
			for (int i = 0; i < candidates.size(); i++) {
				if (chunkStride > 1 && (i % chunkStride) != 0) continue;
				ChunkPos candidate = candidates.get(i);
				if (tooClose(accepted, candidate.x, candidate.z, spacingSqr)) continue;
				if (budget != null && budget.decrementAndGet() < 0) {
					return bestNonOverlap != null ? bestNonOverlap : overlapFallback;
				}
				if (!biomePrefilter(wanted, cache, candidate.x, candidate.z)) continue;
				int spread = evaluateCandidate(structureBiomes, cache, candidate.x, candidate.z, minHeight);
				if (spread < 0) continue;
				if (evaluator != null) {
					boolean hasSite = bestNonOverlap != null || overlapFallback != null;
					if (probes++ >= (hasSite ? MAX_PROBES_PER_SEARCH : MAX_PROBES_WITHOUT_SITE)) {
						return bestNonOverlap != null ? bestNonOverlap : overlapFallback;
					}
					int score = evaluator.score(candidate);
					if (score < 0) continue;
					spread = score <= PROBE_ACCEPT_SCORE ? Math.min(score, FLATNESS_MAX_SPREAD) : score;
				}
				if (firstValidRing < 0) firstValidRing = ring;
				if (overlapsOtherStructures(state, avoid, candidate.x, candidate.z)) {
					if (spread < overlapFallbackSpread) {
						overlapFallbackSpread = spread;
						overlapFallback = candidate;
					}
					continue;
				}
				if (spread <= FLATNESS_MAX_SPREAD) return candidate;
				if (spread < bestNonOverlapSpread) {
					bestNonOverlapSpread = spread;
					bestNonOverlap = candidate;
				}
			}
		}
		if (bestNonOverlap != null) return bestNonOverlap;
		return overlapFallback;
	}

	static int minRingFor(BiomeAwareUniquePlacement placement, int minRing, int maxRing) {
		int own = (int) Math.ceil(placement.getMinDistanceFromSpawn() / 16.0D);
		return Math.min(maxRing, Math.max(minRing, own));
	}

	static List<ChunkPos> ringChunks(int ring, ChunkPos center) {
		List<ChunkPos> out = new ArrayList<>();
		if (ring <= 0) {
			out.add(center);
			return out;
		}
		for (int dx = -ring; dx <= ring; dx++) {
			out.add(new ChunkPos(center.x + dx, center.z - ring));
			out.add(new ChunkPos(center.x + dx, center.z + ring));
		}
		for (int dz = -ring + 1; dz <= ring - 1; dz++) {
			out.add(new ChunkPos(center.x - ring, center.z + dz));
			out.add(new ChunkPos(center.x + ring, center.z + dz));
		}
		out.sort((a, b) -> Long.compare(distSqr(a, center), distSqr(b, center)));
		return out;
	}

	private static long distSqr(ChunkPos pos, ChunkPos center) {
		long dx = (long) pos.x - center.x;
		long dz = (long) pos.z - center.z;
		return dx * dx + dz * dz;
	}

	private static ChunkPos spawnCenter(RandomState randomState) {
		try {
			Climate.Sampler sampler = randomState.sampler();
			if (sampler.spawnTarget().isEmpty()) return new ChunkPos(0, 0);
			return new ChunkPos(sampler.findSpawnPosition());
		} catch (RuntimeException e) {
			return new ChunkPos(0, 0);
		}
	}

	private static boolean biomeSourceHasAny(BiomeSource biomeSource, HolderSet<Biome> validBiomes) {
		if (biomeSource == null || validBiomes == null) return false;
		for (Holder<Biome> biome : biomeSource.possibleBiomes()) {
			if (validBiomes.contains(biome)) return true;
		}
		return false;
	}

	private static boolean biomePrefilter(HolderSet<Biome> wanted, SampleCache cache, int chunkX, int chunkZ) {
		for (Holder<Biome> biome : cache.columnBiomes(chunkX, chunkZ)) {
			if (wanted.contains(biome)) return true;
		}
		return false;
	}

	static int evaluateCandidate(HolderSet<Biome> structureBiomes, SampleCache cache,
	                             int chunkX, int chunkZ, int minHeight) {
		ChunkTerrain terrain = cache.terrain(chunkX, chunkZ);
		if (terrain == null) {
			return 0;
		}

		if (!structureBiomes.contains(terrain.cornerBiome())) return -1;

		if (minHeight > Integer.MIN_VALUE) {
			if (terrain.maxSurface() < minHeight) return -1;
			// TallJigsawStructure samples a 4x4 interior grid; the chunk-corner
			// heights above can overestimate, so verify with the exact same grid.
			if (cache.interiorMaxSurface(chunkX, chunkZ) < minHeight) return -1;
		}

		if (!terrain.cornerBiome().is(BiomeTags.IS_OCEAN)) {
			if (terrain.cornerSurface() > terrain.floor()) return -1;
		}
		return terrain.maxSurface() - terrain.minSurface();
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

	private static ChunkGenerator resolveGeneratorFromServer(ChunkGeneratorStructureState state) {
		if (state == null) return null;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return null;
		for (ServerLevel level : server.getAllLevels()) {
			var chunkSource = level.getChunkSource();
			if (chunkSource.getGeneratorState() == state) {
				return chunkSource.getGenerator();
			}
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

	private static final class SampleCache {
		final BiomeSource biomeSource;
		final RandomState randomState;
		final ChunkGenerator generator;
		final LevelHeightAccessor heightAccessor;
		final ChunkPos center;
		private final ConcurrentHashMap<Long, List<Holder<Biome>>> columnBiomeCache = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<Long, ChunkTerrain> terrainCache = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<Long, Integer> interiorMaxCache = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<Long, Long> footprintCache = new ConcurrentHashMap<>();

		SampleCache(BiomeSource biomeSource, RandomState randomState, ChunkGenerator generator,
		            LevelHeightAccessor heightAccessor) {
			this.biomeSource = biomeSource;
			this.randomState = randomState;
			this.generator = generator;
			this.heightAccessor = heightAccessor;
			this.center = spawnCenter(randomState);
		}

		private void trim() {
			if (columnBiomeCache.size() > MAX_CACHE_ENTRIES) columnBiomeCache.clear();
			if (terrainCache.size() > MAX_CACHE_ENTRIES) terrainCache.clear();
			if (interiorMaxCache.size() > MAX_CACHE_ENTRIES) interiorMaxCache.clear();
			if (footprintCache.size() > MAX_CACHE_ENTRIES) footprintCache.clear();
		}

		HeightSampler heights() {
			HeightSampler shared = HeightSampler.estimating(generator, heightAccessor, randomState, footprintCache);
			return (x, z) -> {
				trim();
				return shared.sample(x, z);
			};
		}

		Holder<Biome> biomeAt(int x, int y, int z) {
			return biomeSource.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), randomState.sampler());
		}

		List<Holder<Biome>> columnBiomes(int chunkX, int chunkZ) {
			trim();
			return columnBiomeCache.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), key -> {
				int quartX = QuartPos.fromBlock((chunkX << 4) + 8);
				int quartZ = QuartPos.fromBlock((chunkZ << 4) + 8);
				List<Holder<Biome>> column = new ArrayList<>(3);
				for (int y = 160; y >= 64; y -= 48) {
					column.add(biomeSource.getNoiseBiome(quartX, QuartPos.fromBlock(y), quartZ, randomState.sampler()));
				}
				return column;
			});
		}

		int interiorMaxSurface(int chunkX, int chunkZ) {
			return interiorMaxCache.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), key -> {
				int startX = chunkX << 4;
				int startZ = chunkZ << 4;
				int best = Integer.MIN_VALUE;
				for (int dx = 0; dx < 16; dx += 4) {
					for (int dz = 0; dz < 16; dz += 4) {
						int h = generator.getFirstFreeHeight(startX + dx, startZ + dz,
								Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
						if (h > best) best = h;
					}
				}
				return best;
			});
		}

		ChunkTerrain terrain(int chunkX, int chunkZ) {
			if (generator == null || heightAccessor == null) return null;
			return terrainCache.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), key -> {
				int startX = chunkX << 4;
				int startZ = chunkZ << 4;

				int minSurface = Integer.MAX_VALUE;
				int maxSurface = Integer.MIN_VALUE;
				for (int dx = 0; dx <= 16; dx += 16) {
					for (int dz = 0; dz <= 16; dz += 16) {
						int h = generator.getFirstFreeHeight(startX + dx, startZ + dz, Heightmap.Types.WORLD_SURFACE_WG,
								heightAccessor, randomState);
						if (h < minSurface) minSurface = h;
						if (h > maxSurface) maxSurface = h;
					}
				}

				// Middle column: jigsaw starts generate at the chunk middle and
				// vanilla's final biome check samples there, not at the corner.
				int midX = startX + 8;
				int midZ = startZ + 8;
				int midSurface = generator.getFirstFreeHeight(midX, midZ, Heightmap.Types.WORLD_SURFACE_WG,
						heightAccessor, randomState);
				Holder<Biome> biome = biomeSource.getNoiseBiome(QuartPos.fromBlock(midX),
						QuartPos.fromBlock(midSurface), QuartPos.fromBlock(midZ), randomState.sampler());
				int floor = generator.getFirstFreeHeight(midX, midZ, Heightmap.Types.OCEAN_FLOOR_WG,
						heightAccessor, randomState);
				return new ChunkTerrain(biome, minSurface, maxSurface, midSurface, floor);
			});
		}
	}

	private record ChunkTerrain(Holder<Biome> cornerBiome, int minSurface, int maxSurface,
	                            int cornerSurface, int floor) {}

	static final class PlanHolder {
		final long seed;
		final BiomeSource biomeSource;
		final RandomState randomState;
		final ChunkGeneratorStructureState state;
		final AtomicBoolean started = new AtomicBoolean(false);
		final CountDownLatch ready = new CountDownLatch(1);
		final Object writeLock = new Object();
		final List<ChunkPos> excluded = Collections.synchronizedList(new ArrayList<>());
		final Set<Integer> exhausted = ConcurrentHashMap.newKeySet();
		volatile Map<Integer, ChunkPos> positions = null;
		volatile boolean primaryDone = false;

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
				POSITIONS_VERSION.incrementAndGet();
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
