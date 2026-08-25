package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

public final class QuestUpgrader {

	public static final String DEFAULTS_VERSION = "2.1.2";

	static final String VERSION_KEY = "defaultsVersion";

	private static final String PREVIOUS_QUESTS_ROOT = "/data/dragonminez/previousQuests/";
	private static final String OLD_BACKUP_DIR = "oldBackup";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@Setter
    private static boolean autoUpdateEnabled = true;

	private QuestUpgrader() {}

    public static void upgradeOrWrite(Path dmzBase, Path file, JsonObject newDefault) {
		newDefault.addProperty(VERSION_KEY, DEFAULTS_VERSION);
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(file.getParent());
				writeJson(file, newDefault);
				return;
			}

			if (!autoUpdateEnabled) return;

			JsonObject userObj = readJson(file);
			if (userObj == null) return; // unreadable/corrupt user file — leave it, don't clobber

			String userVersion = userObj.has(VERSION_KEY) && userObj.get(VERSION_KEY).isJsonPrimitive()
					? userObj.get(VERSION_KEY).getAsString() : null;
			if (DEFAULTS_VERSION.equals(userVersion)) return; // already current — fast path

			String relative = relativeKey(dmzBase, file);
			JsonObject baseline = loadBaseline(relative);
			QuestUpdateReport.FileReport report =
					QuestUpdateReport.forFile(relative, userVersion == null ? "unversioned" : userVersion, DEFAULTS_VERSION);

			JsonObject merged = deepCopy(newDefault);
			mergeInto(userObj, baseline, merged, "", report);
			merged.addProperty(VERSION_KEY, DEFAULTS_VERSION);

			// Back up only when the merge actually altered the player's content. A pure re-stamp
			// (no applied updates, no conflicts) rewrites in place without cluttering oldBackup/.
			if (report.hasChanges()) {
				backup(dmzBase, file);
				LogUtil.info(Env.COMMON, "Upgraded quest file '{}' ({} → {}): {} update(s), {} conflict(s)",
						relative, report.fromVersion, report.toVersion, report.appliedCount, report.conflicts.size());
			}
			writeJson(file, merged);
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to upgrade quest file '{}': {}", file.getFileName(), e.getMessage());
		}
	}

	// ===============================================================================================
	// Three-way merge (JSON-only)
	// ===============================================================================================

	private static void mergeInto(JsonObject user, JsonObject baseline, JsonObject target,
								  String path, QuestUpdateReport.FileReport report) {
		for (String key : new ArrayList<>(target.keySet())) {
			if (VERSION_KEY.equals(key) || !user.has(key)) continue;
			JsonElement userVal = user.get(key);
			JsonElement newVal = target.get(key);
			JsonElement baseVal = child(baseline, key);
			String childPath = path.isEmpty() ? key : path + "." + key;

			if (userVal.isJsonObject() && newVal.isJsonObject()) {
				JsonObject baseObj = (baseVal != null && baseVal.isJsonObject()) ? baseVal.getAsJsonObject() : null;
				mergeInto(userVal.getAsJsonObject(), baseObj, newVal.getAsJsonObject(), childPath, report);
			} else if (userVal.isJsonArray() && newVal.isJsonArray()) {
				JsonArray baseArr = (baseVal != null && baseVal.isJsonArray()) ? baseVal.getAsJsonArray() : null;
				target.add(key, mergeArray(userVal.getAsJsonArray(), baseArr, newVal.getAsJsonArray(), childPath, report));
			} else {
				resolveScalar(target, key, childPath, userVal, baseVal, newVal, report);
			}
		}
		// Preserve keys the user added that the new default doesn't know about.
		for (String key : user.keySet()) {
			if (!target.has(key) && !VERSION_KEY.equals(key)) target.add(key, user.get(key));
		}
	}

	private static void resolveScalar(JsonObject target, String key, String path,
									  JsonElement userVal, JsonElement baseVal, JsonElement newVal,
									  QuestUpdateReport.FileReport report) {
		boolean userMatchesNew = equal(userVal, newVal);
		if (userMatchesNew) return; // nothing to decide

		if (baseVal != null) {
			boolean userEdited = !equal(userVal, baseVal);
			boolean newChanged = !equal(newVal, baseVal);
			if (!userEdited) {
				// User left the old default → adopt the new default (already in target).
				report.appliedCount++;
				return;
			}
			// User edited this field. Keep their value.
			target.add(key, userVal);
			if (newChanged) {
				// Both sides changed it differently → conflict.
				report.conflicts.add(new QuestUpdateReport.Conflict(
						path, asText(userVal), asText(baseVal), asText(newVal)));
			}
			return;
		}

		// No baseline for this field: can't tell an edit from the old default. Preserve the user's
		// value (non-destructive), consistent with the config merge's no-baseline fallback.
		target.add(key, userVal);
	}

	private static JsonArray mergeArray(JsonArray userArr, JsonArray baseArr, JsonArray newArr,
										String path, QuestUpdateReport.FileReport report) {
		if (baseArr != null && equal(userArr, baseArr)) {
			// Untouched by the user: adopt the (possibly rebalanced) new array.
			if (!equal(newArr, baseArr)) report.appliedCount++;
			return newArr;
		}
		if (baseArr != null && equal(newArr, baseArr)) {
			return userArr; // only the user changed it
		}
		// Both changed, or no baseline to compare against → keep the user's array.
		if (baseArr != null) {
			report.conflicts.add(new QuestUpdateReport.Conflict(
					path + "[]", "user-edited list", "old default list", "new default list"));
		}
		return userArr;
	}

	// ===============================================================================================
	// Helpers
	// ===============================================================================================

	private static JsonElement child(JsonObject obj, String key) {
		return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key) : null;
	}

	/** Numeric-aware equality so {@code 6} and {@code 6.0} don't read as a difference. */
	private static boolean equal(JsonElement a, JsonElement b) {
		if (b == null) return false;
		if (a.isJsonPrimitive() && b.isJsonPrimitive()) {
			var pa = a.getAsJsonPrimitive();
			var pb = b.getAsJsonPrimitive();
			if (pa.isNumber() && pb.isNumber()) {
				try { return pa.getAsBigDecimal().compareTo(pb.getAsBigDecimal()) == 0; }
				catch (NumberFormatException e) { return pa.getAsString().equals(pb.getAsString()); }
			}
		}
		return a.equals(b);
	}

	private static String asText(JsonElement e) {
		return e == null ? "(absent)" : e.toString();
	}

	private static JsonObject deepCopy(JsonObject obj) {
		return JsonParser.parseString(obj.toString()).getAsJsonObject();
	}

	private static String relativeKey(Path dmzBase, Path file) {
		return dmzBase.relativize(file).toString().replace('\\', '/');
	}

	/** Loads the previous release's default for this file from the jar, or {@code null} if absent. */
	static JsonObject loadBaseline(String relativeKey) {
		String resource = PREVIOUS_QUESTS_ROOT + relativeKey;
		try (InputStream in = QuestUpgrader.class.getResourceAsStream(resource)) {
			if (in == null) return null;
			JsonElement parsed = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static void backup(Path dmzBase, Path file) {
		try {
			Path backup = dmzBase.resolve(OLD_BACKUP_DIR).resolve(dmzBase.relativize(file));
			Files.createDirectories(backup.getParent());
			Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to back up quest file '{}': {}", file.getFileName(), e.getMessage());
		}
	}

	private static JsonObject readJson(Path file) {
		try {
			JsonElement parsed = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
			return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
		} catch (Exception e) {
			LogUtil.warn(Env.COMMON, "Could not read quest file '{}' for upgrade: {}", file.getFileName(), e.getMessage());
			return null;
		}
	}

	private static void writeJson(Path file, JsonObject obj) throws IOException {
		try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(obj, w);
		}
	}
}
