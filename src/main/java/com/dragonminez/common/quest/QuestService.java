package com.dragonminez.common.quest;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.StoryToastS2C;
import com.dragonminez.common.quest.objectives.KillObjective;
import com.dragonminez.common.quest.objectives.TalkToObjective;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Es como el QuestRegistry.java pero en vez de ser puras tecnicalidades, ahora usa cosas de MC directamente. Este es como
// el "bridge" para los packets.
public final class QuestService {

	public static final String QUEST_KEY_TAG = "dmz_quest_key";
	public static final String QUEST_OBJECTIVE_INDEX_TAG = "dmz_quest_objective_index";
	public static final String QUEST_OWNER_TAG = "dmz_quest_owner";
	public static final String SAGA_ID_TAG = "dmz_saga_id";

	private QuestService() {
	}

	public record ResolvedQuest(String questKey, Quest quest, @Nullable Saga saga) {
	}

	public record NPCQuestOptions(List<String> offerableQuestIds, List<String> turnInQuestIds,
								  List<String> inProgressQuestIds) {
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

	@Nullable
	public static Component startQuest(ServerPlayer requester, String questKey, boolean isHardMode) {
		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		ServerPlayer controller = PartyManager.resolveQuestController(requester);
		if (controller == null) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		return StatsProvider.get(StatsCapability.INSTANCE, controller)
				.map(data -> startQuest(requester, controller, resolved, data, isHardMode))
				.orElse(Component.translatable("message.dragonminez.quest.start.unavailable"));
	}

	@Nullable
	public static Component turnInQuest(ServerPlayer requester, String questKey, @Nullable String npcId) {
		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null || npcId == null || npcId.isBlank()) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		ServerPlayer controller = PartyManager.resolveQuestController(requester);
		if (controller == null) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		return StatsProvider.get(StatsCapability.INSTANCE, controller)
				.map(data -> turnInQuest(requester, controller, resolved, data, npcId))
				.orElse(Component.translatable("message.dragonminez.quest.start.unavailable"));
	}

	public static void claimRewards(ServerPlayer requester, String questKey) {
		if (PartyManager.isInParty(requester) && !PartyManager.canClaimSharedRewards(requester)) {
			requester.sendSystemMessage(Component.translatable("quest.dmz.party.reward.leader_only")
					.withStyle(ChatFormatting.RED));
			return;
		}

		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null) {
			return;
		}
		if (resolved.quest().getClaimMode() == Quest.ClaimMode.NPC_ONLY) {
			requester.sendSystemMessage(Component.translatable("quest.dmz.reward.npc_only")
					.withStyle(ChatFormatting.RED));
			return;
		}

		ServerPlayer controller = PartyManager.resolveQuestController(requester);
		if (controller == null) {
			return;
		}

		StatsProvider.get(StatsCapability.INSTANCE, controller).ifPresent(data -> {
			PlayerQuestData pqd = data.getPlayerQuestData();
			if (!pqd.isQuestCompleted(questKey)) {
				return;
			}

			if (claimAvailableRewards(controller, resolved.quest(), questKey, pqd)) {
				syncQuestState(controller);
			}
		});
	}

	/**
	 * Claims every unclaimed reward across all completed quests for the player.
	 * NPC-only quests are skipped (they must be claimed by talking to their NPC).
	 * Party reward-sharing rules are respected: only the controller may claim.
	 */
	public static void claimAllRewards(ServerPlayer requester) {
		if (PartyManager.isInParty(requester) && !PartyManager.canClaimSharedRewards(requester)) {
			requester.sendSystemMessage(Component.translatable("quest.dmz.party.reward.leader_only")
					.withStyle(ChatFormatting.RED));
			return;
		}

		ServerPlayer controller = PartyManager.resolveQuestController(requester);
		if (controller == null) {
			return;
		}

		StatsProvider.get(StatsCapability.INSTANCE, controller).ifPresent(data -> {
			PlayerQuestData pqd = data.getPlayerQuestData();
			boolean anyClaimed = false;

			for (String questKey : new ArrayList<>(pqd.getCompletedQuestIds())) {
				ResolvedQuest resolved = resolveQuest(questKey);
				if (resolved == null) {
					continue;
				}
				if (resolved.quest().getClaimMode() == Quest.ClaimMode.NPC_ONLY) {
					continue;
				}
				anyClaimed |= claimAvailableRewards(controller, resolved.quest(), questKey, pqd);
			}

			if (anyClaimed) {
				syncQuestState(controller);
			}
		});
	}

	public static boolean isTurnInReady(PlayerQuestData pqd, String questKey, Quest quest) {
		if (pqd == null || quest == null || questKey == null || questKey.isBlank()) {
			return false;
		}

		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			if (objective.getType() == QuestObjective.ObjectiveType.TALK_TO) {
				continue;
			}

			int progress = pqd.getObjectiveProgress(questKey, i);
			if (progress < quest.getObjectiveRequired(pqd, questKey, i)) {
				return false;
			}
		}

		return true;
	}

	public static boolean requiresTurnInAction(Quest quest) {
		return quest != null && quest.getTurnIn() != null && !quest.getTurnIn().isBlank();
	}

	public static NPCQuestOptions collectNpcQuestOptions(String npcId, StatsData data) {
		List<String> offerableQuestIds = new ArrayList<>();
		List<String> turnInQuestIds = new ArrayList<>();
		List<String> inProgressQuestIds = new ArrayList<>();

		if (npcId == null || npcId.isBlank() || data == null) {
			return new NPCQuestOptions(offerableQuestIds, turnInQuestIds, inProgressQuestIds);
		}

		PlayerQuestData pqd = data.getPlayerQuestData();
		Map<String, Quest> allQuests = QuestRegistry.getAllQuests();

		for (String questId : QuestRegistry.getQuestIdsByGiver(npcId)) {
			Quest quest = allQuests.get(questId);
			if (quest == null || pqd.isQuestCompleted(questId)) {
				continue;
			}

			if (pqd.isQuestAccepted(questId)) {
				inProgressQuestIds.add(questId);
			} else if (isOfferableNpcQuest(questId, quest, data)) {
				offerableQuestIds.add(questId);
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
			if (isTurnInReady(pqd, questId, quest) && !turnInQuestIds.contains(questId)) {
				turnInQuestIds.add(questId);
			}
		}

		return new NPCQuestOptions(offerableQuestIds, turnInQuestIds, inProgressQuestIds);
	}

	public static void spawnKillObjectivesForQuest(ServerPlayer requester, String questKey, int partySize, boolean isHardMode) {
		ResolvedQuest resolved = resolveQuest(questKey);
		if (resolved == null) {
			return;
		}

		StatsProvider.get(StatsCapability.INSTANCE, requester).ifPresent(data ->
				spawnKillObjectives(requester, resolved, data.getPlayerQuestData(), partySize, isHardMode));
	}

	private static Component startQuest(ServerPlayer requester, ServerPlayer controller, ResolvedQuest resolved,
									 StatsData data, boolean isHardMode) {
		PlayerQuestData pqd = data.getPlayerQuestData();
		String questKey = resolved.questKey();
		Quest quest = resolved.quest();
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(controller);

		PlayerQuestData.QuestStatus questStatus = pqd.getQuestStatus(questKey);
		boolean restartingFailed = questStatus == PlayerQuestData.QuestStatus.FAILED;
		if (pqd.isQuestCompleted(questKey) || questStatus == PlayerQuestData.QuestStatus.ACCEPTED) {
			return Component.translatable("message.dragonminez.quest.start.already_active");
		}

		if (!restartingFailed) {
			if (!isQuestAvailableToStart(resolved, data)) {
				Component reason = QuestAvailabilityChecker.describeAvailabilityFailure(quest, data);
				return reason != null ? reason : Component.translatable("message.dragonminez.quest.start.locked");
			}

			Component controllerBlocker = QuestAvailabilityChecker.describeQuestStartBlocker(quest, questKey, controller, data);
			if (controllerBlocker != null) {
				if (requester.getUUID().equals(controller.getUUID())) {
					return controllerBlocker;
				}
				return Component.translatable(
						"message.dragonminez.quest.start.party_member_requirement",
						controller.getGameProfile().getName(),
						controllerBlocker
				);
			}

			Component partyBlocker = getPartyRequirementFailure(requester, controller, quest, questKey);
			if (partyBlocker != null) {
				return partyBlocker;
			}
		}

		DMZEvent.QuestStartEvent startEvent = new DMZEvent.QuestStartEvent(
				controller,
				questKey,
				resolved.saga(),
				quest,
				partyMembers,
				isHardMode
		);
		if (MinecraftForge.EVENT_BUS.post(startEvent)) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		int partySize = partyMembers.size();
		pqd.acceptQuest(questKey);
		quest.initializeObjectiveRequirements(pqd, questKey, partySize);
		pqd.setQuestHardMode(questKey, startEvent.isHardMode());
		pqd.setTrackedQuestId(questKey);

		spawnKillObjectives(requester, resolved, pqd, partySize, startEvent.isHardMode());

		NetworkHandler.sendToPlayer(StoryToastS2C.questStarted(questKey), controller);
		if (PartyManager.isInParty(controller)) {
			PartyManager.syncPartyQuestState(controller);
			for (ServerPlayer member : partyMembers) {
				if (!member.getUUID().equals(controller.getUUID())) {
					NetworkHandler.sendToPlayer(StoryToastS2C.questStarted(questKey), member);
				}
			}
		} else {
			NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(controller), controller);
		}

		return null;
	}

	private static Component turnInQuest(ServerPlayer requester, ServerPlayer controller, ResolvedQuest resolved,
									 StatsData data, String npcId) {
		Quest quest = resolved.quest();
		String questKey = resolved.questKey();
		PlayerQuestData pqd = data.getPlayerQuestData();
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(controller);

		if (!requiresTurnInAction(quest) || !npcId.equals(quest.getTurnIn())) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		if (pqd.isQuestCompleted(questKey)) {
			boolean anyClaimed = claimAvailableRewards(controller, quest, questKey, pqd);
			if (anyClaimed) {
				syncQuestState(controller);
				return null;
			}
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}
		if (!pqd.isQuestAccepted(questKey)) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}
		if (!isTurnInReady(pqd, questKey, quest)) {
			return Component.translatable("message.dragonminez.quest.start.locked");
		}

		DMZEvent.QuestTurnInEvent turnInEvent = new DMZEvent.QuestTurnInEvent(
				controller,
				questKey,
				resolved.saga(),
				quest,
				partyMembers,
				npcId
		);
		if (MinecraftForge.EVENT_BUS.post(turnInEvent)) {
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		boolean objectiveProgressChanged = false;
		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			if (objective instanceof TalkToObjective talkToObjective && npcId.equals(talkToObjective.getNpcId())) {
				int required = quest.getObjectiveRequired(pqd, questKey, i);
				objectiveProgressChanged |= updateObjectiveProgressWithEvent(
						controller,
						pqd,
						resolved,
						quest,
						partyMembers,
						i,
						required
				);
			}
		}

		DMZEvent.QuestCompletedEvent completeEvent = new DMZEvent.QuestCompletedEvent(
				controller,
				questKey,
				resolved.saga(),
				quest,
				partyMembers
		);
		if (MinecraftForge.EVENT_BUS.post(completeEvent)) {
			if (objectiveProgressChanged) {
				syncQuestState(controller);
			}
			return Component.translatable("message.dragonminez.quest.start.unavailable");
		}

		pqd.completeQuest(questKey);
		if (questKey.equals(pqd.getTrackedQuestId())) {
			pqd.setTrackedQuestId(null);
		}

		requester.displayClientMessage(
				Component.translatable("command.dragonminez.story.sidequest.turned_in", questKey), false);
		claimAvailableRewards(controller, quest, questKey, pqd);
		NetworkHandler.sendToPlayer(StoryToastS2C.questComplete(questKey), controller);

		if (PartyManager.isInParty(controller)) {
			for (ServerPlayer member : partyMembers) {
				if (!member.getUUID().equals(controller.getUUID())) {
					NetworkHandler.sendToPlayer(StoryToastS2C.questComplete(questKey), member);
				}
			}
		}
		syncQuestState(controller);

		return null;
	}

	private static boolean updateObjectiveProgressWithEvent(ServerPlayer controller, PlayerQuestData pqd,
												  ResolvedQuest resolved, Quest quest, List<ServerPlayer> partyMembers,
												  int objectiveIndex, int newProgress) {
		int current = pqd.getObjectiveProgress(resolved.questKey(), objectiveIndex);
		if (current == newProgress) {
			return false;
		}

		int required = objectiveIndex >= 0 && objectiveIndex < quest.getObjectives().size()
				? quest.getObjectiveRequired(pqd, resolved.questKey(), objectiveIndex)
				: 0;
		DMZEvent.QuestObjectiveProgressEvent progressEvent = new DMZEvent.QuestObjectiveProgressEvent(
				controller,
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

		int updated = progressEvent.getNewProgress();
		if (updated == current) {
			return false;
		}

		pqd.setObjectiveProgress(resolved.questKey(), objectiveIndex, updated);
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
		for (int i = 0; i < quest.getRewards().size(); i++) {
			if (!pqd.isRewardClaimed(questKey, i)) {
				return true;
			}
		}
		return false;
	}

	private static boolean claimAvailableRewards(ServerPlayer rewardTarget, Quest quest, String questKey, PlayerQuestData pqd) {
		boolean anyClaimed = false;
		List<QuestReward> rewards = quest.getRewards();
		ResolvedQuest resolved = resolveQuest(questKey);
		Saga saga = resolved != null ? resolved.saga() : null;
		List<ServerPlayer> partyMembers = PartyManager.getAllPartyMembers(rewardTarget);
		for (int i = 0; i < rewards.size(); i++) {
			if (pqd.isRewardClaimed(questKey, i)) {
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
			rewards.get(i).giveReward(rewardTarget);
			pqd.claimReward(questKey, i);
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

	private static void spawnKillObjectives(ServerPlayer requester, ResolvedQuest resolved, PlayerQuestData pqd,
											int partySize, boolean isHardMode) {
		Quest quest = resolved.quest();
		String questKey = resolved.questKey();

		for (int i = 0; i < quest.getObjectives().size(); i++) {
			QuestObjective objective = quest.getObjectives().get(i);
			if (!(objective instanceof KillObjective killObjective)) {
				continue;
			}
			if (killObjective.getSpawnMode() != KillObjective.SpawnMode.QUEST) {
				continue;
			}

			int currentProgress = pqd.getObjectiveProgress(questKey, i);
			int required = quest.getObjectiveRequired(pqd, questKey, i);
			int remaining = Math.max(0, required - currentProgress);
			if (remaining <= 0) {
				continue;
			}

			String entityIdStr = killObjective.getEntityId();
			EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityIdStr));
			if (entityType == null) {
				continue;
			}

			double spawnRadius = quest.isSagaQuest() ? 8.0 : 2.0;
			for (int j = 0; j < remaining; j++) {
				Entity entity = entityType.create(requester.level());
				if (entity == null) {
					continue;
				}

				double offsetX = (Math.random() - 0.5) * spawnRadius;
				double offsetZ = (Math.random() - 0.5) * spawnRadius;
				entity.setPos(requester.getX() + offsetX, requester.getY(), requester.getZ() + offsetZ);

				if (isHardMode) {
					entity.getPersistentData().putBoolean("dmz_is_hardmode", true);
				}
				if (resolved.saga() != null) {
					entity.getPersistentData().putString(SAGA_ID_TAG, resolved.saga().getId());
				}

				entity.getPersistentData().putString(QUEST_KEY_TAG, questKey);
				entity.getPersistentData().putInt(QUEST_OBJECTIVE_INDEX_TAG, i);
				entity.getPersistentData().putString(QUEST_OWNER_TAG, requester.getStringUUID());
				entity.getPersistentData().putDouble("dmz_quest_hp", quest.getScaledKillHealth(killObjective, partySize));
				entity.getPersistentData().putDouble("dmz_quest_melee", quest.getScaledKillMeleeDamage(killObjective, partySize));
				entity.getPersistentData().putDouble("dmz_quest_ki", quest.getScaledKillKiDamage(killObjective, partySize));
				if (killObjective.getTextureVariant() >= 0) {
					entity.getPersistentData().putInt("dmz_quest_texture_variant", killObjective.getTextureVariant());
				}

				if (entity instanceof Mob mob) {
					mob.setTarget(requester);
				}

				requester.serverLevel().addFreshEntity(entity);
			}
		}
	}

	@Nullable
	private static Component getPartyRequirementFailure(ServerPlayer requester, ServerPlayer controller, Quest quest, String questKey) {
		if (!PartyManager.isInParty(controller)) {
			return null;
		}

		for (ServerPlayer member : PartyManager.getAllPartyMembers(controller)) {
			Component blocker = StatsProvider.get(StatsCapability.INSTANCE, member)
					.map(data -> QuestAvailabilityChecker.describeQuestStartBlocker(quest, questKey, member, data))
					.orElse(Component.translatable("message.dragonminez.quest.start.unavailable"));
			if (blocker == null) {
				continue;
			}

			if (member.getUUID().equals(requester.getUUID())) {
				return blocker;
			}

			return Component.translatable(
					"message.dragonminez.quest.start.party_member_requirement",
					member.getGameProfile().getName(),
					blocker
			);
		}

		return null;
	}

	public static void syncQuestState(ServerPlayer controller) {
		if (PartyManager.isInParty(controller)) {
			PartyManager.syncPartyQuestState(controller);
		} else {
			NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(controller), controller);
		}
	}
}
