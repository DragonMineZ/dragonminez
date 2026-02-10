package com.dragonminez.server.world.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class OtherworldNPCSavedData extends SavedData {
    private boolean npcsSpawned = false;

    public static OtherworldNPCSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(OtherworldNPCSavedData::load, OtherworldNPCSavedData::new, "otherworld_npc_data");
    }

    public boolean hasNPCsSpawned() {
        return npcsSpawned;
    }

    public void setNPCsSpawned() {
        this.npcsSpawned = true;
        setDirty();
    }

    public OtherworldNPCSavedData() {}

    public static OtherworldNPCSavedData load(CompoundTag tag) {
        OtherworldNPCSavedData data = new OtherworldNPCSavedData();
        data.npcsSpawned = tag.getBoolean("NPCsSpawned");
        return data;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putBoolean("NPCsSpawned", npcsSpawned);
        return tag;
    }
}

