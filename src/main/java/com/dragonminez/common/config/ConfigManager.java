package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.stats.Character;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create();

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("dragonminez");
    private static final Path RACES_DIR = CONFIG_DIR.resolve("races");

    private static final Map<String, RaceStatsConfig> RACE_CONFIGS = new HashMap<>();
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
        LogUtil.info(Env.COMMON, "Inicializando sistema de configuración de DragonMineZ...");

        try {
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(RACES_DIR);

            loadGeneralConfigs();
            loadRaceConfigs();

            LogUtil.info(Env.COMMON, "Sistema de configuración inicializado correctamente");
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error al inicializar el sistema de configuración: {}", e.getMessage());
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
            LogUtil.info(Env.COMMON, "Configuración de usuario cargada desde: {}", userConfigPath);
        } else {
            userConfig = new GeneralUserConfig();
            saveGeneralUserConfig();
            LogUtil.info(Env.COMMON, "Configuración de usuario creada por defecto en: {}", userConfigPath);
        }

        Path serverConfigPath = CONFIG_DIR.resolve("general-server.json5");
        if (Files.exists(serverConfigPath)) {
            String content = Files.readString(serverConfigPath, StandardCharsets.UTF_8);
            String cleanedJson = cleanJson5(content);
            JsonReader reader = new JsonReader(new StringReader(cleanedJson));
            reader.setLenient(true);
            serverConfig = GSON.fromJson(reader, GeneralServerConfig.class);
            LogUtil.info(Env.COMMON, "Configuración del servidor cargada desde: {}", serverConfigPath);
        } else {
            serverConfig = new GeneralServerConfig();
            saveGeneralServerConfig();
            LogUtil.info(Env.COMMON, "Configuración del servidor creada por defecto en: {}", serverConfigPath);
        }
    }

    private static void loadRaceConfigs() throws IOException {
        for (String raceName : Character.RACE_NAMES) {
            loadRaceConfig(raceName);
        }
    }

    private static void loadRaceConfig(String raceName) throws IOException {
        Path racePath = RACES_DIR.resolve(raceName);
        Files.createDirectories(racePath);

        Path statsPath = racePath.resolve("stats.json5");
        Path formsPath = racePath.resolve("forms");
        Files.createDirectories(formsPath);

        RaceStatsConfig config;
        if (Files.exists(statsPath)) {
            String content = Files.readString(statsPath, StandardCharsets.UTF_8);
            String cleanedJson = cleanJson5(content);
            JsonReader reader = new JsonReader(new StringReader(cleanedJson));
            reader.setLenient(true);
            config = GSON.fromJson(reader, RaceStatsConfig.class);
            LogUtil.info(Env.COMMON, "Configuración de raza '{}' cargada desde: {}", raceName, statsPath);
        } else {
            config = createDefaultRaceConfig(raceName);
            String json5Content = GSON.toJson(config);
            Files.writeString(statsPath, json5Content, StandardCharsets.UTF_8);
            LogUtil.info(Env.COMMON, "Configuración de raza '{}' creada por defecto en: {}", raceName, statsPath);
        }

        RACE_CONFIGS.put(raceName, config);

        Path superformPath = formsPath.resolve("superform.json5");
        if (!Files.exists(superformPath)) {
            Files.writeString(superformPath, "{\n  // Configuración de transformaciones - Próximamente\n}", StandardCharsets.UTF_8);
        }
    }

    private static RaceStatsConfig createDefaultRaceConfig(String raceName) {
        RaceStatsConfig config = new RaceStatsConfig();
        config.setRaceName(raceName);

        int raceId = Character.getRaceIdByName(raceName);
        config.setHasGender(raceId >= 0 && raceId < Character.HAS_GENDER.length
            ? Character.HAS_GENDER[raceId]
            : true);

        switch (raceName) {
            case "human" -> {
                config.setDescription("Los humanos son versátiles y equilibrados, con buen potencial en todas las áreas.");
                setupHumanStats(config);
            }
            case "saiyan" -> {
                config.setDescription("Los Saiyans son guerreros natos con gran poder físico y potencial de transformación.");
                setupSaiyanStats(config);
            }
            case "namekian" -> {
                config.setDescription("Los Namekianos destacan en regeneración y técnicas espirituales.");
                setupNamekianStats(config);
            }
            case "colddemon" -> {
                config.setDescription("Los Cold Demons poseen inmenso poder natural y alta resistencia.");
                setupColdDemonStats(config);
            }
            case "bioandroid" -> {
                config.setDescription("Los Bio Androides son adaptables con capacidad de absorber poder.");
                setupBioAndroidStats(config);
            }
            case "majin" -> {
                config.setDescription("Los Majin tienen gran energía mágica y habilidades de regeneración.");
                setupMajinStats(config);
            }
        }

        return config;
    }

    private static void setupHumanStats(RaceStatsConfig config) {
        setupDefaultClassStats(config);
    }

    private static void setupSaiyanStats(RaceStatsConfig config) {
        setupDefaultClassStats(config);
    }

    private static void setupNamekianStats(RaceStatsConfig config) {
        setupDefaultClassStats(config);
    }

    private static void setupColdDemonStats(RaceStatsConfig config) {
        setupDefaultClassStats(config);
    }

    private static void setupBioAndroidStats(RaceStatsConfig config) {
        setupDefaultClassStats(config);
    }

    private static void setupMajinStats(RaceStatsConfig config) {
        setupDefaultClassStats(config);
    }

    private static void setupDefaultClassStats(RaceStatsConfig config) {
        var warrior = config.getClassConfig("warrior");
        warrior.getBaseStats().setStrength(5);
        warrior.getBaseStats().setStrikePower(5);
        warrior.getBaseStats().setResistance(5);
        warrior.getBaseStats().setVitality(5);
        warrior.getBaseStats().setKiPower(5);
        warrior.getBaseStats().setEnergy(5);
        warrior.getStatScaling().setStrengthScaling(1.5);
        warrior.getStatScaling().setStrikePowerScaling(1.5);
        warrior.getStatScaling().setStaminaScaling(2.5);
        warrior.getStatScaling().setDefenseScaling(0.6);
        warrior.getStatScaling().setVitalityScaling(1.2);
        warrior.getStatScaling().setKiPowerScaling(0.8);
        warrior.getStatScaling().setEnergyScaling(0.8);

        var spiritualist = config.getClassConfig("spiritualist");
        spiritualist.getBaseStats().setStrength(5);
        spiritualist.getBaseStats().setStrikePower(5);
        spiritualist.getBaseStats().setResistance(5);
        spiritualist.getBaseStats().setVitality(5);
        spiritualist.getBaseStats().setKiPower(5);
        spiritualist.getBaseStats().setEnergy(5);
        spiritualist.getStatScaling().setStrengthScaling(0.8);
        spiritualist.getStatScaling().setStrikePowerScaling(0.8);
        spiritualist.getStatScaling().setStaminaScaling(2.0);
        spiritualist.getStatScaling().setDefenseScaling(0.5);
        spiritualist.getStatScaling().setVitalityScaling(1.0);
        spiritualist.getStatScaling().setKiPowerScaling(1.5);
        spiritualist.getStatScaling().setEnergyScaling(1.5);

        var martialartist = config.getClassConfig("martialartist");
        martialartist.getBaseStats().setStrength(5);
        martialartist.getBaseStats().setStrikePower(5);
        martialartist.getBaseStats().setResistance(5);
        martialartist.getBaseStats().setVitality(5);
        martialartist.getBaseStats().setKiPower(5);
        martialartist.getBaseStats().setEnergy(5);
        martialartist.getStatScaling().setStrengthScaling(1.2);
        martialartist.getStatScaling().setStrikePowerScaling(1.2);
        martialartist.getStatScaling().setStaminaScaling(2.2);
        martialartist.getStatScaling().setDefenseScaling(0.55);
        martialartist.getStatScaling().setVitalityScaling(1.1);
        martialartist.getStatScaling().setKiPowerScaling(1.2);
        martialartist.getStatScaling().setEnergyScaling(1.2);
    }

    public static RaceStatsConfig getRaceConfig(String raceName) {
        return RACE_CONFIGS.getOrDefault(raceName, RACE_CONFIGS.get("human"));
    }

    public static RaceStatsConfig getRaceConfig(int raceId) {
        if (raceId >= 0 && raceId < Character.RACE_NAMES.length) {
            return getRaceConfig(Character.RACE_NAMES[raceId]);
        }
        return getRaceConfig("human");
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
            LogUtil.info(Env.COMMON, "Configuración de usuario guardada en: {}", path);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error al guardar la configuración de usuario: {}", e.getMessage());
        }
    }

    public static void saveGeneralServerConfig() {
        try {
            Path path = CONFIG_DIR.resolve("general-server.json5");
            String json5Content = GSON.toJson(serverConfig);
            Files.writeString(path, json5Content, StandardCharsets.UTF_8);
            LogUtil.info(Env.COMMON, "Configuración del servidor guardada en: {}", path);
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error al guardar la configuración del servidor: {}", e.getMessage());
        }
    }

    public static void saveRaceConfig(String raceName) {
        try {
            Path path = RACES_DIR.resolve(raceName).resolve("stats.json5");
            RaceStatsConfig config = RACE_CONFIGS.get(raceName);
            if (config != null) {
                String json5Content = GSON.toJson(config);
                Files.writeString(path, json5Content, StandardCharsets.UTF_8);
                LogUtil.info(Env.COMMON, "Configuración de raza '{}' guardada en: {}", raceName, path);
            }
        } catch (IOException e) {
            LogUtil.error(Env.COMMON, "Error al guardar la configuración de la raza '{}': {}", raceName, e.getMessage());
        }
    }
}

