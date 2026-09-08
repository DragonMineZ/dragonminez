package com.dragonminez.client.systems;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BioSwellRenderState {
	private static final float BODY_GROWTH = 0.30f;
	private static final float EASE_PER_TICK = 0.18f;

	private static final Map<UUID, Float> RENDERED = new HashMap<>();

	private BioSwellRenderState() {
	}

	public static void tick(Player player, StatsData data) {
		UUID id = player.getUUID();
		float target = Mth.clamp(data.getRacialData().getBioSwell(), 0.0f, 1.0f);
		float current = RENDERED.getOrDefault(id, 0.0f);

		if (target <= 0.0f && current <= 0.001f) {
			RENDERED.remove(id);
			return;
		}
		RENDERED.put(id, Mth.lerp(EASE_PER_TICK, current, target));
	}

	public static float swell(Player player) {
		return RENDERED.getOrDefault(player.getUUID(), 0.0f);
	}

	public static float bodyScale(Player player) {
		return 1.0f + swell(player) * BODY_GROWTH;
	}

	public static void clear() {
		RENDERED.clear();
	}
}
