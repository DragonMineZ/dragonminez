package com.dragonminez.common.racial;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class RacialRegistry {
	private static final Map<String, RacialAbility> ABILITIES = new HashMap<>();

	private RacialRegistry() {
	}

	public static void register(RacialAbility ability) {
		ABILITIES.put(ability.id(), ability);
	}

	public static Optional<RacialAbility> get(String id) {
		return Optional.ofNullable(id).map(ABILITIES::get);
	}

	public static Optional<RacialAbility> forPlayer(StatsData data) {
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(data.getCharacter().getRace());
		if (config == null) return Optional.empty();
		return get(config.getRacialSkill());
	}
}
