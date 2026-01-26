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
	private static final String CODE_PREFIX_V1 = "DMZ_HAIR:";
	private static final String CODE_PREFIX_V2 = "DMZHair_v2:";

    static {
        initializeDefaultPresets();
    }

    private static void initializeDefaultPresets() {
		// Goku
		registerPreset(1, "DMZHair_v2:8gcUTqeXsS0jsuijpI1VGNoUiW2a7QxMTfXLhaG4zE3gVjeV0a69NHwsAm0ueiNtwcBzZ2gB0Q20ZenhGdlEGt7RLVgveZFpZPxHZ0d8Eeakui3bSx62N4G5J4nCeUID9G9eq1QYa9NIYPjRDGkBmdTFIu7quXwoKkd5GOE9mkRBXFetj2bxjMhTqkmUPQPaTW2NprHrkO8mqw5vtBtLJ7nuqVFyPQScsCQw3ar0BXgxxzEEGrLP5VjWkgOK6r1k4AksoTN9RT88ON1fw0kqZgD9XSn1RAjv77MRUBRKoOnZ64R8yKCSoxy9tzR68JNUXSwZXiPCG26CrSgiX0Z1chDIH0u9eANvPZfEDAyC0OBmgVf0EIHCnaqYuWDmpnvDjMqwxW54Mj391iPldSPiJv3797GAE7mTv4mOgvvDybtmaCWlHUDiOW3EiWOgyvSc9UkQ2DaTTjG6Jb9Lxyzo0bVuHAZttyBYTl32xcTf9jpOFDXL7t41WgKGkKM3kMTmWy1kwVabDVjchaLCwhi5AyRRzTZv3iaRmeV4bPwsTGFpYuar4y30Jz9yyn4lfHdRC436XkyKy1iwoefUG4Gz1aMYdGh3dbiiQjt1LcAnoaOW76vOgFyMZt7CvA3UpRLpeSJxT0TNJi1YntuJkxyl79ouA2DgUjsAElmXgxDeC5kqli8BmLCoiMNRXqzGdRKxfClPjOw0de1bYgvEP8btq69DvNCV4UF4hS1PT1mT7xKe1IJoXSMRcSRTdVSWHvWmRj1E1EWGPEymXWAhFqfGbCuJ6pIY39gnL1I5O75UPpata3I5Ulty9YF0spHxskQcdkBfQiMI3bDOGhIucjg7k6uCWwUXsPMDn8r4HTlk5ykJrPgBHt07nlm6RjKevzgCkfrx3kCGIZHGdOd9NfJATIAm1EvdHQlX04jmxym8pZNWZ2idOtFefHtu7i0cGF9wYRZEBFVSR6Dk7l4PCZJmxt1JLiyqibyWNznmdo3560TNASqqG80eLuoWcycC0lP9rkTCbph3W51M9LrZsCL2NuDwjjPc");
    	// Vegeta
		//registerPreset(2, "DMZ_HAIR:...");
		// Trunks
		registerPreset(3, "DMZHair_v2:109u3NyEBqNT1hlTR4XJ1Tyk4bG7d5naHigcDM6fNgwOLp8Vu1yFt7qrMLRkEpAsCSZ6WPbxoeLbLdF4VMbTTcjF8Muq6BDvdx9mjcHkIdQW3vrcedSao01KfxVfSyw9ARFke5DbijCG7hmPZ0KfHw5CX5kyruspUjLbov2tNPtkrso5mNqA98g5DmqFAaA5Ovz0qeh1jm6FgkrWK1SwOfDQ8gzEcLUX8m3Cma8quXWAnPAWbMxUDkpzdyxdE53JMsrEN8Pfvnp0BvLDB0Ud0Fql5iyiMxQU7PRYA4RfL8ExamfHX2ljXDWxl9C9DoemUZ4MRdE8wshyFhyt3ZYb2Ud46aO9cFd4zDPtS8vrDhxf4o5w81rP5r5rut7fwI68luYLJpRkiWhYE1MhrDcwI8MxEAcYmq2DQxIVnqARinUHYG2KMOTwEQfvUCeKCMc8rQENnoizYncBMY5jfRmi7e6wpmJhPzm69MdYEYb9u8YAWzcZvRFn8KC1mD37LCEcis2FhqKIw2OEdjIxkIRkPycAlTKQ26syx6JSM3rMQHPWLnTGeVl0XPKJJyzM34c3b0VGSQOQmyWgBpv8QxETafedUY8DpCnY2eDuNE3tAauTZxxtgUkM4mVRthM0IQQ01s4lgaQks7DftbDlNQG3rlvSIYjUA7NMr6gdEqpk1Z7m7NabVeaLcUSTl0g7TG7HJn41tWtw5EiXw2qieW");
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

			return CODE_PREFIX_V2 + encodeToNumbers(byteStream.toByteArray());
		} catch (Exception e) {
			return null;
		}
	}

    public static CustomHair fromCode(String code) {
        if (code == null || code.isEmpty()) return null;

		String rawContent;
		boolean isV1 = false;

		if (code.startsWith(CODE_PREFIX_V2)) {
			rawContent = code.substring(CODE_PREFIX_V2.length());
		} else if (code.startsWith(CODE_PREFIX_V1)) {
			rawContent = code.substring(CODE_PREFIX_V1.length());
			isV1 = true;
		} else {
			rawContent = code;
		}

		try {
			byte[] compressed = decodeFromNumbers(rawContent);
			ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
			DataInputStream dataInput = new DataInputStream(new GZIPInputStream(byteStream));
			CompoundTag tag = NbtIo.read(dataInput);
			dataInput.close();

			CustomHair hair = new CustomHair();

			if (isV1 && !tag.contains("Version")) {
				tag.putInt("Version", 1);
			}

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
