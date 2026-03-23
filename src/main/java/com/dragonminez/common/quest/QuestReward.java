package com.dragonminez.common.quest;

import net.minecraft.network.chat.Component;

public abstract class QuestReward {
	private final RewardType type;
	private DifficultyType difficultyType = DifficultyType.ALL;

	public QuestReward(RewardType type) {
		this.type = type;
	}

	public RewardType getType() {
		return type;
	}

	public DifficultyType getDifficultyType() {
		return difficultyType;
	}

	public void setDifficultyType(DifficultyType difficultyType) {
		this.difficultyType = difficultyType;
	}

	public abstract void giveReward(net.minecraft.server.level.ServerPlayer player);

	public abstract Component getDescription();

	public enum RewardType {
		ITEM,
		COMMAND,
		TPS,
		SKILL
	}

	public enum DifficultyType {
		ALL,
		NORMAL,
		HARD
	}
}