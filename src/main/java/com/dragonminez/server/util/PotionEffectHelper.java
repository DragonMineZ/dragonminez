package com.dragonminez.server.util;

import com.dragonminez.common.init.MainEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class PotionEffectHelper {
	private PotionEffectHelper() {}

	public static double applyKiRegenMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.KI_REGEN.get(), "ki_regen");
		return baseValue * multiplier;
	}

	public static double applyStaminaRegenMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.STAMINA_REGEN.get(), "stamina_regen");
		return baseValue * multiplier;
	}

	public static double applyTpGainMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.TP_GAIN.get(), "tp_gain");
		return baseValue * multiplier;
	}

	public static double applyMasteryGainMultiplier(LivingEntity entity, double baseValue) {
		double multiplier = getMultiplierFromEffect(entity, MainEffects.MASTERY_GAIN.get(), "mastery_gain");
		return baseValue * multiplier;
	}

	public static double getMultiplierFromEffect(LivingEntity entity, MobEffect effect, String effectName) {
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