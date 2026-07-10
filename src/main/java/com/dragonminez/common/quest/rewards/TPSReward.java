package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class TPSReward extends QuestReward {
	private final int amount;

	public TPSReward(int amount) {
		super(RewardType.TPS);
		this.amount = amount;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		giveReward(player, 1.0);
	}

	@Override
	public void giveReward(ServerPlayer player, double rewardMultiplier) {
		int scaled = scaledAmount(rewardMultiplier);
		if (scaled <= 0) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> data.getResources().addTrainingPoints(scaled, false));
	}

	public int scaledAmount(double rewardMultiplier) {
		return (int) Math.max(0, Math.round(amount * rewardMultiplier));
	}

	@Override
	public Component getDescription() {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.tps",
				amount
		);
	}

	@Override
	public Component getDescription(double rewardMultiplier) {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.tps",
				scaledAmount(rewardMultiplier)
		);
	}
}

