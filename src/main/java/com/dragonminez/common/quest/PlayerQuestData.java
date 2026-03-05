package com.dragonminez.common.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * Per-player quest progress for all quest types (saga, sidequest, daily, event).
 * <p>
 * All quests are keyed by their string ID. Saga quests use the composite key
 * {@code "sagaId:numericId"} (e.g. {@code "saiyan_saga:1"}). Side-quests use their
 * natural string ID (e.g. {@code "roshi_basic_training"}).
 * <p>
 * On first load, this class automatically migrates legacy data from the old
 * {@code "QuestData"} and {@code "SideQuestData"} NBT keys for 2.0 worlds.
 * <p>
 * Serialized to/from NBT under the {@code "PlayerQuestData"} key in the player's stats compound.
 *
 * @since 2.0
 * @see QuestRegistry
 */
public class PlayerQuestData {

    // ========================================================================================
    // Progress Status
    // ========================================================================================

    /**
     * The status of a quest for a given player.
     * Can return NOT_STARTED (default), ACCEPTED (in progress), or COMPLETED.
     */
    public enum QuestStatus {
        /** Quest has not been started. */
        NOT_STARTED,
        /** Quest has been accepted and is in progress. */
        ACCEPTED,
        /** Quest has been completed. */
        COMPLETED
    }

    // ========================================================================================
    // Internal State
    // ========================================================================================

    /** All quest progress, keyed by string quest ID. */
    private final Map<String, QuestProgress> quests = new LinkedHashMap<>();

    /** Tracks which sagas the player has unlocked (legacy saga-unlock support). */
    private final Map<String, Boolean> sagaUnlockState = new HashMap<>();

    // ========================================================================================
    // Quest Progress — Accept / Complete / Reset
    // ========================================================================================

    /**
     * Accepts a quest, marking it as in-progress.
     *
     * @param questId the string quest ID
     */
    public void acceptQuest(String questId) {
        getOrCreateProgress(questId).setStatus(QuestStatus.ACCEPTED);
    }

    /**
     * Completes a quest, marking it as finished.
     *
     * @param questId the string quest ID
     */
    public void completeQuest(String questId) {
        getOrCreateProgress(questId).setStatus(QuestStatus.COMPLETED);
    }

    /**
     * Returns whether the given quest has been accepted (in progress or completed).
     */
    public boolean isQuestAccepted(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.getStatus() != QuestStatus.NOT_STARTED;
    }

    /**
     * Returns whether the given quest has been completed.
     */
    public boolean isQuestCompleted(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.getStatus() == QuestStatus.COMPLETED;
    }

    /**
     * Returns the status of a given quest.
     */
    public QuestStatus getQuestStatus(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getStatus() : QuestStatus.NOT_STARTED;
    }

    /**
     * Resets all progress for a given quest.
     */
    public void resetQuest(String questId) {
        quests.remove(questId);
    }

    /**
     * Resets all quest progress.
     */
    public void resetAll() {
        quests.clear();
        sagaUnlockState.clear();
    }

    /**
     * Resets all quest progress for quests belonging to the given saga (keys starting with "sagaId:").
     */
    public void resetSaga(String sagaId) {
        String prefix = sagaId + ":";
        quests.keySet().removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Returns the set of all quest IDs that have been accepted (in progress).
     */
    public Set<String> getAcceptedQuestIds() {
        Set<String> accepted = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().getStatus() == QuestStatus.ACCEPTED) {
                accepted.add(entry.getKey());
            }
        }
        return accepted;
    }

    /**
     * Returns the set of all quest IDs that have been completed.
     */
    public Set<String> getCompletedQuestIds() {
        Set<String> completed = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().getStatus() == QuestStatus.COMPLETED) {
                completed.add(entry.getKey());
            }
        }
        return completed;
    }

    // ========================================================================================
    // Objective Progress
    // ========================================================================================

    /**
     * Sets the progress value for a specific objective within a quest.
     */
    public void setObjectiveProgress(String questId, int objectiveIndex, int progress) {
        getOrCreateProgress(questId).setObjectiveProgress(objectiveIndex, progress);
    }

    /**
     * Returns the progress value for a specific objective within a quest.
     */
    public int getObjectiveProgress(String questId, int objectiveIndex) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getObjectiveProgress(objectiveIndex) : 0;
    }

    // ========================================================================================
    // Reward Claims
    // ========================================================================================

    /**
     * Marks a reward as claimed for a given quest.
     */
    public void claimReward(String questId, int rewardIndex) {
        getOrCreateProgress(questId).claimReward(rewardIndex);
    }

    /**
     * Returns whether a reward has been claimed for a given quest.
     */
    public boolean isRewardClaimed(String questId, int rewardIndex) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.isRewardClaimed(rewardIndex);
    }

    // ========================================================================================
    // Saga Unlock State (legacy compatibility)
    // ========================================================================================

    /**
     * Sets the unlock state for a saga.
     * Used during legacy migration and by the saga progression system.
     */
    public void setSagaUnlocked(String sagaId, boolean unlocked) {
        sagaUnlockState.put(sagaId, unlocked);
    }

    /**
     * Returns whether a saga has been unlocked.
     */
    public boolean isSagaUnlocked(String sagaId) {
        return sagaUnlockState.getOrDefault(sagaId, false);
    }

    // ========================================================================================
    // Legacy Compatibility — Saga Composite Keys
    // ========================================================================================

    /**
     * Builds the composite key used for saga quests: {@code "sagaId:numericId"}.
     *
     * @param sagaId  the saga identifier
     * @param questId the numeric quest ID within the saga
     * @return the composite string key
     */
    public static String sagaQuestKey(String sagaId, int questId) {
        return sagaId + ":" + questId;
    }

    // ---- Saga convenience overloads (auto-build composite key) ----

    /** Checks if a saga quest is completed. Builds composite key from saga + numeric quest ID. */
    public boolean isQuestCompleted(String sagaId, int questId) {
        return isQuestCompleted(sagaQuestKey(sagaId, questId));
    }

    /** Completes a saga quest. Builds composite key from saga + numeric quest ID. */
    public void completeQuest(String sagaId, int questId) {
        completeQuest(sagaQuestKey(sagaId, questId));
    }

    /** Gets objective progress for a saga quest. */
    public int getObjectiveProgress(String sagaId, int questId, int objectiveIndex) {
        return getObjectiveProgress(sagaQuestKey(sagaId, questId), objectiveIndex);
    }

    /** Alias for backwards compatibility with old QuestData API. */
    public int getQuestObjectiveProgress(String sagaId, int questId, int objectiveIndex) {
        return getObjectiveProgress(sagaQuestKey(sagaId, questId), objectiveIndex);
    }

    /** Sets objective progress for a saga quest. */
    public void setObjectiveProgress(String sagaId, int questId, int objectiveIndex, int progress) {
        setObjectiveProgress(sagaQuestKey(sagaId, questId), objectiveIndex, progress);
    }

    /** Alias for backwards compatibility with old QuestData API. */
    public void setQuestObjectiveProgress(String sagaId, int questId, int objectiveIndex, int progress) {
        setObjectiveProgress(sagaQuestKey(sagaId, questId), objectiveIndex, progress);
    }

    /** Checks if a reward is claimed for a saga quest. */
    public boolean isRewardClaimed(String sagaId, int questId, int rewardIndex) {
        return isRewardClaimed(sagaQuestKey(sagaId, questId), rewardIndex);
    }

    /** Claims a reward for a saga quest. */
    public void claimReward(String sagaId, int questId, int rewardIndex) {
        claimReward(sagaQuestKey(sagaId, questId), rewardIndex);
    }

    // ========================================================================================
    // Internal Helpers
    // ========================================================================================

    private QuestProgress getOrCreateProgress(String questId) {
        return quests.computeIfAbsent(questId, id -> new QuestProgress(id));
    }

    // ========================================================================================
    // NBT Serialization
    // ========================================================================================

    /**
     * Serializes all quest progress to NBT.
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Quest progress
        ListTag questList = new ListTag();
        for (QuestProgress progress : quests.values()) {
            questList.add(progress.serializeNBT());
        }
        tag.put("quests", questList);

        // Saga unlock state
        CompoundTag sagaUnlocks = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : sagaUnlockState.entrySet()) {
            sagaUnlocks.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put("sagaUnlocks", sagaUnlocks);

        return tag;
    }

    /**
     * Deserializes quest progress from NBT.
     */
    public void deserializeNBT(CompoundTag tag) {
        quests.clear();
        sagaUnlockState.clear();

        // Quest progress
        ListTag questList = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questList.size(); i++) {
            CompoundTag questTag = questList.getCompound(i);
            QuestProgress progress = QuestProgress.deserialize(questTag);
            quests.put(progress.getQuestId(), progress);
        }

        // Saga unlock state
        if (tag.contains("sagaUnlocks")) {
            CompoundTag sagaUnlocks = tag.getCompound("sagaUnlocks");
            for (String key : sagaUnlocks.getAllKeys()) {
                sagaUnlockState.put(key, sagaUnlocks.getBoolean(key));
            }
        }
    }

    // ========================================================================================
    // Legacy Migration
    // ========================================================================================

    /**
     * Migrates legacy data from the old {@code "QuestData"} and {@code "SideQuestData"}
     * NBT compounds into this unified structure.
     * <p>
     * Called automatically when loading a player who has not yet been migrated.
     *
     * @param fullNbt the full player stats NBT compound
     */
    public void migrateFromLegacy(CompoundTag fullNbt) {
        // Migrate old saga QuestData
        if (fullNbt.contains("QuestData")) {
            CompoundTag questDataTag = fullNbt.getCompound("QuestData");
            ListTag sagaList = questDataTag.getList("sagas", Tag.TAG_COMPOUND);

            for (int i = 0; i < sagaList.size(); i++) {
                CompoundTag sagaTag = sagaList.getCompound(i);
                String sagaId = sagaTag.getString("sagaId");
                boolean unlocked = sagaTag.getBoolean("unlocked");

                sagaUnlockState.put(sagaId, unlocked);

                ListTag questList = sagaTag.getList("quests", Tag.TAG_COMPOUND);
                for (int j = 0; j < questList.size(); j++) {
                    CompoundTag questTag = questList.getCompound(j);
                    int numericId = questTag.getInt("questId");
                    boolean completed = questTag.getBoolean("completed");
                    String compositeKey = sagaQuestKey(sagaId, numericId);

                    QuestProgress progress = new QuestProgress(compositeKey);
                    progress.setStatus(completed ? QuestStatus.COMPLETED : QuestStatus.ACCEPTED);

                    // Migrate objective progress
                    CompoundTag objectivesTag = questTag.getCompound("objectives");
                    for (String key : objectivesTag.getAllKeys()) {
                        progress.setObjectiveProgress(Integer.parseInt(key), objectivesTag.getInt(key));
                    }

                    // Migrate reward claims
                    CompoundTag rewardsTag = questTag.getCompound("rewards");
                    for (String key : rewardsTag.getAllKeys()) {
                        if (rewardsTag.getBoolean(key)) {
                            progress.claimReward(Integer.parseInt(key));
                        }
                    }

                    quests.put(compositeKey, progress);
                }
            }
        }

        // Migrate old SideQuestData
        if (fullNbt.contains("SideQuestData")) {
            CompoundTag sideQuestDataTag = fullNbt.getCompound("SideQuestData");
            ListTag sideQuestList = sideQuestDataTag.getList("sideQuests", Tag.TAG_COMPOUND);

            for (int i = 0; i < sideQuestList.size(); i++) {
                CompoundTag sqTag = sideQuestList.getCompound(i);
                String sideQuestId = sqTag.getString("sideQuestId");
                boolean accepted = sqTag.getBoolean("accepted");
                boolean completed = sqTag.getBoolean("completed");

                QuestProgress progress = new QuestProgress(sideQuestId);
                if (completed) {
                    progress.setStatus(QuestStatus.COMPLETED);
                } else if (accepted) {
                    progress.setStatus(QuestStatus.ACCEPTED);
                }

                // Migrate objective progress
                CompoundTag objectivesTag = sqTag.getCompound("objectives");
                for (String key : objectivesTag.getAllKeys()) {
                    progress.setObjectiveProgress(Integer.parseInt(key), objectivesTag.getInt(key));
                }

                // Migrate reward claims
                CompoundTag rewardsTag = sqTag.getCompound("rewards");
                for (String key : rewardsTag.getAllKeys()) {
                    if (rewardsTag.getBoolean(key)) {
                        progress.claimReward(Integer.parseInt(key));
                    }
                }

                quests.put(sideQuestId, progress);
            }
        }
    }

    // ========================================================================================
    // Quest Progress Inner Class
    // ========================================================================================

    /**
     * Tracks progress for a single quest: status, per-objective progress, and reward claims.
     */
    public static class QuestProgress {
        private final String questId;
        private QuestStatus status;
        private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
        private final Map<Integer, Boolean> rewardsClaimed = new HashMap<>();

        public QuestProgress(String questId) {
            this.questId = questId;
            this.status = QuestStatus.NOT_STARTED;
        }

        public String getQuestId() {
            return questId;
        }

        public QuestStatus getStatus() {
            return status;
        }

        public void setStatus(QuestStatus status) {
            this.status = status;
        }

        public void setObjectiveProgress(int index, int progress) {
            objectiveProgress.put(index, progress);
        }

        public int getObjectiveProgress(int index) {
            return objectiveProgress.getOrDefault(index, 0);
        }

        public void claimReward(int index) {
            rewardsClaimed.put(index, true);
        }

        public boolean isRewardClaimed(int index) {
            return rewardsClaimed.getOrDefault(index, false);
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("questId", questId);
            tag.putString("status", status.name());

            CompoundTag objectivesTag = new CompoundTag();
            for (Map.Entry<Integer, Integer> entry : objectiveProgress.entrySet()) {
                objectivesTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.put("objectives", objectivesTag);

            CompoundTag rewardsTag = new CompoundTag();
            for (Map.Entry<Integer, Boolean> entry : rewardsClaimed.entrySet()) {
                rewardsTag.putBoolean(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.put("rewards", rewardsTag);

            return tag;
        }

        public static QuestProgress deserialize(CompoundTag tag) {
            String questId = tag.getString("questId");
            QuestProgress progress = new QuestProgress(questId);

            try {
                progress.status = QuestStatus.valueOf(tag.getString("status"));
            } catch (IllegalArgumentException e) {
                progress.status = QuestStatus.NOT_STARTED;
            }

            CompoundTag objectivesTag = tag.getCompound("objectives");
            for (String key : objectivesTag.getAllKeys()) {
                progress.objectiveProgress.put(Integer.parseInt(key), objectivesTag.getInt(key));
            }

            CompoundTag rewardsTag = tag.getCompound("rewards");
            for (String key : rewardsTag.getAllKeys()) {
                progress.rewardsClaimed.put(Integer.parseInt(key), rewardsTag.getBoolean(key));
            }

            return progress;
        }
    }
}

