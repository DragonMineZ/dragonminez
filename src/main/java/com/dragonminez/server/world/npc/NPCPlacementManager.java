package com.dragonminez.server.world.npc;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.diagnostics.JsonKeys;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads and enforces world-editable friendly NPC placements.
 */
public final class NPCPlacementManager {
	public static final String PLACEMENT_TAG = "dmz_npc_placement_id";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String NPC_FOLDER = "dragonminez" + java.io.File.separator + "npcs";
	private static final String PLACEMENTS_FILE = "placements.json";

	private static List<NPCPlacement> placements = List.of();
	private static Path loadedFrom = null;

	private NPCPlacementManager() {
	}

	public static void load(MinecraftServer server) {
		if (server == null) {
			return;
		}

		Path worldFolder = server.getWorldPath(LevelResource.ROOT);
		Path npcDir = worldFolder.resolve(NPC_FOLDER);
		Path file = npcDir.resolve(PLACEMENTS_FILE);

		try {
			Files.createDirectories(npcDir);
			if (!Files.exists(file)) {
				writeDefaultPlacements(file);
			}

			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				JsonObject root = GSON.fromJson(reader, JsonObject.class);
				placements = parsePlacements(root);
				loadedFrom = file;
				LogUtil.info(Env.SERVER, "NPCPlacementManager: loaded {} placement(s)", placements.size());
			}
		} catch (Exception e) {
			placements = List.of();
			loadedFrom = file;
			LogUtil.error(Env.SERVER, "NPCPlacementManager: failed to load NPC placements from {}", file, e);
		}
	}

	public static void spawnForLoadedLevels(MinecraftServer server) {
		if (server == null) {
			return;
		}

		ensureLoaded(server);
		for (ServerLevel level : server.getAllLevels()) {
			spawnForLevel(server, level);
		}
	}

	public static void spawnForLevel(ServerLevel level) {
		if (level == null) {
			return;
		}
		spawnForLevel(level.getServer(), level);
	}

	public static void spawnForLevel(MinecraftServer server, ServerLevel level) {
		if (server == null || level == null) {
			return;
		}

		ensureLoaded(server);
		if (placements.isEmpty()) {
			return;
		}

		for (NPCPlacement placement : placements) {
			if (!placement.enabled() || !placement.dimension().equals(level.dimension())) {
				continue;
			}
			spawnOrUpdate(level, placement);
		}
	}

	private static void ensureLoaded(MinecraftServer server) {
		Path file = server.getWorldPath(LevelResource.ROOT).resolve(NPC_FOLDER).resolve(PLACEMENTS_FILE);
		if (loadedFrom == null || !loadedFrom.equals(file)) {
			load(server);
		}
	}

	private static final String PLACEMENTS_LABEL = "npcs/" + PLACEMENTS_FILE;
	private static final java.util.Set<String> ROOT_KEYS = java.util.Set.of("placements", "schema");
	private static final java.util.Set<String> PLACEMENT_KEYS = java.util.Set.of("id", "entity", "dimension",
			"npc_id", "model", "texture", "structure", "x", "y", "z", "yaw", "pitch", "surface",
			"relative_to_spawn", "enabled", "override", "alignment", "relation");

	private static List<NPCPlacement> parsePlacements(@Nullable JsonObject root) {
		if (root == null || !root.has("placements") || !root.get("placements").isJsonArray()) {
			return List.of();
		}

		JsonLoadReport.clear("npcs");
		JsonKeys.checkObject("npcs", PLACEMENTS_LABEL, "", root, ROOT_KEYS);

		List<NPCPlacement> parsed = new ArrayList<>();
		int index = 0;
		for (JsonElement element : root.getAsJsonArray("placements")) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonKeys.checkObject("npcs", PLACEMENTS_LABEL, "placements[" + index++ + "]", element.getAsJsonObject(), PLACEMENT_KEYS);
			NPCPlacement placement = parsePlacement(element.getAsJsonObject());
			if (placement != null) {
				parsed.add(placement);
			}
		}
		return List.copyOf(parsed);
	}

	@Nullable
	private static NPCPlacement parsePlacement(JsonObject json) {
		String id = getString(json, "id", null);
		String entity = getString(json, "entity", null);
		String dimension = getString(json, "dimension", Level.OVERWORLD.location().toString());
		if (id == null || id.isBlank() || entity == null || entity.isBlank()) {
			return null;
		}

		ResourceKey<Level> dimensionKey;
		try {
			dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, ResourceLocation.parse(dimension));
		} catch (Exception ignored) {
			return null;
		}

		return new NPCPlacement(
				id,
				entity,
				dimensionKey,
				getString(json, "npc_id", null),
				getString(json, "model", ""),
				getString(json, "texture", ""),
				getString(json, "structure", null),
				getDouble(json, "x", 0.5D),
				getDouble(json, "y", 64.0D),
				getDouble(json, "z", 0.5D),
				getFloat(json, "yaw", 0.0F),
				getFloat(json, "pitch", 0.0F),
				getBoolean(json, "surface", false),
				getBoolean(json, "relative_to_spawn", false),
				getBoolean(json, "enabled", true),
				getBoolean(json, "override", false),
				getOptionalInt(json, "alignment"),
				getString(json, "relation", null)
		);
	}

	private static void spawnOrUpdate(ServerLevel level, NPCPlacement placement) {
		NPCPlacementSavedData placementData = NPCPlacementSavedData.get(level);
		boolean overridePlacement = placement.override() || shouldForceManualSpawn(placement);
		ResourceLocation entityId;
		try {
			entityId = ResourceLocation.parse(placement.entity());
		} catch (Exception ignored) {
			LogUtil.warn(Env.SERVER, "NPCPlacementManager: invalid entity id '{}' for placement '{}'", placement.entity(), placement.id());
			return;
		}

		EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
		if (entityType == null) {
			LogUtil.warn(Env.SERVER, "NPCPlacementManager: unknown entity '{}' for placement '{}'", placement.entity(), placement.id());
			return;
		}

		Entity existing = findPlacedEntity(level, placement.id());
		if (existing == null) {
			Optional<UUID> placedUuid = placementData.getEntityUuid(placement.id());
			if (placedUuid.isPresent()) {
				Entity trackedEntity = level.getEntity(placedUuid.get());
				if (trackedEntity == null) {
					ResolvedPosition pos = resolvePosition(level, placement);
					MissingTrackedEntityAction action = resolveMissingTrackedEntity(isPlacementChunkLoaded(level, pos), overridePlacement);
					if (action != MissingTrackedEntityAction.RESPAWN) {
						return;
					}
					placementData.clear(placement.id());
				} else {
					existing = trackedEntity;
					applyPlacementMetadata(existing, placement);
				}
			}
		}

		if (existing != null && existing.getType() != entityType) {
			if (!overridePlacement) {
				LogUtil.warn(Env.SERVER, "NPCPlacementManager: placement '{}' is tagged on entity type '{}' but expects '{}'",
						placement.id(), existing.getType(), entityType);
				return;
			}
			existing.discard();
			placementData.clear(placement.id());
			existing = null;
		}

		if (existing != null) {
			placementData.markSpawned(placement.id(), existing.getUUID());
			if (!overridePlacement) {
				applyPlacementMetadata(existing, placement);
				return;
			}
		}

		if (existing == null) {
			if (!overridePlacement) {
				return;
			}

			ResolvedPosition pos = resolvePosition(level, placement);
			existing = entityType.create(level);
			if (existing == null) {
				LogUtil.warn(Env.SERVER, "NPCPlacementManager: failed to create entity '{}' for placement '{}'", placement.entity(), placement.id());
				return;
			}
			applyPlacementData(existing, placement, pos);
			boolean added = level.addFreshEntity(existing);
			if (!added) {
				LogUtil.warn(Env.SERVER, "NPCPlacementManager: failed to add spawned entity for placement '{}'", placement.id());
				return;
			}
			placementData.markSpawned(placement.id(), existing.getUUID());
			LogUtil.info(Env.SERVER, "NPCPlacementManager: spawned '{}' at {}, {}, {}", placement.id(), pos.x(), pos.y(), pos.z());
			return;
		}

		applyPlacementData(existing, placement, resolvePosition(level, placement));
	}

	@Nullable
	private static Entity findPlacedEntity(ServerLevel level, String placementId) {
		List<Entity> matches = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity != null && placementId.equals(entity.getPersistentData().getString(PLACEMENT_TAG))) {
				matches.add(entity);
			}
		}
		if (matches.isEmpty()) {
			return null;
		}
		Entity first = matches.get(0);
		for (int i = 1; i < matches.size(); i++) {
			matches.get(i).discard();
			LogUtil.warn(Env.SERVER, "NPCPlacementManager: removed duplicate entity for placement '{}'", placementId);
		}
		return first;
	}

	private static void applyPlacementData(Entity entity, NPCPlacement placement, ResolvedPosition pos) {
		entity.moveTo(pos.x(), pos.y(), pos.z(), placement.yaw(), placement.pitch());
		entity.setYHeadRot(placement.yaw());
		entity.setYBodyRot(placement.yaw());
		applyPlacementMetadata(entity, placement);
	}

	private static void applyPlacementMetadata(Entity entity, NPCPlacement placement) {
		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
		}
		entity.getPersistentData().putString(PLACEMENT_TAG, placement.id());
		if (placement.alignment() != null) {
			entity.getPersistentData().putInt(NpcDispositionService.NPC_ALIGNMENT_TAG, Math.max(0, Math.min(100, placement.alignment())));
		}
		if (placement.relationOverride() != null && !placement.relationOverride().isBlank()) {
			entity.getPersistentData().putString(NpcDispositionService.NPC_RELATION_OVERRIDE_TAG, placement.relationOverride());
		}

		if (entity instanceof QuestNPCEntity questNPC) {
			if (placement.npcId() != null && !placement.npcId().isBlank()) {
				questNPC.setNpcId(placement.npcId());
			}
			questNPC.setNpcModel(placement.model());
			questNPC.setNpcTexture(placement.texture());
		}
	}

	private static ResolvedPosition resolvePosition(ServerLevel level, NPCPlacement placement) {
		double x = placement.x();
		double y = placement.y();
		double z = placement.z();
		if (placement.structureId() != null && !placement.structureId().isBlank()) {
			ResourceKey<Structure> structureKey = structureKeyFromId(placement.structureId());
			if (structureKey != null) {
				BlockPos searchFrom = level.getSharedSpawnPos();
				BlockPos structureOrigin = StructureLocator.locateStructure(level, structureKey, searchFrom);
				if (structureOrigin != null) {
					x += structureOrigin.getX();
					y += structureOrigin.getY();
					z += structureOrigin.getZ();
				} else {
					LogUtil.warn(Env.SERVER, "NPCPlacementManager: structure '{}' not found for placement '{}', using raw coordinates", placement.structureId(), placement.id());
				}
			} else {
				LogUtil.warn(Env.SERVER, "NPCPlacementManager: unknown structure '{}' for placement '{}'", placement.structureId(), placement.id());
			}
		}
		if (placement.relativeToSpawn()) {
			BlockPos spawn = level.getSharedSpawnPos();
			x += spawn.getX() + 0.5D;
			z += spawn.getZ() + 0.5D;
		}
		if (placement.surface()) {
			BlockPos column = BlockPos.containing(x, 0, z);
			level.getChunk(column);
			y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
		}
		return new ResolvedPosition(x, y, z);
	}

	private static boolean isPlacementChunkLoaded(ServerLevel level, ResolvedPosition pos) {
		return level.isLoaded(BlockPos.containing(pos.x(), pos.y(), pos.z()));
	}

	private static void writeDefaultPlacements(Path file) throws IOException {
		JsonObject root = new JsonObject();
		root.addProperty("schema", 1);
		root.add("placements", defaultPlacements());
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(root, writer);
		}
	}

	private static JsonArray defaultPlacements() {
		JsonArray placements = new JsonArray();

		addMasterInStructure(placements, "master_roshi", "dragonminez:master_roshi", "minecraft:overworld", "roshi_house", 0.0, 0.0, 0.0, true, 225);
		addMasterInStructure(placements, "master_goku", "dragonminez:master_goku", "minecraft:overworld", "goku_house", 0.0, 0.0, 0.0, true, 225);
		addMasterInStructure(placements, "master_karin", "dragonminez:master_karin", "minecraft:overworld", "kamilookout", 0.0, 0.0, 0.0, true, 135);
		addMasterInStructure(placements, "master_dende", "dragonminez:master_dende", "minecraft:overworld", "kamilookout", 0.0, 0.0, 0.0, true, 135);
		addMasterInStructure(placements, "master_popo", "dragonminez:master_popo", "minecraft:overworld", "kamilookout", 0.0, 0.0, 0.0, true, 135);
		addMasterInStructure(placements, "master_gero", "dragonminez:master_gero", "minecraft:overworld", "gero_lab", 0.0, 0.0, 0.0, true, 315);
		addMasterInStructure(placements, "master_guru", "dragonminez:master_guru", "dragonminez:namek", "elder_guru", 0.0, 0.0, 0.0, true, 180);
		addManualMaster(placements, "master_kaiosama", "dragonminez:master_kaiosama", "dragonminez:otherworld", false, 54.5, 190, 1082.5, false, 180);
		addManualMaster(placements, "master_enma", "dragonminez:master_enma", "dragonminez:otherworld", false, 0.5, 41, 66.5, false, 180);
		addManualMaster(placements, "master_baba", "dragonminez:master_uranai", "dragonminez:otherworld", false, 6.5, 41, 53.5, false, 180);
		addManualMaster(placements, "master_toribot", "dragonminez:master_toribot", "dragonminez:otherworld", false, 50.5, 190, 1079.5, false, 180);

		// Quest NPCs (TALK_TO / quest-giver / turn-in targets) are no longer spawned at runtime here:
		// they are baked directly into structure NBT via structure blocks (mirroring the namek_trader
		// pattern), so they live in fixed, hand-placed spots instead of piling up at world spawn.
		// These entries are kept DISABLED as a reference for each NPC's id/model/texture; addQuestNPC
		// forces enabled=false. See docs/quest_npc_structure_placement.md for the in-game workflow and
		// the per-NPC spawn commands. Masters above stay runtime-placed (they resolve into structures).
		addQuestNPC(placements, "npc_bulma", "bulma", "minecraft:overworld", 0.0, 0.0, 0.0, 210, "saga_bulma", "saga_bulma");
		addQuestNPC(placements, "npc_krillin", "krillin", "minecraft:overworld", 0.0, 0.0, 0.0, 210, "saga_vegeta", "saga_krillin");
		addQuestNPC(placements, "npc_yamcha", "yamcha", "minecraft:overworld", 0.0, 0.0, 0.0, 210, "saga_yamcha", "saga_yamcha");
		addQuestNPC(placements, "npc_tien", "tien", "minecraft:overworld", 0.0, 0.0, 0.0, 210, "saga_goku", "saga_tien_early");
		addQuestNPC(placements, "npc_piccolo", "piccolo", "minecraft:overworld", 0.0, 0.0, 0.0, 150, "saga_piccolo", "saga_piccolo");
		addQuestNPC(placements, "npc_gohan", "gohan", "minecraft:overworld", 0.0, 0.0, 0.0, 150, "saga_gohan_mid", "saga_gohan_mid_base");
		addQuestNPC(placements, "npc_vegeta", "vegeta", "minecraft:overworld", 0.0, 0.0, 0.0, 330, "saga_vegeta", "saga_vegeta");
		addQuestNPC(placements, "npc_trunks", "trunks", "minecraft:overworld", 0.0, 0.0, 0.0, 330, "saga_trunks", "saga_ftrunks_base");
		addQuestNPC(placements, "npc_chi_chi", "chi_chi", "minecraft:overworld", 0.0, 0.0, 0.0, 150, "", "");
		addQuestNPC(placements, "npc_videl", "videl", "minecraft:overworld", 0.0, 0.0, 0.0, 150, "saga_videl", "saga_videl");
		addQuestNPC(placements, "npc_shin", "shin", "minecraft:overworld", 0.0, 0.0, 0.0, 180, "saga_shin", "saga_shin");
		addQuestNPC(placements, "npc_namek_elder", "namek_elder", "dragonminez:namek", 0.0, 0.0, 0.0, 180, "", "");

		return placements;
	}

	private static void addManualMaster(JsonArray placements, String id, String entity, String dimension,
									 boolean relativeToSpawn, double x, double y, double z, boolean surface, float yaw) {
		JsonObject placement = basePlacement(id, entity, dimension, relativeToSpawn, x, y, z, surface, yaw);
		placement.addProperty("override", true);
		placements.add(placement);
	}

	private static void addMasterInStructure(JsonArray placements, String id, String entity, String dimension,
										 String structureId, double offsetX, double offsetY, double offsetZ,
										 boolean surface, float yaw) {
		JsonObject placement = basePlacement(id, entity, dimension, false, offsetX, offsetY, offsetZ, surface, yaw);
		placement.addProperty("structure", structureId);
		placements.add(placement);
	}

	private static void addQuestNPC(JsonArray placements, String id, String npcId, String dimension,
									double x, double y, double z, float yaw, String model, String texture) {
		JsonObject placement = basePlacement(id, Reference.MOD_ID + ":quest_npc", dimension, true, x, y, z, true, yaw);
		placement.addProperty("npc_id", npcId);
		placement.addProperty("model", model);
		placement.addProperty("texture", texture);
		// Disabled: these NPCs are baked into structure NBT instead of runtime-spawned at world spawn.
		placement.addProperty("enabled", false);
		placements.add(placement);
	}

	private static JsonObject basePlacement(String id, String entity, String dimension, boolean relativeToSpawn,
											double x, double y, double z, boolean surface, float yaw) {
		JsonObject placement = new JsonObject();
		placement.addProperty("id", id);
		placement.addProperty("entity", entity);
		placement.addProperty("dimension", dimension);
		placement.addProperty("x", x);
		placement.addProperty("y", y);
		placement.addProperty("z", z);
		placement.addProperty("yaw", yaw);
		placement.addProperty("pitch", 0.0F);
		placement.addProperty("surface", surface);
		placement.addProperty("relative_to_spawn", relativeToSpawn);
		placement.addProperty("enabled", true);
		placement.addProperty("override", false);
		return placement;
	}

	@Nullable
	private static String getString(JsonObject json, String key, @Nullable String fallback) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return fallback;
		}
		return json.get(key).getAsString();
	}

	private static double getDouble(JsonObject json, String key, double fallback) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return fallback;
		}
		return json.get(key).getAsDouble();
	}

	private static float getFloat(JsonObject json, String key, float fallback) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return fallback;
		}
		return json.get(key).getAsFloat();
	}

	private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return fallback;
		}
		return json.get(key).getAsBoolean();
	}

	@Nullable
	private static Integer getOptionalInt(JsonObject json, String key) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return null;
		}
		return json.get(key).getAsInt();
	}

	private static boolean shouldForceManualSpawn(NPCPlacement placement) {
		if (!"dragonminez:otherworld".equals(placement.dimension().location().toString())) {
			return false;
		}

		return switch (placement.id()) {
			case "master_kaiosama", "master_enma", "master_baba", "master_toribot" -> true;
			default -> false;
		};
	}

	static MissingTrackedEntityAction resolveMissingTrackedEntity(boolean placementChunkLoaded, boolean overridePlacement) {
		if (!placementChunkLoaded) {
			return MissingTrackedEntityAction.WAIT_FOR_CHUNK;
		}
		return overridePlacement ? MissingTrackedEntityAction.RESPAWN : MissingTrackedEntityAction.SKIP;
	}

	enum MissingTrackedEntityAction {
		WAIT_FOR_CHUNK,
		RESPAWN,
		SKIP
	}

	private record NPCPlacement(String id, String entity, ResourceKey<Level> dimension, @Nullable String npcId,
								String model, String texture, @Nullable String structureId, double x, double y, double z, float yaw, float pitch,
								boolean surface, boolean relativeToSpawn, boolean enabled, boolean override,
								@Nullable Integer alignment, @Nullable String relationOverride) {
	}

	@Nullable
	private static ResourceKey<Structure> structureKeyFromId(String structureId) {
		return switch (structureId) {
			case "goku_house" -> DMZStructures.GOKU_HOUSE;
			case "roshi_house" -> DMZStructures.ROSHI_HOUSE;
			case "elder_guru" -> DMZStructures.ELDER_GURU;
			case "timechamber" -> DMZStructures.TIMECHAMBER;
			case "kamilookout" -> DMZStructures.KAMILOOKOUT;
			case "gero_lab" -> DMZStructures.GERO_LAB;
			default -> null;
		};
	}

	private record ResolvedPosition(double x, double y, double z) {
	}
}
