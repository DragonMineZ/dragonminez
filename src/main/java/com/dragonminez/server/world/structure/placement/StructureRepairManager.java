package com.dragonminez.server.world.structure.placement;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.data.StructurePlanSavedData;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class StructureRepairManager {
	private static final int CHECK_INTERVAL_TICKS = 100;
	private static final int MAX_RELOCATIONS = 2;
	private static final int COMPLETION_RANGE = 14;
	private static final int SLICE_HEIGHT = 48;
	private static final long SLICE_BUDGET_NANOS = 4_000_000L;
	private static final int MAX_CHECKS_PER_TICK = 64;
	private static final long INHABITED_LIMIT = 72_000L;

	private static final Set<String> HANDLED = ConcurrentHashMap.newKeySet();
	private static final Map<String, Integer> RELOCATIONS = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, Materializer> MATERIALIZERS = new ConcurrentHashMap<>();

	private StructureRepairManager() {}

	public static void reset() {
		HANDLED.clear();
		RELOCATIONS.clear();
		MATERIALIZERS.clear();
	}

	public static void onLevelUnload(ServerLevel level) {
		String prefix = level.dimension().location() + "#";
		MATERIALIZERS.remove(level.dimension());
		HANDLED.removeIf(key -> key.startsWith(prefix));
		RELOCATIONS.keySet().removeIf(key -> key.startsWith(prefix));
	}

	public static void tick(ServerLevel level) {
		Materializer materializer = MATERIALIZERS.get(level.dimension());
		if (materializer != null && materializer.isFor(level)) materializer.tick(level);

		if ((level.getGameTime() % CHECK_INTERVAL_TICKS) != 0) return;
		if (level.players().isEmpty()) return;
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return;

		Map<Integer, ChunkPos> nearSpawn = nearSpawnPositions(level);
		Map<Integer, ChunkPos> positions = new HashMap<>(StructureSpawnPlanner.publishedPositions(level));
		positions.putAll(nearSpawn);
		if (positions.isEmpty()) return;

		StructurePlanSavedData plan = StructurePlanSavedData.get(level);

		Map<Integer, Holder<Structure>> structuresBySalt = null;
		for (Map.Entry<Integer, ChunkPos> entry : positions.entrySet()) {
			int salt = entry.getKey();
			ChunkPos pos = entry.getValue();
			String saltKey = level.dimension().location() + "#" + salt;
			String key = saltKey + "@" + pos.toLong();
			if (HANDLED.contains(key)) continue;
			if (plan.isBuilt(salt)) {
				HANDLED.add(key);
				continue;
			}

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
				plan.markBuilt(salt);
				if (structure.value() instanceof IncrementalStructure) {
					materializer(level).remember(salt, start);
					enqueueLoaded(level, salt, start.getBoundingBox());
				}
				continue;
			}

			String name = structure.unwrapKey().map(k -> k.location().toString()).orElse("salt:" + salt);
			if (forcePlace(level, structure.value(), pos, salt)) {
				HANDLED.add(key);
				plan.markBuilt(salt);
				LogUtil.info(Env.SERVER, "[DMZ] Materialized missing structure " + name
						+ " at chunk " + pos.x + ", " + pos.z + " in " + level.dimension().location());
			} else if (nearSpawn.containsKey(salt)) {
				HANDLED.add(key);
				LogUtil.error(Env.SERVER, "[DMZ] Could not materialize " + name
						+ " at chunk " + pos.x + ", " + pos.z + " in " + level.dimension().location() + ".");
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

	public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
		if (!ConfigManager.getServerConfig().getWorldGen().getGenerateCustomStructures()) return;
		Map<Integer, ChunkPos> positions = StructureSpawnPlanner.publishedPositions(level);
		if (positions.isEmpty()) return;
		ChunkPos pos = chunk.getPos();
		Materializer materializer = null;
		for (Map.Entry<Integer, ChunkPos> entry : positions.entrySet()) {
			ChunkPos origin = entry.getValue();
			if (Math.abs(origin.x - pos.x) > COMPLETION_RANGE || Math.abs(origin.z - pos.z) > COMPLETION_RANGE) continue;
			if (materializer == null) materializer = materializer(level);
			if (!materializer.isIncremental(level, entry.getKey())) continue;
			materializer.enqueueCheck(pos, entry.getKey());
		}
	}

	public static int restore(ServerLevel level, StructureStart start) {
		BoundingBox box = start.getBoundingBox();
		ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
		ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
		Materializer materializer = materializer(level);
		int[] unloaded = {0};
		ChunkPos.rangeClosed(min, max).forEach(p -> {
			if (level.getChunkSource().getChunkNow(p.x, p.z) == null) unloaded[0]++;
			else materializer.forceSchedule(p, start);
		});
		return unloaded[0];
	}

	private static Materializer materializer(ServerLevel level) {
		return MATERIALIZERS.compute(level.dimension(),
				(key, current) -> current != null && current.isFor(level) ? current : new Materializer(level));
	}

	private static void enqueueLoaded(ServerLevel level, int salt, BoundingBox box) {
		ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
		ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
		Materializer materializer = materializer(level);
		ChunkPos.rangeClosed(min, max).forEach(p -> {
			if (level.getChunkSource().getChunkNow(p.x, p.z) != null) materializer.enqueueCheck(p, salt);
		});
	}

	private static Map<Integer, Holder<Structure>> structuresBySalt(ServerLevel level) {
		Map<Integer, Holder<Structure>> result = new HashMap<>();
		for (Holder<StructureSet> holder : level.getChunkSource().getGeneratorState().possibleStructureSets()) {
			StructureSet set = holder.value();
			if (set.structures().isEmpty()) continue;
			if (set.placement() instanceof BiomeAwareUniquePlacement placement) {
				result.put(placement.placementSalt(), set.structures().get(0).structure());
			} else if (set.placement() instanceof UniqueNearSpawnPlacement placement) {
				result.put(placement.placementSalt(), set.structures().get(0).structure());
			}
		}
		return result;
	}

	private static Map<Integer, ChunkPos> nearSpawnPositions(ServerLevel level) {
		Map<Integer, ChunkPos> result = new HashMap<>();
		for (Holder<StructureSet> holder : level.getChunkSource().getGeneratorState().possibleStructureSets()) {
			StructureSet set = holder.value();
			if (set.structures().isEmpty()) continue;
			if (set.placement() instanceof UniqueNearSpawnPlacement placement) {
				result.put(placement.placementSalt(), placement.getStructureChunk(level.getSeed()));
			}
		}
		return result;
	}

	private static boolean forcePlace(ServerLevel level, Structure structure, ChunkPos chunkPos, int salt) {
		try {
			ChunkGenerator generator = level.getChunkSource().getGenerator();
			StructureStart start = structure.generate(level.registryAccess(), generator,
					generator.getBiomeSource(), level.getChunkSource().randomState(),
					level.getStructureManager(), level.getSeed(), chunkPos, 0, level, biome -> true);
			if (!start.isValid()) return false;

			BoundingBox box = start.getBoundingBox();
			if (structure instanceof IncrementalStructure) {
				level.getChunk(chunkPos.x, chunkPos.z).setStartForStructure(structure, start);
				materializer(level).remember(salt, start);
				enqueueLoaded(level, salt, box);
				return true;
			}

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

	private static final class ChunkWork {
		final ChunkPos pos;
		final Structure structure;
		final StructureStart start;
		int remaining;

		ChunkWork(ChunkPos pos, Structure structure, StructureStart start) {
			this.pos = pos;
			this.structure = structure;
			this.start = start;
		}
	}

	private record Slice(ChunkWork work, int minY, int maxY, int order, int distance) {}

	private static final class Materializer {
		private final WeakReference<ServerLevel> owner;
		private final ConcurrentLinkedQueue<long[]> checks = new ConcurrentLinkedQueue<>();
		private final PriorityQueue<Slice> slices = new PriorityQueue<>(
				Comparator.comparingInt(Slice::order).thenComparingInt(Slice::distance));
		private final Map<Long, ChunkWork> active = new HashMap<>();
		private final Map<Integer, StructureStart> starts = new HashMap<>();
		private volatile Map<Integer, Holder<Structure>> structures;

		Materializer(ServerLevel level) {
			this.owner = new WeakReference<>(level);
		}

		boolean isFor(ServerLevel level) {
			return this.owner.get() == level;
		}

		void enqueueCheck(ChunkPos pos, int salt) {
			this.checks.add(new long[]{pos.toLong(), salt});
		}

		Map<Integer, Holder<Structure>> structures(ServerLevel level) {
			Map<Integer, Holder<Structure>> current = this.structures;
			if (current == null) {
				current = structuresBySalt(level);
				this.structures = current;
			}
			return current;
		}

		boolean isIncremental(ServerLevel level, int salt) {
			Holder<Structure> holder = structures(level).get(salt);
			return holder != null && holder.value() instanceof IncrementalStructure;
		}

		void remember(int salt, StructureStart start) {
			this.starts.put(salt, start);
		}

		void tick(ServerLevel level) {
			try {
				processChecks(level);
				processSlices(level);
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "[DMZ] Structure materialization failed: " + e.getMessage());
			}
		}

		private void processChecks(ServerLevel level) {
			long[] check;
			int processed = 0;
			while (processed++ < MAX_CHECKS_PER_TICK && (check = this.checks.poll()) != null) {
				ChunkPos pos = new ChunkPos(check[0]);
				int salt = (int) check[1];
				if (this.active.containsKey(pos.toLong())) continue;
				LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
				if (chunk == null) continue;
				Holder<Structure> holder = structures(level).get(salt);
				if (holder == null || !(holder.value() instanceof IncrementalStructure)) continue;
				StructureStart start = start(level, salt, holder.value());
				if (start == null) continue;
				Structure structure = start.getStructure();
				if (level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure) == null) continue;
				ChunkPos origin = start.getChunkPos();
				if (chunk.getReferencesForStructure(structure).contains(origin.toLong())) continue;
				BoundingBox box = start.getBoundingBox();
				if (!box.intersects(pos.getMinBlockX(), pos.getMinBlockZ(), pos.getMaxBlockX(), pos.getMaxBlockZ())) continue;
				if (chunk.getInhabitedTime() > INHABITED_LIMIT) continue;
				schedule(pos, structure, start, box, origin);
			}
		}

		private StructureStart start(ServerLevel level, int salt, Structure structure) {
			StructureStart cached = this.starts.get(salt);
			if (cached != null) return cached;
			ChunkPos origin = StructureSpawnPlanner.publishedPositions(level).get(salt);
			if (origin == null || !StructurePlanSavedData.get(level).isBuilt(salt)) return null;
			LevelChunk loaded = level.getChunkSource().getChunkNow(origin.x, origin.z);
			ChunkAccess chunk = loaded != null ? loaded : level.getChunk(origin.x, origin.z);
			StructureStart start = chunk.getStartForStructure(structure);
			if (start == null || !start.isValid()) return null;
			this.starts.put(salt, start);
			return start;
		}

		void forceSchedule(ChunkPos pos, StructureStart start) {
			this.active.remove(pos.toLong());
			schedule(pos, start.getStructure(), start, start.getBoundingBox(), start.getChunkPos());
		}

		private void schedule(ChunkPos pos, Structure structure, StructureStart start, BoundingBox box, ChunkPos origin) {
			ChunkWork work = new ChunkWork(pos, structure, start);
			int distance = Math.max(Math.abs(pos.x - origin.x), Math.abs(pos.z - origin.z));
			int order = 0;
			for (int y = box.minY(); y <= box.maxY(); y += SLICE_HEIGHT, order++) {
				this.slices.add(new Slice(work, y, Math.min(box.maxY(), y + SLICE_HEIGHT - 1), order, distance));
				work.remaining++;
			}
			this.active.put(pos.toLong(), work);
		}

		private void processSlices(ServerLevel level) {
			if (this.slices.isEmpty()) return;
			ChunkGenerator generator = level.getChunkSource().getGenerator();
			long deadline = System.nanoTime() + SLICE_BUDGET_NANOS;
			while (!this.slices.isEmpty() && System.nanoTime() < deadline) {
				Slice slice = this.slices.poll();
				ChunkWork work = slice.work();
				if (this.active.get(work.pos.toLong()) != work) continue;
				LevelChunk chunk = level.getChunkSource().getChunkNow(work.pos.x, work.pos.z);
				if (chunk == null) {
					this.active.remove(work.pos.toLong());
					continue;
				}
				BoundingBox sliceBox = new BoundingBox(work.pos.getMinBlockX(), slice.minY(), work.pos.getMinBlockZ(),
						work.pos.getMaxBlockX(), slice.maxY(), work.pos.getMaxBlockZ());
				work.start.placeInChunk(level, level.structureManager(), generator, level.getRandom(), sliceBox, work.pos);
				if (--work.remaining <= 0) {
					chunk.addReferenceForStructure(work.structure, work.start.getChunkPos().toLong());
					this.active.remove(work.pos.toLong());
				}
			}
		}
	}
}
