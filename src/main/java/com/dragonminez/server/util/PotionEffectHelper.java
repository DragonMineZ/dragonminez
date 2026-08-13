package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class PotionEffectHelper {
	private PotionEffectHelper() {}

	public static void syncCooldownIndicator(Player player, StatsData data, String cooldownKey, Holder<MobEffect> effect) {
		if (player == null || effect == null) return;

		int remaining = data.getCooldowns().getCooldown(cooldownKey);
		if (remaining > 0) {
			MobEffectInstance existing = player.getEffect(effect);
			if (existing == null || existing.getDuration() < remaining) {
				player.addEffect(new MobEffectInstance(effect, remaining, 0, false, false, true));
			}
		} else if (player.hasEffect(effect)) {
			player.removeEffect(effect);
		}
	}

	public static double applyKiRegenMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.KI_REGEN, "ki_regen");
		return baseValue * multiplier;
	}

	public static double applyStaminaRegenMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.STAMINA_REGEN, "stamina_regen");
		return baseValue * multiplier;
	}

	public static double applyTpGainMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.TP_GAIN, "tp_gain");
		return baseValue * multiplier;
	}

	public static double applyMasteryGainMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.MASTERY_GAIN, "mastery_gain");
		return baseValue * multiplier * getMutantMasteryMultiplier(entity);
	}

	private static double getMutantMasteryMultiplier(LivingEntity entity) {
		if (entity == null || !entity.hasEffect(MainEffects.MUTANT)) return 1.0D;
		var serverConfig = ConfigManager.getServerConfig();
		if (serverConfig == null || serverConfig.getMutant() == null) return 1.0D;
		return serverConfig.getMutant().getMasteryGainMultiplier();
	}

	public static double getMultiplierFromEffect(LivingEntity entity, Holder<MobEffect> effect, String effectName) {
		if (entity == null || effect == null) return 1.0D;
		MobEffectInstance instance = entity.getEffect(effect);
		if (instance == null) return 1.0D;

		int amplifier = instance.getAmplifier();
		double bonus = getBonusFromAmplifier(amplifier);

		return 1.0D + bonus;
	}

	private static double getBonusFromAmplifier(int amplifier) {
		if (amplifier < 0) return 0.0D;
		return (amplifier + 1) * 0.25D;
	}
}
