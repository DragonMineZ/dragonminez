package com.dragonminez.common.quest;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.quest.rewards.KiTechniqueReward;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestDataMigrations {
	private static final int MAX_CHECKED_PARTY_SIZE = 16;

	private QuestDataMigrations() {
	}

	public static boolean run(ServerPlayer player, PlayerQuestData pqd) {
		if (player == null || pqd == null) return false;
		String version = QuestUpgrader.DEFAULTS_VERSION;
		if (!version.equals(pqd.getMigratedFor()) && sagasLoaded()) {
			boolean legacy = pqd.getMigratedFor() == null;
			repairSagaContinuity(pqd);
			normalizeRewardClaims(pqd);
			verifyObjectiveSignatures(player, pqd, legacy);
			pqd.setMigratedFor(version);
			return true;
		}
		return verifyObjectiveSignatures(player, pqd, false);
	}

	private static boolean sagasLoaded() {
		return ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled() && !QuestRegistry.getAllSagas().isEmpty();
	}

	private static boolean repairSagaContinuity(PlayerQuestData pqd) {
		Set<String> startedSagas = new HashSet<>();
		Set<String> progressed = new LinkedHashSet<>();
		for (String key : pqd.getKnownQuestIds()) {
			if (pqd.getQuestStatus(key) == PlayerQuestData.QuestStatus.NOT_STARTED) continue;
			progressed.add(key);
			int separator = key.lastIndexOf(':');
			if (separator > 0) startedSagas.add(key.substring(0, separator));
		}

		Set<String> missing = new LinkedHashSet<>();
		Deque<String> work = new ArrayDeque<>();
		for (String key : progressed) {
			requirePrerequisites(pqd, key, startedSagas, missing, work);
		}

		while (!work.isEmpty()) {
			String key = work.poll();
			QuestService.ResolvedQuest resolved = QuestService.resolveQuest(key);
			if (resolved == null) continue;

			Saga saga = resolved.saga();
			if (saga != null) {
				List<Quest> sagaQuests = saga.getQuests();
				int index = sagaQuests.indexOf(resolved.quest());
				for (int i = 0; i < index; i++) {
					Quest earlier = sagaQuests.get(i);
					if (earlier.getId() < resolved.quest().getId()) {
						require(pqd, PlayerQuestData.sagaQuestKey(saga.getId(), earlier.getId()), missing, work);
					}
				}
			}
			requirePrerequisites(pqd, key, startedSagas, missing, work);
		}

		for (String key : missing) {
			Quest quest = QuestRegistry.getQuest(key);
			pqd.completeWithoutRewards(key, quest != null ? quest.getRewards().size() : 0);
		}
		return !missing.isEmpty();
	}

	private static void requirePrerequisites(PlayerQuestData pqd, String key, Set<String> startedSagas,
											 Set<String> missing, Deque<String> work) {
		Quest quest = QuestRegistry.getQuest(key);
		if (quest == null) return;
		QuestPrerequisites prerequisites = quest.getPrerequisites();
		if (prerequisites == null || prerequisites.conditions() == null) return;
		if (prerequisites.operator() != QuestPrerequisites.Operator.AND && prerequisites.conditions().size() != 1) return;
		for (QuestPrerequisites.Condition condition : prerequisites.conditions()) {
			if (condition == null || condition.isNestedGroup()
					|| condition.getType() != QuestPrerequisites.ConditionType.SAGA_QUEST) continue;
			String sagaId = condition.getSagaId();
			Integer questId = condition.getQuestId();
			if (sagaId == null || questId == null || !startedSagas.contains(sagaId)) continue;
			require(pqd, PlayerQuestData.sagaQuestKey(sagaId, questId), missing, work);
		}
	}

	private static void require(PlayerQuestData pqd, String key, Set<String> missing, Deque<String> work) {
		if (QuestRegistry.getQuest(key) == null) return;
		if (pqd.isQuestCompleted(key) || pqd.isQuestAccepted(key) || missing.contains(key)) return;
		missing.add(key);
		work.add(key);
	}

	private static boolean normalizeRewardClaims(PlayerQuestData pqd) {
		boolean changed = false;
		for (String key : pqd.getCompletedQuestIds()) {
			Quest quest = QuestRegistry.getQuest(key);
			if (quest == null || !pqd.hasAnyRewardClaim(key)) continue;
			Difficulty difficulty = pqd.getQuestDifficulty(key);
			List<QuestReward> rewards = quest.getRewards();
			for (int i = pqd.getHighestClaimedRewardIndex(key) + 1; i < rewards.size(); i++) {
				QuestReward reward = rewards.get(i);
				if (reward instanceof KiTechniqueReward || reward.isPlaceholder() || !reward.isUnlockedFor(difficulty)) continue;
				if (pqd.isRewardClaimed(key, i)) continue;
				pqd.claimReward(key, i);
				changed = true;
			}
		}
		return changed;
	}

	private static boolean verifyObjectiveSignatures(ServerPlayer player, PlayerQuestData pqd, boolean legacy) {
		boolean changed = false;
		for (String key : pqd.getAcceptedQuestIds()) {
			Quest quest = QuestRegistry.getQuest(key);
			if (quest == null || hasPlaceholderObjective(quest)) continue;
			String signature = QuestService.objectiveSignature(quest);
			String stored = pqd.getObjectiveSignature(key);
			if (signature.equals(stored)) continue;

			changed = true;
			if (stored == null && !(legacy && requirementsMismatch(pqd, key, quest))) {
				pqd.setObjectiveSignature(key, signature);
				continue;
			}

			pqd.resetObjectiveProgress(key);
			quest.initializeObjectiveRequirements(pqd, key, Math.max(1, QuestParty.participants(player, key).size()));
			pqd.setObjectiveSignature(key, signature);
			player.sendSystemMessage(Component.translatable("message.dragonminez.quest.objectives_changed",
					Component.translatable(quest.getTitle())).withStyle(ChatFormatting.YELLOW));
		}
		return changed;
	}

	private static boolean hasPlaceholderObjective(Quest quest) {
		for (QuestObjective objective : quest.getObjectives()) {
			if (objective.isPlaceholder()) return true;
		}
		return false;
	}

	private static boolean requirementsMismatch(PlayerQuestData pqd, String key, Quest quest) {
		Map<Integer, Integer> stored = pqd.getObjectiveRequiredSnapshot(key);
		if (stored.isEmpty()) return false;
		int size = quest.getObjectives().size();
		for (Integer index : stored.keySet()) {
			if (index >= size) return true;
		}
		for (int partySize = 1; partySize <= MAX_CHECKED_PARTY_SIZE; partySize++) {
			PlayerQuestData scratch = new PlayerQuestData();
			quest.initializeObjectiveRequirements(scratch, key, partySize);
			boolean matches = true;
			for (Map.Entry<Integer, Integer> entry : stored.entrySet()) {
				if (scratch.getObjectiveRequired(key, entry.getKey(), -1) != entry.getValue()) {
					matches = false;
					break;
				}
			}
			if (matches) return false;
		}
		return true;
	}
}
