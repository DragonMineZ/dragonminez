package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.animation.AnimationCache;
import com.dragonminez.common.config.migration.RacialConfigMigrator;
import com.dragonminez.common.diagnostics.JsonLoadReport;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.util.FusionForms;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import com.dragonminez.common.network.CompressionUtil;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SyncServerConfigS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ConfigManager {
	public static final String CONFIG_VERSION = "2.2.0";
	public static final String CLIENT_ONLY_CONFIG = "general-user";

	private static final String PREVIOUS_CONFIGS_ROOT = "/data/dragonminez/previousConfigs/";
	private static final String OLD_BACKUP_DIR = "oldBackup";

	private static final double DEFENSE_SCALING_FOLD = 0.12;
	private static final double DEFENSE_SCALING_FOLD_VERSION = 21.2;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient()
			.registerTypeAdapter(RaceCharacterConfig.FormSkillCost.class, new RaceCharacterConfig.FormSkillCost.Adapter())
			.create();
	private static final ConfigLoader LOADER = new ConfigLoader(GSON);
	private static final DefaultFormsFactory FORMS_FACTORY = new DefaultFormsFactory();

	private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
	private static final Path STACK_FORMS_DIR = CONFIG_DIR.resolve("forms");
	private static final Path RACES_DIR = CONFIG_DIR.resolve("races");
	private static final Path RAIDS_DIR = CONFIG_DIR.resolve("raids");
	private static final Path TOURNAMENTS_DIR = CONFIG_DIR.resolve("tournaments");
	private static final String[] DEFAULT_RACES = {"human", "saiyan", "namekian", "frostdemon", "bioandroid", "majin", "glind"};
	private static final Set<String> RACES_WITH_GENDER = new HashSet<>(Arrays.asList("human", "saiyan", "majin", "glind"));

	private static final Map<String, RaceStatsConfig> RACE_STATS = new HashMap<>();
	private static final Map<String, RaceCharacterConfig> RACE_CHARACTER = new HashMap<>();
	private static final Map<String, Map<String, FormConfig>> RACE_FORMS = new HashMap<>();
	private static final List<String> LOADED_RACES = new ArrayList<>();
	private static final List<String> CACHED_CONFIG_FILES = new ArrayList<>();

	private static Map<String, FormConfig> STACK_FORMS = new HashMap<>();
	private static final Map<String, RaidDefinition> RAIDS = new LinkedHashMap<>();
	private static final Map<String, TournamentDefinition> TOURNAMENTS = new LinkedHashMap<>();
	public static final String SYNC_END_MARKER = "__end__";
	private static final String SYNC_EXCLUDED_SERVER_SECTION = "storage";

	private static final class SyncedConfigs {
		private GeneralServerConfig generalServer;
		private SkillsConfig skills;
		private TechniqueConfig techniques;
		private CombatConfig combat;
		private TrainingConfig training;
		private Map<String, Map<String, FormConfig>> forms = new HashMap<>();
		private Map<String, RaceStatsConfig> stats = new HashMap<>();
		private Map<String, RaceCharacterConfig> character = new HashMap<>();
		private Map<String, FormConfig> stackForms = new HashMap<>();
		private EntitiesConfig entities;
	}

	public record SyncPayload(String path, byte[] data) {}

	private static volatile SyncedConfigs SYNCED = new SyncedConfigs();
	private static SyncedConfigs pendingSync;
	private static volatile Thread syncClientThread;
	private static volatile List<SyncPayload> cachedSyncPayloads;
	private static volatile boolean serverSyncActive = false;

	private static GeneralUserConfig userConfig;
	private static HudLayoutConfig hudLayoutConfig;
	private static GeneralServerConfig serverConfig;
	private static CombatConfig combatConfig;
	private static TrainingConfig trainingConfig;
	private static SkillsConfig skillsConfig;
	private static TechniqueConfig techniqueConfig;
	private static EntitiesConfig entitiesConfig;

	public static void initialize() {
		LogUtil.info(Env.COMMON, "Initializing DragonMineZ configuration system...");
		invalidateSyncPayloads();
		JsonLoadReport.clear("config");

		try {
			Files.createDirectories(CONFIG_DIR);
			Files.createDirectories(RACES_DIR);

			loadGeneralConfigs();
			loadAllRaces();
			createOrLoadStackForms(true);
			loadRaids();
			loadTournaments();

			LogUtil.info(Env.COMMON, "Configuration system initialized successfully");
			LogUtil.info(Env.COMMON, "Loaded races: {}", LOADED_RACES);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Error initializing configuration system: {}", e.getMessage());
		}
	}

	public static void reload() {
		LogUtil.info(Env.COMMON, "Reloading DragonMineZ configuration system...");
		invalidateSyncPayloads();
		JsonLoadReport.clear("config");

		try {
			RACE_STATS.clear();
			RACE_CHARACTER.clear();
			RACE_FORMS.clear();
			LOADED_RACES.clear();
			STACK_FORMS.clear();
			CACHED_CONFIG_FILES.clear();
			AnimationCache.clear();

			loadGeneralConfigs();
			loadAllRaces();
			createOrLoadStackForms(true);
			loadRaids();
			loadTournaments();
			LogUtil.info(Env.COMMON, "Configuration system reloaded successfully");
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Error reloading configuration system: {}", e.getMessage());
		}
	}

	private static void loadRaids() throws IOException {
		RAIDS.clear();
		Files.createDirectories(RAIDS_DIR);

		Map<String, RaidDefinition> shipped = RaidDefaults.create();
		for (Map.Entry<String, RaidDefinition> entry : shipped.entrySet()) {
			Path path = RAIDS_DIR.resolve(entry.getKey() + ".json");
			RaidDefinition loaded = loadAndValidate(path, RaidDefinition.class, entry::getValue,
					RaidDefinition::getConfigVersion, RaidDefinition::setConfigVersion,
					RaidDefinition.CURRENT_VERSION, null);
			if (loaded != null) RAIDS.put(entry.getKey(), loaded);
		}

		try (Stream<Path> stream = Files.list(RAIDS_DIR)) {
			stream.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".json"))
					.filter(p -> !p.getFileName().toString().toLowerCase().startsWith("old_"))
					.forEach(path -> {
						String id = path.getFileName().toString();
						id = id.substring(0, id.length() - 5);
						if (shipped.containsKey(id)) return;

						try {
							RaidDefinition custom = LOADER.loadConfig(path, RaidDefinition.class);
							if (custom != null) RAIDS.put(id, custom);
						} catch (Exception e) {
							LogUtil.error(Env.COMMON, "Could not read custom raid '{}': {}", id, e.getMessage());
							JsonLoadReport.error("config", "raids/" + id + ".json",
									"Malformed JSON: " + JsonLoadReport.rootCause(e) + " — this raid was skipped");
						}
					});
		}

		LogUtil.info(Env.COMMON, "Loaded {} raid definition(s): {}", RAIDS.size(), RAIDS.keySet());
	}

	/** Raid id to definition, in load order. Feed this to {@code RaidTypes.reload(...)}. */
	public static Map<String, RaidDefinition> getRaids() {
		return Collections.unmodifiableMap(RAIDS);
	}

	/** Mirrors {@link #loadRaids()}: shipped defaults get the version merge, extra files load as-is. */
	private static void loadTournaments() throws IOException {
		TOURNAMENTS.clear();
		Files.createDirectories(TOURNAMENTS_DIR);

		Map<String, TournamentDefinition> shipped = TournamentDefinition.Defaults.create();
		for (Map.Entry<String, TournamentDefinition> entry : shipped.entrySet()) {
			Path path = TOURNAMENTS_DIR.resolve(entry.getKey() + ".json");
			TournamentDefinition loaded = loadAndValidate(path, TournamentDefinition.class, entry::getValue,
					TournamentDefinition::getConfigVersion, TournamentDefinition::setConfigVersion,
					TournamentDefinition.CURRENT_VERSION, null);
			if (loaded != null) TOURNAMENTS.put(entry.getKey(), loaded);
		}

		try (Stream<Path> stream = Files.list(TOURNAMENTS_DIR)) {
			stream.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".json"))
					.filter(p -> !p.getFileName().toString().toLowerCase().startsWith("old_"))
					.forEach(path -> {
						String id = path.getFileName().toString();
						id = id.substring(0, id.length() - 5);
						if (shipped.containsKey(id)) return;

						try {
							TournamentDefinition custom = LOADER.loadConfig(path, TournamentDefinition.class);
							if (custom != null) TOURNAMENTS.put(id, custom);
						} catch (Exception e) {
							LogUtil.error(Env.COMMON, "Could not read custom tournament '{}': {}", id, e.getMessage());
							JsonLoadReport.error("config", "tournaments/" + id + ".json",
									"Malformed JSON: " + JsonLoadReport.rootCause(e) + " — this tournament was skipped");
						}
					});
		}

		LogUtil.info(Env.COMMON, "Loaded {} tournament definition(s): {}", TOURNAMENTS.size(), TOURNAMENTS.keySet());
	}

	public static Map<String, TournamentDefinition> getTournaments() {
		return Collections.unmodifiableMap(TOURNAMENTS);
	}

	/**
	 * Records the ring position for a tournament and writes it back to its JSON, so the offset
	 * only has to be measured once per structure rather than guessed.
	 *
	 * @param x offset in blocks from the structure's corner
	 * @return false when the tournament is unknown or the file could not be written
	 */
	public static boolean saveTournamentRing(String id, int x, int y, int z) {
		TournamentDefinition def = TOURNAMENTS.get(id);
		if (def == null) return false;

		TournamentDefinition.RingOffset ring = def.getRing();
		if (ring == null) {
			ring = new TournamentDefinition.RingOffset();
			def.setRing(ring);
		}
		ring.setX(x);
		ring.setY(y);
		ring.setZ(z);

		try {
			LOADER.saveConfig(TOURNAMENTS_DIR.resolve(id + ".json"), def);
			return true;
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Could not save ring position for tournament '{}': {}", id, e.getMessage());
			return false;
		}
	}

	/** Null when the id is unknown, disabled, or the file is missing fighters. */
	public static TournamentDefinition getTournament(String id) {
		if (id == null) return null;
		TournamentDefinition def = TOURNAMENTS.get(id);
		if (def == null || !def.isEnabled() || !def.isUsable()) return null;
		return def;
	}

	private static String peekConfigVersion(Path path) {
		if (!Files.exists(path)) return null;
		try {
			JsonElement parsed = JsonParser.parseString(Files.readString(path));
			if (parsed == null || !parsed.isJsonObject()) return "";
			JsonObject obj = parsed.getAsJsonObject();
			if (obj.has("configVersion") && obj.get("configVersion").isJsonPrimitive()) {
				return obj.get("configVersion").getAsString();
			}
			return "";
		} catch (Exception e) {
			return "";
		}
	}

	private static Integer[] parseSemver(String version) {
		if (version == null || version.isBlank()) return null;
		String v = version.trim();
		int suffixRank = 0;
		int end = v.length();
		while (end > 0 && Character.isLetter(v.charAt(end - 1))) end--;
		if (end < v.length()) {
			suffixRank = Character.toLowerCase(v.charAt(end)) - 'a' + 1;
			v = v.substring(0, end);
		}
		String[] parts = v.split("\\.");
		if (parts.length != 3) return null;
		Integer[] comps = new Integer[4];
		for (int i = 0; i < 3; i++) {
			try { comps[i] = Integer.parseInt(parts[i].trim()); }
			catch (NumberFormatException e) { return null; }
		}
		comps[3] = suffixRank;
		return comps;
	}

	private static int compareSemver(Integer[] a, Integer[] b) {
		int n = Math.min(a.length, b.length);
		for (int i = 0; i < n; i++) {
			int cmp = Integer.compare(a[i], b[i]);
			if (cmp != 0) return cmp;
		}
		return 0;
	}

	static boolean isOutdated(String storedVersion) {
		Integer[] stored = parseSemver(storedVersion);
		if (stored == null) return true;
		Integer[] current = parseSemver(CONFIG_VERSION);
		return compareSemver(stored, current) < 0;
	}

	private static boolean isLegacyPreFoldVersion(String storedVersion) {
		if (storedVersion == null || storedVersion.isBlank()) return false;
		if (parseSemver(storedVersion) != null) return false;
		try {
			double legacy = Double.parseDouble(storedVersion.trim());
			return legacy >= 0.0 && legacy < DEFENSE_SCALING_FOLD_VERSION;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static Double defaultDefenseScaling(RaceStatsConfig config, String className) {
		RaceStatsConfig.ClassStats classStats = config.getClasses().get(className);
		if (classStats == null) return null;
		RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
		return scaling != null ? scaling.getDefenseScaling() : null;
	}

	private static String relativeName(Path path) {
		try { return CONFIG_DIR.relativize(path).toString().replace('\\', '/'); }
		catch (Exception e) { return path.getFileName().toString(); }
	}

	private static void backupOldConfig(Path configPath) {
		if (Files.exists(configPath)) {
			try {
				Path relative = CONFIG_DIR.relativize(configPath);
				Path backupPath = CONFIG_DIR.resolve(OLD_BACKUP_DIR).resolve(relative);
				Files.createDirectories(backupPath.getParent());
				Files.move(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
				LogUtil.info(Env.COMMON, "Obsolete config backed up: {}", CONFIG_DIR.relativize(backupPath));
			} catch (Exception e) {
				LogUtil.error(Env.COMMON, "Failed to backup old config '{}': {}", configPath.getFileName(), e);
			}
		}
	}

	private static JsonObject loadBaselineObject(Path configPath) {
		Path relative;
		try { relative = CONFIG_DIR.relativize(configPath); }
		catch (Exception e) { return null; }
		String resource = PREVIOUS_CONFIGS_ROOT + relative.toString().replace('\\', '/');
		try (InputStream in = ConfigManager.class.getResourceAsStream(resource)) {
			if (in == null) return null;
			JsonElement parsed = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static <T> T loadAndValidate(Path path, Class<T> clazz, Supplier<T> defaultProvider, Function<T, String> versionGetter, BiConsumer<T, String> versionSetter, String currentVersion, String templateName) {
		boolean overwrite = false;
		String reason = "";
		T config = null;

		if (Files.exists(path)) {
			try {
				config = LOADER.loadConfig(path, clazz);
				String version = versionGetter.apply(config);
				if (isOutdated(version)) {
					reason = (version == null || version.isBlank()) ? "Missing config version" : "Outdated version (" + version + " < " + currentVersion + ")";
					overwrite = true;
				}
			} catch (Exception e) {
				reason = "Parsing error: " + e.getMessage();
				overwrite = true;
				JsonLoadReport.error("config", relativeName(path),
						"Malformed JSON: " + JsonLoadReport.rootCause(e) + " — running on defaults for now; your file was left untouched, fix the syntax and /dmzreload to apply your edits");
			}
		} else {
			reason = "File not found";
			overwrite = true;
			if (templateName != null) {
				try {
					LOADER.saveDefaultFromTemplate(path, templateName);
					config = LOADER.loadConfig(path, clazz);
					if (isOutdated(versionGetter.apply(config))) overwrite = true;
				} catch (Exception e) {
					reason = "Template loading failed: " + e.getMessage();
				}
			}
		}

		if (overwrite) {
			boolean parsingError = reason.startsWith("Parsing error");
			if (parsingError) {
				LogUtil.warn(Env.COMMON, String.format("%s has malformed JSON; using defaults this session and leaving your file untouched. Reason: %s", path.getFileName(), reason));
				return defaultProvider.get();
			}

			String oldRawJson = null;
			JsonObject baseline = loadBaselineObject(path);
			if (Files.exists(path)) {
				try { oldRawJson = Files.readString(path); }
				catch (IOException e) { LogUtil.error(Env.COMMON, "Could not read old config '{}' for value preservation: {}", path.getFileName(), e.getMessage()); }
				backupOldConfig(path);
			}
			config = defaultProvider.get();

			try {
				versionSetter.accept(config, currentVersion);
				if (oldRawJson != null) config = mergePreservedValues(oldRawJson, config, clazz, currentVersion, versionSetter, baseline);
				LogUtil.warn(Env.COMMON, String.format("Regenerating %s. Reason: %s", path.getFileName(), reason));
				LOADER.saveConfig(path, config);
			} catch (Exception e) {
				LogUtil.error(Env.COMMON, "Error saving regenerated config: " + e.getMessage());
			}
		}
		return config != null ? config : defaultProvider.get();
	}

	private static <T> T mergePreservedValues(String oldRawJson, T defaultConfig, Class<T> clazz, String currentVersion, BiConsumer<T, String> versionSetter, JsonObject baseline) {
		try {
			JsonElement oldParsed = JsonParser.parseString(oldRawJson);
			if (oldParsed == null || !oldParsed.isJsonObject()) return defaultConfig;
			JsonElement newTree = GSON.toJsonTree(defaultConfig);
			if (newTree == null || !newTree.isJsonObject()) return defaultConfig;

			JsonObject oldObj = oldParsed.getAsJsonObject();
			JsonObject newObj = newTree.getAsJsonObject();
			if (clazz == GeneralServerConfig.class) RacialConfigMigrator.migrate(oldObj);
			int preserved = mergeMatchingValues(oldObj, newObj, baseline, clazz);

			T merged = GSON.fromJson(newObj, clazz);
			if (merged == null) return defaultConfig;
			versionSetter.accept(merged, currentVersion);
			if (preserved > 0) LogUtil.info(Env.COMMON, "Preserved {} user-modified value(s) from the old config", preserved);
			return merged;
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Could not preserve old config values, falling back to defaults: {}", e.getMessage());
			return defaultConfig;
		}
	}

	private static JsonElement baselineChild(JsonObject baseline, String key) {
		return (baseline != null && baseline.has(key) && !baseline.get(key).isJsonNull()) ? baseline.get(key) : null;
	}

	private static boolean shouldPreserve(JsonElement oldVal, JsonElement newVal, JsonElement baseVal) {
		if (baseVal != null) return !valuesEqual(oldVal, baseVal);
		return !valuesEqual(oldVal, newVal);
	}

	private static int mergeMatchingValues(JsonObject oldObj, JsonObject newObj, JsonObject baseline, Class<?> type) {
		int count = 0;
		for (String key : new ArrayList<>(newObj.keySet())) {
			if (key.equals("configVersion") || !oldObj.has(key)) continue;
			JsonElement oldVal = oldObj.get(key);
			JsonElement newVal = newObj.get(key);
			if (oldVal.isJsonNull()) continue;
			JsonElement baseVal = baselineChild(baseline, key);

			Field field = findField(type, key);
			Class<?> fieldType = field != null ? field.getType() : null;

			if (field != null && field.isAnnotationPresent(ConfigNonPreservable.class)) continue;

			if (fieldType != null && isMapType(fieldType) && oldVal.isJsonObject() && newVal.isJsonObject()) {
				JsonObject baseMap = (baseVal != null && baseVal.isJsonObject()) ? baseVal.getAsJsonObject() : null;
				count += mergeMapValues(oldVal.getAsJsonObject(), newVal.getAsJsonObject(), baseMap, mapValueClass(field));
			} else if (fieldType != null && isCollectionOrArray(fieldType)) {
				if (oldVal.isJsonArray() && shouldPreserve(oldVal, newVal, baseVal)) { newObj.add(key, stripNullElements(oldVal.getAsJsonArray())); count++; }
			} else if (oldVal.isJsonObject() && newVal.isJsonObject()) {
				JsonObject baseObj = (baseVal != null && baseVal.isJsonObject()) ? baseVal.getAsJsonObject() : null;
				count += mergeMatchingValues(oldVal.getAsJsonObject(), newVal.getAsJsonObject(), baseObj, fieldType);
			} else if (isValueCompatible(oldVal, newVal, fieldType) && shouldPreserve(oldVal, newVal, baseVal)) {
				newObj.add(key, oldVal);
				count++;
			}
		}
		return count;
	}

	private static int mergeMapValues(JsonObject oldMap, JsonObject newMap, JsonObject baseMap, Class<?> valueType) {
		int count = 0;
		for (String key : new ArrayList<>(newMap.keySet())) {
			if (!oldMap.has(key)) {
				if (baseMap != null && baseMap.has(key)) {
					newMap.remove(key);
					count++;
				}
				continue;
			}
			JsonElement oldVal = oldMap.get(key);
			JsonElement newVal = newMap.get(key);
			if (oldVal.isJsonNull()) continue;
			JsonElement baseVal = baselineChild(baseMap, key);

			if (oldVal.isJsonObject() && newVal.isJsonObject()) {
				if (valueType != null && !isMapType(valueType) && !isCollectionOrArray(valueType)) {
					JsonObject baseObj = (baseVal != null && baseVal.isJsonObject()) ? baseVal.getAsJsonObject() : null;
					count += mergeMatchingValues(oldVal.getAsJsonObject(), newVal.getAsJsonObject(), baseObj, valueType);
				} else if (shouldPreserve(oldVal, newVal, baseVal)) {
					newMap.add(key, oldVal);
					count++;
				}
			} else if (oldVal.isJsonArray()) {
				if (shouldPreserve(oldVal, newVal, baseVal)) { newMap.add(key, stripNullElements(oldVal.getAsJsonArray())); count++; }
			} else if (isValueCompatible(oldVal, newVal, valueType) && shouldPreserve(oldVal, newVal, baseVal)) {
				newMap.add(key, oldVal);
				count++;
			}
		}
		for (String key : oldMap.keySet()) {
			if (!newMap.has(key)) {
				newMap.add(key, oldMap.get(key));
				count++;
			}
		}
		return count;
	}

	private static boolean isValueCompatible(JsonElement oldVal, JsonElement newVal, Class<?> type) {
		if (!oldVal.isJsonPrimitive()) return false;
		JsonPrimitive oldPrim = oldVal.getAsJsonPrimitive();

		if (type == null) {
			if (newVal == null || !newVal.isJsonPrimitive()) return false;
			JsonPrimitive newPrim = newVal.getAsJsonPrimitive();
			if (oldPrim.isBoolean() || newPrim.isBoolean()) return oldPrim.isBoolean() && newPrim.isBoolean();
			if (oldPrim.isString() || newPrim.isString()) return oldPrim.isString() && newPrim.isString();
			if (oldPrim.isNumber() && newPrim.isNumber()) return isIntegralLiteral(oldPrim) == isIntegralLiteral(newPrim);
			return false;
		}

		if (type == boolean.class || type == Boolean.class) return oldPrim.isBoolean();
		if (type == String.class || type == char.class || type == Character.class || type.isEnum()) return oldPrim.isString();
		if (isIntegralType(type)) return oldPrim.isNumber() && isIntegralLiteral(oldPrim);
		if (isDecimalType(type)) return oldPrim.isNumber();
		return false;
	}

	private static boolean valuesEqual(JsonElement oldVal, JsonElement newVal) {
		if (newVal == null) return false;
		if (oldVal.isJsonPrimitive() && newVal.isJsonPrimitive()) {
			JsonPrimitive op = oldVal.getAsJsonPrimitive();
			JsonPrimitive np = newVal.getAsJsonPrimitive();
			if (op.isNumber() && np.isNumber()) {
				try { return op.getAsBigDecimal().compareTo(np.getAsBigDecimal()) == 0; }
				catch (NumberFormatException e) { return op.getAsString().equals(np.getAsString()); }
			}
		}
		return oldVal.equals(newVal);
	}

	private static boolean isIntegralLiteral(JsonPrimitive prim) {
		if (!prim.isNumber()) return false;
		String s = prim.getAsString();
		return s.indexOf('.') < 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0;
	}

	private static boolean isIntegralType(Class<?> type) {
		return type == int.class || type == Integer.class || type == long.class || type == Long.class
				|| type == short.class || type == Short.class || type == byte.class || type == Byte.class
				|| java.math.BigInteger.class.isAssignableFrom(type);
	}

	private static boolean isDecimalType(Class<?> type) {
		return type == double.class || type == Double.class || type == float.class || type == Float.class
				|| java.math.BigDecimal.class.isAssignableFrom(type);
	}

	private static boolean isMapType(Class<?> type) {
		return Map.class.isAssignableFrom(type);
	}

	private static boolean isCollectionOrArray(Class<?> type) {
		return type.isArray() || Collection.class.isAssignableFrom(type);
	}

	private static JsonArray stripNullElements(JsonArray array) {
		JsonArray cleaned = new JsonArray();
		for (JsonElement element : array) {
			if (!element.isJsonNull()) cleaned.add(element);
		}
		return cleaned;
	}

	private static Field findField(Class<?> type, String name) {
		if (type == null) return null;
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
				if (f.getName().equals(name)) return f;
			}
		}
		return null;
	}

	private static Class<?> mapValueClass(Field field) {
		try {
			java.lang.reflect.Type generic = field.getGenericType();
			if (generic instanceof java.lang.reflect.ParameterizedType pt) {
				java.lang.reflect.Type[] args = pt.getActualTypeArguments();
				if (args.length == 2 && args[1] instanceof Class<?> c) return c;
			}
		} catch (Exception ignored) {}
		return null;
	}

	private static FormConfig regenerateOutdatedForm(Path formFilePath, FormConfig defaultFormConfig, String label) {
		LogUtil.warn(Env.COMMON, "Regenerating form '{}'. Reason: Outdated version", label);
		String oldRaw = null;
		if (Files.exists(formFilePath)) {
			try { oldRaw = Files.readString(formFilePath); }
			catch (IOException e) { LogUtil.error(Env.COMMON, "Could not read old form '{}' for value preservation: {}", formFilePath.getFileName(), e.getMessage()); }
		}
		JsonObject baseline = loadBaselineObject(formFilePath);
		backupOldConfig(formFilePath);
		defaultFormConfig.setConfigVersion(FormConfig.CURRENT_VERSION);
		FormConfig result = oldRaw != null
				? mergePreservedValues(oldRaw, defaultFormConfig, FormConfig.class, FormConfig.CURRENT_VERSION, FormConfig::setConfigVersion, baseline)
				: defaultFormConfig;
		try { LOADER.saveConfig(formFilePath, result); } catch (Exception e) {
			LogUtil.error(Env.COMMON, "Failed to save regenerated form '{}': {}", formFilePath.getFileName(), e.getMessage());
		}
		return result;
	}

	private static void upgradeUserFormFiles(Path formsDir, Map<String, FormConfig> defaultForms) {
		if (!Files.exists(formsDir)) return;
		Set<String> defaults = new HashSet<>();
		for (FormConfig form : defaultForms.values()) if (form != null) defaults.add(form.getGroupName().toLowerCase());
		try (Stream<Path> stream = Files.list(formsDir)) {
			stream.filter(p -> p.toString().endsWith(".json"))
					.filter(p -> !p.getFileName().toString().toLowerCase().startsWith("old_"))
					.forEach(p -> {
						try {
							FormConfig existing = LOADER.loadConfig(p, FormConfig.class);
							if (existing == null || defaults.contains(existing.getGroupName().toLowerCase())) return;
							if (isOutdated(existing.getConfigVersion())) {
								LogUtil.warn(Env.COMMON, "Regenerating user form '{}'. Reason: Outdated version", p.getFileName());
								backupOldConfig(p);
								existing.setConfigVersion(FormConfig.CURRENT_VERSION);
								LOADER.saveConfig(p, existing);
							}
						} catch (Exception e) {
							LogUtil.error(Env.COMMON, "Failed to upgrade user form '{}': {}", p.getFileName(), e.getMessage());
							JsonLoadReport.error("config", relativeName(p), "Form file failed to upgrade: " + JsonLoadReport.rootCause(e));
						}
					});
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to scan user forms in '{}': {}", formsDir, e.getMessage());
		}
	}

	private static void backupRenamedDefaultForms(Path formsDir, Map<String, FormConfig> defaultForms) {
		if (!Files.exists(formsDir) || defaultForms.isEmpty()) return;
		Set<String> defaults = new HashSet<>();
		for (FormConfig form : defaultForms.values()) {
			if (form != null && form.getGroupName() != null) defaults.add(form.getGroupName().toLowerCase());
		}
		try (Stream<Path> stream = Files.list(formsDir)) {
			stream.filter(p -> p.toString().endsWith(".json"))
					.filter(p -> !p.getFileName().toString().toLowerCase().startsWith("old_"))
					.forEach(p -> {
						try {
							FormConfig existing = LOADER.loadConfig(p, FormConfig.class);
							if (existing == null || existing.getGroupName() == null) return;
							String group = existing.getGroupName().toLowerCase();
							if (defaults.contains(group) || !defaults.contains(group + "s")) return;
							if (!isOutdated(existing.getConfigVersion())) return;
							LogUtil.warn(Env.COMMON, "Backing up obsolete renamed form file '{}' (group '{}' renamed to '{}s')", p.getFileName(), group, group);
							backupOldConfig(p);
						} catch (Exception e) {
							LogUtil.error(Env.COMMON, "Failed to inspect form file '{}' for legacy rename: {}", p.getFileName(), e.getMessage());
						}
					});
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Failed to scan forms for legacy renames in '{}': {}", formsDir, e.getMessage());
		}
	}

	private static void loadGeneralConfigs() {
		userConfig = loadAndValidate(CONFIG_DIR.resolve("general-user.json"), GeneralUserConfig.class, GeneralUserConfig::new, GeneralUserConfig::getConfigVersion, GeneralUserConfig::setConfigVersion, GeneralUserConfig.CURRENT_VERSION, null);
		loadHudLayoutConfig();
		serverConfig = loadAndValidate(CONFIG_DIR.resolve("general-server.json"), GeneralServerConfig.class, GeneralServerConfig::new, GeneralServerConfig::getConfigVersion, GeneralServerConfig::setConfigVersion, GeneralServerConfig.CURRENT_VERSION, "general-server.json");
		combatConfig = loadAndValidate(CONFIG_DIR.resolve("combat.json"), CombatConfig.class, CombatConfig::new, CombatConfig::getConfigVersion, CombatConfig::setConfigVersion, CombatConfig.CURRENT_VERSION, null);
		trainingConfig = loadAndValidate(CONFIG_DIR.resolve("training.json"), TrainingConfig.class, TrainingConfig::new, TrainingConfig::getConfigVersion, TrainingConfig::setConfigVersion, TrainingConfig.CURRENT_VERSION, null);
		skillsConfig = loadAndValidate(CONFIG_DIR.resolve("skills.json"), SkillsConfig.class, SkillsConfig::new, SkillsConfig::getConfigVersion, SkillsConfig::setConfigVersion, SkillsConfig.CURRENT_VERSION, "skills.json");
		techniqueConfig = loadAndValidate(CONFIG_DIR.resolve("techniques.json"), TechniqueConfig.class, TechniqueConfig::new, TechniqueConfig::getConfigVersion, TechniqueConfig::setConfigVersion, TechniqueConfig.CURRENT_VERSION, null);
		entitiesConfig = loadAndValidate(CONFIG_DIR.resolve("entities.json"), EntitiesConfig.class, ConfigManager::createDefaultEntitiesConfig, EntitiesConfig::getConfigVersion, EntitiesConfig::setConfigVersion, EntitiesConfig.CURRENT_VERSION, null);
	}

	private static void createOrLoadRace(String raceName, boolean isDefault) throws IOException {
		Path racePath = RACES_DIR.resolve(raceName);
		Files.createDirectories(racePath);
		Path formsPath = racePath.resolve("forms");
		Files.createDirectories(formsPath);

		RaceCharacterConfig characterConfig = loadAndValidate(racePath.resolve("character.json"), RaceCharacterConfig.class, () -> createDefaultCharacterConfig(raceName, isDefault), RaceCharacterConfig::getConfigVersion, RaceCharacterConfig::setConfigVersion, RaceCharacterConfig.CURRENT_VERSION, null);

		if (skillsConfig != null && characterConfig.normalizeFormSkillKeys(skillsConfig.getFormSkills())) {
			LogUtil.warn(Env.COMMON, "Normalized legacy form-skill keys in character.json for race '{}'", raceName);
			try { LOADER.saveConfig(racePath.resolve("character.json"), characterConfig); } catch (Exception e) {
				LogUtil.error(Env.COMMON, "Failed to save normalized character.json for race '{}': {}", raceName, e.getMessage());
			}
		}
		Path statsPath = racePath.resolve("stats.json");
		String previousStatsVersion = peekConfigVersion(statsPath);
		RaceStatsConfig statsConfig = loadAndValidate(statsPath, RaceStatsConfig.class, ConfigManager::createDefaultStatsConfig, RaceStatsConfig::getConfigVersion, RaceStatsConfig::setConfigVersion, RaceStatsConfig.CURRENT_VERSION, null);
		if (isLegacyPreFoldVersion(previousStatsVersion)) {
			RaceStatsConfig newDefaults = createDefaultStatsConfig();
			boolean folded = false;
			for (Map.Entry<String, RaceStatsConfig.ClassStats> entry : statsConfig.getClasses().entrySet()) {
				RaceStatsConfig.StatScaling scaling = entry.getValue().getStatScaling();
				if (scaling == null || scaling.getDefenseScaling() == null) continue;
				Double newDefault = defaultDefenseScaling(newDefaults, entry.getKey());
				if (newDefault != null && newDefault.equals(scaling.getDefenseScaling())) continue;
				scaling.setDefenseScaling(scaling.getDefenseScaling() * DEFENSE_SCALING_FOLD);
				folded = true;
			}
			if (folded) {
				try { LOADER.saveConfig(statsPath, statsConfig); } catch (Exception e) {
					LogUtil.error(Env.COMMON, "Failed to save migrated stats.json for race '{}': {}", raceName, e.getMessage());
				}
				LogUtil.warn(Env.COMMON, "Migrated user-modified DEF_scaling to flat-defense units for race '{}'", raceName);
			}
		}

		Map<String, FormConfig> raceForms = new HashMap<>();
		if (isDefault) FORMS_FACTORY.createDefaultFormsForRace(raceName, formsPath, raceForms);
		if (isDefault) backupRenamedDefaultForms(formsPath, raceForms);
		upgradeUserFormFiles(formsPath, raceForms);
		Map<String, FormConfig> userDiskForms = LOADER.loadRaceForms(raceName, formsPath);

		if (!isDefault) {
			raceForms.putAll(userDiskForms);
		} else {
			for (Map.Entry<String, FormConfig> defaultEntry : raceForms.entrySet()) {
				String groupKey = defaultEntry.getKey().toLowerCase();
				FormConfig defaultFormConfig = defaultEntry.getValue();
				Path formFilePath = formsPath.resolve(defaultEntry.getKey() + ".json");

				if (userDiskForms.containsKey(groupKey)) {
					FormConfig userConfig = userDiskForms.get(groupKey);
					if (isOutdated(userConfig.getConfigVersion())) {
						FormConfig regenerated = regenerateOutdatedForm(formFilePath, defaultFormConfig, defaultEntry.getKey() + "' for race '" + raceName);
						raceForms.put(groupKey, regenerated);
					} else {
						raceForms.put(groupKey, userConfig);
					}
				} else {
					defaultFormConfig.setConfigVersion(FormConfig.CURRENT_VERSION);
					if (!Files.exists(formFilePath)) {
						try { LOADER.saveConfig(formFilePath, defaultFormConfig); } catch (Exception ignored) {}
					}
				}
			}
			userDiskForms.forEach(raceForms::putIfAbsent);
		}

		RACE_FORMS.put(raceName.toLowerCase(), raceForms);
		RACE_CHARACTER.put(raceName.toLowerCase(), characterConfig);
		RACE_STATS.put(raceName.toLowerCase(), statsConfig);
		LOADED_RACES.add(raceName);
	}

	private static void createOrLoadStackForms(boolean isDefault) throws IOException {
		Files.createDirectories(STACK_FORMS_DIR);
		Map<String, FormConfig> finalStackForms = new HashMap<>();
		if (isDefault) FORMS_FACTORY.createDefaultStackForms(STACK_FORMS_DIR, finalStackForms);
		if (isDefault) backupRenamedDefaultForms(STACK_FORMS_DIR, finalStackForms);
		upgradeUserFormFiles(STACK_FORMS_DIR, finalStackForms);
		Map<String, FormConfig> userDiskForms = LOADER.loadStackForms(STACK_FORMS_DIR);

		if (isDefault) {
			for (Map.Entry<String, FormConfig> defaultEntry : finalStackForms.entrySet()) {
				String groupKey = defaultEntry.getKey().toLowerCase();
				FormConfig defaultFormConfig = defaultEntry.getValue();
				Path formFilePath = STACK_FORMS_DIR.resolve(defaultEntry.getKey() + ".json");

				if (userDiskForms.containsKey(groupKey)) {
					FormConfig userConfig = userDiskForms.get(groupKey);
					if (isOutdated(userConfig.getConfigVersion())) {
						FormConfig regenerated = regenerateOutdatedForm(formFilePath, defaultFormConfig, defaultEntry.getKey());
						finalStackForms.put(groupKey, regenerated);
					} else {
						finalStackForms.put(groupKey, userConfig);
					}
				} else {
					defaultFormConfig.setConfigVersion(FormConfig.CURRENT_VERSION);
					if (!Files.exists(formFilePath)) {
						try { LOADER.saveConfig(formFilePath, defaultFormConfig); } catch (Exception ignored) {}
					}
				}
			}
			userDiskForms.forEach(finalStackForms::putIfAbsent);
		} else {
			finalStackForms.putAll(userDiskForms);
		}

		STACK_FORMS = finalStackForms;
	}

	private static EntitiesConfig createDefaultEntitiesConfig() {
		EntitiesConfig config = new EntitiesConfig();
		Map<String, EntitiesConfig.EntityStats> statsMap = config.getDefaultEntityStats();

		addDefaultEntityStats(statsMap, MainEntities.DINO_KID, 30.0, 4.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.DINOSAUR1, 100.0, 8.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.DINOSAUR2, 150.0, 12.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.DINOSAUR3, 75.0, 10.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.SABERTOOTH, 30.0, 5.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.GIANT_FISH, 80.0, 8.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.GIANT_TURTLE, 40.0, 0.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.BANDIT, 75.0, 10.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_SOLDIER, 40.0, 5.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_ROBOT1, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_ROBOT2, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_ROBOT3, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.MINI_BUU, 60.0, 8.0, 6.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_OGRE_RED, 200.0, 20.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_OGRE_BLUE, 200.0, 20.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.MINI_JANEMBA_STAMPEDE, 20.0, 2.0, 0.0);

		EntitiesConfig.TransformSettings transform = config.getTransformDefaults();
		transform.setHealthMultiplier(1.5D);
		transform.setMeleeMultiplier(1.5D);
		transform.setKiMultiplier(1.5D);
		transform.setTriggerHealthPercent(0.5D);

		return config;
	}

	private static void addDefaultEntityStats(Map<String, EntitiesConfig.EntityStats> map, RegistryObject<? extends EntityType<?>> entityType, double health, double meleeDamage, double kiDamage) {
		EntitiesConfig.EntityStats stats = new EntitiesConfig.EntityStats();
		stats.setHealth(health);
		stats.setMeleeDamage(meleeDamage);
		stats.setKiDamage(kiDamage);
		map.put(entityType.getKey().location().toString(), stats);
	}

	private static void loadAllRaces() throws IOException {
		RACE_STATS.clear();
		RACE_CHARACTER.clear();
		RACE_FORMS.clear();
		LOADED_RACES.clear();

		for (String raceName : DEFAULT_RACES) createOrLoadRace(raceName, true);

		try (var stream = Files.list(RACES_DIR)) {
			stream.forEach(racePath -> {
				if (Files.isDirectory(racePath)) {
					String raceName = racePath.getFileName().toString();
					if (!isDefaultRace(raceName)) {
						try {
							createOrLoadRace(raceName, false);
							LogUtil.info(Env.COMMON, "Custom race detected: {}", raceName);
						} catch (IOException e) {
							LogUtil.error(Env.COMMON, "Error loading custom race '{}': {}", raceName, e.getMessage());
							JsonLoadReport.error("config", "races/" + raceName, "Custom race failed to load: " + JsonLoadReport.rootCause(e));
						}
					}
				}
			});
		}
	}

	public static boolean isDefaultRace(String raceName) {
		for (String vanilla : DEFAULT_RACES) if (vanilla.equalsIgnoreCase(raceName)) return true;
		return false;
	}

	private static RaceCharacterConfig createDefaultCharacterConfig(String raceName, boolean isDefault) {
		RaceCharacterConfig config = new RaceCharacterConfig();
		config.setUseVanillaSkin(false);
		config.setCustomModel("");

		if (isDefault) {
			boolean hasGender = RACES_WITH_GENDER.contains(raceName.toLowerCase());
			config.setHasGender(hasGender);

			switch (raceName.toLowerCase()) {
				case "human" -> setupHumanCharacter(config);
				case "saiyan" -> setupSaiyanCharacter(config);
				case "namekian" -> setupNamekianCharacter(config);
				case "frostdemon" -> setupFrostDemonCharacter(config);
				case "bioandroid" -> setupBioAndroidCharacter(config);
				case "majin" -> setupMajinCharacter(config);
				case "glind" -> setupGlindCharacter(config);
				default -> setupDefaultCharacter(config);
			}
		} else {
			config.setHasGender(true);
			setupDefaultCharacter(config);
		}

		return config;
	}

	private static void setupHumanCharacter(RaceCharacterConfig config) {
		config.setUseVanillaSkin(true);
		config.setIsLayered(true);
		config.setRacialSkill("human");
		config.setHeadBones(new String[]{"hair"});
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(1);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#FFD3C9");
		config.setDefaultBodyColor2("#FFD3C9");
		config.setDefaultBodyColor3("#FFD3C9");
		config.setDefaultHairColor("#222629");
		config.setDefaultEye1Color("#222629");
		config.setDefaultEye2Color("#222629");
		config.setDefaultAuraColor("#7FFFFF");
		config.setFormSkillTpCosts("superforms", new Integer[]{21000, 42000, 65000, 104000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
		config.setFormSkillTpCosts("androidforms", new Integer[]{42000, 104000});
	}

	private static void setupSaiyanCharacter(RaceCharacterConfig config) {
		config.setUseVanillaSkin(true);
		config.setIsLayered(true);
		config.setRacialSkill("saiyan");
		config.setHeadBones(new String[]{"hair"});
		config.setHasSaiyanTail(true);
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(1);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#FFD3C9");
		config.setDefaultBodyColor2("#572117");
		config.setDefaultBodyColor3("#FFD3C9");
		config.setDefaultHairColor("#222629");
		config.setDefaultEye1Color("#222629");
		config.setDefaultEye2Color("#222629");
		config.setDefaultAuraColor("#7FFFFF");
		config.setFormSkillTpCosts("superforms", new Integer[]{13000, 21000, 31000, 42000, 52000, 65000, 78000, 104000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupNamekianCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("namekian");
		config.setIsLayered(true);
		config.setHeadBones(new String[]{"ears1", "ears2", "ears3"});
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(0);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#1FAA24");
		config.setDefaultBodyColor2("#BB2024");
		config.setDefaultBodyColor3("#FF86A6");
		config.setDefaultHairColor("#80FF69");
		config.setDefaultEye1Color("#222629");
		config.setDefaultEye2Color("#222629");
		config.setDefaultAuraColor("#7FFF00");
		config.setFormSkillTpCosts("superforms", new Integer[]{23000, 47000, 78000, 117000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupFrostDemonCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("frostdemon");
		config.setIsLayered(true);
		config.setHeadBones(new String[]{"horns1", "horns2", "horns3", "horns4", "horns5"});
		config.setDefaultModelScaling(new Float[]{0.7375f, 0.7375f, 0.7375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(0);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#FFFFFF");
		config.setDefaultBodyColor2("#E8A2FF");
		config.setDefaultBodyColor3("#FF39A9");
		config.setDefaultHairColor("#8B1BCC");
		config.setDefaultEye1Color("#FF001D");
		config.setDefaultEye2Color("#FF001D");
		config.setDefaultAuraColor("#5F00FF");
		config.setFormSkillTpCosts("superforms", new Integer[]{18000, 31000, 52000, 83000, 117000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupBioAndroidCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("bioandroid");
		config.setIsLayered(true);
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(0);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#396E14");
		config.setDefaultBodyColor2("#c7b022");
		config.setDefaultBodyColor3("#fa9a02");
		config.setDefaultHairColor("#38628F");
		config.setDefaultEye1Color("#2E2424");
		config.setDefaultEye2Color("#F06F6E");
		config.setDefaultAuraColor("#1AA700");
		config.setFormSkillTpCosts("superforms", new Integer[]{26000, 57000, 88000, 125000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupMajinCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("majin");
		config.setIsLayered(true);
		config.setHeadBones(new String[]{"majin1", "majin2", "majin3"});
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(0);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#FFA4FF");
		config.setDefaultBodyColor2("#FFA4FF");
		config.setDefaultBodyColor3("#FFA4FF");
		config.setDefaultHairColor("#FFA4FF");
		config.setDefaultEye1Color("#B40000");
		config.setDefaultEye2Color("#B40000");
		config.setDefaultAuraColor("#FF6DFF");
		config.setFormSkillTpCosts("superforms", new Integer[]{23000, 47000, 78000, 114000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupGlindCharacter(RaceCharacterConfig config) {
		config.setCustomModel("human");
		config.setIsLayered(true);
		config.setRacialSkill("glind");
		config.setHeadBones(new String[]{"ears1", "hair"});
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(1);
		config.setSlimBodyTypes(new Integer[]{2});
		config.setDefaultHairType(28);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#D2A3DA");
		config.setDefaultBodyColor2("#D2A3DA");
		config.setDefaultBodyColor3("#D2A3DA");
		config.setDefaultHairColor("#EDEDED");
		config.setDefaultEye1Color("#222629");
		config.setDefaultEye2Color("#222629");
		config.setDefaultAuraColor("#E3C4FF");
		config.setFormSkillTpCosts("superforms", new Integer[]{18000, 31000, 52000, 83000, 117000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupDefaultCharacter(RaceCharacterConfig config) {
		config.setUseVanillaSkin(true);
		config.setIsLayered(true);
		config.setRacialSkill("human");
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(1);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#FFD3C9");
		config.setDefaultBodyColor2("#FFD3C9");
		config.setDefaultBodyColor3("#FFD3C9");
		config.setDefaultHairColor("#222629");
		config.setDefaultEye1Color("#222629");
		config.setDefaultEye2Color("#222629");
		config.setDefaultAuraColor("#7FFFFF");
		config.setFormSkillTpCosts("superforms", new Integer[]{});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{});
	}

	private static final double DEFAULT_VIT_SCALING_MAX_RATIO = 4.0;
	private static final double DEFAULT_DEF_SCALING_MAX_RATIO = 4.0;

	private static RaceStatsConfig createDefaultStatsConfig() {
		RaceStatsConfig config = new RaceStatsConfig();

		setupInitialStats(config.getOrCreateClassStats("warrior"), 10, 0, 5, 5, 0, 0, 1.75, 0.06, 4.0, 0.08, 12.0, 0.045, 0.06);
		setupScalingStats(config.getOrCreateClassStats("warrior"), 1.4, 1.0, 0.24, 1.6, 1.8, 0.5, 1.5);

		setupInitialStats(config.getOrCreateClassStats("spiritualist"), 0, 0, 0, 0, 10, 10, 0.5, 0.015, 8.0, 0.20, 5.0, 0.01875, 0.025);
		setupScalingStats(config.getOrCreateClassStats("spiritualist"), 0.3, 0.5, 0.156, 0.7, 1.4, 1.9, 3.7);

		setupInitialStats(config.getOrCreateClassStats("martialartist"), 0, 10, 0, 10, 0, 0, 1.5, 0.0525, 4.0, 0.08, 9.0, 0.03375, 0.045);
		setupScalingStats(config.getOrCreateClassStats("martialartist"), 0.8, 1.8, 0.18, 1.3, 2.2, 0.6, 1.6);

		setupInitialStats(config.getOrCreateClassStats("berserker"), 10, 0, 0, 10, 0, 0, 1.0, 0.0375, 2.0, 0.04, 14.0, 0.04875, 0.065);
		setupScalingStats(config.getOrCreateClassStats("berserker"), 1.7, 0.8, 0.18, 1.1, 3.0, 0.4, 1.3);

		setupInitialStats(config.getOrCreateClassStats("paladin"), 0, 5, 10, 5, 0, 0, 2.0, 0.0675, 4.0, 0.08, 8.0, 0.03, 0.04);
		setupScalingStats(config.getOrCreateClassStats("paladin"), 0.8, 1.2, 0.336, 1.2, 2.0, 0.6, 1.2);

		setupInitialStats(config.getOrCreateClassStats("tank"), 0, 0, 10, 10, 0, 0, 2.25, 0.075, 5.0, 0.10, 9.0, 0.03375, 0.045);
		setupScalingStats(config.getOrCreateClassStats("tank"), 0.6, 0.7, 0.384, 1.5, 2.5, 0.5, 0.8);
		config.getOrCreateClassStats("tank").setTpGainMultiplier(1.25);

		setupInitialStats(config.getOrCreateClassStats("cleric"), 0, 0, 5, 0, 0, 15, 0.5, 0.015, 12.0, 0.24, 16.0, 0.045, 0.06);
		setupScalingStats(config.getOrCreateClassStats("cleric"), 0.5, 0.5, 0.168, 2.6, 1.2, 0.8, 3.0);
		config.getOrCreateClassStats("cleric").setTpGainMultiplier(1.25);
		config.getOrCreateClassStats("cleric").setTpCostMultiplier(0.9);

		setupInitialStats(config.getOrCreateClassStats("unversed"), 4, 3, 3, 4, 3, 3, 1.4, 0.048, 5.0, 0.10, 10.0, 0.0375, 0.05);
		setupScalingStats(config.getOrCreateClassStats("unversed"), 0.9, 0.9, 0.22, 1.35, 1.9, 0.85, 1.75);
		setupDefaultPassives(config);
		return config;
	}

	private static void setupDefaultPassives(RaceStatsConfig config) {
		Map<String, Double> warrior = new HashMap<>();
		warrior.put("comboHits", 3.0);
		warrior.put("maxStacks", 5.0);
		warrior.put("stmRegenPerStack", 0.10);
		warrior.put("armorPenAtMax", 0.10);
		warrior.put("stackDurationTicks", 100.0);
		warrior.put("comboResetTicks", 60.0);
		setupPassive(config.getOrCreateClassStats("warrior"), warrior);

		Map<String, Double> martial = new HashMap<>();
		martial.put("maxBonus", 0.25);
		martial.put("hpHigh", 0.75);
		martial.put("hpLow", 0.25);
		setupPassive(config.getOrCreateClassStats("martialartist"), martial);

		Map<String, Double> spiritualist = new HashMap<>();
		spiritualist.put("cdPrimary", 0.20);
		spiritualist.put("cdSecondary", 0.15);
		spiritualist.put("durationBonus", 0.25);
		setupPassive(config.getOrCreateClassStats("spiritualist"), spiritualist);

		Map<String, Double> berserker = new HashMap<>();
		berserker.put("hpThreshHigh", 0.66);
		berserker.put("hpThreshLow", 0.33);
		berserker.put("hpRegenHigh", 0.25);
		berserker.put("critHigh", 0.10);
		berserker.put("hpRegenLow", 0.75);
		berserker.put("critLow", 0.25);
		setupPassive(config.getOrCreateClassStats("berserker"), berserker);

		Map<String, Double> paladin = new HashMap<>();
		paladin.put("redirectPct", 0.15);
		paladin.put("lifestealPct", 0.15);
		setupPassive(config.getOrCreateClassStats("paladin"), paladin);

		Map<String, Double> tank = new HashMap<>();
		tank.put("stmToHpRegenRatio", 0.5);
		tank.put("healingBonus", 0.25);
		tank.put("lowHpThreshold", 0.30);
		tank.put("lowHpMultiplier", 2.0);
		setupPassive(config.getOrCreateClassStats("tank"), tank);

		Map<String, Double> cleric = new HashMap<>();
		cleric.put("cdPrimary", 0.20);
		cleric.put("cdSecondary", 0.15);
		cleric.put("durationBonus", 0.25);
		setupPassive(config.getOrCreateClassStats("cleric"), cleric);

		Map<String, Double> unversed = new HashMap<>();
		unversed.put("physicalBonus", 0.15);
		unversed.put("kiBonus", 0.15);
		unversed.put("physicalCharges", 3.0);
		unversed.put("meleeHitsToPrime", 3.0);
		unversed.put("chargeDurationTicks", 160.0);
		unversed.put("comboResetTicks", 100.0);
		unversed.put("strikeWindowTicks", 60.0);
		setupPassive(config.getOrCreateClassStats("unversed"), unversed);
	}

	private static void setupPassive(RaceStatsConfig.ClassStats classStats, Map<String, Double> values) {
		RaceStatsConfig.Passive passive = classStats.getPassive();
		passive.setEnabled(true);
		passive.setValues(values);
	}

	private static void setupInitialStats(RaceStatsConfig.ClassStats classStats, int str, int skp, int res, int vit, int pwr, int ene, double baseHp5, double hp5VitScaling, double baseEp5, double ep5EneScaling, double baseSp5, double sp5VitScaling, double sp5ResScaling) {
		RaceStatsConfig.BaseStats base = classStats.getBaseStats();
		base.setStrength(str);
		base.setStrikePower(skp);
		base.setResistance(res);
		base.setVitality(vit);
		base.setKiPower(pwr);
		base.setEnergy(ene);
		classStats.setBaseHp5(baseHp5);
		classStats.setHp5VitScaling(hp5VitScaling);
		classStats.setBaseEp5(baseEp5);
		classStats.setEp5EneScaling(ep5EneScaling);
		classStats.setBaseSp5(baseSp5);
		classStats.setSp5StmScaling(sp5VitScaling);
		classStats.setSp5ResScaling(sp5ResScaling);
	}

	private static void setupScalingStats(RaceStatsConfig.ClassStats classStats, double strScale, double skpScale, double defScale, double stmScale, double vitScale, double pwrScale, double eneScale) {
		RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
		scaling.setStrengthScaling(strScale);
		scaling.setStrikePowerScaling(skpScale);
		scaling.setDefenseScaling(defScale);
		scaling.setDefenseScalingMax(defScale * DEFAULT_DEF_SCALING_MAX_RATIO);
		scaling.setStaminaScaling(stmScale);
		scaling.setVitalityScaling(vitScale);
		scaling.setVitalityScalingMax(vitScale * DEFAULT_VIT_SCALING_MAX_RATIO);
		scaling.setKiPowerScaling(pwrScale);
		scaling.setEnergyScaling(eneScale);
	}

	public static RaceStatsConfig getRaceStats(String raceName) {
		String key = raceName != null ? raceName.toLowerCase() : "human";
		if (useServerSync()) {
			Map<String, RaceStatsConfig> synced = SYNCED.stats != null ? SYNCED.stats : Collections.emptyMap();
			RaceStatsConfig config = synced.getOrDefault(key, synced.get("human"));
			return config != null ? config : createDefaultStatsConfig();
		}
		RaceStatsConfig config = RACE_STATS.getOrDefault(key, RACE_STATS.get("human"));
		return config != null ? config : createDefaultStatsConfig();
	}

	public static RaceCharacterConfig getRaceCharacter(String raceName) {
		String key = raceName != null ? raceName.toLowerCase() : "human";
		if (useServerSync()) {
			Map<String, RaceCharacterConfig> synced = SYNCED.character != null ? SYNCED.character : Collections.emptyMap();
			RaceCharacterConfig config = synced.getOrDefault(key, synced.get("human"));
			return config != null ? config : createDefaultCharacterConfig(key, false);
		}
		RaceCharacterConfig config = RACE_CHARACTER.getOrDefault(key, RACE_CHARACTER.get("human"));
		return config != null ? config : createDefaultCharacterConfig(key, false);
	}

	public static List<String> getLoadedRaces() {
		List<String> races;
		if (useServerSync()) races = SYNCED.character != null ? new ArrayList<>(SYNCED.character.keySet()) : new ArrayList<>();
		else races = new ArrayList<>(LOADED_RACES);

		races.sort((r1, r2) -> {
			int index1 = -1;
			int index2 = -1;
			for (int i = 0; i < DEFAULT_RACES.length; i++) {
				if (DEFAULT_RACES[i].equalsIgnoreCase(r1)) index1 = i;
				if (DEFAULT_RACES[i].equalsIgnoreCase(r2)) index2 = i;
			}
			if (index1 != -1 && index2 != -1) return Integer.compare(index1, index2);
			if (index1 != -1) return -1;
			if (index2 != -1) return 1;
			return r1.compareToIgnoreCase(r2);
		});

		return races;
	}

	public static List<String> getDefaultRaces() { return Arrays.asList(DEFAULT_RACES); }
	public static boolean isRaceLoaded(String raceName) {
		if (raceName == null) return false;
		if (useServerSync()) return SYNCED.character != null && SYNCED.character.containsKey(raceName.toLowerCase());
		return LOADED_RACES.stream().anyMatch(r -> r.equalsIgnoreCase(raceName));
	}
	public static GeneralUserConfig getUserConfig() { return userConfig != null ? userConfig : new GeneralUserConfig(); }
	private static final class DefaultConfigs {
		private static final GeneralServerConfig SERVER = new GeneralServerConfig();
		private static final CombatConfig COMBAT = new CombatConfig();
		private static final TrainingConfig TRAINING = new TrainingConfig();
		private static final SkillsConfig SKILLS = new SkillsConfig();
		private static final TechniqueConfig TECHNIQUES = new TechniqueConfig();
		private static final EntitiesConfig ENTITIES = new EntitiesConfig();
	}

	public static GeneralServerConfig getServerConfig() {
		if (useServerSync()) return SYNCED.generalServer != null ? SYNCED.generalServer : DefaultConfigs.SERVER;
		return serverConfig != null ? serverConfig : DefaultConfigs.SERVER;
	}
	public static CombatConfig getCombatConfig() {
		if (useServerSync()) return SYNCED.combat != null ? SYNCED.combat : DefaultConfigs.COMBAT;
		return combatConfig != null ? combatConfig : DefaultConfigs.COMBAT;
	}
	public static TrainingConfig getTrainingConfig() {
		if (useServerSync()) return SYNCED.training != null ? SYNCED.training : DefaultConfigs.TRAINING;
		return trainingConfig != null ? trainingConfig : DefaultConfigs.TRAINING;
	}
	private static void loadHudLayoutConfig() {
		if (FMLEnvironment.dist != Dist.CLIENT) {
			hudLayoutConfig = new HudLayoutConfig();
			return;
		}
		Path path = CONFIG_DIR.resolve(HudLayoutConfig.FILE_NAME + ".json");
		boolean isNew = !Files.exists(path);
		hudLayoutConfig = loadAndValidate(path, HudLayoutConfig.class, HudLayoutConfig::new, HudLayoutConfig::getConfigVersion, HudLayoutConfig::setConfigVersion, HudLayoutConfig.CURRENT_VERSION, null);
		boolean movedFromUserConfig = userConfig != null && userConfig.migrateLegacyHud(hudLayoutConfig, isNew);
		boolean renamed = hudLayoutConfig.migrateLegacyNames();
		if (movedFromUserConfig || renamed) saveHudLayoutConfig();
		if (movedFromUserConfig) saveGeneralUserConfig();
	}

	public static HudLayoutConfig getHudLayoutConfig() {
		if (hudLayoutConfig == null) hudLayoutConfig = new HudLayoutConfig();
		return hudLayoutConfig;
	}

	public static void saveHudLayoutConfig() {
		try { LOADER.saveConfig(CONFIG_DIR.resolve(HudLayoutConfig.FILE_NAME + ".json"), getHudLayoutConfig()); }
		catch (IOException e) { LogUtil.error(Env.COMMON, "Error saving HUD layout: {}", e.getMessage()); }
	}

	public static void reloadHudLayoutConfig() {
		Path path = CONFIG_DIR.resolve(HudLayoutConfig.FILE_NAME + ".json");
		if (!Files.exists(path)) return;
		try {
			HudLayoutConfig loaded = LOADER.loadConfig(path, HudLayoutConfig.class);
			if (loaded == null) return;
			hudLayoutConfig = loaded;
			if (loaded.migrateLegacyNames()) saveHudLayoutConfig();
		} catch (Exception e) {
			LogUtil.warn(Env.COMMON, "Could not reload {}.json, keeping the layout in memory: {}", HudLayoutConfig.FILE_NAME, e.getMessage());
		}
	}

	public static boolean isClientOnlyConfig(String configFile) {
		return CLIENT_ONLY_CONFIG.equals(configFile) || HudLayoutConfig.FILE_NAME.equals(configFile);
	}

	public static void saveGeneralUserConfig() {
		try { LOADER.saveConfig(CONFIG_DIR.resolve("general-user.json"), userConfig); }
		catch (IOException e) { LogUtil.error(Env.COMMON, "Error saving user configuration: {}", e.getMessage()); }
	}

	public static boolean updateConfigValue(String configFileName, String optionalSubtype, String key, String value) {
		try {
			Path configPath = CONFIG_DIR.resolve(configFileName + ".json");
			if (!Files.exists(configPath)) return false;

			String content = Files.readString(configPath);
			JsonObject rootObj = JsonParser.parseString(content).getAsJsonObject();
			JsonObject targetObj = rootObj;

			if (optionalSubtype != null && !optionalSubtype.isEmpty()) {
				if (rootObj.has(optionalSubtype) && rootObj.get(optionalSubtype).isJsonObject()) {
					targetObj = rootObj.getAsJsonObject(optionalSubtype);
				} else return false;
			}

			if (!targetObj.has(key)) return false;

			JsonElement existing = targetObj.get(key);
			if (existing != null && (existing.isJsonObject() || existing.isJsonArray())) return false;

			JsonElement parsedValue;
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
				parsedValue = JsonParser.parseString(value.toLowerCase(Locale.ROOT));
			} else {
				try { Double.parseDouble(value); parsedValue = JsonParser.parseString(value); }
				catch (NumberFormatException e) { parsedValue = GSON.toJsonTree(value); }
			}

			targetObj.add(key, parsedValue);
			Files.writeString(configPath, GSON.toJson(rootObj));
			return true;
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Error updating config value: " + e.getMessage());
			return false;
		}
	}

	public static List<String> getAvailableConfigFiles() {
		if (!CACHED_CONFIG_FILES.isEmpty()) return CACHED_CONFIG_FILES;
		try (Stream<Path> stream = Files.walk(CONFIG_DIR)) {
			stream.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".json"))
					.filter(p -> !p.getFileName().toString().toLowerCase().startsWith("old_"))
					.filter(p -> !CONFIG_DIR.relativize(p).toString().replace("\\", "/").startsWith(OLD_BACKUP_DIR + "/"))
					.forEach(p -> {
						String relativePath = CONFIG_DIR.relativize(p).toString().replace("\\", "/");
						CACHED_CONFIG_FILES.add(relativePath.substring(0, relativePath.length() - 5));
					});
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Error scanning config files: " + e.getMessage());
		}
		return CACHED_CONFIG_FILES;
	}

	public static boolean isSubtype(String configFileName, String name) {
		if (name == null || name.isEmpty()) return false;
		try {
			Path configPath = CONFIG_DIR.resolve(configFileName + ".json");
			if (!Files.exists(configPath)) return false;
			JsonObject rootObj = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
			return rootObj.has(name) && rootObj.get(name).isJsonObject();
		} catch (Exception ignored) {
			return false;
		}
	}

	public static List<String> getKeysOrSubtypes(String configFileName, String optionalSubtype) {
		List<String> list = new ArrayList<>();
		try {
			Path configPath = CONFIG_DIR.resolve(configFileName + ".json");
			if (!Files.exists(configPath)) return list;
			String content = Files.readString(configPath);
			JsonObject rootObj = JsonParser.parseString(content).getAsJsonObject();
			JsonObject targetObj = rootObj;

			if (optionalSubtype != null && !optionalSubtype.isEmpty()) {
				if (rootObj.has(optionalSubtype) && rootObj.get(optionalSubtype).isJsonObject()) targetObj = rootObj.getAsJsonObject(optionalSubtype);
				else return list;
			}

			for (Map.Entry<String, JsonElement> entry : targetObj.entrySet()) list.add(entry.getKey());

		} catch (Exception ignored) {}
		return list;
	}

	public static List<String> getValueSuggestions(String configFileName, String optionalSubtype, String key) {
		java.util.Set<String> list = new java.util.LinkedHashSet<>();
		try {
			Path configPath = CONFIG_DIR.resolve(configFileName + ".json");
			if (!Files.exists(configPath)) return new ArrayList<>();
			String content = Files.readString(configPath);
			JsonObject rootObj = JsonParser.parseString(content).getAsJsonObject();
			JsonObject targetObj = rootObj;

			if (optionalSubtype != null && !optionalSubtype.isEmpty()) {
				if (rootObj.has(optionalSubtype) && rootObj.get(optionalSubtype).isJsonObject()) targetObj = rootObj.getAsJsonObject(optionalSubtype);
				else return new ArrayList<>();
			}

			if (targetObj.has(key)) {
				JsonElement element = targetObj.get(key);
				if (element.isJsonPrimitive()) {
					if (element.getAsJsonPrimitive().isBoolean()) {
						list.add("true");
						list.add("false");
					} else if (element.getAsJsonPrimitive().isNumber()) {
						String currentNum = element.getAsString();
						list.add(currentNum);
						if (currentNum.contains(".")) list.addAll(Arrays.asList("0.0", "0.5", "1.0", "1.5", "2.0", "5.0", "10.0"));
						else list.addAll(Arrays.asList("0", "1", "2", "5", "10", "20", "50", "100"));
					} else if (element.getAsJsonPrimitive().isString()) list.add(element.getAsString());
				}
			}
		} catch (Exception ignored) {}
		return new ArrayList<>(list);
	}

	public static void reloadSpecificConfig(String configFilePath) throws IOException {
		invalidateSyncPayloads();
		Path path = CONFIG_DIR.resolve(configFilePath + ".json");
		if (configFilePath.equals("general-server")) {
			serverConfig = LOADER.loadConfig(path, GeneralServerConfig.class);
		} else if (configFilePath.equals("combat")) {
			combatConfig = LOADER.loadConfig(path, CombatConfig.class);
		} else if (configFilePath.equals("training")) {
			trainingConfig = LOADER.loadConfig(path, TrainingConfig.class);
		} else if (configFilePath.equals("skills")) {
			skillsConfig = LOADER.loadConfig(path, SkillsConfig.class);
		} else if (configFilePath.equals("techniques")) {
			techniqueConfig = LOADER.loadConfig(path, TechniqueConfig.class);
		} else if (configFilePath.equals("entities")) {
			entitiesConfig = LOADER.loadConfig(path, EntitiesConfig.class);
		} else if (configFilePath.startsWith("races/")) {
			String[] parts = configFilePath.split("/");
			String raceName = parts[1];
			if (parts[2].equals("stats")) {
				RACE_STATS.put(raceName.toLowerCase(), LOADER.loadConfig(path, RaceStatsConfig.class));
			} else if (parts[2].equals("character")) {
				RACE_CHARACTER.put(raceName.toLowerCase(), LOADER.loadConfig(path, RaceCharacterConfig.class));
			} else if (parts[2].equals("forms")) {
				FormConfig formConfig = LOADER.loadConfig(path, FormConfig.class);
				RACE_FORMS.computeIfAbsent(raceName.toLowerCase(), k -> new HashMap<>())
						.put(formGroupKey(formConfig, parts[3]), formConfig);
			}
		} else if (configFilePath.startsWith("forms/")) {
			FormConfig formConfig = LOADER.loadConfig(path, FormConfig.class);
			STACK_FORMS.put(formGroupKey(formConfig, configFilePath.split("/")[1]), formConfig);
		}
	}

	public static boolean saveRawConfig(String configFilePath, String json) {
		try {
			JsonElement parsed = JsonParser.parseString(json);
			Path path = CONFIG_DIR.resolve(configFilePath + ".json");
			if (!Files.exists(path)) return false;
			Files.writeString(path, GSON.toJson(parsed));
			return true;
		} catch (Exception e) {
			LogUtil.error(Env.COMMON, "Error saving raw config '{}': {}", configFilePath, e.getMessage());
			return false;
		}
	}

	public static String getSpecificConfigJson(String configFilePath) {
		Path path = CONFIG_DIR.resolve(configFilePath + ".json");
		if (!Files.exists(path)) return null;
		try {
			String content = Files.readString(path);
			return (content == null || content.isBlank()) ? null : content;
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Could not read config file: " + configFilePath);
			return null;
		}
	}

	private static String formGroupKey(FormConfig config, String fileNameFallback) {
		if (config != null && config.getGroupName() != null && !config.getGroupName().isEmpty()) {
			return config.getGroupName().toLowerCase();
		}
		return fileNameFallback.toLowerCase();
	}

	public static boolean isViewingRemoteServerConfig() {
		return serverSyncActive && ServerLifecycleHooks.getCurrentServer() == null;
	}

	public static List<String> getEditorConfigFiles() {
		if (!isViewingRemoteServerConfig()) return new ArrayList<>(getAvailableConfigFiles());
		SyncedConfigs synced = SYNCED;
		List<String> files = new ArrayList<>(List.of("general-server", "combat", "training", "skills", "techniques", "entities", CLIENT_ONLY_CONFIG, HudLayoutConfig.FILE_NAME));
		synced.character.keySet().forEach(race -> files.add("races/" + race + "/character"));
		synced.stats.keySet().forEach(race -> files.add("races/" + race + "/stats"));
		synced.forms.forEach((race, groups) -> groups.keySet().forEach(group -> files.add("races/" + race + "/forms/" + group)));
		synced.stackForms.keySet().forEach(group -> files.add("forms/" + group));
		return files;
	}

	public static String getEditorConfigJson(String configFilePath) {
		if (!isViewingRemoteServerConfig() || isClientOnlyConfig(configFilePath)) return getSpecificConfigJson(configFilePath);
		SyncedConfigs synced = SYNCED;
		Object value = switch (configFilePath) {
			case "general-server" -> synced.generalServer;
			case "combat" -> synced.combat;
			case "training" -> synced.training;
			case "skills" -> synced.skills;
			case "techniques" -> synced.techniques;
			case "entities" -> synced.entities;
			default -> null;
		};
		String[] parts = configFilePath.split("/");
		if (value == null && parts.length >= 3 && parts[0].equals("races")) {
			if (parts[2].equals("character")) value = synced.character.get(parts[1]);
			else if (parts[2].equals("stats")) value = synced.stats.get(parts[1]);
			else if (parts[2].equals("forms") && parts.length >= 4) value = synced.forms.getOrDefault(parts[1], Map.of()).get(parts[3]);
		} else if (value == null && parts.length == 2 && parts[0].equals("forms")) {
			value = synced.stackForms.get(parts[1]);
		}
		return value != null ? GSON.toJson(value) : null;
	}

	public static void reloadClientOnlyConfig(String configFilePath) {
		if (CLIENT_ONLY_CONFIG.equals(configFilePath)) {
			userConfig = loadAndValidate(CONFIG_DIR.resolve(CLIENT_ONLY_CONFIG + ".json"), GeneralUserConfig.class, GeneralUserConfig::new, GeneralUserConfig::getConfigVersion, GeneralUserConfig::setConfigVersion, GeneralUserConfig.CURRENT_VERSION, null);
		} else if (HudLayoutConfig.FILE_NAME.equals(configFilePath)) {
			reloadHudLayoutConfig();
		}
	}

	private static void invalidateSyncPayloads() {
		cachedSyncPayloads = null;
	}

	public static List<SyncPayload> getSyncPayloads() {
		List<SyncPayload> cached = cachedSyncPayloads;
		if (cached != null) return cached;
		List<SyncPayload> payloads = new ArrayList<>();
		JsonObject server = GSON.toJsonTree(serverConfig != null ? serverConfig : DefaultConfigs.SERVER).getAsJsonObject();
		server.remove(SYNC_EXCLUDED_SERVER_SECTION);
		addSyncPayload(payloads, "general-server", GSON.toJson(server));
		addSyncPayload(payloads, "combat", GSON.toJson(combatConfig != null ? combatConfig : DefaultConfigs.COMBAT));
		addSyncPayload(payloads, "training", GSON.toJson(trainingConfig != null ? trainingConfig : DefaultConfigs.TRAINING));
		addSyncPayload(payloads, "skills", GSON.toJson(skillsConfig != null ? skillsConfig : DefaultConfigs.SKILLS));
		addSyncPayload(payloads, "techniques", GSON.toJson(techniqueConfig != null ? techniqueConfig : DefaultConfigs.TECHNIQUES));
		addSyncPayload(payloads, "entities", GSON.toJson(entitiesConfig != null ? entitiesConfig : DefaultConfigs.ENTITIES));
		for (String race : new ArrayList<>(LOADED_RACES)) {
			String key = race.toLowerCase();
			RaceCharacterConfig character = RACE_CHARACTER.get(key);
			RaceStatsConfig stats = RACE_STATS.get(key);
			if (character != null) addSyncPayload(payloads, "races/" + key + "/character", GSON.toJson(character));
			if (stats != null) addSyncPayload(payloads, "races/" + key + "/stats", GSON.toJson(stats));
			Map<String, FormConfig> forms = RACE_FORMS.get(key);
			if (forms == null) continue;
			for (Map.Entry<String, FormConfig> entry : forms.entrySet()) {
				if (entry.getValue() != null) addSyncPayload(payloads, "races/" + key + "/forms/" + entry.getKey(), GSON.toJson(entry.getValue()));
			}
		}
		for (Map.Entry<String, FormConfig> entry : STACK_FORMS.entrySet()) {
			if (entry.getValue() != null) addSyncPayload(payloads, "forms/" + entry.getKey(), GSON.toJson(entry.getValue()));
		}
		List<SyncPayload> result = List.copyOf(payloads);
		cachedSyncPayloads = result;
		return result;
	}

	private static void addSyncPayload(List<SyncPayload> payloads, String path, String json) {
		if (json == null || json.isBlank()) return;
		payloads.add(new SyncPayload(path, CompressionUtil.compress(json)));
	}

	public static void sendConfigSync(ServerPlayer player) {
		List<SyncPayload> payloads = getSyncPayloads();
		boolean reset = true;
		for (SyncPayload payload : payloads) {
			NetworkHandler.sendToPlayer(new SyncServerConfigS2C(payload.path(), payload.data(), reset), player);
			reset = false;
		}
		NetworkHandler.sendToPlayer(new SyncServerConfigS2C(SYNC_END_MARKER, new byte[0], reset), player);
	}

	public static boolean applySpecificSyncedConfig(String configFilePath, String json) {
		if (SYNC_END_MARKER.equals(configFilePath)) {
			commitServerSyncBatch();
			return true;
		}
		SyncedConfigs target = pendingSync != null ? pendingSync : SYNCED;
		try {
			if (configFilePath.equals("general-server")) target.generalServer = GSON.fromJson(json, GeneralServerConfig.class);
			else if (configFilePath.equals("combat")) target.combat = GSON.fromJson(json, CombatConfig.class);
			else if (configFilePath.equals("training")) target.training = GSON.fromJson(json, TrainingConfig.class);
			else if (configFilePath.equals("skills")) target.skills = GSON.fromJson(json, SkillsConfig.class);
			else if (configFilePath.equals("techniques")) target.techniques = GSON.fromJson(json, TechniqueConfig.class);
			else if (configFilePath.equals("entities")) target.entities = GSON.fromJson(json, EntitiesConfig.class);
			else if (configFilePath.startsWith("races/")) {
				String[] parts = configFilePath.split("/");
				if (parts.length < 3) return false;
				String raceName = parts[1].toLowerCase();
				if (parts[2].equals("stats")) {
					target.stats.put(raceName, GSON.fromJson(json, RaceStatsConfig.class));
				} else if (parts[2].equals("character")) {
					target.character.put(raceName, GSON.fromJson(json, RaceCharacterConfig.class));
				} else if (parts[2].equals("forms") && parts.length >= 4) {
					FormConfig formConfig = GSON.fromJson(json, FormConfig.class);
					target.forms.computeIfAbsent(raceName, k -> new HashMap<>()).put(formGroupKey(formConfig, parts[3]), formConfig);
				}
			} else if (configFilePath.startsWith("forms/")) {
				FormConfig formConfig = GSON.fromJson(json, FormConfig.class);
				target.stackForms.put(formGroupKey(formConfig, configFilePath.split("/")[1]), formConfig);
			}
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Error applying synced config '{}': {}", configFilePath, e.getMessage());
		}
		return false;
	}

	public static void beginServerSyncBatch() {
		pendingSync = new SyncedConfigs();
	}

	private static void commitServerSyncBatch() {
		if (pendingSync == null) return;
		SYNCED = pendingSync;
		pendingSync = null;
		syncClientThread = Thread.currentThread();
		serverSyncActive = true;
	}

	public static void clearServerSync() {
		pendingSync = null;
		SYNCED = new SyncedConfigs();
		serverSyncActive = false;
		syncClientThread = null;
	}

	private static boolean useServerSync() {
		if (!serverSyncActive) return false;
		if (ServerLifecycleHooks.getCurrentServer() == null) return true;
		return Thread.currentThread() == syncClientThread;
	}

	public static Map<String, RaceStatsConfig> getAllRaceStats() {
		if (useServerSync()) return SYNCED.stats != null ? SYNCED.stats : new HashMap<>();
		return new HashMap<>(RACE_STATS);
	}
	public static Map<String, RaceCharacterConfig> getAllRaceCharacters() {
		if (useServerSync()) return SYNCED.character != null ? SYNCED.character : new HashMap<>();
		return new HashMap<>(RACE_CHARACTER);
	}
	public static Map<String, Map<String, FormConfig>> getAllForms() {
		if (useServerSync()) return SYNCED.forms != null ? SYNCED.forms : new HashMap<>();
		return RACE_FORMS;
	}
	public static Map<String, FormConfig> getAllFormsForRace(String raceName) { return getAllForms().getOrDefault(raceName.toLowerCase(), new HashMap<>()); }
	public static FormConfig getFormGroup(String raceName, String groupName) {
		String qualifierRace = FusionForms.qualifierRace(groupName);
		if (qualifierRace != null) {
			raceName = qualifierRace;
			groupName = FusionForms.baseGroup(groupName);
		}
		Map<String, FormConfig> raceForms = getAllFormsForRace(raceName);
		return raceForms != null ? raceForms.get(groupName.toLowerCase()) : null;
	}
	public static FormConfig.FormData getForm(String raceName, String groupName, String formName) {
		FormConfig group = getFormGroup(raceName, groupName);
		return group != null ? group.getForm(formName) : null;
	}
	public static Map<String, FormConfig> getAllStackForms() {
		if (useServerSync()) return SYNCED.stackForms != null ? SYNCED.stackForms : new HashMap<>();
		return STACK_FORMS;
	}
	public static FormConfig getStackFormGroup(String groupName) {
		Map<String, FormConfig> stackForms = getAllStackForms();
		return stackForms != null ? stackForms.get(groupName.toLowerCase()) : null;
	}
	public static FormConfig.FormData getStackForm(String groupName, String formName) {
		FormConfig group = getStackFormGroup(groupName);
		return group != null ? group.getForm(formName) : null;
	}
	public static SkillsConfig getSkillsConfig() {
		if (useServerSync()) return SYNCED.skills != null ? SYNCED.skills : DefaultConfigs.SKILLS;
		return skillsConfig != null ? skillsConfig : DefaultConfigs.SKILLS;
	}
	public static TechniqueConfig getTechniqueConfig() {
		if (useServerSync()) return SYNCED.techniques != null ? SYNCED.techniques : DefaultConfigs.TECHNIQUES;
		return techniqueConfig != null ? techniqueConfig : DefaultConfigs.TECHNIQUES;
	}
	public static EntitiesConfig getEntitiesConfig() {
		if (useServerSync()) return SYNCED.entities != null ? SYNCED.entities : DefaultConfigs.ENTITIES;
		return entitiesConfig != null ? entitiesConfig : DefaultConfigs.ENTITIES;
	}
	public static EntitiesConfig.EntityStats getEntityStats(String registryName) {
		EntitiesConfig config = getEntitiesConfig();
		return config != null && config.getDefaultEntityStats() != null ? config.getDefaultEntityStats().get(registryName) : null;
	}

	public static EntitiesConfig.TransformSettings getEntityTransformDefaults() {
		EntitiesConfig config = getEntitiesConfig();
		EntitiesConfig.TransformSettings transform = config != null ? config.getTransformDefaults() : null;
		return transform != null ? transform : new EntitiesConfig.TransformSettings();
	}
}
