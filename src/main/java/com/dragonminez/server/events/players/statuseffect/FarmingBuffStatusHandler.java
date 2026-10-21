package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;


public class FarmingBuffStatusHandler implements IStatusEffectHandler {

	private static final double STAT_BONUS = 15.0;
	private static final double KI_REGEN_PER_SECOND = 0.10;

	@Override
	public void handleStatusEffects(ServerPlayer player, StatsData data) {
		// Buffs planos de estadística (el bonus vive mientras el efecto esté activo).
		syncStatBonus(player, data, MainEffects.OOZARU_ROOT.get(), "STR", "DEF", "oozaru_root", false);
		syncStatBonus(player, data, MainEffects.KATCHIN_SPROUT.get(), "RES", "DEF", "katchin_sprout", true);
		syncStatBonus(player, data, MainEffects.METEOR_FLOWER.get(), "SKP", "SKP", "meteor_flower", false);
		syncStatBonus(player, data, MainEffects.AURA_LILY.get(), "PWR", "PWR", "aura_lily", false);
		syncStatBonus(player, data, MainEffects.HERMIT_FERN.get(), "VIT", "VIT", "hermit_fern", false);
	}

	private void syncStatBonus(ServerPlayer player, StatsData data, MobEffect effect,
	                           String bonusStat, String checkStat, String bonusName, boolean split) {
		boolean active = player.hasEffect(effect);
		boolean present = data.getBonusStats().hasBonus(checkStat, bonusName);

		if (active && !present) {
			if (split) data.getBonusStats().addBonusSplit(bonusStat, bonusName, "+", STAT_BONUS, false);
			else data.getBonusStats().addBonus(bonusStat, bonusName, "+", STAT_BONUS, false);
		} else if (!active && present) {
			if (split) data.getBonusStats().removeBonusSplit(bonusStat, bonusName);
			else data.getBonusStats().removeBonus(bonusStat, bonusName);
		}
	}

	@Override
	public void onPlayerTick(ServerPlayer serverPlayer, StatsData data) {
	}

	@Override
	public void onPlayerSecond(ServerPlayer serverPlayer, StatsData data) {
		if (serverPlayer.hasEffect(MainEffects.KAIOSHIN_FRUIT.get())) {
			float maxEnergy = data.getMaxEnergy();
			float regen = (float) (maxEnergy * KI_REGEN_PER_SECOND);
			float newEnergy = Math.min(maxEnergy, data.getResources().getCurrentEnergy() + regen);
			data.getResources().setCurrentEnergy(newEnergy);
		}
	}
}
