package com.dragonminez.common.quest;

import net.minecraft.network.chat.Component;

/**
 * Abstract base class for all quest rewards.
 * <p>
 * Each reward has a {@link RewardType} discriminator and an optional {@link DifficultyType}
 * filter that determines whether the reward is given on normal, hard, or all difficulties.
 * <p>
 * Subclasses implement {@link #giveReward} to actually grant the reward to the player,
 * and {@link #getDescription()} for the GUI display text.
 *
 * @since 2.0
 * @see QuestParser#parseReward
 */
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