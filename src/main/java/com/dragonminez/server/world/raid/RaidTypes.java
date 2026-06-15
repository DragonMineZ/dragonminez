package com.dragonminez.server.world.raid;

import com.dragonminez.common.init.MainEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RaidTypes {

	private static final Map<String, RaidType> REGISTRY = new LinkedHashMap<>();

	/** Fallback raid used when an unknown id is requested. */
	public static final String DEFAULT_ID = "saiyan_assault";

	public static RaidType register(RaidType type) {
		REGISTRY.put(type.getId(), type);
		return type;
	}

	public static RaidType get(String id) {
		return REGISTRY.get(id);
	}

	public static RaidType getOrDefault(String id) {
		RaidType type = REGISTRY.get(id);
		return type != null ? type : REGISTRY.get(DEFAULT_ID);
	}

	public static boolean contains(String id) {
		return REGISTRY.containsKey(id);
	}

	public static Set<String> ids() {
		return Collections.unmodifiableSet(REGISTRY.keySet());
	}

	static {
		// Built-in starter raid: escalating Saiyan-saga waves that culminate in a Nappa boss fight.
		register(RaidType.builder(DEFAULT_ID)
				.name(Component.translatable("raid.dragonminez.saiyan_assault"))
				.activationRadius(48.0)
				.leashDistance(80.0)
				.interWaveDelay(100)
				// Wave 1 — a probing group of saibamen.
				.wave(RaidWave.builder()
						.add(MainEntities.SAGA_SAIBAMAN, 3)
						.build())
				// Wave 2 — more saibamen, slightly tougher.
				.wave(RaidWave.builder()
						.add(MainEntities.SAGA_SAIBAMAN, 5)
						.health(1.25).damage(1.15)
						.build())
				// Wave 3 — saibamen escorting Raditz.
				.wave(RaidWave.builder()
						.add(MainEntities.SAGA_SAIBAMAN, 4)
						.add(MainEntities.SAGA_RADITZ, 1)
						.health(1.5).damage(1.3)
						.build())
				// Final wave — Nappa boss with a saibaman guard.
				.wave(RaidWave.builder()
						.add(MainEntities.SAGA_SAIBAMAN, 3)
						.add(MainEntities.SAGA_NAPPA, 1)
						.health(2.0).damage(1.6)
						.boss(true)
						.build())
				.reward(RaidReward.builder()
						.item(() -> Items.DIAMOND, 4)
						.build())
				.build());
	}

	private RaidTypes() {}
}
