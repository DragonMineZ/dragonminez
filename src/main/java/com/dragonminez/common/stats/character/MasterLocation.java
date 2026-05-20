package com.dragonminez.common.stats.character;

import net.minecraft.core.BlockPos;

public class MasterLocation {
	private final String masterId;
	private final String displayName;
	private final String dimension;
	private final BlockPos position;

	public MasterLocation(String masterId, String displayName, String dimension, BlockPos position) {
		this.masterId = masterId;
		this.displayName = displayName;
		this.dimension = dimension;
		this.position = position;
	}

	public String getMasterId() { return masterId; }
	public String getDisplayName() { return displayName; }
	public String getDimension() { return dimension; }
	public BlockPos getPosition() { return position; }
}