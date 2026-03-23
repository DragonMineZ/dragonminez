package com.dragonminez.server.world.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DragonBallSavedData extends SavedData {
	// Ahora cada estrella (1-7) apunta a una LISTA de posiciones.
	private final Map<Integer, List<BlockPos>> activeEarthBalls = new HashMap<>();
	private final Map<Integer, List<BlockPos>> activeNamekBalls = new HashMap<>();
	private final Map<Integer, List<BlockPos>> pendingEarthBalls = new HashMap<>();
	private final Map<Integer, List<BlockPos>> pendingNamekBalls = new HashMap<>();
	private boolean firstSpawnEarth = false, firstSpawnNamek = false;

	public DragonBallSavedData() {
		// Inicializamos las listas vacías para las 7 estrellas
		for (int i = 1; i <= 7; i++) {
			activeEarthBalls.put(i, new ArrayList<>());
			activeNamekBalls.put(i, new ArrayList<>());
			pendingEarthBalls.put(i, new ArrayList<>());
			pendingNamekBalls.put(i, new ArrayList<>());
		}
	}

	public static DragonBallSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(DragonBallSavedData::load, DragonBallSavedData::new, "dragon_balls_data");
	}

	public Map<Integer, List<BlockPos>> getActiveBalls(boolean isNamek) {
		return isNamek ? activeNamekBalls : activeEarthBalls;
	}

	public Map<Integer, List<BlockPos>> getPendingBalls(boolean isNamek) {
		return isNamek ? pendingNamekBalls : pendingEarthBalls;
	}

	public List<BlockPos> getAllKnownPositionsForRadar(boolean isNamek) {
		List<BlockPos> allPos = new ArrayList<>();
		Map<Integer, List<BlockPos>> active = getActiveBalls(isNamek);
		Map<Integer, List<BlockPos>> pending = getPendingBalls(isNamek);

		for (int star = 1; star <= 7; star++) {
			List<BlockPos> activePositions = active.get(star);
			if (activePositions != null) allPos.addAll(activePositions);

			List<BlockPos> pendingPositions = pending.get(star);
			if (pendingPositions != null) allPos.addAll(pendingPositions);
		}

		return allPos;
	}

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

	private static void loadMap(ListTag list, Map<Integer, List<BlockPos>> map) {
		for (int i = 0; i < list.size(); i++) {
			CompoundTag item = list.getCompound(i);
			int star = item.getInt("Star");
			BlockPos pos = NbtUtils.readBlockPos(item.getCompound("Pos"));
			if (map.containsKey(star)) {
				map.get(star).add(pos);
			}
		}
	}

	private static ListTag saveMap(Map<Integer, List<BlockPos>> map) {
		ListTag list = new ListTag();
		for (Map.Entry<Integer, List<BlockPos>> entry : map.entrySet()) {
			for (BlockPos pos : entry.getValue()) {
				CompoundTag item = new CompoundTag();
				item.putInt("Star", entry.getKey());
				item.put("Pos", NbtUtils.writeBlockPos(pos));
				list.add(item);
			}
		}
		return list;
	}

	public boolean isFirstSpawnEarth() {
		return firstSpawnEarth;
	}

	public void setFirstSpawnEarth(boolean firstSpawnEarth) {
		this.firstSpawnEarth = firstSpawnEarth;
		this.setDirty();
	}

	public boolean isFirstSpawnNamek() {
		return firstSpawnNamek;
	}

	public void setFirstSpawnNamek(boolean firstSpawnNamek) {
		this.firstSpawnNamek = firstSpawnNamek;
		this.setDirty();
	}
}