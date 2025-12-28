package com.dragonminez.server.world.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DragonBallSavedData extends SavedData {
	private final Map<Integer, BlockPos> activeEarthBalls = new HashMap<>(), activeNamekBalls = new HashMap<>();
	private final Map<Integer, BlockPos> pendingEarthBalls = new HashMap<>(), pendingNamekBalls = new HashMap<>();
	private boolean firstSpawnEarth = false, firstSpawnNamek = false;

	public static DragonBallSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(DragonBallSavedData::load, DragonBallSavedData::new, "dragon_balls_data");
	}public Map<Integer, BlockPos> getActiveBalls(boolean isNamek) {
		return isNamek ? activeNamekBalls : activeEarthBalls;
	}

	public Map<Integer, BlockPos> getPendingBalls(boolean isNamek) {
		return isNamek ? pendingNamekBalls : pendingEarthBalls;
	}

	public Map<Integer, BlockPos> getAllPositionsForRadar(boolean isNamek) {
		Map<Integer, BlockPos> combined = new HashMap<>(isNamek ? activeNamekBalls : activeEarthBalls);
		combined.putAll(isNamek ? pendingNamekBalls : pendingEarthBalls);
		return combined;
	}

	public void addActiveBall(int star, BlockPos pos, boolean isNamek) {
		Map<Integer, BlockPos> map = isNamek ? activeNamekBalls : activeEarthBalls;
		map.put(star, pos);
		(isNamek ? pendingNamekBalls : pendingEarthBalls).remove(star);
		setDirty();
	}

	public void clearPending(boolean isNamek) {
		(isNamek ? pendingNamekBalls : pendingEarthBalls).clear();
		setDirty();
	}

	public void removeActiveBall(BlockPos pos, boolean isNamek) {
		Map<Integer, BlockPos> map = isNamek ? activeNamekBalls : activeEarthBalls;
		map.values().removeIf(p -> p.equals(pos));
		setDirty();
	}

	public void addPendingBall(int star, BlockPos pos, boolean isNamek) {
		Map<Integer, BlockPos> map = isNamek ? pendingNamekBalls : pendingEarthBalls;
		map.put(star, pos);
		(isNamek ? activeNamekBalls : activeEarthBalls).remove(star);
		setDirty();
	}

	public boolean hasFirstSpawnHappened(boolean isNamek) {
		return isNamek ? firstSpawnNamek : firstSpawnEarth;
	}

	public void setFirstSpawnHappened(boolean isNamek) {
		if (isNamek) this.firstSpawnNamek = true;
		else this.firstSpawnEarth = true;
		setDirty();
	}

	public DragonBallSavedData() {}

	public static DragonBallSavedData load(CompoundTag tag) {
		DragonBallSavedData data = new DragonBallSavedData();
		data.firstSpawnEarth = tag.getBoolean("FirstSpawnEarth");
		data.firstSpawnNamek = tag.getBoolean("FirstSpawnNamek");

		loadMap(tag.getList("ActiveEarth", 10), data.activeEarthBalls);
		loadMap(tag.getList("ActiveNamek", 10), data.activeNamekBalls);
		loadMap(tag.getList("PendingEarth", 10), data.pendingEarthBalls);
		loadMap(tag.getList("PendingNamek", 10), data.pendingNamekBalls);
		return data;
	}

	@Override
	public @NotNull CompoundTag save(CompoundTag tag) {
		tag.putBoolean("FirstSpawnEarth", firstSpawnEarth);
		tag.putBoolean("FirstSpawnNamek", firstSpawnNamek);

		tag.put("ActiveEarth", saveMap(activeEarthBalls));
		tag.put("ActiveNamek", saveMap(activeNamekBalls));
		tag.put("PendingEarth", saveMap(pendingEarthBalls));
		tag.put("PendingNamek", saveMap(pendingNamekBalls));
		return tag;
	}

	private static void loadMap(ListTag list, Map<Integer, BlockPos> map) {
		map.clear();
		for (int i = 0; i < list.size(); i++) {
			CompoundTag item = list.getCompound(i);
			map.put(item.getInt("Star"), NbtUtils.readBlockPos(item.getCompound("Pos")));
		}
	}

	private static ListTag saveMap(Map<Integer, BlockPos> map) {
		ListTag list = new ListTag();
		map.forEach((star, pos) -> {
			CompoundTag item = new CompoundTag();
			item.putInt("Star", star);
			item.put("Pos", NbtUtils.writeBlockPos(pos));
			list.add(item);
		});
		return list;
	}
}