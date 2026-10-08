package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.Wish;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;

@Getter
public class AttributePointsWish extends Wish {
	public static final String WISH_TYPE = "attributepoints";

	private final int amount;

	public AttributePointsWish(String name, String description, int amount) {
		super(name, description, WISH_TYPE);
		this.amount = amount;
	}

	@Override
	public void grant(ServerPlayer player) {
		if (amount <= 0) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			data.getResources().addPendingAttributePoints(amount);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}
}
