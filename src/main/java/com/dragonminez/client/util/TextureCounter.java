package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class TextureCounter {

    private static final HashMap<String, ResourceLocation> CACHE = new HashMap<>();
    private static final Map<String, Integer> BODY_TYPE_CACHE = new HashMap<>();
    private static final Map<String, Integer> HAIR_TYPE_CACHE = new HashMap<>();
    private static final Map<String, Integer> EYES_TYPE_CACHE = new HashMap<>();
    private static final Map<String, Integer> NOSE_TYPE_CACHE = new HashMap<>();
    private static final Map<String, Integer> MOUTH_TYPE_CACHE = new HashMap<>();
    private static final Map<String, Integer> TATTOO_TYPE_CACHE = new HashMap<>();

    public static ResourceLocation cache(String namespace, String path) {
        return CACHE.computeIfAbsent(namespace + ":" + path, ResourceLocation::new);
    }

    private static String normalizeRace(String race) {
        String lower = race.toLowerCase();
        if (lower.startsWith("frostdemon")) return "frostdemon";
        if (lower.startsWith("bioandroid")) return "bioandroid";
        if (lower.startsWith("majin")) return "majin";
        if (lower.startsWith("namekian")) return "namekian";
        if (lower.equals("saiyan_ssj4")) return "saiyan";
        return lower;
    }

    public static int getMaxBodyTypes(String race, String gender) {
        String normalizedRace = normalizeRace(race);
        String genderNormalized = (gender.equals("female") || gender.equals("mujer")) ? "female" : "male";
        String key = normalizedRace + "_" + genderNormalized;
        if (BODY_TYPE_CACHE.containsKey(key)) return BODY_TYPE_CACHE.get(key);
        int count = countBodyTextures(normalizedRace, genderNormalized);
        BODY_TYPE_CACHE.put(key, count);
        return count;
    }

    public static int getMaxEyesTypes(String race) {
        String normalizedRace = normalizeRace(race);
        if (EYES_TYPE_CACHE.containsKey(normalizedRace)) return EYES_TYPE_CACHE.get(normalizedRace);
        int count = countFaceTextures(normalizedRace, "eye");
        EYES_TYPE_CACHE.put(normalizedRace, count);
        return count;
    }

    public static int getMaxNoseTypes(String race) {
        String normalizedRace = normalizeRace(race);
        if (NOSE_TYPE_CACHE.containsKey(normalizedRace)) return NOSE_TYPE_CACHE.get(normalizedRace);
        int count = countFaceTextures(normalizedRace, "nose");
        NOSE_TYPE_CACHE.put(normalizedRace, count);
        return count;
    }

    public static int getMaxMouthTypes(String race) {
        String normalizedRace = normalizeRace(race);
        if (MOUTH_TYPE_CACHE.containsKey(normalizedRace)) return MOUTH_TYPE_CACHE.get(normalizedRace);
        int count = countFaceTextures(normalizedRace, "mouth");
        MOUTH_TYPE_CACHE.put(normalizedRace, count);
        return count;
    }

    public static int getMaxTattooTypes(String race) {
        String normalizedRace = normalizeRace(race);
        if (TATTOO_TYPE_CACHE.containsKey(normalizedRace)) return TATTOO_TYPE_CACHE.get(normalizedRace);
        int count = countTattooTextures();
        TATTOO_TYPE_CACHE.put(normalizedRace, count);
        return count;
    }

    private static int countBodyTextures(String race, String gender) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        int count = 0;

        boolean isLayeredRace = race.equals("namekian") || race.equals("frostdemon") || race.equals("bioandroid");

        if (isLayeredRace) {
            for (int i = 0; i <= 100; i++) {
                String basePath = "textures/entity/races/" + race + "/bodytype_" + i + "_layer1.png";
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath);
                if (resourceManager.getResource(location).isPresent()) count++;
                else break;
            }
            return count > 0 ? count - 1 : -1;
        } else {
            boolean usesVanillaSkin = race.equals("human") || race.equals("saiyan");
            int startIndex = usesVanillaSkin ? 1 : 0;

            String basePath = getBasePathForBodyType(race, gender);
            for (int i = startIndex; i <= 100; i++) {
                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + i + ".png");
                if (resourceManager.getResource(location).isPresent()) count++;
                else break;
            }
            if (count > 0) return usesVanillaSkin ? count : count - 1;
            return -1;
        }
    }

    private static int countFaceTextures(String race, String type) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        int count = 0;

        String raceFolder = race.equals("human") || race.equals("saiyan") ? "humansaiyan" : race;
        String basePath = "textures/entity/races/" + raceFolder + "/faces/" + raceFolder + "_" + type + "_";

        boolean isEyes = type.equals("eye");
        String suffix = isEyes ? "_0.png" : ".png";

        for (int i = 0; i <= 100; i++) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + i + suffix);

            if (resourceManager.getResource(location).isPresent()) count++;
            else break;
        }

        return count > 0 ? count - 1 : 0;
    }

    private static int countTattooTextures() {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        int count = 0;

        String basePath = "textures/entity/races/tattoos/tattoo_";

        for (int i = 0; i <= 100; i++) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + i + ".png");
            if (resourceManager.getResource(location).isPresent()) count++;
            else break;
        }

        return count > 0 ? count - 1 : 0;
    }

    private static String getBasePathForBodyType(String race, String gender) {
        if (race.equals("human") || race.equals("saiyan")) {
            return "textures/entity/races/humansaiyan/bodytype_" + gender + "_";
        }

        if (race.equals("majin")) {
            return "textures/entity/races/majin/bodytype_" + gender + "_";
        }

        return "textures/entity/races/" + race + "/bodytype_";
    }

    public static void clearCache() {
        CACHE.clear();
        BODY_TYPE_CACHE.clear();
        HAIR_TYPE_CACHE.clear();
        EYES_TYPE_CACHE.clear();
        NOSE_TYPE_CACHE.clear();
        MOUTH_TYPE_CACHE.clear();
        TATTOO_TYPE_CACHE.clear();
    }
}
