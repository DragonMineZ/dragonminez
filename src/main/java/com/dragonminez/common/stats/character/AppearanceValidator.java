package com.dragonminez.common.stats.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.hair.HairPresets;
import net.minecraft.util.Mth;

import java.util.regex.Pattern;

public final class AppearanceValidator {
	private static final Pattern HEX = Pattern.compile("^#?[0-9A-Fa-f]{6}$");
	public static final int MAX_STYLE_INDEX = 64;
	public static final float MIN_BOOB_SCALE = 0.75f;
	public static final float MAX_BOOB_SCALE = 1.25f;

	private AppearanceValidator() {
	}

	public static String color(String incoming, String current) {
		return incoming != null && HEX.matcher(incoming).matches() ? incoming : current;
	}

	public static float boobScale(float incoming, float current) {
		if (!Float.isFinite(incoming)) return Float.isFinite(current) ? current : 1.0f;
		return Mth.clamp(incoming, MIN_BOOB_SCALE, MAX_BOOB_SCALE);
	}

	public static int styleIndex(int incoming, int current) {
		return incoming >= 0 && incoming <= MAX_STYLE_INDEX ? incoming : current;
	}

	public static int tattoo(int incoming, int current) {
		return incoming >= Character.TATTOO_CUSTOM && incoming <= MAX_STYLE_INDEX ? incoming : current;
	}

	public static int hairId(int incoming, int current) {
		return incoming == 0 || HairPresets.exists(incoming) ? incoming : current;
	}

	public static String headBone(String race, String incoming, String current) {
		if (incoming == null || incoming.isEmpty()) return "";
		RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
		if (config == null || config.getHeadBones() == null) return current;
		for (String bone : config.getHeadBones()) {
			if (incoming.equals(bone)) return incoming;
		}
		return current;
	}

	public static String characterClass(String race, String incoming, String current) {
		RaceStatsConfig stats = ConfigManager.getRaceStats(race);
		if (stats == null) return current;
		if (stats.hasClass(incoming)) return incoming;
		if (stats.hasClass(current)) return current;
		if (stats.hasClass(Character.CLASS_WARRIOR)) return Character.CLASS_WARRIOR;
		return stats.getAllClasses().stream().sorted().findFirst().orElse(current);
	}

	public static String gender(String incoming) {
		return Character.GENDER_FEMALE.equals(incoming) ? Character.GENDER_FEMALE : Character.GENDER_MALE;
	}

	public static void applyBody(Character c, int bodyType, int eyesType, int noseType, int mouthType, int tattooType, float boobScale,
								 String activeHeadBone, String hairColor, String bodyColor, String bodyColor2, String bodyColor3,
								 String eye1Color, String eye2Color, String auraColor) {
		c.setBodyType(styleIndex(bodyType, c.getBodyType()));
		c.setEyesType(styleIndex(eyesType, c.getEyesType()));
		c.setNoseType(styleIndex(noseType, c.getNoseType()));
		c.setMouthType(styleIndex(mouthType, c.getMouthType()));
		c.setTattooType(tattoo(tattooType, c.getTattooType()));
		c.setBoobScale(boobScale(boobScale, c.getBoobScale()));
		c.setActiveHeadBone(headBone(c.getRaceName(), activeHeadBone, c.getActiveHeadBone()));
		c.setHairColor(color(hairColor, c.getHairColor()));
		c.setBodyColor(color(bodyColor, c.getBodyColor()));
		c.setBodyColor2(color(bodyColor2, c.getBodyColor2()));
		c.setBodyColor3(color(bodyColor3, c.getBodyColor3()));
		c.setEye1Color(color(eye1Color, c.getEye1Color()));
		c.setEye2Color(color(eye2Color, c.getEye2Color()));
		c.setAuraColor(color(auraColor, c.getAuraColor()));
	}
}
