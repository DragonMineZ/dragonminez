package com.dragonminez.common.passives;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.config.RaceStatsConfig.Passive;
import com.dragonminez.common.passives.handlers.BerserkerPassive;
import com.dragonminez.common.passives.handlers.EnchanterPassive;
import com.dragonminez.common.passives.handlers.MartialArtistPassive;
import com.dragonminez.common.passives.handlers.PaladinPassive;
import com.dragonminez.common.passives.handlers.SpiritualistPassive;
import com.dragonminez.common.passives.handlers.TankPassive;
import com.dragonminez.common.passives.handlers.WarriorPassive;
import com.dragonminez.common.stats.StatsData;

import java.util.HashMap;
import java.util.Map;

public final class ClassPassives {

	public static final IClassPassive NONE = () -> "";

	private static final Map<String, IClassPassive> REGISTRY = new HashMap<>();

	static {
		register(new WarriorPassive());
		register(new MartialArtistPassive());
		register(new SpiritualistPassive());
		register(new BerserkerPassive());
		register(new PaladinPassive());
		register(new TankPassive());
		register(new EnchanterPassive());
	}

	private ClassPassives() {}

	public static void register(IClassPassive passive) {
		if (passive != null && passive.classKey() != null && !passive.classKey().isEmpty()) {
			REGISTRY.put(passive.classKey().toLowerCase(), passive);
		}
	}

	public static IClassPassive get(StatsData data) {
		Passive cfg = configFor(data);
		if (cfg == null || !cfg.isEnabled()) return NONE;
		String characterClass = data.getCharacter().getCharacterClass();
		if (characterClass == null) return NONE;
		IClassPassive passive = REGISTRY.get(characterClass.toLowerCase());
		return passive != null ? passive : NONE;
	}

	public static Passive configFor(StatsData data) {
		if (data == null || data.getCharacter() == null) return null;
		String race = data.getCharacter().getRaceName();
		String characterClass = data.getCharacter().getCharacterClass();
		if (race == null || characterClass == null) return null;
		RaceStatsConfig raceConfig = ConfigManager.getRaceStats(race);
		if (raceConfig == null) return null;
		RaceStatsConfig.ClassStats classStats = raceConfig.getClassStats(characterClass);
		if (classStats == null) return null;
		return classStats.getPassive();
	}

	public static double value(StatsData data, String key, double def) {
		Passive p = configFor(data);
		if (p == null || p.getValues() == null) return def;
		Double v = p.getValues().get(key);
		return v != null ? v : def;
	}

	public static boolean is(StatsData data, String classKey) {
		return classKey != null && classKey.equalsIgnoreCase(get(data).classKey());
	}
}
