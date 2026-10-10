package com.dragonminez.common.quest;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Registry for all quest types — saga quests, (side)-quests, dailies, and events.
 * <p>
 * This is the <b>sole quest loader</b>. It loads:
 * <ul>
 *   <li><b>Saga manifest files</b> from {@code dragonminez/sagas/} — each saga references a quest folder</li>
 *   <li><b>Side-quest files</b> from {@code dragonminez/sidequests/} — each quest is its own JSON</li>
 *   <li><b>Individual quest files</b> from {@code dragonminez/quests/} — new unified format (future expansion)</li>
 * </ul>
 * <p>
 * On first run, default saga and side-quest files are generated automatically via
 * {@link SagaDefaults} and {@link SideQuestDefaults}.
 * Note: Default SideQuests are tied by default to some Saga in specific.
 *
 * <h3>Folder Structure</h3>
 * <pre>
 * worldfolder/
 *   dragonminez/
 *     sagas/                           ← Saga manifest files (reference quest folders)
 *       saiyan_saga.json               →  {"id":"saiyan_saga", "questFolder":"saga_saiyan", ...}
 *       frieza_saga.json
 *       android_saga.json
 *     quests/                          ← Quest files organized by saga folder
 *       saga_saiyan/
 *         01_find_roshi.json           →  {"id":1, "title":"...", "objectives":[...], "rewards":[...]}
 *         02_defeat_raditz.json
 *         ...
 *       saga_frieza/
 *         01_defeat_cui.json
 *         ...
 *     sidequests/                      ← Side-quest JSON files
 *       training/
 *         roshi_basic_training.json
 *       exploration/
 *         world_explorer.json
 *       combat/
 *         monster_hunter.json
 * </pre>
 *
 * @since 2.1
 */
public class QuestRegistry {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final String SAGA_FOLDER = "dragonminez" + File.separator + "sagas";
	private static final String SIDEQUEST_FOLDER = "dragonminez" + File.separator + "sidequests";
	private static final String QUESTS_FOLDER = "dragonminez" + File.separator + "quests";

	// ========================================================================================
	// Server-Side State
	// ========================================================================================

	/** All loaded quests, keyed by their effective string ID. */
	private static final Map<String, Quest> LOADED_QUESTS = new LinkedHashMap<>();

	/** All loaded saga manifests, keyed by saga ID. */
	private static final Map<String, Saga> LOADED_SAGAS = new LinkedHashMap<>();

	// ========================================================================================
	// Client-Side State (synced from server)
	// ========================================================================================

	private static final Map<String, Quest> CLIENT_QUESTS = new LinkedHashMap<>();
	private static final Map<String, Saga> CLIENT_SAGAS = new LinkedHashMap<>();

	// ========================================================================================
	// Performance Indexes
	// ========================================================================================

	/** ObjectiveType → list of quest IDs containing that objective type. */
	private static final Map<QuestObjective.ObjectiveType, List<String>> OBJECTIVE_INDEX = new EnumMap<>(QuestObjective.ObjectiveType.class);

	/** NPC ID → list of quest IDs where that NPC is the quest giver. */
	private static final Map<String, List<String>> QUEST_GIVER_INDEX = new HashMap<>();

	/** NPC ID → list of quest IDs where that NPC is the turn-in target. */
	private static final Map<String, List<String>> TURN_IN_INDEX = new HashMap<>();

	private static final Comparator<Path> QUEST_FILE_ORDER =
			Comparator.comparing((Path path) -> path.getFileName().toString(), QuestRegistry::compareQuestFileNames);

	/** Cached world folder path for resolving quest folders during saga loading. */
	private static Path cachedWorldFolder = null;

	// ========================================================================================
	// Initialization
	// ========================================================================================

	/** Called during mod setup to ensure the class is loaded. */
	public static void init() {
		// No-op — ensures class is loaded
	}

	// ========================================================================================
	// Loading — Primary Entry Point
	// ========================================================================================

	/**
	 * Loads all quests from all sources. This is the sole loading method.
	 * <p>
	 * Loading order:
	 * <ol>
	 *   <li>Generate default quest files in {@code dragonminez/quests/} if missing</li>
	 *   <li>Load saga manifest files from {@code dragonminez/sagas/} — each saga references a quest folder</li>
	 *   <li>Load side-quest JSON files from {@code dragonminez/sidequests/}</li>
	 *   <li>Build performance indexes</li>
	 * </ol>
	 */
	public static void loadAll(@Nullable MinecraftServer server) {
		LOADED_QUESTS.clear();
		LOADED_SAGAS.clear();
		OBJECTIVE_INDEX.clear();
		QUEST_GIVER_INDEX.clear();
		TURN_IN_INDEX.clear();

		if (server == null) {
			LogUtil.error(Env.COMMON, "QuestRegistry: cannot load — server is null");
			return;
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			LogUtil.error(Env.COMMON, "QuestRegistry: cannot load — overworld is null");
			return;
		}

		Path worldFolder = overworld.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
		cachedWorldFolder = worldFolder;

		QuestUpdateReport.clear();
		JsonLoadReport.clear("quests");
		QuestUpgrader.setAutoUpdateEnabled(
				ConfigManager.getServerConfig().getGameplay().getAutoUpdateQuests());

		boolean storyMode = ConfigManager.getServerConfig().getGameplay().getStoryModeEnabled();
		QuestUpgrader.retireObsoleteDefaults(worldFolder.resolve("dragonminez"),
				storyMode && ConfigManager.getServerConfig().getGameplay().getCreateDefaultSagas(),
				ConfigManager.getServerConfig().getGameplay().getCreateDefaultSideQuests());

		// --- Step 1: Generate default quest files (before sagas, so sagas can reference them) ---
		Path questsDir = worldFolder.resolve(QUESTS_FOLDER);
		try {
			if (!Files.exists(questsDir)) Files.createDirectories(questsDir);
			QuestDefaults.createDefaultQuestFiles(questsDir);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to create quests directory", e);
		}

		// --- Step 2: Load sagas (reads quest files from quests/ subfolders) ---
		if (storyMode) {
			loadSagaFiles(worldFolder.resolve(SAGA_FOLDER));
		}

		// --- Step 3: Load side-quests ---
		if (ConfigManager.getServerConfig().getGameplay().getSideQuestsEnabled()) {
			loadSideQuestFiles(worldFolder.resolve(SIDEQUEST_FOLDER));
		}


		// --- Step 4: Build indexes ---
		buildIndexes();

		int sagaQuestCount = 0;
		for (Saga saga : LOADED_SAGAS.values()) {
			sagaQuestCount += saga.getQuests().size();
		}
		int sideCount = (int) LOADED_QUESTS.values().stream().filter(Quest::isSideQuest).count();
		LogUtil.info(Env.COMMON, "QuestRegistry: loaded {} quest(s) ({} saga quests across {} sagas, {} sidequests)",
				LOADED_QUESTS.size(), sagaQuestCount, LOADED_SAGAS.size(), sideCount);

		emitUpgradeReport(worldFolder);
	}

	private static void emitUpgradeReport(Path worldFolder) {
		if (QuestUpdateReport.isEmpty()) return;

		int applied = QuestUpdateReport.totalApplied();
		int conflicts = QuestUpdateReport.totalConflicts();
		int keptDeletions = QuestUpdateReport.totalKeptDeletions();
		List<QuestUpdateReport.Retired> retired = QuestUpdateReport.retiredFiles();
		LogUtil.info(Env.COMMON, "QuestRegistry: quest defaults upgraded — {} value(s) auto-updated, {} conflict(s) kept as your edits, {} deletion(s) kept across {} file(s); {} obsolete default file(s) retired",
				applied, conflicts, keptDeletions, QuestUpdateReport.changedFiles().size(), retired.size());
		if (!QuestUpdateReport.changedFiles().isEmpty()) {
			JsonLoadReport.update("quests", "quest defaults", updateReportSummary());
		}

		StringBuilder sb = new StringBuilder();
		sb.append("DragonMineZ quest update report\n");
		sb.append("Target defaults version: ").append(QuestUpgrader.DEFAULTS_VERSION).append('\n');
		sb.append(applied).append(" value(s) auto-updated to the new defaults; ")
				.append(conflicts).append(" conflict(s) left as your edits; ")
				.append(keptDeletions).append(" field(s) you deleted were left deleted.\n");
		sb.append("Originals were backed up under dragonminez/oldBackup/.\n\n");
		if (!retired.isEmpty()) {
			sb.append("Obsolete default files from an older version (moved out so they no longer collide with the current defaults):\n");
			for (QuestUpdateReport.Retired file : retired) {
				sb.append("  ").append(file.relativePath()).append(" (").append(file.fromVersion()).append(") -> ")
						.append(file.backupPath()).append('\n');
			}
			sb.append('\n');
		}
		for (QuestUpdateReport.FileReport file : QuestUpdateReport.changedFiles()) {
			sb.append(file.relativePath).append(" (").append(file.fromVersion).append(" -> ").append(file.toVersion).append(")\n");
			if (file.appliedCount > 0) {
				sb.append("  auto-updated ").append(file.appliedCount).append(" field(s) you had left at the old default\n");
			}
			for (String note : file.notes) {
				sb.append("  ").append(note).append('\n');
			}
			for (String deleted : file.keptDeletions) {
				sb.append("  KEPT DELETION at '").append(deleted).append("': you removed it, so the new default was not added back\n");
			}
			for (QuestUpdateReport.Conflict c : file.conflicts) {
				sb.append("  CONFLICT at '").append(c.path()).append("': kept your value ").append(c.userValue())
						.append(" (old default ").append(c.oldDefault()).append(", new default ").append(c.newDefault()).append(")\n");
			}
			sb.append('\n');
		}

		try {
			Path reportFile = worldFolder.resolve("dragonminez").resolve("quest_update_report.txt");
			Files.createDirectories(reportFile.getParent());
			Files.writeString(reportFile, sb.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to write quest update report: {}", e.getMessage());
		}
	}

	public static boolean hasPendingUpdateReport() {
		return !QuestUpdateReport.isEmpty();
	}

	public static String updateReportSummary() {
		return String.format("DragonMineZ upgraded %d quest file(s): %d value(s) auto-updated, %d conflict(s) kept as your edits, %d deletion(s) kept. See dragonminez/quest_update_report.txt.",
				QuestUpdateReport.changedFiles().size(), QuestUpdateReport.totalApplied(), QuestUpdateReport.totalConflicts(),
				QuestUpdateReport.totalKeptDeletions());
	}

	// ========================================================================================
	// Saga Loading
	// ========================================================================================

	/**
	 * Loads saga JSON files from the given directory. Generates defaults if configured.
	 */
	private static void loadSagaFiles(Path sagaDir) {
		try {
			if (!Files.exists(sagaDir)) {
				Files.createDirectories(sagaDir);
			}
			SagaDefaults.createDefaultSagaFiles(sagaDir);

			List<Path> files;
			try (var stream = Files.walk(sagaDir)) {
				files = stream.filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".json"))
						.sorted(Comparator.comparing(path -> relativeName(sagaDir, path)))
						.toList();
			}
			Map<String, String> sagaSources = new HashMap<>();
			for (Path file : files) {
				loadSingleSagaFile(sagaDir, file, sagaSources);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to load saga files from {}", sagaDir, e);
		}
	}

	/**
	 * Loads a Single Default Saga File, used in loadSagaFiles.
	 */
	private static void loadSingleSagaFile(Path sagaDir, Path file, Map<String, String> sagaSources) {
		String display = "sagas/" + relativeName(sagaDir, file);
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null) {
				reportError(display, "the file is empty, saga skipped");
				return;
			}
			QuestParser.validateSaga("quests", display, root);
			for (String key : new String[]{"id", "name"}) {
				if (!root.has(key) || !root.get(key).isJsonPrimitive() || root.get(key).getAsString().isBlank()) {
					reportError(display, "missing required field '" + key + "', saga skipped");
					return;
				}
			}

			String sagaId = root.get("id").getAsString();
			String previous = sagaSources.putIfAbsent(sagaId, display);
			if (previous != null) {
				reportError(display, "duplicate saga id '" + sagaId + "' (already loaded from " + previous + "), saga skipped");
				return;
			}

			Saga saga = parseSagaFromJson(root, cachedWorldFolder);
			LOADED_SAGAS.put(saga.getId(), saga);

			for (Quest quest : saga.getQuests()) {
				LOADED_QUESTS.put(saga.getId() + ":" + quest.getId(), quest);
			}

			LogUtil.info(Env.COMMON, "Loaded saga: {} ({} quests)", saga.getName(), saga.getQuests().size());
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load saga file: {}", display, e);
			JsonLoadReport.error("quests", display, "Malformed saga JSON, file skipped: " + JsonLoadReport.rootCause(e));
		}
	}

	/**
	 * Parses a saga manifest from a JSON object.
	 * <p>
	 * Supports only the 2.1 format:
	 * <ul>
	 *   <li>{@code "questFolder": "saga_saiyan"} — loads quest files from
	 *       {@code dragonminez/quests/saga_saiyan/}, sorted by filename (e.g. {@code 01_find_roshi.json}).</li>
	 * </ul>
	 *
	 * @implNote This parser is for server/world loading. Client sync should use packet-specific parsing.
	 *
	 * @since 2.1
	 */
	private static Saga parseSagaFromJson(JsonObject json, @Nullable Path worldFolder) {
		String id = json.get("id").getAsString();
		String name = json.get("name").getAsString();

		Saga.SagaRequirements requirements = null;
		if (json.has("requirements")) {
			JsonObject reqJson = json.getAsJsonObject("requirements");
			String prevSaga = reqJson.has("previousSaga") ? reqJson.get("previousSaga").getAsString() : "";
			requirements = new Saga.SagaRequirements(prevSaga);
		}

		List<Quest> quests = new ArrayList<>();
		if (!json.has("questFolder")) {
			LogUtil.warn(Env.COMMON, "Saga '{}' is missing required field 'questFolder'", id);
			return new Saga(id, name, quests, requirements);
		}

		String folderName = json.get("questFolder").getAsString();
		if (worldFolder == null) {
			LogUtil.warn(Env.COMMON, "Saga '{}' cannot resolve questFolder '{}' because world folder context is unavailable", id, folderName);
			return new Saga(id, name, quests, requirements);
		}

		Path questsBase = worldFolder.resolve(QUESTS_FOLDER).normalize();
		Path questFolder = questsBase.resolve(folderName).normalize();
		if (!questFolder.startsWith(questsBase)) {
			LogUtil.warn(Env.COMMON, "Saga '{}' has a questFolder '{}' that escapes the quests directory; skipping", id, folderName);
			return new Saga(id, name, quests, requirements);
		}
		if (Files.exists(questFolder)) {
			quests = loadQuestsFromFolder(questFolder);
		} else {
			LogUtil.warn(Env.COMMON, "Saga '{}' references questFolder '{}' but it doesn't exist", id, folderName);
		}

		return new Saga(id, name, quests, requirements);
	}

	private static List<Quest> loadQuestsFromFolder(Path folder) {
		List<Quest> quests = new ArrayList<>();
		Map<Integer, String> idSources = new HashMap<>();
		try (var stream = Files.list(folder)) {
			List<Path> files = stream
					.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".json"))
					.sorted(QUEST_FILE_ORDER)
					.toList();

			for (Path file : files) {
				String display = "quests/" + folder.getFileName() + "/" + file.getFileName();
				try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
					JsonObject root = GSON.fromJson(reader, JsonObject.class);
					QuestParser.validate("quests", display, root);
					Quest quest = parseQuestOrReport(display, root);
					if (quest == null) continue;

					if (quest.getStringId() != null) {
						reportError(display, "saga quests need a numeric 'id' (found \"" + quest.getStringId() + "\"), file skipped");
						continue;
					}
					String previous = idSources.putIfAbsent(quest.getId(), display);
					if (previous != null) {
						reportError(display, "duplicate quest id " + quest.getId() + " (already used by " + previous + "), file skipped");
						continue;
					}

					quests.add(quest);
					reportIssues(display, root, quest);
				} catch (Exception e) {
					LogUtil.error(Env.COMMON, "Failed to load quest file: {}", display, e);
					JsonLoadReport.error("quests", display, "Malformed quest JSON, file skipped: " + JsonLoadReport.rootCause(e));
				}
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to list quest files in folder: {}", folder, e);
		}
		return quests;
	}

	static int compareQuestFileNames(String a, String b) {
		int digitsA = leadingDigits(a);
		int digitsB = leadingDigits(b);
		if (digitsA > 0 && digitsB == 0) return -1;
		if (digitsA == 0 && digitsB > 0) return 1;
		if (digitsA > 0) {
			int numeric = compareDigitRuns(a.substring(0, digitsA), b.substring(0, digitsB));
			if (numeric != 0) return numeric;
		}
		int rest = a.substring(digitsA).toLowerCase(Locale.ROOT).compareTo(b.substring(digitsB).toLowerCase(Locale.ROOT));
		return rest != 0 ? rest : a.compareTo(b);
	}

	private static int leadingDigits(String value) {
		int i = 0;
		while (i < value.length() && value.charAt(i) >= '0' && value.charAt(i) <= '9') i++;
		return i;
	}

	private static int compareDigitRuns(String a, String b) {
		String x = stripLeadingZeros(a);
		String y = stripLeadingZeros(b);
		if (x.length() != y.length()) return Integer.compare(x.length(), y.length());
		return x.compareTo(y);
	}

	private static String stripLeadingZeros(String digits) {
		int i = 0;
		while (i < digits.length() - 1 && digits.charAt(i) == '0') i++;
		return digits.substring(i);
	}

	// ========================================================================================
	// Side-Quest Loading
	// ========================================================================================

	/**
	 * Loads side-quest JSON files from the given directory. Generates defaults if configured.
	 */
	private static void loadSideQuestFiles(Path sideQuestDir) {
		try {
			if (!Files.exists(sideQuestDir)) {
				Files.createDirectories(sideQuestDir);
			}
			SideQuestDefaults.createDefaultSideQuestFiles(sideQuestDir);

			List<Path> files;
			try (var stream = Files.walk(sideQuestDir)) {
				files = stream.filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".json"))
						.sorted(Comparator.comparing(path -> relativeName(sideQuestDir, path)))
						.toList();
			}
			Map<String, String> idSources = new HashMap<>();
			for (Path file : files) {
				loadSingleSideQuestFile(sideQuestDir, file, idSources);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to load side-quest files from {}", sideQuestDir, e);
		}
	}

	private static void loadSingleSideQuestFile(Path sideQuestDir, Path file, Map<String, String> idSources) {
		String display = "sidequests/" + relativeName(sideQuestDir, file);
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			QuestParser.validate("quests", display, root);
			Quest quest = parseQuestOrReport(display, root);
			if (quest == null) return;

			String effectiveId = quest.getEffectiveId();
			if (effectiveId.indexOf(':') >= 0) {
				reportError(display, "side quest id '" + effectiveId + "' must not contain ':' (reserved for saga quest keys), file skipped");
				return;
			}
			String previous = idSources.putIfAbsent(effectiveId, display);
			if (previous != null) {
				reportError(display, "duplicate side quest id '" + effectiveId + "' (already loaded from " + previous + "), file skipped");
				return;
			}

			LOADED_QUESTS.put(effectiveId, quest);
			reportIssues(display, root, quest);
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to load side-quest file: {}", display, e);
			JsonLoadReport.error("quests", display, "Malformed side-quest JSON, file skipped: " + JsonLoadReport.rootCause(e));
		}
	}

	@Nullable
	private static Quest parseQuestOrReport(String display, @Nullable JsonObject root) {
		String rejection = QuestParser.rejectionReason(root);
		if (rejection != null) {
			reportError(display, rejection + ", file skipped");
			return null;
		}
		Quest quest = QuestParser.parseQuest(root);
		if (quest == null) {
			reportError(display, "the quest could not be parsed, file skipped");
		}
		return quest;
	}

	private static void reportIssues(String display, JsonObject root, Quest quest) {
		for (String issue : QuestParser.describeIssues(root, quest)) {
			LogUtil.warn(Env.COMMON, "Quest file '{}': {}", display, issue);
			JsonLoadReport.error("quests", display, issue);
		}
	}

	private static void reportError(String display, String message) {
		LogUtil.error(Env.COMMON, "Quest file '{}': {}", display, message);
		JsonLoadReport.error("quests", display, message);
	}

	private static String relativeName(Path base, Path file) {
		return base.relativize(file).toString().replace('\\', '/');
	}


	// ========================================================================================
	// Index Building
	// ========================================================================================

	private static void buildIndexes() {
		for (Map.Entry<String, Quest> entry : LOADED_QUESTS.entrySet()) {
			String questId = entry.getKey();
			Quest quest = entry.getValue();

			// Objective type index
			for (QuestObjective objective : quest.getObjectives()) {
				if (objective.isPlaceholder()) continue;
				OBJECTIVE_INDEX.computeIfAbsent(objective.getType(), k -> new ArrayList<>()).add(questId);
			}

			// Quest giver index
			if (quest.getQuestGiver() != null && !quest.getQuestGiver().isEmpty()) {
				QUEST_GIVER_INDEX.computeIfAbsent(quest.getQuestGiver(), k -> new ArrayList<>()).add(questId);
			}

			// Turn-in index
			if (quest.getTurnIn() != null && !quest.getTurnIn().isEmpty()) {
				TURN_IN_INDEX.computeIfAbsent(quest.getTurnIn(), k -> new ArrayList<>()).add(questId);
			}
		}
	}

	// ========================================================================================
	// Server-Side Lookups
	// ========================================================================================

	/** Returns a quest by its string ID, or {@code null} if not found. */
	@Nullable
	public static Quest getQuest(String questId) {
		return LOADED_QUESTS.get(questId);
	}

	/** Returns an unmodifiable view of all loaded quests. */
	public static Map<String, Quest> getAllQuests() {
		return Collections.unmodifiableMap(LOADED_QUESTS);
	}

	/** Returns a saga by its saga ID, or {@code null} if not found. */
	@Nullable
	public static Saga getSaga(String sagaId) {
		return LOADED_SAGAS.get(sagaId);
	}

	/** Returns an unmodifiable view of all loaded sagas. */
	public static Map<String, Saga> getAllSagas() {
		return Collections.unmodifiableMap(LOADED_SAGAS);
	}

	/** Returns all quest IDs that contain the given objective type. */
	public static List<String> getQuestIdsByObjectiveType(QuestObjective.ObjectiveType type) {
		return OBJECTIVE_INDEX.getOrDefault(type, Collections.emptyList());
	}

	/** Returns all quest IDs where the given NPC is the quest giver. */
	public static List<String> getQuestIdsByGiver(String npcId) {
		return QUEST_GIVER_INDEX.getOrDefault(npcId, Collections.emptyList());
	}

	/** Returns all quest IDs where the given NPC is the turn-in target. */
	public static List<String> getQuestIdsByTurnIn(String npcId) {
		return TURN_IN_INDEX.getOrDefault(npcId, Collections.emptyList());
	}

	/** Returns all quests of a given type. */
	public static List<Quest> getQuestsByType(Quest.QuestType type) {
		List<Quest> result = new ArrayList<>();
		for (Quest quest : LOADED_QUESTS.values()) {
			if (quest.getType() == type) result.add(quest);
		}
		return result;
	}

	/** Returns all quests belonging to a given category. */
	public static List<Quest> getQuestsByCategory(String category) {
		List<Quest> result = new ArrayList<>();
		for (Quest quest : LOADED_QUESTS.values()) {
			if (category.equalsIgnoreCase(quest.getCategory())) result.add(quest);
		}
		return result;
	}

	// ========================================================================================
	// Client-Side Lookups
	// ========================================================================================

	@Nullable
	public static Quest getClientQuest(String questId) {
		return CLIENT_QUESTS.get(questId);
	}

	public static Map<String, Quest> getClientQuests() {
		return Collections.unmodifiableMap(CLIENT_QUESTS);
	}

	@Nullable
	public static Saga getClientSaga(String sagaId) {
		return CLIENT_SAGAS.get(sagaId);
	}

	public static Map<String, Saga> getClientSagas() {
		return Collections.unmodifiableMap(CLIENT_SAGAS);
	}

	// ========================================================================================
	// Client Sync
	// ========================================================================================

	public static void applySyncedQuests(Map<String, Quest> quests) {
		CLIENT_QUESTS.clear();
		CLIENT_QUESTS.putAll(quests);
		LogUtil.info(Env.CLIENT, "QuestRegistry: synced {} quest(s) from server", quests.size());
	}

	public static void applySyncedSagas(Map<String, Saga> sagas) {
		CLIENT_SAGAS.clear();
		CLIENT_SAGAS.putAll(sagas);
		LogUtil.info(Env.CLIENT, "QuestRegistry: synced {} saga(s) from server", sagas.size());
	}
}
