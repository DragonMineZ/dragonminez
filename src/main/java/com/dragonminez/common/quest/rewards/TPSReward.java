package com.dragonminez.common.quest.rewards;

import com.dragonminez.common.quest.QuestReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TPSReward extends QuestReward {
	private final int amount;

	public TPSReward(int amount) {
		super(RewardType.TPS);
		this.amount = amount;
	}

	public int getAmount() {
		return amount;
	}

	@Override
	public void giveReward(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> data.getResources().addTrainingPoints(amount));
	}

	@Override
	public Component getDescription() {
		return Component.translatable(
				"gui.dragonminez.quests.rewards.tps",
				amount
		);
	}
}

