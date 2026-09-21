package com.dragonminez.server.world.worldboss;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldBossSavedData extends SavedData {

    private static final String FILE_NAME = "dragonminez_worldbosses";

    public static class Entry {
        public BlockPos lair;
        public boolean lairResolved;
        public boolean arenaBuilt;
        public UUID bossId;
        public long nextRespawnTick;
    }

    private final Map<String, Entry> entries = new HashMap<>();

    public static WorldBossSavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        return storage.computeIfAbsent(WorldBossSavedData::load, WorldBossSavedData::new, FILE_NAME);
    }

    public static WorldBossSavedData load(CompoundTag tag) {
        WorldBossSavedData data = new WorldBossSavedData();
        for (String key : tag.getAllKeys()) {
            CompoundTag e = tag.getCompound(key);
            Entry entry = new Entry();
            if (e.contains("LairX")) {
                entry.lair = new BlockPos(e.getInt("LairX"), e.getInt("LairY"), e.getInt("LairZ"));
            }
            entry.lairResolved = e.getBoolean("LairResolved");
            entry.arenaBuilt = e.getBoolean("ArenaBuilt");
            if (e.hasUUID("BossId")) entry.bossId = e.getUUID("BossId");
            entry.nextRespawnTick = e.getLong("NextRespawn");
            data.entries.put(key, entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            CompoundTag e = new CompoundTag();
            if (entry.lair != null) {
                e.putInt("LairX", entry.lair.getX());
                e.putInt("LairY", entry.lair.getY());
                e.putInt("LairZ", entry.lair.getZ());
            }
            e.putBoolean("LairResolved", entry.lairResolved);
            e.putBoolean("ArenaBuilt", entry.arenaBuilt);
            if (entry.bossId != null) e.putUUID("BossId", entry.bossId);
            e.putLong("NextRespawn", entry.nextRespawnTick);
            tag.put(mapEntry.getKey(), e);
        }
        return tag;
    }

    public Entry entry(String bossId) {
        return entries.computeIfAbsent(bossId, k -> new Entry());
    }

    public Entry peek(String bossId) {
        return entries.get(bossId);
    }

    public void markDirty() {
        setDirty();
    }
}
