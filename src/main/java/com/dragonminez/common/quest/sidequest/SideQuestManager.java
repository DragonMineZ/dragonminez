package com.dragonminez.common.quest.sidequest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.quest.QuestObjective;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SideQuestManager extends SimplePreparableReloadListener<Map<String, SideQuest>> {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, SideQuest> LOADED_QUESTS = new HashMap<>();
	private static final Map<String, SideQuest> CLIENT_QUESTS = new HashMap<>();
	private static final String SIDE_QUEST_FOLDER = "dragonminez" + File.separator + "sidequests";

	/**
	 * Performance index: ObjectiveType → list of side-quest IDs that contain that objective type.
	 * Built once after all files are loaded. Allows event handlers to only iterate relevant quests.
	 */
	private static final Map<QuestObjective.ObjectiveType, List<String>> OBJECTIVE_INDEX = new EnumMap<>(QuestObjective.ObjectiveType.class);

	/**
	 * Maps NPC ID → list of side-quest IDs where that NPC is the quest giver.
	 */
	private static final Map<String, List<String>> QUEST_GIVER_INDEX = new HashMap<>();

	/**
	 * Maps NPC ID → list of side-quest IDs where that NPC is the turn-in target.
	 */
	private static final Map<String, List<String>> TURN_IN_INDEX = new HashMap<>();

	public static void init() {}

	public static void loadSideQuests(MinecraftServer server) {
		LOADED_QUESTS.clear();
		OBJECTIVE_INDEX.clear();
		QUEST_GIVER_INDEX.clear();
		TURN_IN_INDEX.clear();

		if (!ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) return;

		if (server == null) {
			LogUtil.warn(Env.COMMON, "Cannot load side-quests: server is null");
			return;
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			LogUtil.warn(Env.COMMON, "Cannot load side-quests: overworld is null");
			return;
		}

		Path worldFolder = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
		Path sideQuestDir = worldFolder.resolve(SIDE_QUEST_FOLDER);

		try {
			if (!Files.exists(sideQuestDir)) {
				Files.createDirectories(sideQuestDir);
				createDefaultSideQuestFiles(sideQuestDir);
			}

			try (var stream = Files.walk(sideQuestDir)) {
				stream.filter(path -> path.toString().endsWith(".json"))
						.forEach(SideQuestManager::loadFile);
			}

			buildObjectiveIndex();

			LogUtil.info(Env.COMMON, "Loaded {} side-quest(s) from world folder", LOADED_QUESTS.size());
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to load side-quests", e);
		}
	}

	private static void loadFile(Path file) {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			SideQuest sideQuest = SideQuestParser.parse(root);
			LOADED_QUESTS.put(sideQuest.getId(), sideQuest);
			LogUtil.info(Env.COMMON, "Loaded side-quest: {}", sideQuest.getId());
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load side-quest file: {}", file, e);
		}
	}

	// ---- Default Side-Quest File Generation ----

	private static void createDefaultSideQuestFiles(Path sideQuestDir) {
		if (!ConfigManager.getServerConfig().getGameplay().getCreateDefaultSideQuests()) return;

		createTrainingCategory(sideQuestDir);
		createExplorationCategory(sideQuestDir);
		createCombatCategory(sideQuestDir);
	}

	private static void writeQuestFile(Path dir, String filename, JsonObject quest) {
		Path file = dir.resolve(filename);
		if (Files.exists(file)) return;
		try {
			Files.createDirectories(dir);
			try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
				GSON.toJson(quest, writer);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create default side-quest file: {}", filename, e);
		}
	}

	private static JsonObject createObjective(String type, String description, String target, String entity, int count) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type);
		obj.addProperty("description", description);
		if (target != null) {
			switch (type.toUpperCase()) {
				case "BIOME" -> obj.addProperty("biome", target);
				case "STRUCTURE" -> obj.addProperty("structure", target);
				case "ITEM" -> { obj.addProperty("item", target); obj.addProperty("count", count); }
				case "TALK_TO" -> obj.addProperty("npcId", target);
			}
		}
		if (entity != null) {
			obj.addProperty("entity", entity);
			if (count > 0) obj.addProperty("count", count);
		}
		return obj;
	}

	/**
	 * Convenience method to create a TALK_TO objective JSON.
	 */
	private static JsonObject createTalkToObjective(String description, String npcId) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", "TALK_TO");
		obj.addProperty("description", description);
		obj.addProperty("npcId", npcId);
		return obj;
	}

	private static JsonObject createReward(String type, int amount, String item, int count, String command) {
		JsonObject reward = new JsonObject();
		reward.addProperty("type", type);
		if (type.equalsIgnoreCase("TPS")) reward.addProperty("amount", amount);
		if (item != null) { reward.addProperty("item", item); reward.addProperty("count", count); }
		if (command != null) reward.addProperty("command", command);
		return reward;
	}

	private static JsonObject createPrerequisites(String operator, JsonObject... conditions) {
		JsonObject prereqs = new JsonObject();
		prereqs.addProperty("operator", operator);
		JsonArray condArray = new JsonArray();
		for (JsonObject cond : conditions) condArray.add(cond);
		prereqs.add("conditions", condArray);
		return prereqs;
	}

	private static JsonObject condSagaQuest(String sagaId, int questId) {
		JsonObject cond = new JsonObject();
		cond.addProperty("type", "SAGA_QUEST");
		cond.addProperty("sagaId", sagaId);
		cond.addProperty("questId", questId);
		return cond;
	}

	private static JsonObject condSideQuest(String sideQuestId) {
		JsonObject cond = new JsonObject();
		cond.addProperty("type", "SIDE_QUEST");
		cond.addProperty("sideQuestId", sideQuestId);
		return cond;
	}

	private static JsonObject condLevel(int minLevel) {
		JsonObject cond = new JsonObject();
		cond.addProperty("type", "LEVEL");
		cond.addProperty("minLevel", minLevel);
		return cond;
	}

	private static JsonObject buildQuest(String id, String name, String description, String category,
										 boolean parallel, JsonObject prerequisites,
										 JsonObject[] objectives, JsonObject[] rewards) {
		return buildQuest(id, name, description, category, parallel, prerequisites, objectives, rewards, null, null);
	}

	private static JsonObject buildQuest(String id, String name, String description, String category,
										 boolean parallel, JsonObject prerequisites,
										 JsonObject[] objectives, JsonObject[] rewards,
										 String questGiver, String turnIn) {
		JsonObject quest = new JsonObject();
		quest.addProperty("id", id);
		quest.addProperty("name", name);
		quest.addProperty("description", description);
		quest.addProperty("category", category);
		quest.addProperty("parallelObjectives", parallel);
		if (questGiver != null) quest.addProperty("questGiver", questGiver);
		if (turnIn != null) quest.addProperty("turnIn", turnIn);
		if (prerequisites != null) quest.add("prerequisites", prerequisites);

		JsonArray objArray = new JsonArray();
		for (JsonObject obj : objectives) objArray.add(obj);
		quest.add("objectives", objArray);

		JsonArray rewArray = new JsonArray();
		for (JsonObject rew : rewards) rewArray.add(rew);
		quest.add("rewards", rewArray);
		return quest;
	}

	/* ---- Training Side-Quests ----
	* More made with AI
	* Perdóname IA WAAA */

	private static void createTrainingCategory(Path baseDir) {
		Path dir = baseDir.resolve("training");

		// Quest 1: Roshi's Basic Training — given by Roshi, turn in to Roshi
		writeQuestFile(dir, "roshi_basic_training.json", buildQuest(
				"roshi_basic_training",
				"dmz.sidequest.roshi_basic.name",
				"dmz.sidequest.roshi_basic.desc",
				"training", false, null,
				new JsonObject[]{
						createObjective("STRUCTURE", "dmz.sidequest.roshi_basic.obj1", "dragonminez:roshi_house", null, 0),
						createObjective("KILL", "dmz.sidequest.roshi_basic.obj2", null, "minecraft:zombie", 10),
						createTalkToObjective("dmz.sidequest.roshi_basic.obj3", "roshi")
				},
				new JsonObject[]{
						createReward("TPS", 300, null, 0, null)
				},
				"roshi", "roshi"
		));

		// Quest 2: Endurance Training — given by Roshi
		writeQuestFile(dir, "endurance_training.json", buildQuest(
				"endurance_training",
				"dmz.sidequest.endurance.name",
				"dmz.sidequest.endurance.desc",
				"training", false,
				createPrerequisites("AND", condSideQuest("roshi_basic_training")),
				new JsonObject[]{
						createObjective("KILL", "dmz.sidequest.endurance.obj1", null, "minecraft:skeleton", 15),
						createObjective("KILL", "dmz.sidequest.endurance.obj2", null, "minecraft:spider", 10),
						createTalkToObjective("dmz.sidequest.endurance.obj3", "roshi")
				},
				new JsonObject[]{
						createReward("TPS", 500, null, 0, null)
				},
				"roshi", "roshi"
		));

		// Quest 3: Weighted Training — given by Roshi
		writeQuestFile(dir, "weighted_training.json", buildQuest(
				"weighted_training",
				"dmz.sidequest.weighted.name",
				"dmz.sidequest.weighted.desc",
				"training", false,
				createPrerequisites("AND",
						condSideQuest("endurance_training"),
						condSagaQuest("saiyan_saga", 2)
				),
				new JsonObject[]{
						createObjective("KILL", "dmz.sidequest.weighted.obj1", null, "minecraft:iron_golem", 3),
						createObjective("ITEM", "dmz.sidequest.weighted.obj2", "minecraft:iron_ingot", null, 32),
						createTalkToObjective("dmz.sidequest.weighted.obj3", "roshi")
				},
				new JsonObject[]{
						createReward("TPS", 1000, null, 0, null),
						createReward("ITEM", 0, "minecraft:golden_apple", 5, null)
				},
				"roshi", "roshi"
		));

		// Quest 4: Gravity Chamber — given by Goku
		writeQuestFile(dir, "gravity_chamber.json", buildQuest(
				"gravity_chamber",
				"dmz.sidequest.gravity.name",
				"dmz.sidequest.gravity.desc",
				"training", true,
				createPrerequisites("AND",
						condSideQuest("weighted_training"),
						condLevel(10)
				),
				new JsonObject[]{
						createObjective("KILL", "dmz.sidequest.gravity.obj1", null, "minecraft:wither_skeleton", 5),
						createObjective("KILL", "dmz.sidequest.gravity.obj2", null, "minecraft:blaze", 10),
						createObjective("ITEM", "dmz.sidequest.gravity.obj3", "minecraft:blaze_rod", null, 10),
						createTalkToObjective("dmz.sidequest.gravity.obj4", "goku")
				},
				new JsonObject[]{
						createReward("TPS", 2000, null, 0, null)
				},
				"goku", "goku"
		));
	}

	// ---- Exploration Side-Quests ----

	private static void createExplorationCategory(Path baseDir) {
		Path dir = baseDir.resolve("exploration");

		// Quest 1: World Explorer (no prereqs)
		writeQuestFile(dir, "world_explorer.json", buildQuest(
				"world_explorer",
				"dmz.sidequest.explorer.name",
				"dmz.sidequest.explorer.desc",
				"exploration", true, null,
				new JsonObject[]{
						createObjective("BIOME", "dmz.sidequest.explorer.obj1", "minecraft:plains", null, 0),
						createObjective("BIOME", "dmz.sidequest.explorer.obj2", "minecraft:desert", null, 0),
						createObjective("BIOME", "dmz.sidequest.explorer.obj3", "minecraft:forest", null, 0)
				},
				new JsonObject[]{
						createReward("TPS", 400, null, 0, null)
				}
		));

		// Quest 2: Namek Explorer (requires Saiyan Saga quest 9)
		writeQuestFile(dir, "namek_explorer.json", buildQuest(
				"namek_explorer",
				"dmz.sidequest.namek_explorer.name",
				"dmz.sidequest.namek_explorer.desc",
				"exploration", true,
				createPrerequisites("AND", condSagaQuest("saiyan_saga", 9)),
				new JsonObject[]{
						createObjective("BIOME", "dmz.sidequest.namek_explorer.obj1", "dragonminez:ajissa_plains", null, 0),
						createObjective("STRUCTURE", "dmz.sidequest.namek_explorer.obj2", "dragonminez:village_ajissa", null, 0)
				},
				new JsonObject[]{
						createReward("TPS", 800, null, 0, null)
				}
		));

		// Quest 3: Sacred Lands (requires namek_explorer)
		writeQuestFile(dir, "sacred_lands.json", buildQuest(
				"sacred_lands",
				"dmz.sidequest.sacred_lands.name",
				"dmz.sidequest.sacred_lands.desc",
				"exploration", false,
				createPrerequisites("AND", condSideQuest("namek_explorer")),
				new JsonObject[]{
						createObjective("STRUCTURE", "dmz.sidequest.sacred_lands.obj1", "dragonminez:village_sacred", null, 0)
				},
				new JsonObject[]{
						createReward("TPS", 1200, null, 0, null)
				}
		));
	}

	// ---- Combat Side-Quests ----

	private static void createCombatCategory(Path baseDir) {
		Path dir = baseDir.resolve("combat");

		// Quest 1: Monster Hunter (no prereqs)
		writeQuestFile(dir, "monster_hunter.json", buildQuest(
				"monster_hunter",
				"dmz.sidequest.monster_hunter.name",
				"dmz.sidequest.monster_hunter.desc",
				"combat", true, null,
				new JsonObject[]{
						createObjective("KILL", "dmz.sidequest.monster_hunter.obj1", null, "minecraft:zombie", 20),
						createObjective("KILL", "dmz.sidequest.monster_hunter.obj2", null, "minecraft:skeleton", 20),
						createObjective("KILL", "dmz.sidequest.monster_hunter.obj3", null, "minecraft:creeper", 10)
				},
				new JsonObject[]{
						createReward("TPS", 600, null, 0, null)
				}
		));

		// Quest 2: Nether Warrior (requires monster_hunter + level 5)
		writeQuestFile(dir, "nether_warrior.json", buildQuest(
				"nether_warrior",
				"dmz.sidequest.nether_warrior.name",
				"dmz.sidequest.nether_warrior.desc",
				"combat", false,
				createPrerequisites("AND",
						condSideQuest("monster_hunter"),
						condLevel(5)
				),
				new JsonObject[]{
						createObjective("BIOME", "dmz.sidequest.nether_warrior.obj1", "minecraft:nether_wastes", null, 0),
						createObjective("KILL", "dmz.sidequest.nether_warrior.obj2", null, "minecraft:blaze", 10),
						createObjective("KILL", "dmz.sidequest.nether_warrior.obj3", null, "minecraft:wither_skeleton", 10)
				},
				new JsonObject[]{
						createReward("TPS", 1500, null, 0, null),
						createReward("ITEM", 0, "minecraft:diamond", 3, null)
				}
		));

		// Quest 3: Dragon Ball Hunter (requires Saiyan Saga quest 4 — gather the dragon balls)
		writeQuestFile(dir, "dragon_ball_hunter.json", buildQuest(
				"dragon_ball_hunter",
				"dmz.sidequest.dball_hunter.name",
				"dmz.sidequest.dball_hunter.desc",
				"combat", false,
				createPrerequisites("AND", condSagaQuest("saiyan_saga", 4)),
				new JsonObject[]{
						createObjective("KILL", "dmz.sidequest.dball_hunter.obj1", null, "minecraft:pillager", 15),
						createObjective("STRUCTURE", "dmz.sidequest.dball_hunter.obj2", "minecraft:pillager_outpost", null, 0)
				},
				new JsonObject[]{
						createReward("TPS", 1000, null, 0, null)
				}
		));
	}

	/**
	 * Builds the objective-type index after all side-quests are loaded.
	 * Maps each ObjectiveType to the list of side-quest IDs containing at least one objective of that type.
	 */
	private static void buildObjectiveIndex() {
		OBJECTIVE_INDEX.clear();
		QUEST_GIVER_INDEX.clear();
		TURN_IN_INDEX.clear();

		for (QuestObjective.ObjectiveType type : QuestObjective.ObjectiveType.values()) {
			OBJECTIVE_INDEX.put(type, new ArrayList<>());
		}

		for (SideQuest quest : LOADED_QUESTS.values()) {
			Set<QuestObjective.ObjectiveType> typesInQuest = EnumSet.noneOf(QuestObjective.ObjectiveType.class);
			for (QuestObjective objective : quest.getObjectives()) {
				typesInQuest.add(objective.getType());
			}
			for (QuestObjective.ObjectiveType type : typesInQuest) {
				OBJECTIVE_INDEX.get(type).add(quest.getId());
			}

			// Build quest-giver index
			if (quest.getQuestGiver() != null && !quest.getQuestGiver().isEmpty()) {
				QUEST_GIVER_INDEX.computeIfAbsent(quest.getQuestGiver(), k -> new ArrayList<>()).add(quest.getId());
			}

			// Build turn-in index
			if (quest.getTurnIn() != null && !quest.getTurnIn().isEmpty()) {
				TURN_IN_INDEX.computeIfAbsent(quest.getTurnIn(), k -> new ArrayList<>()).add(quest.getId());
			}
		}
	}

	// ---- Lookups ----

	public static SideQuest getSideQuest(String id) {
		return LOADED_QUESTS.get(id);
	}

	public static Map<String, SideQuest> getAllSideQuests() {
		if (!ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) return Collections.emptyMap();
		return new HashMap<>(LOADED_QUESTS);
	}

	/**
	 * Returns the list of side-quest IDs that contain at least one objective of the given type.
	 * Used by event handlers to avoid iterating all side-quests.
	 */
	public static List<String> getSideQuestIdsByObjectiveType(QuestObjective.ObjectiveType type) {
		return OBJECTIVE_INDEX.getOrDefault(type, Collections.emptyList());
	}

	/**
	 * Returns quest IDs where the given NPC is the quest giver.
	 */
	public static List<String> getQuestIdsByGiver(String npcId) {
		return QUEST_GIVER_INDEX.getOrDefault(npcId, Collections.emptyList());
	}

	/**
	 * Returns quest IDs where the given NPC is the turn-in target.
	 */
	public static List<String> getQuestIdsByTurnIn(String npcId) {
		return TURN_IN_INDEX.getOrDefault(npcId, Collections.emptyList());
	}

	// ---- Client-side ----

	public static Map<String, SideQuest> getClientSideQuests() {
		if (!ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) return Collections.emptyMap();
		return new HashMap<>(CLIENT_QUESTS);
	}

	public static SideQuest getClientSideQuest(String id) {
		return CLIENT_QUESTS.get(id);
	}

	public static void applySyncedSideQuests(Map<String, SideQuest> quests) {
		CLIENT_QUESTS.clear();
		CLIENT_QUESTS.putAll(quests);
		LogUtil.info(Env.CLIENT, "Loaded {} side-quest(s) from server", quests.size());
	}

	// ---- SimplePreparableReloadListener ----

	@Override
	protected @NotNull Map<String, SideQuest> prepare(@NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
		return new HashMap<>(LOADED_QUESTS);
	}

	@Override
	protected void apply(@NotNull Map<String, SideQuest> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
		LOADED_QUESTS.clear();
		LOADED_QUESTS.putAll(pObject);
		buildObjectiveIndex();
	}
}

