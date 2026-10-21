package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.RaidDefinition;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RaidMusicS2C;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidTriggerEvents {

	public static final String PENDING_RAID_TAG = DBSagasEntity.RAID_SUPERVILLAIN_SOURCE_TAG;

	private static final double DEFAULT_SPAWN_CHANCE = 0.05D;

	@SubscribeEvent
	public static void onNaturalSpawn(MobSpawnEvent.FinalizeSpawn event) {
		if (event.getSpawnType() != MobSpawnType.NATURAL) return;
		if (!(event.getEntity() instanceof DBSagasEntity saga)) return;
		ServerLevel level = event.getLevel().getLevel();
		// Already marked (a re-fired finalize, or a mob restored from disk).
		if (saga.getPersistentData().contains(PENDING_RAID_TAG)) return;

		for (RaidType type : RaidTypes.triggerable()) {
			RaidDefinition.Trigger trigger = type.getTrigger();
			if (!matchesDimension(level, trigger)) continue;
			if (!RaidEntityResolver.matches(saga.getType(), trigger.getEntityId())) continue;
			if (level.getRandom().nextDouble() >= trigger.spawnChanceOr(DEFAULT_SPAWN_CHANCE)) continue;

			saga.getPersistentData().putString(PENDING_RAID_TAG, type.getId());
			saga.setSupervillain(trigger.isSupervillain());
			return;
		}
	}

	@SubscribeEvent
	public static void onTransformedMobJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) return;
		if (!(event.getEntity() instanceof DBSagasEntity saga)) return;

		CompoundTag data = saga.getPersistentData();
		if (!data.contains(DBSagasEntity.RAID_REPLACES_TAG)) return;

		UUID oldId = data.getUUID(DBSagasEntity.RAID_REPLACES_TAG);
		data.remove(DBSagasEntity.RAID_REPLACES_TAG);

		String rawRaidId = data.getString(DBSagasEntity.RAID_ID_TAG);
		if (rawRaidId.isBlank()) return;

		Raid raid = RaidSavedData.get(event.getLevel().getServer()).getRaid(UUID.fromString(rawRaidId));
		if (raid == null || raid.isFinished()) return;

		if (raid.replaceMob(oldId, saga)) {
			LogUtil.info(Env.SERVER, "Raid {} tracked a transform: {} -> {}", rawRaidId, oldId, saga.getUUID());
		}
	}

	private static boolean matchesDimension(ServerLevel level, RaidDefinition.Trigger trigger) {
		String dimension = trigger.getDimension();
		if (dimension == null || dimension.isBlank()) return true;

		ResourceLocation location = ResourceLocation.tryParse(dimension);
		if (location == null) return false;
		return level.dimension().equals(ResourceKey.create(Registries.DIMENSION, location));
	}

	@SubscribeEvent
	public static void onMarkedMobKilled(LivingDeathEvent event) {
		LivingEntity dead = event.getEntity();
		if (dead.level().isClientSide()) return;
		if (!dead.getPersistentData().contains(PENDING_RAID_TAG)) return;

		String raidId = dead.getPersistentData().getString(PENDING_RAID_TAG);
		RaidType type = RaidTypes.get(raidId);
		if (type == null) return;

		ServerPlayer killer = resolveCredit(dead, event, type);
		if (killer == null) return;

		// Already counting down for something; don't stack omens.
		if (killer.hasEffect(MainEffects.RAID_WRATH.get())) return;

		killer.getPersistentData().putString(PENDING_RAID_TAG, raidId);
		killer.addEffect(new MobEffectInstance(MainEffects.RAID_WRATH.get(),
				type.preparationTicks(), 0, false, true, true));
		NetworkHandler.sendToPlayer(new RaidMusicS2C(type.preparationMusic()), killer);

		RaidFeedback.announce(killer, type.getTrigger().getOmenMessage());

		LogUtil.info(Env.SERVER, "{} killed a raid trigger for '{}'; omen running for {} ticks",
				killer.getGameProfile().getName(), raidId, type.preparationTicks());
	}

	private static ServerPlayer resolveCredit(LivingEntity dead, LivingDeathEvent event, RaidType type) {
		if (event.getSource().getEntity() instanceof ServerPlayer direct) return direct;
		if (dead.getKillCredit() instanceof ServerPlayer credited) return credited;

		if (!(dead.level() instanceof ServerLevel level)) return null;
		Player nearest = level.getNearestPlayer(dead, type.getActivationRadius());
		return nearest instanceof ServerPlayer server && !server.isSpectator() ? server : null;
	}

	@SubscribeEvent
	public static void onOmenExpired(MobEffectEvent.Expired event) {
		if (event.getEffectInstance() == null) return;
		if (event.getEffectInstance().getEffect() != MainEffects.RAID_WRATH.get()) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		String raidId = player.getPersistentData().getString(PENDING_RAID_TAG);
		player.getPersistentData().remove(PENDING_RAID_TAG);
		if (raidId.isBlank()) return;

		RaidType type = RaidTypes.get(raidId);
		if (type == null) return;
		if (!(player.level() instanceof ServerLevel level)) return;

		Raid raid = RaidManager.startRaid(level, player.blockPosition(), player, raidId);
		if (raid == null) {
			LogUtil.warn(Env.SERVER, "Omen for '{}' expired but the raid could not start (one is already nearby?)", raidId);
			return;
		}

		String message = type.hasTrigger() ? type.getTrigger().getStartMessage() : null;
		for (UUID id : raid.getParticipants()) {
			ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
			if (participant != null) RaidFeedback.announce(participant, message);
		}
	}

	@SubscribeEvent
	public static void onOmenRemoved(MobEffectEvent.Remove event) {
		if (event.getEffect() != MainEffects.RAID_WRATH.get()) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		player.getPersistentData().remove(PENDING_RAID_TAG);
		NetworkHandler.sendToPlayer(new RaidMusicS2C(""), player);
	}
}
