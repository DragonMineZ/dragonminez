package com.dragonminez.server.world.worldboss;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.WorldBossStateS2C;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorldBossManager {

    private static final List<WorldBossLair> LAIRS = List.of(new WorldBossLair.Janemba(), new WorldBossLair.Turles());

    private static final int ACTIVATION_RADIUS = 128;
    private static final int TICK_INTERVAL = 20;
    private static final int MISSING_CHECKS_BEFORE_LOST = 30;
    private static final Map<String, Integer> MISSING_CHECKS = new HashMap<>();
    private static final int SYNC_INTERVAL = 200;
    private static final int DISCOVERY_INTERVAL = 20;
    private static final int MISSING_BOSS_TICKS_BEFORE_END = 200;

    private static final Map<String, WorldBossSession> ACTIVE = new HashMap<>();

    private WorldBossManager() {}

    public static List<WorldBossLair> lairs() {
        return LAIRS;
    }

    public static WorldBossLair lair(String key) {
        for (WorldBossLair lair : LAIRS) {
            if (lair.key().equals(key)) return lair;
        }
        return null;
    }

    public static void tick(ServerLevel level) {
        tickSessions(level);
        if (level.getGameTime() % TICK_INTERVAL != 0) return;

        for (WorldBossLair lair : LAIRS) {
            if (lair.dimension().equals(level.dimension())) tickLair(level, lair);
        }
    }

    private static void tickLair(ServerLevel level, WorldBossLair lair) {
        Data data = Data.get(level.getServer());
        Data.Entry entry = data.entry(lair.key());

        if (level.getGameTime() % SYNC_INTERVAL == 0) syncToAll(level, lair.key());

        if (!lair.isReady(level)) return;

        if (entry.lair == null) {
            BlockPos column = lair.pickColumn(level);
            if (column == null) return;
            entry.lair = column;
            entry.lairResolved = false;
            data.markDirty();
            LogUtil.info(Env.SERVER, "World boss lair column for {} chosen at {}", lair.key(), entry.lair);
        }

        if (!hasNearbyPlayer(level, entry.lair)) return;
        if (!lair.isAreaLoaded(level, entry.lair)) return;

        if (!entry.lairResolved) {
            BlockPos ground = lair.resolveGround(level, entry.lair);
            if (ground == null) return;
            entry.lair = ground;
            entry.lairResolved = true;
            data.markDirty();
            LogUtil.info(Env.SERVER, "World boss lair for {} resolved to {}", lair.key(), ground);
        }

        if (!entry.arenaBuilt) {
            lair.buildArena(level, entry.lair);
            entry.arenaBuilt = true;
            data.markDirty();
        }

        if (isBossPresent(level, lair, entry, data)) return;
        if (level.getGameTime() < entry.nextRespawnTick) return;

        spawnBoss(level, lair, entry, data);
    }

    private static boolean hasNearbyPlayer(ServerLevel level, BlockPos lair) {
        for (Player player : level.players()) {
            if (player.isSpectator()) continue;
            if (horizontalDistance(player.blockPosition(), lair) <= ACTIVATION_RADIUS) return true;
        }
        return false;
    }

    private static boolean isBossPresent(ServerLevel level, WorldBossLair lair, Data.Entry entry, Data data) {
        if (entry.bossId == null) return false;

        Entity existing = level.getEntity(entry.bossId);
        if (existing instanceof WorldBossEntity boss && boss.isAlive()) {
            MISSING_CHECKS.remove(lair.key());
            return true;
        }

        if (!level.areEntitiesLoaded(ChunkPos.asLong(entry.lair))) {
            MISSING_CHECKS.remove(lair.key());
            return true;
        }

        int missing = MISSING_CHECKS.merge(lair.key(), 1, Integer::sum);
        if (missing < MISSING_CHECKS_BEFORE_LOST) return true;

        LogUtil.warn(Env.SERVER, "World boss {} vanished without dying, clearing its record", lair.key());
        MISSING_CHECKS.remove(lair.key());
        entry.bossId = null;
        data.markDirty();
        return false;
    }

    public static boolean isRegistered(WorldBossEntity boss) {
        if (boss.level().isClientSide || boss.getServer() == null) return true;
        Data.Entry entry = Data.get(boss.getServer()).peek(boss.getWorldBossKey());
        return entry == null || entry.bossId == null || entry.bossId.equals(boss.getUUID());
    }

    public static void onBossRemoved(WorldBossEntity boss) {
        if (boss.level().isClientSide || boss.getServer() == null) return;
        Data data = Data.get(boss.getServer());
        Data.Entry entry = data.peek(boss.getWorldBossKey());
        if (entry == null || entry.bossId == null || !entry.bossId.equals(boss.getUUID())) return;

        entry.bossId = null;
        data.markDirty();
    }

    private static void spawnBoss(ServerLevel level, WorldBossLair lair, Data.Entry entry, Data data) {
        WorldBossEntity boss = lair.createBoss(level);
        if (boss == null) return;

        BlockPos spawn = entry.lair.above();
        boss.setAnchor(entry.lair);
        boss.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.EVENT, null, null);
        boss.fallAsleep();

        if (!level.addFreshEntity(boss)) return;

        entry.bossId = boss.getUUID();
        entry.arenaBuilt = true;
        data.markDirty();
        syncToAll(level, lair.key());
        LogUtil.info(Env.SERVER, "World boss {} spawned at {}", lair.key(), spawn);
    }

    public static void onBossTransformed(WorldBossEntity next) {
        if (!(next.level() instanceof ServerLevel level)) return;

        Data data = Data.get(level.getServer());
        Data.Entry entry = data.peek(next.getWorldBossKey());
        if (entry == null) return;

        entry.bossId = next.getUUID();
        data.markDirty();

        WorldBossSession session = ACTIVE.get(next.getWorldBossKey());
        if (session == null || session.isEnded() || next.isBossAsleep()) return;
        session.attachBoss(next);
    }

    public static void onBossDefeated(WorldBossEntity boss) {
        if (!(boss.level() instanceof ServerLevel level)) return;

        WorldBossSession session = onBossEngaged(boss);
        if (session != null) {
            session.finishVictory(level, boss);
            ACTIVE.remove(boss.getWorldBossKey());
        }

        String key = boss.getWorldBossKey();
        Data data = Data.get(level.getServer());
        Data.Entry entry = data.peek(key);
        if (entry == null) return;

        WorldBossLair lair = lair(key);
        long respawnTicks = lair != null ? lair.respawnTicks() : 72000L;

        entry.bossId = null;
        entry.arenaBuilt = false;
        entry.nextRespawnTick = level.getGameTime() + respawnTicks;
        data.markDirty();
        syncToAll(level, key);
        WorldBossContribution.clear(key);
        LogUtil.info(Env.SERVER, "World boss {} defeated, respawning in {} ticks", key, respawnTicks);
    }

    public static void syncToAll(ServerLevel level, String key) {
        WorldBossStateS2C packet = statePacket(level.getServer(), key);
        if (packet != null) NetworkHandler.sendToAllPlayers(packet);
    }

    public static void syncTo(ServerPlayer player) {
        for (WorldBossLair lair : LAIRS) {
            WorldBossStateS2C packet = statePacket(player.server, lair.key());
            if (packet != null) NetworkHandler.sendToPlayer(packet, player);
        }
    }

    private static WorldBossStateS2C statePacket(MinecraftServer server, String key) {
        Data.Entry entry = Data.get(server).peek(key);
        if (entry == null) return null;

        boolean alive = entry.bossId != null;
        long remaining = Math.max(0L, entry.nextRespawnTick - server.overworld().getGameTime());
        return new WorldBossStateS2C(key, entry.lair != null, entry.lair, alive, remaining);
    }

    public static BlockPos getLair(MinecraftServer server, String key) {
        Data.Entry entry = Data.get(server).peek(key);
        return entry == null ? null : entry.lair;
    }

    public static long getRespawnRemainingTicks(MinecraftServer server, String key) {
        Data.Entry entry = Data.get(server).peek(key);
        if (entry == null) return 0L;
        return Math.max(0L, entry.nextRespawnTick - server.overworld().getGameTime());
    }

    public static ServerLevel levelFor(MinecraftServer server, String key) {
        WorldBossLair lair = lair(key);
        return lair == null ? null : server.getLevel(lair.dimension());
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static WorldBossSession onBossEngaged(WorldBossEntity boss) {
        if (!(boss.level() instanceof ServerLevel level)) return null;
        WorldBossSession session = ACTIVE.get(boss.getWorldBossKey());
        if (session != null && session.isEnded()) {
            ACTIVE.remove(boss.getWorldBossKey());
            session = null;
        }
        if (session == null) {
            session = new WorldBossSession(boss.getWorldBossKey(), level);
            ACTIVE.put(boss.getWorldBossKey(), session);
            LogUtil.info(Env.SERVER, "World boss fight started for {}", boss.getWorldBossKey());
        }
        session.attachBoss(boss);
        return session;
    }

    public static void onBossReset(WorldBossEntity boss) {
        WorldBossSession session = ACTIVE.get(boss.getWorldBossKey());
        if (session == null || !(boss.level() instanceof ServerLevel level)) return;
        session.finishDefeat(level);
        ACTIVE.remove(boss.getWorldBossKey());
    }

    public static void endFight(String bossKey, ServerLevel level) {
        WorldBossSession session = ACTIVE.remove(bossKey);
        if (session == null || level == null) return;
        session.finishDefeat(level);
    }

    public static boolean isFightEngaged(WorldBossEntity boss, double leashRadius) {
        WorldBossSession session = ACTIVE.get(boss.getWorldBossKey());
        if (session == null || session.isEnded() || !(boss.level() instanceof ServerLevel level)) return false;
        return session.hasEngagedParticipant(level, boss, leashRadius);
    }

    private static void tickSessions(ServerLevel level) {
        if (level.getGameTime() % DISCOVERY_INTERVAL == 0) discoverAwakeBosses(level);
        if (ACTIVE.isEmpty()) return;

        List<String> ended = new ArrayList<>();
        for (WorldBossSession session : new ArrayList<>(ACTIVE.values())) {
            if (!session.dimension().equals(level.dimension())) continue;
            WorldBossEntity boss = session.resolveBoss(level);
            if (boss == null) {
                if (session.noteBossMissing() >= MISSING_BOSS_TICKS_BEFORE_END) {
                    LogUtil.warn(Env.SERVER, "World boss {} vanished mid-fight, ending the fight as lost", session.bossKey());
                    session.finishDefeat(level);
                    ended.add(session.bossKey());
                }
                continue;
            }
            if (boss.isBossAsleep()) {
                session.finishDefeat(level);
                ended.add(session.bossKey());
                continue;
            }
            session.tick(level, boss);
            if (session.isEnded()) ended.add(session.bossKey());
        }
        for (String key : ended) ACTIVE.remove(key);
    }

    private static void discoverAwakeBosses(ServerLevel level) {
        Data data = null;
        for (WorldBossLair lair : LAIRS) {
            if (!lair.dimension().equals(level.dimension()) || ACTIVE.containsKey(lair.key())) continue;
            if (data == null) data = Data.get(level.getServer());
            Data.Entry entry = data.peek(lair.key());
            if (entry == null || entry.bossId == null) continue;
            Entity entity = level.getEntity(entry.bossId);
            if (entity instanceof WorldBossEntity boss && boss.isAlive() && !boss.isBossAsleep()) onBossEngaged(boss);
        }
    }

    public static WorldBossSession activeFor(ServerPlayer player) {
        if (player == null) return null;
        for (WorldBossSession session : ACTIVE.values()) {
            if (!session.isEnded() && session.isParticipant(player)) return session;
        }
        return null;
    }

    public static boolean tryKnockOut(ServerPlayer victim, StatsData stats, DamageSource source) {
        WorldBossSession session = activeFor(victim);
        if (session == null || !(victim.level() instanceof ServerLevel level)) return false;
        return session.knockOut(level, victim, stats, source);
    }

    public static boolean isKnockedOut(ServerPlayer player) {
        WorldBossSession session = activeFor(player);
        return session != null && session.isKnockedOut(player.getUUID());
    }

    public static boolean isCastingRevive(ServerPlayer player) {
        WorldBossSession session = activeFor(player);
        return session != null && session.isCasting(player.getUUID());
    }

    public static void requestRevive(ServerPlayer caster, int targetEntityId) {
        WorldBossSession session = activeFor(caster);
        if (session == null || !(caster.level() instanceof ServerLevel level)) return;
        session.tryStartRevive(level, caster, targetEntityId);
    }

    public static void interruptRevive(ServerPlayer caster) {
        WorldBossSession session = activeFor(caster);
        if (session != null) session.interruptCast(caster, true);
    }

    public static void onPlayerLogout(ServerPlayer player) {
        for (WorldBossSession session : ACTIVE.values()) session.onPlayerLogout(player);
    }

    public static void onPlayerLogin(ServerPlayer player, StatsData data) {
        boolean handled = false;
        for (WorldBossSession session : ACTIVE.values()) {
            if (session.onPlayerLogin(player, data)) handled = true;
        }
        if (!handled && data.getTechniques().hasRevive()) {
            data.getTechniques().uninstallRevive();
        }
    }

    public static void onPlayerChangedDimension(ServerPlayer player, StatsData data) {
        for (WorldBossSession session : ACTIVE.values()) session.onPlayerLeftFight(player, data);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    public static final class Data extends SavedData {

        private static final String FILE_NAME = "dragonminez_worldbosses";

        public static final class Entry {
            public BlockPos lair;
            public boolean lairResolved;
            public boolean arenaBuilt;
            public UUID bossId;
            public long nextRespawnTick;
        }

        private final Map<String, Entry> entries = new HashMap<>();

        public static Data get(MinecraftServer server) {
            DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
            return storage.computeIfAbsent(Data::load, Data::new, FILE_NAME);
        }

        public static Data load(CompoundTag tag) {
            Data data = new Data();
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

        public Entry entry(String bossKey) {
            return entries.computeIfAbsent(bossKey, k -> new Entry());
        }

        public Entry peek(String bossKey) {
            return entries.get(bossKey);
        }

        public void markDirty() {
            setDirty();
        }
    }
}
