package com.dragonminez.common.util;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.character.Character;
import com.google.gson.Gson;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class FusionAppearance {
	private static final Gson GSON = new Gson();
	private static final float FORM_COLOR_WEIGHT = 0.5f;
	private static final int MAX_ENTRIES_PER_CHARACTER = 32;
	private static final Map<Character, Map<FormConfig.FormData, Entry>> CACHE = new WeakHashMap<>();

	private record Entry(String signature, FormConfig.FormData presented) {
	}

	private FusionAppearance() {
	}

	public static FormConfig.FormData present(Character character, String group, FormConfig.FormData raw) {
		if (raw == null || character == null) return raw;
		boolean foreign = FusionForms.isQualified(group);
		String signature = character.getRaceName() + '|' + character.getBodyColor() + '|' + character.getBodyColor2() + '|'
				+ character.getBodyColor3() + '|' + character.getHairColor() + '|' + character.getAuraColor() + '|' + foreign;

		synchronized (CACHE) {
			Map<FormConfig.FormData, Entry> entries = CACHE.computeIfAbsent(character, key -> new IdentityHashMap<>());
			Entry entry = entries.get(raw);
			if (entry != null && entry.signature().equals(signature)) return entry.presented();
			if (entries.size() >= MAX_ENTRIES_PER_CHARACTER) entries.clear();
			FormConfig.FormData presented = build(character, raw, foreign);
			entries.put(raw, new Entry(signature, presented));
			return presented;
		}
	}

	private static FormConfig.FormData build(Character character, FormConfig.FormData raw, boolean foreign) {
		FormConfig.FormData copy = GSON.fromJson(GSON.toJson(raw), FormConfig.FormData.class);
		copy.setBodyColor1(blend(character.getBodyColor(), raw.getBodyColor1()));
		copy.setBodyColor2(blend(character.getBodyColor2(), raw.getBodyColor2()));
		copy.setBodyColor3(blend(character.getBodyColor3(), raw.getBodyColor3()));
		copy.setHairColor(blend(character.getHairColor(), raw.getHairColor()));
		copy.setAuraColor(blend(character.getAuraColor(), raw.getAuraColor()));

		if (foreign) {
			copy.setHairType("");
			copy.setForcedHairCode("");
			copy.setKeepBaseFormHeadBones(true);
		}
		return copy;
	}

	private static String blend(String base, String form) {
		if (form == null || form.isEmpty()) return form;
		int[] baseRgb = parse(base);
		int[] formRgb = parse(form);
		if (baseRgb == null || formRgb == null) return form;
		int r = Math.round(baseRgb[0] + (formRgb[0] - baseRgb[0]) * FORM_COLOR_WEIGHT);
		int g = Math.round(baseRgb[1] + (formRgb[1] - baseRgb[1]) * FORM_COLOR_WEIGHT);
		int b = Math.round(baseRgb[2] + (formRgb[2] - baseRgb[2]) * FORM_COLOR_WEIGHT);
		return String.format("#%02X%02X%02X", r, g, b);
	}

	private static int[] parse(String hex) {
		if (hex == null) return null;
		String value = hex.trim();
		if (value.startsWith("#")) value = value.substring(1);
		if (value.length() != 6) return null;
		try {
			int rgb = Integer.parseInt(value, 16);
			return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
