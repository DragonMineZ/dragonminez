package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.MainEntities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
    private static final ConfigLoader LOADER = new ConfigLoader(GSON);
    private static final DefaultFormsFactory FORMS_FACTORY = new DefaultFormsFactory(GSON, LOADER);

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
	private static final Path STACK_FORMS_DIR = CONFIG_DIR.resolve("forms");
	private static final Path RACES_DIR = CONFIG_DIR.resolve("races");
    private static final String[] DEFAULT_RACES = {"human", "saiyan", "namekian", "frostdemon", "bioandroid", "majin"};
    private static final Set<String> RACES_WITH_GENDER = new HashSet<>(Arrays.asList("human", "saiyan", "majin"));

    private static final Map<String, RaceStatsConfig> RACE_STATS = new HashMap<>();
    private static final Map<String, RaceCharacterConfig> RACE_CHARACTER = new HashMap<>();
    private static final Map<String, Map<String, FormConfig>> RACE_FORMS = new HashMap<>();
    private static final List<String> LOADED_RACES = new ArrayList<>();

	private static Map<String, FormConfig> STACK_FORMS = new HashMap<>();

    private static GeneralServerConfig SERVER_SYNCED_GENERAL_SERVER;
    private static SkillsConfig SERVER_SYNCED_SKILLS;
    private static Map<String, Map<String, FormConfig>> SERVER_SYNCED_FORMS;
    private static Map<String, RaceStatsConfig> SERVER_SYNCED_STATS;
    private static Map<String, RaceCharacterConfig> SERVER_SYNCED_CHARACTER;

    private static GeneralUserConfig userConfig;
    private static GeneralServerConfig serverConfig;
    private static SkillsConfig skillsConfig;
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

			loadGeneralConfigs();
			loadAllRaces();
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

	private static boolean isMissingConfigVersion(Path path) {
		if (!Files.exists(path)) return false;
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			return !json.has("configVersion");
		} catch (Exception e) {
			return false;
		}
	}

	private static void loadGeneralConfigs() throws IOException {
		// General User
		Path userConfigPath = CONFIG_DIR.resolve("general-user.json");
		boolean overwriteUser = false;
		if (Files.exists(userConfigPath)) {
			try {
				userConfig = LOADER.loadConfig(userConfigPath, GeneralUserConfig.class);
				if (userConfig.getConfigVersion() < GeneralUserConfig.CURRENT_VERSION || isMissingConfigVersion(userConfigPath)) {
					backupOldConfig(userConfigPath);
					userConfig = new GeneralUserConfig();
					userConfig.setConfigVersion(GeneralUserConfig.CURRENT_VERSION);
					overwriteUser = true;
				}
			} catch (Exception e) {
				backupOldConfig(userConfigPath);
				userConfig = new GeneralUserConfig();
				userConfig.setConfigVersion(GeneralUserConfig.CURRENT_VERSION);
				overwriteUser = true;
			}
		} else {
			userConfig = new GeneralUserConfig();
			userConfig.setConfigVersion(GeneralUserConfig.CURRENT_VERSION);
			overwriteUser = true;
		}
		if (overwriteUser) LOADER.saveConfig(userConfigPath, userConfig);

		// General Server
		Path serverConfigPath = CONFIG_DIR.resolve("general-server.json");
		boolean overwriteServer = false;
		if (Files.exists(serverConfigPath)) {
			try {
				serverConfig = LOADER.loadConfig(serverConfigPath, GeneralServerConfig.class);
				if (serverConfig.getConfigVersion() < GeneralServerConfig.CURRENT_VERSION || isMissingConfigVersion(serverConfigPath)) {
					backupOldConfig(serverConfigPath);
					serverConfig = new GeneralServerConfig();
					serverConfig.setConfigVersion(GeneralServerConfig.CURRENT_VERSION);
					overwriteServer = true;
				}
			} catch (Exception e) {
				backupOldConfig(serverConfigPath);
				serverConfig = new GeneralServerConfig();
				serverConfig.setConfigVersion(GeneralServerConfig.CURRENT_VERSION);
				overwriteServer = true;
			}
		} else {
			try {
				LOADER.saveDefaultFromTemplate(serverConfigPath, "general-server.json");
				serverConfig = LOADER.loadConfig(serverConfigPath, GeneralServerConfig.class);
				if (serverConfig.getConfigVersion() < GeneralServerConfig.CURRENT_VERSION || isMissingConfigVersion(serverConfigPath)) {
					serverConfig.setConfigVersion(GeneralServerConfig.CURRENT_VERSION);
					overwriteServer = true;
				}
			} catch (Exception e) {
				serverConfig = new GeneralServerConfig();
				serverConfig.setConfigVersion(GeneralServerConfig.CURRENT_VERSION);
				overwriteServer = true;
			}
		}
		if (overwriteServer) LOADER.saveConfig(serverConfigPath, serverConfig);

		// Skills
		Path skillsConfigPath = CONFIG_DIR.resolve("skills.json");
		boolean overwriteSkills = false;
		if (Files.exists(skillsConfigPath)) {
			try {
				skillsConfig = LOADER.loadConfig(skillsConfigPath, SkillsConfig.class);
				if (skillsConfig.getConfigVersion() < SkillsConfig.CURRENT_VERSION || isMissingConfigVersion(skillsConfigPath)) {
					backupOldConfig(skillsConfigPath);
					skillsConfig = new SkillsConfig();
					skillsConfig.setConfigVersion(SkillsConfig.CURRENT_VERSION);
					overwriteSkills = true;
				}
			} catch (Exception e) {
				backupOldConfig(skillsConfigPath);
				skillsConfig = new SkillsConfig();
				skillsConfig.setConfigVersion(SkillsConfig.CURRENT_VERSION);
				overwriteSkills = true;
			}
		} else {
			try {
				LOADER.saveDefaultFromTemplate(skillsConfigPath, "skills.json");
				skillsConfig = LOADER.loadConfig(skillsConfigPath, SkillsConfig.class);
				if (skillsConfig.getConfigVersion() < SkillsConfig.CURRENT_VERSION || isMissingConfigVersion(skillsConfigPath)) {
					skillsConfig.setConfigVersion(SkillsConfig.CURRENT_VERSION);
					overwriteSkills = true;
				}
			} catch (Exception e) {
				skillsConfig = new SkillsConfig();
				skillsConfig.setConfigVersion(SkillsConfig.CURRENT_VERSION);
				overwriteSkills = true;
			}
		}
		if (overwriteSkills) LOADER.saveConfig(skillsConfigPath, skillsConfig);

		// Entities
		Path entitiesConfigPath = CONFIG_DIR.resolve("entities.json");
		boolean overwriteEntities = false;
		if (Files.exists(entitiesConfigPath)) {
			try {
				entitiesConfig = LOADER.loadConfig(entitiesConfigPath, EntitiesConfig.class);
				if (entitiesConfig.getConfigVersion() < EntitiesConfig.CURRENT_VERSION || isMissingConfigVersion(entitiesConfigPath)) {
					backupOldConfig(entitiesConfigPath);
					entitiesConfig = createDefaultEntitiesConfig();
					entitiesConfig.setConfigVersion(EntitiesConfig.CURRENT_VERSION);
					overwriteEntities = true;
				}
			} catch (Exception e) {
				backupOldConfig(entitiesConfigPath);
				entitiesConfig = createDefaultEntitiesConfig();
				entitiesConfig.setConfigVersion(EntitiesConfig.CURRENT_VERSION);
				overwriteEntities = true;
			}
		} else {
			entitiesConfig = createDefaultEntitiesConfig();
			entitiesConfig.setConfigVersion(EntitiesConfig.CURRENT_VERSION);
			overwriteEntities = true;
		}
		if (overwriteEntities) LOADER.saveConfig(entitiesConfigPath, entitiesConfig);
	}

	private static void createOrLoadRace(String raceName, boolean isDefault) throws IOException {
		Path racePath = RACES_DIR.resolve(raceName);
		Files.createDirectories(racePath);

		Path characterPath = racePath.resolve("character.json");
		Path statsPath = racePath.resolve("stats.json");
		Path formsPath = racePath.resolve("forms");
		Files.createDirectories(formsPath);

		// Character Config
		RaceCharacterConfig characterConfig;
		boolean overwriteCharacter = false;
		if (Files.exists(characterPath)) {
			try {
				characterConfig = LOADER.loadConfig(characterPath, RaceCharacterConfig.class);
				if (characterConfig.getConfigVersion() < RaceCharacterConfig.CURRENT_VERSION || characterConfig.getConfigVersion() == 0 || isMissingConfigVersion(characterPath)) {
					backupOldConfig(characterPath);
					characterConfig = createDefaultCharacterConfig(raceName, isDefault);
					characterConfig.setConfigVersion(RaceCharacterConfig.CURRENT_VERSION);
					overwriteCharacter = true;
				}
			} catch (Exception e) {
				backupOldConfig(characterPath);
				characterConfig = createDefaultCharacterConfig(raceName, isDefault);
				characterConfig.setConfigVersion(RaceCharacterConfig.CURRENT_VERSION);
				overwriteCharacter = true;
			}
		} else {
			characterConfig = createDefaultCharacterConfig(raceName, isDefault);
			characterConfig.setConfigVersion(RaceCharacterConfig.CURRENT_VERSION);
			overwriteCharacter = true;
		}
		if (overwriteCharacter) LOADER.saveConfig(characterPath, characterConfig);

		// Stats Config
		RaceStatsConfig statsConfig;
		boolean overwriteStats = false;
		if (Files.exists(statsPath)) {
			try {
				statsConfig = LOADER.loadConfig(statsPath, RaceStatsConfig.class);
				if (statsConfig.getConfigVersion() < RaceStatsConfig.CURRENT_VERSION || statsConfig.getConfigVersion() == 0 || isMissingConfigVersion(statsPath)) {
					backupOldConfig(statsPath);
					statsConfig = createDefaultStatsConfig();
					statsConfig.setConfigVersion(RaceStatsConfig.CURRENT_VERSION);
					overwriteStats = true;
				}
			} catch (Exception e) {
				backupOldConfig(statsPath);
				statsConfig = createDefaultStatsConfig();
				statsConfig.setConfigVersion(RaceStatsConfig.CURRENT_VERSION);
				overwriteStats = true;
			}
		} else {
			statsConfig = createDefaultStatsConfig();
			statsConfig.setConfigVersion(RaceStatsConfig.CURRENT_VERSION);
			overwriteStats = true;
		}
		if (overwriteStats) LOADER.saveConfig(statsPath, statsConfig);

		// Forms Config
		Map<String, FormConfig> raceForms = LOADER.loadRaceForms(raceName, formsPath);
		boolean recreateForms = false;

		if (isDefault && !LOADER.hasExistingFiles(formsPath)) {
			recreateForms = true;
		} else if (!raceForms.isEmpty()) {
			for (Map.Entry<String, FormConfig> entry : raceForms.entrySet()) {
				Path formFilePath = formsPath.resolve(entry.getKey() + ".json");
				FormConfig formGroup = entry.getValue();
				if (formGroup.getConfigVersion() < FormConfig.CURRENT_VERSION || formGroup.getConfigVersion() == 0 || isMissingConfigVersion(formFilePath)) {
					recreateForms = true;
					break;
				}
			}
		}

		if (recreateForms && isDefault) {
			try (var stream = Files.list(formsPath)) {
				stream.filter(path -> path.toString().endsWith(".json")).forEach(ConfigManager::backupOldConfig);
			}
			raceForms.clear();
			FORMS_FACTORY.createDefaultFormsForRace(raceName, formsPath, raceForms);

			for (Map.Entry<String, FormConfig> entry : raceForms.entrySet()) {
				entry.getValue().setConfigVersion(FormConfig.CURRENT_VERSION);
				Path formFilePath = formsPath.resolve(entry.getKey() + ".json");
				LOADER.saveConfig(formFilePath, entry.getValue());
			}
		} else {
			for (Map.Entry<String, FormConfig> entry : raceForms.entrySet()) {
				Path formFilePath = formsPath.resolve(entry.getKey() + ".json");
				LOADER.saveConfig(formFilePath, entry.getValue());
			}
		}

		RACE_FORMS.put(raceName.toLowerCase(), raceForms);
		RACE_CHARACTER.put(raceName.toLowerCase(), characterConfig);
		RACE_STATS.put(raceName.toLowerCase(), statsConfig);
		LOADED_RACES.add(raceName);
	}

	private static void createOrLoadStackForms(boolean isDefault) throws IOException {
		Files.createDirectories(STACK_FORMS_DIR);

		Map<String, FormConfig> stackForms = LOADER.loadStackForms(STACK_FORMS_DIR);
		boolean recreateForms = false;

		if (isDefault && !LOADER.hasExistingFiles(STACK_FORMS_DIR)) {
			recreateForms = true;
		} else if (!stackForms.isEmpty()) {
			for (Map.Entry<String, FormConfig> entry : stackForms.entrySet()) {
				Path formFilePath = STACK_FORMS_DIR.resolve(entry.getKey() + ".json");
				FormConfig formGroup = entry.getValue();
				if (formGroup.getConfigVersion() < FormConfig.CURRENT_VERSION || formGroup.getConfigVersion() == 0 || isMissingConfigVersion(formFilePath)) {
					recreateForms = true;
					break;
				}
			}
		}

		if (recreateForms && isDefault) {
			try (var stream = Files.list(STACK_FORMS_DIR)) {
				stream.filter(path -> path.toString().endsWith(".json")).forEach(ConfigManager::backupOldConfig);
			}
			stackForms.clear();
			FORMS_FACTORY.createDefaultStackForms(STACK_FORMS_DIR, stackForms);

			for (Map.Entry<String, FormConfig> entry : stackForms.entrySet()) {
				entry.getValue().setConfigVersion(FormConfig.CURRENT_VERSION);
				Path formFilePath = STACK_FORMS_DIR.resolve(entry.getKey() + ".json");
				LOADER.saveConfig(formFilePath, entry.getValue());
			}
		} else {
			for (Map.Entry<String, FormConfig> entry : stackForms.entrySet()) {
				entry.getValue().setConfigVersion(FormConfig.CURRENT_VERSION);
				Path formFilePath = STACK_FORMS_DIR.resolve(entry.getKey() + ".json");
				LOADER.saveConfig(formFilePath, entry.getValue());
			}
		}

		STACK_FORMS = stackForms;
	}

	private static EntitiesConfig createDefaultEntitiesConfig() {
		EntitiesConfig config = new EntitiesConfig();
		EntitiesConfig.HardModeSettings hardMode = config.getHardModeSettings();
		Map<String, Map<String, EntitiesConfig.EntityStats>> statsMap = config.getSagaEntityStats();hardMode.setHpMultiplier(3.0);
		hardMode.setDamageMultiplier(2.0);

		//OPEN WORLD
		addDefaultEntityStats(statsMap, "default", MainEntities.DINO_KID, 30.0, 4.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.DINOSAUR1, 100.0, 8.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.DINOSAUR2, 150.0, 12.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.DINOSAUR3, 75.0, 10.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.SABERTOOTH, 30.0, 5.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.BANDIT, 75.0, 10.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.RED_RIBBON_SOLDIER, 40.0, 5.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.RED_RIBBON_ROBOT1, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.RED_RIBBON_ROBOT2, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.RED_RIBBON_ROBOT3, 120.0, 15.0, 0.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.SAGA_SAIBAMAN, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.SAGA_SAIBAMAN2, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.SAGA_SAIBAMAN3, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.SAGA_SAIBAMAN4, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.SAGA_SAIBAMAN5, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "default", MainEntities.SAGA_SAIBAMAN6, 400.0, 25.0, 50.0);

		//SAIYAN SAGA
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_RADITZ, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_SAIBAMAN, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_SAIBAMAN2, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_SAIBAMAN3, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_SAIBAMAN4, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_SAIBAMAN5, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_SAIBAMAN6, 400.0, 25.0, 50.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_NAPPA, 750.0, 45.0, 100.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_VEGETA, 1200.0, 70.0, 150.0);
		addDefaultEntityStats(statsMap, "saiyan_saga", MainEntities.SAGA_OZARU_VEGETA, 2500.0, 140.0, 200.0);

		//FRIEZA SAGA
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FRIEZA_SOLDIER, 200.0, 15.0, 20.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FRIEZA_SOLDIER2, 200.0, 15.0, 20.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FRIEZA_SOLDIER3, 200.0, 15.0, 20.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_MORO_SOLDIER, 200.0, 15.0, 20.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_CUI, 1200.0, 70.0, 150.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_DODORIA, 1400.0, 80.0, 180.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_ZARBON, 1500.0, 85.0, 200.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_VEGETA_NAMEK, 1600.0, 90.0, 200.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_ZARBON_TRANSF, 1800.0, 100.0, 200.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_GULDO, 800.0, 50.0, 90.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_RECOOME, 2000.0, 110.0, 180.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_BURTER, 2000.0, 110.0, 180.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_JEICE, 2000.0, 110.0, 180.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_GINYU, 3000.0, 160.0, 260.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_GINYU_GOKU, 1500.0, 85.0, 140.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FREEZER_FIRST, 4000.0, 200.0, 350.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FREEZER_SECOND, 6000.0, 300.0, 500.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FREEZER_THIRD, 8000.0, 400.0, 650.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FREEZER_BASE, 12000.0, 550.0, 900.0);
		addDefaultEntityStats(statsMap, "frieza_saga", MainEntities.SAGA_FREEZER_FP, 16000.0, 750.0, 1200.0);

		//ANDROID SAGA
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_MECHA_FRIEZA, 17000.0, 800.0, 1300.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_KING_COLD, 8000.0, 400.0, 650.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_GOKU_YARDRAT, 20000.0, 900.0, 1500.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_DRGERO, 18000.0, 850.0, 1350.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_A19, 22000.0, 1000.0, 1600.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_A17, 30000.0, 1400.0, 2200.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_A18, 30000.0, 1400.0, 2200.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_A16, 32000.0, 1500.0, 2400.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_PICCOLO_KAMI, 35000.0, 1600.0, 2600.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_SUPER_VEGETA, 45000.0, 2100.0, 3400.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_TRUNKS_SSJ, 42000.0, 1950.0, 3200.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_GOHAN_SSJ, 55000.0, 2500.0, 4000.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_CELL_IMPERFECT, 28000.0, 1300.0, 2000.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_CELL_SEMIPERFECT, 40000.0, 1800.0, 3000.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_CELL_PERFECT, 60000.0, 2800.0, 4500.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_CELL_SUPERPERFECT, 80000.0, 3800.0, 6000.0);
		addDefaultEntityStats(statsMap, "android_saga", MainEntities.SAGA_CELL_JR, 42500.0, 1950.0, 3800.0);

		return config;
	}

	private static void addDefaultEntityStats(Map<String, Map<String, EntitiesConfig.EntityStats>> map, String saga, RegistryObject<? extends EntityType<?>> entityType, double health, double meleeDamage, double kiDamage) {
		EntitiesConfig.EntityStats stats = new EntitiesConfig.EntityStats();
		stats.setHealth(health);
		stats.setMeleeDamage(meleeDamage);
		stats.setKiDamage(kiDamage);
		map.computeIfAbsent(saga, k -> new HashMap<>()).put(entityType.getKey().location().toString(), stats);
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
		config.setDefaultModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
        config.setDefaultBodyType(0);
        config.setDefaultHairType(1);
		config.setCanUseHair(true);
        config.setDefaultEyesType(0);
        config.setDefaultNoseType(0);
        config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
        config.setDefaultBodyColor("#FFD3C9");
        config.setDefaultBodyColor2("#FFD3C9");
        config.setDefaultBodyColor3("#FFD3C9");
        config.setDefaultHairColor("#0E1011");
        config.setDefaultEye1Color("#0E1011");
        config.setDefaultEye2Color("#0E1011");
        config.setDefaultAuraColor("#7FFFFF");

		config.setFormSkillTpCosts("superform", new Integer[]{20000, 80000, 120000, 160000});
		config.setFormSkillTpCosts("godform", new Integer[]{});
		config.setFormSkillTpCosts( "legendaryforms", new Integer[]{});
		config.setFormSkillTpCosts("androidforms" , new Integer[]{80000, 120000});
    }

    private static void setupSaiyanCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("saiyan");
		config.setDefaultModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
        config.setDefaultBodyType(0);
        config.setDefaultHairType(1);
		config.setCanUseHair(true);
        config.setDefaultEyesType(0);
        config.setDefaultNoseType(0);
        config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
        config.setDefaultBodyColor("#FFD3C9");
        config.setDefaultBodyColor2("#FFD3C9");
        config.setDefaultBodyColor3("#FFD3C9");
        config.setDefaultHairColor("#0E1011");
        config.setDefaultEye1Color("#0E1011");
        config.setDefaultEye2Color("#0E1011");
        config.setDefaultAuraColor("#7FFFFF");

		config.setFormSkillTpCosts("superform", new Integer[]{20000, 40000, 60000, 80000, 100000, 120000, 140000, 160000});
		config.setFormSkillTpCosts("godform", new Integer[]{});
		config.setFormSkillTpCosts( "legendaryforms", new Integer[]{});
    }

    private static void setupNamekianCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("namekian");
		config.setDefaultModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
		config.setCanUseHair(false);
        config.setDefaultEyesType(0);
        config.setDefaultNoseType(0);
        config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
        config.setDefaultBodyColor("#1FAA24");
        config.setDefaultBodyColor2("#BB2024");
        config.setDefaultBodyColor3("#FF86A6");
        config.setDefaultHairColor("#1FAA24");
        config.setDefaultEye1Color("#0E1011");
        config.setDefaultEye2Color("#0E1011");
        config.setDefaultAuraColor("#7FFF00");

		config.setFormSkillTpCosts("superform", new Integer[]{20000, 80000, 120000, 160000});
		config.setFormSkillTpCosts("godform", new Integer[]{});
		config.setFormSkillTpCosts( "legendaryforms", new Integer[]{});
    }

    private static void setupFrostDemonCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("frostdemon");
		config.setDefaultModelScaling(new float[]{0.7375f, 0.7375f, 0.7375f});
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
		config.setCanUseHair(false);
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

		config.setFormSkillTpCosts("superform", new Integer[]{20000, 80000, 120000, 160000, 200000});
		config.setFormSkillTpCosts("godform", new Integer[]{});
		config.setFormSkillTpCosts( "legendaryforms", new Integer[]{});
    }

    private static void setupBioAndroidCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("bioandroid");
		config.setDefaultModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
		config.setCanUseHair(false);
        config.setDefaultEyesType(0);
        config.setDefaultNoseType(0);
        config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
        config.setDefaultBodyColor("#187600");
        config.setDefaultBodyColor2("#9FE321");
        config.setDefaultBodyColor3("#FF7600");
        config.setDefaultHairColor("#187600");
        config.setDefaultEye1Color("#383838");
        config.setDefaultEye2Color("#383838");
        config.setDefaultAuraColor("#1AA700");

		config.setFormSkillTpCosts("superform", new Integer[]{20000, 80000, 120000, 160000});
		config.setFormSkillTpCosts("godform", new Integer[]{});
		config.setFormSkillTpCosts( "legendaryforms", new Integer[]{});
    }

    private static void setupMajinCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("majin");
		config.setDefaultModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
		config.setCanUseHair(true);
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

		config.setFormSkillTpCosts("superform", new Integer[]{20000, 80000, 120000, 160000});
		config.setFormSkillTpCosts("godform", new Integer[]{});
		config.setFormSkillTpCosts( "legendaryforms", new Integer[]{});
    }

    private static void setupDefaultCharacter(RaceCharacterConfig config) {
		config.setRacialSkill("human");
		config.setDefaultModelScaling(new float[]{0.9375f, 0.9375f, 0.9375f});
		config.setDefaultBodyType(0);
		config.setDefaultHairType(1);
		config.setDefaultEyesType(0);
		config.setDefaultNoseType(0);
		config.setDefaultMouthType(0);
		config.setDefaultTattooType(0);
		config.setDefaultBodyColor("#FFD3C9");
		config.setDefaultBodyColor2("#FFD3C9");
		config.setDefaultBodyColor3("#FFD3C9");
		config.setDefaultHairColor("#0E1011");
		config.setDefaultEye1Color("#0E1011");
		config.setDefaultEye2Color("#0E1011");
		config.setDefaultAuraColor("#7FFFFF");

		config.setFormSkillTpCosts("superform", new Integer[]{});
		config.setFormSkillTpCosts("godform", new Integer[]{});
		config.setFormSkillTpCosts( "legendaryforms", new Integer[]{});
    }

    private static RaceStatsConfig createDefaultStatsConfig() {
        RaceStatsConfig config = new RaceStatsConfig();
		setupDefaultStats(config);
        return config;
    }

    private static void setupDefaultStats(RaceStatsConfig config) {
        setupInitialStats(config.getClassStats("warrior"), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getClassStats("warrior"), 1.0, 0.75, 0.5, 0.75, 1.5, 0.5, 1.0);
        setupInitialStats(config.getClassStats("spiritualist"), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getClassStats("spiritualist"), 0.5, 0.5, 0.25, 0.25, 1.0, 1.0, 1.5);
        setupInitialStats(config.getClassStats("martialartist"), 5, 10, 10, 10, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getClassStats("martialartist"), 0.75, 1.0, 0.75, 1.0, 1.75, 0.75, 1.25);
    }

    private static void setupInitialStats(RaceStatsConfig.ClassStats classStats, int str, int skp, int res, int vit, int pwr, int ene, double healthRegen, double energyRegen, double staminaRegen) {
        RaceStatsConfig.BaseStats base = classStats.getBaseStats();
        base.setStrength(str);
        base.setStrikePower(skp);
        base.setResistance(res);
        base.setVitality(vit);
        base.setKiPower(pwr);
        base.setEnergy(ene);

        classStats.setHealthRegenRate(healthRegen);
        classStats.setEnergyRegenRate(energyRegen);
        classStats.setStaminaRegenRate(staminaRegen);
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
        if (SERVER_SYNCED_STATS != null && SERVER_SYNCED_STATS.containsKey(raceName.toLowerCase())) return SERVER_SYNCED_STATS.get(raceName.toLowerCase());
        return RACE_STATS.getOrDefault(raceName.toLowerCase(), RACE_STATS.get("human"));
    }

    public static RaceCharacterConfig getRaceCharacter(String raceName) {
        if (SERVER_SYNCED_CHARACTER != null && SERVER_SYNCED_CHARACTER.containsKey(raceName.toLowerCase())) return SERVER_SYNCED_CHARACTER.get(raceName.toLowerCase());
        return RACE_CHARACTER.getOrDefault(raceName.toLowerCase(), RACE_CHARACTER.get("human"));
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

	public static List<String> getDefaultRaces() {
		return Arrays.asList(DEFAULT_RACES);
	}

    public static boolean isRaceLoaded(String raceName) {
        if (SERVER_SYNCED_CHARACTER != null) return SERVER_SYNCED_CHARACTER.containsKey(raceName.toLowerCase());
        return LOADED_RACES.stream().anyMatch(r -> r.equalsIgnoreCase(raceName));
    }

    public static GeneralUserConfig getUserConfig() {
        return userConfig != null ? userConfig : new GeneralUserConfig();
    }

    public static GeneralServerConfig getServerConfig() {
        if (SERVER_SYNCED_GENERAL_SERVER != null) return SERVER_SYNCED_GENERAL_SERVER;
        return serverConfig != null ? serverConfig : new GeneralServerConfig();
    }

    public static void saveGeneralUserConfig() {
        try {
            Path path = CONFIG_DIR.resolve("general-user.json");
            LOADER.saveConfig(path, userConfig);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving user configuration: {}", e.getMessage());
        }
    }

    public static void applySyncedServerConfig(GeneralServerConfig syncedServerConfig, SkillsConfig syncedSkillsConfig, Map<String, Map<String, FormConfig>> syncedForms, Map<String, RaceStatsConfig> syncedStats, Map<String, RaceCharacterConfig> syncedCharacters) {
        SERVER_SYNCED_GENERAL_SERVER = syncedServerConfig;
        SERVER_SYNCED_SKILLS = syncedSkillsConfig;
        SERVER_SYNCED_FORMS = syncedForms;
        SERVER_SYNCED_STATS = syncedStats;
        SERVER_SYNCED_CHARACTER = syncedCharacters;
    }

    public static void clearServerSync() {
        SERVER_SYNCED_GENERAL_SERVER = null;
        SERVER_SYNCED_SKILLS = null;
        SERVER_SYNCED_FORMS = null;
        SERVER_SYNCED_STATS = null;
        SERVER_SYNCED_CHARACTER = null;
    }

    public static Map<String, RaceStatsConfig> getAllRaceStats() {
        if (SERVER_SYNCED_STATS != null) return SERVER_SYNCED_STATS;
        return new HashMap<>(RACE_STATS);
    }

    public static Map<String, RaceCharacterConfig> getAllRaceCharacters() {
        if (SERVER_SYNCED_CHARACTER != null) return SERVER_SYNCED_CHARACTER;
        return new HashMap<>(RACE_CHARACTER);
    }

    public static FormConfig getFormGroup(String raceName, String groupName) {
        Map<String, FormConfig> raceForms = getAllFormsForRace(raceName);
        if (raceForms != null) return raceForms.get(groupName.toLowerCase());
        return null;
    }

    public static FormConfig.FormData getForm(String raceName, String groupName, String formName) {
        FormConfig group = getFormGroup(raceName, groupName);
        if (group != null) return group.getForm(formName);
        return null;
    }

	public static FormConfig getStackFormGroup(String groupName) {
		Map<String, FormConfig> stackForms = getAllStackForms();
		if (stackForms != null) return stackForms.get(groupName.toLowerCase());
		return null;
	}

	public static FormConfig.FormData getStackForm(String groupName, String formName) {
		FormConfig group = getStackFormGroup(groupName);
		if (group != null) return group.getForm(formName);
		return null;
	}

    public static Map<String, Map<String, FormConfig>> getAllForms() {
        if (SERVER_SYNCED_FORMS != null) return SERVER_SYNCED_FORMS;
        return RACE_FORMS;
    }

    public static Map<String, FormConfig> getAllFormsForRace(String raceName) {
        Map<String, Map<String, FormConfig>> forms = getAllForms();
        return forms.getOrDefault(raceName.toLowerCase(), new HashMap<>());
    }

	public static Map<String, FormConfig> getAllStackForms() {
		return STACK_FORMS;
	}

    public static SkillsConfig getSkillsConfig() {
        if (SERVER_SYNCED_SKILLS != null) return SERVER_SYNCED_SKILLS;
        return skillsConfig != null ? skillsConfig : new SkillsConfig();
    }

	public static EntitiesConfig.EntityStats getEntityStats(String sagaId, String registryName) {
		if (entitiesConfig != null && entitiesConfig.getSagaEntityStats() != null) {
			if (sagaId != null && !sagaId.isEmpty()) {
				Map<String, EntitiesConfig.EntityStats> sagaMap = entitiesConfig.getSagaEntityStats().get(sagaId);
				if (sagaMap != null && sagaMap.containsKey(registryName)) {
					return sagaMap.get(registryName);
				}
			}
			Map<String, EntitiesConfig.EntityStats> defaultMap = entitiesConfig.getSagaEntityStats().get("default");
			if (defaultMap != null && defaultMap.containsKey(registryName)) {
				return defaultMap.get(registryName);
			}
		}
		return null;
	}

	public static EntitiesConfig getEntitiesConfig() {
		return entitiesConfig;
	}
}
