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
		registerPreset(1, "DMZ_HAIR:1EsjS8N8fJ1zQCTewaqwJy2nV19XxhwF4pmF70rZO7bZ6J2ZZfHuqsuXSpnpzhtqTTHdvxUijTdZHWy0C8FXtW3uOrBold6CgPYzrsMoz9wpTeSg6EUdbc4pkpMqyo2jm8DYRgBy8RWvaAa2HlNwogTlwkCvaOyBjPCNXBely6O8PaQFrj47EuZNrPj83osOY6MXNk7huPxONdUmToCY20AgoqmEMj6RKmJsvxddMbrAcaCvIySMHk08AV8TKjd8Awjaxusw3K8yRr2OvO6nvmlXp3qeTTjpSn1ynE4T87X0ZuAJ2wZJeUXze0OffXnvw712sUJWVH7eSrcwRPQYOPQAwsDhi9NI77YqHGcJQRfrwgjS5ARfj9xvNs6wX7d2vqrgcQRQDBvMx8jU13XKAE4FYSDmV02G8m1AtLUVNIegiQEqOqu0cbfshCYpdbwfhWZP0C0rSuovCmyOfyaNFe9t34fOD97GXqND42hCxGFYrYfiQP9zH0RH72yOqbjVSB6nDySQo0cW1JGaBf3HrlXLwQdZuyHKNW0g1ZtjnEWyiXNNLYtuVQlElIDZEj3bYyQ27K94TjfnIVrZhzxusOLQnigijsO85aDDvmSLDsxyw2HbEf6lvq6P41AzBnNkexqLHnp5W4J5dDlfHx20CikMLjlyN7EyaE85di6wuKbp1KfIfBX5fddIahQZlqN9ccPpT8V6jkTq7cWaMUqFXqQorwRn17DktFfaThCmQY4Ku9YBZ20yhMfhnBQG44HWcVA0kxcCY7K06Zjhhqp03plbtsrPIBTwRYifrkcyVuFJkUgUCb0kfpMRpn8zCg0vlRqo22RSPW8zz6MmBOBw0OfHevGvhCpgoXy4QBlWwLNTuR3qUy46Gj7TtqDh08lrHZLsn7SIFmFKaIvuYCimjIlcIHPgW4xQfuMjwuoXO4XBTOlu6uOg4dmWEIxZTxeAxVyotLyGJ8TaPhmjFFi7kuyhp6rAr5CDwUY11ryO88CiCm7jUhaH4fzLubGroCfulSZOf767fahiwW0KcNafC3N0d3E9ws7jocweSitwxYInCRQDvbbb9Krlr2woM1bV0vOVBp9aSF7Pbd592FXJOM0b3WNUqmCQp8EhZLcyRI64ojhdiMJvaE93KYlUwn4gpNPEG35UOv9UXPpEi1TikJBlAsuBDWbQJ2XLvgetr6MBfdP6mtwOwyvJR8mP6xfvKHHpaLRvZPymmd4BbaXAmMvoAmXUNdB3DxynO3Y5WHeLWH8MnYY8E0etMuZdmOsP7oll82iJnAESiPgeK5mJdRAYTKSEVw5zNzWHiHDFT2ec3nO8AcdB6nxoOKQa");
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
