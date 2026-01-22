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
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class HairManager {
    private static final String[] DEFAULT_HAIR_RACES = {"human", "saiyan"};
    private static final Map<Integer, String> PRESET_CODES = new HashMap<>();
    private static final String CODE_PREFIX = "DMZ_HAIR:";

    static {
        initializeDefaultPresets();
    }

    private static void initializeDefaultPresets() {
		// Goku
		registerPreset(1, "DMZ_HAIR:1qvy9y6pY7krEjfref7mPTHLZL8LWPF8fCfuqUwkCs7T0z8AgyxmbKSGwMAsZFOdGVog7GUncLpR9UfKgghDiFRRWWwAUhk7gr1sLpCG8D1h2J1xxSuxb07PMnEv8Sz0Ism6ZC92NfGXnlBz8BSTE4r6Z5TOfElRMcy6NlCTnD3xP6fmBFucT2WB4T2iXLS75Ow9yYerwEKWyPk8kNcS3Vz8mHDiDrEvPn8sCofMIesEyLwwmlH7pKo9DCR4HW7iMuZ8kx38gQe73HcJm47NrnRPTTIqyRlUgm7HxWPRBSxE8BYDvgexHTywnjN1suiKdxa7Jr36LoGHXzr1XvZtX13Lmrgtly38AEmPvHMjaQSaFd9e5P5GEU7yNgiaiIJVtstQVKzkpxHvDn57AqmBbVW4OUlWlzNkOLI7ly2hSm3N3AUo4ilEFdpi6ZP1dk0DwnC4yDtE0jwngsmOrsE1qot7w1xv4vcLTCL3dNn0uMRk9ChKDEwI1GAy8dPFcHyibxr9kbnDjmhPwSK5ZxkPxL9H1GtSg46p5OwROutgo6qhR6tDrbsL3BB04DahZ7Wpse5gFILyZ1APa2r5nbfDWl4sIm6DBl2tSKcabZG0rtMn14l31ItJIkEavEW6BoQ97LFzgZons9nr58Sui3szBZZYmj35w9QCHrTtJq6huE4QkYlN4nzjR7jN2ovktHa6R6UofUlyb2UpbWQXk2V7jmMYeW0c7Fto3EkqEyn9126hbjUUxLZA51AVAaDtZPQ2EwPU8clHjUhxEJrbS5QFQA3JxHl9uRDTgD0zA0OJvgoLK7DjOEB87GD5kWh9E2bKEN6xEyIwpy7cASq5OZejbWcvZJbFNl79qYjVYXe7L9TdrtgU0PmQEPgjVfAusbb5rIIwJZeqSUfH1ljE1EmaGcdPu9LAHgng4euZT6Npk3TtiaPOqLfrOsxah6SK9r00BBs4CN0z2ztrUu8slbsZO1TUm7xLBHUtXOmTtz02MXlbpX4grxVnpDlxkA1O6N2vxb4z9BsHoGUHBsxuUi40TRivMWdLYAVq1sZtRe3pPUmFDF7Oe18SrFmUwlKtXz7lGzLbVBIBDp50JVY2YfJZCgLplJBGM7YpsKkdyAtGJKuJiX8lHtsIihDqp094FVstZjUBVWGkLvQb9deGvJB37InuuvpUSQyDRKIn3XkHJLoXKwIcuiJymeJvshifjzI3ls3QlvxwVPkbo1btoQ8FJx6xqSfD3qrTF9GNWo7S0YHblv0s4zUXRg3HxZeXy2AdWO6LSlxgfQQTmf6IG6a79h4Zxamhc");
    	// Vegeta
		//registerPreset(2, "DMZ_HAIR:...");
		// Trunks
		//registerPreset(3, "DMZ_HAIR:...");
		// Gohan
		//registerPreset(4, "DMZ_HAIR:...");
		// Krillin
		//registerPreset(5, "DMZ_HAIR:...");
	}

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    private static String encodeToNumbers(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";

        BigInteger value = new BigInteger(1, bytes);

        if (value.equals(BigInteger.ZERO)) {
            return String.valueOf(BASE62_ALPHABET.charAt(0));
        }

        StringBuilder result = new StringBuilder();
        BigInteger base = BigInteger.valueOf(BASE);

        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = value.divideAndRemainder(base);
            result.append(BASE62_ALPHABET.charAt(divmod[1].intValue()));
            value = divmod[0];
        }

        return result.reverse().toString();
    }

    private static byte[] decodeFromNumbers(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new byte[0];

        BigInteger value = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(BASE);

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            int digit = BASE62_ALPHABET.indexOf(c);

            if (digit < 0) {
                throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
            }

            value = value.multiply(base).add(BigInteger.valueOf(digit));
        }

        byte[] bytes = value.toByteArray();

        if (bytes.length > 1 && bytes[0] == 0) {
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

            return CODE_PREFIX + encodeToNumbers(byteStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static CustomHair fromCode(String code) {
        if (code == null || code.isEmpty()) return null;

        String rawContent = code.startsWith(CODE_PREFIX) ? code.substring(CODE_PREFIX.length()) : code;

        try {
            byte[] compressed = decodeFromNumbers(rawContent);
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
