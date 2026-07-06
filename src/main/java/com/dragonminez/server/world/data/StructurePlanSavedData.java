package com.dragonminez.server.world.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StructurePlanSavedData extends SavedData {
	private static final String NAME = "dragonminez_structure_plan";

	private boolean resolved = false;
	private final Map<Integer, ChunkPos> positions = new HashMap<>();

	public static StructurePlanSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(StructurePlanSavedData::load, StructurePlanSavedData::new, NAME);
	}

	public boolean isResolved() {
		return resolved;
	}

	public Map<Integer, ChunkPos> getPositions() {
		return Collections.unmodifiableMap(positions);
	}

	public void setResolved(Map<Integer, ChunkPos> resolvedPositions) {
		this.positions.clear();
		if (resolvedPositions != null) this.positions.putAll(resolvedPositions);
		this.resolved = true;
		setDirty();
	}

	public static StructurePlanSavedData load(CompoundTag tag) {
		StructurePlanSavedData data = new StructurePlanSavedData();
		data.resolved = tag.getBoolean("resolved");
		ListTag list = tag.getList("positions", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			data.positions.put(entry.getInt("salt"), new ChunkPos(entry.getInt("x"), entry.getInt("z")));
		}
		return data;
	}

	@Override
	public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
		tag.putBoolean("resolved", resolved);
		ListTag list = new ListTag();
		for (Map.Entry<Integer, ChunkPos> e : positions.entrySet()) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("salt", e.getKey());
			entry.putInt("x", e.getValue().x);
			entry.putInt("z", e.getValue().z);
			list.add(entry);
		}
		tag.put("positions", list);
		return tag;
	}
}
