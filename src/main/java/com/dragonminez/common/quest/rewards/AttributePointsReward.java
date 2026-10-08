package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class AttributePointsReward extends QuestReward {
	private final int amount;

	public AttributePointsReward(int amount) {
		super(RewardType.ATTRIBUTE_POINTS);
		this.amount = amount;
	}

	public int scaledAmount(double rewardMultiplier) {
		return (int) Math.max(0, Math.round(amount * rewardMultiplier));
	}

	@Override
	public void giveReward(ServerPlayer player) {
		giveReward(player, 1.0);
	}

	@Override
	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		int scaled = scaledAmount(rewardMultiplier);
		if (scaled <= 0) return;
		StatsProvider.get(StatsCapability.INSTANCE, player)
				.ifPresent(data -> data.getResources().addPendingAttributePoints(scaled));
	}

	@Override
	public Component getDescription() {
		return Component.translatable("gui.dragonminez.quests.rewards.attribute_points", amount);
	}

	@Override
	public Component getDescription(double rewardMultiplier) {
		return Component.translatable("gui.dragonminez.quests.rewards.attribute_points", scaledAmount(rewardMultiplier));
	}
}
