package com.dragonminez.client.systems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SilentDamageState {
	private static final long WINDOW_MS = 1500L;
	private static final float TOLERANCE = 0.5F;

	private static float pending;
	private static long expiresAt;

	private SilentDamageState() {
	}

	public static void add(float amount) {
		if (!Float.isFinite(amount) || amount <= 0.0F) return;
		long now = System.currentTimeMillis();
		if (now > expiresAt) pending = 0.0F;
		pending += amount;
		expiresAt = now + WINDOW_MS;
	}

	public static boolean consume(float healthLoss) {
		if (healthLoss <= 0.0F) return false;
		if (pending <= 0.0F || System.currentTimeMillis() > expiresAt) {
			pending = 0.0F;
			return false;
		}
		if (healthLoss <= pending + TOLERANCE) {
			pending = Math.max(0.0F, pending - healthLoss);
			return true;
		}
		pending = 0.0F;
		return false;
	}

	public static void reset() {
		pending = 0.0F;
		expiresAt = 0L;
	}
}
