package com.dragonminez.common.network;

import net.minecraft.nbt.CompoundTag;

public interface StatsGroupsPacket {
	int playerId();

	CompoundTag groups();
}
