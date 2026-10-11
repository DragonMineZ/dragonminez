package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.ai.AiTierResolver;
import com.dragonminez.common.init.entities.questnpc.QuestNPCEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.SagaTitleCardS2C;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.objectives.DeliverObjective;
import com.dragonminez.common.quest.objectives.ItemObjective;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.quest.rewards.KiTechniqueReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestService {

	public static final String QUEST_KEY_TAG = "dmz_quest_key";
	public static final String QUEST_OBJECTIVE_INDEX_TAG = "dmz_quest_objective_index";
	public static final String QUEST_OWNER_TAG = "dmz_quest_owner";
	private static final double TURN_IN_NPC_RANGE = 10.0;
	public static final String SAGA_ID_TAG = "dmz_saga_id";
	public static final String QUEST_TEAM_TAG = "dmz_quest_team";

	private static final long RESUMMON_MIN_INTERVAL_MS = 1500L;
	private static final Map<UUID, Long> resummonAntiSpam = new ConcurrentHashMap<>();

	private QuestService() {
	}

	public record ResolvedQuest(String questKey, Quest quest, @Nullable Saga saga) {
	}

	public record NPCQuestOptions(List<String> offerableQuestIds, List<String> turnInQuestIds,
								  List<String> inProgressQuestIds, List<LockedNpcQuest> lockedQuests) {
	}

	public record LockedNpcQuest(String questId, Component reason, long cooldownSeconds) {
	}

	@Nullable
	public static ResolvedQuest resolveQuest(String questKey) {
		if (questKey == null || questKey.isBlank()) {
			return null;
		}

		Quest quest = QuestRegistry.getQuest(questKey);
		if (quest == null) {
			return null;
		}

		Saga saga = null;
		if (quest.isSagaQuest()) {
			int separator = questKey.lastIndexOf(':');
			if (separator <= 0) {
				return null;
			}
			saga = QuestRegistry.getSaga(questKey.substring(0, separator));
			if (saga == null) {
				return null;
			}
		}

		return new ResolvedQuest(questKey, quest, saga);
	}

	public static void clearPlayer(UUID playerId) {
		resummonAntiSpam.remove(playerId);
	}

	@Nullable
	public static Component startQuest(ServerPlayer requester, String questKey) {
		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null) {
			return unavailable();
		}

		StatsData data = stats(requester);
		if (data == null) {
			return unavailable();
		}

		Component blocker = describeStartBlocker(requester, data, resolved, true);
		if (blocker != null) {
			return blocker;
		}

		Quest quest = resolved.quest();
		List<ServerPlayer> joiners = new ArrayList<>();
		joiners.add(requester);
		for (ServerPlayer member : QuestParty.nearbyMembers(requester)) {
			if (member == requester || StorageManager.isLoadPending(member)) continue;
			StatsData memberData = stats(member);
			if (memberData == null || memberData.getPlayerQuestData().isQuestAccepted(questKey)) continue;

			Component memberBlocker = describeStartBlocker(member, memberData, resolved, false);
			if (memberBlocker == null && quest.getQuestGiver() != null && !quest.getQuestGiver().isBlank()) {
				memberBlocker = NpcDispositionService.masterAlignmentBlocker(memberData, quest.getQuestGiver());
			}
			if (memberBlocker == null) {
				joiners.add(member);
			} else if (!isFinishedFor(memberData.getPlayerQuestData(), resolved)) {
				requester.sendSystemMessage(Component.translatable("message.dragonminez.quest.start.party_member_skipped",
						member.getGameProfile().getName(), memberBlocker).withStyle(ChatFormatting.YELLOW));
			}
		}

		DMZEvent.QuestStartEvent startEvent = new DMZEvent.QuestStartEvent(
				requester,
				questKey,
				resolved.saga(),
				quest,
				joiners,
				data.getPlayerQuestData().getDifficulty()
		);
		if (MinecraftForge.EVENT_BUS.post(startEvent)) {
			return unavailable();
		}

		Difficulty difficulty = startEvent.getDifficulty();
		for (ServerPlayer joiner : joiners) {
			StatsData joinerData = stats(joiner);
			if (joinerData != null) beginQuest(joiner, joinerData.getPlayerQuestData(), resolved, difficulty);
		}

		int partySize = Math.max(1, QuestParty.participants(requester, questKey).size());
		for (ServerPlayer joiner : joiners) {
			PlayerQuestData joinerQuests = QuestParty.questData(joiner);
			if (joinerQuests != null) quest.initializeObjectiveRequirements(joinerQuests, questKey, partySize);
		}

		try {
			spawnKillObjectives(requester, resolved, partySize, difficulty);
		} catch (Exception exception) {
			LogUtil.error(Env.SERVER, "Failed to spawn kill objectives for quest '" + questKey
					+ "' started by " + requester.getGameProfile().getName(), exception);
		}

		for (ServerPlayer joiner : joiners) {
			NetworkHandler.sendToPlayer(StoryToastS2C.questStarted(questKey), joiner);
			syncQuestState(joiner);
		}
		return null;
	}

	@Nullable
	public static Component resummonQuest(ServerPlayer requester, String questKey) {
		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null) {
			return unavailable();
		}

		PlayerQuestData pqd = QuestParty.questData(requester);
		if (pqd == null || pqd.getQuestStatus(questKey) != PlayerQuestData.QuestStatus.ACCEPTED) {
			return unavailable();
		}

		long now = System.currentTimeMillis();
		Long last = resummonAntiSpam.get(requester.getUUID());
		if (last != null && now - last < RESUMMON_MIN_INTERVAL_MS) {
			return null;
		}
		resummonAntiSpam.put(requester.getUUID(), now);

		int partySize = Math.max(1, QuestParty.participants(requester, questKey).size());
		try {
			spawnKillObjectives(requester, resolved, partySize, pqd.getQuestDifficulty(questKey));
		} catch (Exception exception) {
			LogUtil.error(Env.SERVER, "Failed to re-summon kill objectives for quest '" + questKey
					+ "' requested by " + requester.getGameProfile().getName(), exception);
		}
		return null;
	}

	@Nullable
	public static Component turnInQuest(ServerPlayer requester, String questKey, @Nullable String npcId) {
		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null || npcId == null || npcId.isBlank() || !isTurnInNpcNearby(requester, npcId)) {
			return unavailable();
		}

		Quest quest = resolved.quest();
		if (!requiresTurnInAction(quest) || !npcId.equalsIgnoreCase(quest.getTurnIn())) {
			return unavailable();
		}

		PlayerQuestData pqd = QuestParty.questData(requester);
		if (pqd == null) {
			return unavailable();
		}

		if (pqd.isQuestCompleted(questKey)) {
			return claimRewardsForRequester(requester, quest, questKey) ? null : unavailable();
		}
		if (!pqd.isQuestAccepted(questKey)) {
			return unavailable();
		}

		refreshItemObjectives(requester, pqd, resolved);
		if (!areObjectivesReadyExceptDeliveries(pqd, questKey, quest, npcId)) {
			return Component.translatable("message.dragonminez.quest.start.locked");
		}

		Component deliveryBlocker = describeDeliveryBlocker(requester, pqd, questKey, quest, npcId);
		if (deliveryBlocker != null) {
			return deliveryBlocker;
		}

		List<ServerPlayer> participants = QuestParty.participants(requester, questKey);
		DMZEvent.QuestTurnInEvent turnInEvent = new DMZEvent.QuestTurnInEvent(
				requester,
				questKey,
				resolved.saga(),
				quest,
				participants,
				npcId
		);
		if (MinecraftForge.EVENT_BUS.post(turnInEvent)) {
			return unavailable();
		}

		performDeliveries(requester, pqd, resolved, npcId, participants);
		if (!areDeliveriesComplete(pqd, questKey, quest, npcId)) {
			syncQuestState(requester);
			return Component.translatable("message.dragonminez.quest.start.locked");
		}

		if (!finishTurnIn(requester, pqd, resolved, npcId, participants)) {
			syncQuestState(requester);
			return unavailable();
		}

		for (ServerPlayer participant : participants) {
			if (participant == requester) continue;
			PlayerQuestData participantData = QuestParty.questData(participant);
			if (participantData == null || !participantData.isQuestAccepted(questKey)) continue;
			refreshItemObjectives(participant, participantData, resolved);
			if (!areObjectivesReadyExceptDeliveries(participantData, questKey, quest, npcId)) continue;
			if (!areDeliveriesComplete(participantData, questKey, quest, npcId)) continue;
			finishTurnIn(participant, participantData, resolved, npcId, participants);
		}

		requester.displayClientMessage(
				Component.translatable("command.dragonminez.story.sidequest.turned_in", questKey), false);
		claimRewardsForRequester(requester, quest, questKey);
		return null;
	}

	private static boolean finishTurnIn(ServerPlayer player, PlayerQuestData pqd, ResolvedQuest resolved, String npcId,
										List<ServerPlayer> participants) {
		Quest quest = resolved.quest();
		String questKey = resolved.questKey();
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			if (objective instanceof TalkToObjective talkTo && npcId.equalsIgnoreCase(talkTo.getNpcId())) {
				setObjectiveProgress(player, pqd, resolved, participants, i, quest.getObjectiveRequired(pqd, questKey, i));
			}
		}

		DMZEvent.QuestCompletedEvent completeEvent = new DMZEvent.QuestCompletedEvent(
				player,
				questKey,
				resolved.saga(),
				quest,
				participants
		);
		if (MinecraftForge.EVENT_BUS.post(completeEvent)) {
			return false;
		}

		pqd.completeQuest(questKey);
		if (questKey.equals(pqd.getTrackedQuestId())) {
			pqd.setTrackedQuestId(null);
		}
		NetworkHandler.sendToPlayer(StoryToastS2C.questComplete(questKey), player);
		syncQuestState(player);
		return true;
	}

	private static boolean isTurnInNpcNearby(ServerPlayer player, String npcId) {
		AABB range = player.getBoundingBox().inflate(TURN_IN_NPC_RANGE);
		if (!player.serverLevel().getEntitiesOfClass(MastersEntity.class, range,
				master -> npcId.equalsIgnoreCase(master.getMasterName())).isEmpty()) return true;
		return !player.serverLevel().getEntitiesOfClass(QuestNPCEntity.class, range,
				npc -> npcId.equalsIgnoreCase(npc.getNpcId())).isEmpty();
	}

	public static void claimRewards(ServerPlayer requester, String questKey) {
		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null) {
			return;
		}
		if (resolved.quest().getClaimMode() == Quest.ClaimMode.NPC_ONLY) {
			requester.sendSystemMessage(Component.translatable("quest.dmz.reward.npc_only")
					.withStyle(ChatFormatting.RED));
			return;
		}

		PlayerQuestData pqd = QuestParty.questData(requester);
		if (pqd == null || !pqd.isQuestCompleted(questKey)) {
			return;
		}

		if (claimAvailableRewards(requester, resolved.quest(), questKey, pqd)) {
			syncQuestState(requester);
		}
	}

	public static void claimAllRewards(ServerPlayer requester) {
		PlayerQuestData pqd = QuestParty.questData(requester);
		if (pqd == null) {
			return;
		}

		boolean anyClaimed = false;
		for (String questKey : new ArrayList<>(pqd.getCompletedQuestIds())) {
			ResolvedQuest resolved = resolveQuest(questKey);
			if (resolved == null) {
				continue;
			}
			if (resolved.quest().getClaimMode() == Quest.ClaimMode.NPC_ONLY) {
				continue;
			}
			anyClaimed |= claimAvailableRewards(requester, resolved.quest(), questKey, pqd);
		}

		if (anyClaimed) {
			syncQuestState(requester);
		}
	}

	public static boolean isTurnInReady(PlayerQuestData pqd, String questKey, Quest quest) {
		if (pqd == null || quest == null || questKey == null || questKey.isBlank()) {
			return false;
		}

		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (isTurnInTalk(quest, quest.getObjectives().get(i))) {
				continue;
			}
			if (pqd.getObjectiveProgress(questKey, i) < quest.getObjectiveRequired(pqd, questKey, i)) {
				return false;
			}
		}

		return true;
	}

	public static boolean isTurnInReady(ServerPlayer player, PlayerQuestData pqd, String questKey, Quest quest) {
		if (player == null) {
			return isTurnInReady(pqd, questKey, quest);
		}
		if (pqd == null || quest == null || !requiresTurnInAction(quest)) {
			return false;
		}

		String npcId = quest.getTurnIn();
		Map<net.minecraft.world.item.Item, Integer> needed = new HashMap<>();
		Map<net.minecraft.world.item.Item, DeliverObjective> sample = new HashMap<>();
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			if (isTurnInTalk(quest, objective)) {
				continue;
			}
			int required = quest.getObjectiveRequired(pqd, questKey, i);
			int progress = pqd.getObjectiveProgress(questKey, i);
			if (objective instanceof ItemObjective itemObjective) {
				progress = Math.min(QuestParty.countItemById(player, itemObjective.getItemId()), required);
			}
			if (progress >= required) {
				continue;
			}
			if (objective instanceof DeliverObjective deliver && deliver.matchesNpc(npcId) && deliver.getItem() != null) {
				needed.merge(deliver.getItem(), required - progress, Integer::sum);
				sample.putIfAbsent(deliver.getItem(), deliver);
				continue;
			}
			return false;
		}
		for (Map.Entry<net.minecraft.world.item.Item, Integer> entry : needed.entrySet()) {
			if (partyHeld(player, sample.get(entry.getKey())) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	public static boolean isTurnInTalk(Quest quest, QuestObjective objective) {
		return objective instanceof TalkToObjective talkTo && requiresTurnInAction(quest)
				&& talkTo.getNpcId() != null && talkTo.getNpcId().equalsIgnoreCase(quest.getTurnIn());
	}

	public static boolean requiresTurnInAction(Quest quest) {
		return quest != null && quest.getTurnIn() != null && !quest.getTurnIn().isBlank();
	}

	public static NPCQuestOptions collectNpcQuestOptions(String npcId, StatsData data) {
		List<String> offerableQuestIds = new ArrayList<>();
		List<String> turnInQuestIds = new ArrayList<>();
		List<String> inProgressQuestIds = new ArrayList<>();
		List<LockedNpcQuest> lockedQuests = new ArrayList<>();

		if (npcId == null || npcId.isBlank() || data == null) {
			return new NPCQuestOptions(offerableQuestIds, turnInQuestIds, inProgressQuestIds, lockedQuests);
		}

		PlayerQuestData pqd = data.getPlayerQuestData();
		ServerPlayer player = data.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
		Map<String, Quest> allQuests = QuestRegistry.getAllQuests();
		long now = System.currentTimeMillis();

		for (String questId : QuestRegistry.getQuestIdsByGiver(npcId)) {
			Quest quest = allQuests.get(questId);
			if (quest == null) {
				continue;
			}
			if (pqd.isQuestCompleted(questId) && !quest.isRepeatReady(pqd.getLastCompletedRealMs(questId), now)) {
				if (quest.isRepeatable() && !pqd.isQuestAccepted(questId)) {
					long elapsedSeconds = Math.max(0L, now - pqd.getLastCompletedRealMs(questId)) / 1000L;
					long remaining = Math.max(1L, quest.getRepeatCooldownSeconds() - elapsedSeconds);
					lockedQuests.add(new LockedNpcQuest(questId,
							Component.translatable("gui.dragonminez.dialogue.quest.repeat_cooldown"), remaining));
				}
				continue;
			}

			if (pqd.isQuestAccepted(questId)) {
				inProgressQuestIds.add(questId);
			} else if (isOfferableNpcQuest(questId, quest, data)) {
				offerableQuestIds.add(questId);
			} else if (!quest.isSecret()) {
				Component reason = QuestAvailabilityChecker.describeAvailabilityFailure(quest, data);
				lockedQuests.add(new LockedNpcQuest(questId,
						reason != null ? reason : Component.translatable("message.dragonminez.quest.start.unavailable"), 0L));
			}
		}

		for (String questId : QuestRegistry.getQuestIdsByTurnIn(npcId)) {
			Quest quest = allQuests.get(questId);
			if (quest == null) {
				continue;
			}
			if (pqd.isQuestCompleted(questId)) {
				if (hasUnclaimedRewards(pqd, questId, quest) && !turnInQuestIds.contains(questId)) {
					turnInQuestIds.add(questId);
				}
				continue;
			}
			if (!pqd.isQuestAccepted(questId)) {
				continue;
			}
			if (isTurnInReady(player, pqd, questId, quest) && !turnInQuestIds.contains(questId)) {
				turnInQuestIds.add(questId);
			}
		}

		return new NPCQuestOptions(offerableQuestIds, turnInQuestIds, inProgressQuestIds, lockedQuests);
	}

	@Nullable
	public static Component deliverOnInteract(ServerPlayer player, PlayerQuestData pqd, ResolvedQuest resolved,
											  String npcId, int objectiveIndex) {
		Quest quest = resolved.quest();
		String questKey = resolved.questKey();
		if (!(quest.getObjectives().get(objectiveIndex) instanceof DeliverObjective deliver)) {
			return null;
		}
		int required = quest.getObjectiveRequired(pqd, questKey, objectiveIndex);
		int remaining = required - pqd.getObjectiveProgress(questKey, objectiveIndex);
		if (remaining <= 0) {
			return null;
		}

		int held = QuestParty.countItem(player, deliver.getItem());
		if (held < remaining) {
			return partyHeld(player, deliver) >= remaining
					? carryAllMessage(deliver, remaining, held)
					: null;
		}

		QuestParty.consumeItem(player, deliver.getItem(), remaining);
		List<ServerPlayer> participants = QuestParty.participants(player, questKey);
		for (ServerPlayer participant : participants) {
			PlayerQuestData participantData = participant == player ? pqd : QuestParty.questData(participant);
			if (participantData == null) continue;
			setObjectiveProgress(participant, participantData, resolved, participants, objectiveIndex,
					quest.getObjectiveRequired(participantData, questKey, objectiveIndex));
		}
		return null;
	}

	private static boolean areObjectivesReadyExceptDeliveries(PlayerQuestData pqd, String questKey, Quest quest, String npcId) {
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			if (isTurnInTalk(quest, objective)) continue;
			if (objective instanceof DeliverObjective deliver && deliver.matchesNpc(npcId)) continue;
			if (pqd.getObjectiveProgress(questKey, i) < quest.getObjectiveRequired(pqd, questKey, i)) return false;
		}
		return true;
	}

	private static boolean areDeliveriesComplete(PlayerQuestData pqd, String questKey, Quest quest, String npcId) {
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (quest.getObjectives().get(i) instanceof DeliverObjective deliver && deliver.matchesNpc(npcId)
					&& pqd.getObjectiveProgress(questKey, i) < quest.getObjectiveRequired(pqd, questKey, i)) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	private static Component describeDeliveryBlocker(ServerPlayer player, PlayerQuestData pqd, String questKey,
													 Quest quest, String npcId) {
		Map<net.minecraft.world.item.Item, Integer> needed = new HashMap<>();
		Map<net.minecraft.world.item.Item, DeliverObjective> sample = new HashMap<>();
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (!(quest.getObjectives().get(i) instanceof DeliverObjective deliver) || !deliver.matchesNpc(npcId)) continue;
			int remaining = quest.getObjectiveRequired(pqd, questKey, i) - pqd.getObjectiveProgress(questKey, i);
			if (remaining <= 0 || deliver.getItem() == null) continue;
			needed.merge(deliver.getItem(), remaining, Integer::sum);
			sample.putIfAbsent(deliver.getItem(), deliver);
		}

		for (Map.Entry<net.minecraft.world.item.Item, Integer> entry : needed.entrySet()) {
			int held = QuestParty.countItem(player, entry.getKey());
			if (held >= entry.getValue()) continue;
			DeliverObjective deliver = sample.get(entry.getKey());
			if (partyHeld(player, deliver) >= entry.getValue()) {
				return carryAllMessage(deliver, entry.getValue(), held);
			}
			return Component.translatable("message.dragonminez.quest.deliver.missing",
					entry.getValue(), deliver.getItem().getDescription(), held);
		}
		return null;
	}

	private static void performDeliveries(ServerPlayer player, PlayerQuestData pqd, ResolvedQuest resolved, String npcId,
										  List<ServerPlayer> participants) {
		Quest quest = resolved.quest();
		String questKey = resolved.questKey();
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (!(quest.getObjectives().get(i) instanceof DeliverObjective deliver) || !deliver.matchesNpc(npcId)) continue;
			int remaining = quest.getObjectiveRequired(pqd, questKey, i) - pqd.getObjectiveProgress(questKey, i);
			if (remaining <= 0) continue;
			if (QuestParty.consumeItem(player, deliver.getItem(), remaining) < remaining) continue;
			for (ServerPlayer participant : participants) {
				PlayerQuestData participantData = participant == player ? pqd : QuestParty.questData(participant);
				if (participantData == null) continue;
				setObjectiveProgress(participant, participantData, resolved, participants, i,
						quest.getObjectiveRequired(participantData, questKey, i));
			}
		}
	}

	private static int partyHeld(ServerPlayer player, DeliverObjective deliver) {
		if (deliver == null || deliver.getItem() == null) return 0;
		int total = 0;
		for (ServerPlayer member : QuestParty.nearbyMembers(player)) {
			total += QuestParty.countItem(member, deliver.getItem());
		}
		return total;
	}

	private static Component carryAllMessage(DeliverObjective deliver, int needed, int held) {
		return Component.translatable("message.dragonminez.quest.deliver.carry_all",
				needed, deliver.getItem().getDescription(), held);
	}

	public static void refreshItemObjectives(ServerPlayer player, PlayerQuestData pqd, ResolvedQuest resolved) {
		Quest quest = resolved.quest();
		String questKey = resolved.questKey();
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (!(quest.getObjectives().get(i) instanceof ItemObjective itemObjective)) continue;
			int required = quest.getObjectiveRequired(pqd, questKey, i);
			int held = Math.min(QuestParty.countItemById(player, itemObjective.getItemId()), required);
			if (held != pqd.getObjectiveProgress(questKey, i)) {
				setObjectiveProgress(player, pqd, resolved, List.of(player), i, held);
			}
		}
	}

	private static boolean setObjectiveProgress(ServerPlayer player, PlayerQuestData pqd, ResolvedQuest resolved,
												List<ServerPlayer> partyMembers, int objectiveIndex, int newProgress) {
		Quest quest = resolved.quest();
		int current = pqd.getObjectiveProgress(resolved.questKey(), objectiveIndex);
		if (current == newProgress) {
			return false;
		}

		int required = objectiveIndex >= 0 && objectiveIndex < quest.getObjectives().size()
				? quest.getObjectiveRequired(pqd, resolved.questKey(), objectiveIndex)
				: 0;
		DMZEvent.QuestObjectiveProgressEvent progressEvent = new DMZEvent.QuestObjectiveProgressEvent(
				player,
				resolved.questKey(),
				resolved.saga(),
				quest,
				partyMembers,
				objectiveIndex,
				current,
				newProgress,
				required
		);
		if (MinecraftForge.EVENT_BUS.post(progressEvent)) {
			return false;
		}

		int updated = Math.max(0, progressEvent.getNewProgress());
		if (updated == current) {
			return false;
		}

		pqd.setObjectiveProgress(resolved.questKey(), objectiveIndex, updated);
		return true;
	}

	private static Component describeStartBlocker(ServerPlayer player, StatsData data, ResolvedQuest resolved, boolean positional) {
		PlayerQuestData pqd = data.getPlayerQuestData();
		String questKey = resolved.questKey();
		Quest quest = resolved.quest();

		PlayerQuestData.QuestStatus status = pqd.getQuestStatus(questKey);
		boolean restartingFailed = status == PlayerQuestData.QuestStatus.FAILED;
		boolean restartingRepeat = status == PlayerQuestData.QuestStatus.SUCCESS
				&& quest.isRepeatReady(pqd.getLastCompletedRealMs(questKey), System.currentTimeMillis());
		if (status == PlayerQuestData.QuestStatus.ACCEPTED
				|| (status == PlayerQuestData.QuestStatus.SUCCESS && !restartingRepeat)) {
			return Component.translatable("message.dragonminez.quest.start.already_active");
		}

		if (!restartingFailed && !isQuestAvailableToStart(resolved, data)) {
			Component reason = QuestAvailabilityChecker.describeAvailabilityFailure(quest, data);
			return reason != null ? reason : Component.translatable("message.dragonminez.quest.start.locked");
		}

		if (positional) {
			return restartingFailed
					? QuestAvailabilityChecker.describeStartRequirementFailure(quest, questKey, player, data)
					: QuestAvailabilityChecker.describeQuestStartBlocker(quest, questKey, player, data);
		}
		return restartingFailed
				? QuestAvailabilityChecker.describeNonPositionalStartRequirementFailure(quest, questKey, player, data)
				: QuestAvailabilityChecker.describeNonPositionalStartBlocker(quest, questKey, player, data);
	}

	private static boolean isFinishedFor(PlayerQuestData pqd, ResolvedQuest resolved) {
		String questKey = resolved.questKey();
		return pqd.isQuestCompleted(questKey)
				&& !resolved.quest().isRepeatReady(pqd.getLastCompletedRealMs(questKey), System.currentTimeMillis());
	}

	private static void beginQuest(ServerPlayer player, PlayerQuestData pqd, ResolvedQuest resolved, Difficulty difficulty) {
		String questKey = resolved.questKey();
		Quest quest = resolved.quest();
		boolean firstSagaStart = resolved.saga() != null && isFirstSagaQuestStart(resolved.saga(), questKey, pqd);

		PlayerQuestData.QuestStatus status = pqd.getQuestStatus(questKey);
		if (status == PlayerQuestData.QuestStatus.SUCCESS) {
			claimAvailableRewards(player, quest, questKey, pqd);
			pqd.restartCompletedQuest(questKey);
		} else if (status == PlayerQuestData.QuestStatus.FAILED) {
			pqd.restartFailedQuest(questKey);
		}

		pqd.acceptQuest(questKey);
		pqd.setQuestAcceptedGameTime(questKey, player.serverLevel().getGameTime());
		pqd.setAcceptedDimension(questKey, player.level().dimension().location().toString());
		pqd.setObjectiveSignature(questKey, objectiveSignature(quest));
		pqd.setQuestDifficulty(questKey, difficulty);
		pqd.setTrackedQuestId(questKey);

		if (firstSagaStart) {
			NetworkHandler.sendToPlayer(new SagaTitleCardS2C(resolved.saga().getName()), player);
		}
	}

	public static String objectiveSignature(Quest quest) {
		StringBuilder builder = new StringBuilder();
		for (QuestObjective objective : quest.getObjectives()) {
			if (!builder.isEmpty()) builder.append('|');
			builder.append(objective.getTypeKey()).append(':').append(objective.getRequired());
		}
		return builder.toString();
	}

	private static boolean isFirstSagaQuestStart(Saga saga, String startedQuestKey, PlayerQuestData pqd) {
		for (Quest sagaQuest : saga.getQuests()) {
			String key = PlayerQuestData.sagaQuestKey(saga.getId(), sagaQuest.getId());
			if (key.equals(startedQuestKey)) continue;
			if (pqd.getQuestStatus(key) != PlayerQuestData.QuestStatus.NOT_STARTED) return false;
		}
		return true;
	}

	private static boolean isQuestAvailableToStart(ResolvedQuest resolved, StatsData data) {
		if (!resolved.quest().isSagaQuest()) {
			return QuestAvailabilityChecker.isAvailable(resolved.quest(), data);
		}

		Saga saga = resolved.saga();
		if (saga == null) {
			return false;
		}

		int questIndex = saga.getQuests().indexOf(resolved.quest());
		return questIndex >= 0 && QuestAvailabilityChecker.isSagaQuestAvailable(resolved.quest(), saga, questIndex, data);
	}

	private static boolean hasUnclaimedRewards(PlayerQuestData pqd, String questKey, Quest quest) {
		List<QuestReward> rewards = quest.getRewards();
		Difficulty questDifficulty = pqd.getQuestDifficulty(questKey);
		for (int i = 0; i < rewards.size(); i++) {
			if (!rewards.get(i).isUnlockedFor(questDifficulty)) {
				continue;
			}
			if (!pqd.isRewardClaimed(questKey, i)) {
				return true;
			}
		}
		return false;
	}

	private static boolean claimRewardsForRequester(ServerPlayer requester, Quest quest, String questKey) {
		PlayerQuestData pqd = QuestParty.questData(requester);
		if (pqd == null || !pqd.isQuestCompleted(questKey)) {
			return false;
		}
		if (claimAvailableRewards(requester, quest, questKey, pqd)) {
			syncQuestState(requester);
			return true;
		}
		return false;
	}

	private static boolean claimAvailableRewards(ServerPlayer rewardTarget, Quest quest, String questKey, PlayerQuestData pqd) {
		boolean anyClaimed = false;
		boolean techniqueSlotsWarned = false;
		List<QuestReward> rewards = quest.getRewards();
		ResolvedQuest resolved = resolveQuest(questKey);
		Saga saga = resolved != null ? resolved.saga() : null;
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(rewardTarget);
		Difficulty questDifficulty = pqd.getQuestDifficulty(questKey);
		for (int i = 0; i < rewards.size(); i++) {
			QuestReward reward = rewards.get(i);
			if (pqd.isRewardClaimed(questKey, i)) {
				continue;
			}
			if (!reward.isUnlockedFor(questDifficulty)) {
				continue;
			}
			if (reward instanceof KiTechniqueReward technique && !technique.canGrant(rewardTarget)) {
				if (!techniqueSlotsWarned) {
					rewardTarget.sendSystemMessage(Component.translatable("quest.dmz.reward.technique_slots_full")
							.withStyle(ChatFormatting.RED));
					techniqueSlotsWarned = true;
				}
				continue;
			}
			DMZEvent.QuestRewardClaimEvent rewardEvent = new DMZEvent.QuestRewardClaimEvent(
					rewardTarget,
					questKey,
					saga,
					quest,
					partyMembers,
					i
			);
			if (MinecraftForge.EVENT_BUS.post(rewardEvent)) {
				continue;
			}
			pqd.claimReward(questKey, i);
			try {
				reward.giveReward(rewardTarget, pqd.rewardMultiplierFor(reward, questDifficulty));
			} catch (Exception exception) {
				LogUtil.error(Env.SERVER, "Quest reward " + i + " of '" + questKey + "' failed for "
						+ rewardTarget.getGameProfile().getName(), exception);
			}
			anyClaimed = true;
		}
		return anyClaimed;
	}

	private static boolean isOfferableNpcQuest(String questId, Quest quest, StatsData data) {
		if (!quest.isSagaQuest()) {
			return QuestAvailabilityChecker.isAvailable(quest, data);
		}

		ResolvedQuest resolved = resolveQuest(questId);
		if (resolved == null || resolved.saga() == null) {
			return false;
		}

		int questIndex = resolved.saga().getQuests().indexOf(quest);
		return questIndex >= 0 && QuestAvailabilityChecker.isSagaQuestAvailable(quest, resolved.saga(), questIndex, data);
	}

	private static Map<Integer, Integer> countAliveQuestMobs(ServerLevel level, String questKey, Set<String> ownerIds) {
		Map<Integer, Integer> alive = new HashMap<>();
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
			CompoundTag tags = entity.getPersistentData();
			if (!questKey.equals(tags.getString(QUEST_KEY_TAG)) || !ownerIds.contains(tags.getString(QUEST_OWNER_TAG))) continue;
			alive.merge(tags.getInt(QUEST_OBJECTIVE_INDEX_TAG), 1, Integer::sum);
		}
		return alive;
	}

	private static void spawnKillObjectives(ServerPlayer requester, ResolvedQuest resolved, int partySize, Difficulty difficulty) {
		Quest quest = resolved.quest();
		String questKey = resolved.questKey();
		PlayerQuestData pqd = QuestParty.questData(requester);
		if (pqd == null) {
			return;
		}

		Set<String> ownerIds = new HashSet<>();
		for (UUID memberId : PartyManager.getPartyMemberIds(requester)) {
			ownerIds.add(memberId.toString());
		}
		ownerIds.add(requester.getStringUUID());
		Map<Integer, Integer> alive = countAliveQuestMobs(requester.serverLevel(), questKey, ownerIds);

		int[] toSpawn = new int[quest.getObjectives().size()];
		int totalToSpawn = 0;
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (!(quest.getObjectives().get(i) instanceof KillObjective killObjective)
					|| killObjective.getSpawnMode() != KillObjective.SpawnMode.QUEST) {
				continue;
			}
			int remaining = quest.getObjectiveRequired(pqd, questKey, i) - pqd.getObjectiveProgress(questKey, i);
			toSpawn[i] = Math.max(0, remaining - alive.getOrDefault(i, 0));
			totalToSpawn += toSpawn[i];
		}

		String questTeam = totalToSpawn > 1
				? questKey + "@" + requester.getStringUUID() + "@" + System.nanoTime()
				: null;

		for (int i = 0; i < quest.getObjectives().size(); i++) {
			if (!(quest.getObjectives().get(i) instanceof KillObjective killObjective) || toSpawn[i] <= 0) {
				continue;
			}

			for (int j = 0; j < toSpawn[i]; j++) {
				EntityType<?> entityType = killObjective.resolveEntityType();
				if (entityType == null) {
					continue;
				}

				Entity entity = entityType.create(requester.level());
				if (entity == null) {
					continue;
				}

				boolean oneAtATime = entity instanceof DBSagasEntity sagasEntity && sagasEntity.questSpawnsOneAtATime();
				if (oneAtATime && alive.getOrDefault(i, 0) > 0) {
					entity.discard();
					break;
				}

				positionQuestEntity(requester, entity);

				if (difficulty != null && difficulty != Difficulty.NORMAL) {
					entity.getPersistentData().putString("dmz_difficulty", difficulty.name());
				}
				if (resolved.saga() != null) {
					entity.getPersistentData().putString(SAGA_ID_TAG, resolved.saga().getId());
				}

				entity.getPersistentData().putString(QUEST_KEY_TAG, questKey);
				entity.getPersistentData().putInt(QUEST_OBJECTIVE_INDEX_TAG, i);
				entity.getPersistentData().putString(QUEST_OWNER_TAG, requester.getStringUUID());
				if (questTeam != null) {
					entity.getPersistentData().putString(QUEST_TEAM_TAG, questTeam);
				}
				entity.getPersistentData().putDouble("dmz_quest_hp", quest.getScaledKillHealth(killObjective, partySize));
				entity.getPersistentData().putDouble("dmz_quest_melee", quest.getScaledKillMeleeDamage(killObjective, partySize));
				entity.getPersistentData().putDouble("dmz_quest_ki", quest.getScaledKillKiDamage(killObjective, partySize));
				if (killObjective.getTextureVariant() >= 0) {
					entity.getPersistentData().putInt("dmz_quest_texture_variant", killObjective.getTextureVariant());
				}
				int aiTier = killObjective.getAiTier() > 0
						? killObjective.getAiTier()
						: AiTierResolver.storyTier(difficulty, entityType);
				entity.getPersistentData().putInt("dmz_quest_ai_tier", aiTier);
				if (!killObjective.isCanTransform()) {
					entity.getPersistentData().putBoolean("dmz_quest_no_transform", true);
				}

				// Per-quest transform tuning. Absolute values are party-scaled here (difficulty is
				// applied later, when the new form spawns); multipliers/trigger pass through as-is.
				Double transformHealth = quest.getScaledTransformHealth(killObjective, partySize);
				if (transformHealth != null) {
					entity.getPersistentData().putDouble("dmz_quest_tf_hp_abs", transformHealth);
				}
				Double transformMelee = quest.getScaledTransformMeleeDamage(killObjective, partySize);
				if (transformMelee != null) {
					entity.getPersistentData().putDouble("dmz_quest_tf_melee_abs", transformMelee);
				}
				Double transformKi = quest.getScaledTransformKiDamage(killObjective, partySize);
				if (transformKi != null) {
					entity.getPersistentData().putDouble("dmz_quest_tf_ki_abs", transformKi);
				}
				if (killObjective.getTransformHealthMultiplier() != null) {
					entity.getPersistentData().putDouble("dmz_quest_tf_hp_mult", killObjective.getTransformHealthMultiplier());
				}
				if (killObjective.getTransformMeleeMultiplier() != null) {
					entity.getPersistentData().putDouble("dmz_quest_tf_melee_mult", killObjective.getTransformMeleeMultiplier());
				}
				if (killObjective.getTransformKiMultiplier() != null) {
					entity.getPersistentData().putDouble("dmz_quest_tf_ki_mult", killObjective.getTransformKiMultiplier());
				}
				if (killObjective.getTransformTriggerPercent() != null) {
					entity.getPersistentData().putDouble("dmz_quest_tf_trigger", killObjective.getTransformTriggerPercent());
				}

				if (entity instanceof Mob mob) {
					mob.setTarget(requester);
				}

				requester.serverLevel().addFreshEntity(entity);
				if (oneAtATime) {
					break;
				}
			}
		}
	}

	public static boolean hasPendingKills(ServerLevel level, CompoundTag questTags) {
		if (!questTags.contains(QUEST_KEY_TAG) || !questTags.contains(QUEST_OBJECTIVE_INDEX_TAG)
				|| !questTags.contains(QUEST_OWNER_TAG)) {
			return false;
		}

		ServerPlayer owner;
		try {
			owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(questTags.getString(QUEST_OWNER_TAG)));
		} catch (IllegalArgumentException e) {
			return false;
		}
		if (owner == null) return false;

		String questKey = questTags.getString(QUEST_KEY_TAG);
		int objectiveIndex = questTags.getInt(QUEST_OBJECTIVE_INDEX_TAG);
		Quest quest = QuestRegistry.getQuest(questKey);
		if (quest == null || objectiveIndex < 0 || objectiveIndex >= quest.getObjectives().size()) return false;

		for (ServerPlayer member : PartyManager.getAllPartyMembers(owner)) {
			PlayerQuestData pqd = QuestParty.questData(member);
			if (pqd == null || !pqd.isQuestAccepted(questKey)) continue;
			if (pqd.getObjectiveProgress(questKey, objectiveIndex) < quest.getObjectiveRequired(pqd, questKey, objectiveIndex)) {
				return true;
			}
		}
		return false;
	}

	// Quest-spawned enemies used to appear on top of the player. Instead, drop them ~10 blocks away in a
	// random direction, on a spot where the entity's bounding box actually fits (so it doesn't suffocate
	// inside walls) and has ground beneath it. Falls back to the player's position if no safe spot is found.
	private static final double QUEST_SPAWN_DISTANCE = 10.0;
	private static final int QUEST_SPAWN_ANGLE_ATTEMPTS = 16;

	private static void positionQuestEntity(ServerPlayer requester, Entity entity) {
		ServerLevel level = requester.serverLevel();
		RandomSource random = requester.getRandom();

		for (int attempt = 0; attempt < QUEST_SPAWN_ANGLE_ATTEMPTS; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double dist = QUEST_SPAWN_DISTANCE + (random.nextDouble() - 0.5) * 2.0;
			double targetX = requester.getX() + Math.cos(angle) * dist;
			double targetZ = requester.getZ() + Math.sin(angle) * dist;

			Double safeY = findSafeSpawnY(level, entity, targetX, targetZ, requester.getY());
			if (safeY != null) {
				entity.setPos(targetX, safeY, targetZ);
				return;
			}
		}

		// No safe spot ~10 blocks away: keep the entity spawnable by placing it on the player.
		entity.setPos(requester.getX(), requester.getY(), requester.getZ());
	}

	@Nullable
	private static Double findSafeSpawnY(ServerLevel level, Entity entity, double x, double z, double baseY) {
		int startY = Mth.floor(baseY) + 4;
		int minY = Mth.floor(baseY) - 8;

		for (int y = startY; y >= minY; y--) {
			entity.setPos(x, y, z);
			// The entity's full bounding box must be clear of blocks, otherwise it would suffocate in a wall.
			if (!level.noCollision(entity, entity.getBoundingBox())) {
				continue;
			}
			// Require solid ground directly below so it doesn't spawn floating in mid-air or a cave ceiling.
			BlockPos ground = BlockPos.containing(x, y, z).below();
			BlockState groundState = level.getBlockState(ground);
			if (groundState.getCollisionShape(level, ground).isEmpty()) {
				continue;
			}
			return (double) y;
		}
		return null;
	}

	public static void syncQuestState(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
	}

	private static StatsData stats(ServerPlayer player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player).resolve().orElse(null);
	}

	private static Component unavailable() {
		return Component.translatable("message.dragonminez.quest.start.unavailable");
	}
}
