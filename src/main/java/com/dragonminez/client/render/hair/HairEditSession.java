package com.dragonminez.client.render.hair;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairStyleSlot;
import com.dragonminez.common.stats.character.Character;

import java.util.EnumMap;
import java.util.UUID;

public final class HairEditSession {
	private static UUID owner;
	private static EnumMap<HairStyleSlot, CustomHair> styles;
	private static boolean renderHairBase;

	private HairEditSession() {}

	public static void begin(UUID playerId, EnumMap<HairStyleSlot, CustomHair> workingStyles, boolean hairBase) {
		owner = playerId;
		styles = workingStyles;
		renderHairBase = hairBase;
	}

	public static void end() {
		owner = null;
		styles = null;
	}

	public static boolean isActiveFor(UUID playerId) {
		return owner != null && styles != null && owner.equals(playerId);
	}

	public static CustomHair resolve(UUID playerId, HairStyleSlot slot) {
		if (!isActiveFor(playerId)) return null;
		return Character.resolveOwnStyle(styles, slot);
	}

	public static void setRenderHairBase(boolean value) {
		renderHairBase = value;
	}

	public static Boolean renderHairBaseOverride(UUID playerId) {
		return isActiveFor(playerId) ? renderHairBase : null;
	}
}
