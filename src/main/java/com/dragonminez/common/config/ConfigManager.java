package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create();

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
    private static final Path RACES_DIR = CONFIG_DIR.resolve("races");

    private static final String[] DEFAULT_RACES  = {"human", "saiyan", "namekian", "frostdemon", "bioandroid", "majin"};
    private static final Set<String> RACES_WITH_GENDER = new HashSet<>(Arrays.asList("human", "saiyan", "majin"));

    private static final Map<String, RaceStatsConfig> RACE_STATS = new HashMap<>();
    private static final Map<String, RaceCharacterConfig> RACE_CHARACTER = new HashMap<>();
    private static final List<String> LOADED_RACES = new ArrayList<>();

    private static final Map<String, RaceStatsConfig> SERVER_SYNCED_STATS = new HashMap<>();

    private static GeneralUserConfig userConfig;
    private static GeneralServerConfig serverConfig;

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//.*");
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern TRAILING_COMMA = Pattern.compile(",\\s*([}\\]])");

    private static String cleanJson5(String json5) {
        String json = json5;
        json = MULTI_LINE_COMMENT.matcher(json).replaceAll("");
        json = SINGLE_LINE_COMMENT.matcher(json).replaceAll("");
        json = TRAILING_COMMA.matcher(json).replaceAll("$1");
        return json;
    }

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
            String content = Files.readString(userConfigPath, StandardCharsets.UTF_8);
            String cleanedJson = cleanJson5(content);
            JsonReader reader = new JsonReader(new StringReader(cleanedJson));
            reader.setLenient(true);
            userConfig = GSON.fromJson(reader, GeneralUserConfig.class);
            LogUtil.info(Env.COMMON, "User configuration loaded from: {}", userConfigPath);
        } else {
            userConfig = new GeneralUserConfig();
            saveGeneralUserConfig();
            LogUtil.info(Env.COMMON, "Default user configuration created at: {}", userConfigPath);
        }

        Path serverConfigPath = CONFIG_DIR.resolve("general-server.json5");
        if (Files.exists(serverConfigPath)) {
            String content = Files.readString(serverConfigPath, StandardCharsets.UTF_8);
            String cleanedJson = cleanJson5(content);
            JsonReader reader = new JsonReader(new StringReader(cleanedJson));
            reader.setLenient(true);
            serverConfig = GSON.fromJson(reader, GeneralServerConfig.class);
            LogUtil.info(Env.COMMON, "Server configuration loaded from: {}", serverConfigPath);
        } else {
            serverConfig = new GeneralServerConfig();
            saveGeneralServerConfig();
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

    private static void createOrLoadRace(String raceName, boolean isVanilla) throws IOException {
        Path racePath = RACES_DIR.resolve(raceName);
        Files.createDirectories(racePath);

        Path characterPath = racePath.resolve("character.json5");
        Path statsPath = racePath.resolve("stats.json5");
        Path formsPath = racePath.resolve("forms");
        Files.createDirectories(formsPath);

        RaceCharacterConfig characterConfig;
        if (Files.exists(characterPath)) {
            String content = Files.readString(characterPath, StandardCharsets.UTF_8);
            String cleanedJson = cleanJson5(content);
            JsonReader reader = new JsonReader(new StringReader(cleanedJson));
            reader.setLenient(true);
            characterConfig = GSON.fromJson(reader, RaceCharacterConfig.class);
            LogUtil.info(Env.COMMON, "Character config for '{}' loaded", raceName);
        } else {
            characterConfig = createDefaultCharacterConfig(raceName, isVanilla);
            String json5Content = GSON.toJson(characterConfig);
            Files.writeString(characterPath, json5Content, StandardCharsets.UTF_8);
            LogUtil.info(Env.COMMON, "Character config for '{}' created", raceName);
        }

        RaceStatsConfig statsConfig;
        if (Files.exists(statsPath)) {
            String content = Files.readString(statsPath, StandardCharsets.UTF_8);
            String cleanedJson = cleanJson5(content);
            JsonReader reader = new JsonReader(new StringReader(cleanedJson));
            reader.setLenient(true);
            statsConfig = GSON.fromJson(reader, RaceStatsConfig.class);
            LogUtil.info(Env.COMMON, "Stats config for '{}' loaded", raceName);
        } else {
            statsConfig = createDefaultStatsConfig(raceName, isVanilla);
            String json5Content = GSON.toJson(statsConfig);
            Files.writeString(statsPath, json5Content, StandardCharsets.UTF_8);
            LogUtil.info(Env.COMMON, "Stats config for '{}' created", raceName);
        }

        Path superformPath = formsPath.resolve("superform.json5");
        if (!Files.exists(superformPath)) {
            Files.writeString(superformPath, "{\n  // Transformation configuration - Coming soon\n}", StandardCharsets.UTF_8);
        }

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

        if (isDefault) {
            boolean hasGender = RACES_WITH_GENDER.contains(raceName.toLowerCase());
            config.setHasGender(hasGender);
        } else {
            config.setHasGender(true);
        }

        return config;
    }

    private static RaceStatsConfig createDefaultStatsConfig(String raceName, boolean isVanilla) {
        RaceStatsConfig config = new RaceStatsConfig();
        config.setRaceName(raceName);

        RaceStatsConfig.BaseStats baseStats = config.getBaseStats();
        RaceStatsConfig.StatScaling statScaling = config.getStatScaling();

        if (isVanilla) {
            switch (raceName.toLowerCase()) {
                case "human" -> setupHumanStats(baseStats, statScaling);
                case "saiyan" -> setupSaiyanStats(baseStats, statScaling);
                case "namekian" -> setupNamekianStats(baseStats, statScaling);
                case "frostdemon" -> setupFrostDemonStats(baseStats, statScaling);
                case "bioandroid" -> setupBioAndroidStats(baseStats, statScaling);
                case "majin" -> setupMajinStats(baseStats, statScaling);
            }
        } else {
            setupDefaultStats(baseStats, statScaling);
        }

        return config;
    }

    private static void setupHumanStats(RaceStatsConfig.BaseStats base, RaceStatsConfig.StatScaling scaling) {
        base.setStrength(5);
        base.setStrikePower(5);
        base.setResistance(5);
        base.setVitality(5);
        base.setKiPower(5);
        base.setEnergy(5);

        scaling.setStrengthScaling(1.2);
        scaling.setStrikePowerScaling(1.2);
        scaling.setStaminaScaling(2.2);
        scaling.setDefenseScaling(0.55);
        scaling.setVitalityScaling(1.1);
        scaling.setKiPowerScaling(1.2);
        scaling.setEnergyScaling(1.2);
    }

    private static void setupSaiyanStats(RaceStatsConfig.BaseStats base, RaceStatsConfig.StatScaling scaling) {
        base.setStrength(6);
        base.setStrikePower(6);
        base.setResistance(5);
        base.setVitality(5);
        base.setKiPower(5);
        base.setEnergy(5);

        scaling.setStrengthScaling(1.5);
        scaling.setStrikePowerScaling(1.5);
        scaling.setStaminaScaling(2.5);
        scaling.setDefenseScaling(0.6);
        scaling.setVitalityScaling(1.2);
        scaling.setKiPowerScaling(1.3);
        scaling.setEnergyScaling(1.2);
    }

    private static void setupNamekianStats(RaceStatsConfig.BaseStats base, RaceStatsConfig.StatScaling scaling) {
        base.setStrength(4);
        base.setStrikePower(4);
        base.setResistance(6);
        base.setVitality(6);
        base.setKiPower(6);
        base.setEnergy(6);

        scaling.setStrengthScaling(1.0);
        scaling.setStrikePowerScaling(1.0);
        scaling.setStaminaScaling(2.3);
        scaling.setDefenseScaling(0.6);
        scaling.setVitalityScaling(1.4);
        scaling.setKiPowerScaling(1.5);
        scaling.setEnergyScaling(1.5);
    }

    private static void setupFrostDemonStats(RaceStatsConfig.BaseStats base, RaceStatsConfig.StatScaling scaling) {
        base.setStrength(7);
        base.setStrikePower(7);
        base.setResistance(6);
        base.setVitality(5);
        base.setKiPower(6);
        base.setEnergy(5);

        scaling.setStrengthScaling(1.4);
        scaling.setStrikePowerScaling(1.4);
        scaling.setStaminaScaling(2.4);
        scaling.setDefenseScaling(0.65);
        scaling.setVitalityScaling(1.3);
        scaling.setKiPowerScaling(1.4);
        scaling.setEnergyScaling(1.3);
    }

    private static void setupBioAndroidStats(RaceStatsConfig.BaseStats base, RaceStatsConfig.StatScaling scaling) {
        base.setStrength(5);
        base.setStrikePower(5);
        base.setResistance(5);
        base.setVitality(5);
        base.setKiPower(6);
        base.setEnergy(6);

        scaling.setStrengthScaling(1.3);
        scaling.setStrikePowerScaling(1.3);
        scaling.setStaminaScaling(2.3);
        scaling.setDefenseScaling(0.58);
        scaling.setVitalityScaling(1.2);
        scaling.setKiPowerScaling(1.4);
        scaling.setEnergyScaling(1.4);
    }

    private static void setupMajinStats(RaceStatsConfig.BaseStats base, RaceStatsConfig.StatScaling scaling) {
        base.setStrength(5);
        base.setStrikePower(5);
        base.setResistance(6);
        base.setVitality(6);
        base.setKiPower(6);
        base.setEnergy(7);

        scaling.setStrengthScaling(1.2);
        scaling.setStrikePowerScaling(1.2);
        scaling.setStaminaScaling(2.5);
        scaling.setDefenseScaling(0.6);
        scaling.setVitalityScaling(1.3);
        scaling.setKiPowerScaling(1.5);
        scaling.setEnergyScaling(1.6);
    }

    private static void setupDefaultStats(RaceStatsConfig.BaseStats base, RaceStatsConfig.StatScaling scaling) {
        base.setStrength(5);
        base.setStrikePower(5);
        base.setResistance(5);
        base.setVitality(5);
        base.setKiPower(5);
        base.setEnergy(5);

        scaling.setStrengthScaling(1.2);
        scaling.setStrikePowerScaling(1.2);
        scaling.setStaminaScaling(2.2);
        scaling.setDefenseScaling(0.55);
        scaling.setVitalityScaling(1.1);
        scaling.setKiPowerScaling(1.2);
        scaling.setEnergyScaling(1.2);
    }

    public static RaceStatsConfig getRaceStats(String raceName) {
        if (SERVER_SYNCED_STATS.containsKey(raceName.toLowerCase())) {
            return SERVER_SYNCED_STATS.get(raceName.toLowerCase());
        }
        return RACE_STATS.getOrDefault(raceName.toLowerCase(), RACE_STATS.get("human"));
    }

    public static RaceCharacterConfig getRaceCharacter(String raceName) {
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
            String json5Content = GSON.toJson(userConfig);
            Files.writeString(path, json5Content, StandardCharsets.UTF_8);
            LogUtil.info(Env.COMMON, "User configuration saved to: {}", path);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving user configuration: {}", e.getMessage());
        }
    }

    public static void saveGeneralServerConfig() {
        try {
            Path path = CONFIG_DIR.resolve("general-server.json5");
            String json5Content = GSON.toJson(serverConfig);
            Files.writeString(path, json5Content, StandardCharsets.UTF_8);
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
                String json5Content = GSON.toJson(config);
                Files.writeString(path, json5Content, StandardCharsets.UTF_8);
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
                String json5Content = GSON.toJson(config);
                Files.writeString(path, json5Content, StandardCharsets.UTF_8);
                LogUtil.info(Env.COMMON, "Character config for '{}' saved", raceName);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error saving character for '{}': {}", raceName, e.getMessage());
        }
    }

    public static void applySyncedServerConfig(Map<String, ?> syncedStats) {
        SERVER_SYNCED_STATS.clear();
        syncedStats.forEach((raceName, data) -> {
            if (data instanceof com.dragonminez.common.network.S2C.SyncServerConfigS2C.RaceStatsData) {
                var statsData = (com.dragonminez.common.network.S2C.SyncServerConfigS2C.RaceStatsData) data;
                SERVER_SYNCED_STATS.put(raceName.toLowerCase(), statsData.toConfig(raceName));
            }
        });
        LogUtil.info(Env.COMMON, "Server configuration synced for {} races", SERVER_SYNCED_STATS.size());
    }

    public static void clearServerSync() {
        SERVER_SYNCED_STATS.clear();
        LogUtil.info(Env.COMMON, "Server configuration sync cleared, using local config");
    }

    public static boolean isUsingServerConfig() {
        return !SERVER_SYNCED_STATS.isEmpty();
    }

    public static Map<String, RaceStatsConfig> getAllRaceStats() {
        return new HashMap<>(RACE_STATS);
    }
}

