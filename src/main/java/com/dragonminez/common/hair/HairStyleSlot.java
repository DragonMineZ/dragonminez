package com.dragonminez.common.hair;

import java.util.Locale;

public enum HairStyleSlot {
	BASE("HairBase", "B", "Base", "base"),
	SSJ("HairSSJ", "S", "SSJ", "ssj"),
	SSJ2("HairSSJ2", "S2", "SSJ2", "ssj2"),
	SSJ3("HairSSJ3", "T", "SSJ3", "ssj3"),
	SSJ4("HairSSJ4", "S4", "SSJ4", "ssj4");

	private static final HairStyleSlot[] VALUES = values();

	private final String nbtKey;
	private final String codeKey;
	private final String legacyCodeKey;
	private final String hairType;

	HairStyleSlot(String nbtKey, String codeKey, String legacyCodeKey, String hairType) {
		this.nbtKey = nbtKey;
		this.codeKey = codeKey;
		this.legacyCodeKey = legacyCodeKey;
		this.hairType = hairType;
	}

	public String getNbtKey() {
		return nbtKey;
	}

	public String getCodeKey() {
		return codeKey;
	}

	public String getLegacyCodeKey() {
		return legacyCodeKey;
	}

	public String getHairType() {
		return hairType;
	}

	public int index() {
		return ordinal();
	}

	public static int count() {
		return VALUES.length;
	}

	public static HairStyleSlot byIndex(int index) {
		return index >= 0 && index < VALUES.length ? VALUES[index] : null;
	}

	public static HairStyleSlot byHairType(String hairType) {
		if (hairType == null) return null;
		String normalized = hairType.trim().toLowerCase(Locale.ROOT);
		for (HairStyleSlot slot : VALUES) {
			if (slot.hairType.equals(normalized)) return slot;
		}
		return null;
	}
}
