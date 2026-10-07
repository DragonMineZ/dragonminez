package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.rewards.AttributesReward;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.Wish;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

@Getter
public class AttributesWish extends Wish {
	public static final String WISH_TYPE = "attributes";

	private final int amount;

	public AttributesWish(String name, String description, int amount) {
		super(name, description, WISH_TYPE);
		this.amount = amount;
	}

	@Override
	public void grant(ServerPlayer player) {
		if (amount <= 0) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			data.grantAttributeReward(amount);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}

	@Override
	public List<Component> getTooltipExtras(StatsData data) {
		List<Component> lines = AttributesReward.breakdown(
				Component.translatable("gui.dragonminez.quests.rewards.attributes", amount), amount, data);
		return lines.subList(1, lines.size());
	}
}
