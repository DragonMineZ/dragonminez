package com.dragonminez.client.gui.character.util;

import com.dragonminez.common.config.RaceStatsConfig;

import java.util.Locale;

public final class RegenScalingFormat {
	private RegenScalingFormat() {
	}

	public static String stamina(RaceStatsConfig.ClassStats classStats) {
		double perRes = classStats.resolveSp5ResScaling() * 0.2;
		double perVit = classStats.resolveSp5VitScaling() * 0.2;
		StringBuilder sb = new StringBuilder(" (");
		if (perRes > 0) sb.append(String.format(Locale.US, "+%.3f/RES", perRes));
		if (perVit > 0) {
			if (perRes > 0) sb.append(", ");
			sb.append(String.format(Locale.US, "+%.3f/VIT", perVit));
		}
		if (perRes <= 0 && perVit <= 0) return "";
		return sb.append(')').toString();
	}
}
