package com.dragonminez.server.world.structure.placement;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.server.world.data.StructurePlanSavedData;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class StructureRepairManager {
	private static final int CHECK_INTERVAL_TICKS = 100;
	private static final int MAX_RELOCATIONS = 2;
	public static final int COMPLETION_RANGE = 14;
	private static final int SLICE_HEIGHT = 48;
	private static final long SLICE_BUDGET_NANOS = 1_500_000L;
	private static final int MAX_CHECKS_PER_TICK = 64;
	private static final long INHABITED_LIMIT = 72_000L;
	private static final double FAST_PLAYER_SPEED_SQR = 0.6D * 0.6D;
	private static final int MAX_DEFERRED_TICKS = 40;

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

		resumeForced(level);
		if (materializer != null && materializer.isFor(level)) {
			materializer.sweepForced(level);
			materializer.sweepIncremental(level);
		}

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
			if (forcePlace(level, chunk, structure.value(), pos, salt)) {
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
		ChunkPos pos = chunk.getPos();
		Materializer existing = MATERIALIZERS.get(level.dimension());
		if (existing != null && existing.isFor(level)) existing.onChunkLoaded(pos);
		Map<Integer, ChunkPos> positions = StructureSpawnPlanner.publishedPositions(level);
		if (positions.isEmpty()) return;
		Materializer materializer = null;
		for (Map.Entry<Integer, ChunkPos> entry : positions.entrySet()) {
			ChunkPos origin = entry.getValue();
			if (Math.abs(origin.x - pos.x) > COMPLETION_RANGE || Math.abs(origin.z - pos.z) > COMPLETION_RANGE) continue;
			if (materializer == null) materializer = materializer(level);
			if (!materializer.isIncremental(level, entry.getKey())) continue;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) materializer.enqueueCheck(new ChunkPos(pos.x + dx, pos.z + dz), entry.getKey());
			}
		}
	}

	public static int restore(ServerLevel level, StructureStart start) {
		BoundingBox box = start.getBoundingBox();
		Materializer materializer = materializer(level);
		int[] unloaded = {0};
		ChunkPos.rangeClosed(minChunk(box), maxChunk(box)).forEach(p -> {
			if (level.getChunkSource().getChunkNow(p.x, p.z) == null) unloaded[0]++;
			else materializer.forceSchedule(p, start);
		});
		return unloaded[0];
	}

	private static Materializer materializer(ServerLevel level) {
		return MATERIALIZERS.compute(level.dimension(),
				(key, current) -> current != null && current.isFor(level) ? current : new Materializer(level));
	}

	private static ChunkPos minChunk(BoundingBox box) {
		return new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
	}

	private static ChunkPos maxChunk(BoundingBox box) {
		return new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
	}

	private static boolean neighboursLoaded(ServerLevel level, ChunkPos pos) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (level.getChunkSource().getChunkNow(pos.x + dx, pos.z + dz) == null) return false;
			}
		}
		return true;
	}

	private static void enqueueLoaded(ServerLevel level, int salt, BoundingBox box) {
		Materializer materializer = materializer(level);
		ChunkPos.rangeClosed(minChunk(box), maxChunk(box)).forEach(p -> {
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

	private static StructureStart generateStart(ServerLevel level, Structure structure, ChunkPos chunkPos) {
		try {
			ChunkGenerator generator = level.getChunkSource().getGenerator();
			StructureStart start = structure.generate(level.registryAccess(), generator,
					generator.getBiomeSource(), level.getChunkSource().randomState(),
					level.getStructureManager(), level.getSeed(), chunkPos, 0, level, biome -> true);
			return start.isValid() ? start : null;
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "[DMZ] Force-placing structure at chunk " + chunkPos.x + ", "
					+ chunkPos.z + " failed: " + e.getMessage());
			return null;
		}
	}

	private static boolean forcePlace(ServerLevel level, LevelChunk chunk, Structure structure, ChunkPos chunkPos, int salt) {
		StructureStart start = generateStart(level, structure, chunkPos);
		if (start == null) return false;
		try {
			BoundingBox box = start.getBoundingBox();
			chunk.setStartForStructure(structure, start);
			if (structure instanceof IncrementalStructure) {
				materializer(level).remember(salt, start);
				enqueueLoaded(level, salt, box);
				return true;
			}

			Set<Long> pending = ConcurrentHashMap.newKeySet();
			ChunkPos.rangeClosed(minChunk(box), maxChunk(box)).forEach(p -> pending.add(p.toLong()));
			RepairData.get(level).begin(salt, chunkPos, pending);
			materializer(level).beginForced(level, salt, structure, start, pending);
			return true;
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "[DMZ] Force-placing structure at chunk " + chunkPos.x + ", "
					+ chunkPos.z + " failed: " + e.getMessage());
			return false;
		}
	}

	private static void resumeForced(ServerLevel level) {
		RepairData data = RepairData.get(level);
		if (data.forced.isEmpty()) return;
		Materializer materializer = materializer(level);
		Map<Integer, Holder<Structure>> structures = null;
		for (Map.Entry<Integer, Pending> entry : new ArrayList<>(data.forced.entrySet())) {
			int salt = entry.getKey();
			String key = level.dimension().location() + "#" + salt + "#repair";
			if (materializer.isForcing(salt) || HANDLED.contains(key)) continue;
			ChunkPos origin = new ChunkPos(entry.getValue().origin());
			LevelChunk chunk = level.getChunkSource().getChunkNow(origin.x, origin.z);
			if (chunk == null) continue;
			HANDLED.add(key);

			if (structures == null) structures = structuresBySalt(level);
			Holder<Structure> holder = structures.get(salt);
			if (holder == null || holder.value() instanceof IncrementalStructure) continue;
			Structure structure = holder.value();
			StructureStart start = chunk.getStartForStructure(structure);
			if (start == null || !start.isValid()) {
				start = generateStart(level, structure, origin);
				if (start == null) continue;
				chunk.setStartForStructure(structure, start);
			}
			LogUtil.info(Env.SERVER, "[DMZ] Resuming materialization of " + holder.unwrapKey().map(k -> k.location().toString()).orElse("salt:" + salt)
					+ " in " + level.dimension().location() + ", " + entry.getValue().chunks().size() + " chunks left.");
			materializer.beginForced(level, salt, structure, start, entry.getValue().chunks());
		}
	}

	private static final class ChunkWork {
		final ChunkPos pos;
		final Structure structure;
		final StructureStart start;
		final ForcedPlacement forced;
		int remaining;

		ChunkWork(ChunkPos pos, Structure structure, StructureStart start, ForcedPlacement forced) {
			this.pos = pos;
			this.structure = structure;
			this.start = start;
			this.forced = forced;
		}
	}

	private record Slice(ChunkWork work, int minY, int maxY, int order, int distance) {}

	private static final class ForcedPlacement {
		final int salt;
		final Structure structure;
		final StructureStart start;
		final Set<Long> pending;

		ForcedPlacement(int salt, Structure structure, StructureStart start, Set<Long> pending) {
			this.salt = salt;
			this.structure = structure;
			this.start = start;
			this.pending = pending;
		}
	}

	private static final class PlayerSample {
		double x;
		double z;
		long tick = Long.MIN_VALUE;
	}

	private record Pending(long origin, Set<Long> chunks) {}

	private static final class RepairData extends SavedData {
		private static final String NAME = "dragonminez_structure_repair";

		private final Map<Integer, Pending> forced = new HashMap<>();

		static RepairData get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(RepairData::load, RepairData::new, NAME);
		}

		void begin(int salt, ChunkPos origin, Set<Long> chunks) {
			this.forced.put(salt, new Pending(origin.toLong(), chunks));
			setDirty();
		}

		void finish(int salt) {
			if (this.forced.remove(salt) != null) setDirty();
		}

		static RepairData load(CompoundTag tag) {
			RepairData data = new RepairData();
			ListTag list = tag.getList("forced", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entry = list.getCompound(i);
				Set<Long> chunks = ConcurrentHashMap.newKeySet();
				for (long chunk : entry.getLongArray("chunks")) chunks.add(chunk);
				if (!chunks.isEmpty()) data.forced.put(entry.getInt("salt"), new Pending(entry.getLong("origin"), chunks));
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			for (Map.Entry<Integer, Pending> e : this.forced.entrySet()) {
				CompoundTag entry = new CompoundTag();
				entry.putInt("salt", e.getKey());
				entry.putLong("origin", e.getValue().origin());
				entry.putLongArray("chunks", e.getValue().chunks().stream().mapToLong(Long::longValue).toArray());
				list.add(entry);
			}
			tag.put("forced", list);
			return tag;
		}
	}

	private static final class Materializer {
		private final WeakReference<ServerLevel> owner;
		private final ConcurrentLinkedQueue<long[]> checks = new ConcurrentLinkedQueue<>();
		private final PriorityQueue<Slice> slices = new PriorityQueue<>(
				Comparator.comparingInt(Slice::order).thenComparingInt(Slice::distance));
		private final Map<Long, ChunkWork> active = new HashMap<>();
		private final Map<Integer, StructureStart> starts = new HashMap<>();
		private final Map<Integer, ForcedPlacement> forced = new ConcurrentHashMap<>();
		private final Map<UUID, PlayerSample> samples = new HashMap<>();
		private volatile Map<Integer, Holder<Structure>> structures;
		private int deferred;

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

		boolean isForcing(int salt) {
			return this.forced.containsKey(salt);
		}

		void beginForced(ServerLevel level, int salt, Structure structure, StructureStart start, Set<Long> pending) {
			ForcedPlacement placement = new ForcedPlacement(salt, structure, start, pending);
			this.forced.put(salt, placement);
			if (pending.isEmpty()) finishForced(level, placement);
			else sweep(level, placement);
		}

		void sweepIncremental(ServerLevel level) {
			for (Map.Entry<Integer, StructureStart> entry : this.starts.entrySet()) {
				enqueueLoaded(level, entry.getKey(), entry.getValue().getBoundingBox());
			}
		}

		void sweepForced(ServerLevel level) {
			for (ForcedPlacement placement : this.forced.values()) sweep(level, placement);
		}

		private void sweep(ServerLevel level, ForcedPlacement placement) {
			for (long key : placement.pending) {
				if (this.active.containsKey(key)) continue;
				if (level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) == null) continue;
				this.checks.add(new long[]{key, placement.salt});
			}
		}

		void onChunkLoaded(ChunkPos pos) {
			if (this.forced.isEmpty()) return;
			for (ForcedPlacement placement : this.forced.values()) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						long key = ChunkPos.asLong(pos.x + dx, pos.z + dz);
						if (placement.pending.contains(key)) this.checks.add(new long[]{key, placement.salt});
					}
				}
			}
		}

		void tick(ServerLevel level) {
			boolean fast = playersMovingFast(level);
			if (this.checks.isEmpty() && this.slices.isEmpty()) {
				this.deferred = 0;
				return;
			}
			if (fast && this.deferred < MAX_DEFERRED_TICKS) {
				this.deferred++;
				return;
			}
			this.deferred = 0;
			try {
				processChecks(level);
				processSlices(level);
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "[DMZ] Structure materialization failed: " + e.getMessage());
			}
		}

		private boolean playersMovingFast(ServerLevel level) {
			long now = level.getGameTime();
			boolean fast = false;
			List<ServerPlayer> players = level.players();
			for (ServerPlayer player : players) {
				PlayerSample sample = this.samples.computeIfAbsent(player.getUUID(), id -> new PlayerSample());
				if (sample.tick == now - 1) {
					double dx = player.getX() - sample.x;
					double dz = player.getZ() - sample.z;
					if (dx * dx + dz * dz > FAST_PLAYER_SPEED_SQR) fast = true;
				}
				sample.x = player.getX();
				sample.z = player.getZ();
				sample.tick = now;
			}
			if (this.samples.size() > players.size()) this.samples.values().removeIf(sample -> sample.tick != now);
			return fast;
		}

		private void processChecks(ServerLevel level) {
			long[] check;
			int processed = 0;
			while (processed++ < MAX_CHECKS_PER_TICK && (check = this.checks.poll()) != null) {
				ChunkPos pos = new ChunkPos(check[0]);
				int salt = (int) check[1];
				ForcedPlacement placement = this.forced.get(salt);
				if (placement != null) {
					checkForced(level, placement, pos);
					continue;
				}
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
				if (!neighboursLoaded(level, pos)) continue;
				schedule(pos, structure, start, box, origin);
			}
		}

		private void checkForced(ServerLevel level, ForcedPlacement placement, ChunkPos pos) {
			long key = pos.toLong();
			if (!placement.pending.contains(key) || this.active.containsKey(key)) return;
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
			if (chunk == null) return;
			ChunkPos origin = placement.start.getChunkPos();
			if (chunk.getReferencesForStructure(placement.structure).contains(origin.toLong())) {
				completeForced(level, placement, key);
				return;
			}
			if (!neighboursLoaded(level, pos)) return;
			ChunkWork work = new ChunkWork(pos, placement.structure, placement.start, placement);
			int distance = Math.max(Math.abs(pos.x - origin.x), Math.abs(pos.z - origin.z));
			this.slices.add(new Slice(work, level.getMinBuildHeight(), level.getMaxBuildHeight(), 0, distance));
			work.remaining = 1;
			this.active.put(key, work);
		}

		private void completeForced(ServerLevel level, ForcedPlacement placement, long key) {
			if (placement.pending.remove(key)) RepairData.get(level).setDirty();
			if (placement.pending.isEmpty()) finishForced(level, placement);
		}

		private void finishForced(ServerLevel level, ForcedPlacement placement) {
			if (!this.forced.remove(placement.salt, placement)) return;
			RepairData.get(level).finish(placement.salt);
			ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(placement.structure);
			LogUtil.info(Env.SERVER, "[DMZ] Finished materializing " + (id != null ? id.toString() : "salt:" + placement.salt)
					+ " in " + level.dimension().location());
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
			ChunkWork work = new ChunkWork(pos, structure, start, null);
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
				long key = work.pos.toLong();
				if (this.active.get(key) != work) continue;
				LevelChunk chunk = level.getChunkSource().getChunkNow(work.pos.x, work.pos.z);
				if (chunk == null || !neighboursLoaded(level, work.pos)) {
					this.active.remove(key);
					continue;
				}
				BoundingBox sliceBox = new BoundingBox(work.pos.getMinBlockX(), slice.minY(), work.pos.getMinBlockZ(),
						work.pos.getMaxBlockX(), slice.maxY(), work.pos.getMaxBlockZ());
				work.start.placeInChunk(level, level.structureManager(), generator, level.getRandom(), sliceBox, work.pos);
				if (--work.remaining <= 0) {
					chunk.addReferenceForStructure(work.structure, work.start.getChunkPos().toLong());
					this.active.remove(key);
					if (work.forced != null) completeForced(level, work.forced, key);
				}
			}
		}
	}
}
