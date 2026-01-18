package com.dragonminez.server.storage;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public interface IDataStorage {
	void init();
	void shutdown();

	CompoundTag loadData(UUID playerUUID);

	boolean saveData(UUID playerUUID, String playerName, CompoundTag data);

	String getName();
}
