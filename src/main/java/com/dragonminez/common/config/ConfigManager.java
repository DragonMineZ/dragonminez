package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
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
        Path userConfigPath = CONFIG_DIR.resolve("general-user.json5");
        if (Files.exists(userConfigPath)) {
            userConfig = LOADER.loadConfig(userConfigPath, GeneralUserConfig.class);
            LogUtil.info(Env.COMMON, "User configuration loaded from: {}", userConfigPath);
        } else {
            userConfig = new GeneralUserConfig();
            LOADER.saveConfigWithComments(userConfigPath, userConfig,
                "DragonMineZ - User Configuration",
                "This file contains client-side settings.",
                "These settings only affect your client and are not synced to servers."
            );
            LogUtil.info(Env.COMMON, "Default user configuration created at: {}", userConfigPath);
        }

        Path serverConfigPath = CONFIG_DIR.resolve("general-server.json5");
        if (Files.exists(serverConfigPath)) {
            serverConfig = LOADER.loadConfig(serverConfigPath, GeneralServerConfig.class);
            LogUtil.info(Env.COMMON, "Server configuration loaded from: {}", serverConfigPath);
        } else {
            serverConfig = new GeneralServerConfig();
            LOADER.saveConfigWithComments(serverConfigPath, serverConfig,
                "DragonMineZ - Server Configuration",
                "This file contains server-side settings.",
                "These settings are synced to all connected clients."
            );
            LogUtil.info(Env.COMMON, "Default server configuration created at: {}", serverConfigPath);
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

        Path characterPath = racePath.resolve("character.json5");
        Path statsPath = racePath.resolve("stats.json5");
        Path formsPath = racePath.resolve("forms");
        Files.createDirectories(formsPath);

        RaceCharacterConfig characterConfig;
        if (Files.exists(characterPath)) {
            characterConfig = LOADER.loadConfig(characterPath, RaceCharacterConfig.class);
            LogUtil.info(Env.COMMON, "Character config for '{}' loaded", raceName);
        } else {
            characterConfig = createDefaultCharacterConfig(raceName, isDefault);
            LOADER.saveConfigWithComments(characterPath, characterConfig,
                "DragonMineZ - " + raceName.toUpperCase() + " Character Configuration",
                "custom_model: Path to custom model file (e.g., 'myrace.geo.json')",
                "has_gender: Whether this race can select male/female",
                "use_vanilla_skin: Use vanilla Minecraft skins instead of custom model",
                "default_*: Default appearance values for this race"
            );
            LogUtil.info(Env.COMMON, "Character config for '{}' created", raceName);
        }

        RaceStatsConfig statsConfig;
        if (Files.exists(statsPath)) {
            statsConfig = LOADER.loadConfig(statsPath, RaceStatsConfig.class);
            LogUtil.info(Env.COMMON, "Stats config for '{}' loaded", raceName);
        } else {
            statsConfig = createDefaultStatsConfig(raceName, isDefault);
            LOADER.saveConfigWithComments(statsPath, statsConfig,
                "DragonMineZ - " + raceName.toUpperCase() + " Stats Configuration",
                "Each class (warrior, spiritualist, martial_artist) has:",
                "  base_stats: Starting values for each stat",
                "  stat_scaling: Multipliers applied when leveling up (1.0 = normal)",
                "Stats: str=Strength, skp=Strike Power, res=Resistance, vit=Vitality, pwr=Ki Power, ene=Energy"
            );
            LogUtil.info(Env.COMMON, "Stats config for '{}' created", raceName);
        }

        Map<String, FormConfig> raceForms = LOADER.loadRaceForms(raceName, formsPath);

        if (isDefault && !LOADER.hasExistingFiles(formsPath)) {
            FORMS_FACTORY.createDefaultFormsForRace(raceName, formsPath, raceForms);
        }

        RACE_FORMS.put(raceName, raceForms);

        RACE_CHARACTER.put(raceName, characterConfig);
        RACE_STATS.put(raceName, statsConfig);
        LOADED_RACES.add(raceName);
    }

    private static boolean isDefaultRace(String raceName) {
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
        config.setDefaultBodyColor("#F5D5A6");
        config.setDefaultBodyColor2("#F5D5A6");
        config.setDefaultBodyColor3("#F5D5A6");
        config.setDefaultHairColor("#000000");
        config.setDefaultEye1Color("#000000");
        config.setDefaultEye2Color("#000000");
        config.setDefaultAuraColor("#FFFFFF");
    }

    private static void setupSaiyanCharacter(RaceCharacterConfig config) {
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
        config.setDefaultEyesType(0);
        config.setDefaultBodyColor("#F5D5A6");
        config.setDefaultBodyColor2("#F5D5A6");
        config.setDefaultBodyColor3("#F5D5A6");
        config.setDefaultHairColor("#000000");
        config.setDefaultEye1Color("#000000");
        config.setDefaultEye2Color("#000000");
        config.setDefaultAuraColor("#FFFFFF");
    }

    private static void setupNamekianCharacter(RaceCharacterConfig config) {
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
        config.setDefaultEyesType(0);
        config.setDefaultBodyColor("#3D7C3F");
        config.setDefaultBodyColor2("#FFCC99");
        config.setDefaultBodyColor3("#FF99CC");
        config.setDefaultHairColor("#3D7C3F");
        config.setDefaultEye1Color("#000000");
        config.setDefaultEye2Color("#000000");
        config.setDefaultAuraColor("#7FFF00");
    }

    private static void setupFrostDemonCharacter(RaceCharacterConfig config) {
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
        config.setDefaultEyesType(0);
        config.setDefaultBodyColor("#F5F5FF");
        config.setDefaultBodyColor2("#B366CC");
        config.setDefaultBodyColor3("#660099");
        config.setDefaultHairColor("#F5F5FF");
        config.setDefaultEye1Color("#FF0000");
        config.setDefaultEye2Color("#000000");
        config.setDefaultAuraColor("#B366CC");
    }

    private static void setupBioAndroidCharacter(RaceCharacterConfig config) {
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
        config.setDefaultEyesType(0);
        config.setDefaultBodyColor("#8BC34A");
        config.setDefaultBodyColor2("#000000");
        config.setDefaultBodyColor3("#FF9800");
        config.setDefaultHairColor("#8BC34A");
        config.setDefaultEye1Color("#FF0000");
        config.setDefaultEye2Color("#000000");
        config.setDefaultAuraColor("#8BC34A");
    }

    private static void setupMajinCharacter(RaceCharacterConfig config) {
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
        config.setDefaultEyesType(0);
        config.setDefaultBodyColor("#FF69B4");
        config.setDefaultBodyColor2("#FF69B4");
        config.setDefaultBodyColor3("#FF69B4");
        config.setDefaultHairColor("#FF69B4");
        config.setDefaultEye1Color("#000000");
        config.setDefaultEye2Color("#000000");
        config.setDefaultAuraColor("#FF69B4");
    }

    private static void setupDefaultCharacter(RaceCharacterConfig config) {
        config.setDefaultBodyType(0);
        config.setDefaultHairType(0);
        config.setDefaultEyesType(0);
        config.setDefaultBodyColor("#F5D5A6");
        config.setDefaultBodyColor2("#F5D5A6");
        config.setDefaultBodyColor3("#F5D5A6");
        config.setDefaultHairColor("#3D2914");
        config.setDefaultEye1Color("#4A3728");
        config.setDefaultEye2Color("#4A3728");
        config.setDefaultAuraColor("#FFFFFF");
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
        setupClassStats(config.getWarrior(), 5, 5, 5, 5, 5, 5);
        setupClassStats(config.getSpiritualist(), 5, 5, 5, 5, 5, 5);
        setupClassStats(config.getMartialArtist(), 5, 5, 5, 5, 5, 5);
    }

    private static void setupSaiyanStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 6, 6, 5, 5, 5, 5);
        setupClassStats(config.getSpiritualist(), 6, 6, 5, 5, 5, 5);
        setupClassStats(config.getMartialArtist(), 6, 6, 5, 5, 5, 5);
    }

    private static void setupNamekianStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 4, 4, 6, 6, 6, 6);
        setupClassStats(config.getSpiritualist(), 4, 4, 6, 6, 6, 6);
        setupClassStats(config.getMartialArtist(), 4, 4, 6, 6, 6, 6);
    }

    private static void setupFrostDemonStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 7, 7, 6, 5, 6, 5);
        setupClassStats(config.getSpiritualist(), 7, 7, 6, 5, 6, 5);
        setupClassStats(config.getMartialArtist(), 7, 7, 6, 5, 6, 5);
    }

    private static void setupBioAndroidStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 5, 5, 5, 5, 6, 6);
        setupClassStats(config.getSpiritualist(), 5, 5, 5, 5, 6, 6);
        setupClassStats(config.getMartialArtist(), 5, 5, 5, 5, 6, 6);
    }

    private static void setupMajinStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 5, 5, 6, 6, 6, 7);
        setupClassStats(config.getSpiritualist(), 5, 5, 6, 6, 6, 7);
        setupClassStats(config.getMartialArtist(), 5, 5, 6, 6, 6, 7);
    }

    private static void setupDefaultStats(RaceStatsConfig config) {
        setupClassStats(config.getWarrior(), 5, 5, 5, 5, 5, 5);
        setupClassStats(config.getSpiritualist(), 5, 5, 5, 5, 5, 5);
        setupClassStats(config.getMartialArtist(), 5, 5, 5, 5, 5, 5);
    }

    private static void setupClassStats(RaceStatsConfig.ClassStats classStats,
                                        int str, int skp, int res, int vit, int pwr, int ene) {
        RaceStatsConfig.BaseStats base = classStats.getBaseStats();
        base.setStrength(str);
        base.setStrikePower(skp);
        base.setResistance(res);
        base.setVitality(vit);
        base.setKiPower(pwr);
        base.setEnergy(ene);

        RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
        scaling.setStrengthScaling(1.0);
        scaling.setStrikePowerScaling(1.0);
        scaling.setStaminaScaling(1.0);
        scaling.setDefenseScaling(1.0);
        scaling.setVitalityScaling(1.0);
        scaling.setKiPowerScaling(1.0);
        scaling.setKiPowerScaling(1.0);
        scaling.setKiPowerScaling(1.0);
        scaling.setEnergyScaling(1.0);
    }


    public static RaceStatsConfig getRaceStats(String raceName) {
        if (SERVER_SYNCED_STATS.containsKey(raceName.toLowerCase())) {
            return SERVER_SYNCED_STATS.get(raceName.toLowerCase());
        }
        return RACE_STATS.getOrDefault(raceName.toLowerCase(), RACE_STATS.get("human"));
    }

    public static RaceCharacterConfig getRaceCharacter(String raceName) {
        if (SERVER_SYNCED_CHARACTER.containsKey(raceName.toLowerCase())) {
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
        return serverConfig != null ? serverConfig : new GeneralServerConfig();
    }

    public static void saveGeneralUserConfig() {
        try {
            Path path = CONFIG_DIR.resolve("general-user.json5");
            LOADER.saveConfig(path, userConfig);
            LogUtil.info(Env.COMMON, "User configuration saved to: {}", path);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving user configuration: {}", e.getMessage());
        }
    }

    public static void saveGeneralServerConfig() {
        try {
            Path path = CONFIG_DIR.resolve("general-server.json5");
            LOADER.saveConfig(path, serverConfig);
            LogUtil.info(Env.COMMON, "Server configuration saved to: {}", path);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving server configuration: {}", e.getMessage());
        }
    }

    public static void saveRaceStats(String raceName) {
        try {
            Path path = RACES_DIR.resolve(raceName).resolve("stats.json5");
            RaceStatsConfig config = RACE_STATS.get(raceName);
            if (config != null) {
                LOADER.saveConfig(path, config);
                LogUtil.info(Env.COMMON, "Stats config for '{}' saved", raceName);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving stats for '{}': {}", raceName, e.getMessage());
        }
    }

    public static void saveRaceCharacter(String raceName) {
        try {
            Path path = RACES_DIR.resolve(raceName).resolve("character.json5");
            RaceCharacterConfig config = RACE_CHARACTER.get(raceName);
            if (config != null) {
                LOADER.saveConfig(path, config);
                LogUtil.info(Env.COMMON, "Character config for '{}' saved", raceName);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving character for '{}': {}", raceName, e.getMessage());
        }
    }

    public static void applySyncedServerConfig(Map<String, ?> syncedStats, Map<String, ?> syncedCharacters) {
        SERVER_SYNCED_STATS.clear();
        SERVER_SYNCED_CHARACTER.clear();

        syncedStats.forEach((raceName, data) -> {
            if (data instanceof com.dragonminez.common.network.S2C.SyncServerConfigS2C.RaceStatsData statsData) {
                SERVER_SYNCED_STATS.put(raceName.toLowerCase(), statsData.toConfig(raceName));
            }
        });

        syncedCharacters.forEach((raceName, data) -> {
            if (data instanceof com.dragonminez.common.network.S2C.SyncServerConfigS2C.RaceCharacterData characterData) {
                SERVER_SYNCED_CHARACTER.put(raceName.toLowerCase(), characterData.toConfig());
            }
        });

        LogUtil.info(Env.COMMON, "Server configuration synced for {} races ({} stats, {} characters)",
            syncedStats.size(), SERVER_SYNCED_STATS.size(), SERVER_SYNCED_CHARACTER.size());
    }

    public static void clearServerSync() {
        SERVER_SYNCED_STATS.clear();
        SERVER_SYNCED_CHARACTER.clear();
        LogUtil.info(Env.COMMON, "Server configuration sync cleared, using local config");
    }

    public static boolean isUsingServerConfig() {
        return !SERVER_SYNCED_STATS.isEmpty();
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
}
