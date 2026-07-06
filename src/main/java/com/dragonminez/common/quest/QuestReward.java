package com.dragonminez.common.quest;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public abstract class QuestReward {
	private final RewardType type;
	@Setter
	private DifficultyType difficultyType = DifficultyType.ALL;

	public QuestReward(RewardType type) {
		this.type = type;
	}

	public abstract void giveReward(net.minecraft.server.level.ServerPlayer player);

	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		giveReward(player);
	}

	public abstract Component getDescription();

	public Difficulty getMinimumDifficulty() {
		return switch (difficultyType) {
			case ALL -> Difficulty.EASY;
			case NORMAL -> Difficulty.NORMAL;
			case HARD -> Difficulty.HARD;
		};
	}

	public boolean isUnlockedFor(Difficulty difficulty) {
		Difficulty effective = difficulty != null ? difficulty : Difficulty.NORMAL;
		return effective.ordinal() >= getMinimumDifficulty().ordinal();
	}

	public enum RewardType {
		ITEM,
		COMMAND,
		TPS,
		SKILL,
		ALIGNMENT,
		TRANSFORMATION,
		KI_TECHNIQUE
	}

	public enum DifficultyType {
		ALL,
		NORMAL,
		HARD
	}
}
