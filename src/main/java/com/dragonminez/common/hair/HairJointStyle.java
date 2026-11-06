package com.dragonminez.common.hair;

public enum HairJointStyle {
	BLOCKS,
	CONNECTED;

	public static HairJointStyle byId(int id) {
		HairJointStyle[] values = values();
		return id >= 0 && id < values.length ? values[id] : BLOCKS;
	}

	public HairJointStyle next() {
		HairJointStyle[] values = values();
		return values[(ordinal() + 1) % values.length];
	}
}
