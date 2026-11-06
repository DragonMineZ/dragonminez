package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialRegistry;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;

public class RacialStatusHandler implements IStatusEffectHandler {

	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
		RacialRegistry.forPlayer(data).ifPresent(ability -> ability.onTick(new RacialContext(serverPlayer, data)));
	}

	@Override
	public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
		RacialRegistry.forPlayer(data).ifPresent(ability -> ability.onSecond(new RacialContext(serverPlayer, data)));
	}
}
