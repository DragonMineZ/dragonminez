package com.dragonminez.common.hair;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
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
	private static final Map<Integer, CustomHair> PRESET_CACHE = new HashMap<>();

	private static final String CODE_PREFIX_V1 = "DMZ_HAIR:";
	private static final String CODE_PREFIX_V2 = "DMZHair_v2:";
	private static final String CODE_PREFIX_V3 = "DMZHair_v3:";

	private static final String CODE_PREFIX_FULL = "DMZHairFull_v3:";
	private static final String FULL_SET_SEPARATOR = "\\|";
	private static final String FULL_SET_JOINER = "|";

    static {
        initializeDefaultPresets();
    }

    private static void initializeDefaultPresets() {
		// Goku
		registerPreset(1, "DMZHair_v3:63yOwN8GQ5KHD1VZGNkDq8oPFrEsqxbtABZNaFFZwkIyd0ttKDcabgjSupBCTQndQZXcYPDDNtfuHwy0K42l1hJIznl8ZXTu4gLwL4t1mwGTyvJkQbNoHywi36KaGGOOiT0IdflK7CfCjRd2uj4F8k0xwzJGUkTvwFmBS4k64T981kyNdtfQFxRsNrRsfMz5zYizZuiuIoQdCANGFV0TM9kE8tylpjUZnrftedeADwf9Vkd3nOzHDL0KVZR9ZD93CLweqFyrttMmTGeFV7HTUOM6OWX2Ohr5izD7RVudKOHq67Yfr7bUXD5ilJv9plKEq1zDQaryNr18zLoyRjUKmTX3Bj31UCbOnvZxYtEgDeCHwoGdNUaS9tEkWD5jk5ftXbitXx3QGbFHPbuT1tklVv3ZiMz2DFpc0zPS3utawvqV0aKdpnwDE0OZvGRzGyyafFdSrffPXOacUxFfeF2Z7unTOKc3orpJ73cYuvEiEQaw14YznhemcA19XJSWInuYSvFY9wm5npRl2M6W7XhTLC4Xjwiv1udvRmN7C6OPCPzQcbeZPPIlSL3zHKOGc3XjlEECpKGroYHDCOpsOF8cqYupLOyiDB7o0Yn8Uxm7wDPg6vnMDpHr13FCCdGuI6du3nIwAQHMVa2ItRyoqALTun5d7BVPcEzHswTPaGbInJyxuB661YpevcmcnNaSxvAlPUBf2wuTOmtR32jPQ2lAwDxqS7LHwwpIwKPda4JzmbKPzixTBYnnl0D1z2c0sK0hOW1NJfTXD1VkuP8p4MuASeb4t8dZACeLjstRMTzewtJL7cYH3GuJBEC6zJyoxTunRcSFLuoQniJ6XbSlOHjKaHX2vgecrb0n6MqGZXFpZ9zZNTD1GC8WNgRvtibAdeJDlat0Wu67AJhOQwr0nFEyEHCpSsH2EwqLY86MPgRgA8A8zjSILHF0VY46DvZuvzrcV9PrRkLy65KJdS1XyL09nkPAaZssviE3GzcLR4J0WYdZlgzEWOTkz58Y76obR4TyQDI07TEnqk0MgpnVzv7zXCBz5owXEtA1y3iCfPrIYyJyV9PsKs08gBTh66n6Tct9UUUiK5CqKSTawargy3cBdj24aI5plIqrzQD7Cc5WTQeBqH6PUcDktjkucgtz59nzQ4cNQ9iZ1WGO27jwp7zbb2Tr57KblprzW5K0IZ627q6kFUEKPzaEY8KD1uYINPROuJ0jFUxC09owNO4bMinu6KcqMm5xsv2QSnJh53yhWFM");
    	// Vegeta
		registerPreset(2, "DMZHair_v3:4uoLr5KEk64ZX1zsmFlZZ2t3uKMjglZAMFPAUrgztfNBTLiz6KIfdcFOs2bjW6iTyHjFLtWUsqG1O2Gu2A7JlvIkDKnLeGMFrUDgYrbb9scI09VQkOsowyQEMDIpHygA1xP90FWJdlZNRDMRtmOaKLTaw4p34QIcYcjB3bpPHAB1uyqPU7jBNHvkaEhjdQimEamxSE2RPmHzJXPOqUCj7p3eZT8zhOxsxQnGFPwgMWCVMCRnL0gQQGv2QmgaTtWQ75xtZjD72EPMX2IkNZdRxFQca6x6GrJ1Kfgp4VL4Wk9oSeF9CTLOM1s3JPrsGCkWcbkppxMHq5wcUm1mcJgBW74vb33Eq7GB4aZL4Hqz0tlVB0vM8Y5xD5aSTiAvkxJbJZTiaradiYiqqta5pUvE0K8tbx7QrnqXWm0KbmYezuYjOy4ja7L423nudux9TE4POfOZvPKQuFOnsTZlbDRlgtOFbr3C820UVCSAikvIETZL5G5C9ReojpHNhx7gtkoZvpg0pjse0ttANSwrAPe3DsxRfLuPrxnpmb3K7BCJ2IhRcao8ojOwkkEC7swXmnwUqqcFj04WdOxA0EEwBqIWwezwUMdYn86LhYHx0FdKOv9S6VJYecMg7Zyicxqv5z8rfzKSMoZSGsrrxldFGWYf1kT7MEKXYePRRqNht6jVborAkJpRYGo7J7SZfS0dpoilHV3aPQf5db3O7PQDe7iulAnsC8SQe5VXHLL3ZQyXL1RCuU3hKQz1jtjZ1GCry7bMuHWQSINfzofeUw84dN398sjkg7d2jdNytQfqAKgSih43n0QYyCqTwp0T7PhPiliJRCgKMlW4FxnEzPeqLwTCJ7lVcXqIB4JAaM4CGa04Hsbti8u2jM9Bq7x305dm3eF7rTYYOlXkmc9skgsaa0xuFMgm9GmwBQkPhi2pY0tXSo0stbAaFpPMOAQq7apGLplN22sChOpfd5ut979jyr02iGrHBk3F0pDU6v0Pt56517IXjMt7SBPHyJHLOLsuSJ4gkEr5Ruk2mH97d3EnmKtHSTDZOzHxqwNA0vcpxbg8UkT0ZkBz1zTaiTlziJokZiB0xf343ZBJ7uoYsp0fyxhyWJNGqb1ZJoAJMcwgT169YLJCjIfdjDgiQXAowzDiQouwPgHRHsUU4bUfRUtPva1hZXWoXSGKADGkn4dpkhuHx76P2GbHdlGrDfPqV9iBuBEl4pHYrHhW4yt4ZtIlSz1FmMoIm6n6ky79vv3dcQBL90wntXvNT0EsVVbiLgizIQ5EdRLUyLW29oJ4OAUChdYd8Emg5upGNDuZQ6BrUni58bH029OsQV4ksQ3lcvz7toqgqm7K9pUWFM1XuAfHBjR8yIiz43xkS0nDTCrWc6hPkm1LOZ8p9C78G2rdB64OA2xfgXJXUmaFhWcolINhFk0srXTdtJpzjOy9TDjkXSJ6u9f2Ad7VFsvvyuzQTlBmrxNPXrrtYx05lFv4N0b5E3ezxvG5QKnIGq12UxDFtn96v1KVvEfhvaBb7NuEp9YaG3bDAcYSNZBkB5XIC8xFzw6eRA2YYI6rLuaQggzd4J5iAbR1DdWRIueGEjQquVK4d9YAyXy2CTZQCKH81yqmwP4T88hSYq7xN1RXrVb7kjOK43Pp7yAFxmFp9vr6II1oduA26ZvbNhnNT6wVH86SF4hmHUuHiGGQQEYGhBZ64XpxFlmwakoJxUPRcM4pbq35vTC7dnEUTk99uK2B8F4kzyJpRXy1H2b0gz5sFLcjkG3ZcnXmxImgn8tq4U4wbL9M4lzq3csUnr85Ti6VOfj8EtZLMCWGV3NNv7F22D1urbkTB6P48T1FDoLVlyt9tCU9UVxfDjRDX6GQigPwQ642qbotd1keXdaNpD6oh6VgXAgXm8HLworwKFFOxWNSivFfdD4g914QuMJ3DsSdWELA0Pllbaz5UuiC0mnAzZhlr7QzuSsF5TIzC4mpjcxs1h6y1yYCf86wfiruMJHgKEs5AllmighksA3DtpPA67e4GZTpR1FLgchbfne7YoCsmMh6GNpLRHmFdDvDgwOLd6hAM9CQW0cIWcYCyB9a1V8GhcAxnup2Jqvhgmpr9NzJN6vMWP6NeSGsxc5hLagM8EzNmXI6eW5tpHkEs2y8OG3tWzlO5zfhcu9EFLX6jzcjKlmUpgCN7p7pTp1HMkJ0FjDxndVCSLOi85QomI77E86Adta4TXyn6yCjAky8n4A5QnXO1nthTDpfPPichFcDVyXBM8Fc8oUhyYGmWzbcDu60aG7aR1UBA3gPSHYyz5erAhJb5IwvXxXDfl9s2mtx5AJnOwas3Cd7BfOuyI3FWRURGCC8ctU6mHt24iIzEd2lN7fWvAPD7FoPCvX9W8VK6Gq60ECO8V1RLvnO2FSHozyINxJrEffN34cBYDEm2PrN0s3j0le7W6wty8t1P96NpSTy3eHn448RfXlR0emMZ1kYJXdiIbI");
		// Trunks
		registerPreset(3, "DMZHair_v3:Fj83WA31xa0nLyisZeefvAMIrXOtHHl7FdawUgwyaWljK9HMyHQl7YmZ7zugjFDl6gLAXMv93UyVZ7JB8XkSjv7C4OjLNdEQ80YmHOfeah4z26hongLJKxe9I26YqFUUbV5TvCcm8ArPvlb3kN6Eh9f08e5Wuj90lpKmcNV5FrX2abj5mkHIr7dS9BbnUWU1KAl5bnDbAFnyILtqkoFx1sqgS4PGnjFJKqOTEUXFijCyWPBE0fNJgN0fXwWMsSaM3NWHxSoHnbQeHKjekjhxMEFoRBGc1Q3WBJQLDs4rsfAoZK4hk92jUVA83MM19t1HIOOX4ggOYdnRg5fsYnCbmnSk7EE1GAU5uGdZOoszlDQPpTm2moHo64OdvpIckOlHZ1YoKC1otxXpQHEKMQ190GrWlaS2I9aRpIfOwptZy2M4QpFuV8DDJ3kP0lRaqz6SXyKI3m5YKNjk3fGSQjOjWcEvaoMc0ESM3VrArvGTZcvs1ZFYZTqkMemZrA9zc9pclPRzXeCsMA9lMg04QakEqY4a3afGan5lacO04FdSbCIekmOxpSnROEEW2nwo0vRg0ygODFIZ2iPvxHeaqdjbPqXtfJap1kMegJEJBKTzGZxAFvP7PSObCflyx1SyckxdBJOGkqZQW1di2FgrTEW6zlhplYXPakUHyYMdMhT8jZwN9Q4qzqon7lYV88D1soetuyzyon1c2wG5tQcn0YdSW5HGjcG3MRNiK6I3xvSzu7mvDSBap1VSUcPpaFnj3blTJxPCx7ErDuBQDxDXkRFsJxR3h9hDQQpmHtNtE73V9PTvrEIUO4idob6O7abkMYVhkXfDmUzKxgctKIVLeK4XYemrjJ1Fgkvc9xoPGZFUSBGfoqfaoQJN2qIBeSaIIzXqGSkKrLCclVm9Jd7VhudI12CC9YQUwv9d82TJaBTeTAeYanw19OTGtAy9Ep24Opbj7hiAsadJk8WgiAtaD2QfHraLOG6a8J4QYUNph4fPHzu2BAo2IbGSEXY9JVxI5qPn9lO0AlWaABSKhHXQ6QIxpyhp0tFMP80bjwRT6fJBMRiInOHUDtInmjycE9AFNbRtaJp6KofmAqUu95AeMuqGrcQW6jrWuB01tnxFhXhWXTBUKZkea5evCUyGHazjxm2bmNR3RWq5zk3UO0NYFJbRSs4KCL50nENz4WcgxcfbM6ITc9yKoNanL6dJBifPNupexpDee7w9F4HCNbtFBqCVkmpwiJwlQYUD7k9WgkxDvJKdEZZC0WZWli6Mke7lV4RxNPm851rd78cb7k1iBc9X3w0zpVAuZjguqWJxNU7tuC1TK07lPwZi6Bww6gdjKEOFZyHiSZlKOsgp3j2AScKCDUkzsufoNUzPK4ZPU6Arv6M4STOtWoHQwsm8daQy5ZYQM5WQzSQCmBabMKWRwphAU1is94iIgZK7JPG9ZxBM1anjPwmGLpFEVRvv916QvzZ60kBdSOJEMvK2iwoVjO8y0hbUAy8Jry4mgtltzGY2ChXxk7QcS0jazjol0OZf72Oyzm3IF66GkTNDBG0fXLMDiuSEi7GRFmxtZlHtmU7IvJcs4YOQfjwX2Vo3k8PRerwQyvMEYnGmVWXRGOIqrrhITVJlilXgEdP5z7hQz3prYyqz12EvnupAemmIYNCQovFKYAoNk3Wxpyn7i2gK4e");
		// Gohan
		//registerPreset(4, "DMZ_HAIR:...");
		// Krillin
		registerPreset(5, "DMZHair_v3:7z1cOemO44pBikeI5FSI0DrDGspiel2bhjue82OvVpztIB5YAfJW7zaPvS8cX1waoOVBoNy3SqRTPVqrBGufxb1YHGbVTyPsAX806mL8PAy3ZuDg2lzZ1XVGTIWeujMCCeYRQFl1PWhs9l9KHj66nvVzLswY8w1JlD1EZC9vdx6bqVGh4R9IWMOBm7JRXoSPiGTx0JgWMiiAQEjN9yvNvbHGr4NijAcIB2lUdA13fpdQcpzYy7KzN3SQXG2aFdp7K6YpIJ6dlHrhDW0jJ9GoIlwL6bNww417eWkfA2pptqiYLUDv5BaxjuFgEk27nwOGHxWOlbgNh2oXxz4p7Esh4rckvyxyEBzrU0fVKimRr5lD7yDzKtNuPnqykI7ydWE8nZ2E9X30J4Y991VUnznY7RPaxTS5Q0yW38I0W25OrMlrtbiQHR5Rsyy6qdgM002g7Ep7ulA5JkYARA9X2AJ56Q6rWXWcb6VLYpvEmjrMSgf3vrRhV20AIpLiqmrbpfrwbWdtTBSEhNnKBlCwwm5EEROfRDJgIFNfNB65uQZcuxy5UeTJxIotfOU2cfyzPX1E3MwLVRnBmU56rnfHOkmEbf756nesSq34VXQtlojCpnlYe2TQAoiMHiShDpW4raA01gqq9PsUZ2tecTDXO0jqX7r0Wiyr0hMHbE9uS6TvBFcYcqfnVTxlpkCgu0RXut061gDs1CPUzK77Yu9kjyM9yAVoU7UWUU3ikfHbjRCwBr6Se7iioMPnrEamB9xQygpAZkavEv5S1juBgIVTQeJ6V4eSd68X59UqvnlDgU6p6ZuXusGeKtD8F1bndPWmInUbZ751LGCsVHtdyyrCkfdCqTdeLyv7fnoJRbDn4bej0I3tcgVzZfVvhTzscEPDPGAGSmeEiCpKwuSVabf6eESNqNkL2fq2Be5vfJhzm5RCPcJtH3sDxnYfFWXNwbHcoexJN4rQbmA0e7GnNnvVCtnOXH9KcZnuD508xAVTqbx1x3q7l6Q4mz93LbGgfijUlXHHZJqqagN7j5tOBxEAxqKzLHaX0uQOI8gGD97sCujLrVV5KbmzjDQCq92NskFuTFbEmfiScSYUkQzVYgW7mxzbevtFl6Bg4JbPApp87p3EdFynQwuWF092iVhRKCRIL2seH3V1yKk1YOcKoQ3XnqXQk6Cb83EPbE0wgSteeHR9NBPrWzma0SeyQAahHV3iwiKnMPkh7e5o");
		// Trunks (Large)
		registerPreset(6, "DMZHair_v3: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");
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
		if (hair == null) return "";
		try {
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut);
			DataOutputStream dataOut = new DataOutputStream(gzipOut);
			CompoundTag tag = hair.save();
			NbtIo.write(tag, dataOut);
			dataOut.close();
			gzipOut.close();
			byte[] compressed = byteOut.toByteArray();
			return CODE_PREFIX_V3 + encodeToNumbers(compressed);
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Failed to serialize CustomHair to code", e);
			return "";
		}
	}

	public static CustomHair fromCode(String code) {
		if (code == null || code.isEmpty()) return null;

		if (isFullSetCode(code)) {
			CustomHair[] set = fromFullSetCode(code);
			return (set != null && set.length > 0) ? set[0] : null;
		}

		try {
			String cleanCode = code;
			if (code.startsWith(CODE_PREFIX_V3)) cleanCode = code.substring(CODE_PREFIX_V3.length());
			else if (code.startsWith(CODE_PREFIX_V2)) cleanCode = code.substring(CODE_PREFIX_V2.length());
			else if (code.startsWith(CODE_PREFIX_V1)) cleanCode = code.substring(CODE_PREFIX_V1.length());
			byte[] bytes = decodeFromNumbers(cleanCode);
			ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
			GZIPInputStream gzipIn = new GZIPInputStream(byteIn);
			DataInputStream dataIn = new DataInputStream(gzipIn);

			CompoundTag tag = NbtIo.read(dataIn);
			if (code.startsWith(CODE_PREFIX_V3) && tag.contains("Base") && (tag.contains("SSJ") || tag.contains("SSJ2") || tag.contains("SSJ3"))) {
				CustomHair base = new CustomHair();
				if (tag.contains("Base")) base.load(tag.getCompound("Base"));
				return base;
			}

			CustomHair hair = new CustomHair();
			hair.load(tag);
			return hair;
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Failed to deserialize CustomHair from code", e);
			return null;
		}
	}

	public static String toFullSetCode(CustomHair base, CustomHair ssj, CustomHair ssj3) {
		if (base == null) base = new CustomHair();
		if (ssj == null) ssj = base.copy();
		if (ssj3 == null) ssj3 = base.copy();

		return CODE_PREFIX_FULL + toCode(base) + FULL_SET_JOINER + toCode(ssj) + FULL_SET_JOINER + toCode(ssj3);
	}

	public static CustomHair[] fromFullSetCode(String code) {
		if (code == null || !code.startsWith(CODE_PREFIX_FULL)) return null;

		String rawData = code.substring(CODE_PREFIX_FULL.length());
		String[] parts = rawData.split(FULL_SET_SEPARATOR);

		if (parts.length < 3) return null;

		CustomHair base = fromCode(parts[0]);
		CustomHair ssj = fromCode(parts[1]);
		CustomHair ssj3 = fromCode(parts[2]);

		if (base == null || ssj == null || ssj3 == null) return null;

		return new CustomHair[]{base, ssj, ssj3};
	}

	public static boolean isFullSetCode(String code) {
		return code != null && code.startsWith(CODE_PREFIX_FULL);
	}

    public static boolean canUseHair(Character character) {
        if (character == null) return false;
        String race = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();

        for (String defaultRace : DEFAULT_HAIR_RACES) if (race.equals(defaultRace)) return true;

        if (race.equals("majin")) return gender.equals(Character.GENDER_FEMALE);
        if (race.equals("bioandroid") || race.equals("frostdemon") || race.equals("namekian")) return false;

        RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);
        return config != null && config.canUseHair();
    }

    public static CustomHair getEffectiveHair(Character character) {
        if (!canUseHair(character)) return null;
        int hairId = character.getHairId();
        if (hairId == 0) {
            CustomHair custom = character.getHairBase();
            if (custom == null) {
                custom = new CustomHair();
                character.setHairBase(custom);
            }
            return custom;
        }

        return getPresetHair(hairId, character.getHairColor());
    }

    public static CustomHair getPresetHair(int presetId, String hairColor) {
        String code = PRESET_CODES.get(presetId);
		if (code == null) return new CustomHair();

		if (!PRESET_CACHE.containsKey(presetId)) {
			CustomHair baseHair = fromCode(code);
			if (baseHair != null) {
				PRESET_CACHE.put(presetId, baseHair);
			} else {
				return new CustomHair();
			}
		}

		CustomHair hair = PRESET_CACHE.get(presetId).copy();
		if (hair != null) {
			if (hairColor != null && !hairColor.isEmpty()) {
				hair.setGlobalColor(hairColor);
			}
			return hair;
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
}
