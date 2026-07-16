package com.dragonminez.common.quest;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Set;

@Getter
public abstract class QuestReward {
	private final RewardType type;
	private Set<Difficulty> difficulties = EnumSet.allOf(Difficulty.class);

	public QuestReward(RewardType type) {
		this.type = type;
	}

	public void setDifficulties(Set<Difficulty> difficulties) {
		this.difficulties = (difficulties == null || difficulties.isEmpty())
				? EnumSet.allOf(Difficulty.class)
				: EnumSet.copyOf(difficulties);
	}

	public abstract void giveReward(ServerPlayer player);

	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		giveReward(player);
	}

	public abstract Component getDescription();

	public Component getDescription(double rewardMultiplier) {
		return getDescription();
	}

	public boolean isUnlockedFor(Difficulty difficulty) {
		return difficulties.contains(difficulty != null ? difficulty : Difficulty.NORMAL);
	}

	public enum RewardType {
		ITEM,
		GENERIC_ITEM,
		COMMAND,
		TPS,
		SKILL,
		ALIGNMENT,
		TRANSFORMATION,
		KI_TECHNIQUE
	}
}
