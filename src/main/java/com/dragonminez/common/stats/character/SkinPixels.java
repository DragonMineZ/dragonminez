package com.dragonminez.common.stats.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class SkinPixels {
	public static final String NBT_KEY = "SkinPixels";
	public static final int SKIN_SIZE = 64;
	public static final int MARK_COUNT = SKIN_SIZE * SKIN_SIZE;
	public static final byte MARK_NONE = 0;
	public static final byte MARK_BROW = 1;
	public static final byte MARK_SCLERA = 2;
	public static final byte MARK_IRIS = 3;
	public static final byte MARK_HAIR = 1;
	public static final int[] TATTOO_SIZES = {64, 128, 256};

	private static final String EYE_KEY = "EyeMarks";
	private static final String HAIR_KEY = "HairMarks";
	private static final String TATTOO_SIZE_KEY = "TattooSize";
	private static final String TATTOO_KEY = "Tattoo";
	private static final int MAX_COMPRESSED_MARKS = 16 * 1024;
	private static final int MAX_COMPRESSED_TATTOO = 512 * 1024;

	private byte[] eyeMarks;
	private byte[] hairMarks;
	private int tattooSize;
	private byte[] tattoo;
	private int revision;

	public int getRevision() {
		return revision;
	}

	public void markChanged() {
		revision++;
	}

	public byte[] getEyeMarks() {
		return eyeMarks;
	}

	public byte[] getHairMarks() {
		return hairMarks;
	}

	public byte[] eyeMarksForEdit() {
		if (eyeMarks == null) eyeMarks = new byte[MARK_COUNT];
		return eyeMarks;
	}

	public byte[] hairMarksForEdit() {
		if (hairMarks == null) hairMarks = new byte[MARK_COUNT];
		return hairMarks;
	}

	public void setEyeMarks(byte[] marks) {
		this.eyeMarks = marks != null && marks.length == MARK_COUNT ? marks : null;
		markChanged();
	}

	public void setHairMarks(byte[] marks) {
		this.hairMarks = marks != null && marks.length == MARK_COUNT ? marks : null;
		markChanged();
	}

	public boolean hasEyeMarks() {
		return hasAny(eyeMarks);
	}

	public boolean hasHairMarks() {
		return hasAny(hairMarks);
	}

	public int getTattooSize() {
		return tattooSize;
	}

	public byte[] getTattoo() {
		return tattoo;
	}

	public boolean hasTattoo() {
		return tattoo != null && tattooSize > 0 && hasAnyAlpha(tattoo);
	}

	public byte[] tattooForEdit(int size) {
		if (!isTattooSize(size)) size = TATTOO_SIZES[0];
		if (tattoo == null || tattooSize != size) {
			tattoo = tattoo == null ? new byte[size * size * 4] : resampleRgba(tattoo, tattooSize, size);
			tattooSize = size;
			markChanged();
		}
		return tattoo;
	}

	public void setTattoo(int size, byte[] rgba) {
		if (rgba == null || !isTattooSize(size) || rgba.length != size * size * 4) {
			this.tattoo = null;
			this.tattooSize = 0;
		} else {
			this.tattoo = rgba;
			this.tattooSize = size;
		}
		markChanged();
	}

	public void clearTattoo() {
		setTattoo(0, null);
	}

	public boolean isEmpty() {
		return !hasEyeMarks() && !hasHairMarks() && !hasTattoo();
	}

	public boolean sameAs(SkinPixels other) {
		if (other == null) return false;
		return Arrays.equals(eyeMarks, other.eyeMarks)
				&& Arrays.equals(hairMarks, other.hairMarks)
				&& tattooSize == other.tattooSize
				&& Arrays.equals(tattoo, other.tattoo);
	}

	public void copyFrom(SkinPixels other) {
		this.eyeMarks = other.eyeMarks != null ? other.eyeMarks.clone() : null;
		this.hairMarks = other.hairMarks != null ? other.hairMarks.clone() : null;
		this.tattooSize = other.tattooSize;
		this.tattoo = other.tattoo != null ? other.tattoo.clone() : null;
		markChanged();
	}

	public SkinPixels copy() {
		SkinPixels result = new SkinPixels();
		result.copyFrom(this);
		return result;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		if (hasEyeMarks()) tag.putByteArray(EYE_KEY, deflate(eyeMarks));
		if (hasHairMarks()) tag.putByteArray(HAIR_KEY, deflate(hairMarks));
		if (hasTattoo()) {
			tag.putInt(TATTOO_SIZE_KEY, tattooSize);
			tag.putByteArray(TATTOO_KEY, deflate(tattoo));
		}
		return tag;
	}

	public void load(CompoundTag tag) {
		SkinPixels parsed = parse(tag, false);
		this.eyeMarks = parsed.eyeMarks;
		this.hairMarks = parsed.hairMarks;
		this.tattooSize = parsed.tattooSize;
		this.tattoo = parsed.tattoo;
		markChanged();
	}

	public static SkinPixels parse(CompoundTag tag, boolean strict) {
		SkinPixels result = new SkinPixels();
		if (tag == null) return result;
		result.eyeMarks = readMarks(tag, EYE_KEY, MARK_IRIS, strict);
		result.hairMarks = readMarks(tag, HAIR_KEY, MARK_HAIR, strict);
		if (tag.contains(TATTOO_KEY, Tag.TAG_BYTE_ARRAY)) {
			int size = tag.getInt(TATTOO_SIZE_KEY);
			byte[] compressed = tag.getByteArray(TATTOO_KEY);
			if (!isTattooSize(size)) {
				fail(strict, "invalid tattoo size " + size);
			} else if (compressed.length > MAX_COMPRESSED_TATTOO) {
				fail(strict, "tattoo payload too large (" + compressed.length + " bytes)");
			} else {
				try {
					result.tattoo = inflate(compressed, size * size * 4);
					result.tattooSize = size;
				} catch (DataFormatException e) {
					fail(strict, "corrupt tattoo payload: " + e.getMessage());
				}
			}
		}
		return result;
	}

	private static byte[] readMarks(CompoundTag tag, String key, byte maxMark, boolean strict) {
		if (!tag.contains(key, Tag.TAG_BYTE_ARRAY)) return null;
		byte[] compressed = tag.getByteArray(key);
		if (compressed.length > MAX_COMPRESSED_MARKS) {
			fail(strict, key + " payload too large (" + compressed.length + " bytes)");
			return null;
		}
		try {
			byte[] marks = inflate(compressed, MARK_COUNT);
			for (int i = 0; i < marks.length; i++) {
				if (marks[i] < 0 || marks[i] > maxMark) {
					fail(strict, key + " contains an unknown marker " + marks[i]);
					marks[i] = MARK_NONE;
				}
			}
			return hasAny(marks) ? marks : null;
		} catch (DataFormatException e) {
			fail(strict, "corrupt " + key + " payload: " + e.getMessage());
			return null;
		}
	}

	private static void fail(boolean strict, String message) {
		if (strict) throw new IllegalArgumentException(message);
	}

	public static boolean isTattooSize(int size) {
		for (int allowed : TATTOO_SIZES) if (allowed == size) return true;
		return false;
	}

	public static byte[] resampleRgba(byte[] source, int from, int to) {
		byte[] result = new byte[to * to * 4];
		if (source == null || from <= 0) return result;
		for (int y = 0; y < to; y++) {
			int sy = (int) ((long) y * from / to);
			for (int x = 0; x < to; x++) {
				int sx = (int) ((long) x * from / to);
				System.arraycopy(source, (sy * from + sx) * 4, result, (y * to + x) * 4, 4);
			}
		}
		return result;
	}

	private static boolean hasAny(byte[] marks) {
		if (marks == null) return false;
		for (byte mark : marks) if (mark != MARK_NONE) return true;
		return false;
	}

	private static boolean hasAnyAlpha(byte[] rgba) {
		for (int i = 3; i < rgba.length; i += 4) if (rgba[i] != 0) return true;
		return false;
	}

	static byte[] deflate(byte[] raw) {
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
		try {
			deflater.setInput(raw);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, raw.length / 8));
			byte[] buffer = new byte[8192];
			while (!deflater.finished()) {
				int written = deflater.deflate(buffer);
				out.write(buffer, 0, written);
			}
			return out.toByteArray();
		} finally {
			deflater.end();
		}
	}

	static byte[] inflate(byte[] compressed, int expectedLength) throws DataFormatException {
		Inflater inflater = new Inflater();
		try {
			inflater.setInput(compressed);
			byte[] out = new byte[expectedLength];
			int total = 0;
			while (total < expectedLength) {
				int read = inflater.inflate(out, total, expectedLength - total);
				if (read == 0 && (inflater.finished() || inflater.needsInput() || inflater.needsDictionary())) break;
				total += read;
			}
			if (total != expectedLength) throw new DataFormatException("expected " + expectedLength + " bytes, got " + total);
			if (!inflater.finished() && inflater.inflate(new byte[1]) > 0) throw new DataFormatException("payload longer than " + expectedLength + " bytes");
			return out;
		} finally {
			inflater.end();
		}
	}
}
