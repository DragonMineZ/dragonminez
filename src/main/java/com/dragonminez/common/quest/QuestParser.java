package com.dragonminez.common.quest;

import com.dragonminez.common.quest.objectives.BiomeObjective;
import com.dragonminez.common.quest.objectives.CoordsObjective;
import com.dragonminez.common.quest.objectives.DimensionObjective;
import com.dragonminez.common.quest.objectives.DragonSummonObjective;
import com.dragonminez.common.quest.objectives.InteractObjective;
import com.dragonminez.common.quest.objectives.ItemObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.SkillObjective;
import com.dragonminez.common.quest.objectives.StructureObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.quest.rewards.CommandReward;
import com.dragonminez.common.quest.rewards.AlignmentReward;
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
 * Central parser for the unified quest JSON format.
 */
public class QuestParser {

	/**
	 * Parses a quest from the unified quest JSON format.
	 */
	public static Quest parseQuest(JsonObject json) {
		if (json == null || !json.has("id") || !json.has("title") || !json.has("type")) {
			return null;
		}

		int numericId = -1;
		String stringId = null;
		JsonElement idElement = json.get("id");
		if (idElement.getAsJsonPrimitive().isNumber()) {
			numericId = idElement.getAsInt();
		} else {
			stringId = idElement.getAsString();
		}

		if (numericId == -1 && (stringId == null || stringId.isBlank())) {
			return null;
		}

		Quest.QuestType type;
		try {
			type = Quest.QuestType.valueOf(json.get("type").getAsString().toUpperCase());
		} catch (IllegalArgumentException ignored) {
			return null;
		}

		String title = json.get("title").getAsString();
		String description = json.has("description") ? json.get("description").getAsString() : "";
		String category = json.has("category") ? json.get("category").getAsString() : "general";
		boolean parallelObjectives = json.has("parallel_objectives") && json.get("parallel_objectives").getAsBoolean();
		boolean partyScaling = json.has("party_scaling") && json.get("party_scaling").getAsBoolean();
		String questGiver = json.has("quest_giver") && !json.get("quest_giver").isJsonNull()
				? json.get("quest_giver").getAsString()
				: null;
		String turnIn = json.has("turn_in") && !json.get("turn_in").isJsonNull()
				? json.get("turn_in").getAsString()
				: null;
		boolean secret = json.has("secret") && json.get("secret").getAsBoolean();
		Quest.ClaimMode claimMode = parseClaimMode(json.has("claim_mode") && !json.get("claim_mode").isJsonNull()
				? json.get("claim_mode").getAsString()
				: null);

		QuestPrerequisites prerequisites = parseConditionsBlock(json, "prerequisites");
		QuestPrerequisites startRequirements = parseConditionsBlock(json, "requirements");

		List<QuestObjective> objectives = parseObjectiveList(json);
		List<QuestReward> rewards = parseRewardList(json);

		return new Quest(numericId, stringId, type, title, description, category, parallelObjectives, partyScaling,
				objectives, rewards, prerequisites, startRequirements, questGiver, turnIn, secret, claimMode);
	}

	private static QuestPrerequisites parseConditionsBlock(JsonObject json, String key) {
		if (json.has(key) && json.get(key).isJsonObject()) {
			return parsePrerequisites(json.getAsJsonObject(key));
		}
		return null;
	}

	/**
	 * Parses the {@code objectives} array from a quest JSON object.
	 */
	public static List<QuestObjective> parseObjectiveList(JsonObject questJson) {
		List<QuestObjective> objectives = new ArrayList<>();
		if (!questJson.has("objectives")) {
			return objectives;
		}

		JsonArray objArray = questJson.getAsJsonArray("objectives");
		for (JsonElement element : objArray) {
			QuestObjective obj = parseObjective(element.getAsJsonObject());
			if (obj != null) {
				objectives.add(obj);
			}
		}
		return objectives;
	}

	/**
	 * Parses a single objective from a JSON object.
	 */
	public static QuestObjective parseObjective(JsonObject json) {
		if (json == null || !json.has("type")) {
			return null;
		}

		String type = json.get("type").getAsString();

		return switch (type.toUpperCase()) {
			case "ITEM" -> {
				String itemId = json.get("item").getAsString();
				int count = json.get("count").getAsInt();
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
				yield (item != Items.AIR) ? new ItemObjective(item, count) : null;
			}
			case "KILL" -> {
				String entityId = json.get("entity").getAsString();
				int killCount = json.get("count").getAsInt();
				double health = json.has("health") ? json.get("health").getAsDouble() : 20.0;
				double meleeDamage = json.has("meleeDamage") ? json.get("meleeDamage").getAsDouble() : 1.0;
				double kiDamage = json.has("kiDamage") ? json.get("kiDamage").getAsDouble() : 1.0;
				KillObjective.SpawnMode spawnMode = parseKillSpawnMode(json.has("spawn") && !json.get("spawn").isJsonNull()
						? json.get("spawn").getAsString()
						: null);
				KillObjective.CountMode countMode = parseKillCountMode(json.has("count_mode") && !json.get("count_mode").isJsonNull()
						? json.get("count_mode").getAsString()
						: null);
				int textureVariant = json.has("TextureVariant") && !json.get("TextureVariant").isJsonNull()
						? json.get("TextureVariant").getAsInt()
						: -1;
				yield new KillObjective(entityId, killCount, health, meleeDamage, kiDamage, spawnMode, countMode, textureVariant);
			}
			case "BIOME" -> new BiomeObjective(json.get("biome").getAsString());
			case "DIMENSION" -> new DimensionObjective(json.get("dimension").getAsString());
			case "COORDS" -> {
				int x = json.get("x").getAsInt();
				int y = json.get("y").getAsInt();
				int z = json.get("z").getAsInt();
				int radius = json.has("radius") ? json.get("radius").getAsInt() : 10;
				yield new CoordsObjective(new BlockPos(x, y, z), radius);
			}
			case "INTERACT" -> {
				String interactEntity = json.has("entity") ? json.get("entity").getAsString() : null;
				String entityName = json.has("entityName") ? json.get("entityName").getAsString() : null;
				EntityType<?> interactType = interactEntity != null
						? BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(interactEntity))
						: null;
				yield new InteractObjective(interactType, entityName);
			}
			case "STRUCTURE" -> new StructureObjective(json.get("structure").getAsString());
			case "DRAGON_SUMMON" -> {
				String dragonId = firstString(json, "dragon", "dragon_id", "dragonId");
				String ballSetId = firstString(json, "ball_set", "ballSet", "ball_set_id", "ballSetId", "set");
				yield new DragonSummonObjective(dragonId, ballSetId);
			}
			case "TALK_TO" -> {
				String npcId = json.has("npcId") ? json.get("npcId").getAsString() : null;
				yield npcId != null ? new TalkToObjective(npcId) : null;
			}
			case "SKILL" -> {
				String skill = firstString(json, "skill", "skillId", "id");
				int level = firstInt(json, 1, "level", "minLevel", "required");
				yield skill != null ? new SkillObjective(skill, level) : null;
			}
			default -> null;
		};
	}

	/**
	 * Parses the {@code rewards} array from a quest JSON object.
	 */
	public static List<QuestReward> parseRewardList(JsonObject questJson) {
		List<QuestReward> rewards = new ArrayList<>();
		if (!questJson.has("rewards")) {
			return rewards;
		}

		JsonArray rewardArray = questJson.getAsJsonArray("rewards");
		for (JsonElement element : rewardArray) {
			QuestReward reward = parseReward(element.getAsJsonObject());
			if (reward != null) {
				rewards.add(reward);
			}
		}
		return rewards;
	}

	/**
	 * Parses a single reward from a JSON object.
	 */
	public static QuestReward parseReward(JsonObject json) {
		if (json == null || !json.has("type")) {
			return null;
		}

		String type = json.get("type").getAsString();
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
			case "TPS" -> new TPSReward(json.get("amount").getAsInt());
			case "ALIGNMENT" -> new AlignmentReward(json.get("amount").getAsInt());
			case "COMMAND" -> {
				String command = json.get("command").getAsString();
				JsonElement translationKeyElement = json.get("translationKey");
				yield new CommandReward(command, translationKeyElement != null ? translationKeyElement.getAsString() : null);
			}
			case "SKILL" -> new SkillReward(json.get("skill").getAsString(), json.get("level").getAsInt());
			default -> null;
		};

		if (reward != null) {
			reward.setDifficultyType(difficultyType);
		}
		return reward;
	}

	public static KillObjective.SpawnMode parseKillSpawnMode(String rawMode) {
		return parseEnum(rawMode, KillObjective.SpawnMode.class, KillObjective.SpawnMode.QUEST);
	}

	public static KillObjective.CountMode parseKillCountMode(String rawMode) {
		return parseEnum(rawMode, KillObjective.CountMode.class, KillObjective.CountMode.QUEST_SPAWNED_ONLY);
	}

	public static Quest.ClaimMode parseClaimMode(String rawMode) {
		return parseEnum(rawMode, Quest.ClaimMode.class, Quest.ClaimMode.TREE_OR_NPC);
	}

	private static <T extends Enum<T>> T parseEnum(String rawMode, Class<T> enumClass, T fallback) {
		if (rawMode == null || rawMode.isBlank()) {
			return fallback;
		}

		String normalized = rawMode.trim().toUpperCase().replace('-', '_').replace(' ', '_');
		try {
			return Enum.valueOf(enumClass, normalized);
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}

	private static String firstString(JsonObject json, String... keys) {
		for (String key : keys) {
			if (json.has(key) && !json.get(key).isJsonNull()) {
				String value = json.get(key).getAsString();
				if (value != null && !value.isBlank()) {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * Parses a prerequisites or requirements block.
	 */
	public static QuestPrerequisites parsePrerequisites(JsonObject json) {
		QuestPrerequisites.Operator operator = QuestPrerequisites.Operator.AND;
		if (json.has("operator") && "OR".equalsIgnoreCase(json.get("operator").getAsString())) {
			operator = QuestPrerequisites.Operator.OR;
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

	private static QuestPrerequisites.Condition parseCondition(JsonObject json) {
		if (json.has("operator")) {
			return QuestPrerequisites.Condition.nestedGroup(parsePrerequisites(json));
		}
		if (!json.has("type")) {
			return null;
		}

		String type = json.get("type").getAsString().toUpperCase();
		return switch (type) {
			case "SAGA_QUEST" -> QuestPrerequisites.Condition.sagaQuest(
					json.get("sagaId").getAsString(),
					json.get("questId").getAsInt()
			);
			case "QUEST" -> QuestPrerequisites.Condition.quest(json.get("questId").getAsString());
			case "STAT" -> QuestPrerequisites.Condition.stat(
					json.get("stat").getAsString().toUpperCase(),
					json.get("minValue").getAsInt()
			);
			case "LEVEL" -> QuestPrerequisites.Condition.level(json.get("minLevel").getAsInt());
			case "BIOME" -> QuestPrerequisites.Condition.biome(json.get("biome").getAsString());
			case "STRUCTURE" -> QuestPrerequisites.Condition.structure(
					json.get("structure").getAsString(),
					parseStructureHint(json)
			);
			case "DIMENSION" -> QuestPrerequisites.Condition.dimension(json.get("dimension").getAsString());
			case "TIME" -> {
				QuestPrerequisites.TimeMode timeMode = parseTimeMode(json);
				yield QuestPrerequisites.Condition.time(timeMode, parseTimeDuration(json, timeMode));
			}
			case "ALIGNMENT" -> QuestPrerequisites.Condition.alignment(
					json.has("min") ? json.get("min").getAsInt() : null,
					json.has("max") ? json.get("max").getAsInt() : null
			);
			case "SKILL" -> {
				String skill = firstString(json, "skill", "skillId", "id");
				yield skill != null
						? QuestPrerequisites.Condition.skill(skill, firstInt(json, 1, "minLevel", "level", "required"))
						: null;
			}
			default -> null;
		};
	}

	private static int firstInt(JsonObject json, int fallback, String... keys) {
		for (String key : keys) {
			if (json.has(key) && !json.get(key).isJsonNull()) {
				return json.get(key).getAsInt();
			}
		}
		return fallback;
	}

	private static QuestPrerequisites.StructureHint parseStructureHint(JsonObject json) {
		if (!json.has("hint") || !json.get("hint").isJsonObject()) {
			return null;
		}

		JsonObject hintJson = json.getAsJsonObject("hint");
		String dimensionId = hintJson.has("dimension") && !hintJson.get("dimension").isJsonNull()
				? hintJson.get("dimension").getAsString()
				: null;
		Integer x = hintJson.has("x") ? hintJson.get("x").getAsInt() : null;
		Integer y = hintJson.has("y") ? hintJson.get("y").getAsInt() : null;
		Integer z = hintJson.has("z") ? hintJson.get("z").getAsInt() : null;
		if (dimensionId == null && x == null && y == null && z == null) {
			return null;
		}
		return new QuestPrerequisites.StructureHint(dimensionId, x, y, z);
	}

	private static QuestPrerequisites.TimeMode parseTimeMode(JsonObject json) {
		String rawMode = json.has("mode") ? json.get("mode").getAsString() : "GAME_TIME";
		if (rawMode == null) {
			return QuestPrerequisites.TimeMode.GAME_TIME;
		}

		String normalized = rawMode.trim().toUpperCase().replace('-', '_').replace(' ', '_');

		try {
			return QuestPrerequisites.TimeMode.valueOf(normalized);
		} catch (IllegalArgumentException ignored) {
			return QuestPrerequisites.TimeMode.GAME_TIME;
		}
	}

	private static long parseTimeDuration(JsonObject json, QuestPrerequisites.TimeMode mode) {
		return switch (mode) {
			case GAME_TIME -> json.has("ticks") ? Math.max(0L, json.get("ticks").getAsLong()) : 0L;
			case REAL_TIME -> json.has("milliseconds") ? Math.max(0L, json.get("milliseconds").getAsLong()) : 0L;
		};
	}
}
