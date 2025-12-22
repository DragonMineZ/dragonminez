package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.network.S2C.SyncServerConfigS2C;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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

    private static final Map<String, RaceStatsConfig> SERVER_SYNCED_STATS = new HashMap<>();
    private static final Map<String, RaceCharacterConfig> SERVER_SYNCED_CHARACTER = new HashMap<>();

    private static GeneralUserConfig userConfig;
    private static GeneralServerConfig serverConfig;
    private static SkillsConfig skillsConfig;

    private static SkillsConfig SERVER_SYNCED_SKILLS;

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
			//LOADER.saveDefaultFromTemplate(serverConfigPath, "general-server.json");
			//serverConfig = LOADER.loadConfig(serverConfigPath, GeneralServerConfig.class);
			serverConfig = new GeneralServerConfig();
			LOADER.saveConfig(serverConfigPath, serverConfig);
        }

        Path skillsConfigPath = CONFIG_DIR.resolve("skills.json");
        if (Files.exists(skillsConfigPath)) {
            skillsConfig = LOADER.loadConfig(skillsConfigPath, SkillsConfig.class);
        } else {
            skillsConfig = new SkillsConfig();
            LOADER.saveConfig(skillsConfigPath, skillsConfig);
        }
    }

    private static void loadAllRaces() throws IOException {
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
        config.setDefaultHairType(0);
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
        config.setDefaultHairType(0);
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
		config.setDefaultHairType(0);
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
        setupClassStats(config.getWarrior(), 5, 5, 5, 5, 5, 5, 0.003, 0.008, 0.012);
        setupClassStats(config.getSpiritualist(), 5, 5, 5, 5, 5, 5, 0.002, 0.015, 0.008);
        setupClassStats(config.getMartialArtist(), 5, 5, 5, 5, 5, 5, 0.0035, 0.008, 0.009);
    }

    private static void setupSaiyanStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 6, 6, 5, 5, 5, 5, 0.003, 0.008, 0.012);
        setupClassStats(config.getSpiritualist(), 6, 6, 5, 5, 5, 5, 0.002, 0.015, 0.008);
        setupClassStats(config.getMartialArtist(), 6, 6, 5, 5, 5, 5, 0.0035, 0.008, 0.009);
    }

    private static void setupNamekianStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 4, 4, 6, 6, 6, 6, 0.003, 0.008, 0.012);
        setupClassStats(config.getSpiritualist(), 4, 4, 6, 6, 6, 6, 0.002, 0.015, 0.008);
        setupClassStats(config.getMartialArtist(), 4, 4, 6, 6, 6, 6, 0.0035, 0.008, 0.009);
    }

    private static void setupFrostDemonStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 7, 7, 6, 5, 6, 5, 0.003, 0.008, 0.012);
        setupClassStats(config.getSpiritualist(), 7, 7, 6, 5, 6, 5, 0.002, 0.015, 0.008);
        setupClassStats(config.getMartialArtist(), 7, 7, 6, 5, 6, 5, 0.0035, 0.008, 0.009);
    }

    private static void setupBioAndroidStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 5, 5, 5, 5, 6, 6, 0.003, 0.008, 0.012);
        setupClassStats(config.getSpiritualist(), 5, 5, 5, 5, 6, 6, 0.002, 0.015, 0.008);
        setupClassStats(config.getMartialArtist(), 5, 5, 5, 5, 6, 6, 0.0035, 0.008, 0.009);
    }

    private static void setupMajinStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 5, 5, 6, 6, 6, 7, 0.003, 0.008, 0.012);
        setupClassStats(config.getSpiritualist(), 5, 5, 6, 6, 6, 7, 0.002, 0.015, 0.008);
        setupClassStats(config.getMartialArtist(), 5, 5, 6, 6, 6, 7, 0.0035, 0.008, 0.009);
    }

    private static void setupDefaultStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 5, 5, 5, 5, 5, 5, 0.003, 0.008, 0.012);
        setupClassStats(config.getSpiritualist(), 5, 5, 5, 5, 5, 5, 0.002, 0.015, 0.008);
        setupClassStats(config.getMartialArtist(), 5, 5, 5, 5, 5, 5, 0.0035, 0.008, 0.009);
    }

    private static void setupClassStats(RaceStatsConfig.ClassStats classStats,
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

        RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
        scaling.setStrengthScaling(1.0);
        scaling.setStrikePowerScaling(1.0);
        scaling.setStaminaScaling(1.0);
        scaling.setDefenseScaling(1.0);
        scaling.setVitalityScaling(1.0);
        scaling.setKiPowerScaling(1.0);
        scaling.setEnergyScaling(1.0);
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
        if (SERVER_SYNCED_STATS.containsKey(raceName.toLowerCase())) {
            return SERVER_SYNCED_STATS.get(raceName.toLowerCase());
        }
        return RACE_STATS.getOrDefault(raceName.toLowerCase(), RACE_STATS.get("human"));
    }

    public static RaceCharacterConfig getRaceCharacter(String raceName) {
        String key = raceName.toLowerCase();

        if (SERVER_SYNCED_CHARACTER.containsKey(key)) {
            RaceCharacterConfig syncedConfig = SERVER_SYNCED_CHARACTER.get(key);
            if (syncedConfig != null && syncedConfig.getDefaultBodyColor() != null) {
                return syncedConfig;
            }
        }

        return RACE_CHARACTER.getOrDefault(key, RACE_CHARACTER.get("human"));
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

    public static void applySyncedServerConfig(Map<String, ?> syncedStats,
                                               Map<String, ?> syncedCharacters,
                                               Map<String, ?> syncedForms,
                                               Object generalServerData,
                                               Object skillsData) {
        SERVER_SYNCED_STATS.clear();
        SERVER_SYNCED_CHARACTER.clear();
        RACE_FORMS.clear();

        if (syncedStats != null) {
            syncedStats.forEach((raceName, data) -> {
                if (data instanceof SyncServerConfigS2C.RaceStatsData statsData) {
                    SERVER_SYNCED_STATS.put(raceName.toLowerCase(), statsData.toConfig(raceName));
                }
            });
        }

        if (syncedCharacters != null) {
            syncedCharacters.forEach((raceName, data) -> {
                if (data instanceof SyncServerConfigS2C.RaceCharacterData characterData) {
                    SERVER_SYNCED_CHARACTER.put(raceName.toLowerCase(), characterData.toConfig());
                }
            });
        }

        if (syncedForms != null) {
            syncedForms.forEach((raceName, data) -> {
                if (data instanceof SyncServerConfigS2C.RaceFormsData formsData) {
                    Map<String, FormConfig> forms = formsData.toConfig(raceName.toString());
                    if (forms != null) {
                        RACE_FORMS.put(raceName.toLowerCase(), forms);
                    }
                }
            });
        }

        if (generalServerData instanceof SyncServerConfigS2C.GeneralServerData generalData) {
            serverConfig = generalData.toConfig();
        }

        if (skillsData instanceof SyncServerConfigS2C.SkillsData syncedSkills) {
            SERVER_SYNCED_SKILLS = syncedSkills.toConfig();
        }
    }

    public static void clearServerSync() {
        SERVER_SYNCED_STATS.clear();
        SERVER_SYNCED_CHARACTER.clear();
        RACE_FORMS.clear();
        SERVER_SYNCED_SKILLS = null;
    }

    public static boolean isUsingServerConfig() {
        return !SERVER_SYNCED_STATS.isEmpty() || !SERVER_SYNCED_CHARACTER.isEmpty() || !RACE_FORMS.isEmpty();
    }

    public static Map<String, RaceStatsConfig> getAllRaceStats() {
        return new HashMap<>(RACE_STATS);
    }

    public static Map<String, RaceCharacterConfig> getAllRaceCharacters() {
        return new HashMap<>(RACE_CHARACTER);
    }

    public static FormConfig getFormGroup(String raceName, String groupName) {
        Map<String, FormConfig> raceForms = RACE_FORMS.get(raceName.toLowerCase());
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

    public static Map<String, FormConfig> getAllFormsForRace(String raceName) {
        Map<String, FormConfig> forms = RACE_FORMS.get(raceName.toLowerCase());
        return forms != null ? new HashMap<>(forms) : new HashMap<>();
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
}
