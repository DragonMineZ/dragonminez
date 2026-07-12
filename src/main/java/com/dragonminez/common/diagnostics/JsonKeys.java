package com.dragonminez.common.diagnostics;

import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

public final class JsonKeys {

	private JsonKeys() {}

	private static boolean isComment(String key) {
		return key.isEmpty() || key.charAt(0) == '_' || key.charAt(0) == '$' || key.startsWith("//");
	}

	public static Set<String> of(String... keys) {
		return new HashSet<>(Set.of(keys));
	}

	public static Set<String> union(Set<String> base, String... extra) {
		Set<String> result = new HashSet<>(base);
		for (String key : extra) result.add(key);
		return result;
	}

	public static void checkObject(String source, String file, String path, JsonObject json, Set<String> allowed) {
		if (json == null) return;
		for (String key : json.keySet()) {
			if (isComment(key) || allowed.contains(key)) continue;
			JsonLoadReport.error(source, file, "unknown field '" + key + "'" + at(path) + " (ignored)");
		}
	}

	public static void reportBadType(String source, String file, String path, String rawType) {
		if (rawType == null || rawType.isBlank()) {
			JsonLoadReport.error(source, file, "missing 'type'" + at(path) + " (entry ignored)");
		} else {
			JsonLoadReport.error(source, file, "unknown type '" + rawType + "'" + at(path) + " (entry ignored)");
		}
	}

	private static String at(String path) {
		return (path == null || path.isEmpty()) ? "" : " in " + path;
	}
}
