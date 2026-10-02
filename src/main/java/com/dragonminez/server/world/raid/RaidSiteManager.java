package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.RaidDefaults;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.SagaSaiyanInvadersEntity;
import com.dragonminez.server.world.dimension.DemonRealmDimension;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.dragonminez.server.world.structure.helper.DMZStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class RaidSiteManager {

	public static final String SITE_TAG = "dmz_raid_site";
	public static final int SCOUT_OFFSET_X = 3;
	public static final int SCOUT_OFFSET_Z = 1;

	private static final int SCAN_INTERVAL = 40;
	private static final int SCAN_RADIUS_CHUNKS = 6;
	private static final double ANNOUNCE_RADIUS = 80.0D;
	private static final double ANNOUNCE_HEIGHT = 48.0D;
	private static final int GROUND_SCAN_ABOVE = 4;
	private static final int GROUND_SCAN_BELOW = 16;
	private static final int MISSING_CHECKS_BEFORE_LOST = 3;
	private static final double CREW_SEARCH_MARGIN = 16.0D;
	private static final int SCOUT_LEASH = 10;
	private static final Map<Long, Integer> MISSING_CHECKS = new HashMap<>();

	public enum Kind {
		SAIYAN_CRATER(Level.OVERWORLD, RaidDefaults.SAIYAN_ASSAULT, List.of(DMZStructures.SAIYAN_CRATER), true,
				"raid.dragonminez.saiyan_assault.crater", 500.0D, 12.0D, 10.0D, () -> MainEntities.SAGA_SAIYAN_SOLDIER.get()),
		NAMEK_RUINS(NamekDimension.NAMEK_KEY, RaidDefaults.FRIEZA_INVASION, List.of(DMZStructures.NAMEK_RUINS), false,
				"raid.dragonminez.frieza_invasion.ruins", 400.0D, 18.0D, 12.0D, () -> MainEntities.SAGA_FRIEZA_SOLDIER.get()),
		GOMAH_CAMP(DemonRealmDimension.DEMON_REALM_KEY, RaidDefaults.GOMAH_ASSAULT, List.of(DMZStructures.GOMAH_CAMP), false,
				"raid.dragonminez.gomah_assault.camp", 10000.0D, 900.0D, 800.0D, () -> MainEntities.SAGA_GOMAH_SOLDIER_1.get());

		private final ResourceKey<Level> dimension;
		private final String raidId;
		private final List<ResourceKey<Structure>> structures;
		private final boolean withPod;
		private final String defaultAnnounce;
		private final double defaultHealth;
		private final double defaultMelee;
		private final double defaultKi;
		private final Supplier<EntityType<?>> defaultScout;

		Kind(ResourceKey<Level> dimension, String raidId, List<ResourceKey<Structure>> structures, boolean withPod,
			 String defaultAnnounce, double defaultHealth, double defaultMelee, double defaultKi, Supplier<EntityType<?>> defaultScout) {
			this.dimension = dimension;
			this.raidId = raidId;
			this.structures = structures;
			this.withPod = withPod;
			this.defaultAnnounce = defaultAnnounce;
			this.defaultHealth = defaultHealth;
			this.defaultMelee = defaultMelee;
			this.defaultKi = defaultKi;
			this.defaultScout = defaultScout;
		}

		public List<ResourceKey<Structure>> structures() {
			return this.structures;
		}

		private static Kind forDimension(ResourceKey<Level> dimension) {
			for (Kind kind : values()) {
				if (kind.dimension.equals(dimension)) return kind;
			}
			return null;
		}
	}

	private RaidSiteManager() {}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
		if (!(event.level instanceof ServerLevel level)) return;
		if (level.getGameTime() % SCAN_INTERVAL != 0L) return;

		Kind kind = Kind.forDimension(level.dimension());
		if (kind != null) scan(level, kind);
	}

	@SubscribeEvent
	public static void onScoutDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (!(entity.level() instanceof ServerLevel level)) return;
		CompoundTag data = entity.getPersistentData();
		if (!data.contains(SITE_TAG)) return;
		Data.get(level).consume(data.getLong(SITE_TAG));
	}

	private static void scan(ServerLevel level, Kind kind) {
		if (level.getDifficulty() == Difficulty.PEACEFUL || level.players().isEmpty()) return;

		Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
		List<Structure> structures = new ArrayList<>();
		for (ResourceKey<Structure> key : kind.structures) {
			Structure structure = registry.get(key);
			if (structure != null) structures.add(structure);
		}
		if (structures.isEmpty()) return;

		Data data = Data.get(level);
		Set<Long> visited = new HashSet<>();

		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			ChunkPos origin = player.chunkPosition();

			for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
				for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
					ChunkPos chunkPos = new ChunkPos(origin.x + dx, origin.z + dz);
					long key = chunkPos.toLong();
					if (!visited.add(key)) continue;

					LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
					if (chunk == null) continue;

					for (Structure structure : structures) {
						StructureStart start = chunk.getStartForStructure(structure);
						if (start == null || !start.isValid()) continue;
						visitSite(level, kind, data, key, start.getPieces().get(0).getBoundingBox());
						break;
					}
				}
			}
		}
	}

	private static void visitSite(ServerLevel level, Kind kind, Data data, long key, BoundingBox box) {
		Data.Entry entry = data.entry(key);
		if (entry.consumed) return;

		BlockPos center = box.getCenter();
		List<ServerPlayer> nearby = playersNear(level, center);
		if (nearby.isEmpty()) return;

		if (entry.scout == null) {
			if (!level.areEntitiesLoaded(ChunkPos.asLong(center))) return;
			AABB area = AABB.of(box).inflate(CREW_SEARCH_MARGIN);
			Mob existing = findScout(level, area, key);
			if (existing != null) entry.scout = existing.getUUID();
			else if (!spawnMissingCrew(level, kind, entry, key, box, area)) return;
			data.setDirty();
		} else if (scoutLost(level, entry, key, center)) {
			entry.consumed = true;
			entry.announced.clear();
			data.setDirty();
			return;
		}

		BlockPos floor = groundIn(level, box, center.getX(), center.getZ());
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, floor.getX() + 0.5D, floor.getY() + 1.0D, floor.getZ() + 0.5D,
				3, 0.6D, 0.2D, 0.6D, 0.01D);

		String message = announceMessage(kind);
		for (ServerPlayer player : nearby) {
			if (entry.announced.add(player.getUUID())) {
				RaidFeedback.announce(player, message);
				data.setDirty();
			}
		}
	}

	private static String announceMessage(Kind kind) {
		RaidType type = RaidTypes.get(kind.raidId);
		if (type != null && type.hasTrigger() && type.getTrigger().getAnnounceMessage() != null) {
			return type.getTrigger().getAnnounceMessage();
		}
		return kind.defaultAnnounce;
	}

	private static List<ServerPlayer> playersNear(ServerLevel level, BlockPos center) {
		List<ServerPlayer> nearby = new ArrayList<>();
		double radiusSqr = ANNOUNCE_RADIUS * ANNOUNCE_RADIUS;
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - (center.getX() + 0.5D);
			double dz = player.getZ() - (center.getZ() + 0.5D);
			if (Math.abs(player.getY() - center.getY()) > ANNOUNCE_HEIGHT) continue;
			if (dx * dx + dz * dz <= radiusSqr) nearby.add(player);
		}
		return nearby;
	}

	private static boolean scoutLost(ServerLevel level, Data.Entry entry, long key, BlockPos center) {
		Entity existing = level.getEntity(entry.scout);
		if (existing != null && existing.isAlive()) {
			MISSING_CHECKS.remove(key);
			return false;
		}

		if (!level.areEntitiesLoaded(ChunkPos.asLong(center))) {
			MISSING_CHECKS.remove(key);
			return false;
		}

		int missing = MISSING_CHECKS.merge(key, 1, Integer::sum);
		if (missing < MISSING_CHECKS_BEFORE_LOST) return false;

		MISSING_CHECKS.remove(key);
		LogUtil.info(Env.SERVER, "Raid site scout {} vanished without dying; the site at {} is spent", entry.scout, center);
		return true;
	}

	private static Mob findScout(ServerLevel level, AABB area, long key) {
		for (Mob mob : level.getEntitiesOfClass(Mob.class, area, Mob::isAlive)) {
			CompoundTag data = mob.getPersistentData();
			if (data.contains(SITE_TAG) && data.getLong(SITE_TAG) == key) return mob;
		}
		return null;
	}

	private static boolean spawnMissingCrew(ServerLevel level, Kind kind, Data.Entry entry, long key, BoundingBox box, AABB area) {
		int centerX = box.getCenter().getX();
		int centerZ = box.getCenter().getZ();
		BlockPos floor = groundIn(level, box, centerX, centerZ);
		if (kind.withPod && level.getEntitiesOfClass(SpacePodEntity.class, area).isEmpty()) {
			SpacePodEntity pod = createPod(level, floor);
			if (pod != null) level.addFreshEntity(pod);
		}

		Mob scout = createScout(level, kind, key, groundIn(level, box, centerX + SCOUT_OFFSET_X, centerZ + SCOUT_OFFSET_Z), floor);
		if (scout == null || !level.addFreshEntity(scout)) return false;

		entry.scout = scout.getUUID();
		LogUtil.info(Env.SERVER, "Raid site at {} had no scout; placed one", floor);
		return true;
	}

	public static SpacePodEntity createPod(ServerLevelAccessor level, BlockPos floor) {
		SpacePodEntity pod = MainEntities.SPACE_POD.get().create(level.getLevel());
		if (pod == null) return null;
		pod.moveTo(floor.getX() + 0.5D, floor.getY(), floor.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
		pod.setOpenNave(true);
		return pod;
	}

	public static Mob createScout(ServerLevelAccessor level, Kind kind, long siteKey, BlockPos stand, BlockPos home) {
		RaidType type = RaidTypes.get(kind.raidId);
		RaidDefinition.Trigger trigger = type != null && type.hasTrigger() ? type.getTrigger() : null;

		EntityType<?> scoutType = trigger != null ? RaidEntityResolver.resolve(trigger.getEntityId(), level.getRandom()) : null;
		if (scoutType == null) scoutType = kind.defaultScout.get();

		Entity created = scoutType.create(level.getLevel());
		if (!(created instanceof Mob scout)) {
			if (created != null) created.discard();
			return null;
		}

		scout.moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
		scout.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), MobSpawnType.STRUCTURE, null, null);
		scout.setPersistenceRequired();
		if (scout instanceof PathfinderMob pathfinder) pathfinder.restrictTo(home, SCOUT_LEASH);

		CompoundTag data = scout.getPersistentData();
		data.putString(RaidTriggerEvents.PENDING_RAID_TAG, kind.raidId);
		data.putLong(SITE_TAG, siteKey);
		data.putBoolean("dmz_stats_configured", true);

		if (scout instanceof DBSagasEntity saga) {
			saga.setSupervillain(trigger == null || trigger.isSupervillain());
			saga.setAiTierById(trigger != null && trigger.getAiTier() != null ? trigger.getAiTier() : 1);
		}
		if (scout instanceof SagaSaiyanInvadersEntity.Soldier soldier) soldier.setHome(home);

		applyStats(scout,
				trigger != null && trigger.getHealth() != null ? trigger.getHealth() : kind.defaultHealth,
				trigger != null && trigger.getMeleeDamage() != null ? trigger.getMeleeDamage() : kind.defaultMelee,
				trigger != null && trigger.getKiDamage() != null ? trigger.getKiDamage() : kind.defaultKi);
		return scout;
	}

	private static void applyStats(Mob scout, double health, double melee, double ki) {
		double maxHealthValue = Math.max(1.0D, health);
		AttributeInstance maxHealth = scout.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null) maxHealth.setBaseValue(maxHealthValue);

		AttributeInstance attack = scout.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null) attack.setBaseValue(Math.max(0.0D, melee));

		AttributeInstance kiDamage = scout.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get());
		if (kiDamage != null) kiDamage.setBaseValue(Math.max(0.0D, ki));

		scout.setHealth(scout.getMaxHealth());
		if (scout instanceof DBSagasEntity saga) saga.setBattlePower((int) Math.round(maxHealthValue));
	}

	private static BlockPos groundIn(ServerLevel level, BoundingBox box, int x, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, box.maxY() + GROUND_SCAN_ABOVE, z);
		int bottom = box.minY() - GROUND_SCAN_BELOW;
		while (pos.getY() > bottom) {
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return pos.above().immutable();
			pos.move(Direction.DOWN);
		}
		return new BlockPos(x, box.minY(), z);
	}

	static final class Data extends SavedData {
		private static final String FILE_NAME = "dragonminez_raid_sites";

		private final Map<Long, Entry> entries = new HashMap<>();

		static Data get(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(Data::load, Data::new, FILE_NAME);
		}

		Entry entry(long key) {
			return this.entries.computeIfAbsent(key, k -> new Entry());
		}

		void consume(long key) {
			Entry entry = this.entry(key);
			if (entry.consumed) return;
			entry.consumed = true;
			entry.announced.clear();
			this.setDirty();
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			ListTag list = tag.getList("Sites", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag siteTag = list.getCompound(i);
				Entry entry = data.entry(siteTag.getLong("Key"));
				entry.consumed = siteTag.getBoolean("Consumed");
				if (siteTag.hasUUID("Scout")) entry.scout = siteTag.getUUID("Scout");
				ListTag announced = siteTag.getList("Announced", Tag.TAG_INT_ARRAY);
				for (Tag uuid : announced) entry.announced.add(NbtUtils.loadUUID(uuid));
			}
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			for (Map.Entry<Long, Entry> mapEntry : this.entries.entrySet()) {
				Entry entry = mapEntry.getValue();
				CompoundTag siteTag = new CompoundTag();
				siteTag.putLong("Key", mapEntry.getKey());
				siteTag.putBoolean("Consumed", entry.consumed);
				if (entry.scout != null) siteTag.putUUID("Scout", entry.scout);
				if (!entry.consumed) {
					ListTag announced = new ListTag();
					for (UUID uuid : entry.announced) announced.add(NbtUtils.createUUID(uuid));
					siteTag.put("Announced", announced);
				}
				list.add(siteTag);
			}
			tag.put("Sites", list);
			return tag;
		}

		static final class Entry {
			private boolean consumed;
			private UUID scout;
			private final Set<UUID> announced = new HashSet<>();
		}
	}
}
