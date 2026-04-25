package com.dragonminez.common.network.S2C;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestParser;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.objectives.BiomeObjective;
import com.dragonminez.common.quest.objectives.CoordsObjective;
import com.dragonminez.common.quest.objectives.DimensionObjective;
import com.dragonminez.common.quest.objectives.InteractObjective;
import com.dragonminez.common.quest.objectives.ItemObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.StructureObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.quest.rewards.CommandReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.dragonminez.server.world.dimension.HTCDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import com.dragonminez.server.world.structure.helper.StructureLocator;
import com.mojang.datafixers.util.Pair;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sync packet that sends the entire QuestRegistry state (sagas + quests) to the client.
 */
public class SyncQuestRegistryS2C {

	private static final Gson GSON = new GsonBuilder().create();
	private static final Map<String, Pair<ResourceKey<Structure>, ResourceKey<Level>>> DMZ_STRUCTURE_INFO = Map.of(
			Reference.MOD_ID + ":goku_house", Pair.of(DMZStructures.GOKU_HOUSE, Level.OVERWORLD),
			Reference.MOD_ID + ":roshi_house", Pair.of(DMZStructures.ROSHI_HOUSE, Level.OVERWORLD),
			Reference.MOD_ID + ":elder_guru", Pair.of(DMZStructures.ELDER_GURU, NamekDimension.NAMEK_KEY),
			Reference.MOD_ID + ":timechamber", Pair.of(DMZStructures.TIMECHAMBER, HTCDimension.HTC_KEY),
			Reference.MOD_ID + ":kamilookout", Pair.of(DMZStructures.KAMILOOKOUT, Level.OVERWORLD),
			Reference.MOD_ID + ":gero_lab", Pair.of(DMZStructures.GERO_LAB, Level.OVERWORLD)
	);

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

	private void handleOnClient() {
		Map<String, Saga> sagas = new LinkedHashMap<>();
		JsonObject sagasRoot = JsonParser.parseString(sagasJson).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : sagasRoot.entrySet()) {
			Saga saga = parseSyncedSagaFromJson(entry.getValue().getAsJsonObject());
			sagas.put(entry.getKey(), saga);
		}

		Map<String, Quest> allQuests = new LinkedHashMap<>();
		for (Map.Entry<String, Saga> entry : sagas.entrySet()) {
			String sagaId = entry.getKey();
			for (Quest quest : entry.getValue().getQuests()) {
				allQuests.put(sagaId + ":" + quest.getId(), quest);
			}
		}

		JsonObject questsRoot = JsonParser.parseString(questsJson).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : questsRoot.entrySet()) {
			Quest quest = QuestParser.parseQuest(entry.getValue().getAsJsonObject());
			if (quest != null) {
				allQuests.put(entry.getKey(), quest);
			}
		}

		QuestRegistry.applySyncedSagas(sagas);
		QuestRegistry.applySyncedQuests(allQuests);
	}

	private static Saga parseSyncedSagaFromJson(JsonObject json) {
		String id = json.has("id") ? json.get("id").getAsString() : "";
		String name = json.has("name") ? json.get("name").getAsString() : id;

		Saga.SagaRequirements requirements = null;
		if (json.has("requirements") && json.get("requirements").isJsonObject()) {
			JsonObject reqJson = json.getAsJsonObject("requirements");
			String prevSaga = reqJson.has("previousSaga") ? reqJson.get("previousSaga").getAsString() : "";
			requirements = new Saga.SagaRequirements(prevSaga);
		}

		List<Quest> quests = new ArrayList<>();
		if (json.has("quests") && json.get("quests").isJsonArray()) {
			for (JsonElement questElement : json.getAsJsonArray("quests")) {
				if (!questElement.isJsonObject()) continue;
				Quest parsed = QuestParser.parseQuest(questElement.getAsJsonObject());
				if (parsed != null) {
					quests.add(parsed);
				} else {
					LogUtil.warn(Env.CLIENT, "SyncQuestRegistryS2C: failed to parse a saga quest in saga '{}'", id);
				}
			}
		}

		return new Saga(id, name, quests, requirements);
	}

	private static String serializeSagas(Map<String, Saga> sagas) {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, Saga> entry : sagas.entrySet()) {
			root.add(entry.getKey(), serializeSaga(entry.getValue()));
		}
		return GSON.toJson(root);
	}

	private static JsonObject serializeSaga(Saga saga) {
		JsonObject obj = new JsonObject();
		obj.addProperty("id", saga.getId());
		obj.addProperty("name", saga.getName());

		if (saga.getRequirements() != null) {
			JsonObject req = new JsonObject();
			req.addProperty("previousSaga", saga.getRequirements().previousSagaId());
			obj.add("requirements", req);
		}

		JsonArray questsArr = new JsonArray();
		for (Quest quest : saga.getQuests()) {
			questsArr.add(serializeQuest(quest));
		}
		obj.add("quests", questsArr);
		return obj;
	}

	private static String serializeStandaloneQuests(Map<String, Quest> quests) {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, Quest> entry : quests.entrySet()) {
			Quest quest = entry.getValue();
			if (quest.getType() == Quest.QuestType.SAGA) continue;
			root.add(entry.getKey(), serializeQuest(quest));
		}
		return GSON.toJson(root);
	}

	private static JsonObject serializeQuest(Quest quest) {
		JsonObject obj = new JsonObject();
		if (quest.getStringId() != null) obj.addProperty("id", quest.getStringId());
		else obj.addProperty("id", quest.getId());

		obj.addProperty("type", quest.getType().name());
		obj.addProperty("title", quest.getTitle());
		obj.addProperty("description", quest.getDescription());
		obj.addProperty("category", quest.getCategory());
		obj.addProperty("parallel_objectives", quest.isParallelObjectives());
		obj.addProperty("party_scaling", quest.isPartyScaling());
		obj.addProperty("secret", quest.isSecret());
		obj.addProperty("claim_mode", quest.getClaimMode().name());

		if (quest.getQuestGiver() != null) obj.addProperty("quest_giver", quest.getQuestGiver());
		else obj.add("quest_giver", JsonNull.INSTANCE);
		if (quest.getTurnIn() != null) obj.addProperty("turn_in", quest.getTurnIn());
		else obj.add("turn_in", JsonNull.INSTANCE);

		if (quest.getPrerequisites() != null && !quest.getPrerequisites().conditions().isEmpty()) {
			obj.add("prerequisites", serializePrerequisites(quest.getPrerequisites()));
		}
		if (quest.getStartRequirements() != null && !quest.getStartRequirements().conditions().isEmpty()) {
			obj.add("requirements", serializePrerequisites(quest.getStartRequirements()));
		}

		obj.add("objectives", serializeObjectives(quest.getObjectives()));
		obj.add("rewards", serializeRewards(quest.getRewards()));
		return obj;
	}

	private static JsonArray serializeObjectives(List<QuestObjective> objectives) {
		JsonArray arr = new JsonArray();
		for (QuestObjective objective : objectives) {
			arr.add(serializeObjective(objective));
		}
		return arr;
	}

	private static JsonObject serializeObjective(QuestObjective objective) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", objective.getType().name());

		if (objective instanceof KillObjective kill) {
			obj.addProperty("entity", kill.getEntityId());
			obj.addProperty("count", kill.getCount());
			obj.addProperty("health", kill.getHealth());
			obj.addProperty("meleeDamage", kill.getMeleeDamage());
			obj.addProperty("kiDamage", kill.getKiDamage());
			obj.addProperty("spawn", kill.getSpawnMode().name());
			obj.addProperty("count_mode", kill.getCountMode().name());
		} else if (objective instanceof ItemObjective item) {
			obj.addProperty("item", item.getItemId());
			obj.addProperty("count", item.getCount());
		} else if (objective instanceof BiomeObjective biome) {
			obj.addProperty("biome", biome.getBiomeId());
		} else if (objective instanceof DimensionObjective dimension) {
			obj.addProperty("dimension", dimension.getDimensionId());
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

	private static JsonArray serializeRewards(List<QuestReward> rewards) {
		JsonArray arr = new JsonArray();
		for (QuestReward reward : rewards) {
			arr.add(serializeReward(reward));
		}
		return arr;
	}

	private static JsonObject serializeReward(QuestReward reward) {
		JsonObject obj = new JsonObject();

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

	private static JsonObject serializePrerequisites(QuestPrerequisites prereqs) {
		JsonObject obj = new JsonObject();
		obj.addProperty("operator", prereqs.operator().name());

		JsonArray conditions = new JsonArray();
		for (QuestPrerequisites.Condition condition : prereqs.conditions()) {
			conditions.add(serializeCondition(condition));
		}
		obj.add("conditions", conditions);
		return obj;
	}

	private static JsonObject serializeCondition(QuestPrerequisites.Condition condition) {
		if (condition.getNested() != null) {
			return serializePrerequisites(condition.getNested());
		}

		JsonObject obj = new JsonObject();
		obj.addProperty("type", condition.getType().name());

		switch (condition.getType()) {
			case SAGA_QUEST -> {
				obj.addProperty("sagaId", condition.getSagaId());
				obj.addProperty("questId", condition.getQuestId());
			}
			case QUEST -> obj.addProperty("questId", condition.getRequiredQuestId());
			case STAT -> {
				obj.addProperty("stat", condition.getStat());
				obj.addProperty("minValue", condition.getMinValue());
			}
			case LEVEL -> obj.addProperty("minLevel", condition.getMinLevel());
			case BIOME -> obj.addProperty("biome", condition.getBiomeId());
			case STRUCTURE -> {
				obj.addProperty("structure", condition.getStructureId());
				QuestPrerequisites.StructureHint hint = condition.getStructureHint();
				if (hint == null) {
					hint = resolveStructureHint(condition.getStructureId());
				}
				if (hint != null) {
					JsonObject hintObj = new JsonObject();
					if (hint.dimensionId() != null) hintObj.addProperty("dimension", hint.dimensionId());
					if (hint.x() != null) hintObj.addProperty("x", hint.x());
					if (hint.y() != null) hintObj.addProperty("y", hint.y());
					if (hint.z() != null) hintObj.addProperty("z", hint.z());
					if (!hintObj.entrySet().isEmpty()) {
						obj.add("hint", hintObj);
					}
				}
			}
			case DIMENSION -> obj.addProperty("dimension", condition.getDimensionId());
			case TIME -> {
				if (condition.getTimeMode() != null) {
					obj.addProperty("mode", condition.getTimeMode().name());
				}
				if (condition.getDuration() != null) {
					if (condition.getTimeMode() == QuestPrerequisites.TimeMode.GAME_TIME) {
						obj.addProperty("ticks", condition.getDuration());
					} else {
						obj.addProperty("milliseconds", condition.getDuration());
					}
				}
			}
		}

		return obj;
	}

	private static QuestPrerequisites.StructureHint resolveStructureHint(String structureId) {
		if (structureId == null || structureId.isBlank() || !structureId.contains(":")) {
			return null;
		}

		String normalized;
		try {
			normalized = ResourceLocation.parse(structureId).toString();
		} catch (Exception e) {
			return null;
		}

		Pair<ResourceKey<Structure>, ResourceKey<Level>> info = DMZ_STRUCTURE_INFO.get(normalized);
		if (info == null) {
			return null;
		}

		String dimensionId = info.getSecond().location().toString();
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);
		}

		ServerLevel targetLevel = server.getLevel(info.getSecond());
		if (targetLevel == null) {
			return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);
		}

		BlockPos structurePos = StructureLocator.locateStructure(targetLevel, info.getFirst(), targetLevel.getSharedSpawnPos());
		if (structurePos == null) {
			return new QuestPrerequisites.StructureHint(dimensionId, null, null, null);
		}

		return new QuestPrerequisites.StructureHint(
				dimensionId,
				structurePos.getX(),
				structurePos.getY(),
				structurePos.getZ()
		);
	}
}
