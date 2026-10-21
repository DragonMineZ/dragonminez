package com.dragonminez.server.world.npc;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.diagnostics.JsonKeys;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.server.world.structure.helper.StructureFrame;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class StructureNpcs extends SimpleJsonResourceReloadListener {
	private static final Gson GSON = new GsonBuilder().create();
	public static final StructureNpcs INSTANCE = new StructureNpcs();

	private static final String DIRECTORY = "structure_npcs";
	private static final String REPORT = "structure_npcs";
	private static final Set<String> ROOT_KEYS = JsonKeys.of("structure", "npcs");
	private static final Set<String> NPC_KEYS = JsonKeys.of("id", "entity", "npc_id", "model", "texture", "template",
			"pos", "yaw", "alignment", "relation");
	private static final int CHUNKS_PER_TICK = 8;
	private static final int MAX_RETRIES = 100;
	private static final int RESCAN_RADIUS = 8;

	private static volatile Map<ResourceLocation, List<Definition>> definitions = Map.of();
	private static volatile int generation;
	private static final Map<ResourceKey<Level>, ConcurrentLinkedQueue<Long>> PENDING = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, Integer> SCANNED_GENERATION = new ConcurrentHashMap<>();
	private static final Map<ResourceKey<Level>, Map<Long, Integer>> RETRIES = new ConcurrentHashMap<>();

	private StructureNpcs() {
		super(GSON, DIRECTORY);
	}

	public record Definition(ResourceLocation structure, String id, ResourceLocation entity, @Nullable String npcId,
							 String model, String texture, @Nullable ResourceLocation template, Vec3 pos, float yaw,
							 @Nullable Integer alignment, @Nullable String relation) {
		public String trackingKey(StructureStart start) {
			return "structure/" + this.structure + "/" + start.getChunkPos().toLong() + "/" + this.id;
		}
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
		JsonLoadReport.clear(REPORT);
		Map<ResourceLocation, List<Definition>> loaded = new HashMap<>();
		int count = 0;
		for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
			String label = DIRECTORY + "/" + entry.getKey();
			if (!entry.getValue().isJsonObject()) {
				JsonLoadReport.error(REPORT, label, "root must be an object");
				continue;
			}
			JsonObject root = entry.getValue().getAsJsonObject();
			JsonKeys.checkObject(REPORT, label, "", root, ROOT_KEYS);
			ResourceLocation structure = root.has("structure")
					? ResourceLocation.tryParse(root.get("structure").getAsString())
					: entry.getKey();
			if (structure == null || !root.has("npcs") || !root.get("npcs").isJsonArray()) {
				JsonLoadReport.error(REPORT, label, "needs a valid 'structure' and an 'npcs' array");
				continue;
			}
			JsonArray npcs = root.getAsJsonArray("npcs");
			for (int i = 0; i < npcs.size(); i++) {
				if (!npcs.get(i).isJsonObject()) continue;
				JsonObject json = npcs.get(i).getAsJsonObject();
				JsonKeys.checkObject(REPORT, label, "npcs[" + i + "]", json, NPC_KEYS);
				Definition definition = parse(structure, json);
				if (definition == null) {
					JsonLoadReport.error(REPORT, label, "npcs[" + i + "] needs 'id', 'entity' and a 3-number 'pos'");
					continue;
				}
				loaded.computeIfAbsent(structure, key -> new ArrayList<>()).add(definition);
				count++;
			}
		}
		Map<ResourceLocation, List<Definition>> frozen = new HashMap<>();
		loaded.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
		definitions = Collections.unmodifiableMap(frozen);
		generation++;
		LogUtil.info(Env.SERVER, "StructureNpcs: loaded {} NPC(s) for {} structure(s)", count, frozen.size());
	}

	@Nullable
	private static Definition parse(ResourceLocation structure, JsonObject json) {
		if (!json.has("id") || !json.has("entity") || !json.has("pos") || !json.get("pos").isJsonArray()) return null;
		JsonArray pos = json.getAsJsonArray("pos");
		if (pos.size() != 3) return null;
		ResourceLocation entity = ResourceLocation.tryParse(json.get("entity").getAsString());
		if (entity == null) return null;
		ResourceLocation template = json.has("template") ? ResourceLocation.tryParse(json.get("template").getAsString()) : null;
		return new Definition(structure, json.get("id").getAsString(), entity,
				json.has("npc_id") ? json.get("npc_id").getAsString() : null,
				json.has("model") ? json.get("model").getAsString() : "",
				json.has("texture") ? json.get("texture").getAsString() : "",
				template,
				new Vec3(pos.get(0).getAsDouble(), pos.get(1).getAsDouble(), pos.get(2).getAsDouble()),
				json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0F,
				json.has("alignment") ? json.get("alignment").getAsInt() : null,
				json.has("relation") ? json.get("relation").getAsString() : null);
	}

	public static List<Definition> definitionsFor(ResourceLocation structure) {
		return definitions.getOrDefault(structure, List.of());
	}

	public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
		Map<ResourceLocation, List<Definition>> current = definitions;
		if (current.isEmpty()) return;
		var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		for (Structure structure : chunk.getAllReferences().keySet()) {
			ResourceLocation id = registry.getKey(structure);
			if (id != null && current.containsKey(id)) {
				PENDING.computeIfAbsent(level.dimension(), key -> new ConcurrentLinkedQueue<>()).add(chunk.getPos().toLong());
				return;
			}
		}
	}

	public static void onLevelUnload(ServerLevel level) {
		PENDING.remove(level.dimension());
		SCANNED_GENERATION.remove(level.dimension());
		RETRIES.remove(level.dimension());
	}

	public static void tick(ServerLevel level) {
		if (definitions.isEmpty()) return;
		if (SCANNED_GENERATION.getOrDefault(level.dimension(), -1) != generation) {
			SCANNED_GENERATION.put(level.dimension(), generation);
			rescanAroundPlayers(level);
		}
		ConcurrentLinkedQueue<Long> queue = PENDING.get(level.dimension());
		if (queue == null || queue.isEmpty()) return;
		Set<Long> batch = new LinkedHashSet<>();
		Long next;
		while (batch.size() < CHUNKS_PER_TICK && (next = queue.poll()) != null) batch.add(next);
		Map<Long, Integer> retries = RETRIES.computeIfAbsent(level.dimension(), key -> new HashMap<>());
		for (long packed : batch) {
			ChunkPos pos = new ChunkPos(packed);
			LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
			if (chunk == null) {
				if (retries.merge(packed, 1, Integer::sum) <= MAX_RETRIES) queue.add(packed);
				else retries.remove(packed);
				continue;
			}
			retries.remove(packed);
			try {
				processChunk(level, chunk);
			} catch (RuntimeException e) {
				LogUtil.error(Env.SERVER, "StructureNpcs: failed to process chunk {} in {}", pos, level.dimension().location(), e);
			}
		}
	}

	private static void rescanAroundPlayers(ServerLevel level) {
		ConcurrentLinkedQueue<Long> queue = PENDING.computeIfAbsent(level.dimension(), key -> new ConcurrentLinkedQueue<>());
		for (ServerPlayer player : level.players()) {
			ChunkPos center = player.chunkPosition();
			for (int dx = -RESCAN_RADIUS; dx <= RESCAN_RADIUS; dx++) {
				for (int dz = -RESCAN_RADIUS; dz <= RESCAN_RADIUS; dz++) {
					if (level.getChunkSource().getChunkNow(center.x + dx, center.z + dz) == null) continue;
					queue.add(ChunkPos.asLong(center.x + dx, center.z + dz));
				}
			}
		}
	}

	private static void processChunk(ServerLevel level, LevelChunk chunk) {
		var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		for (Structure structure : chunk.getAllReferences().keySet()) {
			ResourceLocation id = registry.getKey(structure);
			if (id == null) continue;
			List<Definition> list = definitionsFor(id);
			if (list.isEmpty()) continue;
			for (StructureStart start : level.structureManager().startsForStructure(chunk.getPos(), candidate -> candidate == structure)) {
				if (!start.isValid()) continue;
				for (Definition definition : list) spawnIfMissing(level, chunk.getPos(), start, definition);
			}
		}
	}

	public static Optional<Vec3> resolve(StructureStart start, Definition definition) {
		return StructureFrame.forTemplate(start, definition.template()).map(frame -> frame.toWorld(definition.pos()));
	}

	private static void spawnIfMissing(ServerLevel level, ChunkPos chunk, StructureStart start, Definition definition) {
		Optional<StructureFrame> frame = StructureFrame.forTemplate(start, definition.template());
		if (frame.isEmpty()) return;
		Vec3 pos = frame.get().toWorld(definition.pos());
		BlockPos block = BlockPos.containing(pos);
		if (SectionPos.blockToSectionCoord(block.getX()) != chunk.x || SectionPos.blockToSectionCoord(block.getZ()) != chunk.z) return;

		String key = definition.trackingKey(start);
		NPCPlacementSavedData tracking = NPCPlacementSavedData.get(level);
		if (tracking.hasPlacement(key)) return;

		EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(definition.entity());
		if (type == null) {
			LogUtil.warn(Env.SERVER, "StructureNpcs: unknown entity '{}' for '{}' in {}", definition.entity(), definition.id(), definition.structure());
			return;
		}
		Entity entity = type.create(level);
		if (entity == null) return;
		float yaw = frame.get().toWorldYaw(definition.yaw());
		entity.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
		entity.setYHeadRot(yaw);
		entity.setYBodyRot(yaw);
		if (entity instanceof Mob mob) mob.setPersistenceRequired();
		entity.getPersistentData().putString(NPCPlacementManager.PLACEMENT_TAG, definition.id());
		if (definition.alignment() != null) {
			entity.getPersistentData().putInt(NpcDispositionService.NPC_ALIGNMENT_TAG, Math.max(0, Math.min(100, definition.alignment())));
		}
		if (definition.relation() != null && !definition.relation().isBlank()) {
			entity.getPersistentData().putString(NpcDispositionService.NPC_RELATION_OVERRIDE_TAG, definition.relation());
		}
		if (entity instanceof QuestNPCEntity questNpc) {
			if (definition.npcId() != null && !definition.npcId().isBlank()) questNpc.setNpcId(definition.npcId());
			questNpc.setNpcModel(definition.model());
			questNpc.setNpcTexture(definition.texture());
		}
		if (!level.addFreshEntity(entity)) return;
		tracking.markSpawned(key, entity.getUUID());
		LogUtil.info(Env.SERVER, "StructureNpcs: spawned '{}' in {} at {}, {}, {}", definition.id(), definition.structure(),
				Math.round(pos.x), Math.round(pos.y), Math.round(pos.z));
	}
}
