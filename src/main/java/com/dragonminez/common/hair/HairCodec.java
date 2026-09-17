package com.dragonminez.common.hair;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.EnumMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class HairCodec {
	public static final String CODE_PREFIX = "DMZ22:";
	public static final String FULL_CODE_PREFIX = "DMZF22:";

	private static final String LEGACY_CODE_PREFIX = "DMZ1:";
	private static final String LEGACY_FULL_CODE_PREFIX = "DMZF1:";
	private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int MAX_DECOMPRESSED_BYTES = 1 << 21;
	private static final long MAX_NBT_BYTES = 1L << 21;
	private static final int MAX_CODE_LENGTH = 1 << 18;

	private HairCodec() {}

	public static boolean isFullSetCode(String code) {
		if (code == null) return false;
		String trimmed = code.trim();
		return trimmed.startsWith(FULL_CODE_PREFIX) || trimmed.startsWith(LEGACY_FULL_CODE_PREFIX);
	}

	public static String toCode(CustomHair hair) {
		if (hair == null) return "";
		try {
			return CODE_PREFIX + encode(hair.save());
		} catch (IOException exception) {
			LogUtil.error(Env.COMMON, "Failed to encode hair style '{}': {}", hair.getName(), exception.getMessage());
			return "";
		}
	}

	public static String toFullSetCode(EnumMap<HairStyleSlot, CustomHair> styles) {
		CompoundTag tag = new CompoundTag();
		for (HairStyleSlot slot : HairStyleSlot.values()) {
			CustomHair hair = styles.get(slot);
			if (hair != null) tag.put(slot.getCodeKey(), hair.save());
		}
		try {
			return FULL_CODE_PREFIX + encode(tag);
		} catch (IOException exception) {
			LogUtil.error(Env.COMMON, "Failed to encode full hair set: {}", exception.getMessage());
			return "";
		}
	}

	public static CustomHair fromCode(String code) {
		if (code == null || code.isBlank()) return null;
		String trimmed = code.trim();
		if (isFullSetCode(trimmed)) {
			EnumMap<HairStyleSlot, CustomHair> set = fromFullSetCode(trimmed);
			return set != null ? set.get(HairStyleSlot.BASE) : null;
		}

		CompoundTag tag = decodeTag(trimmed);
		if (tag == null) return null;
		if (tag.contains("Base") && (tag.contains("SSJ") || tag.contains("SSJ2") || tag.contains("SSJ3"))) {
			return CustomHair.fromTag(tag.getCompound("Base"));
		}
		return CustomHair.fromTag(tag);
	}

	public static EnumMap<HairStyleSlot, CustomHair> fromFullSetCode(String code) {
		if (!isFullSetCode(code)) return null;
		String trimmed = code.trim();
		CompoundTag tag = decodeTag(trimmed);
		if (tag == null) return null;

		EnumMap<HairStyleSlot, CustomHair> styles = new EnumMap<>(HairStyleSlot.class);
		for (HairStyleSlot slot : HairStyleSlot.values()) {
			String key = tag.contains(slot.getCodeKey()) ? slot.getCodeKey() : (tag.contains(slot.getLegacyCodeKey()) ? slot.getLegacyCodeKey() : null);
			if (key != null) styles.put(slot, CustomHair.fromTag(tag.getCompound(key)));
		}

		CustomHair base = styles.computeIfAbsent(HairStyleSlot.BASE, slot -> new CustomHair());
		if (trimmed.startsWith(LEGACY_FULL_CODE_PREFIX) && !styles.containsKey(HairStyleSlot.SSJ2) && styles.containsKey(HairStyleSlot.SSJ)) {
			styles.put(HairStyleSlot.SSJ2, styles.get(HairStyleSlot.SSJ).copy());
		}
		styles.computeIfAbsent(HairStyleSlot.SSJ, slot -> base.copy());
		styles.computeIfAbsent(HairStyleSlot.SSJ2, slot -> base.copy());
		styles.computeIfAbsent(HairStyleSlot.SSJ3, slot -> base.copy());
		styles.computeIfAbsent(HairStyleSlot.SSJ4, slot -> new CustomHair());
		return styles;
	}

	private static CompoundTag decodeTag(String code) {
		if (code.length() > MAX_CODE_LENGTH) {
			LogUtil.warn(Env.COMMON, "Rejected hair code of {} characters (max {})", code.length(), MAX_CODE_LENGTH);
			return null;
		}
		try {
			byte[] compressed;
			if (code.startsWith(FULL_CODE_PREFIX)) {
				compressed = Base64.getUrlDecoder().decode(code.substring(FULL_CODE_PREFIX.length()));
			} else if (code.startsWith(CODE_PREFIX)) {
				compressed = Base64.getUrlDecoder().decode(code.substring(CODE_PREFIX.length()));
			} else if (code.startsWith(LEGACY_FULL_CODE_PREFIX)) {
				compressed = decodeBase62(code.substring(LEGACY_FULL_CODE_PREFIX.length()));
			} else if (code.startsWith(LEGACY_CODE_PREFIX)) {
				compressed = decodeBase62(code.substring(LEGACY_CODE_PREFIX.length()));
			} else {
				return null;
			}
			byte[] raw = inflate(compressed);
			try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(raw))) {
				return NbtIo.read(input, new NbtAccounter(MAX_NBT_BYTES));
			}
		} catch (IOException | RuntimeException exception) {
			LogUtil.warn(Env.COMMON, "Failed to decode hair code ({} chars): {}", code.length(), exception.getMessage());
			return null;
		}
	}

	private static String encode(CompoundTag tag) throws IOException {
		ByteArrayOutputStream nbtBytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(nbtBytes)) {
			NbtIo.write(tag, output);
		}
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
		try (DeflaterOutputStream output = new DeflaterOutputStream(compressed, deflater)) {
			output.write(nbtBytes.toByteArray());
		} finally {
			deflater.end();
		}
		return Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray());
	}

	private static byte[] inflate(byte[] data) throws IOException {
		Inflater inflater = new Inflater(true);
		try (InputStream input = new InflaterInputStream(new ByteArrayInputStream(data), inflater)) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int read;
			while ((read = input.read(buffer)) != -1) {
				output.write(buffer, 0, read);
				if (output.size() > MAX_DECOMPRESSED_BYTES) throw new IOException("decompressed hair code exceeds " + MAX_DECOMPRESSED_BYTES + " bytes");
			}
			return output.toByteArray();
		} finally {
			inflater.end();
		}
	}

	private static byte[] decodeBase62(String encoded) {
		if (encoded.isEmpty()) return new byte[0];
		BigInteger value = BigInteger.ZERO;
		BigInteger base = BigInteger.valueOf(BASE62_ALPHABET.length());
		for (int i = 0; i < encoded.length(); i++) {
			int digit = BASE62_ALPHABET.indexOf(encoded.charAt(i));
			if (digit < 0) throw new IllegalArgumentException("invalid character '" + encoded.charAt(i) + "'");
			value = value.multiply(base).add(BigInteger.valueOf(digit));
		}
		byte[] bytes = value.toByteArray();
		if (bytes.length > 1 && bytes[0] == 0) {
			byte[] trimmed = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
			return trimmed;
		}
		return bytes;
	}
}
