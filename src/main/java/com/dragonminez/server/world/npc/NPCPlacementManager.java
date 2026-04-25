package com.dragonminez.server.world.npc;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
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

	private static List<NPCPlacement> parsePlacements(@Nullable JsonObject root) {
		if (root == null || !root.has("placements") || !root.get("placements").isJsonArray()) {
			return List.of();
		}

		List<NPCPlacement> parsed = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("placements")) {
			if (!element.isJsonObject()) {
				continue;
			}

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
				getString(json, "structure", null),
				getDouble(json, "x", 0.5D),
				getDouble(json, "y", 64.0D),
				getDouble(json, "z", 0.5D),
				getFloat(json, "yaw", 0.0F),
				getFloat(json, "pitch", 0.0F),
				getBoolean(json, "surface", false),
				getBoolean(json, "relative_to_spawn", false),
				getBoolean(json, "enabled", true),
				getBoolean(json, "override", true)
		);
	}

	private static void spawnOrUpdate(ServerLevel level, NPCPlacement placement) {
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
		if (existing != null && existing.getType() != entityType) {
			existing.discard();
			existing = null;
		}

		if (!placement.override()) {
			return;
		}

		ResolvedPosition pos = resolvePosition(level, placement);

		if (existing == null) {
			existing = entityType.create(level);
			if (existing == null) {
				LogUtil.warn(Env.SERVER, "NPCPlacementManager: failed to create entity '{}' for placement '{}'", placement.entity(), placement.id());
				return;
			}
			applyPlacementData(existing, placement, pos);
			level.addFreshEntity(existing);
			LogUtil.info(Env.SERVER, "NPCPlacementManager: spawned '{}' at {}, {}, {}", placement.id(), pos.x(), pos.y(), pos.z());
			return;
		}

		applyPlacementData(existing, placement, pos);
	}

	@Nullable
	private static Entity findPlacedEntity(ServerLevel level, String placementId) {
		for (Entity entity : level.getAllEntities()) {
			if (placementId.equals(entity.getPersistentData().getString(PLACEMENT_TAG))) {
				return entity;
			}
		}
		return null;
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

		if (entity instanceof QuestNPCEntity questNPC) {
			if (placement.npcId() != null && !placement.npcId().isBlank()) {
				questNPC.setNpcId(placement.npcId());
			}
			questNPC.setNpcModel(placement.model());
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
		addMaster(placements, "master_kaiosama", "dragonminez:master_kaiosama", "dragonminez:otherworld", false, 0.0, 0.0, 0.0, false, 180);
		addMaster(placements, "master_enma", "dragonminez:master_enma", "dragonminez:otherworld", false, 0.0, 0.0, 0.0, false, 180);
		addMaster(placements, "master_baba", "dragonminez:master_uranai", "dragonminez:otherworld", false, 0.0, 0.0, 0.0, false, 180);
		addMaster(placements, "master_toribot", "dragonminez:master_toribot", "dragonminez:otherworld", false, 0.0, 0.0, 0.0, false, 180);

		addQuestNPC(placements, "npc_bulma", "bulma", "minecraft:overworld", 0.0, 0.0, 0.0, 210);
		addQuestNPC(placements, "npc_krillin", "krillin", "minecraft:overworld", 0.0, 0.0, 0.0, 210);
		addQuestNPC(placements, "npc_yamcha", "yamcha", "minecraft:overworld", 0.0, 0.0, 0.0, 210);
		addQuestNPC(placements, "npc_tien", "tien", "minecraft:overworld", 0.0, 0.0, 0.0, 210);
		addQuestNPC(placements, "npc_chiaotzu", "chiaotzu", "minecraft:overworld", 0.0, 0.0, 0.0, 210);
		addQuestNPC(placements, "npc_piccolo", "piccolo", "minecraft:overworld", 0.0, 0.0, 0.0, 150);
		addQuestNPC(placements, "npc_gohan", "gohan", "minecraft:overworld", 0.0, 0.0, 0.0, 150);
		addQuestNPC(placements, "npc_vegeta", "vegeta", "minecraft:overworld", 0.0, 0.0, 0.0, 330);
		addQuestNPC(placements, "npc_trunks", "trunks", "minecraft:overworld", 0.0, 0.0, 0.0, 330);
		addQuestNPC(placements, "npc_chi_chi", "chi_chi", "minecraft:overworld", 0.0, 0.0, 0.0, 150);
		addQuestNPC(placements, "npc_videl", "videl", "minecraft:overworld", 0.0, 0.0, 0.0, 150);
		addQuestNPC(placements, "npc_farmer_01", "farmer_01", "minecraft:overworld", 0.0, 0.0, 0.0, 300);
		addQuestNPC(placements, "npc_merchant_01", "merchant_01", "minecraft:overworld", 0.0, 0.0, 0.0, 60);
		addQuestNPC(placements, "npc_scholar_01", "scholar_01", "minecraft:overworld", 0.0, 0.0, 0.0, 45);
		addQuestNPC(placements, "npc_namek_elder", "namek_elder", "dragonminez:namek", 0.0, 0.0, 0.0, 180);

		return placements;
	}

	private static void addMaster(JsonArray placements, String id, String entity, String dimension,
								  boolean relativeToSpawn, double x, double y, double z, boolean surface, float yaw) {
		JsonObject placement = basePlacement(id, entity, dimension, relativeToSpawn, x, y, z, surface, yaw);
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
									double x, double y, double z, float yaw) {
		JsonObject placement = basePlacement(id, Reference.MOD_ID + ":quest_npc", dimension, true, x, y, z, true, yaw);
		placement.addProperty("npc_id", npcId);
		placement.addProperty("model", npcId);
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

	private record NPCPlacement(String id, String entity, ResourceKey<Level> dimension, @Nullable String npcId,
								String model, @Nullable String structureId, double x, double y, double z, float yaw, float pitch,
								boolean surface, boolean relativeToSpawn, boolean enabled, boolean override) {
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
