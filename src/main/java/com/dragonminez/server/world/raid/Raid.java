package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.entities.IBattlePower;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Raid {

	public enum Status { ACTIVE, VICTORY, DEFEAT }

	/** Persistent tag for mobs omg */
	public static final String RAID_ID_TAG = "dmz_raid_id";

	@Getter
	private final UUID raidId;
	private final String typeId;
	@Getter
	private final ResourceKey<Level> dimension;
	@Getter
	private final BlockPos center;
	@Getter
	private final Set<UUID> participants;

	@Getter
	private Status status = Status.ACTIVE;
	private int currentWaveIndex = -1;                 // -1 = no wave spawned yet
	private final Set<UUID> currentWaveMobs = new HashSet<>();
	private int waveDelayTimer = 0;                     // intermission countdown before the next wave
	private int totalMobsThisWave = 0;                  // denominator for boss-bar progress

	private ServerBossEvent bossEvent;                  // transient, rebuilt on demand / after load

	public Raid(UUID raidId, String typeId, ResourceKey<Level> dimension, BlockPos center, Set<UUID> participants) {
		this.raidId = raidId;
		this.typeId = typeId;
		this.dimension = dimension;
		this.center = center;
		this.participants = new HashSet<>(participants);
	}

	public boolean isFinished() { return status != Status.ACTIVE; }
	public int getCurrentWave() { return currentWaveIndex + 1; }

	public boolean hasParticipant(UUID id) { return participants.contains(id); }
	public void removeParticipant(UUID id) { participants.remove(id); }

	private RaidType type() { return RaidTypes.getOrDefault(typeId); }

	// ------------------------------------------------------------------------------------------------
	// Tick loop
	// ------------------------------------------------------------------------------------------------

	/** Advances the raid by one tick. Called by {@link RaidSavedData} with the matching level. */
	public void tick(ServerLevel level) {
		if (status != Status.ACTIVE) return;

		RaidType type = type();
		ensureBossEvent(type);

		List<ServerPlayer> active = resolveActiveParticipants(level, type);
		refreshBossPlayers(active);

		// End condition: no participant remains alive and within range (solo death, or whole party out).
		if (active.isEmpty()) {
			fail(level);
			return;
		}

		// Kick off the first wave.
		if (currentWaveIndex < 0) {
			spawnWave(level, 0);
			return;
		}

		// Intermission between waves.
		if (waveDelayTimer > 0) {
			waveDelayTimer--;
			updateBossBar(type, totalMobsThisWave);
			if (waveDelayTimer == 0) {
				spawnWave(level, currentWaveIndex + 1);
			}
			return;
		}

		int alive = countAliveMobs(level);
		updateBossBar(type, alive);

		if (alive <= 0) {
			if (type.isFinalWave(currentWaveIndex)) {
				win(level, active);
			} else {
				waveDelayTimer = type.getInterWaveDelayTicks();
			}
		}
	}

	private int countAliveMobs(ServerLevel level) {
		int alive = 0;
		Iterator<UUID> it = currentWaveMobs.iterator();
		while (it.hasNext()) {
			Entity entity = level.getEntity(it.next());
			if (entity instanceof LivingEntity living && living.isAlive()) {
				alive++;
			} else if (entity == null) {
				// Not currently loaded (chunk unloaded). Treat as still pending so the wave doesn't
				// complete prematurely; it will be recounted once the chunk reloads.
				alive++;
			} else {
				it.remove();
			}
		}
		return alive;
	}

	// ------------------------------------------------------------------------------------------------
	// Wave spawning
	// ------------------------------------------------------------------------------------------------

	private void spawnWave(ServerLevel level, int waveIndex) {
		RaidType type = type();
		if (waveIndex >= type.waveCount()) {
			win(level, resolveActiveParticipants(level, type));
			return;
		}

		currentWaveIndex = waveIndex;
		currentWaveMobs.clear();

		RaidWave wave = type.wave(waveIndex);
		RandomSource random = level.getRandom();
		LivingEntity focus = nearestParticipant(level, type);

		for (RaidWave.SpawnEntry entry : wave.getSpawns()) {
			EntityType<?> entityType = entry.type().get();
			for (int i = 0; i < entry.count(); i++) {
				Mob mob = spawnOne(level, entityType, wave, random, focus);
				if (mob != null) currentWaveMobs.add(mob.getUUID());
			}
		}

		totalMobsThisWave = Math.max(1, currentWaveMobs.size());
		updateBossBar(type, currentWaveMobs.size());

		LogUtil.info(Env.SERVER, "Raid {} spawned wave {}/{} ({} mobs){}",
				raidId, waveIndex + 1, type.waveCount(), currentWaveMobs.size(), wave.isBossWave() ? " [BOSS]" : "");
	}

	private Mob spawnOne(ServerLevel level, EntityType<?> entityType, RaidWave wave, RandomSource random, LivingEntity focus) {
		Entity created = entityType.create(level);
		if (!(created instanceof Mob mob)) {
			if (created != null) created.discard();
			return null;
		}

		BlockPos pos = findSpawnPos(level, random);
		mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);

		// Scale after finalizeSpawn so wave multipliers aren't overwritten by default initialisation.
		applyScaling(mob, wave);

		// Tag so raid mobs can be recognised / cleaned up, and stop them despawning naturally.
		mob.getPersistentData().putString(RAID_ID_TAG, raidId.toString());
		mob.setPersistenceRequired();

		if (focus != null) mob.setTarget(focus);

		level.addFreshEntity(mob);
		return mob;
	}

	private void applyScaling(Mob mob, RaidWave wave) {
		AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null && wave.getHealthMultiplier() != 1.0) {
			maxHealth.setBaseValue(maxHealth.getBaseValue() * wave.getHealthMultiplier());
		}

		AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null && wave.getDamageMultiplier() != 1.0) {
			attack.setBaseValue(attack.getBaseValue() * wave.getDamageMultiplier());
		}

		mob.setHealth(mob.getMaxHealth());

		if (mob instanceof IBattlePower battlePower && wave.getHealthMultiplier() != 1.0) {
			battlePower.setBattlePower((int) Math.round(battlePower.getBattlePower() * wave.getHealthMultiplier()));
		}
	}

	private BlockPos findSpawnPos(ServerLevel level, RandomSource random) {
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double dist = 6.0 + random.nextDouble() * 10.0;
			int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
			int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			if (level.noCollision(new AABB(pos))) {
				return pos;
			}
		}
		return center.above();
	}

	// ------------------------------------------------------------------------------------------------
	// Participants
	// ------------------------------------------------------------------------------------------------

	/** Participants that are online, alive, in the right dimension and within leash range of the centre. */
	private List<ServerPlayer> resolveActiveParticipants(ServerLevel level, RaidType type) {
		List<ServerPlayer> active = new ArrayList<>();
		double leashSqr = type.getLeashDistance() * type.getLeashDistance();
		double cx = center.getX() + 0.5, cy = center.getY() + 0.5, cz = center.getZ() + 0.5;
		MinecraftServer server = level.getServer();

		for (UUID id : participants) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) continue;                 // offline -> left
			if (player.level() != level) continue;         // different dimension -> left
			if (player.isSpectator()) continue;
			if (!player.isAlive() || player.isDeadOrDying()) continue; // dead -> out
			if (player.distanceToSqr(cx, cy, cz) > leashSqr) continue;  // too far -> left
			active.add(player);
		}
		return active;
	}

	private LivingEntity nearestParticipant(ServerLevel level, RaidType type) {
		ServerPlayer nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : resolveActiveParticipants(level, type)) {
			double d = player.distanceToSqr(center.getX(), center.getY(), center.getZ());
			if (d < best) {
				best = d;
				nearest = player;
			}
		}
		return nearest;
	}

	// ------------------------------------------------------------------------------------------------
	// Boss bar
	// ------------------------------------------------------------------------------------------------

	private void ensureBossEvent(RaidType type) {
		if (bossEvent == null) {
			bossEvent = new ServerBossEvent(type.getDisplayName(),
					BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
			bossEvent.setProgress(1.0F);
		}
	}

	private void refreshBossPlayers(List<ServerPlayer> active) {
		if (bossEvent == null) return;
		Set<UUID> activeIds = new HashSet<>();
		for (ServerPlayer player : active) activeIds.add(player.getUUID());

		for (ServerPlayer shown : new ArrayList<>(bossEvent.getPlayers())) {
			if (!activeIds.contains(shown.getUUID())) bossEvent.removePlayer(shown);
		}
		for (ServerPlayer player : active) bossEvent.addPlayer(player);
	}

	private void updateBossBar(RaidType type, int aliveMobs) {
		if (bossEvent == null) return;

		int waveNumber = Math.max(1, currentWaveIndex + 1);
		boolean boss = currentWaveIndex >= 0 && type.wave(currentWaveIndex).isBossWave();

		bossEvent.setName(Component.translatable("raid.dragonminez.bossbar",
				type.getDisplayName(), waveNumber, type.waveCount()));
		bossEvent.setColor(boss ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);

		if (waveDelayTimer > 0) {
			bossEvent.setProgress(1.0F);
		} else if (totalMobsThisWave > 0) {
			bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F, (float) aliveMobs / totalMobsThisWave)));
		}
	}

	// ------------------------------------------------------------------------------------------------
	// Resolution
	// ------------------------------------------------------------------------------------------------

	private void win(ServerLevel level, List<ServerPlayer> winners) {
		status = Status.VICTORY;
		clearBossEvent();
		discardRemainingMobs(level);

		type().getReward().grant(level, winners, center);
		for (ServerPlayer player : winners) {
			player.sendSystemMessage(Component.translatable("raid.dragonminez.victory"));
		}
		LogUtil.info(Env.SERVER, "Raid {} completed (victory)", raidId);
	}

	private void fail(ServerLevel level) {
		status = Status.DEFEAT;
		clearBossEvent();
		discardRemainingMobs(level);
		LogUtil.info(Env.SERVER, "Raid {} ended (all participants died or left)", raidId);
	}

	/** Forcibly ends the raid (debug command / external cancel). */
	public void cancel(ServerLevel level) {
		if (status != Status.ACTIVE) return;
		status = Status.DEFEAT;
		clearBossEvent();
		discardRemainingMobs(level);
		LogUtil.info(Env.SERVER, "Raid {} cancelled", raidId);
	}

	private void discardRemainingMobs(ServerLevel level) {
		for (UUID id : currentWaveMobs) {
			Entity entity = level.getEntity(id);
			if (entity != null) entity.discard();
		}
		currentWaveMobs.clear();
	}

	private void clearBossEvent() {
		if (bossEvent != null) {
			bossEvent.removeAllPlayers();
			bossEvent.setVisible(false);
			bossEvent = null;
		}
	}

	// ------------------------------------------------------------------------------------------------
	// Persistence
	// ------------------------------------------------------------------------------------------------

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("RaidId", raidId);
		tag.putString("Type", typeId);
		tag.putString("Dimension", dimension.location().toString());
		tag.putLong("Center", center.asLong());
		tag.putString("Status", status.name());
		tag.putInt("WaveIndex", currentWaveIndex);
		tag.putInt("WaveDelay", waveDelayTimer);
		tag.putInt("TotalMobs", totalMobsThisWave);

		ListTag participantsTag = new ListTag();
		for (UUID id : participants) {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Id", id);
			participantsTag.add(entry);
		}
		tag.put("Participants", participantsTag);

		ListTag mobsTag = new ListTag();
		for (UUID id : currentWaveMobs) {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Id", id);
			mobsTag.add(entry);
		}
		tag.put("Mobs", mobsTag);
		return tag;
	}

	public static Raid load(CompoundTag tag) {
		UUID raidId = tag.getUUID("RaidId");
		String typeId = tag.getString("Type");
		ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
				new ResourceLocation(tag.getString("Dimension")));
		BlockPos center = BlockPos.of(tag.getLong("Center"));

		Set<UUID> participants = new HashSet<>();
		ListTag participantsTag = tag.getList("Participants", Tag.TAG_COMPOUND);
		for (int i = 0; i < participantsTag.size(); i++) {
			participants.add(participantsTag.getCompound(i).getUUID("Id"));
		}

		Raid raid = new Raid(raidId, typeId, dimension, center, participants);
		raid.status = Status.valueOf(tag.getString("Status"));
		raid.currentWaveIndex = tag.getInt("WaveIndex");
		raid.waveDelayTimer = tag.getInt("WaveDelay");
		raid.totalMobsThisWave = tag.getInt("TotalMobs");

		ListTag mobsTag = tag.getList("Mobs", Tag.TAG_COMPOUND);
		for (int i = 0; i < mobsTag.size(); i++) {
			raid.currentWaveMobs.add(mobsTag.getCompound(i).getUUID("Id"));
		}
		return raid;
	}
}
