package com.dragonminez.server.world.data;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MutantSavedData extends SavedData {
	private static final String FILE_NAME = "dragonminez_mutants";

	private final Set<UUID> holders = new HashSet<>();
	@Getter
	private long nextRollTick = -1L;

	public MutantSavedData() {}

	public static MutantSavedData get(MinecraftServer server) {
		DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
		return storage.computeIfAbsent(MutantSavedData::load, MutantSavedData::new, FILE_NAME);
	}

	public static MutantSavedData load(CompoundTag tag) {
		MutantSavedData data = new MutantSavedData();
		ListTag holdersList = tag.getList("Holders", Tag.TAG_COMPOUND);
		for (int i = 0; i < holdersList.size(); i++) {
			CompoundTag holderTag = holdersList.getCompound(i);
			if (holderTag.hasUUID("Id")) data.holders.add(holderTag.getUUID("Id"));
		}
		data.nextRollTick = tag.contains("NextRollTick") ? tag.getLong("NextRollTick") : -1L;
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag holdersList = new ListTag();
		for (UUID holder : holders) {
			CompoundTag holderTag = new CompoundTag();
			holderTag.putUUID("Id", holder);
			holdersList.add(holderTag);
		}
		tag.put("Holders", holdersList);
		tag.putLong("NextRollTick", nextRollTick);
		return tag;
	}

	public boolean isHolder(UUID playerId) {
		return holders.contains(playerId);
	}

	public Set<UUID> getHolders() {
		return new HashSet<>(holders);
	}

	public int count() {
		return holders.size();
	}

	public void addHolder(UUID playerId) {
		if (holders.add(playerId)) setDirty();
	}

	public void removeHolder(UUID playerId) {
		if (holders.remove(playerId)) setDirty();
	}

	public void setNextRollTick(long tick) {
		this.nextRollTick = tick;
		setDirty();
	}
}
