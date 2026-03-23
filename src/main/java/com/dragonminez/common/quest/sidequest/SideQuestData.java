package com.dragonminez.common.quest.sidequest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SideQuestData {
	private final Map<String, SideQuestProgress> questProgress = new HashMap<>();

	public SideQuestProgress getQuestProgress(String sideQuestId) {
		return questProgress.computeIfAbsent(sideQuestId, id -> new SideQuestProgress(sideQuestId));
	}

	public void acceptQuest(String sideQuestId) {
		getQuestProgress(sideQuestId).setAccepted(true);
	}

	public boolean isQuestAccepted(String sideQuestId) {
		return getQuestProgress(sideQuestId).isAccepted();
	}

	public void completeQuest(String sideQuestId) {
		getQuestProgress(sideQuestId).setCompleted(true);
	}

	public boolean isQuestCompleted(String sideQuestId) {
		return getQuestProgress(sideQuestId).isCompleted();
	}

	public void setObjectiveProgress(String sideQuestId, int objectiveIndex, int progress) {
		getQuestProgress(sideQuestId).setObjectiveProgress(objectiveIndex, progress);
	}

	public int getObjectiveProgress(String sideQuestId, int objectiveIndex) {
		return getQuestProgress(sideQuestId).getObjectiveProgress(objectiveIndex);
	}

	public void claimReward(String sideQuestId, int rewardIndex) {
		getQuestProgress(sideQuestId).claimReward(rewardIndex);
	}

	public boolean isRewardClaimed(String sideQuestId, int rewardIndex) {
		return getQuestProgress(sideQuestId).isRewardClaimed(rewardIndex);
	}

	public Set<String> getAcceptedQuestIds() {
		Set<String> accepted = new java.util.HashSet<>();
		for (Map.Entry<String, SideQuestProgress> entry : questProgress.entrySet()) {
			if (entry.getValue().isAccepted()) {
				accepted.add(entry.getKey());
			}
		}
		return accepted;
	}

	public void resetQuest(String sideQuestId) {
		questProgress.remove(sideQuestId);
	}

	public void resetAll() {
		questProgress.clear();
	}

	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();
		ListTag questList = new ListTag();

		for (SideQuestProgress progress : questProgress.values()) {
			questList.add(progress.serializeNBT());
		}

		tag.put("sideQuests", questList);
		return tag;
	}

	public void deserializeNBT(CompoundTag tag) {
		questProgress.clear();
		ListTag questList = tag.getList("sideQuests", Tag.TAG_COMPOUND);

		for (int i = 0; i < questList.size(); i++) {
			CompoundTag questTag = questList.getCompound(i);
			String sideQuestId = questTag.getString("sideQuestId");
			SideQuestProgress progress = new SideQuestProgress(sideQuestId);
			progress.deserializeNBT(questTag);
			questProgress.put(progress.getSideQuestId(), progress);
		}
	}

	public static class SideQuestProgress {
		private final String sideQuestId;
		private boolean accepted;
		private boolean completed;
		private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
		private final Map<Integer, Boolean> rewardsClaimed = new HashMap<>();

		public SideQuestProgress(String sideQuestId) {
			this.sideQuestId = sideQuestId;
			this.accepted = false;
			this.completed = false;
		}

		public String getSideQuestId() {
			return sideQuestId;
		}

		public boolean isAccepted() {
			return accepted;
		}

		public void setAccepted(boolean accepted) {
			this.accepted = accepted;
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
			tag.putString("sideQuestId", sideQuestId);
			tag.putBoolean("accepted", accepted);
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
			accepted = tag.getBoolean("accepted");
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

