package com.dragonminez.common.quest;

import com.dragonminez.common.stats.StatsData;

import java.util.List;

/**
 * Shared branch/saga availability helpers used by both server logic and client UI.
 */
public final class SagaBranchingHelper {

    private SagaBranchingHelper() {
    }

    public static boolean hasBranchMetadata(Quest quest) {
        // Branch paths are currently enabled for sidequest chains only.
        return quest != null && quest.isSideQuest() && quest.isBranchingQuest();
    }

    public static boolean isQuestLockedByBranch(PlayerQuestData pqd, String sagaId, Quest quest) {
        if (pqd == null || sagaId == null || !hasBranchMetadata(quest)) return false;

        String selectedPath = pqd.getBranchSelection(sagaId, quest.getBranchGroup());
        return selectedPath != null && !selectedPath.equalsIgnoreCase(quest.getBranchPath());
    }

    public static void selectBranchIfNeeded(PlayerQuestData pqd, String sagaId, Quest quest) {
        if (pqd == null || sagaId == null || !hasBranchMetadata(quest)) return;
        if (pqd.getBranchSelection(sagaId, quest.getBranchGroup()) == null) {
            pqd.setBranchSelection(sagaId, quest.getBranchGroup(), quest.getBranchPath());
        }
    }

    public static boolean isSagaQuestAvailable(Quest quest, Saga saga, int questIndex, StatsData statsData) {
        if (quest == null || saga == null || statsData == null) return false;
        PlayerQuestData pqd = statsData.getPlayerQuestData();

        if (isQuestLockedByBranch(pqd, saga.getId(), quest)) return false;

        if (!isSequentiallyReachable(saga, questIndex, pqd)) {
            return false;
        }

        if (quest.hasPrerequisites()) {
            return QuestAvailabilityChecker.isAvailable(quest, statsData);
        }

        return true;
    }

    private static boolean isSequentiallyReachable(Saga saga, int questIndex, PlayerQuestData pqd) {
        if (questIndex <= 0) return true;

        List<Quest> sagaQuests = saga.getQuests();
        if (questIndex >= sagaQuests.size()) return false;

        Quest previous = sagaQuests.get(questIndex - 1);
        String previousKey = PlayerQuestData.sagaQuestKey(saga.getId(), previous.getId());
        if (pqd.isQuestCompleted(previousKey)) {
            return true;
        }

        // If previous quest is in an excluded branch path, allow continuing on the selected path.
        return isQuestLockedByBranch(pqd, saga.getId(), previous);
    }
}
