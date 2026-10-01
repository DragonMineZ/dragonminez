package com.dragonminez.server.world.data;

import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DragonBallSavedData extends SavedData {
	private final Map<String, Map<Integer, List<BlockPos>>> activeBallsBySet = new HashMap<>();
	private final Map<String, Map<Integer, List<BlockPos>>> pendingBallsBySet = new HashMap<>();
	private final Set<String> firstSpawnedSetIds = new HashSet<>();
	private final Map<String, Integer> spawnedCopiesBySet = new HashMap<>();
	private final Map<UUID, Summon> summons = new HashMap<>();

	public record Summon(String setId, long expiresAt) {}

	public DragonBallSavedData() {
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			activeBallsBySet.put(definition.getId(), createStarMap(definition));
			pendingBallsBySet.put(definition.getId(), createStarMap(definition));
		}
	}

	private static Map<Integer, List<BlockPos>> createStarMap(DragonBallSetDefinition definition) {
		Map<Integer, List<BlockPos>> map = new HashMap<>();
		for (int star : definition.getStars()) {
			map.put(star, new ArrayList<>());
		}
		return map;
	}

	private static Map<Integer, List<BlockPos>> createStarMapForSetId(String setId) {
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		return definition == null ? new HashMap<>() : createStarMap(definition);
	}

	public static DragonBallSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(DragonBallSavedData::load, DragonBallSavedData::new, "dragon_balls_data");
	}

	public Map<Integer, List<BlockPos>> getActiveBalls(String setId) {
		return activeBallsBySet.computeIfAbsent(setId, DragonBallSavedData::createStarMapForSetId);
	}

	public Map<Integer, List<BlockPos>> getPendingBalls(String setId) {
		return pendingBallsBySet.computeIfAbsent(setId, DragonBallSavedData::createStarMapForSetId);
	}

	public List<BlockPos> getActiveBalls(String setId, int star) {
		return getActiveBalls(setId).computeIfAbsent(star, ignored -> new ArrayList<>());
	}

	public List<BlockPos> getPendingBalls(String setId, int star) {
		return getPendingBalls(setId).computeIfAbsent(star, ignored -> new ArrayList<>());
	}

	public int getTrackedCount(String setId, int star) {
		return getActiveBalls(setId, star).size() + getPendingBalls(setId, star).size();
	}

	public Set<String> getKnownSetIds() {
		Set<String> ids = new LinkedHashSet<>(activeBallsBySet.keySet());
		ids.addAll(pendingBallsBySet.keySet());
		return ids;
	}

	public List<BlockPos> getAllKnownPositionsForRadar(String setId) {
		List<BlockPos> allPos = new ArrayList<>();
		DragonBallSetDefinition definition = DragonBallDefinitions.getBallSet(setId);
		if (definition == null) return allPos;
		Map<Integer, List<BlockPos>> active = getActiveBalls(setId);
		Map<Integer, List<BlockPos>> pending = getPendingBalls(setId);
		for (int star : definition.getStars()) {
			allPos.addAll(active.getOrDefault(star, List.of()));
			allPos.addAll(pending.getOrDefault(star, List.of()));
		}
		return allPos;
	}

	public boolean isFirstSpawnComplete(String setId) {
		return firstSpawnedSetIds.contains(setId);
	}

	public void setFirstSpawnComplete(String setId, boolean value) {
		if (value) firstSpawnedSetIds.add(setId);
		else firstSpawnedSetIds.remove(setId);
		setDirty();
	}

	public int getSpawnedCopies(String setId) {
		return spawnedCopiesBySet.getOrDefault(setId, -1);
	}

	public void setSpawnedCopies(String setId, int copies) {
		spawnedCopiesBySet.put(setId, copies);
		setDirty();
	}

	public void addSummon(UUID dragonId, String setId, long expiresAt) {
		summons.put(dragonId, new Summon(setId, expiresAt));
		setDirty();
	}

	public Summon getSummon(UUID dragonId) {
		return summons.get(dragonId);
	}

	public Summon removeSummon(UUID dragonId) {
		Summon removed = summons.remove(dragonId);
		if (removed != null) setDirty();
		return removed;
	}

	public Map<UUID, Summon> getSummons() {
		return summons;
	}

	public static DragonBallSavedData load(CompoundTag tag) {
		DragonBallSavedData data = new DragonBallSavedData();
		if (tag.contains("SetData")) {
			CompoundTag setData = tag.getCompound("SetData");
			for (String setId : setData.getAllKeys()) {
				CompoundTag entry = setData.getCompound(setId);
				loadMap(entry.getList("Active", 10), data.getActiveBalls(setId));
				loadMap(entry.getList("Pending", 10), data.getPendingBalls(setId));
				if (entry.getBoolean("FirstSpawned")) data.firstSpawnedSetIds.add(setId);
				if (entry.contains("SpawnedCopies")) data.spawnedCopiesBySet.put(setId, entry.getInt("SpawnedCopies"));
			}
		} else {
			loadLegacySet(tag, data, "earth", "ActiveEarth", "PendingEarth", "FirstSpawnEarth");
			loadLegacySet(tag, data, "namek", "ActiveNamek", "PendingNamek", "FirstSpawnNamek");
		}
		ListTag summonList = tag.getList("Summons", 10);
		for (int i = 0; i < summonList.size(); i++) {
			CompoundTag item = summonList.getCompound(i);
			if (!item.hasUUID("Dragon")) continue;
			data.summons.put(item.getUUID("Dragon"), new Summon(item.getString("Set"), item.getLong("ExpiresAt")));
		}
		return data;
	}

	private static void loadLegacySet(CompoundTag tag, DragonBallSavedData data, String setId, String activeKey, String pendingKey, String firstKey) {
		if (tag.contains(activeKey)) loadMap(tag.getList(activeKey, 10), data.getActiveBalls(setId));
		if (tag.contains(pendingKey)) loadMap(tag.getList(pendingKey, 10), data.getPendingBalls(setId));
		if (tag.getBoolean(firstKey)) data.firstSpawnedSetIds.add(setId);
	}

	@Override
	public @NotNull CompoundTag save(CompoundTag tag) {
		Set<String> setIds = new LinkedHashSet<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) setIds.add(definition.getId());
		setIds.addAll(getKnownSetIds());
		setIds.addAll(firstSpawnedSetIds);
		setIds.addAll(spawnedCopiesBySet.keySet());

		CompoundTag setData = new CompoundTag();
		for (String setId : setIds) {
			CompoundTag entry = new CompoundTag();
			entry.put("Active", saveMap(getActiveBalls(setId)));
			entry.put("Pending", saveMap(getPendingBalls(setId)));
			entry.putBoolean("FirstSpawned", firstSpawnedSetIds.contains(setId));
			if (spawnedCopiesBySet.containsKey(setId)) entry.putInt("SpawnedCopies", spawnedCopiesBySet.get(setId));
			setData.put(setId, entry);
		}
		tag.put("SetData", setData);

		ListTag summonList = new ListTag();
		for (Map.Entry<UUID, Summon> entry : summons.entrySet()) {
			CompoundTag item = new CompoundTag();
			item.putUUID("Dragon", entry.getKey());
			item.putString("Set", entry.getValue().setId());
			item.putLong("ExpiresAt", entry.getValue().expiresAt());
			summonList.add(item);
		}
		tag.put("Summons", summonList);
		return tag;
	}

	private static void loadMap(ListTag list, Map<Integer, List<BlockPos>> map) {
		for (int i = 0; i < list.size(); i++) {
			CompoundTag item = list.getCompound(i);
			int star = item.getInt("Star");
			BlockPos pos = NbtUtils.readBlockPos(item.getCompound("Pos"));
			List<BlockPos> positions = map.computeIfAbsent(star, ignored -> new ArrayList<>());
			if (!positions.contains(pos)) positions.add(pos);
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
}
