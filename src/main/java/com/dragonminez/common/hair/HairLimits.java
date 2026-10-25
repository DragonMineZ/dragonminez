package com.dragonminez.common.hair;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;

public final class HairLimits {
	private final int maxSegments;
	private final float[] maxLengths;
	private final float maxWidth;
	private final float maxRootOffset;
	private final int maxTotalSegments;

	private HairLimits(GeneralServerConfig.HairConfig config) {
		this.maxSegments = config.getMaxSegmentsPerStrand();
		this.maxLengths = new float[HairStyleSlot.count()];
		for (HairStyleSlot slot : HairStyleSlot.values()) maxLengths[slot.index()] = config.getMaxStrandLength(slot.getHairType());
		this.maxWidth = config.getMaxStrandWidth();
		this.maxRootOffset = config.getMaxRootOffset();
		this.maxTotalSegments = config.getMaxTotalSegmentsPerStyle();
	}

	public static HairLimits current() {
		GeneralServerConfig serverConfig = ConfigManager.getServerConfig();
		return new HairLimits(serverConfig != null ? serverConfig.getHair() : new GeneralServerConfig.HairConfig());
	}

	public int getMaxSegments() {
		return maxSegments;
	}

	public float getMaxLength(HairStyleSlot slot) {
		return maxLengths[slot.index()];
	}

	public float getMaxWidth() {
		return maxWidth;
	}

	public float getMaxRootOffset() {
		return maxRootOffset;
	}

	public int getMaxTotalSegments() {
		return maxTotalSegments;
	}
}
