package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.animation.AnimationCache;
import com.dragonminez.common.init.MainEntities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public class ConfigManager {
	public static final int CONFIG_VERSION = 7;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
	private static final ConfigLoader LOADER = new ConfigLoader(GSON);
	private static final DefaultFormsFactory FORMS_FACTORY = new DefaultFormsFactory();

	private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
	private static final Path STACK_FORMS_DIR = CONFIG_DIR.resolve("forms");
	private static final Path RACES_DIR = CONFIG_DIR.resolve("races");
	private static final String[] DEFAULT_RACES = {"human", "saiyan", "namekian", "frostdemon", "bioandroid", "majin"};
	private static final Set<String> RACES_WITH_GENDER = new HashSet<>(Arrays.asList("human", "saiyan", "majin"));

	private static final Map<String, RaceStatsConfig> RACE_STATS = new HashMap<>();
	private static final Map<String, RaceCharacterConfig> RACE_CHARACTER = new HashMap<>();
	private static final Map<String, Map<String, FormConfig>> RACE_FORMS = new HashMap<>();
	private static final List<String> LOADED_RACES = new ArrayList<>();
	private static final List<String> CACHED_CONFIG_FILES = new ArrayList<>();

	private static Map<String, FormConfig> STACK_FORMS = new HashMap<>();

	private static GeneralServerConfig SERVER_SYNCED_GENERAL_SERVER;
	private static SkillsConfig SERVER_SYNCED_SKILLS;
	private static TechniqueConfig SERVER_SYNCED_TECHNIQUES;
	private static CombatConfig SERVER_SYNCED_COMBAT;
	private static TrainingConfig SERVER_SYNCED_TRAINING;
	private static Map<String, Map<String, FormConfig>> SERVER_SYNCED_FORMS;
	private static Map<String, RaceStatsConfig> SERVER_SYNCED_STATS;
	private static Map<String, RaceCharacterConfig> SERVER_SYNCED_CHARACTER;
	private static Map<String, FormConfig> SERVER_SYNCED_STACK_FORMS;

	private static GeneralUserConfig userConfig;
	private static GeneralServerConfig serverConfig;
	private static CombatConfig combatConfig;
	private static TrainingConfig trainingConfig;
	private static SkillsConfig skillsConfig;
	private static TechniqueConfig techniqueConfig;
	@Getter
	private static EntitiesConfig entitiesConfig;

	public static void initialize() {
		LogUtil.info(Env.COMMON, "Initializing DragonMineZ configuration system...");

		try {
			Files.createDirectories(CONFIG_DIR);
			Files.createDirectories(RACES_DIR);

			loadGeneralConfigs();
			loadAllRaces();
			createOrLoadStackForms(true);

			LogUtil.info(Env.COMMON, "Configuration system initialized successfully");
			LogUtil.info(Env.COMMON, "Loaded races: {}", LOADED_RACES);
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Error initializing configuration system: {}", e.getMessage());
		}
	}

	public static void reload() {
		LogUtil.info(Env.COMMON, "Reloading DragonMineZ configuration system...");

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
			LogUtil.info(Env.COMMON, "Configuration system reloaded successfully");
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Error reloading configuration system: {}", e.getMessage());
		}
	}

	private static void backupOldConfig(Path configPath) {
		if (Files.exists(configPath)) {
			try {
				String fileName = configPath.getFileName().toString();
				if (fileName.startsWith("old_")) return;
				Path backupPath = configPath.getParent().resolve("old_" + fileName);
				Files.move(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
				LogUtil.info(Env.COMMON, "Obsolete config backed up: {}", backupPath.getFileName());
			} catch (IOException e) {
				LogUtil.error(Env.COMMON, "Failed to backup old config '{}': {}", configPath.getFileName(), e);
			}
		}
	}

	private static <T> T loadAndValidate(Path path, Class<T> clazz, Supplier<T> defaultProvider, ToIntFunction<T> versionGetter, BiConsumer<T, Integer> versionSetter, int currentVersion, String templateName) {
		boolean overwrite = false;
		String reason = "";
		T config = null;

		if (Files.exists(path)) {
			try {
				config = LOADER.loadConfig(path, clazz);
				int version = versionGetter.applyAsInt(config);
				if (version < currentVersion) {
					reason = version == 0 ? "Missing config version" : "Outdated version (" + version + " < " + currentVersion + ")";
					overwrite = true;
				}
			} catch (Exception e) {
				reason = "Parsing error: " + e.getMessage();
				overwrite = true;
			}
		} else {
			reason = "File not found";
			overwrite = true;
			if (templateName != null) {
				try {
					LOADER.saveDefaultFromTemplate(path, templateName);
					config = LOADER.loadConfig(path, clazz);
					if (versionGetter.applyAsInt(config) < currentVersion) overwrite = true;
				} catch (Exception e) {
					reason = "Template loading failed: " + e.getMessage();
				}
			}
		}

		if (overwrite) {
			if (Files.exists(path)) backupOldConfig(path);
			if (config == null || (templateName != null && reason.contains("failed"))) config = defaultProvider.get();
			try {
				versionSetter.accept(config, currentVersion);
				LogUtil.warn(Env.COMMON, String.format("Regenerating %s. Reason: %s", path.getFileName(), reason));
				LOADER.saveConfig(path, config);
			} catch (Exception e) {
				LogUtil.error(Env.COMMON, "Error saving regenerated config: " + e.getMessage());
			}
		}
		return config != null ? config : defaultProvider.get();
	}

	private static void loadGeneralConfigs() {
		userConfig = loadAndValidate(CONFIG_DIR.resolve("general-user.json"), GeneralUserConfig.class, GeneralUserConfig::new, GeneralUserConfig::getConfigVersion, GeneralUserConfig::setConfigVersion, GeneralUserConfig.CURRENT_VERSION, null);
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
		RaceStatsConfig statsConfig = loadAndValidate(racePath.resolve("stats.json"), RaceStatsConfig.class, ConfigManager::createDefaultStatsConfig, RaceStatsConfig::getConfigVersion, RaceStatsConfig::setConfigVersion, RaceStatsConfig.CURRENT_VERSION, null);

		Map<String, FormConfig> raceForms = new HashMap<>();
		if (isDefault) FORMS_FACTORY.createDefaultFormsForRace(raceName, formsPath, raceForms);
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
					if (userConfig.getConfigVersion() < FormConfig.CURRENT_VERSION) {
						LogUtil.warn(Env.COMMON, "Regenerating form '{}' for race '{}'. Reason: Outdated version", defaultEntry.getKey(), raceName);
						backupOldConfig(formFilePath);
						defaultFormConfig.setConfigVersion(FormConfig.CURRENT_VERSION);
						try { LOADER.saveConfig(formFilePath, defaultFormConfig); } catch (Exception ignored) {}
						raceForms.put(groupKey, defaultFormConfig);
					} else {
						raceForms.put(groupKey, userConfig);
					}
				} else {
					defaultFormConfig.setConfigVersion(FormConfig.CURRENT_VERSION);
					try { LOADER.saveConfig(formFilePath, defaultFormConfig); } catch (Exception ignored) {}
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
		Map<String, FormConfig> userDiskForms = LOADER.loadStackForms(STACK_FORMS_DIR);

		if (isDefault) {
			for (Map.Entry<String, FormConfig> defaultEntry : finalStackForms.entrySet()) {
				String groupKey = defaultEntry.getKey().toLowerCase();
				FormConfig defaultFormConfig = defaultEntry.getValue();
				Path formFilePath = STACK_FORMS_DIR.resolve(defaultEntry.getKey() + ".json");

				if (userDiskForms.containsKey(groupKey)) {
					FormConfig userConfig = userDiskForms.get(groupKey);
					if (userConfig.getConfigVersion() < FormConfig.CURRENT_VERSION) {
						LogUtil.warn(Env.COMMON, "Regenerating stack form '{}'. Reason: Outdated version", defaultEntry.getKey());
						backupOldConfig(formFilePath);
						defaultFormConfig.setConfigVersion(FormConfig.CURRENT_VERSION);
						try { LOADER.saveConfig(formFilePath, defaultFormConfig); } catch (Exception ignored) {}
					} else {
						finalStackForms.put(groupKey, userConfig);
					}
				} else {
					defaultFormConfig.setConfigVersion(FormConfig.CURRENT_VERSION);
					try { LOADER.saveConfig(formFilePath, defaultFormConfig); } catch (Exception ignored) {}
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
		EntitiesConfig.HardModeSettings hardMode = config.getHardModeSettings();
		Map<String, EntitiesConfig.EntityStats> statsMap = config.getDefaultEntityStats();
		hardMode.setHpMultiplier(3.0);
		hardMode.setDamageMultiplier(2.0);

		addDefaultEntityStats(statsMap, MainEntities.DINO_KID, 30.0, 4.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.DINOSAUR1, 100.0, 8.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.DINOSAUR2, 150.0, 12.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.DINOSAUR3, 75.0, 10.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.SABERTOOTH, 30.0, 5.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.BANDIT, 75.0, 10.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_SOLDIER, 40.0, 5.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_ROBOT1, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_ROBOT2, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.RED_RIBBON_ROBOT3, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, MainEntities.MINI_BUU, 60.0, 8.0, 6.0);

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
		config.setRaceName(raceName);
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
				default -> setupDefaultCharacter(config);
			}
		} else {
			config.setHasGender(true);
			setupDefaultCharacter(config);
		}

		return config;
	}

	private static void setupHumanCharacter(RaceCharacterConfig config) {
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
		config.setFormSkillTpCosts("superforms", new Integer[]{8000, 16000, 25000, 40000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
		config.setFormSkillTpCosts("androidforms", new Integer[]{16000, 40000});
	}

	private static void setupSaiyanCharacter(RaceCharacterConfig config) {
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
		config.setFormSkillTpCosts("superforms", new Integer[]{5000, 8000, 12000, 16000, 20000, 25000, 30000, 40000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupNamekianCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("namekian");
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
		config.setDefaultHairColor("#1FAA24");
		config.setDefaultEye1Color("#222629");
		config.setDefaultEye2Color("#222629");
		config.setDefaultAuraColor("#7FFF00");
		config.setFormSkillTpCosts("superforms", new Integer[]{9000, 18000, 30000, 45000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupFrostDemonCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("frostdemon");
		config.setHeadBones(new String[]{"horns1"});
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
		config.setFormSkillTpCosts("superforms", new Integer[]{7000, 12000, 20000, 32000, 45000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupBioAndroidCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("bioandroid");
		config.setDefaultModelScaling(new Float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(0);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#187600");
		config.setDefaultBodyColor2("#9FE321");
		config.setDefaultBodyColor3("#FF7600");
		config.setDefaultHairColor("#187600");
		config.setDefaultEye1Color("#2E2424");
		config.setDefaultEye2Color("#F06F6E");
		config.setDefaultAuraColor("#1AA700");
		config.setFormSkillTpCosts("superforms", new Integer[]{10000, 22000, 34000, 48000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupMajinCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("majin");
		config.setHeadBones(new String[]{"majin1"});
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
		config.setFormSkillTpCosts("superforms", new Integer[]{9000, 18000, 30000, 44000});
		config.setFormSkillTpCosts("godforms", new Integer[]{});
		config.setFormSkillTpCosts("legendaryforms", new Integer[]{-1, -1, -1});
	}

	private static void setupDefaultCharacter(RaceCharacterConfig config) {
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

	private static RaceStatsConfig createDefaultStatsConfig() {
		RaceStatsConfig config = new RaceStatsConfig();

		setupInitialStats(config.getClassStats("warrior"), 15, 5, 10, 10, 5, 5, 7.0, 0.08, 4.0, 0.04, 12.0, 0.12);
		setupScalingStats(config.getClassStats("warrior"), 1.6, 1.0, 2.0, 1.6, 1.8, 0.5, 1.5);

		setupInitialStats(config.getClassStats("spiritualist"), 5, 5, 5, 5, 15, 15, 2.0, 0.02, 8.0, 0.10, 5.0, 0.05);
		setupScalingStats(config.getClassStats("spiritualist"), 0.5, 0.5, 1.3, 0.7, 1.4, 1.9, 3.7);

		setupInitialStats(config.getClassStats("martialartist"), 5, 15, 5, 15, 5, 5, 6.0, 0.07, 4.0, 0.04, 9.0, 0.09);
		setupScalingStats(config.getClassStats("martialartist"), 1.0, 1.8, 1.5, 1.3, 2.2, 0.6, 1.6);

		setupInitialStats(config.getClassStats("berserker"), 15, 5, 5, 15, 5, 5, 4.0, 0.05, 2.0, 0.02, 14.0, 0.13);
		setupScalingStats(config.getClassStats("berserker"), 1.9, 0.8, 1.5, 1.1, 3.0, 0.4, 1.3);

		setupInitialStats(config.getClassStats("paladin"), 5, 10, 15, 10, 5, 5, 8.0, 0.09, 4.0, 0.04, 8.0, 0.08);
		setupScalingStats(config.getClassStats("paladin"), 1.0, 1.2, 2.8, 1.2, 2.0, 0.6, 1.2);

		setupInitialStats(config.getClassStats("tank"), 5, 5, 15, 15, 5, 5, 9.0, 0.10, 5.0, 0.05, 9.0, 0.09);
		setupScalingStats(config.getClassStats("tank"), 0.8, 0.7, 3.2, 1.5, 2.5, 0.5, 0.8);
		config.getClassStats("tank").setTpGainMultiplier(1.25);

		setupInitialStats(config.getClassStats("enchanter"), 5, 5, 10, 5, 5, 20, 2.0, 0.02, 12.0, 0.12, 16.0, 0.12);
		setupScalingStats(config.getClassStats("enchanter"), 0.5, 0.5, 1.4, 2.6, 1.2, 0.8, 3.0);
		config.getClassStats("enchanter").setTpGainMultiplier(1.25);
		config.getClassStats("enchanter").setTpCostMultiplier(0.9);
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
		setupPassive(config.getClassStats("warrior"), warrior);

		Map<String, Double> martial = new HashMap<>();
		martial.put("maxBonus", 0.25);
		martial.put("hpHigh", 0.75);
		martial.put("hpLow", 0.25);
		setupPassive(config.getClassStats("martialartist"), martial);

		Map<String, Double> spiritualist = new HashMap<>();
		spiritualist.put("cdPrimary", 0.20);
		spiritualist.put("cdSecondary", 0.15);
		spiritualist.put("durationBonus", 0.25);
		setupPassive(config.getClassStats("spiritualist"), spiritualist);

		Map<String, Double> berserker = new HashMap<>();
		berserker.put("hpThreshHigh", 0.66);
		berserker.put("hpThreshLow", 0.33);
		berserker.put("hpRegenHigh", 0.25);
		berserker.put("critHigh", 0.10);
		berserker.put("hpRegenLow", 0.75);
		berserker.put("critLow", 0.25);
		setupPassive(config.getClassStats("berserker"), berserker);

		Map<String, Double> paladin = new HashMap<>();
		paladin.put("redirectPct", 0.15);
		paladin.put("lifestealPct", 0.15);
		setupPassive(config.getClassStats("paladin"), paladin);

		Map<String, Double> tank = new HashMap<>();
		tank.put("stmToHpRegenRatio", 0.5);
		tank.put("healingBonus", 0.25);
		tank.put("lowHpThreshold", 0.30);
		tank.put("lowHpMultiplier", 2.0);
		setupPassive(config.getClassStats("tank"), tank);

		Map<String, Double> enchanter = new HashMap<>();
		enchanter.put("cdPrimary", 0.20);
		enchanter.put("cdSecondary", 0.15);
		enchanter.put("durationBonus", 0.25);
		setupPassive(config.getClassStats("enchanter"), enchanter);
	}

	private static void setupPassive(RaceStatsConfig.ClassStats classStats, Map<String, Double> values) {
		RaceStatsConfig.Passive passive = classStats.getPassive();
		passive.setEnabled(true);
		passive.setValues(values);
	}

	private static void setupInitialStats(RaceStatsConfig.ClassStats classStats, int str, int skp, int res, int vit, int pwr, int ene, double baseHp5, double hp5VitScaling, double baseEp5, double ep5EneScaling, double baseSp5, double sp5StmScaling) {
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
		classStats.setSp5StmScaling(sp5StmScaling);
	}

	private static void setupScalingStats(RaceStatsConfig.ClassStats classStats, double strScale, double skpScale, double defScale, double stmScale, double vitScale, double pwrScale, double eneScale) {
		RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
		scaling.setStrengthScaling(strScale);
		scaling.setStrikePowerScaling(skpScale);
		scaling.setDefenseScaling(defScale);
		scaling.setStaminaScaling(stmScale);
		scaling.setVitalityScaling(vitScale);
		scaling.setKiPowerScaling(pwrScale);
		scaling.setEnergyScaling(eneScale);
	}

	public static RaceStatsConfig getRaceStats(String raceName) {
		String key = raceName != null ? raceName.toLowerCase() : "human";
		if (SERVER_SYNCED_STATS != null && SERVER_SYNCED_STATS.containsKey(key)) return SERVER_SYNCED_STATS.get(key);
		RaceStatsConfig config = RACE_STATS.getOrDefault(key, RACE_STATS.get("human"));
		return config != null ? config : createDefaultStatsConfig();
	}

	public static RaceCharacterConfig getRaceCharacter(String raceName) {
		String key = raceName != null ? raceName.toLowerCase() : "human";
		if (SERVER_SYNCED_CHARACTER != null && SERVER_SYNCED_CHARACTER.containsKey(key)) return SERVER_SYNCED_CHARACTER.get(key);
		RaceCharacterConfig config = RACE_CHARACTER.getOrDefault(key, RACE_CHARACTER.get("human"));
		return config != null ? config : createDefaultCharacterConfig(key, false);
	}

	public static List<String> getLoadedRaces() {
		List<String> races;
		if (SERVER_SYNCED_CHARACTER != null) races = new ArrayList<>(SERVER_SYNCED_CHARACTER.keySet());
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
		if (SERVER_SYNCED_CHARACTER != null) return SERVER_SYNCED_CHARACTER.containsKey(raceName.toLowerCase());
		return LOADED_RACES.stream().anyMatch(r -> r.equalsIgnoreCase(raceName));
	}
	public static GeneralUserConfig getUserConfig() { return userConfig != null ? userConfig : new GeneralUserConfig(); }
	public static GeneralServerConfig getServerConfig() {
		if (SERVER_SYNCED_GENERAL_SERVER != null) return SERVER_SYNCED_GENERAL_SERVER;
		return serverConfig != null ? serverConfig : new GeneralServerConfig();
	}
	public static CombatConfig getCombatConfig() {
		if (SERVER_SYNCED_COMBAT != null) return SERVER_SYNCED_COMBAT;
		return combatConfig != null ? combatConfig : new CombatConfig();
	}
	public static TrainingConfig getTrainingConfig() {
		if (SERVER_SYNCED_TRAINING != null) return SERVER_SYNCED_TRAINING;
		return trainingConfig != null ? trainingConfig : new TrainingConfig();
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
				RACE_FORMS.computeIfAbsent(raceName.toLowerCase(), k -> new HashMap<>())
						.put(parts[3].toLowerCase(), LOADER.loadConfig(path, FormConfig.class));
			}
		} else if (configFilePath.startsWith("forms/")) {
			STACK_FORMS.put(configFilePath.split("/")[1].toLowerCase(), LOADER.loadConfig(path, FormConfig.class));
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
			LogUtil.error(Env.COMMON, "Could not read config for sync: " + configFilePath);
			return null;
		}
	}

	public static void applySpecificSyncedConfig(String configFilePath, String json) {
		try {
			if (configFilePath.equals("general-server")) SERVER_SYNCED_GENERAL_SERVER = GSON.fromJson(json, GeneralServerConfig.class);
			else if (configFilePath.equals("combat")) SERVER_SYNCED_COMBAT = GSON.fromJson(json, CombatConfig.class);
			else if (configFilePath.equals("training")) SERVER_SYNCED_TRAINING = GSON.fromJson(json, TrainingConfig.class);
			else if (configFilePath.equals("skills")) SERVER_SYNCED_SKILLS = GSON.fromJson(json, SkillsConfig.class);
			else if (configFilePath.equals("techniques")) SERVER_SYNCED_TECHNIQUES = GSON.fromJson(json, TechniqueConfig.class);
			else if (configFilePath.startsWith("races/")) {
				String[] parts = configFilePath.split("/");
				String raceName = parts[1];
				if (parts[2].equals("stats")) {
					if (SERVER_SYNCED_STATS == null) SERVER_SYNCED_STATS = new HashMap<>(RACE_STATS);
					SERVER_SYNCED_STATS.put(raceName.toLowerCase(), GSON.fromJson(json, RaceStatsConfig.class));
				} else if (parts[2].equals("character")) {
					if (SERVER_SYNCED_CHARACTER == null) SERVER_SYNCED_CHARACTER = new HashMap<>(RACE_CHARACTER);
					SERVER_SYNCED_CHARACTER.put(raceName.toLowerCase(), GSON.fromJson(json, RaceCharacterConfig.class));
				} else if (parts[2].equals("forms")) {
					if (SERVER_SYNCED_FORMS == null) SERVER_SYNCED_FORMS = new HashMap<>();
					SERVER_SYNCED_FORMS.computeIfAbsent(raceName.toLowerCase(), k -> new HashMap<>())
							.put(parts[3].toLowerCase(), GSON.fromJson(json, FormConfig.class));
				}
			} else if (configFilePath.startsWith("forms/")) {
				if (SERVER_SYNCED_STACK_FORMS == null) SERVER_SYNCED_STACK_FORMS = new HashMap<>(STACK_FORMS);
				SERVER_SYNCED_STACK_FORMS.put(configFilePath.split("/")[1].toLowerCase(), GSON.fromJson(json, FormConfig.class));
			}
		} catch (Exception e) { LogUtil.error(Env.CLIENT, "Error applying synced config: " + e.getMessage()); }
	}

	public static void applySyncedServerConfig(GeneralServerConfig syncedServerConfig, CombatConfig syncedCombatConfig, SkillsConfig syncedSkillsConfig, Map<String, Map<String, FormConfig>> syncedForms, Map<String, RaceStatsConfig> syncedStats, Map<String, RaceCharacterConfig> syncedCharacters, Map<String, FormConfig> syncedStackForms) {
		SERVER_SYNCED_GENERAL_SERVER = syncedServerConfig;
		SERVER_SYNCED_COMBAT = syncedCombatConfig;
		SERVER_SYNCED_SKILLS = syncedSkillsConfig;
		SERVER_SYNCED_FORMS = syncedForms;
		SERVER_SYNCED_STATS = syncedStats;
		SERVER_SYNCED_CHARACTER = syncedCharacters;
		SERVER_SYNCED_STACK_FORMS = syncedStackForms;
	}

	public static void clearServerSync() {
		SERVER_SYNCED_GENERAL_SERVER = null;
		SERVER_SYNCED_COMBAT = null;
		SERVER_SYNCED_TRAINING = null;
		SERVER_SYNCED_SKILLS = null;
		SERVER_SYNCED_TECHNIQUES = null;
		SERVER_SYNCED_FORMS = null;
		SERVER_SYNCED_STATS = null;
		SERVER_SYNCED_CHARACTER = null;
		SERVER_SYNCED_STACK_FORMS = null;
	}

	public static Map<String, RaceStatsConfig> getAllRaceStats() { return SERVER_SYNCED_STATS != null ? SERVER_SYNCED_STATS : new HashMap<>(RACE_STATS); }
	public static Map<String, RaceCharacterConfig> getAllRaceCharacters() { return SERVER_SYNCED_CHARACTER != null ? SERVER_SYNCED_CHARACTER : new HashMap<>(RACE_CHARACTER); }
	public static Map<String, Map<String, FormConfig>> getAllForms() { return SERVER_SYNCED_FORMS != null ? SERVER_SYNCED_FORMS : RACE_FORMS; }
	public static Map<String, FormConfig> getAllFormsForRace(String raceName) { return getAllForms().getOrDefault(raceName.toLowerCase(), new HashMap<>()); }
	public static FormConfig getFormGroup(String raceName, String groupName) {
		Map<String, FormConfig> raceForms = getAllFormsForRace(raceName);
		return raceForms != null ? raceForms.get(groupName.toLowerCase()) : null;
	}
	public static FormConfig.FormData getForm(String raceName, String groupName, String formName) {
		FormConfig group = getFormGroup(raceName, groupName);
		return group != null ? group.getForm(formName) : null;
	}
	public static Map<String, FormConfig> getAllStackForms() { return SERVER_SYNCED_STACK_FORMS != null ? SERVER_SYNCED_STACK_FORMS : STACK_FORMS; }
	public static FormConfig getStackFormGroup(String groupName) {
		Map<String, FormConfig> stackForms = getAllStackForms();
		return stackForms != null ? stackForms.get(groupName.toLowerCase()) : null;
	}
	public static FormConfig.FormData getStackForm(String groupName, String formName) {
		FormConfig group = getStackFormGroup(groupName);
		return group != null ? group.getForm(formName) : null;
	}
	public static SkillsConfig getSkillsConfig() { return SERVER_SYNCED_SKILLS != null ? SERVER_SYNCED_SKILLS : (skillsConfig != null ? skillsConfig : new SkillsConfig()); }
	public static TechniqueConfig getTechniqueConfig() { return SERVER_SYNCED_TECHNIQUES != null ? SERVER_SYNCED_TECHNIQUES : (techniqueConfig != null ? techniqueConfig : new TechniqueConfig()); }
	public static EntitiesConfig.EntityStats getEntityStats(String registryName) { return entitiesConfig != null && entitiesConfig.getDefaultEntityStats() != null ? entitiesConfig.getDefaultEntityStats().get(registryName) : null; }
}