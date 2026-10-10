package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.diagnostics.JsonLoadReport;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class QuestUpgrader {

	public static final String DEFAULTS_VERSION = "2.2.0";

	static final String VERSION_KEY = "defaultsVersion";

	private static final String PREVIOUS_QUESTS_ROOT = "/data/dragonminez/previousQuests/";
	private static final String OLD_BACKUP_DIR = "oldBackup";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final List<String> RETIRED_SAGA_DEFAULTS = List.of(
			"quests/saga_saiyan/02_survive_wilderness_training.json",
			"quests/saga_saiyan/03_kill_the_saibamans.json",
			"quests/saga_saiyan/04_hold_against_nappa.json",
			"quests/saga_saiyan/05_face_vegeta.json",
			"quests/saga_saiyan/06_defeat_oozaru_vegeta.json",
			"quests/saga_saiyan/07_prepare_for_namek.json",
			"quests/saga_saiyan/08_head_to_namek.json"
	);

	private static final List<String> RETIRED_SIDEQUEST_DEFAULTS = List.of(
			"sidequests/combat/dragon_ball_hunter.json",
			"sidequests/combat/namek_elder_blessing.json",
			"sidequests/story/chi_chi_gohan_worry.json"
	);

	@Setter
	private static boolean autoUpdateEnabled = true;

	private QuestUpgrader() {}

	public static void retireObsoleteDefaults(Path dmzBase, boolean sagaDefaults, boolean sideQuestDefaults) {
		if (sagaDefaults) retire(dmzBase, RETIRED_SAGA_DEFAULTS);
		if (sideQuestDefaults) retire(dmzBase, RETIRED_SIDEQUEST_DEFAULTS);
	}

	private static void retire(Path dmzBase, List<String> relativePaths) {
		for (String relative : relativePaths) {
			Path file = dmzBase.resolve(relative);
			if (!Files.isRegularFile(file)) continue;

			String stamp = readStamp(file);
			if (stamp != null && compareVersions(stamp, DEFAULTS_VERSION) > 0) continue;

			String from = stamp == null ? "unversioned" : stamp;
			try {
				Path target = freeBackupTarget(dmzBase, file);
				Files.createDirectories(target.getParent());
				Files.move(file, target);
				String backupRelative = relativeKey(dmzBase, target);
				QuestUpdateReport.retired(relative, backupRelative, from);
				LogUtil.warn(Env.COMMON, "Retired obsolete default quest file '{}' ({}): moved to '{}'", relative, from, backupRelative);
				JsonLoadReport.update("quests", relative, "obsolete default from an older version (" + from + "), moved to " + backupRelative);
			} catch (Exception e) {
				LogUtil.error(Env.COMMON, "Failed to retire obsolete default quest file '{}': {}", relative, e.toString());
				JsonLoadReport.error("quests", relative, "obsolete default could not be moved to " + OLD_BACKUP_DIR
						+ " (" + JsonLoadReport.rootCause(e) + "); delete it by hand, it collides with the current defaults");
			}
		}
	}

	private static Path freeBackupTarget(Path dmzBase, Path file) {
		Path target = dmzBase.resolve(OLD_BACKUP_DIR).resolve(dmzBase.relativize(file));
		if (!Files.exists(target)) return target;
		String name = target.getFileName().toString();
		int dot = name.lastIndexOf('.');
		String stem = dot > 0 ? name.substring(0, dot) : name;
		String extension = dot > 0 ? name.substring(dot) : "";
		for (int i = 1; ; i++) {
			Path candidate = target.resolveSibling(stem + "_" + i + extension);
			if (!Files.exists(candidate)) return candidate;
		}
	}

	private static String readStamp(Path file) {
		JsonObject json = readJson(file);
		if (json == null || !json.has(VERSION_KEY) || !json.get(VERSION_KEY).isJsonPrimitive()) return null;
		String stamp = json.get(VERSION_KEY).getAsString();
		return stamp.isBlank() ? null : stamp;
	}

	public static void upgradeOrWrite(Path dmzBase, Path file, JsonObject newDefault) {
		newDefault.addProperty(VERSION_KEY, DEFAULTS_VERSION);
		try {
			String relative = relativeKey(dmzBase, file);
			if (!Files.exists(file)) {
				writeJson(file, newDefault);
				return;
			}

			if (isBlank(file)) {
				writeJson(file, newDefault);
				QuestUpdateReport.forFile(relative, "empty file", DEFAULTS_VERSION).notes
						.add("the file was empty and was regenerated from the defaults");
				LogUtil.warn(Env.COMMON, "Quest file '{}' was empty; regenerated it from the defaults", relative);
				JsonLoadReport.update("quests", relative, "the file was empty, regenerated from the defaults");
				return;
			}

			if (!autoUpdateEnabled) return;

			JsonObject userObj = readJson(file);
			if (userObj == null) return;

			String userVersion = userObj.has(VERSION_KEY) && userObj.get(VERSION_KEY).isJsonPrimitive()
					? userObj.get(VERSION_KEY).getAsString() : null;
			if (userVersion != null && !userVersion.isBlank() && compareVersions(userVersion, DEFAULTS_VERSION) >= 0) return;

			JsonObject baseline = loadBaseline(relative);
			QuestUpdateReport.FileReport report = QuestUpdateReport.forFile(relative,
					userVersion == null || userVersion.isBlank() ? "unversioned" : userVersion, DEFAULTS_VERSION);

			JsonObject merged = deepCopy(newDefault);
			mergeInto(userObj, baseline, merged, "", report);
			merged.addProperty(VERSION_KEY, DEFAULTS_VERSION);

			if (report.hasChanges()) {
				backup(dmzBase, file);
				LogUtil.info(Env.COMMON, "Upgraded quest file '{}' ({} → {}): {} update(s), {} conflict(s), {} deletion(s) kept",
						relative, report.fromVersion, report.toVersion, report.appliedCount, report.conflicts.size(),
						report.keptDeletions.size());
			}
			writeJson(file, merged);
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to upgrade quest file '{}': {}", file.getFileName(), e.toString());
		}
	}

	static int compareVersions(String a, String b) {
		String coreA = versionCore(a);
		String coreB = versionCore(b);
		String[] partsA = coreA.split("\\.");
		String[] partsB = coreB.split("\\.");
		for (int i = 0; i < Math.max(partsA.length, partsB.length); i++) {
			int x = i < partsA.length ? leadingInt(partsA[i]) : 0;
			int y = i < partsB.length ? leadingInt(partsB[i]) : 0;
			if (x != y) return Integer.compare(x, y);
		}
		String preA = preRelease(a);
		String preB = preRelease(b);
		if (preA.isEmpty() || preB.isEmpty()) {
			return Boolean.compare(preA.isEmpty(), preB.isEmpty());
		}
		return comparePreRelease(preA, preB);
	}

	private static String versionCore(String version) {
		String v = version == null ? "" : version.trim();
		if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
		int cut = firstIndexOf(v, '-', '+');
		return cut >= 0 ? v.substring(0, cut) : v;
	}

	private static String preRelease(String version) {
		String v = version == null ? "" : version.trim();
		int dash = v.indexOf('-');
		if (dash < 0) return "";
		int plus = v.indexOf('+', dash);
		return plus >= 0 ? v.substring(dash + 1, plus) : v.substring(dash + 1);
	}

	private static int comparePreRelease(String a, String b) {
		String[] partsA = a.split("\\.");
		String[] partsB = b.split("\\.");
		for (int i = 0; i < Math.min(partsA.length, partsB.length); i++) {
			boolean numericA = partsA[i].matches("\\d+");
			boolean numericB = partsB[i].matches("\\d+");
			int result;
			if (numericA && numericB) result = Integer.compare(leadingInt(partsA[i]), leadingInt(partsB[i]));
			else if (numericA != numericB) result = numericA ? -1 : 1;
			else result = partsA[i].toLowerCase(Locale.ROOT).compareTo(partsB[i].toLowerCase(Locale.ROOT));
			if (result != 0) return result;
		}
		return Integer.compare(partsA.length, partsB.length);
	}

	private static int firstIndexOf(String value, char... chars) {
		int best = -1;
		for (char c : chars) {
			int index = value.indexOf(c);
			if (index >= 0 && (best < 0 || index < best)) best = index;
		}
		return best;
	}

	private static int leadingInt(String part) {
		int end = 0;
		while (end < part.length() && end < 9 && Character.isDigit(part.charAt(end)) && part.charAt(end) < 128) end++;
		return end == 0 ? 0 : Integer.parseInt(part.substring(0, end));
	}

	private static void mergeInto(JsonObject user, JsonObject baseline, JsonObject target,
								  String path, QuestUpdateReport.FileReport report) {
		for (String key : new ArrayList<>(target.keySet())) {
			if (VERSION_KEY.equals(key)) continue;
			String childPath = path.isEmpty() ? key : path + "." + key;
			JsonElement baseVal = child(baseline, key);
			JsonElement newVal = target.get(key);

			if (!user.has(key)) {
				if (baseVal == null) continue;
				if (baseVal.isJsonNull()) {
					if (!newVal.isJsonNull()) report.appliedCount++;
					continue;
				}
				target.remove(key);
				report.keptDeletions.add(childPath);
				continue;
			}

			JsonElement userVal = user.get(key);
			if (userVal.isJsonObject() && newVal.isJsonObject()) {
				JsonObject baseObj = baseVal != null && baseVal.isJsonObject() ? baseVal.getAsJsonObject() : null;
				mergeInto(userVal.getAsJsonObject(), baseObj, newVal.getAsJsonObject(), childPath, report);
			} else if (userVal.isJsonArray() && newVal.isJsonArray()) {
				JsonArray baseArr = baseVal != null && baseVal.isJsonArray() ? baseVal.getAsJsonArray() : null;
				target.add(key, mergeArray(userVal.getAsJsonArray(), baseArr, newVal.getAsJsonArray(), childPath, report));
			} else {
				resolveScalar(target, key, childPath, userVal, baseVal, newVal, report);
			}
		}

		for (String key : user.keySet()) {
			if (target.has(key) || VERSION_KEY.equals(key)) continue;
			JsonElement userVal = user.get(key);
			JsonElement baseVal = child(baseline, key);
			if (baseVal != null && equal(userVal, baseVal)) {
				report.appliedCount++;
				continue;
			}
			target.add(key, userVal);
			if (baseVal != null) {
				String childPath = path.isEmpty() ? key : path + "." + key;
				report.conflicts.add(new QuestUpdateReport.Conflict(childPath, asText(userVal), asText(baseVal), "(removed)"));
			}
		}
	}

	private static void resolveScalar(JsonObject target, String key, String path,
									  JsonElement userVal, JsonElement baseVal, JsonElement newVal,
									  QuestUpdateReport.FileReport report) {
		if (equal(userVal, newVal)) return;

		if (baseVal != null) {
			if (equal(userVal, baseVal)) {
				report.appliedCount++;
				return;
			}
			target.add(key, userVal);
			if (!equal(newVal, baseVal)) {
				report.conflicts.add(new QuestUpdateReport.Conflict(
						path, asText(userVal), asText(baseVal), asText(newVal)));
			}
			return;
		}

		target.add(key, userVal);
	}

	private static JsonArray mergeArray(JsonArray userArr, JsonArray baseArr, JsonArray newArr,
										String path, QuestUpdateReport.FileReport report) {
		if (equal(userArr, newArr)) return newArr;
		if (baseArr == null) return userArr;
		if (equal(userArr, baseArr)) {
			report.appliedCount++;
			return newArr;
		}
		if (equal(newArr, baseArr)) return userArr;

		if (isAligned(userArr, baseArr, newArr)) {
			for (int i = 0; i < newArr.size(); i++) {
				mergeInto(userArr.get(i).getAsJsonObject(), baseArr.get(i).getAsJsonObject(),
						newArr.get(i).getAsJsonObject(), path + "[" + i + "]", report);
			}
			return newArr;
		}

		report.conflicts.add(new QuestUpdateReport.Conflict(
				path + "[]", "user-edited list", "old default list", "new default list"));
		return userArr;
	}

	private static boolean isAligned(JsonArray userArr, JsonArray baseArr, JsonArray newArr) {
		if (userArr.size() != baseArr.size() || baseArr.size() != newArr.size()) return false;
		for (int i = 0; i < newArr.size(); i++) {
			JsonElement u = userArr.get(i);
			JsonElement b = baseArr.get(i);
			JsonElement n = newArr.get(i);
			if (!u.isJsonObject() || !b.isJsonObject() || !n.isJsonObject()) return false;
			String typeU = elementType(u.getAsJsonObject());
			if (!typeU.equals(elementType(b.getAsJsonObject())) || !typeU.equals(elementType(n.getAsJsonObject()))) return false;
		}
		return true;
	}

	private static String elementType(JsonObject obj) {
		JsonElement type = obj.get("type");
		return type != null && type.isJsonPrimitive() ? type.getAsString().trim().toUpperCase(Locale.ROOT) : "";
	}

	private static JsonElement child(JsonObject obj, String key) {
		return obj != null && obj.has(key) ? obj.get(key) : null;
	}

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

	private static boolean isBlank(Path file) throws IOException {
		return Files.size(file) == 0 || Files.readString(file, StandardCharsets.UTF_8).isBlank();
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
		Path dir = file.toAbsolutePath().getParent();
		Files.createDirectories(dir);
		Path temp = Files.createTempFile(dir, file.getFileName().toString() + ".", ".tmp");
		try {
			try (Writer w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(obj, w);
			}
			try {
				Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}
}
