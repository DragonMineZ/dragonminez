package com.dragonminez.common.wish.wishes;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.wish.Wish;
import com.dragonminez.server.util.BabaReviveService;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ReviveWish extends Wish {
	public static final String WISH_TYPE = "revive";

	private final Integer maxTargets;

	public ReviveWish(String name, String description, int maxTargets) {
		super(name, description, WISH_TYPE);
		this.maxTargets = maxTargets;
	}

	@Override
	public int getMaxTargets() {
		return maxTargets != null ? Math.max(1, maxTargets) : 1;
	}

	@Override
	public void grant(ServerPlayer player) {
		grant(player, List.of(player));
	}

	@Override
	public void grant(ServerPlayer wisher, List<ServerPlayer> targets) {
		for (ServerPlayer target : targets) {
			StatsProvider.get(StatsCapability.INSTANCE, target).ifPresent(data -> BabaReviveService.reviveFully(target, data));
		}
	}
}
