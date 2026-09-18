package com.dragonminez.common.hair;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.character.Character;

import java.util.Locale;

public final class HairManager {
	private static final String[] DEFAULT_HAIR_RACES = {"human", "saiyan"};

	private HairManager() {}

	public static boolean canUseHair(Character character) {
		if (character == null) return false;
		String race = character.getRace().toLowerCase(Locale.ROOT);
		String gender = character.getGender().toLowerCase(Locale.ROOT);

		for (String defaultRace : DEFAULT_HAIR_RACES) {
			if (race.equals(defaultRace)) return true;
		}
		if (race.equals("majin") && gender.equals("female")) return true;

		RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
		if (config != null && config.getHeadBones() != null) {
			for (String bone : config.getHeadBones()) {
				if (bone.equals("hair")) return true;
			}
		}
		return false;
	}

	public static CustomHair getPresetStyle(int presetId, HairStyleSlot slot) {
		return HairPresets.get(presetId, slot);
	}

	public static int getPresetCount() {
		return HairPresets.count();
	}
}
