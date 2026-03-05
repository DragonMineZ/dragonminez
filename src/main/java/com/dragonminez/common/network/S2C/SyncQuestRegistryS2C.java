package com.dragonminez.common.network.S2C;

import com.dragonminez.common.quest.*;
import com.dragonminez.common.quest.objectives.*;
import com.dragonminez.common.quest.rewards.*;
import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * Unified sync packet that sends the entire QuestRegistry state (sagas + quests) to the client.
 * Replaces the old SyncSagasS2C packets.
 * <p>
 * Serialization uses the same JSON format as the disk files so that the existing
 * {@link QuestParser} methods can deserialize everything without a custom format.
 *
 * @since 2.1
 */
public class SyncQuestRegistryS2C {

	private static final Gson GSON = new GsonBuilder().create();

	private final String sagasJson;
	private final String questsJson;

	public SyncQuestRegistryS2C(Map<String, Saga> sagas, Map<String, Quest> quests) {
		this.sagasJson = serializeSagas(sagas);
		this.questsJson = serializeStandaloneQuests(quests);
	}

	public SyncQuestRegistryS2C(FriendlyByteBuf buf) {
		this.sagasJson = buf.readUtf(1048576);
		this.questsJson = buf.readUtf(1048576);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(sagasJson, 1048576);
		buf.writeUtf(questsJson, 1048576);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() ->
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleOnClient)
		);
		ctx.get().setPacketHandled(true);
	}

	// ========================================================================================
	// Client-side handling
	// ========================================================================================

	private void handleOnClient() {
		// Deserialize sagas (each saga is a full saga JSON with embedded quests)
		Map<String, Saga> sagas = new LinkedHashMap<>();
		JsonObject sagasRoot = JsonParser.parseString(sagasJson).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : sagasRoot.entrySet()) {
			Saga saga = QuestRegistry.parseSagaFromJson(entry.getValue().getAsJsonObject());
			sagas.put(entry.getKey(), saga);
		}

		// Deserialize standalone quests (sidequests etc. — not embedded in sagas)
		Map<String, Quest> allQuests = new LinkedHashMap<>();

		// First, index all saga quests by composite key
		for (Map.Entry<String, Saga> entry : sagas.entrySet()) {
			String sagaId = entry.getKey();
			for (Quest quest : entry.getValue().getQuests()) {
				allQuests.put(sagaId + ":" + quest.getId(), quest);
			}
		}

		// Then add standalone quests (sidequests)
		JsonObject questsRoot = JsonParser.parseString(questsJson).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : questsRoot.entrySet()) {
			Quest quest = QuestParser.parseSideQuest(entry.getValue().getAsJsonObject());
			allQuests.put(entry.getKey(), quest);
		}

		QuestRegistry.applySyncedSagas(sagas);
		QuestRegistry.applySyncedQuests(allQuests);
	}

	// ========================================================================================
	// Saga Serialization — reuses the same format as saga JSON files
	// ========================================================================================

	private static String serializeSagas(Map<String, Saga> sagas) {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, Saga> entry : sagas.entrySet()) {
			root.add(entry.getKey(), serializeSaga(entry.getValue()));
		}
		return GSON.toJson(root);
	}

	/**
	 * Serializes a Saga in the exact JSON format that {@link QuestRegistry#parseSagaFromJson} expects.
	 */
	private static JsonObject serializeSaga(Saga saga) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", saga.getId());
		obj.addProperty("name", saga.getName());

		if (saga.getRequirements() != null) {
			JsonObject req = new JsonObject();
			req.addProperty("previousSaga", saga.getRequirements().getPreviousSagaId());
			obj.add("requirements", req);
		}

		// Serialize quests in parseSagaQuest format
		JsonArray questsArr = new JsonArray();
		for (Quest quest : saga.getQuests()) {
			questsArr.add(serializeSagaQuest(quest));
		}
		obj.add("quests", questsArr);
		return obj;
	}

	/**
	 * Serializes a saga quest in the format {@link QuestParser#parseSagaQuest} expects.
	 */
	private static JsonObject serializeSagaQuest(Quest quest) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", quest.getId());
		obj.addProperty("title", quest.getTitle());
		obj.addProperty("description", quest.getDescription());
		obj.add("objectives", serializeObjectives(quest.getObjectives()));
		obj.add("rewards", serializeRewards(quest.getRewards()));
		return obj;
	}

	// ========================================================================================
	// Standalone Quest Serialization — only sidequests (not embedded in sagas)
	// ========================================================================================

	/**
	 * Serializes only standalone (non-saga) quests. Saga quests are already embedded in the saga JSON.
	 */
	private static String serializeStandaloneQuests(Map<String, Quest> quests) {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, Quest> entry : quests.entrySet()) {
			Quest quest = entry.getValue();
			// Skip saga quests — they're already embedded in the saga JSON
			if (quest.getType() == Quest.QuestType.SAGA) continue;
			root.add(entry.getKey(), serializeSideQuest(quest));
		}
		return GSON.toJson(root);
	}

	/**
	 * Serializes a side-quest in the format {@link QuestParser#parseSideQuest} expects.
	 */
	private static JsonObject serializeSideQuest(Quest quest) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", quest.getStringId() != null ? quest.getStringId() : quest.getEffectiveId());
		obj.addProperty("name", quest.getTitle());
		obj.addProperty("description", quest.getDescription());
		if (quest.getCategory() != null) obj.addProperty("category", quest.getCategory());
		obj.addProperty("parallelObjectives", quest.isParallelObjectives());
		if (quest.getQuestGiver() != null) obj.addProperty("questGiver", quest.getQuestGiver());
		if (quest.getTurnIn() != null) obj.addProperty("turnIn", quest.getTurnIn());

		// Prerequisites
		if (quest.getPrerequisites() != null && !quest.getPrerequisites().getConditions().isEmpty()) {
			obj.add("prerequisites", serializePrerequisites(quest.getPrerequisites()));
		}

		obj.add("objectives", serializeObjectives(quest.getObjectives()));
		obj.add("rewards", serializeRewards(quest.getRewards()));
		return obj;
	}

	// ========================================================================================
	// Objective Serialization — matches QuestParser.parseObjective format
	// ========================================================================================

	private static JsonArray serializeObjectives(List<QuestObjective> objectives) {
		JsonArray arr = new JsonArray();
		for (QuestObjective objective : objectives) {
			arr.add(serializeObjective(objective));
		}
		return arr;
	}

	/**
	 * Serializes a single objective back into the JSON format {@link QuestParser#parseObjective} expects.
	 */
	private static JsonObject serializeObjective(QuestObjective objective) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", objective.getType().name());
		obj.addProperty("description", objective.getDescription());

		if (objective instanceof KillObjective kill) {
			obj.addProperty("entity", kill.getEntityId());
			obj.addProperty("count", kill.getCount());
			obj.addProperty("health", kill.getHealth());
			obj.addProperty("meleeDamage", kill.getMeleeDamage());
			obj.addProperty("kiDamage", kill.getKiDamage());
		} else if (objective instanceof ItemObjective item) {
			obj.addProperty("item", item.getItemId());
			obj.addProperty("count", item.getCount());
		} else if (objective instanceof BiomeObjective biome) {
			obj.addProperty("biome", biome.getBiomeId());
		} else if (objective instanceof StructureObjective structure) {
			obj.addProperty("structure", structure.getStructureId());
		} else if (objective instanceof TalkToObjective talkTo) {
			obj.addProperty("npcId", talkTo.getNpcId());
		} else if (objective instanceof CoordsObjective coords) {
			obj.addProperty("x", coords.getTargetPos().getX());
			obj.addProperty("y", coords.getTargetPos().getY());
			obj.addProperty("z", coords.getTargetPos().getZ());
			obj.addProperty("radius", coords.getRadius());
		} else if (objective instanceof InteractObjective interact) {
			if (interact.getEntityTypeId() != null) obj.addProperty("entity", interact.getEntityTypeId());
			if (interact.getEntityName() != null) obj.addProperty("entityName", interact.getEntityName());
		}

		return obj;
	}

	// ========================================================================================
	// Reward Serialization — matches QuestParser.parseReward format
	// ========================================================================================

	private static JsonArray serializeRewards(List<QuestReward> rewards) {
		JsonArray arr = new JsonArray();
		for (QuestReward reward : rewards) {
			arr.add(serializeReward(reward));
		}
		return arr;
	}

	/**
	 * Serializes a single reward back into the JSON format {@link QuestParser#parseReward} expects.
	 */
	private static JsonObject serializeReward(QuestReward reward) {
		JsonObject obj = new JsonObject();

		// Build type string with optional difficulty prefix
		String typeStr = reward.getType().name();
		if (reward.getDifficultyType() == QuestReward.DifficultyType.HARD) {
			typeStr = "hard:" + typeStr;
		} else if (reward.getDifficultyType() == QuestReward.DifficultyType.NORMAL) {
			typeStr = "normal:" + typeStr;
		}
		obj.addProperty("type", typeStr);

		if (reward instanceof TPSReward tps) {
			obj.addProperty("amount", tps.getAmount());
		} else if (reward instanceof ItemReward item) {
			obj.addProperty("item", item.getItemId());
			obj.addProperty("count", item.getCount());
		} else if (reward instanceof CommandReward command) {
			obj.addProperty("command", command.getCommand());
		} else if (reward instanceof SkillReward skill) {
			obj.addProperty("skill", skill.getSkill());
			obj.addProperty("level", skill.getLevel());
		}

		return obj;
	}

	// ========================================================================================
	// Prerequisites Serialization — matches QuestParser.parsePrerequisites format
	// ========================================================================================

	private static JsonObject serializePrerequisites(QuestPrerequisites prereqs) {
		JsonObject obj = new JsonObject();
		obj.addProperty("operator", prereqs.getOperator().name());

		JsonArray conditions = new JsonArray();
		for (QuestPrerequisites.Condition condition : prereqs.getConditions()) {
			conditions.add(serializeCondition(condition));
		}
		obj.add("conditions", conditions);
		return obj;
	}

	private static JsonObject serializeCondition(QuestPrerequisites.Condition condition) {
		JsonObject obj = new JsonObject();

		if (condition.getNested() != null) {
			// Nested group — serialize as a nested prerequisites block
			return serializePrerequisites(condition.getNested());
		}

		obj.addProperty("type", condition.getType().name());

		switch (condition.getType()) {
			case SAGA_QUEST -> {
				obj.addProperty("sagaId", condition.getSagaId());
				obj.addProperty("questId", condition.getQuestId());
			}
			case QUEST -> {
				obj.addProperty("questId", condition.getRequiredQuestId());
			}
			case STAT -> {
				obj.addProperty("stat", condition.getStat());
				obj.addProperty("minValue", condition.getMinValue());
			}
			case RACE -> {
				obj.addProperty("race", condition.getRace());
			}
			case LEVEL -> {
				obj.addProperty("minLevel", condition.getMinLevel());
			}
		}

		return obj;
	}
}

