package com.dragonminez.common.quest;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;

@Getter
public abstract class QuestReward {
	private final RewardType type;
	@Setter
	private DifficultyType difficultyType = DifficultyType.ALL;

	public QuestReward(RewardType type) {
		this.type = type;
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