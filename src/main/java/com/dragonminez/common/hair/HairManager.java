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
		registerPreset(2, "DMZHair_v2:1hdD4igCuI8AsFbxBWw7OVGg2iCkHKD5espevJBKStFXbJUgFMgRI6LBwfPkamwk1F87ZwoBopo1RwzQqWswsJyMIteFMnX1CgQSuCQHJqvEpKCeli33e7QiCSJ27leK2BdwqXdWAIg7iWHZBBkv3i04AblnNBv3o2u9tNUt9mbHHkdPAG4Ed9LneOnnU8FbEc6fQgbp5IxGJnlChFET4dLZRA7KExhq43tqWVguf9Y4lrcHQ9O1on7KvXAzhY9RKMn5TKz8XMTJVLh1LELvECE0qwrjeGK0FA2y3nvL0f2BNRyOp0Okq6B06JKOttlVq7QEe5WZIwLX29l2nC5QYKqFVgfCU2EzM0JipJNAQZZK3wJQxsIYry4Zgu8AxpaNoEWPZQJnF8BIfK1m8QPyAcqXbyU7aV6FEtdvVP8tWXlh445Hru9ldsA4A2OkKQndjxSiYzC2cifzXF0l8yJOgphKkq7OFJRXiT7mPUgBZu4hyJHBmQr7v2UITdr7Mtpn87t2gezGiLqaIFDqG7F1Trk310jNlJi0RFOLXoLIEjgQXBfvnUkymIp0M0bILbJglgrCnobpSK2ja0bwTpEyHoLbFkux5vIyJra2EJTHB2iF98H2Vvw2Ex6Y9Eder1NP3Y7nqty1yArE0vY9506phKLRmz88RFbTrRt0qu7JXia6svZGdURt7dgt6rGe1ndZrSQk34kVJjn6AlraLw2ktehN2qAz3AEwb5K86CWHXV2KYsgOAx704zDyR4RD5M2okDfKgULLw1NXCCwm5duYDcHwP4ZMBLZo468XiWRCeklEOhCQOV05Z9JwHXHlDY0AdBUDiXDnSQMGk0LFMz3DRbmLPTFzVwMBNrV8ecJNPkU8j5UCi0ATi4D2I8P3S1xZqVKdzfmP9yABNVHY3hrLSyrWFSgar4isSu0mPJrw0NE3B60sH04Kbaaf4LDAcJaT0YV2VnnARPVktuNour1Igp6YGQylvSM0DmJ4YHk4wfVVZ0nKsdaPKK2V2Nz2mJs6sQTIHsGomW0hg7jovGWGdH4qu3s331eiolNQxD4HnYVFS3zBmzZsDALWUs3wpIYHCYfXj4HsIr2UnUMO5JfJejqtj07Gfsi9ZN38dCiyiCvV2A5DocjsbaaWC9Bjw0RnBNfxcHkPmvat7C72KsxZuqYcCxik3cKyTjPr3gMoelABDUZUGz7xgv4kQhaku8Xe8rKeHytfUhUs1dMLinzAJr13Ub7ZSUH03uCQCWb5v8f6RtQIRRvpTMC84OCFi2uHcnvdbq8Vw0jOfzM88VWbviWt7zgOx3E5abzYU0Jt1V3ui8Oyl1bCXzWLrhU0j1m8jHyMLZ8te9cNnG1mMjxG91M5JI0C56n2kMmkF5wuL09TxpjVX86s6tzOPuCrpkjBKJG5Zq4blac1b9519lygDvf6WQzjoCuhYZhu6YWGg7W3QgCM4AI2lzrPCExathTsKLTDrBfcG");
		// Trunks
		registerPreset(3, "DMZHair_v2:109u3NyEBqNT1hlTR4XJ1Tyk4bG7d5naHigcDM6fNgwOLp8Vu1yFt7qrMLRkEpAsCSZ6WPbxoeLbLdF4VMbTTcjF8Muq6BDvdx9mjcHkIdQW3vrcedSao01KfxVfSyw9ARFke5DbijCG7hmPZ0KfHw5CX5kyruspUjLbov2tNPtkrso5mNqA98g5DmqFAaA5Ovz0qeh1jm6FgkrWK1SwOfDQ8gzEcLUX8m3Cma8quXWAnPAWbMxUDkpzdyxdE53JMsrEN8Pfvnp0BvLDB0Ud0Fql5iyiMxQU7PRYA4RfL8ExamfHX2ljXDWxl9C9DoemUZ4MRdE8wshyFhyt3ZYb2Ud46aO9cFd4zDPtS8vrDhxf4o5w81rP5r5rut7fwI68luYLJpRkiWhYE1MhrDcwI8MxEAcYmq2DQxIVnqARinUHYG2KMOTwEQfvUCeKCMc8rQENnoizYncBMY5jfRmi7e6wpmJhPzm69MdYEYb9u8YAWzcZvRFn8KC1mD37LCEcis2FhqKIw2OEdjIxkIRkPycAlTKQ26syx6JSM3rMQHPWLnTGeVl0XPKJJyzM34c3b0VGSQOQmyWgBpv8QxETafedUY8DpCnY2eDuNE3tAauTZxxtgUkM4mVRthM0IQQ01s4lgaQks7DftbDlNQG3rlvSIYjUA7NMr6gdEqpk1Z7m7NabVeaLcUSTl0g7TG7HJn41tWtw5EiXw2qieW");
		// Gohan
		//registerPreset(4, "DMZ_HAIR:...");
		// Krillin
		registerPreset(5, "DMZHair_v2:CAcuYhMhxDxRu9BOLjQ564OAA5mhjtyHhDDKQjEzpgG7T9kNUNHEwwtIcbcFq5wGmjwZXfl14nyJ7qkalaxOljksX9yWB361dqqm12");
		// Trunks (Large)
		registerPreset(6, "DMZHair_v2:6b5F8VGQBNymW2aBEcUpUj7SDnX2aUgVLB7dENmUj1YhR9gJ2ubfkTQJF4SYrXATid9oH3MaN3bna2oFY8Y8sYbdeiqZgQ7SffMmP7aFMfA0TN1fpXZv4KfKs3DYOzy8tBP7YJd3S1SE6CE0Eh2QbUR4c3OBBi375rzNt2tjwYzqTgl5X4KgJ5BQ5Sf0kskDNdotcTYQ90xF0HlRQvTk81ryugiD0Tza2kalzTtgFIncd8clkeHS7SNFHoR7u9zUFOk1dsYDjSYimtZeFHnG3dp9L2Aw3EUoDljrXFwBOtBjvQp2G6AoXxok19q00yey0fH3txp78jZP0G9yiFPgAJFWYKTbDkp4sRZ7CRo8Wcf6D7imzpWx9DYrPbWAuLmsi8dsFlTvlLQP14G99jLgDaXeVKeZl4iRuTsaaNnURXR84EpIgWQYiFhsiTrX6YuCWP6rWyN5NlkPp5QOhcuhBItT9QIwYE0MTfhk4DQXhBSwOJtoDRmwSXvtSG3POVRRhOMKS0HNBkHHvEQt1MIsLKCFfBKfqUIxoMuCR91fG2nTnY2bbYwFHd3LWBnR85JjzoqaeRMNfNpESAsQffyAlsi25hNQlsbCXAfujcDIogh9j8He8wyTRupkWQ0wAgTeqW5sUED4wgcfcbAe0M9KrxPw3tZAvd46Cl36pjmYVxlgbJTNqn16PRUZyO2i3sqSZIjLDcuQP0PQ2elbYQVulgXVKOdnLgTvl39O1wZ4Gblat0h2vObq31I2Wp4U4BjdxDna36dI1oDxhIEVs3aWwDqVpYaaT9OFec4AKZCFthuKgkMJYrajHb6JzF3LBqQwbS80d1AsNR193dmA7qUUvK3CengfnVgQcglf47Ah354F4vxWq0ZzdnACLLaqlWu0PS9KYXkieVWCfQmmY6w3R72JfanVn57yfnNOcHL1zJZU60xQKxTAyn4uV47gnxbl35uEUKGSLx1N5rT1cMU2xV3fgFloYi1O9GDK9qA7W1aAzOcbDLL0cC2HBP6gaQRo0YKmbWAfmvxodvkSXX2kVkYkdaDIwMK5h5uududlLFN76uScVq2hTV87mJN6Ufw");
		// Fused Zamasu
		registerPreset(7, "DMZHair_v2:2OE729A1BqENnlgFzQ8sz8eTyN4x9GsrdUtErgNWDkPM9sHBBt7Y9B2B8UQsw8EqqEpty9wjplNXS7YepcIglqIsUoFmbYtmlXsqNBLBvODNJr0VdzoBxqGF9JO80PBLTfrEL68dW2FaasQEmSYScXrjIHGGZDrlXnKmMAdHpAI5v5L7XVziWaTx281O8Xmbt688Up37jMniIorhjlUyxUnX7HxzTpQlIvahYL6ZFU0kliubrr0dZnFao8KDJiXB8MTppZVIYkCmL2vIK0TVDmyyUtiGkreasFIaabpC1IP9OMxIqilTGXBfB4Qmje48VQFBrcaxvzYJAn0h1QVhSdb5Vcho4tuobOAUpLSkqpMUELBa4rTDvzu2joiMo4RAon6CQ0EkedUO5iXy1XxD9hZh1BXs8aDB7bXFPVDeYWP5wEqDZQyOUxl9E1u0CRamSngQC6d07WOg6EjvMuD6qGfFLiexrikcKGFgEQURrLSTXiRL9jmtpY8QawbaHZo8SUhHZuJy0RgC5Y4vnspDyru4AV2E9vRMJKaVfJFiVk72aNsycOSBtXs9WFUs6U4dCbnSzrblj6leesMyyooNDCG0hNyXBnJZu5Cm4mssOIZ5jeqFyUOZNLapoARjSjSVwTgfkQaHM7Z2fvqNVN2z3aPXQf8nkkDogCzKNkGrVCyP2C6ZdyFW92goZmvickXpmRCEDi6oBBZf9HJigB3uSWLWMgwEMXuNADmCzjA4QwAQHrq9QXWMWNzAw4zmQ0l2by6bdvrqo5ClUznuqccGduh6Def51JT0wCymsnTvD1JCJLCp58KXX6TuJVQlQSE939nuhygOw30jTi89dtWVH2X0u0lnDsZferYegozGzdXmYW9IKoxx8QIqO0XRMTZIDBlPxyO5l5O8JTXvX3JYvaCZ9qWvL1xqqMQHzpekjXWyb4scOB89uevER2XxAfoyCqrnDZN0hYkxD2ssJRvxgNET7EqGJhpJCZATqCJJXtVTru0b1gQKkBzVitx0mXPz7reU0Hx53CqWXD6i488Rj7uQTQz3yQr8PfVgb3O8oAMzROKiMoXaY6W0bMeMGzCJXyUbo5GItMjPaVw3FMAbHZj5V9R7Mq4x4psWoheRJTa60YQgUJvMLu7TrGlp8TmJeqF2MIHQCdMpClxJMWlfwaW38hxOT8bynRi7RJxysM58McI7lHdqOw4kVezMeSd3tLuXPX5fwWo4FjvoClPmhRsayO90maxE9g8CKeEiwrPi43UELGydhYGXkmm6hrL95exyoRbhYBsdjH7l59dPQb9g8Sml3MrfHkoeh2LUF8fUAgxOsUCKIqygoc9DxVHBsBUWGKyQLSEzLRHhg9s4msMB5Dm3fukowLyK2cSg5rl79zf0Pgwu3J5T9RwzcZwlRRmpooLpayuUSvlZ1DyfWcQ3s0lv13xgyZMZAyJKRLfE6Qm8W3EBUnLApN2REjieu3ex7rUVeSFyja2G40XJ5nt5B3S8IV7YceVOVjU6Tas2wr0ALbrbfNBSbZr9M2OTTVqFXqzQrBk6NwdagLIFmjR1HjaSUuhi5plBY5uoAy0");
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
