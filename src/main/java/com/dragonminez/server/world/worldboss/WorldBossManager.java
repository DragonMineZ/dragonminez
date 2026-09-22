package com.dragonminez.server.world.worldboss;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.WorldBossStateS2C;
import com.dragonminez.common.init.entities.worldboss.AllWorldBossesEntity;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class WorldBossManager {

    public static final String JANEMBA = "janemba";

    public static final long RESPAWN_TICKS = 72000L;

    private static final int LAIR_DISTANCE = 1000;
    private static final int ACTIVATION_RADIUS = 128;
    private static final int TICK_INTERVAL = 20;
    private static final int MISSING_CHECKS_BEFORE_LOST = 30;
    private static int missingChecks;
    private static final int SYNC_INTERVAL = 200;
    private static final long LAIR_SALT = 0x4A414E454D4241L;
    private static final int HELL_SCAN_TOP = 92;
    private static final int HELL_SCAN_BOTTOM = -50;

    private WorldBossManager() {}

    public static void tick(ServerLevel level) {
        if (!level.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return;
        WorldBossSessions.tick(level);
        if (level.getGameTime() % TICK_INTERVAL != 0) return;

        WorldBossSavedData data = WorldBossSavedData.get(level.getServer());
        WorldBossSavedData.Entry entry = data.entry(JANEMBA);

        if (level.getGameTime() % SYNC_INTERVAL == 0) syncToAll(level);

        if (entry.lair == null) {
            entry.lair = pickLairColumn(level);
            entry.lairResolved = false;
            data.markDirty();
            LogUtil.info(Env.SERVER, "World boss lair column for {} chosen at {}", JANEMBA, entry.lair);
        }

        if (!hasNearbyPlayer(level, entry.lair)) return;
        if (!JanembaArena.isAreaLoaded(level, entry.lair)) return;

        if (!entry.lairResolved) {
            BlockPos ground = findHellSurface(level, entry.lair.getX(), entry.lair.getZ());
            if (ground == null) return;
            entry.lair = ground;
            entry.lairResolved = true;
            data.markDirty();
            LogUtil.info(Env.SERVER, "World boss lair for {} resolved to {}", JANEMBA, ground);
        }

        if (!entry.arenaBuilt) {
            JanembaArena.build(level, entry.lair);
            entry.arenaBuilt = true;
            data.markDirty();
        }

        if (isBossPresent(level, entry, data)) return;
        if (level.getGameTime() < entry.nextRespawnTick) return;

        spawnBoss(level, entry, data);
    }

    private static boolean hasNearbyPlayer(ServerLevel level, BlockPos lair) {
        for (Player player : level.players()) {
            if (player.isSpectator()) continue;
            if (horizontalDistance(player.blockPosition(), lair) <= ACTIVATION_RADIUS) return true;
        }
        return false;
    }

    private static boolean isBossPresent(ServerLevel level, WorldBossSavedData.Entry entry, WorldBossSavedData data) {
        if (entry.bossId == null) return false;

        Entity existing = level.getEntity(entry.bossId);
        if (existing instanceof WorldBossEntity boss && boss.isAlive()) {
            missingChecks = 0;
            return true;
        }

        if (!level.areEntitiesLoaded(ChunkPos.asLong(entry.lair))) {
            missingChecks = 0;
            return true;
        }

        if (++missingChecks < MISSING_CHECKS_BEFORE_LOST) return true;

        LogUtil.warn(Env.SERVER, "World boss {} vanished without dying, clearing its record", JANEMBA);
        missingChecks = 0;
        entry.bossId = null;
        data.markDirty();
        return false;
    }

    public static boolean isRegistered(WorldBossEntity boss) {
        if (boss.level().isClientSide || boss.getServer() == null) return true;
        WorldBossSavedData.Entry entry = WorldBossSavedData.get(boss.getServer()).peek(JANEMBA);
        return entry == null || entry.bossId == null || entry.bossId.equals(boss.getUUID());
    }

    public static void onBossRemoved(WorldBossEntity boss) {
        if (boss.level().isClientSide || boss.getServer() == null) return;
        WorldBossSavedData data = WorldBossSavedData.get(boss.getServer());
        WorldBossSavedData.Entry entry = data.peek(JANEMBA);
        if (entry == null || entry.bossId == null || !entry.bossId.equals(boss.getUUID())) return;

        entry.bossId = null;
        data.markDirty();
    }

    private static void spawnBoss(ServerLevel level, WorldBossSavedData.Entry entry, WorldBossSavedData data) {
        AllWorldBossesEntity.JanembaFat boss = MainEntities.WORLDBOSS_JANEMBA_FAT.get().create(level);
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
        syncToAll(level);
        LogUtil.info(Env.SERVER, "World boss {} spawned at {}", JANEMBA, spawn);
    }

    public static void onBossTransformed(WorldBossEntity next) {
        if (!(next.level() instanceof ServerLevel level)) return;

        WorldBossSavedData data = WorldBossSavedData.get(level.getServer());
        WorldBossSavedData.Entry entry = data.peek(JANEMBA);
        if (entry == null) return;

        entry.bossId = next.getUUID();
        data.markDirty();
    }

    public static void onBossDefeated(WorldBossEntity boss) {
        if (!(boss.level() instanceof ServerLevel level)) return;

        WorldBossSessions.onBossDefeated(boss);

        WorldBossSavedData data = WorldBossSavedData.get(level.getServer());
        WorldBossSavedData.Entry entry = data.peek(JANEMBA);
        if (entry == null) return;

        entry.bossId = null;
        entry.arenaBuilt = false;
        entry.nextRespawnTick = level.getGameTime() + RESPAWN_TICKS;
        data.markDirty();
        syncToAll(level);
        WorldBossContribution.clear(boss.getWorldBossKey());
        LogUtil.info(Env.SERVER, "World boss {} defeated, respawning in {} ticks", JANEMBA, RESPAWN_TICKS);
    }

    public static void syncToAll(ServerLevel level) {
        WorldBossSavedData.Entry entry = WorldBossSavedData.get(level.getServer()).peek(JANEMBA);
        if (entry == null) return;

        boolean alive = entry.bossId != null;
        long remaining = Math.max(0L, entry.nextRespawnTick - level.getGameTime());

        NetworkHandler.sendToAllPlayers(new WorldBossStateS2C(
                entry.lair != null, entry.lair, alive, remaining));
    }

    public static void syncTo(ServerPlayer player) {
        ServerLevel otherworld = otherworld(player.server);
        if (otherworld == null) return;

        WorldBossSavedData.Entry entry = WorldBossSavedData.get(player.server).peek(JANEMBA);
        if (entry == null) return;

        boolean alive = entry.bossId != null;
        long remaining = Math.max(0L, entry.nextRespawnTick - otherworld.getGameTime());

        NetworkHandler.sendToPlayer(new WorldBossStateS2C(
                entry.lair != null, entry.lair, alive, remaining), player);
    }

    public static BlockPos getLair(MinecraftServer server) {
        WorldBossSavedData.Entry entry = WorldBossSavedData.get(server).peek(JANEMBA);
        return entry == null ? null : entry.lair;
    }

    public static long getRespawnRemainingTicks(MinecraftServer server) {
        ServerLevel otherworld = server.getLevel(OtherworldDimension.OTHERWORLD_KEY);
        if (otherworld == null) return 0L;

        WorldBossSavedData.Entry entry = WorldBossSavedData.get(server).peek(JANEMBA);
        if (entry == null) return 0L;

        long remaining = entry.nextRespawnTick - otherworld.getGameTime();
        return Math.max(0L, remaining);
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static BlockPos pickLairColumn(ServerLevel level) {
        RandomSource random = RandomSource.create(level.getSeed() ^ LAIR_SALT);
        double angle = random.nextDouble() * Math.PI * 2.0D;

        int x = Mth.floor(Math.cos(angle) * LAIR_DISTANCE);
        int z = Mth.floor(Math.sin(angle) * LAIR_DISTANCE);
        return new BlockPos(x, HELL_SCAN_TOP, z);
    }

    private static BlockPos findHellSurface(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = HELL_SCAN_TOP; y >= HELL_SCAN_BOTTOM; y--) {
            cursor.set(x, y, z);
            if (!level.getBlockState(cursor).isAir()) return new BlockPos(x, y, z);
        }
        return null;
    }

    public static ServerLevel otherworld(MinecraftServer server) {
        return server.getLevel(OtherworldDimension.OTHERWORLD_KEY);
    }

    public static ServerLevel overworld(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD);
    }
}
