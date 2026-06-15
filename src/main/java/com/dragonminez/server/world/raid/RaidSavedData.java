package com.dragonminez.server.world.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class RaidSavedData extends SavedData {

	private static final String FILE_NAME = "dragonminez_raids";

	private final Map<UUID, Raid> raids = new HashMap<>();

	public static RaidSavedData get(MinecraftServer server) {
		DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
		return storage.computeIfAbsent(RaidSavedData::load, RaidSavedData::new, FILE_NAME);
	}

	public static RaidSavedData load(CompoundTag tag) {
		RaidSavedData data = new RaidSavedData();
		ListTag list = tag.getList("Raids", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			Raid raid = Raid.load(list.getCompound(i));
			if (!raid.isFinished()) data.raids.put(raid.getRaidId(), raid);
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Raid raid : raids.values()) {
			if (!raid.isFinished()) list.add(raid.save());
		}
		tag.put("Raids", list);
		return tag;
	}

	public void addRaid(Raid raid) {
		raids.put(raid.getRaidId(), raid);
		setDirty();
	}

	public Collection<Raid> getRaids() {
		return raids.values();
	}

	public Raid getRaid(UUID id) {
		return raids.get(id);
	}

	/** Ticks every raid belonging to this level, pruning any that finished. */
	public void tick(ServerLevel level) {
		if (raids.isEmpty()) return;

		boolean changed = false;
		Iterator<Map.Entry<UUID, Raid>> it = raids.entrySet().iterator();
		while (it.hasNext()) {
			Raid raid = it.next().getValue();
			if (!raid.getDimension().equals(level.dimension())) continue;

			raid.tick(level);
			if (raid.isFinished()) it.remove();
			changed = true;
		}
		if (changed) setDirty();
	}

	/** The closest active raid to {@code pos} within {@code range}, or {@code null}. */
	public Raid findNearbyRaid(ServerLevel level, BlockPos pos, double range) {
		Raid best = null;
		double bestSqr = range * range;
		for (Raid raid : raids.values()) {
			if (raid.isFinished() || !raid.getDimension().equals(level.dimension())) continue;
			double d = raid.getCenter().distSqr(pos);
			if (d <= bestSqr) {
				bestSqr = d;
				best = raid;
			}
		}
		return best;
	}
}
