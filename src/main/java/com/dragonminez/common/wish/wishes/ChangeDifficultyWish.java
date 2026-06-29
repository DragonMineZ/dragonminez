package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.Wish;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerPlayer;

public class ChangeDifficultyWish extends Wish {

	public ChangeDifficultyWish(String name, String description) {
		super(name, description, "changedifficulty");
	}

	@Override
	public void grant(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			data.getPlayerQuestData().requestDifficultyReselect();
			NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
		});
	}

	@Override
	public String toJson() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this, ChangeDifficultyWish.class);
	}
}
