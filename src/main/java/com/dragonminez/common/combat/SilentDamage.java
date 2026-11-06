package com.dragonminez.common.combat;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SilentDamageS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class SilentDamage {
	public static final float DEFAULT_MIN_HEALTH = 1.0F;

	private SilentDamage() {
	}

	public static float apply(LivingEntity entity, double amount) {
		return apply(entity, amount, DEFAULT_MIN_HEALTH);
	}

	public static float apply(LivingEntity entity, double amount, float minHealth) {
		if (entity == null || entity.level().isClientSide || !entity.isAlive()) return 0.0F;
		if (!Double.isFinite(amount) || amount <= 0.0) return 0.0F;

		float health = entity.getHealth();
		float target = (float) Math.max(minHealth, health - amount);
		float dealt = health - target;
		if (dealt <= 0.0F) return 0.0F;

		if (entity instanceof ServerPlayer player) NetworkHandler.sendToPlayer(new SilentDamageS2C(dealt), player);
		entity.setHealth(target);
		return dealt;
	}

	public static float available(LivingEntity entity, float minHealth) {
		if (entity == null) return 0.0F;
		return Math.max(0.0F, entity.getHealth() - minHealth);
	}
}
