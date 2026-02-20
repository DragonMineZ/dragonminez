package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;

public class BioPassiveHandler implements IStatusEffectHandler {
	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
		if (!data.getCooldowns().hasCooldown(Cooldowns.DRAIN)) {
			player.removeEffect(MainEffects.BIOANDROID_PASSIVE.get());
		}
	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {

	}

	@Override
	public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {

	}
}
