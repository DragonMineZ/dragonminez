package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.MainEntities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegistryObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create();

    private static final ConfigLoader LOADER = new ConfigLoader(GSON);
    private static final DefaultFormsFactory FORMS_FACTORY = new DefaultFormsFactory(GSON, LOADER);

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
    private static final Path RACES_DIR = CONFIG_DIR.resolve("races");

    private static final String[] DEFAULT_RACES = {"human", "saiyan", "namekian", "frostdemon", "bioandroid", "majin"};
    private static final Set<String> RACES_WITH_GENDER = new HashSet<>(Arrays.asList("human", "saiyan", "majin"));

    private static final Map<String, RaceStatsConfig> RACE_STATS = new HashMap<>();
    private static final Map<String, RaceCharacterConfig> RACE_CHARACTER = new HashMap<>();
    private static final Map<String, Map<String, FormConfig>> RACE_FORMS = new HashMap<>();
    private static final List<String> LOADED_RACES = new ArrayList<>();

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

            LogUtil.info(Env.COMMON, "Configuration system initialized successfully");
            LogUtil.info(Env.COMMON, "Loaded races: {}", LOADED_RACES);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error initializing configuration system: {}", e.getMessage());
        }
    }

    private static void loadGeneralConfigs() throws IOException {
        Path userConfigPath = CONFIG_DIR.resolve("general-user.json");
        if (Files.exists(userConfigPath)) {
            userConfig = LOADER.loadConfig(userConfigPath, GeneralUserConfig.class);
        } else {
            userConfig = new GeneralUserConfig();
            LOADER.saveConfig(userConfigPath, userConfig);
        }

        Path serverConfigPath = CONFIG_DIR.resolve("general-server.json");
        if (Files.exists(serverConfigPath)) {
            serverConfig = LOADER.loadConfig(serverConfigPath, GeneralServerConfig.class);
        } else {
			try {
				LOADER.saveDefaultFromTemplate(serverConfigPath, "general-server.json");
				serverConfig = LOADER.loadConfig(serverConfigPath, GeneralServerConfig.class);
			} catch (Exception e) {
				serverConfig = new GeneralServerConfig();
				LOADER.saveConfig(serverConfigPath, serverConfig);
				LogUtil.error(Env.COMMON, "Error creating skills configuration from template, created default instead: {}");
			}
        }

        Path skillsConfigPath = CONFIG_DIR.resolve("skills.json");
        if (Files.exists(skillsConfigPath)) {
            skillsConfig = LOADER.loadConfig(skillsConfigPath, SkillsConfig.class);
        } else {
			try {
				LOADER.saveDefaultFromTemplate(skillsConfigPath, "skills.json");
				skillsConfig = LOADER.loadConfig(skillsConfigPath, SkillsConfig.class);
			} catch (Exception e) {
				skillsConfig = new SkillsConfig();
				LOADER.saveConfig(skillsConfigPath, skillsConfig);
				LogUtil.error(Env.COMMON, "Error creating skills configuration from template, created default instead: {}");
			}
        }

        Path entitiesConfigPath = CONFIG_DIR.resolve("entities.json");
        if (Files.exists(entitiesConfigPath)) {
            entitiesConfig = LOADER.loadConfig(entitiesConfigPath, EntitiesConfig.class);
        } else {
            entitiesConfig = createDefaultEntitiesConfig();
            LOADER.saveConfig(entitiesConfigPath, entitiesConfig);
        }
    }

	private static EntitiesConfig createDefaultEntitiesConfig() {
		EntitiesConfig config = new EntitiesConfig();

		EntitiesConfig.HardModeSettings hardMode = config.getHardModeSettings();
		hardMode.setHpMultiplier(3.0);
		hardMode.setDamageMultiplier(2.0);

		Map<String, EntitiesConfig.EntityStats> statsMap = config.getEntityStats();

		addDefaultEntityStats(statsMap, MainEntities.SAGA_SAIBAMAN, 200.0, 10.0, 20.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_SAIBAMAN2, 200.0, 10.0, 20.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_SAIBAMAN3, 200.0, 10.0, 20.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_SAIBAMAN4, 200.0, 10.0, 20.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_SAIBAMAN5, 200.0, 10.0, 20.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_SAIBAMAN6, 200.0, 10.0, 20.0);

		addDefaultEntityStats(statsMap, MainEntities.SAGA_RADITZ, 200.0, 20.0, 50.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_NAPPA, 400.0, 40.0, 100.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_VEGETA, 500.0, 50.0, 150.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_OZARU_VEGETA, 1000.0, 100.0, 200.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_CUI, 300.0, 30.0, 80.0);
		addDefaultEntityStats(statsMap, MainEntities.SAGA_DODORIA, 350.0, 35.0, 90.0);

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

        for (String raceName : DEFAULT_RACES) {
            createOrLoadRace(raceName, true);
        }

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

    private static void createOrLoadRace(String raceName, boolean isDefault) throws IOException {
        Path racePath = RACES_DIR.resolve(raceName);
        Files.createDirectories(racePath);

        Path characterPath = racePath.resolve("character.json");
        Path statsPath = racePath.resolve("stats.json");
        Path formsPath = racePath.resolve("forms");
        Files.createDirectories(formsPath);

        RaceCharacterConfig characterConfig;
        if (Files.exists(characterPath)) {
            characterConfig = LOADER.loadConfig(characterPath, RaceCharacterConfig.class);

            RaceCharacterConfig defaultConfig = createDefaultCharacterConfig(raceName, isDefault);
            boolean needsUpdate = mergeCharacterConfig(characterConfig, defaultConfig);

            if (needsUpdate) {
                LOADER.saveConfig(characterPath, characterConfig);
            }
        } else {
            characterConfig = createDefaultCharacterConfig(raceName, isDefault);
            LOADER.saveConfig(characterPath, characterConfig);
        }

        RaceStatsConfig statsConfig;
        if (Files.exists(statsPath)) {
            statsConfig = LOADER.loadConfig(statsPath, RaceStatsConfig.class);
        } else {
            statsConfig = createDefaultStatsConfig(raceName, isDefault);
            LOADER.saveConfig(statsPath, statsConfig);
        }

        Map<String, FormConfig> raceForms = LOADER.loadRaceForms(raceName, formsPath);

        if (isDefault && !LOADER.hasExistingFiles(formsPath)) {
            FORMS_FACTORY.createDefaultFormsForRace(raceName, formsPath, raceForms);
        }

        RACE_FORMS.put(raceName.toLowerCase(), raceForms);
        RACE_CHARACTER.put(raceName.toLowerCase(), characterConfig);
        RACE_STATS.put(raceName.toLowerCase(), statsConfig);
        LOADED_RACES.add(raceName);
    }

    public static boolean isDefaultRace(String raceName) {
        for (String vanilla : DEFAULT_RACES) {
            if (vanilla.equalsIgnoreCase(raceName)) {
                return true;
            }
        }
        return false;
    }

    private static RaceCharacterConfig createDefaultCharacterConfig(String raceName, boolean isDefault) {
        RaceCharacterConfig config = new RaceCharacterConfig();
        config.setRaceName(raceName);
        config.setUseVanillaSkin(false);
        config.setCustomModel("");
		config.setDefaultModelScaling(0.9375f);

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

        config.setSuperformTpCost(new int[]{});
        config.setGodformTpCost(new int[]{});
        config.setLegendaryformsTpCost(new int[]{});
    }

    private static void setupSaiyanCharacter(RaceCharacterConfig config) {
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

        config.setSuperformTpCost(new int[]{1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000});
        config.setGodformTpCost(new int[]{10000, 20000, 30000});
        config.setLegendaryformsTpCost(new int[]{50000, 100000});
    }

    private static void setupNamekianCharacter(RaceCharacterConfig config) {
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

        config.setSuperformTpCost(new int[]{1000, 2000});
        config.setGodformTpCost(new int[]{});
        config.setLegendaryformsTpCost(new int[]{});
    }

    private static void setupFrostDemonCharacter(RaceCharacterConfig config) {
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
        config.setDefaultHairColor("#FF001D");
        config.setDefaultEye1Color("#FF001D");
        config.setDefaultEye2Color("#000000");
        config.setDefaultAuraColor("#5F00FF");

        config.setSuperformTpCost(new int[]{});
        config.setGodformTpCost(new int[]{});
        config.setLegendaryformsTpCost(new int[]{});
    }

    private static void setupBioAndroidCharacter(RaceCharacterConfig config) {
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
        config.setDefaultEye1Color("#0E1011");
        config.setDefaultEye2Color("#0E1011");
        config.setDefaultAuraColor("#1AA700");

        config.setSuperformTpCost(new int[]{});
        config.setGodformTpCost(new int[]{});
        config.setLegendaryformsTpCost(new int[]{});
    }

    private static void setupMajinCharacter(RaceCharacterConfig config) {
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

        config.setSuperformTpCost(new int[]{});
        config.setGodformTpCost(new int[]{});
        config.setLegendaryformsTpCost(new int[]{});
    }

    private static void setupDefaultCharacter(RaceCharacterConfig config) {
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

		config.setSuperformTpCost(new int[]{});
		config.setGodformTpCost(new int[]{});
		config.setLegendaryformsTpCost(new int[]{});
    }

    private static RaceStatsConfig createDefaultStatsConfig(String raceName, boolean isVanilla) {
        RaceStatsConfig config = new RaceStatsConfig();

        if (isVanilla) {
            switch (raceName.toLowerCase()) {
                case "human" -> setupHumanStats(config);
                case "saiyan" -> setupSaiyanStats(config);
                case "namekian" -> setupNamekianStats(config);
                case "frostdemon" -> setupFrostDemonStats(config);
                case "bioandroid" -> setupBioAndroidStats(config);
                case "majin" -> setupMajinStats(config);
            }
        } else {
            setupDefaultStats(config);
        }

        return config;
    }

    private static void setupHumanStats(RaceStatsConfig config) {
        setupInitialStats(config.getWarrior(), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getWarrior(), 2.0, 1.5, 0.35, 1.5, 0.6, 1.0, 1.5);
        setupInitialStats(config.getSpiritualist(), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getSpiritualist(), 1.0, 2.0, 0.25, 1.0, 0.3, 2.0, 2.0);
        setupInitialStats(config.getMartialArtist(), 5, 5, 15, 15, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getMartialArtist(), 1.25, 1.25, 0.6, 2.0, 1.0, 1.25, 1.5);
    }

    private static void setupSaiyanStats(RaceStatsConfig config) {
        setupInitialStats(config.getWarrior(), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getWarrior(), 2.0, 1.5, 0.35, 1.5, 0.6, 1.0, 1.5);
        setupInitialStats(config.getSpiritualist(), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getSpiritualist(), 1.0, 2.0, 0.25, 1.0, 0.3, 2.0, 2.0);
        setupInitialStats(config.getMartialArtist(), 5, 5, 15, 15, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getMartialArtist(), 1.25, 1.25, 0.6, 2.0, 1.0, 1.25, 1.5);
    }

    private static void setupNamekianStats(RaceStatsConfig config) {
        setupInitialStats(config.getWarrior(), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getWarrior(), 2.0, 1.5, 0.35, 1.5, 0.6, 1.0, 1.5);
        setupInitialStats(config.getSpiritualist(), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getSpiritualist(), 1.0, 2.0, 0.25, 1.0, 0.3, 2.0, 2.0);
        setupInitialStats(config.getMartialArtist(), 5, 5, 15, 15, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getMartialArtist(), 1.25, 1.25, 0.6, 2.0, 1.0, 1.25, 1.5);
    }

    private static void setupFrostDemonStats(RaceStatsConfig config) {
        setupInitialStats(config.getWarrior(), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getWarrior(), 2.0, 1.5, 0.35, 1.5, 0.6, 1.0, 1.5);
        setupInitialStats(config.getSpiritualist(), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getSpiritualist(), 1.0, 2.0, 0.25, 1.0, 0.3, 2.0, 2.0);
        setupInitialStats(config.getMartialArtist(), 5, 5, 15, 15, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getMartialArtist(), 1.25, 1.25, 0.6, 2.0, 1.0, 1.25, 1.5);
    }

    private static void setupBioAndroidStats(RaceStatsConfig config) {
        setupInitialStats(config.getWarrior(), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getWarrior(), 2.0, 1.5, 0.35, 1.5, 0.6, 1.0, 1.5);
        setupInitialStats(config.getSpiritualist(), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getSpiritualist(), 1.0, 2.0, 0.25, 1.0, 0.3, 2.0, 2.0);
        setupInitialStats(config.getMartialArtist(), 5, 5, 15, 15, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getMartialArtist(), 1.25, 1.25, 0.6, 2.0, 1.0, 1.25, 1.5);
    }

    private static void setupMajinStats(RaceStatsConfig config) {
        setupInitialStats(config.getWarrior(), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getWarrior(), 2.0, 1.5, 0.35, 1.5, 0.6, 1.0, 1.5);
        setupInitialStats(config.getSpiritualist(), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getSpiritualist(), 1.0, 2.0, 0.25, 1.0, 0.3, 2.0, 2.0);
        setupInitialStats(config.getMartialArtist(), 5, 5, 15, 15, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getMartialArtist(), 1.25, 1.25, 0.6, 2.0, 1.0, 1.25, 1.5);
    }

    private static void setupDefaultStats(RaceStatsConfig config) {
        setupInitialStats(config.getWarrior(), 10, 5, 10, 10, 5, 5, 0.003, 0.008, 0.012);
		setupScalingStats(config.getWarrior(), 2.0, 1.5, 0.35, 1.5, 0.6, 1.0, 1.5);
        setupInitialStats(config.getSpiritualist(), 5, 10, 5, 5, 10, 10, 0.002, 0.015, 0.008);
		setupScalingStats(config.getSpiritualist(), 1.0, 2.0, 0.25, 1.0, 0.3, 2.0, 2.0);
        setupInitialStats(config.getMartialArtist(), 5, 5, 15, 15, 5, 5, 0.0035, 0.008, 0.009);
		setupScalingStats(config.getMartialArtist(), 1.25, 1.25, 0.6, 2.0, 1.0, 1.25, 1.5);
    }

    private static void setupInitialStats(RaceStatsConfig.ClassStats classStats,
										  int str, int skp, int res, int vit, int pwr, int ene,
										  double healthRegen, double energyRegen, double staminaRegen) {
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
	
	private static void setupScalingStats(RaceStatsConfig.ClassStats classStats,
										  double strScale, double skpScale, double defScale, double stmScale, double vitScale, double pwrScale, double eneScale) {
		RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
		scaling.setStrengthScaling(strScale);
		scaling.setStrikePowerScaling(skpScale);
		scaling.setDefenseScaling(defScale);
		scaling.setStaminaScaling(stmScale);
		scaling.setVitalityScaling(vitScale);
		scaling.setKiPowerScaling(pwrScale);
		scaling.setEnergyScaling(eneScale);
	}

    private static boolean mergeCharacterConfig(RaceCharacterConfig existing, RaceCharacterConfig defaults) {
        boolean updated = false;

        if (existing.getDefaultBodyColor() == null && defaults.getDefaultBodyColor() != null) {
            existing.setDefaultBodyColor(defaults.getDefaultBodyColor());
            updated = true;
        }

        if (existing.getDefaultBodyColor2() == null && defaults.getDefaultBodyColor2() != null) {
            existing.setDefaultBodyColor2(defaults.getDefaultBodyColor2());
            updated = true;
        }

        if (existing.getDefaultBodyColor3() == null && defaults.getDefaultBodyColor3() != null) {
            existing.setDefaultBodyColor3(defaults.getDefaultBodyColor3());
            updated = true;
        }

        if (existing.getDefaultHairColor() == null && defaults.getDefaultHairColor() != null) {
            existing.setDefaultHairColor(defaults.getDefaultHairColor());
            updated = true;
        }

        if (existing.getDefaultEye1Color() == null && defaults.getDefaultEye1Color() != null) {
            existing.setDefaultEye1Color(defaults.getDefaultEye1Color());
            updated = true;
        }

        if (existing.getDefaultEye2Color() == null && defaults.getDefaultEye2Color() != null) {
            existing.setDefaultEye2Color(defaults.getDefaultEye2Color());
            updated = true;
        }

        if (existing.getDefaultAuraColor() == null && defaults.getDefaultAuraColor() != null) {
            existing.setDefaultAuraColor(defaults.getDefaultAuraColor());
            updated = true;
        }

        if (existing.getSuperformTpCost() == null && defaults.getSuperformTpCost() != null) {
            existing.setSuperformTpCost(defaults.getSuperformTpCost());
            updated = true;
        }

        if (existing.getGodformTpCost() == null && defaults.getGodformTpCost() != null) {
            existing.setGodformTpCost(defaults.getGodformTpCost());
            updated = true;
        }

        if (existing.getLegendaryformsTpCost() == null && defaults.getLegendaryformsTpCost() != null) {
            existing.setLegendaryformsTpCost(defaults.getLegendaryformsTpCost());
            updated = true;
        }

        return updated;
    }

    public static RaceStatsConfig getRaceStats(String raceName) {
        if (SERVER_SYNCED_STATS != null && SERVER_SYNCED_STATS.containsKey(raceName.toLowerCase())) {
            return SERVER_SYNCED_STATS.get(raceName.toLowerCase());
        }
        return RACE_STATS.getOrDefault(raceName.toLowerCase(), RACE_STATS.get("human"));
    }

    public static RaceCharacterConfig getRaceCharacter(String raceName) {
        if (SERVER_SYNCED_CHARACTER != null && SERVER_SYNCED_CHARACTER.containsKey(raceName.toLowerCase())) {
            return SERVER_SYNCED_CHARACTER.get(raceName.toLowerCase());
        }
        return RACE_CHARACTER.getOrDefault(raceName.toLowerCase(), RACE_CHARACTER.get("human"));
    }

    public static List<String> getLoadedRaces() {
        return new ArrayList<>(LOADED_RACES);
    }

    public static boolean isRaceLoaded(String raceName) {
        return LOADED_RACES.contains(raceName.toLowerCase());
    }

    public static GeneralUserConfig getUserConfig() {
        return userConfig != null ? userConfig : new GeneralUserConfig();
    }

    public static GeneralServerConfig getServerConfig() {
        if (SERVER_SYNCED_GENERAL_SERVER != null) {
            return SERVER_SYNCED_GENERAL_SERVER;
        }
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

    public static void saveGeneralServerConfig() {
        try {
            Path path = CONFIG_DIR.resolve("general-server.json");
            LOADER.saveConfig(path, serverConfig);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving server configuration: {}", e.getMessage());
        }
    }

    public static void saveRaceStats(String raceName) {
        try {
            Path path = RACES_DIR.resolve(raceName).resolve("stats.json");
            RaceStatsConfig config = RACE_STATS.get(raceName);
            if (config != null) {
                LOADER.saveConfig(path, config);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving stats for '{}': {}", raceName, e.getMessage());
        }
    }

    public static void saveRaceCharacter(String raceName) {
        try {
            Path path = RACES_DIR.resolve(raceName).resolve("character.json");
            RaceCharacterConfig config = RACE_CHARACTER.get(raceName);
            if (config != null) {
                LOADER.saveConfig(path, config);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving character for '{}': {}", raceName, e.getMessage());
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

    public static boolean isUsingServerConfig() {
        return SERVER_SYNCED_GENERAL_SERVER != null || SERVER_SYNCED_SKILLS != null || SERVER_SYNCED_FORMS != null || SERVER_SYNCED_STATS != null || SERVER_SYNCED_CHARACTER != null;
    }

    public static Map<String, RaceStatsConfig> getAllRaceStats() {
        if (SERVER_SYNCED_STATS != null) {
            return SERVER_SYNCED_STATS;
        }
        return new HashMap<>(RACE_STATS);
    }

    public static Map<String, RaceCharacterConfig> getAllRaceCharacters() {
        if (SERVER_SYNCED_CHARACTER != null) {
            return SERVER_SYNCED_CHARACTER;
        }
        return new HashMap<>(RACE_CHARACTER);
    }

    public static FormConfig getFormGroup(String raceName, String groupName) {
        Map<String, FormConfig> raceForms = getAllFormsForRace(raceName);
        if (raceForms != null) {
            return raceForms.get(groupName.toLowerCase());
        }
        return null;
    }

    public static FormConfig.FormData getForm(String raceName, String groupName, String formName) {
        FormConfig group = getFormGroup(raceName, groupName);
        if (group != null) {
            return group.getForm(formName);
        }
        return null;
    }

    public static Map<String, Map<String, FormConfig>> getAllForms() {
        if (SERVER_SYNCED_FORMS != null) {
            return SERVER_SYNCED_FORMS;
        }
        return RACE_FORMS;
    }

    public static Map<String, FormConfig> getAllFormsForRace(String raceName) {
        Map<String, Map<String, FormConfig>> forms = getAllForms();
        return forms.getOrDefault(raceName.toLowerCase(), new HashMap<>());
    }

    public static boolean hasFormGroup(String raceName, String groupName) {
        return getFormGroup(raceName, groupName) != null;
    }

    public static boolean hasForm(String raceName, String groupName, String formName) {
        return getForm(raceName, groupName, formName) != null;
    }

    public static SkillsConfig getSkillsConfig() {
        if (SERVER_SYNCED_SKILLS != null) {
            return SERVER_SYNCED_SKILLS;
        }
        return skillsConfig != null ? skillsConfig : new SkillsConfig();
    }

    public static void setServerSyncedSkills(SkillsConfig config) {
        SERVER_SYNCED_SKILLS = config;
    }

    public static void clearServerSyncedSkills() {
        SERVER_SYNCED_SKILLS = null;
    }

    public static EntitiesConfig.EntityStats getEntityStats(String registryName) {
        if (entitiesConfig != null && entitiesConfig.getEntityStats() != null) {
            return entitiesConfig.getEntityStats().get(registryName);
        }
        return null;
    }

	public static EntitiesConfig getEntitiesConfig() {
		return entitiesConfig;
	}
}
