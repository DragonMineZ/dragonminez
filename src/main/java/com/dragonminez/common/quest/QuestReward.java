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

	private String customType = null;

	public QuestReward(RewardType type) {
		this.type = type;
	}

	/** Constructor for addon-registered reward types (see QuestRewardRegistry). */
	protected QuestReward(String customType) {
		this(RewardType.CUSTOM);
		this.customType = customType;
	}

	/** Stable string key: the enum name for built-ins, the registered key for addon rewards. */
	public String getTypeKey() {
		return type == RewardType.CUSTOM && customType != null ? customType : type.name();
	}

	public abstract void giveReward(net.minecraft.server.level.ServerPlayer player);

	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		giveReward(player);
	}

	public abstract Component getDescription();

	public enum RewardType {
		ITEM,
		COMMAND,
		TPS,
		SKILL,
		ALIGNMENT,
		TRANSFORMATION,
		KI_TECHNIQUE,
		CUSTOM
	}

	public enum DifficultyType {
		ALL,
		NORMAL,
		HARD
	}
}
