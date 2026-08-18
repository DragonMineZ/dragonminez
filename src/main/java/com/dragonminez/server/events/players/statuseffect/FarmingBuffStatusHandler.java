package com.dragonminez.server.events.players.statuseffect;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.events.players.IStatusEffectHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

/**
 * Sincroniza los bonos temporales de las plantas de agricultura con el sistema de BonusStats mientras
 * el MobEffect correspondiente esté activo, y los retira cuando expira. El MobEffect (aplicado al comer
 * la planta) es la única fuente de verdad: da el icono, el temporizador y evita el spam.
 *
 * También aplica la regeneración de ki activa del Fruto Kaioshin mientras su efecto esté presente.
 */
public class FarmingBuffStatusHandler implements IStatusEffectHandler {

	/** Bono plano de estadística por planta. */
	private static final double STAT_BONUS = 15.0;
	/** Fracción de energía máxima regenerada por segundo con el efecto del Fruto Kaioshin. */
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

	/**
	 * @param bonusStat  estadística a la que se aplica el bono (RES se divide en DEF+STM vía addBonusSplit).
	 * @param checkStat  estadística que se consulta para saber si el bono ya existe (para RES es DEF).
	 * @param split      true si es RES (usa addBonusSplit/removeBonusSplit).
	 */
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
		// Fruto Kaioshin: regenera un 10% de la energía máxima cada segundo mientras el efecto esté activo.
		if (serverPlayer.hasEffect(MainEffects.KAIOSHIN_FRUIT.get())) {
			float maxEnergy = data.getMaxEnergy();
			float regen = (float) (maxEnergy * KI_REGEN_PER_SECOND);
			float newEnergy = Math.min(maxEnergy, data.getResources().getCurrentEnergy() + regen);
			data.getResources().setCurrentEnergy(newEnergy);
		}
	}
}
