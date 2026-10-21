package com.dragonminez.server.world.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;

public class PothalaPairSavedData extends SavedData {
	private static final String FILE_NAME = "dragonminez_pothala_pairs";

	private final Set<Integer> issued = new HashSet<>();

	public PothalaPairSavedData() {}

	public static PothalaPairSavedData get(MinecraftServer server) {
		DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
		return storage.computeIfAbsent(PothalaPairSavedData::load, PothalaPairSavedData::new, FILE_NAME);
	}

	public static PothalaPairSavedData load(CompoundTag tag) {
		PothalaPairSavedData data = new PothalaPairSavedData();
		for (int id : tag.getIntArray("Issued")) data.issued.add(id);
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		int[] ids = new int[issued.size()];
		int i = 0;
		for (int id : issued) ids[i++] = id;
		tag.putIntArray("Issued", ids);
		return tag;
	}

	public int issue(RandomSource random) {
		int id;
		do {
			id = random.nextInt(Integer.MAX_VALUE) + 1;
		} while (!issued.add(id));
		setDirty();
		return id;
	}

	public void reserve(int id) {
		if (id != 0 && issued.add(id)) setDirty();
	}
}
