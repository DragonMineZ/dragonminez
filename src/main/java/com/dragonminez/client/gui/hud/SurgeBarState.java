package com.dragonminez.client.gui.hud;

import com.dragonminez.common.config.CombatConfig;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.util.Mth;

public final class SurgeBarState {

	private static final float CORRECTION_TAU = 0.35f;
	private static final float MAX_FRAME_SECONDS = 0.25f;

	private static float displayed;
	private static long lastNanos;
	private static boolean initialized;

	private SurgeBarState() {}

	public static float fraction(StatsData data) {
		float target = data.getResources().getSurgeCharge();
		boolean surging = data.getStatus().isSurgeActive();
		boolean filling = !surging && data.getStatus().isKiBurstArmed() && data.getStatus().isChargingKi();

		long now = System.nanoTime();
		float dt = initialized ? (now - lastNanos) / 1_000_000_000.0f : 0.0f;
		lastNanos = now;
		initialized = true;
		if (dt < 0.0f) dt = 0.0f;
		if (dt > MAX_FRAME_SECONDS) dt = MAX_FRAME_SECONDS;

		if (target <= 0.0f && !filling && !surging) {
			displayed = 0.0f;
			return 0.0f;
		}

		CombatConfig config = ConfigManager.getCombatConfig();
		if (config != null && dt > 0.0f) {
			if (filling) {
				displayed += 100.0f / (float) Math.max(0.05, config.getSurgeFillSeconds()) * dt;
			} else if (surging) {
				displayed -= 100.0f / (float) Math.max(0.05, config.getSurgeDurationSeconds()) * dt;
			}
		}

		float blend = dt > 0.0f ? 1.0f - (float) Math.exp(-dt / CORRECTION_TAU) : 1.0f;
		displayed += (target - displayed) * blend;
		displayed = Mth.clamp(displayed, 0.0f, 100.0f);

		return displayed / 100.0f;
	}
}
