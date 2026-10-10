package com.dragonminez.common.quest;

import com.dragonminez.common.quest.objectives.BiomeObjective;
import com.dragonminez.common.quest.objectives.CheckpointRaceObjective;
import com.dragonminez.common.quest.objectives.CoordsObjective;
import com.dragonminez.common.quest.objectives.DeliverObjective;
import com.dragonminez.common.quest.objectives.EscortObjective;
import com.dragonminez.common.quest.objectives.SparObjective;
import com.dragonminez.common.quest.objectives.SurviveWavesObjective;
import com.dragonminez.common.quest.objectives.DimensionObjective;
import com.dragonminez.common.quest.objectives.DragonSummonObjective;
import com.dragonminez.common.quest.objectives.InteractObjective;
import com.dragonminez.common.quest.objectives.ItemObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.SkillObjective;
import com.dragonminez.common.quest.objectives.StructureObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.quest.objectives.PlaceholderObjective;
import com.dragonminez.common.diagnostics.JsonKeys;
import com.dragonminez.common.quest.rewards.CommandReward;
import com.dragonminez.common.quest.rewards.AlignmentReward;
import com.dragonminez.common.quest.rewards.GenericItemReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.KiTechniqueReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.AttributePointsReward;
import com.dragonminez.common.quest.rewards.AttributesReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.dragonminez.common.quest.rewards.TransformationReward;
import com.dragonminez.common.quest.rewards.PlaceholderReward;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.util.gson.GsonUtils;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Central parser for the unified quest JSON format.
 */
public class QuestParser {
	/**
	 * Parses a quest from the unified quest JSON format.
	 */
	public static Quest parseQuest(JsonObject json) {
		if (rejectionReason(json) != null) {
			return null;
		}

		int numericId = -1;
		String stringId = null;
		JsonPrimitive idElement = json.getAsJsonPrimitive("id");
		if (idElement.isNumber()) {
			numericId = idElement.getAsInt();
		} else {
			stringId = idElement.getAsString();
		}

		Quest.QuestType type = parseQuestType(json.get("type").getAsString());

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
		Quest.ClaimMode claimMode = parseClaimMode(rawClaimMode(json));

		QuestPrerequisites prerequisites = parseConditionsBlock(json, "prerequisites");
		QuestPrerequisites startRequirements = parseConditionsBlock(json, "requirements");

		List<QuestObjective> objectives = parseObjectiveList(json);
		List<QuestReward> rewards = parseRewardList(json);

		Quest quest = new Quest(numericId, stringId, type, title, description, category, parallelObjectives, partyScaling,
				objectives, rewards, prerequisites, startRequirements, questGiver, turnIn, secret, claimMode);

		boolean repeatable = json.has("repeatable") && json.get("repeatable").getAsBoolean();
		int repeatCooldownSeconds = json.has("repeat_cooldown_seconds") ? json.get("repeat_cooldown_seconds").getAsInt() : 0;
		quest.setRepeatConfig(repeatable, repeatCooldownSeconds);
		quest.setTimeLimitSeconds(json.has("time_limit_seconds") ? json.get("time_limit_seconds").getAsInt() : 0);

		return quest;
	}

	public static String rejectionReason(JsonObject json) {
		if (json == null) {
			return "the file is empty or is not a JSON object";
		}
		for (String key : new String[]{"id", "title", "type"}) {
			if (!json.has(key) || json.get(key).isJsonNull()) {
				return "missing required field '" + key + "'";
			}
			if (!json.get(key).isJsonPrimitive()) {
				return "field '" + key + "' must be a plain value, not an object or a list";
			}
		}
		JsonPrimitive id = json.getAsJsonPrimitive("id");
		if (id.isNumber()) {
			if (id.getAsInt() == -1) {
				return "'id' -1 is reserved, use another id";
			}
		} else if (id.getAsString().isBlank()) {
			return "'id' is blank";
		}
		String rawType = json.get("type").getAsString();
		if (parseQuestType(rawType) == null) {
			return "unknown quest type '" + rawType + "' (expected SAGA, SIDEQUEST, DAILY or EVENT)";
		}
		return null;
	}

	public static List<String> describeIssues(JsonObject json, Quest quest) {
		List<String> issues = new ArrayList<>();
		if (json == null || quest == null) {
			return issues;
		}

		List<QuestObjective> objectives = quest.getObjectives();
		for (int i = 0; i < objectives.size(); i++) {
			if (objectives.get(i) instanceof PlaceholderObjective placeholder && placeholder.isTypeKnown()) {
				issues.add("objectives[" + i + "]" + typeLabel(placeholder.getOriginalType()) + " ignored: "
						+ placeholder.getReason() + " (it counts as already done)");
			}
		}

		List<QuestReward> rewards = quest.getRewards();
		for (int i = 0; i < rewards.size(); i++) {
			if (rewards.get(i) instanceof PlaceholderReward placeholder && placeholder.isTypeKnown()) {
				issues.add("rewards[" + i + "]" + typeLabel(placeholder.getOriginalType()) + " ignored: "
						+ placeholder.getReason() + " (it can never be claimed)");
			}
		}

		if (parseClaimMode(rawClaimMode(json)) == Quest.ClaimMode.NPC_ONLY && quest.getClaimMode() != Quest.ClaimMode.NPC_ONLY) {
			issues.add("claim_mode NPC_ONLY needs a 'turn_in' NPC; using TREE_OR_NPC so the rewards can still be claimed");
		}
		return issues;
	}

	private static String typeLabel(String type) {
		return type == null || type.isBlank() ? "" : " (" + type + ")";
	}

	private static String rawClaimMode(JsonObject json) {
		return json.has("claim_mode") && !json.get("claim_mode").isJsonNull()
				? json.get("claim_mode").getAsString()
				: null;
	}

	private static Quest.QuestType parseQuestType(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Quest.QuestType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
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
			objectives.add(element != null && element.isJsonObject()
					? parseObjectiveOrPlaceholder(element.getAsJsonObject())
					: new PlaceholderObjective(null, "the entry is not a JSON object", true));
		}
		return objectives;
	}

	/**
	 * Parses a single objective from a JSON object.
	 */
	public static QuestObjective parseObjective(JsonObject json) {
		QuestObjective objective = parseObjectiveOrPlaceholder(json);
		return objective.isPlaceholder() ? null : objective;
	}

	public static QuestObjective parseObjectiveOrPlaceholder(JsonObject json) {
		try {
			return parseObjectiveUnchecked(json);
		} catch (RuntimeException e) {
			return new PlaceholderObjective(rawType(json), "invalid entry (" + e + ")", true);
		}
	}

	private static QuestObjective parseObjectiveUnchecked(JsonObject json) {
		if (json == null) {
			return new PlaceholderObjective(null, "the entry is not a JSON object", true);
		}
		if (!json.has("type") || json.get("type").isJsonNull()) {
			return new PlaceholderObjective(null, "missing 'type'", false);
		}

		String type = json.get("type").getAsString();

		return switch (type.trim().toUpperCase(Locale.ROOT)) {
			case "ITEM" -> {
				String itemId = json.get("item").getAsString();
				int count = json.get("count").getAsInt();
				Item item = resolveItem(itemId);
				yield item != null ? new ItemObjective(item, count) : invalidObjective(type, "item '" + itemId + "' does not exist");
			}
			case "KILL", "SPAR" -> {
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
				int aiTier = json.has("AITier") && !json.get("AITier").isJsonNull()
						? json.get("AITier").getAsInt()
						: -1;
				String transformKey = json.has("canTransform") ? "canTransform" : "CanTransform";
				boolean canTransform = !json.has(transformKey) || json.get(transformKey).isJsonNull()
						|| json.get(transformKey).getAsBoolean();
				Double transformHealth = getNullableDouble(json, "TransformHealth");
				Double transformMeleeDamage = getNullableDouble(json, "TransformMeleeDamage");
				Double transformKiDamage = getNullableDouble(json, "TransformKiDamage");
				Double transformHealthMultiplier = getNullableDouble(json, "TransformHealthMultiplier");
				Double transformMeleeMultiplier = getNullableDouble(json, "TransformMeleeDamageMultiplier");
				Double transformKiMultiplier = getNullableDouble(json, "TransformKiMultiplier");
				Double transformTriggerPercent = getNullableDouble(json, "TransformTriggerPercent");
				yield "SPAR".equalsIgnoreCase(type.trim())
						? new SparObjective(entityId, killCount, health, meleeDamage, kiDamage, spawnMode, countMode, textureVariant, aiTier, canTransform)
						: new KillObjective(entityId, killCount, health, meleeDamage, kiDamage, spawnMode, countMode,
								textureVariant, aiTier, canTransform, transformHealth, transformMeleeDamage, transformKiDamage,
								transformHealthMultiplier, transformMeleeMultiplier, transformKiMultiplier, transformTriggerPercent);
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
				yield npcId != null ? new TalkToObjective(npcId) : invalidObjective(type, "missing 'npcId'");
			}
			case "SKILL" -> {
				String skill = firstString(json, "skill", "skillId", "id");
				int level = firstInt(json, 1, "level", "minLevel", "required");
				yield skill != null ? new SkillObjective(skill, level) : invalidObjective(type, "missing 'skill'");
			}
			case "DELIVER" -> {
				String itemId = json.get("item").getAsString();
				int count = json.has("count") ? json.get("count").getAsInt() : 1;
				String npcId = firstString(json, "npcId", "npc_id", "npc");
				Item item = resolveItem(itemId);
				if (item == null) yield invalidObjective(type, "item '" + itemId + "' does not exist");
				yield npcId != null ? new DeliverObjective(item, itemId, count, npcId) : invalidObjective(type, "missing 'npcId'");
			}
			case "SURVIVE_WAVES" -> {
				String entityId = json.get("entity").getAsString();
				int waves = json.has("waves") ? json.get("waves").getAsInt() : 3;
				int mobsPerWave = firstInt(json, 3, "mobs_per_wave", "mobsPerWave");
				int waveDelaySeconds = firstInt(json, 10, "wave_delay_seconds", "waveDelaySeconds");
				double health = json.has("health") ? json.get("health").getAsDouble() : 20.0;
				double meleeDamage = json.has("meleeDamage") ? json.get("meleeDamage").getAsDouble() : 1.0;
				double kiDamage = json.has("kiDamage") ? json.get("kiDamage").getAsDouble() : 1.0;
				int textureVariant = json.has("TextureVariant") && !json.get("TextureVariant").isJsonNull()
						? json.get("TextureVariant").getAsInt() : -1;
				int aiTier = json.has("AITier") && !json.get("AITier").isJsonNull()
						? json.get("AITier").getAsInt() : -1;
				boolean canTransform = !json.has("CanTransform") || json.get("CanTransform").isJsonNull()
						|| json.get("CanTransform").getAsBoolean();
				yield new SurviveWavesObjective(entityId, waves, mobsPerWave, waveDelaySeconds,
						health, meleeDamage, kiDamage, textureVariant, aiTier, canTransform);
			}
			case "ESCORT" -> {
				String entityId = json.get("entity").getAsString();
				BlockPos targetPos = new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
				int radius = json.has("radius") ? json.get("radius").getAsInt() : 6;
				double health = json.has("health") ? json.get("health").getAsDouble() : 0.0;
				yield new EscortObjective(entityId, targetPos, radius, health, firstString(json, "dimension"));
			}
			case "CHECKPOINT_RACE" -> {
				int radius = json.has("radius") ? json.get("radius").getAsInt() : 8;
				List<BlockPos> checkpoints = new ArrayList<>();
				if (json.has("checkpoints") && json.get("checkpoints").isJsonArray()) {
					for (JsonElement element : json.getAsJsonArray("checkpoints")) {
						if (!element.isJsonObject()) continue;
						JsonObject point = element.getAsJsonObject();
						checkpoints.add(new BlockPos(point.get("x").getAsInt(), point.get("y").getAsInt(), point.get("z").getAsInt()));
					}
				}
				yield checkpoints.isEmpty()
						? invalidObjective(type, "it has no checkpoints")
						: new CheckpointRaceObjective(checkpoints, radius, firstString(json, "dimension"));
			}
			default -> {
				if (!QuestObjectiveRegistry.isRegistered(type)) {
					yield new PlaceholderObjective(type, "unknown objective type '" + type + "'", false);
				}
				QuestObjective parsed = QuestObjectiveRegistry.parse(type, json);
				yield parsed != null ? parsed : invalidObjective(type, "the registered objective type failed to parse it");
			}
		};
	}

	private static QuestObjective invalidObjective(String type, String reason) {
		return new PlaceholderObjective(type, reason, true);
	}

	private static Item resolveItem(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return null;
		}
		try {
			Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
			return item != Items.AIR ? item : null;
		} catch (RuntimeException ignored) {
			return null;
		}
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
			rewards.add(element != null && element.isJsonObject()
					? parseRewardOrPlaceholder(element.getAsJsonObject())
					: new PlaceholderReward(null, "the entry is not a JSON object", true));
		}
		return rewards;
	}

	/**
	 * Parses a single reward from a JSON object.
	 */
	public static QuestReward parseReward(JsonObject json) {
		QuestReward reward = parseRewardOrPlaceholder(json);
		return reward.isPlaceholder() ? null : reward;
	}

	public static QuestReward parseRewardOrPlaceholder(JsonObject json) {
		try {
			return parseRewardUnchecked(json);
		} catch (RuntimeException e) {
			return new PlaceholderReward(rawType(json), "invalid entry (" + e + ")", true);
		}
	}

	private static String rawType(JsonObject json) {
		try {
			return json != null && json.has("type") && json.get("type").isJsonPrimitive() ? json.get("type").getAsString() : null;
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static QuestReward parseRewardUnchecked(JsonObject json) {
		if (json == null) {
			return new PlaceholderReward(null, "the entry is not a JSON object", true);
		}
		if (!json.has("type") || json.get("type").isJsonNull()) {
			return new PlaceholderReward(null, "missing 'type'", false);
		}

		String type = json.get("type").getAsString();
		Set<Difficulty> difficulties = parseRewardDifficulties(json);

		// Pre-2.2 files encoded reward difficulty as a "hard:"/"normal:" prefix on the type.
		boolean explicitDifficulty = firstElement(json, "difficulty", "difficulties", "difficultyType", "minDifficulty") != null;
		if (type.toLowerCase(Locale.ROOT).startsWith("hard:")) {
			type = type.substring("hard:".length());
			if (!explicitDifficulty) difficulties = EnumSet.of(Difficulty.HARD);
		} else if (type.toLowerCase(Locale.ROOT).startsWith("normal:")) {
			type = type.substring("normal:".length());
			if (!explicitDifficulty) difficulties = EnumSet.of(Difficulty.EASY, Difficulty.NORMAL);
		}

		QuestReward reward = switch (type.trim().toUpperCase(Locale.ROOT)) {
			case "ITEM" -> {
				String itemId = json.get("item").getAsString();
				int count = json.has("count") ? json.get("count").getAsInt() : 1;
				Item item = resolveItem(itemId);
				yield item != null ? new ItemReward(new ItemStack(item, count)) : invalidReward(type, "item '" + itemId + "' does not exist");
			}
			case "GENERIC_ITEM" -> {
				GenericItemDTO genericItem = GsonUtils.GSON.fromJson(
						json.getAsJsonObject("itemReward"),
						GenericItemDTO.class
				);
				yield genericItem != null ? new GenericItemReward(genericItem) : invalidReward(type, "missing 'itemReward'");
			}
			case "TPS" -> new TPSReward(json.get("amount").getAsInt());
			case "ATTRIBUTES" -> new AttributesReward(json.get("amount").getAsInt());
			case "ATTRIBUTE_POINTS" -> new AttributePointsReward(json.get("amount").getAsInt());
			case "ALIGNMENT" -> new AlignmentReward(json.get("amount").getAsInt());
			case "COMMAND" -> {
				String command = json.get("command").getAsString();
				JsonElement translationKeyElement = json.get("translationKey");
				yield new CommandReward(command, translationKeyElement != null ? translationKeyElement.getAsString() : null);
			}
			case "SKILL" -> new SkillReward(json.get("skill").getAsString(), json.get("level").getAsInt());
			case "TRANSFORMATION" -> {
				String formGroup = firstString(json, "formGroup", "form_group", "group");
				String formName = firstString(json, "formName", "form_name", "form");
				if (formGroup == null || formName == null) yield invalidReward(type, "missing 'formGroup' or 'formName'");
				double mastery = json.has("mastery") ? json.get("mastery").getAsDouble() : 100.0;
				boolean stack = json.has("stack") && json.get("stack").getAsBoolean();
				yield new TransformationReward(formGroup, formName, mastery, stack);
			}
			case "KI_TECHNIQUE" -> {
				String code = firstString(json, "code", "techniqueCode", "technique_code");
				if (code == null) yield invalidReward(type, "missing 'code'");
				KiAttackData technique = KiAttackData.importFromCode(code);
				yield technique != null ? new KiTechniqueReward(technique) : invalidReward(type, "the ki technique code is not valid");
			}
			default -> {
				if (!QuestRewardRegistry.isRegistered(type)) {
					yield new PlaceholderReward(type, "unknown reward type '" + type + "'", false);
				}
				QuestReward parsed = QuestRewardRegistry.parse(type, json);
				yield parsed != null ? parsed : invalidReward(type, "the registered reward type failed to parse it");
			}
		};

		if (!reward.isPlaceholder()) {
			reward.setDifficulties(difficulties);
		}
		return reward;
	}

	private static QuestReward invalidReward(String type, String reason) {
		return new PlaceholderReward(type, reason, true);
	}

	private static Set<Difficulty> parseRewardDifficulties(JsonObject json) {
		JsonElement element = firstElement(json, "difficulty", "difficulties", "difficultyType", "minDifficulty");
		if (element == null || element.isJsonNull()) {
			return EnumSet.allOf(Difficulty.class);
		}

		Set<Difficulty> result = EnumSet.noneOf(Difficulty.class);
		if (element.isJsonArray()) {
			for (JsonElement token : element.getAsJsonArray()) {
				if (token != null && !token.isJsonNull()) addDifficultyTokens(result, token.getAsString());
			}
		} else if (element.isJsonPrimitive()) {
			addDifficultyTokens(result, element.getAsString());
		}

		return result.isEmpty() ? EnumSet.allOf(Difficulty.class) : result;
	}

	private static void addDifficultyTokens(Set<Difficulty> out, String raw) {
		if (raw == null || raw.isBlank()) return;
		for (String token : raw.split("[,\\s]+")) {
			if (token.isBlank()) continue;
			try {
				out.add(Difficulty.valueOf(token.trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException ignored) {
				// Unknown difficulty name — skip it.
			}
		}
	}

	private static JsonElement firstElement(JsonObject json, String... keys) {
		for (String key : keys) {
			if (json.has(key) && !json.get(key).isJsonNull()) {
				return json.get(key);
			}
		}
		return null;
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

	private static Double getNullableDouble(JsonObject json, String key) {
		if (!json.has(key) || json.get(key).isJsonNull()) {
			return null;
		}
		try {
			return json.get(key).getAsDouble();
		} catch (Exception ignored) {
			return null;
		}
	}

	private static <T extends Enum<T>> T parseEnum(String rawMode, Class<T> enumClass, T fallback) {
		if (rawMode == null || rawMode.isBlank()) {
			return fallback;
		}

		String normalized = rawMode.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
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

		String type = json.get("type").getAsString().toUpperCase(Locale.ROOT);
		return switch (type) {
			case "SAGA_QUEST" -> QuestPrerequisites.Condition.sagaQuest(
					json.get("sagaId").getAsString(),
					json.get("questId").getAsInt()
			);
			case "QUEST" -> QuestPrerequisites.Condition.quest(json.get("questId").getAsString());
			case "STAT" -> QuestPrerequisites.Condition.stat(
					json.get("stat").getAsString().toUpperCase(Locale.ROOT),
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
			case "RACE" -> {
				String race = firstString(json, "race", "raceName", "race_name");
				yield race != null ? QuestPrerequisites.Condition.race(race) : null;
			}
			case "CLASS" -> {
				String className = firstString(json, "class", "className", "class_name", "characterClass");
				yield className != null ? QuestPrerequisites.Condition.characterClass(className) : null;
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

		String normalized = rawMode.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

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

	private static final Set<String> QUEST_KEYS = Set.of(
			"id", "title", "type", "description", "category", "parallel_objectives", "party_scaling",
			"quest_giver", "turn_in", "secret", "claim_mode", "prerequisites", "requirements",
			"objectives", "rewards", "defaultsVersion",
			"repeatable", "repeat_cooldown_seconds", "time_limit_seconds");

	private static final Set<String> CONDITION_BLOCK_KEYS = Set.of("operator", "conditions");
	private static final Set<String> STRUCTURE_HINT_KEYS = Set.of("dimension", "x", "y", "z");
	private static final Set<String> SAGA_KEYS = Set.of("id", "name", "requirements", "questFolder", "defaultsVersion");
	private static final Set<String> SAGA_REQUIREMENT_KEYS = Set.of("previousSaga");

	public static void validate(String source, String file, JsonObject json) {
		if (json == null) return;
		JsonKeys.checkObject(source, file, "quest", json, QUEST_KEYS);

		if (json.has("objectives") && json.get("objectives").isJsonArray()) {
			int i = 0;
			for (JsonElement element : json.getAsJsonArray("objectives")) {
				if (element.isJsonObject()) validateObjective(source, file, "objectives[" + i + "]", element.getAsJsonObject());
				i++;
			}
		}

		if (json.has("rewards") && json.get("rewards").isJsonArray()) {
			int i = 0;
			for (JsonElement element : json.getAsJsonArray("rewards")) {
				if (element.isJsonObject()) validateReward(source, file, "rewards[" + i + "]", element.getAsJsonObject());
				i++;
			}
		}

		validateConditionsBlock(source, file, json, "prerequisites");
		validateConditionsBlock(source, file, json, "requirements");
	}

	public static void validateSaga(String source, String file, JsonObject json) {
		if (json == null) return;
		JsonKeys.checkObject(source, file, "saga", json, SAGA_KEYS);
		if (json.has("requirements") && json.get("requirements").isJsonObject()) {
			JsonKeys.checkObject(source, file, "requirements", json.getAsJsonObject("requirements"), SAGA_REQUIREMENT_KEYS);
		}
	}

	private static void validateObjective(String source, String file, String path, JsonObject json) {
		String rawType = json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString() : null;
		String type = rawType != null ? rawType.toUpperCase(Locale.ROOT) : null;
		Set<String> allowed;
		switch (type == null ? "" : type) {
			case "ITEM" -> allowed = JsonKeys.of("type", "item", "count");
			case "KILL" -> allowed = JsonKeys.of("type", "entity", "count", "health", "meleeDamage", "kiDamage",
					"spawn", "count_mode", "TextureVariant", "AITier", "canTransform", "TransformHealth",
					"TransformMeleeDamage", "TransformKiDamage", "TransformHealthMultiplier",
					"TransformMeleeDamageMultiplier", "TransformKiMultiplier", "TransformTriggerPercent", "CanTransform");
			case "SPAR" -> allowed = JsonKeys.of("type", "entity", "count", "health", "meleeDamage", "kiDamage",
					"spawn", "count_mode", "TextureVariant", "AITier", "canTransform", "CanTransform");
			case "DELIVER" -> allowed = JsonKeys.of("type", "item", "count", "npcId", "npc_id", "npc");
			case "SURVIVE_WAVES" -> allowed = JsonKeys.of("type", "entity", "waves", "mobs_per_wave", "mobsPerWave",
					"wave_delay_seconds", "waveDelaySeconds", "health", "meleeDamage", "kiDamage",
					"TextureVariant", "AITier", "CanTransform", "canTransform");
			case "ESCORT" -> allowed = JsonKeys.of("type", "entity", "x", "y", "z", "radius", "health", "dimension");
			case "CHECKPOINT_RACE" -> allowed = JsonKeys.of("type", "radius", "checkpoints", "dimension");
			case "BIOME" -> allowed = JsonKeys.of("type", "biome");
			case "DIMENSION" -> allowed = JsonKeys.of("type", "dimension");
			case "COORDS" -> allowed = JsonKeys.of("type", "x", "y", "z", "radius");
			case "INTERACT" -> allowed = JsonKeys.of("type", "entity", "entityName");
			case "STRUCTURE" -> allowed = JsonKeys.of("type", "structure");
			case "DRAGON_SUMMON" -> allowed = JsonKeys.of("type", "dragon", "dragon_id", "dragonId",
					"ball_set", "ballSet", "ball_set_id", "ballSetId", "set");
			case "TALK_TO" -> allowed = JsonKeys.of("type", "npcId");
			case "SKILL" -> allowed = JsonKeys.of("type", "skill", "skillId", "id", "level", "minLevel", "required");
			default -> {
				if (!QuestObjectiveRegistry.isRegistered(rawType)) JsonKeys.reportBadType(source, file, path, type);
				return;
			}
		}
		JsonKeys.checkObject(source, file, path, json, allowed);
	}

	private static void validateReward(String source, String file, String path, JsonObject json) {
		String rawType = json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString() : null;
		if (rawType != null && (rawType.toLowerCase(Locale.ROOT).startsWith("hard:") || rawType.toLowerCase(Locale.ROOT).startsWith("normal:"))) {
			rawType = rawType.substring(rawType.indexOf(':') + 1);
		}
		String type = rawType != null ? rawType.toUpperCase(Locale.ROOT) : null;
		Set<String> common = JsonKeys.of("type", "difficulty", "difficulties", "difficultyType", "minDifficulty");
		Set<String> allowed;
		switch (type == null ? "" : type) {
			case "ITEM" -> allowed = JsonKeys.union(common, "item", "count");
			case "GENERIC_ITEM"  -> allowed = JsonKeys.union(common, "itemReward", "itemType", "itemId", "count", "enchantments", "potion", "mobEffects", "material", "pattern");
			case "TPS", "ATTRIBUTES", "ATTRIBUTE_POINTS" -> allowed = JsonKeys.union(common, "amount");
			case "ALIGNMENT" -> allowed = JsonKeys.union(common, "amount");
			case "COMMAND" -> allowed = JsonKeys.union(common, "command", "translationKey");
			case "SKILL" -> allowed = JsonKeys.union(common, "skill", "level");
			case "TRANSFORMATION" -> allowed = JsonKeys.union(common, "formGroup", "form_group", "group",
					"formName", "form_name", "form", "mastery", "stack");
			case "KI_TECHNIQUE" -> allowed = JsonKeys.union(common, "code", "techniqueCode", "technique_code");
			default -> {
				if (!QuestRewardRegistry.isRegistered(rawType)) JsonKeys.reportBadType(source, file, path, type);
				return;
			}
		}
		JsonKeys.checkObject(source, file, path, json, allowed);
	}

	private static void validateConditionsBlock(String source, String file, JsonObject parent, String key) {
		if (!parent.has(key) || !parent.get(key).isJsonObject()) return;
		JsonObject block = parent.getAsJsonObject(key);
		JsonKeys.checkObject(source, file, key, block, CONDITION_BLOCK_KEYS);
		validateConditionArray(source, file, key, block);
	}

	private static void validateConditionArray(String source, String file, String path, JsonObject block) {
		if (!block.has("conditions") || !block.get("conditions").isJsonArray()) return;
		int i = 0;
		for (JsonElement element : block.getAsJsonArray("conditions")) {
			if (element.isJsonObject()) validateCondition(source, file, path + ".conditions[" + i + "]", element.getAsJsonObject());
			i++;
		}
	}

	private static void validateCondition(String source, String file, String path, JsonObject json) {
		if (json.has("operator")) {
			JsonKeys.checkObject(source, file, path, json, CONDITION_BLOCK_KEYS);
			validateConditionArray(source, file, path, json);
			return;
		}

		String type = json.has("type") && !json.get("type").isJsonNull() ? json.get("type").getAsString().toUpperCase(Locale.ROOT) : null;
		Set<String> allowed;
		switch (type == null ? "" : type) {
			case "SAGA_QUEST" -> allowed = JsonKeys.of("type", "sagaId", "questId");
			case "QUEST" -> allowed = JsonKeys.of("type", "questId");
			case "STAT" -> allowed = JsonKeys.of("type", "stat", "minValue");
			case "LEVEL" -> allowed = JsonKeys.of("type", "minLevel");
			case "BIOME" -> allowed = JsonKeys.of("type", "biome");
			case "STRUCTURE" -> { validateStructureCondition(source, file, path, json); return; }
			case "DIMENSION" -> allowed = JsonKeys.of("type", "dimension");
			case "TIME" -> allowed = JsonKeys.of("type", "mode", "ticks", "milliseconds");
			case "ALIGNMENT" -> allowed = JsonKeys.of("type", "min", "max");
			case "SKILL" -> allowed = JsonKeys.of("type", "skill", "skillId", "id", "minLevel", "level", "required");
			case "RACE" -> allowed = JsonKeys.of("type", "race", "raceName", "race_name");
			case "CLASS" -> allowed = JsonKeys.of("type", "class", "className", "class_name", "characterClass");
			default -> { JsonKeys.reportBadType(source, file, path, type); return; }
		}
		JsonKeys.checkObject(source, file, path, json, allowed);
	}

	private static void validateStructureCondition(String source, String file, String path, JsonObject json) {
		JsonKeys.checkObject(source, file, path, json, JsonKeys.of("type", "structure", "hint"));
		if (json.has("hint") && json.get("hint").isJsonObject()) {
			JsonKeys.checkObject(source, file, path + ".hint", json.getAsJsonObject("hint"), STRUCTURE_HINT_KEYS);
		}
	}
}
