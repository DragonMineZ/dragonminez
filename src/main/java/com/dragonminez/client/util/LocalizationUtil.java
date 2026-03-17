package com.dragonminez.client.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalizationUtil {

	private static final Pattern OBJECTIVE_SUFFIX = Pattern.compile(".*\\.obj(\\d+)$");
	private static final Pattern SAGA_QUEST_ID = Pattern.compile("([a-z_]+?)(\\d+)$");


	// This whole class is mostly for Quests
	private LocalizationUtil() {
	}

	public static Component localizedOrReadable(String raw) {
		if (raw == null || raw.isBlank()) return Component.empty();
		if (Language.getInstance().has(raw)) return Component.translatable(raw);
		return Component.literal(humanize(raw));
	}

	public static String localizedOrReadableText(String raw) {
		return localizedOrReadable(raw).getString();
	}

	private static String humanize(String key) {
		Matcher objectiveMatcher = OBJECTIVE_SUFFIX.matcher(key);
		if (objectiveMatcher.matches()) {
			return "Objective " + objectiveMatcher.group(1);
		}

		if (key.endsWith(".name") || key.endsWith(".desc")) {
			String base = key.substring(0, key.lastIndexOf('.'));
			String token = base.substring(base.lastIndexOf('.') + 1);
			if (key.contains("dmz.quest.")) {
				Matcher sagaMatcher = SAGA_QUEST_ID.matcher(token);
				if (sagaMatcher.matches()) {
					String saga = titleCase(sagaMatcher.group(1).replace('_', ' '));
					String questNo = sagaMatcher.group(2);
					return key.endsWith(".name")
							? saga + " Quest " + questNo
							: "Complete " + saga + " quest " + questNo + ".";
				}
			}
			String pretty = titleCase(token.replace('_', ' '));
			if (key.endsWith(".name")) return pretty;
			return "Complete this quest step.";
		}

		return key;
	}

	private static String titleCase(String raw) {
		if (raw.isBlank()) return raw;
		String[] words = raw.toLowerCase(Locale.ROOT).split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			if (word.isBlank()) continue;
			if (!sb.isEmpty()) sb.append(' ');
			sb.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) sb.append(word.substring(1));
		}
		return sb.toString();
	}
}

