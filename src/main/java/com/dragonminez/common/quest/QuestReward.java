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
		KI_TECHNIQUE,
		CUSTOM
	}
}
