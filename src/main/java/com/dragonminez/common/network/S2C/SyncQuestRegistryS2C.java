package com.dragonminez.common.network.S2C;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestObjectiveRegistry;
import com.dragonminez.common.quest.QuestParser;
import com.dragonminez.common.quest.QuestPrerequisites;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.quest.QuestRewardRegistry;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.quest.objectives.BiomeObjective;
import com.dragonminez.common.quest.objectives.CheckpointRaceObjective;
import com.dragonminez.common.quest.objectives.CoordsObjective;
import com.dragonminez.common.quest.objectives.DeliverObjective;
import com.dragonminez.common.quest.objectives.EscortObjective;
import com.dragonminez.common.quest.objectives.SurviveWavesObjective;
import com.dragonminez.common.quest.objectives.DimensionObjective;
import com.dragonminez.common.quest.objectives.DragonSummonObjective;
import com.dragonminez.common.quest.objectives.InteractObjective;
import com.dragonminez.common.quest.objectives.ItemObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.SkillObjective;
import com.dragonminez.common.quest.objectives.StructureObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.quest.rewards.AlignmentReward;
import com.dragonminez.common.quest.rewards.CommandReward;
import com.dragonminez.common.quest.rewards.GenericItemReward;
import com.dragonminez.common.quest.rewards.ItemReward;
import com.dragonminez.common.quest.rewards.KiTechniqueReward;
import com.dragonminez.common.quest.rewards.SkillReward;
import com.dragonminez.common.quest.rewards.AttributePointsReward;
import com.dragonminez.common.quest.rewards.AttributesReward;
import com.dragonminez.common.quest.rewards.TPSReward;
import com.dragonminez.common.quest.rewards.TransformationReward;
import com.dragonminez.common.util.gson.GsonUtils;
import com.dragonminez.server.world.structure.helper.QuestStructureHints;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Sync packet that sends the entire QuestRegistry state (sagas + quests) to the client.
 */
public class SyncQuestRegistryS2C {

	public static final int MAX_PAYLOAD_BYTES = 1_000_000;
	private static final int MAX_DECOMPRESSED_BYTES = 64 * 1024 * 1024;

	private final byte[] sagasData;
	private final byte[] questsData;

	public SyncQuestRegistryS2C(Map<String, Saga> sagas, Map<String, Quest> quests) {
		this.sagasData = compress(serializeSagas(sagas));
		this.questsData = compress(serializeStandaloneQuests(quests));
	}

	public SyncQuestRegistryS2C(FriendlyByteBuf buf) {
		this.sagasData = buf.readByteArray(MAX_PAYLOAD_BYTES);
		this.questsData = buf.readByteArray(MAX_PAYLOAD_BYTES);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeByteArray(sagasData);
		buf.writeByteArray(questsData);
	}

	public int payloadSize() {
		return sagasData.length + questsData.length;
	}

	@Nullable
	public static SyncQuestRegistryS2C fromRegistry() {
		try {
			SyncQuestRegistryS2C packet = new SyncQuestRegistryS2C(QuestRegistry.getAllSagas(), QuestRegistry.getAllQuests());
			if (packet.payloadSize() > MAX_PAYLOAD_BYTES) {
				LogUtil.error(Env.SERVER, "SyncQuestRegistryS2C: the quest registry takes {} bytes compressed (limit {}); clients will not receive quests until it is smaller",
						packet.payloadSize(), MAX_PAYLOAD_BYTES);
				return null;
			}
			return packet;
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "SyncQuestRegistryS2C: failed to serialize the quest registry: {}", e.toString());
			return null;
		}
	}

	public static boolean sendTo(@Nullable SyncQuestRegistryS2C packet, @Nullable ServerPlayer player) {
		if (packet == null || player == null) return false;
		try {
			NetworkHandler.sendToPlayer(packet, player);
			return true;
		} catch (Exception e) {
			LogUtil.error(Env.SERVER, "SyncQuestRegistryS2C: failed to send the quest registry to {}: {}",
					player.getGameProfile().getName(), e.toString());
			return false;
		}
	}

	public static boolean sendTo(@Nullable ServerPlayer player) {
		if (player == null) return false;
		return sendTo(fromRegistry(), player);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() ->
				DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handleOnClient)
		);
		ctx.get().setPacketHandled(true);
	}

	private void handleOnClient() {
		String sagasJson;
		String questsJson;
		try {
			sagasJson = decompress(sagasData);
			questsJson = decompress(questsData);
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "SyncQuestRegistryS2C: could not decompress the quest registry sent by the server: {}", e.toString());
			return;
		}

		JsonObject sagasRoot;
		JsonObject questsRoot;
		try {
			sagasRoot = JsonParser.parseString(sagasJson).getAsJsonObject();
			questsRoot = JsonParser.parseString(questsJson).getAsJsonObject();
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "SyncQuestRegistryS2C: the quest registry sent by the server is not valid JSON: {}", e.toString());
			return;
		}

		Map<String, Saga> sagas = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : sagasRoot.entrySet()) {
			try {
				sagas.put(entry.getKey(), parseSyncedSagaFromJson(entry.getValue().getAsJsonObject()));
			} catch (Exception e) {
				LogUtil.error(Env.CLIENT, "SyncQuestRegistryS2C: failed to read saga '{}': {}", entry.getKey(), e.toString());
			}
		}

		Map<String, Quest> allQuests = new LinkedHashMap<>();
		for (Map.Entry<String, Saga> entry : sagas.entrySet()) {
			String sagaId = entry.getKey();
			for (Quest quest : entry.getValue().getQuests()) {
				allQuests.put(sagaId + ":" + quest.getId(), quest);
			}
		}

		for (Map.Entry<String, JsonElement> entry : questsRoot.entrySet()) {
			try {
				Quest quest = QuestParser.parseQuest(entry.getValue().getAsJsonObject());
				if (quest != null) {
					allQuests.put(entry.getKey(), quest);
				} else {
					LogUtil.warn(Env.CLIENT, "SyncQuestRegistryS2C: quest '{}' was rejected: {}", entry.getKey(),
							QuestParser.rejectionReason(entry.getValue().getAsJsonObject()));
				}
			} catch (Exception e) {
				LogUtil.error(Env.CLIENT, "SyncQuestRegistryS2C: failed to read quest '{}': {}", entry.getKey(), e.toString());
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
				Quest parsed = null;
				try {
					parsed = QuestParser.parseQuest(questElement.getAsJsonObject());
				} catch (Exception e) {
					LogUtil.error(Env.CLIENT, "SyncQuestRegistryS2C: failed to read a saga quest in saga '{}': {}", id, e.toString());
					continue;
				}
				if (parsed != null) {
					quests.add(parsed);
				} else {
					LogUtil.warn(Env.CLIENT, "SyncQuestRegistryS2C: failed to parse a saga quest in saga '{}'", id);
				}
			}
		}

		return new Saga(id, name, quests, requirements);
	}

	private static byte[] compress(String json) {
		byte[] input = json.getBytes(StandardCharsets.UTF_8);
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		try {
			deflater.setInput(input);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, input.length / 4));
			byte[] buffer = new byte[8192];
			while (!deflater.finished()) {
				int written = deflater.deflate(buffer);
				out.write(buffer, 0, written);
			}
			return out.toByteArray();
		} finally {
			deflater.end();
		}
	}

	private static String decompress(byte[] data) throws DataFormatException {
		Inflater inflater = new Inflater();
		try {
			inflater.setInput(data);
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, Math.min(data.length * 4, MAX_DECOMPRESSED_BYTES)));
			byte[] buffer = new byte[8192];
			while (!inflater.finished()) {
				int read = inflater.inflate(buffer);
				if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
					throw new DataFormatException("truncated quest registry payload");
				}
				out.write(buffer, 0, read);
				if (out.size() > MAX_DECOMPRESSED_BYTES) {
					throw new DataFormatException("quest registry payload is larger than " + MAX_DECOMPRESSED_BYTES + " bytes");
				}
			}
			return out.toString(StandardCharsets.UTF_8);
		} finally {
			inflater.end();
		}
	}

	private static String serializeSagas(Map<String, Saga> sagas) {
		JsonObject root = new JsonObject();
		for (Map.Entry<String, Saga> entry : sagas.entrySet()) {
			root.add(entry.getKey(), serializeSaga(entry.getValue()));
		}
		return GsonUtils.NETWORK.toJson(root);
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
		return GsonUtils.NETWORK.toJson(root);
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
		obj.addProperty("repeatable", quest.isRepeatable());
		obj.addProperty("repeat_cooldown_seconds", quest.getRepeatCooldownSeconds());
		obj.addProperty("time_limit_seconds", quest.getTimeLimitSeconds());

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
			try {
				arr.add(serializeObjective(objective));
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "SyncQuestRegistryS2C: could not serialize a '{}' objective, clients will see it as inert: {}",
						objective.getTypeKey(), e.toString());
				arr.add(placeholderJson());
			}
		}
		return arr;
	}

	private static JsonObject placeholderJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", "PLACEHOLDER");
		return obj;
	}

	private static JsonObject serializeObjective(QuestObjective objective) {
		if (objective.isPlaceholder()) {
			return placeholderJson();
		}

		JsonObject obj = new JsonObject();
		obj.addProperty("type", objective.getTypeKey());

		if (objective instanceof KillObjective kill) {
			obj.addProperty("entity", kill.getEntityId());
			obj.addProperty("count", kill.getCount());
			obj.addProperty("health", kill.getHealth());
			obj.addProperty("meleeDamage", kill.getMeleeDamage());
			obj.addProperty("kiDamage", kill.getKiDamage());
			obj.addProperty("spawn", kill.getSpawnMode().name());
			obj.addProperty("count_mode", kill.getCountMode().name());
			if (kill.getTextureVariant() >= 0) {
				obj.addProperty("TextureVariant", kill.getTextureVariant());
			}
			obj.addProperty("AITier", kill.getAiTier());
			obj.addProperty("canTransform", kill.isCanTransform());
			addIfPresent(obj, "TransformHealth", kill.getTransformHealth());
			addIfPresent(obj, "TransformMeleeDamage", kill.getTransformMeleeDamage());
			addIfPresent(obj, "TransformKiDamage", kill.getTransformKiDamage());
			addIfPresent(obj, "TransformHealthMultiplier", kill.getTransformHealthMultiplier());
			addIfPresent(obj, "TransformMeleeDamageMultiplier", kill.getTransformMeleeMultiplier());
			addIfPresent(obj, "TransformKiMultiplier", kill.getTransformKiMultiplier());
			addIfPresent(obj, "TransformTriggerPercent", kill.getTransformTriggerPercent());
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
		} else if (objective instanceof SkillObjective skill) {
			obj.addProperty("skill", skill.getSkill());
			obj.addProperty("level", skill.getLevel());
		} else if (objective instanceof DragonSummonObjective dragonSummon) {
			if (!dragonSummon.getDragonId().isBlank()) obj.addProperty("dragon", dragonSummon.getDragonId());
			if (!dragonSummon.getBallSetId().isBlank()) obj.addProperty("ball_set", dragonSummon.getBallSetId());
		} else if (objective instanceof CoordsObjective coords) {
			obj.addProperty("x", coords.getTargetPos().getX());
			obj.addProperty("y", coords.getTargetPos().getY());
			obj.addProperty("z", coords.getTargetPos().getZ());
			obj.addProperty("radius", coords.getRadius());
		} else if (objective instanceof InteractObjective interact) {
			if (interact.getEntityTypeId() != null) obj.addProperty("entity", interact.getEntityTypeId());
			if (interact.getEntityName() != null) obj.addProperty("entityName", interact.getEntityName());
		} else if (objective instanceof DeliverObjective deliver) {
			obj.addProperty("item", deliver.getItemId());
			obj.addProperty("count", deliver.getRequired());
			obj.addProperty("npcId", deliver.getNpcId());
		} else if (objective instanceof SurviveWavesObjective waves) {
			obj.addProperty("entity", waves.getEntityId());
			obj.addProperty("waves", waves.getWaves());
			obj.addProperty("mobs_per_wave", waves.getMobsPerWave());
			obj.addProperty("wave_delay_seconds", waves.getWaveDelaySeconds());
			obj.addProperty("health", waves.getHealth());
			obj.addProperty("meleeDamage", waves.getMeleeDamage());
			obj.addProperty("kiDamage", waves.getKiDamage());
			if (waves.getTextureVariant() >= 0) obj.addProperty("TextureVariant", waves.getTextureVariant());
			if (waves.getAiTier() > 0) obj.addProperty("AITier", waves.getAiTier());
			if (!waves.isCanTransform()) obj.addProperty("CanTransform", false);
		} else if (objective instanceof EscortObjective escort) {
			obj.addProperty("entity", escort.getEntityId());
			obj.addProperty("x", escort.getTargetPos().getX());
			obj.addProperty("y", escort.getTargetPos().getY());
			obj.addProperty("z", escort.getTargetPos().getZ());
			obj.addProperty("radius", escort.getRadius());
			obj.addProperty("health", escort.getEscortHealth());
			if (escort.getDimension() != null) obj.addProperty("dimension", escort.getDimension());
		} else if (objective instanceof CheckpointRaceObjective race) {
			obj.addProperty("radius", race.getRadius());
			JsonArray checkpoints = new JsonArray();
			for (var checkpoint : race.getCheckpoints()) {
				JsonObject point = new JsonObject();
				point.addProperty("x", checkpoint.getX());
				point.addProperty("y", checkpoint.getY());
				point.addProperty("z", checkpoint.getZ());
				checkpoints.add(point);
			}
			obj.add("checkpoints", checkpoints);
			if (race.getDimension() != null) obj.addProperty("dimension", race.getDimension());
		} else {
			QuestObjectiveRegistry.writeSync(objective, obj);
		}

		return obj;
	}

	private static void addIfPresent(JsonObject obj, String key, @Nullable Double value) {
		if (value != null) obj.addProperty(key, value);
	}

	private static JsonArray serializeRewards(List<QuestReward> rewards) {
		JsonArray arr = new JsonArray();
		for (QuestReward reward : rewards) {
			try {
				arr.add(serializeReward(reward));
			} catch (Exception e) {
				LogUtil.error(Env.SERVER, "SyncQuestRegistryS2C: could not serialize a '{}' reward, clients will see it as inert: {}",
						reward.getTypeKey(), e.toString());
				arr.add(placeholderJson());
			}
		}
		return arr;
	}

	private static JsonObject serializeReward(QuestReward reward) {
		if (reward.isPlaceholder()) {
			return placeholderJson();
		}

		JsonObject obj = new JsonObject();

		obj.addProperty("type", reward.getTypeKey());

		Set<Difficulty> difficulties = reward.getDifficulties();
		if (difficulties != null && difficulties.size() < Difficulty.values().length) {
			JsonArray difficultyArr = new JsonArray();
			for (Difficulty difficulty : Difficulty.values()) {
				if (difficulties.contains(difficulty)) difficultyArr.add(difficulty.name());
			}
			obj.add("difficulty", difficultyArr);
		}

		if (reward instanceof TPSReward tps) {
			obj.addProperty("amount", tps.getAmount());
		} else if (reward instanceof AttributesReward attributes) {
			obj.addProperty("amount", attributes.getAmount());
		} else if (reward instanceof AttributePointsReward attributePoints) {
			obj.addProperty("amount", attributePoints.getAmount());
		} else if (reward instanceof ItemReward item) {
			obj.addProperty("item", item.getItemId());
			obj.addProperty("count", item.getCount());
		} else if (reward instanceof GenericItemReward genericItemReward) {
			obj.add("itemReward", GsonUtils.NETWORK.toJsonTree(genericItemReward.getItemReward()));
		} else if (reward instanceof CommandReward command) {
			obj.addProperty("command", command.getCommand());
			if (command.getTranslationKey() != null && !command.getTranslationKey().isEmpty()) {
				obj.addProperty("translationKey", command.getTranslationKey());
			}
		} else if (reward instanceof SkillReward skill) {
			obj.addProperty("skill", skill.getSkill());
			obj.addProperty("level", skill.getLevel());
		} else if (reward instanceof AlignmentReward alignment) {
			obj.addProperty("amount", alignment.getAmount());
		} else if (reward instanceof TransformationReward transformation) {
			obj.addProperty("formGroup", transformation.getFormGroup());
			obj.addProperty("formName", transformation.getFormName());
			obj.addProperty("mastery", transformation.getMastery());
			obj.addProperty("stack", transformation.isStack());
		} else if (reward instanceof KiTechniqueReward kiTechnique) {
			obj.addProperty("code", kiTechnique.getTemplate().generateExportCode());
		} else {
			QuestRewardRegistry.writeSync(reward, obj);
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
					hint = QuestStructureHints.getCached(condition.getStructureId());
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
			case ALIGNMENT -> {
				if (condition.getMinAlignment() != null) {
					obj.addProperty("min", condition.getMinAlignment());
				}
				if (condition.getMaxAlignment() != null) {
					obj.addProperty("max", condition.getMaxAlignment());
				}
			}
			case SKILL -> {
				obj.addProperty("skill", condition.getSkill());
				obj.addProperty("minLevel", condition.getSkillLevel());
			}
			case RACE -> obj.addProperty("race", condition.getRace());
			case CLASS -> obj.addProperty("class", condition.getCharacterClass());
		}

		return obj;
	}

}
