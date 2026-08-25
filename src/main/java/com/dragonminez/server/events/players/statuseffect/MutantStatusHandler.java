package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import com.dragonminez.server.util.MutantManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

public class MutantStatusHandler implements IStatusEffectHandler {
	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
		if (data.getEffects().hasEffect(MutantManager.EFFECT_NAME)) {
			MutantManager.reconcileHolder(player, data);
			if (!player.hasEffect(MainEffects.MUTANT.get())) {
				player.addEffect(new MobEffectInstance(MainEffects.MUTANT.get(), data.getEffects().getEffectDuration(MutantManager.EFFECT_NAME), 0, false, false, true));
			}
		} else player.removeEffect(MainEffects.MUTANT.get());
	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {}

	@Override
	public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {}
}
