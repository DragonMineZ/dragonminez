package com.dragonminez.server.world.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NPCPlacementSavedData extends SavedData {
	private static final String DATA_NAME = "dragonminez_npc_placements";
	private static final String PLACEMENTS_KEY = "placements";

	private final Map<String, UUID> placements = new HashMap<>();

	public static NPCPlacementSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(NPCPlacementSavedData::load, NPCPlacementSavedData::new, DATA_NAME);
	}

	public boolean hasPlacement(String placementId) {
		return placements.containsKey(placementId);
	}

	public Optional<UUID> getEntityUuid(String placementId) {
		return Optional.ofNullable(placements.get(placementId));
	}

	public void markSpawned(String placementId, UUID uuid) {
		if (placementId == null || placementId.isBlank() || uuid == null) {
			return;
		}
		placements.put(placementId, uuid);
		setDirty();
	}

	public void clear(String placementId) {
		if (placements.remove(placementId) != null) {
			setDirty();
		}
	}

	public static NPCPlacementSavedData load(CompoundTag tag) {
		NPCPlacementSavedData data = new NPCPlacementSavedData();
		CompoundTag placementsTag = tag.getCompound(PLACEMENTS_KEY);
		for (String placementId : placementsTag.getAllKeys()) {
			try {
				data.placements.put(placementId, UUID.fromString(placementsTag.getString(placementId)));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return data;
	}

	@Override
	public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
		CompoundTag placementsTag = new CompoundTag();
		for (Map.Entry<String, UUID> entry : placements.entrySet()) {
			placementsTag.putString(entry.getKey(), entry.getValue().toString());
		}
		tag.put(PLACEMENTS_KEY, placementsTag);
		return tag;
	}
}
