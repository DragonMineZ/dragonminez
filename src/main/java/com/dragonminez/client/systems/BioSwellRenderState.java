package com.dragonminez.client.systems;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BioSwellRenderState {
	private static final float BODY_GROWTH = 0.30f;
	private static final float EASE_PER_TICK = 0.18f;

	private static final int FLASH_WINDOW_TICKS = 30;

	private static final Map<UUID, Float> RENDERED = new HashMap<>();
	private static final Map<UUID, Integer> PREDICTED_CHARGE = new HashMap<>();
	private static final Map<UUID, Integer> LAST_SYNCED_CHARGE = new HashMap<>();

	private BioSwellRenderState() {
	}

	public static void tick(Player player, StatsData data) {
		UUID id = player.getUUID();
		float target = Mth.clamp(data.getRacialData().getBioSwell(), 0.0f, 1.0f);
		float current = RENDERED.getOrDefault(id, 0.0f);

		tickChargePrediction(id, data.getRacialData().getBioChargeTicks());

		if (target <= 0.0f && current <= 0.001f) {
			RENDERED.remove(id);
			return;
		}
		RENDERED.put(id, Mth.lerp(EASE_PER_TICK, current, target));
	}

	private static void tickChargePrediction(UUID id, int syncedCharge) {
		if (syncedCharge <= 0) {
			PREDICTED_CHARGE.remove(id);
			LAST_SYNCED_CHARGE.remove(id);
			return;
		}

		Integer lastSynced = LAST_SYNCED_CHARGE.get(id);
		int predicted;
		if (lastSynced == null || lastSynced != syncedCharge) {
			predicted = syncedCharge;
		} else {
			predicted = PREDICTED_CHARGE.getOrDefault(id, syncedCharge) + 1;
		}

		LAST_SYNCED_CHARGE.put(id, syncedCharge);
		PREDICTED_CHARGE.put(id, Math.min(predicted, requiredChargeTicks()));
	}

	public static float swell(Player player) {
		return RENDERED.getOrDefault(player.getUUID(), 0.0f);
	}

	public static float bodyScale(Player player) {
		return 1.0f + swell(player) * BODY_GROWTH;
	}

	public static int ticksUntilDetonation(Player player) {
		Integer charge = PREDICTED_CHARGE.get(player.getUUID());
		if (charge == null) return -1;
		return Math.max(0, requiredChargeTicks() - charge);
	}

	public static boolean isFlashingWhite(Player player) {
		int remaining = ticksUntilDetonation(player);
		if (remaining < 0 || remaining > FLASH_WINDOW_TICKS) return false;

		int period = remaining > 20 ? 4 : remaining > 10 ? 3 : 2;
		return (remaining / period) % 2 == 0;
	}

	private static int requiredChargeTicks() {
		return Math.max(1, ConfigManager.getServerConfig().getRacialSkills()
				.getBioandroid().getExplodeChargeSeconds() * 20);
	}

	public static void clear() {
		RENDERED.clear();
		PREDICTED_CHARGE.clear();
		LAST_SYNCED_CHARGE.clear();
	}
}
