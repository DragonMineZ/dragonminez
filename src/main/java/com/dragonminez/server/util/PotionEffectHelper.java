package com.dragonminez.server.util;

import com.dragonminez.common.init.MainEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class PotionEffectHelper {
	private PotionEffectHelper() {
	}

	public static double applyKiRegenMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.KI_REGEN.get(), "ki_regen");
		double result = baseValue * multiplier;
		return result;
	}

	public static double applyStaminaRegenMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.STAMINA_REGEN.get(), "stamina_regen");
		double result = baseValue * multiplier;
		return result;
	}

	public static double applyTpGainMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.TP_GAIN.get(), "tp_gain");
		double result = baseValue * multiplier;
		return result;
	}

	public static double applyMasteryGainMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.MASTERY_GAIN.get(), "mastery_gain");
		double result = baseValue * multiplier;
		return result;
	}

	public static double getMultiplierFromEffect(LivingEntity entity, MobEffect effect, String effectName) {
		if (entity == null || effect == null) {
			return 1.0D;
		}

		MobEffectInstance instance = entity.getEffect(effect);
		if (instance == null) {
			return 1.0D;
		}

		int amplifier = instance.getAmplifier();
		double bonus = getBonusFromAmplifier(amplifier);
		double multiplier = 1.0D + bonus;

		return multiplier;
	}

	private static double getBonusFromAmplifier(int amplifier) {
		if (amplifier < 0) {
			return 0.0D;
		}

		return (amplifier + 1) * 0.25D;
	}
}