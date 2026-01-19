package com.dragonminez.common.hair;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.Character;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class HairManager {
    private static final String[] DEFAULT_HAIR_RACES = {"human", "saiyan"};
    private static final Map<Integer, String> PRESET_CODES = new HashMap<>();
    private static final String CODE_PREFIX = "DMZ_HAIR:";

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    static {
        initializeDefaultPresets();
    }

    private static void initializeDefaultPresets() {

    }

    /**
     * Convierte los bytes del NBT a una cadena de solo letras (a-z).
     */
    private static String encodeToLetters(byte[] bytes) {
        // Añadimos un byte extra para asegurar que el número sea interpretado como positivo
        byte[] positiveBytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, positiveBytes, 1, bytes.length);
        BigInteger number = new BigInteger(positiveBytes);

        StringBuilder sb = new StringBuilder();
        BigInteger base = BigInteger.valueOf(26);

        while (number.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] quotientAndRemainder = number.divideAndRemainder(base);
            sb.append(ALPHABET.charAt(quotientAndRemainder[1].intValue()));
            number = quotientAndRemainder[0];
        }
        return sb.reverse().toString();
    }

    /**
     * Convierte la cadena de letras de vuelta a bytes para cargar el NBT.
     */
    private static byte[] decodeFromLetters(String s) {
        BigInteger number = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(26);

        for (int i = 0; i < s.length(); i++) {
            int digit = ALPHABET.indexOf(s.charAt(i));
            if (digit == -1) continue;
            number = number.multiply(base).add(BigInteger.valueOf(digit));
        }

        byte[] bytes = number.toByteArray();
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] result = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, result, 0, result.length);
            return result;
        }
        return bytes;
    }

    public static String toCode(CustomHair hair) {
        if (hair == null) return null;

        try {
            CompoundTag tag = hair.save();
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(new GZIPOutputStream(byteStream));
            NbtIo.write(tag, dataOutput);
            dataOutput.close();

            return CODE_PREFIX + encodeToLetters(byteStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static CustomHair fromCode(String code) {
        if (code == null || code.isEmpty()) return null;

        String rawContent = code.startsWith(CODE_PREFIX) ? code.substring(CODE_PREFIX.length()) : code;

        try {
            byte[] compressed = decodeFromLetters(rawContent);
            ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
            DataInputStream dataInput = new DataInputStream(new GZIPInputStream(byteStream));
            CompoundTag tag = NbtIo.read(dataInput);
            dataInput.close();

            CustomHair hair = new CustomHair();
            hair.load(tag);
            return hair;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean canUseHair(Character character) {
        if (character == null) return false;

        String race = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();

        for (String defaultRace : DEFAULT_HAIR_RACES) {
            if (race.equals(defaultRace)) return true;
        }

        if (race.equals("majin")) return gender.equals(Character.GENDER_FEMALE);
        if (race.equals("bioandroid") || race.equals("frostdemon") || race.equals("namekian")) return false;

        RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
        return config != null && config.canUseHair();
    }

    public static CustomHair getEffectiveHair(Character character) {
        if (character == null || !canUseHair(character)) return null;

        int hairId = character.getHairId();

        if (hairId == 0) {
            CustomHair custom = character.getCustomHair();
            if (custom == null) {
                custom = new CustomHair();
                character.setCustomHair(custom);
            }
            return custom;
        }

        return getPresetHair(hairId, character.getHairColor());
    }

    public static CustomHair getPresetHair(int presetId, String hairColor) {
        String code = PRESET_CODES.get(presetId);

        if (code != null) {
            CustomHair hair = fromCode(code);
            if (hair != null) {
                if (hairColor != null && !hairColor.isEmpty()) {
                    hair.setGlobalColor(hairColor);
                }
                return hair;
            }
        }

        CustomHair basic = new CustomHair();
        if (hairColor != null && !hairColor.isEmpty()) {
            basic.setGlobalColor(hairColor);
        }
        return basic;
    }

    public static void registerPreset(int presetId, String code) {
        if (presetId > 0 && code != null && !code.isEmpty()) {
            PRESET_CODES.put(presetId, code);
        }
    }

    public static int getPresetCount() {
        return PRESET_CODES.size();
    }

    public static boolean isValidCode(String code) {
        return fromCode(code) != null;
    }
}
