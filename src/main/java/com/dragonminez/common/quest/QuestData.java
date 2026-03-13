package com.dragonminez.common.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class QuestData {
    private final Map<String, SagaProgress> sagaProgress = new HashMap<>();

    public SagaProgress getSagaProgress(String sagaId) {
        return sagaProgress.computeIfAbsent(sagaId, id -> new SagaProgress(sagaId));
    }

    public void unlockSaga(String sagaId) {
        getSagaProgress(sagaId).setUnlocked(true);
    }

    public boolean isSagaUnlocked(String sagaId) {
        return getSagaProgress(sagaId).isUnlocked();
    }

    public void completeQuest(String sagaId, int questId) {
        SagaProgress progress = getSagaProgress(sagaId);
        progress.completeQuest(questId);
    }

    public boolean isQuestCompleted(String sagaId, int questId) {
        return getSagaProgress(sagaId).isQuestCompleted(questId);
    }

    public void setQuestObjectiveProgress(String sagaId, int questId, int objectiveIndex, int progress) {
        getSagaProgress(sagaId).setObjectiveProgress(questId, objectiveIndex, progress);
    }

    public int getQuestObjectiveProgress(String sagaId, int questId, int objectiveIndex) {
        return getSagaProgress(sagaId).getObjectiveProgress(questId, objectiveIndex);
    }

    public void claimReward(String sagaId, int questId, int rewardIndex) {
        getSagaProgress(sagaId).claimReward(questId, rewardIndex);
    }

    public boolean isRewardClaimed(String sagaId, int questId, int rewardIndex) {
        return getSagaProgress(sagaId).isRewardClaimed(questId, rewardIndex);
    }

	public Map<String, Saga> getActiveSagas() {
		Map<String, Saga> activeSagas = new HashMap<>();
		for (String sagaId : sagaProgress.keySet()) {
			Saga saga = SagaManager.getSaga(sagaId);
			if (saga != null) {
				activeSagas.put(sagaId, saga);
			}
		}
		return activeSagas;
	}

    public void resetSaga(String sagaId) {
        sagaProgress.remove(sagaId);
    }

    public void resetAllSagas() {
        sagaProgress.clear();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag sagaList = new ListTag();

        for (SagaProgress progress : sagaProgress.values()) {
            sagaList.add(progress.serializeNBT());
        }

        tag.put("sagas", sagaList);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        sagaProgress.clear();
        ListTag sagaList = tag.getList("sagas", Tag.TAG_COMPOUND);

        for (int i = 0; i < sagaList.size(); i++) {
            CompoundTag sagaTag = sagaList.getCompound(i);
            String sagaId = sagaTag.getString("sagaId");
            SagaProgress progress = new SagaProgress(sagaId);
            progress.deserializeNBT(sagaTag);
            sagaProgress.put(progress.getSagaId(), progress);
        }
    }

    public static class SagaProgress {
        private final String sagaId;
        private boolean unlocked;
        private final Map<Integer, QuestProgress> questProgress = new HashMap<>();

        public SagaProgress(String sagaId) {
            this.sagaId = sagaId;
            this.unlocked = false;
        }

        public String getSagaId() {
            return sagaId;
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        public void setUnlocked(boolean unlocked) {
            this.unlocked = unlocked;
        }

        public QuestProgress getQuestProgress(int questId) {
            return questProgress.computeIfAbsent(questId, id -> new QuestProgress(questId));
        }

        public void completeQuest(int questId) {
            getQuestProgress(questId).setCompleted(true);
        }

        public boolean isQuestCompleted(int questId) {
            return getQuestProgress(questId).isCompleted();
        }

        public void setObjectiveProgress(int questId, int objectiveIndex, int progress) {
            getQuestProgress(questId).setObjectiveProgress(objectiveIndex, progress);
        }

        public int getObjectiveProgress(int questId, int objectiveIndex) {
            return getQuestProgress(questId).getObjectiveProgress(objectiveIndex);
        }

        public void claimReward(int questId, int rewardIndex) {
            getQuestProgress(questId).claimReward(rewardIndex);
        }

        public boolean isRewardClaimed(int questId, int rewardIndex) {
            return getQuestProgress(questId).isRewardClaimed(rewardIndex);
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("sagaId", sagaId);
            tag.putBoolean("unlocked", unlocked);

            ListTag questList = new ListTag();
            for (QuestProgress progress : questProgress.values()) {
                questList.add(progress.serializeNBT());
            }
            tag.put("quests", questList);

            return tag;
        }

        public void deserializeNBT(CompoundTag tag) {
            unlocked = tag.getBoolean("unlocked");
            questProgress.clear();
            ListTag questList = tag.getList("quests", Tag.TAG_COMPOUND);
            for (int i = 0; i < questList.size(); i++) {
                CompoundTag questTag = questList.getCompound(i);
                int questId = questTag.getInt("questId");
                QuestProgress progress = new QuestProgress(questId);
                progress.deserializeNBT(questTag);
                questProgress.put(progress.getQuestId(), progress);
            }
        }
    }

    public static class QuestProgress {
        private final int questId;
        private boolean completed;
        private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
        private final Map<Integer, Boolean> rewardsClaimed = new HashMap<>();

        public QuestProgress(int questId) {
            this.questId = questId;
            this.completed = false;
        }

        public int getQuestId() {
            return questId;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
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
            tag.putInt("questId", questId);
            tag.putBoolean("completed", completed);

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

        public void deserializeNBT(CompoundTag tag) {
            completed = tag.getBoolean("completed");
            objectiveProgress.clear();
            rewardsClaimed.clear();
            CompoundTag objectivesTag = tag.getCompound("objectives");
            for (String key : objectivesTag.getAllKeys()) {
                objectiveProgress.put(Integer.parseInt(key), objectivesTag.getInt(key));
            }

            CompoundTag rewardsTag = tag.getCompound("rewards");
            for (String key : rewardsTag.getAllKeys()) {
                rewardsClaimed.put(Integer.parseInt(key), rewardsTag.getBoolean(key));
            }
        }
    }
}

