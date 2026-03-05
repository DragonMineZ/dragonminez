package com.dragonminez.common.quest;

import com.dragonminez.common.quest.objectives.*;
import com.dragonminez.common.quest.rewards.CommandReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Central JSON parser for all quest types (saga quests and side-quests).
 * <p>
 * <b>Saga quests</b> are parsed via {@link #parseSagaQuest(JsonObject)} from saga definition files.
 * <b>Side-quests</b> are parsed via {@link #parseSideQuest(JsonObject)} from world-folder JSON files.
 * <p>
 * Both types share the same {@link #parseObjective(JsonObject)} and {@link #parseReward(JsonObject)}
 * methods for their objectives and rewards.
 *
 * @since 2.0
 */
public class QuestParser {

	// ========================================================================================
	// Quest Parsing — Unified Format (new individual quest files)
	// ========================================================================================

	/**
	 * Parses a quest from a JSON file used by individual quest files.
	 * <p>
	 * This is the format for files in {@code dragonminez/quests/}. It supports all quest types
	 * and optional chain membership. The {@code "type"} field determines the quest type.
	 * If {@code "type"} is absent, it defaults to {@code SIDEQUEST}.
	 * <p>
	 * Expected JSON format:
	 * <pre>{@code
	 * {
	 *   "id": "unique_string_id",
	 *   "title": "translation.key.or.literal",
	 *   "description": "translation.key.or.literal",
	 *   "type": "SAGA",
	 *   "category": "saga_saiyan",
	 *   "parallelObjectives": false,
	 *   "questGiver": null,
	 *   "turnIn": null,
	 *   "chain": {
	 *     "saga": "saiyan_saga",
	 *     "order": 1,
	 *     "next": "saga_saiyan_train_with_king_kai"
	 *   },
	 *   "prerequisites": { ... },
	 *   "objectives": [ ... ],
	 *   "rewards": [ ... ]
	 * }
	 * }</pre>
	 *
	 * @param json the JSON object from an individual quest file
	 * @return the parsed {@link Quest}, or {@code null} if parsing fails
	 */
	public static Quest parseQuest(JsonObject json) {
		// ID — can be numeric (saga quests) or string (sidequests)
		int numericId = -1;
		String stringId = null;
		if (json.has("id")) {
			JsonElement idElement = json.get("id");
			if (idElement.getAsJsonPrimitive().isNumber()) {
				numericId = idElement.getAsInt();
			} else {
				stringId = idElement.getAsString();
			}
		}
		if (numericId == -1 && stringId == null) return null;

		// Display name — support "title" and "name" keys
		String title = json.has("title") ? json.get("title").getAsString()
				: (json.has("name") ? json.get("name").getAsString() : (stringId != null ? stringId : String.valueOf(numericId)));

		String description = json.has("description") ? json.get("description").getAsString() : "";

		// Quest type — defaults to SIDEQUEST if absent
		Quest.QuestType type = Quest.QuestType.SIDEQUEST;
		if (json.has("type")) {
			try {
				type = Quest.QuestType.valueOf(json.get("type").getAsString().toUpperCase());
			} catch (IllegalArgumentException ignored) {
				// Unknown type, default to SIDEQUEST
			}
		}

		String category = json.has("category") ? json.get("category").getAsString() : "general";

		// Support both snake_case and camelCase field names
		boolean parallelObjectives = getBool(json, "parallel_objectives", "parallelObjectives");
		String questGiver = getStr(json, "quest_giver", "questGiver");
		String turnIn = getStr(json, "turn_in", "turnIn");

		// Chain (optional — if present, this quest is part of a saga chain)
		String sagaId = null;
		int chainOrder = -1;
		String nextQuestId = null;
		if (json.has("chain")) {
			JsonObject chain = json.getAsJsonObject("chain");
			sagaId = chain.has("saga") ? chain.get("saga").getAsString() : null;
			chainOrder = chain.has("order") ? chain.get("order").getAsInt() : -1;
			nextQuestId = chain.has("next") && !chain.get("next").isJsonNull()
					? chain.get("next").getAsString() : null;
		}

		// Prerequisites (optional)
		QuestPrerequisites prerequisites = null;
		if (json.has("prerequisites")) {
			prerequisites = parsePrerequisites(json.getAsJsonObject("prerequisites"));
		}

		List<QuestObjective> objectives = parseObjectiveList(json);
		List<QuestReward> rewards = parseRewardList(json);

		return new Quest(numericId, stringId, type, title, description, category, parallelObjectives,
				objectives, rewards, prerequisites, questGiver, turnIn,
				sagaId, chainOrder, nextQuestId);
	}

	/** Gets a boolean from JSON, trying snake_case key first, then camelCase key. */
	private static boolean getBool(JsonObject json, String snakeKey, String camelKey) {
		if (json.has(snakeKey)) return json.get(snakeKey).getAsBoolean();
		if (json.has(camelKey)) return json.get(camelKey).getAsBoolean();
		return false;
	}

	/** Gets a string from JSON, trying snake_case key first, then camelCase key. Returns null if absent. */
	private static String getStr(JsonObject json, String snakeKey, String camelKey) {
		if (json.has(snakeKey) && !json.get(snakeKey).isJsonNull()) return json.get(snakeKey).getAsString();
		if (json.has(camelKey) && !json.get(camelKey).isJsonNull()) return json.get(camelKey).getAsString();
		return null;
	}

	// ========================================================================================
	// Quest Parsing — Legacy Formats
	// ========================================================================================

	/**
	 * Parses a saga quest from a JSON object.
	 * <p>
	 * Expected JSON format:
	 * <pre>{@code
	 * {
	 *   "id": 1,
	 *   "title": "translation.key.or.literal",
	 *   "description": "translation.key.or.literal",
	 *   "objectives": [ ... ],
	 *   "rewards": [ ... ]
	 * }
	 * }</pre>
	 *
	 * @param json the JSON object representing the saga quest
	 * @return a {@link Quest} with {@link Quest.QuestType#SAGA}
	 */
	public static Quest parseSagaQuest(JsonObject json) {
		int id = json.get("id").getAsInt();
		String title = json.get("title").getAsString();
		String description = json.get("description").getAsString();

		List<QuestObjective> objectives = parseObjectiveList(json);
		List<QuestReward> rewards = parseRewardList(json);

		return new Quest(id, title, description, objectives, rewards);
	}

	/**
	 * Parses a side-quest from a JSON object (data-driven, loaded from world folder).
	 * <p>
	 * Supports both {@code "name"} and {@code "title"} keys for the display name.
	 * Expected JSON format:
	 * <pre>{@code
	 * {
	 *   "id": "unique_string_id",
	 *   "name": "translation.key.or.literal",
	 *   "description": "translation.key.or.literal",
	 *   "category": "training",
	 *   "parallelObjectives": false,
	 *   "questGiver": "npc_id",
	 *   "turnIn": "npc_id",
	 *   "prerequisites": { ... },
	 *   "objectives": [ ... ],
	 *   "rewards": [ ... ]
	 * }
	 * }</pre>
	 *
	 * @param json the JSON object representing the side-quest
	 * @return a {@link Quest} with {@link Quest.QuestType#SIDEQUEST}
	 */
	public static Quest parseSideQuest(JsonObject json) {
		String id = json.get("id").getAsString();

		// Support both "name" and "title" keys for backward compatibility
		String name = json.has("name") ? json.get("name").getAsString()
				: (json.has("title") ? json.get("title").getAsString() : id);

		String description = json.get("description").getAsString();
		String category = json.has("category") ? json.get("category").getAsString() : "general";
		boolean parallelObjectives = json.has("parallelObjectives") && json.get("parallelObjectives").getAsBoolean();
		String questGiver = json.has("questGiver") ? json.get("questGiver").getAsString() : null;
		String turnIn = json.has("turnIn") ? json.get("turnIn").getAsString() : null;

		QuestPrerequisites prerequisites = null;
		if (json.has("prerequisites")) {
			prerequisites = parsePrerequisites(json.getAsJsonObject("prerequisites"));
		}

		List<QuestObjective> objectives = parseObjectiveList(json);
		List<QuestReward> rewards = parseRewardList(json);

		return new Quest(id, name, description, category, parallelObjectives, objectives, rewards,
				prerequisites, questGiver, turnIn);
	}


	// ========================================================================================
	// Objective Parsing
	// ========================================================================================

	/**
	 * Parses an {@code "objectives"} JSON array from a quest JSON object.
	 *
	 * @param questJson the parent quest JSON containing an "objectives" array
	 * @return list of parsed objectives (never null)
	 */
	public static List<QuestObjective> parseObjectiveList(JsonObject questJson) {
		List<QuestObjective> objectives = new ArrayList<>();
		if (questJson.has("objectives")) {
			JsonArray objArray = questJson.getAsJsonArray("objectives");
			for (JsonElement element : objArray) {
				QuestObjective obj = parseObjective(element.getAsJsonObject());
				if (obj != null) {
					objectives.add(obj);
				}
			}
		}
		return objectives;
	}

	/**
	 * Parses a single objective from a JSON object.
	 * <p>
	 * Supported types: {@code ITEM}, {@code KILL}, {@code BIOME}, {@code COORDS},
	 * {@code INTERACT}, {@code STRUCTURE}, {@code TALK_TO}.
	 *
	 * @param json the JSON object representing the objective
	 * @return the parsed {@link QuestObjective}, or {@code null} if the type is unknown
	 */
	public static QuestObjective parseObjective(JsonObject json) {
		String type = json.get("type").getAsString();
		String description = json.get("description").getAsString();

		return switch (type.toUpperCase()) {
			case "ITEM" -> {
				String itemId = json.get("item").getAsString();
				int count = json.get("count").getAsInt();
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
				yield (item != Items.AIR) ? new ItemObjective(description, item, count) : null;
			}
			case "KILL" -> {
				String entityId = json.get("entity").getAsString();
				int killCount = json.get("count").getAsInt();
				EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
				double health = json.has("health") ? json.get("health").getAsDouble() : 20.0;
				double meleeDamage = json.has("meleeDamage") ? json.get("meleeDamage").getAsDouble() : 1.0;
				double kiDamage = json.has("kiDamage") ? json.get("kiDamage").getAsDouble() : 1.0;
				yield new KillObjective(description, entityType, killCount, health, meleeDamage, kiDamage);
			}
			case "BIOME" -> {
				String biomeId = json.get("biome").getAsString();
				yield new BiomeObjective(description, biomeId);
			}
			case "COORDS" -> {
				int x = json.get("x").getAsInt();
				int y = json.get("y").getAsInt();
				int z = json.get("z").getAsInt();
				int radius = json.has("radius") ? json.get("radius").getAsInt() : 10;
				yield new CoordsObjective(description, new BlockPos(x, y, z), radius);
			}
			case "INTERACT" -> {
				String interactEntity = json.has("entity") ? json.get("entity").getAsString() : null;
				String entityName = json.has("entityName") ? json.get("entityName").getAsString() : null;
				EntityType<?> interactType = interactEntity != null
						? BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(interactEntity)) : null;
				yield new InteractObjective(description, interactType, entityName);
			}
			case "STRUCTURE" -> {
				String structureId = json.get("structure").getAsString();
				yield new StructureObjective(description, structureId);
			}
			case "TALK_TO" -> {
				String npcId = json.has("npcId") ? json.get("npcId").getAsString() : null;
				yield (npcId != null) ? new TalkToObjective(description, npcId) : null;
			}
			default -> null;
		};
	}

	// ========================================================================================
	// Reward Parsing
	// ========================================================================================

	/**
	 * Parses a {@code "rewards"} JSON array from a quest JSON object.
	 *
	 * @param questJson the parent quest JSON containing a "rewards" array
	 * @return list of parsed rewards (never null)
	 */
	public static List<QuestReward> parseRewardList(JsonObject questJson) {
		List<QuestReward> rewards = new ArrayList<>();
		if (questJson.has("rewards")) {
			JsonArray rewardArray = questJson.getAsJsonArray("rewards");
			for (JsonElement element : rewardArray) {
				QuestReward reward = parseReward(element.getAsJsonObject());
				if (reward != null) {
					rewards.add(reward);
				}
			}
		}
		return rewards;
	}

	/**
	 * Parses a single reward from a JSON object.
	 * <p>
	 * Supported types: {@code ITEM}, {@code TPS}, {@code COMMAND}, {@code SKILL}.
	 * Type strings may be prefixed with {@code "hard:"} or {@code "normal:"} for difficulty filtering.
	 *
	 * @param json the JSON object representing the reward
	 * @return the parsed {@link QuestReward}, or {@code null} if the type is unknown
	 */
	public static QuestReward parseReward(JsonObject json) {
		String type = json.get("type").getAsString();

		// Parse optional difficulty prefix (e.g. "hard:ITEM", "normal:TPS")
		QuestReward.DifficultyType difficultyType = QuestReward.DifficultyType.ALL;
		if (type.toLowerCase().startsWith("hard:")) {
			difficultyType = QuestReward.DifficultyType.HARD;
			type = type.substring(5);
		} else if (type.toLowerCase().startsWith("normal:")) {
			difficultyType = QuestReward.DifficultyType.NORMAL;
			type = type.substring(7);
		}

		QuestReward reward = switch (type.toUpperCase()) {
			case "ITEM" -> {
				String itemId = json.get("item").getAsString();
				int count = json.has("count") ? json.get("count").getAsInt() : 1;
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
				yield (item != Items.AIR) ? new ItemReward(new ItemStack(item, count)) : null;
			}
			case "TPS" -> {
				int amount = json.get("amount").getAsInt();
				yield new TPSReward(amount);
			}
			case "COMMAND" -> {
				String command = json.get("command").getAsString();
				JsonElement translationKeyElement = json.get("translationKey");
				yield new CommandReward(command, translationKeyElement != null ? translationKeyElement.getAsString() : null);
			}
			case "SKILL" -> {
				String skill = json.get("skill").getAsString();
				int level = json.get("level").getAsInt();
				yield new SkillReward(skill, level);
			}
			default -> null;
		};

		if (reward != null) {
			reward.setDifficultyType(difficultyType);
		}
		return reward;
	}

	// ========================================================================================
	// Prerequisites Parsing
	// ========================================================================================

	/**
	 * Parses a prerequisites block from a JSON object.
	 * <p>
	 * Expected format example:
	 * <pre>{@code
	 * {
	 *   "operator": "AND", // Can be "AND" or "OR"
	 *   "conditions": [
	 *     { "type": "SAGA_QUEST", "sagaId": "saiyan_saga", "questId": 2 },
	 *     { "type": "QUEST", "questId": "some_sidequest_id" },
	 *     { "type": "LEVEL", "minLevel": 10 },
	 *     { "type": "STAT", "stat": "STR", "minValue": 100 },
	 *     { "type": "RACE", "race": "saiyan" },
	 *     { "operator": "OR", "conditions": [ ... ] }
	 *   ]
	 * }
	 * }</pre>
	 *
	 * @param json the prerequisites JSON object
	 * @return the parsed {@link QuestPrerequisites}
	 */
	public static QuestPrerequisites parsePrerequisites(JsonObject json) {
		QuestPrerequisites.Operator operator = QuestPrerequisites.Operator.AND;
		if (json.has("operator")) {
			String op = json.get("operator").getAsString().toUpperCase();
			if (op.equals("OR")) {
				operator = QuestPrerequisites.Operator.OR;
			}
		}

		List<QuestPrerequisites.Condition> conditions = new ArrayList<>();
		if (json.has("conditions")) {
			JsonArray condArray = json.getAsJsonArray("conditions");
			for (JsonElement element : condArray) {
				QuestPrerequisites.Condition condition = parseCondition(element.getAsJsonObject());
				if (condition != null) {
					conditions.add(condition);
				}
			}
		}

		return new QuestPrerequisites(operator, conditions);
	}

	/**
	 * Parses a single prerequisite condition from a JSON object.
	 * If the JSON contains an {@code "operator"} key, it is treated as a nested group.
	 */
	private static QuestPrerequisites.Condition parseCondition(JsonObject json) {
		// Nested group (has "operator" instead of "type")
		if (json.has("operator")) {
			QuestPrerequisites nested = parsePrerequisites(json);
			return QuestPrerequisites.Condition.nestedGroup(nested);
		}

		if (!json.has("type")) return null;
		String type = json.get("type").getAsString().toUpperCase();

		return switch (type) {
			case "SAGA_QUEST" -> {
				String sagaId = json.get("sagaId").getAsString();
				int questId = json.get("questId").getAsInt();
				yield QuestPrerequisites.Condition.sagaQuest(sagaId, questId);
			}
			// Support both "SIDE_QUEST" (legacy JSON) and "QUEST" (new format)
			case "SIDE_QUEST", "QUEST" -> {
				String reqId = json.has("sideQuestId") ? json.get("sideQuestId").getAsString()
						: json.get("questId").getAsString();
				yield QuestPrerequisites.Condition.quest(reqId);
			}
			case "STAT" -> {
				String stat = json.get("stat").getAsString().toUpperCase();
				int minValue = json.get("minValue").getAsInt();
				yield QuestPrerequisites.Condition.stat(stat, minValue);
			}
			case "RACE" -> {
				String race = json.get("race").getAsString().toLowerCase();
				yield QuestPrerequisites.Condition.race(race);
			}
			case "LEVEL" -> {
				int minLevel = json.get("minLevel").getAsInt();
				yield QuestPrerequisites.Condition.level(minLevel);
			}
			default -> null;
		};
	}
}
