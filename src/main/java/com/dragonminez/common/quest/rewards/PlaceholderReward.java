package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.QuestReward;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class PlaceholderReward extends QuestReward {

	private final String originalType;
	private final String reason;
	private final boolean typeKnown;

	public PlaceholderReward(String originalType, String reason, boolean typeKnown) {
		super(RewardType.PLACEHOLDER);
		this.originalType = originalType;
		this.reason = reason;
		this.typeKnown = typeKnown;
	}

	@Override
	public void giveReward(ServerPlayer player) {
	}

	@Override
	public void giveReward(ServerPlayer player, double rewardMultiplier) {
	}

	@Override
	public Component getDescription() {
		return Component.empty();
	}

	@Override
	public boolean isUnlockedFor(Difficulty difficulty) {
		return false;
	}
}
