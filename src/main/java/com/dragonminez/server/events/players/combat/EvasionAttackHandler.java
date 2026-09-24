package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.util.MultipartTargeting;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AfterimageVfxS2C;
import com.dragonminez.common.network.S2C.RageScreamVfxS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TaiyokenBlindS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.racial.RacialStatUtil;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.EvasionAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EvasionAttackHandler {

	public static final String BLIND_UNTIL_TAG = "dmz_taiyoken_blind_until";

	private static final double TAIYOKEN_RANGE = 20.0;
	private static final double TAIYOKEN_FULL_LOOK_DOT = 0.93;
	private static final double TAIYOKEN_PARTIAL_LOOK_DOT = 0.5;
	private static final double TAIYOKEN_NPC_DURATION_FACTOR = 0.5;

	private static final int RAGE_SCREAM_PULSE_INTERVAL_TICKS = 5;
	private static final double RAGE_SCREAM_BASE_RANGE = 5.0;
	private static final float DEFAULT_HITBOX = 1.8f;
	private static final double RAGE_SCREAM_PULSE_KNOCKBACK = 0.35;
	private static final double RAGE_SCREAM_VERTICAL_LIFT = 0.12;
	private static final int SHARED_EVASION_COOLDOWN_TICKS = 60;

	private static final int SLEEP_RECOVERY_PULSE_INTERVAL_TICKS = 10;
	private static final float SLEEP_RECOVERY_TOTAL_HEAL = 0.20F;

	private static final double AFTERIMAGE_RANGE = 24.0;
	private static final double DIMENSIONAL_WARP_RANGE = 64.0;
	private static final double AFTERIMAGE_GAP = 0.9;
	private static final double AFTERIMAGE_DECOY_DISTANCE = 4.0;
	private static final int AFTERIMAGE_HOPS = 4;
	private static final int AFTERIMAGE_HOP_INTERVAL = 5;
	private static final double AFTERIMAGE_HOP_MIN_DISTANCE = 4.0;
	private static final double AFTERIMAGE_HOP_DISTANCE_SPREAD = 2.0;
	private static final int AFTERIMAGE_HOP_ATTEMPTS = 10;
	private static final String AFTERIMAGE_LOST_UNTIL_TAG = "dmz_afterimage_lost_until";
	private static final String AFTERIMAGE_LOST_BY_TAG = "dmz_afterimage_lost_by";
	private static final double AFTERIMAGE_CONFUSE_RADIUS = 16.0;
	private static final int AFTERIMAGE_LOCK_TICKS = 8;
	private static final double[] AFTERIMAGE_DISTANCE_SCALES = {1.0, 0.6, 1.5};
	private static final double[] AFTERIMAGE_HEIGHT_OFFSETS = {0.0, 0.1, 0.5, 1.0, -0.5};
	private static final double[] AFTERIMAGE_DECOY_HEIGHT_OFFSETS = {0.0, 0.1, 0.6, 1.1, 1.6, -0.5, -1.0};

	private static final Set<LivingEntity> BLINDED_MOBS = new HashSet<>();
	private static final Map<UUID, ActiveEvasion> ACTIVE = new ConcurrentHashMap<>();

	private static final class ActiveEvasion {
		final String techniqueId;
		final int totalTicks;
		int ticksRemaining;
		int ticksSincePulse;
		LivingEntity hopTarget;
		int hopsLeft;
		int hopDurationTicks;

		ActiveEvasion(String techniqueId, int totalTicks) {
			this.techniqueId = techniqueId;
			this.totalTicks = totalTicks;
			this.ticksRemaining = totalTicks;
			this.ticksSincePulse = 0;
		}
	}

	public static void cast(ServerPlayer player, String techniqueId) {
		cast(player, techniqueId, -1);
	}

	public static void cast(ServerPlayer player, String techniqueId, int targetId) {
		if (player.level().isClientSide || techniqueId == null || techniqueId.isEmpty()) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (!stats.getStatus().isHasCreatedCharacter()) return;
			if (stats.getStatus().isStunned()) return;
			if (stats.getStatus().isFused() && !stats.getStatus().isFusionLeader()) return;
			if (player.isSpectator()) return;
			if (!player.getMainHandItem().isEmpty()) return;
			if (ACTIVE.containsKey(player.getUUID())) return;

			TechniqueData unlocked = stats.getTechniques().getUnlockedTechniques().get(techniqueId);
			if (!(unlocked instanceof EvasionAttackData technique)) return;

			String cooldownKey = "TechniqueCooldown_" + techniqueId;
			if (stats.getCooldowns().hasCooldown(cooldownKey)) return;

			AfterimagePlan afterimage = null;
			if ("afterimage".equals(techniqueId) || "dimensional_teleport".equals(techniqueId)) {
				afterimage = "dimensional_teleport".equals(techniqueId)
						? planDimensionalWarp(player, targetId)
						: planAfterimage(player, targetId);
				if (afterimage == null) {
					player.displayClientMessage(Component.translatable("message.dragonminez.technique.afterimage.no_space").withStyle(ChatFormatting.RED), true);
					return;
				}
			}

			double cost = technique.getCalculatedCost(stats);
			if (!player.isCreative() && stats.getResources().getCurrentEnergy() < cost) {
				player.displayClientMessage(Component.translatable("message.dragonminez.technique.no_ki", (int) Math.ceil(cost)).withStyle(ChatFormatting.RED), true);
				return;
			}
			if (!player.isCreative() && cost > 0) stats.getResources().removeEnergy((int) Math.ceil(cost));
			stats.getCooldowns().setCooldown(cooldownKey, technique.getActualCooldown());

			int xpGain = RacialStatUtil.applyTechniqueXpBonus(stats, technique.getXpGainPerHit());
			if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(techniqueId, xpGain);

			int durationTicks = technique.getActualDurationTicks();
			int lockTicks = durationTicks;
			if (afterimage != null) {
				int hopTicks = "afterimage".equals(techniqueId) && afterimage.target() != null ? (AFTERIMAGE_HOPS - 1) * AFTERIMAGE_HOP_INTERVAL : 0;
				lockTicks = Math.min(durationTicks, hopTicks + AFTERIMAGE_LOCK_TICKS);
			}
			stats.getStatus().setEvasionLockTicks(lockTicks);
			applySharedEvasionCooldown(stats, lockTicks + SHARED_EVASION_COOLDOWN_TICKS);

			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			if (afterimage == null) {
				NetworkHandler.sendToTrackingEntityAndSelf(
						new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, technique.getAnimationId()), player);
			}

			ActiveEvasion active = new ActiveEvasion(techniqueId, lockTicks);
			ACTIVE.put(player.getUUID(), active);

			switch (techniqueId) {
				case "afterimage" -> castAfterimage(player, afterimage, durationTicks, active);
				case "dimensional_teleport" -> castDimensionalTeleport(player, afterimage, durationTicks, technique.getAnimationId());
				case "taiyoken" -> castTaiyoken(player);
				case "rage_scream" -> castRageScream(player, durationTicks);
				case "sleep_recovery" -> castSleepRecovery(player);
				default -> { }
			}
		});
	}

	private static void applySharedEvasionCooldown(StatsData stats, int ticks) {
		for (TechniqueData data : stats.getTechniques().getUnlockedTechniques().values()) {
			if (!(data instanceof EvasionAttackData)) continue;
			String key = "TechniqueCooldown_" + data.getId();
			if (stats.getCooldowns().getCooldown(key) < ticks) stats.getCooldowns().setCooldown(key, ticks);
		}
	}

	private record AfterimagePlan(LivingEntity target, Vec3 destination, float casterYaw) {}

	private static AfterimagePlan planDimensionalWarp(ServerPlayer player, int targetId) {
		Entity locked = targetId >= 0 ? TargetHelper.resolveHittable(TargetHelper.getEntityOrPart(player.level(), targetId)) : null;
		if (locked instanceof LivingEntity living && living != player && living.isAlive() && !living.isSpectator()
				&& living.level() == player.level()
				&& living.distanceToSqr(player) <= DIMENSIONAL_WARP_RANGE * DIMENSIONAL_WARP_RANGE) {
			AfterimagePlan behind = planAfterimageBehind(player, living);
			if (behind != null) return behind;
		}
		return planAfterimageDecoy(player);
	}

	private static AfterimagePlan planAfterimage(ServerPlayer player, int targetId) {
		Entity locked = targetId >= 0 ? TargetHelper.resolveHittable(TargetHelper.getEntityOrPart(player.level(), targetId)) : null;
		if (isValidAfterimageTarget(player, locked)) return planAfterimageBehind(player, (LivingEntity) locked);
		return planAfterimageDecoy(player);
	}

	private static AfterimagePlan planAfterimageBehind(ServerPlayer player, LivingEntity target) {
		Vec3 center = target.position();
		Vec3 forward = Vec3.directionFromRotation(0.0F, target.getYRot());
		double behind = target.getBbWidth() * 0.5 + player.getBbWidth() * 0.5 + AFTERIMAGE_GAP;

		for (double scale : AFTERIMAGE_DISTANCE_SCALES) {
			Vec3 base = center.subtract(forward.scale(behind * scale));
			for (double height : AFTERIMAGE_HEIGHT_OFFSETS) {
				Vec3 candidate = base.add(0.0, height, 0.0);
				if (isFreeSpot(player, candidate)) return new AfterimagePlan(target, candidate, yawTowards(candidate, center));
			}
		}
		return null;
	}

	private static Vec3 findHopAround(ServerPlayer player, LivingEntity target) {
		Vec3 center = target.position();
		double minDistance = target.getBbWidth() * 0.5 + AFTERIMAGE_HOP_MIN_DISTANCE;
		for (int attempt = 0; attempt < AFTERIMAGE_HOP_ATTEMPTS; attempt++) {
			double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
			double distance = minDistance + player.getRandom().nextDouble() * AFTERIMAGE_HOP_DISTANCE_SPREAD;
			Vec3 base = center.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
			for (double height : AFTERIMAGE_HEIGHT_OFFSETS) {
				Vec3 candidate = base.add(0.0, height, 0.0);
				if (isFreeSpot(player, candidate)) return candidate;
			}
		}
		return null;
	}

	private static AfterimagePlan planAfterimageDecoy(ServerPlayer player) {
		Vec3 look = player.getViewVector(1.0F);
		Vec3 direction = player.onGround() ? new Vec3(look.x, 0.0, look.z) : look;
		if (direction.lengthSqr() < 1.0E-4) direction = Vec3.directionFromRotation(0.0F, player.getYRot());
		direction = direction.normalize();

		Vec3 origin = player.position();
		Vec3 middle = origin.add(0.0, player.getBbHeight() * 0.5, 0.0);
		double reach = AFTERIMAGE_DECOY_DISTANCE;
		BlockHitResult wall = player.level().clip(new ClipContext(middle, middle.add(direction.scale(reach)),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (wall.getType() != HitResult.Type.MISS) reach = Math.min(reach, wall.getLocation().distanceTo(middle) - 0.5);

		for (double distance = reach; distance >= 1.0; distance -= 0.5) {
			Vec3 base = origin.add(direction.scale(distance));
			for (double height : AFTERIMAGE_DECOY_HEIGHT_OFFSETS) {
				Vec3 candidate = base.add(0.0, height, 0.0);
				if (isFreeSpot(player, candidate)) {
					return new AfterimagePlan(null, candidate, player.getYRot());
				}
			}
		}
		return null;
	}

	private static boolean isFreeSpot(ServerPlayer player, Vec3 position) {
		return player.level().noCollision(player, player.getBoundingBox().move(position.subtract(player.position())));
	}

	private static boolean isValidAfterimageTarget(ServerPlayer player, Entity entity) {
		if (!(entity instanceof LivingEntity living) || living == player) return false;
		if (!living.isAlive() || living.isSpectator() || living.level() != player.level()) return false;
		if (living == player.getVehicle() || living.getVehicle() == player) return false;
		return living.distanceToSqr(player) <= AFTERIMAGE_RANGE * AFTERIMAGE_RANGE;
	}

	private static float yawTowards(Vec3 from, Vec3 to) {
		return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * (180.0 / Math.PI)) - 90.0F;
	}

	private static void castDimensionalTeleport(ServerPlayer caster, AfterimagePlan plan, int durationTicks, String animationId) {
		if (plan == null) return;

		Vec3 from = caster.position();
		Vec3 destination = plan.destination();

		caster.level().playSound(null, from.x, from.y, from.z,
				MainSounds.ZANZOKEN.get(), SoundSource.PLAYERS, 1.0F, 0.85F);

		if (caster.isPassenger()) caster.stopRiding();
		caster.connection.teleport(destination.x, destination.y, destination.z, plan.casterYaw(), 0.0F);
		caster.setYHeadRot(plan.casterYaw());
		caster.setDeltaMovement(Vec3.ZERO);
		caster.hurtMarked = true;
		caster.fallDistance = 0.0F;

		if (caster.level() instanceof ServerLevel serverLevel) {
			NetworkHandler.sendToTrackingEntityAndSelf(new com.dragonminez.common.network.S2C.DimensionalShatterS2C(-1,
					from.x, from.y, from.z, caster.getBbWidth(), caster.getBbHeight(), false), caster);
			NetworkHandler.sendToTrackingEntityAndSelf(new com.dragonminez.common.network.S2C.DimensionalShatterS2C(caster.getId(),
					destination.x, destination.y, destination.z, caster.getBbWidth(), caster.getBbHeight(), true), caster);
		}

		NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(caster.getUUID(),
				TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, animationId), caster);
	}

	private static void castAfterimage(ServerPlayer caster, AfterimagePlan plan, int durationTicks, ActiveEvasion active) {
		if (plan == null) return;

		AABB confuseBox = caster.getBoundingBox().inflate(AFTERIMAGE_CONFUSE_RADIUS);
		for (Mob mob : caster.level().getEntitiesOfClass(Mob.class, confuseBox, m -> m.isAlive() && m.getTarget() == caster)) {
			loseTrackOf(mob, caster, durationTicks);
		}
		if (plan.target() instanceof Mob targetMob) loseTrackOf(targetMob, caster, durationTicks);

		LivingEntity target = plan.target();
		if (target == null || AFTERIMAGE_HOPS <= 1) {
			afterimageHop(caster, plan.destination(), plan.casterYaw(), target != null ? 0.0F : caster.getXRot(), durationTicks);
			return;
		}

		active.hopTarget = target;
		active.hopsLeft = AFTERIMAGE_HOPS - 1;
		active.hopDurationTicks = durationTicks;

		Vec3 hop = findHopAround(caster, target);
		if (hop == null) hop = plan.destination();
		afterimageHop(caster, hop, yawTowards(hop, target.position()), 0.0F, durationTicks);
	}

	private static void afterimageHopTick(ServerPlayer caster, ActiveEvasion active) {
		LivingEntity target = active.hopTarget;
		if (!isValidAfterimageTarget(caster, target)) {
			active.hopsLeft = 0;
			return;
		}
		if (++active.ticksSincePulse < AFTERIMAGE_HOP_INTERVAL) return;
		active.ticksSincePulse = 0;
		active.hopsLeft--;

		Vec3 destination = null;
		if (active.hopsLeft <= 0) {
			AfterimagePlan behind = planAfterimageBehind(caster, target);
			if (behind != null) destination = behind.destination();
		}
		if (destination == null) destination = findHopAround(caster, target);
		if (destination == null) return;

		afterimageHop(caster, destination, yawTowards(destination, target.position()), 0.0F, active.hopDurationTicks);
	}

	private static void afterimageHop(ServerPlayer caster, Vec3 destination, float yaw, float pitch, int durationTicks) {
		Vec3 from = caster.position();
		float fromYaw = caster.yBodyRot;

		caster.level().playSound(null, from.x, from.y, from.z,
				MainSounds.ZANZOKEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

		if (caster.isPassenger()) caster.stopRiding();
		caster.connection.teleport(destination.x, destination.y, destination.z, yaw, pitch);
		caster.setYHeadRot(yaw);
		caster.setDeltaMovement(Vec3.ZERO);
		caster.hurtMarked = true;
		caster.fallDistance = 0.0F;

		NetworkHandler.sendToTrackingEntityAndSelf(new AfterimageVfxS2C(caster.getId(), durationTicks, new Vec3[]{from}, new float[]{fromYaw}, false), caster);
		if (caster.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.CLOUD, destination.x, destination.y + 1.0, destination.z, 5, 0.2, 0.5, 0.2, 0.0);
		}
	}

	private static void loseTrackOf(Mob mob, ServerPlayer caster, int durationTicks) {
		mob.getPersistentData().putLong(AFTERIMAGE_LOST_UNTIL_TAG, mob.level().getGameTime() + durationTicks);
		mob.getPersistentData().putUUID(AFTERIMAGE_LOST_BY_TAG, caster.getUUID());
		mob.setTarget(null);
		if (mob.getLastHurtByMob() == caster) mob.setLastHurtByMob(null);
		mob.getNavigation().stop();
	}

	private static boolean hasLostTrackOf(LivingEntity entity, Entity other) {
		var data = entity.getPersistentData();
		if (data.getLong(AFTERIMAGE_LOST_UNTIL_TAG) <= entity.level().getGameTime()) return false;
		return data.hasUUID(AFTERIMAGE_LOST_BY_TAG) && data.getUUID(AFTERIMAGE_LOST_BY_TAG).equals(other.getUUID());
	}

	@SubscribeEvent
	public static void onChangeTarget(LivingChangeTargetEvent event) {
		LivingEntity newTarget = event.getNewTarget();
		if (newTarget == null || event.getEntity().level().isClientSide()) return;
		if (hasLostTrackOf(event.getEntity(), newTarget)) event.setCanceled(true);
	}

	private static void castSleepRecovery(ServerPlayer caster) {
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				SoundEvents.FOX_SLEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	private static void sleepRecoveryPulse(ServerPlayer caster, int totalTicks) {
		int pulses = Math.max(1, totalTicks / SLEEP_RECOVERY_PULSE_INTERVAL_TICKS);
		caster.heal(caster.getMaxHealth() * SLEEP_RECOVERY_TOTAL_HEAL / pulses);
		if (caster.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, caster.getX(), caster.getY() + caster.getBbHeight(), caster.getZ(),
					5, 0.5, 0.5, 0.5, 0.1);
		}
	}

	private static void castTaiyoken(ServerPlayer caster) {
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.PLAYERS, 1.2F, 1.6F);
		applyTaiyokenBlind(caster);
	}

	public static void applyTaiyokenBlind(LivingEntity caster) {
		boolean npcCaster = !(caster instanceof Player);
		Vec3 casterEye = caster.getEyePosition();
		AABB box = caster.getBoundingBox().inflate(TAIYOKEN_RANGE);

		for (LivingEntity victim : caster.level().getEntitiesOfClass(LivingEntity.class, box,
				e -> e != caster && e.isAlive() && e.isPickable())) {

			if (npcCaster) {
				if (victim instanceof Player victimPlayer) {
					if (victimPlayer.isCreative() || victimPlayer.isSpectator()) continue;
				} else if (!(victim instanceof Mob mob && mob.getTarget() == caster)) {
					continue;
				}
			}

			double distance = caster.distanceTo(victim);
			if (distance > TAIYOKEN_RANGE) continue;
			if (!victim.hasLineOfSight(caster)) continue;

			Vec3 victimLook = victim.getViewVector(1.0F).normalize();
			Vec3 toCaster = casterEye.subtract(victim.getEyePosition());
			if (toCaster.lengthSqr() < 1.0E-6) continue;
			double dot = victimLook.dot(toCaster.normalize());
			if (dot < TAIYOKEN_PARTIAL_LOOK_DOT) continue;

			boolean fullLook = dot >= TAIYOKEN_FULL_LOOK_DOT;
			double distanceFrac = Mth.clamp(distance / TAIYOKEN_RANGE, 0.0, 1.0);
			double seconds = fullLook ? 12.0 - 3.0 * distanceFrac : 9.0 - 3.0 * distanceFrac;

			if (caster instanceof Player playerCaster && TargetHelper.getRelation(playerCaster, victim) == TargetHelper.Relation.FRIENDLY) seconds *= 0.5;
			if (npcCaster) seconds *= TAIYOKEN_NPC_DURATION_FACTOR;

			int durationTicks = Math.max(1, (int) Math.round(seconds * 20.0));

			if (victim instanceof ServerPlayer victimPlayer) {
				NetworkHandler.sendToPlayer(new TaiyokenBlindS2C(durationTicks), victimPlayer);
			} else {
				blindMob(victim, durationTicks);
			}
		}
	}

	private static void blindMob(LivingEntity victim, int durationTicks) {
		long until = victim.level().getGameTime() + durationTicks;
		victim.getPersistentData().putLong(BLIND_UNTIL_TAG, until);
		if (victim instanceof Mob mob) mob.setTarget(null);
		BLINDED_MOBS.add(victim);
	}

	public static boolean isBlinded(LivingEntity entity) {
		if (entity == null) return false;
		return entity.getPersistentData().getLong(BLIND_UNTIL_TAG) > entity.level().getGameTime();
	}

	private static void castRageScream(ServerPlayer caster, int durationTicks) {
		caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
				MainSounds.OOZARU_GROWL_PLAYER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		NetworkHandler.sendToTrackingEntityAndSelf(new RageScreamVfxS2C(caster.getId(), durationTicks), caster);
	}

	private static void rageScreamPulse(ServerPlayer caster, EvasionAttackData technique) {
		StatsProvider.get(StatsCapability.INSTANCE, caster).ifPresent(stats -> {
			float hitbox = Math.max(caster.getBbWidth(), caster.getBbHeight());
			double range = RAGE_SCREAM_BASE_RANGE * (hitbox / DEFAULT_HITBOX);

			float pulseDamage = Math.max(1.0f, (float) technique.getActualHitDamage(stats));

			AABB box = caster.getBoundingBox().inflate(range);
			for (LivingEntity victim : MultipartTargeting.collectTargets(caster.level(), box)) {
				if (victim == caster || !victim.isAlive() || !victim.isPickable()) continue;
				if (victim instanceof Player && TargetHelper.getRelation(caster, victim) == TargetHelper.Relation.FRIENDLY) continue;

				double distance = caster.distanceTo(victim);
				if (distance > range) continue;

				if (pulseDamage > 0) {
					victim.invulnerableTime = 0;
					victim.hurt(MainDamageTypes.kiblast(caster.level(), caster, caster), pulseDamage);
				}

				Vec3 dir = victim.position().subtract(caster.position());
				if (dir.lengthSqr() < 1.0E-6) dir = new Vec3(1, 0, 0);
				dir = dir.normalize();
				double falloff = 1.0 - Mth.clamp(distance / range, 0.0, 1.0) * 0.5;
				Vec3 velocity = new Vec3(dir.x, 0, dir.z).scale(RAGE_SCREAM_PULSE_KNOCKBACK * falloff).add(0, RAGE_SCREAM_VERTICAL_LIFT, 0);
				KnockbackHelper.apply(victim, velocity);
			}
		});
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
		if (BLINDED_MOBS.isEmpty()) return;

		Iterator<LivingEntity> it = BLINDED_MOBS.iterator();
		while (it.hasNext()) {
			LivingEntity entity = it.next();
			if (entity == null || !entity.isAlive() || entity.isRemoved() || !isBlinded(entity)) {
				it.remove();
				continue;
			}
			if (entity instanceof Mob mob && mob.getTarget() != null) mob.setTarget(null);
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return;

		Iterator<Map.Entry<UUID, ActiveEvasion>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ActiveEvasion> entry = it.next();
			ActiveEvasion active = entry.getValue();
			ServerPlayer caster = server.getPlayerList().getPlayer(entry.getKey());

			if (caster == null || !caster.isAlive()) {
				it.remove();
				continue;
			}

			if ("sleep_recovery".equals(active.techniqueId)) {
				active.ticksSincePulse++;
				if (active.ticksSincePulse >= SLEEP_RECOVERY_PULSE_INTERVAL_TICKS) {
					active.ticksSincePulse = 0;
					sleepRecoveryPulse(caster, active.totalTicks);
				}
			}

			if ("afterimage".equals(active.techniqueId) && active.hopsLeft > 0) {
				afterimageHopTick(caster, active);
			}

			if ("rage_scream".equals(active.techniqueId)) {
				active.ticksSincePulse++;
				if (active.ticksSincePulse >= RAGE_SCREAM_PULSE_INTERVAL_TICKS) {
					active.ticksSincePulse = 0;
					StatsProvider.get(StatsCapability.INSTANCE, caster).ifPresent(stats -> {
						TechniqueData unlocked = stats.getTechniques().getUnlockedTechniques().get("rage_scream");
						if (unlocked instanceof EvasionAttackData technique) {
							rageScreamPulse(caster, technique);
						}
					});
				}
			}

			active.ticksRemaining--;
			if (active.ticksRemaining <= 0) {
				StatsProvider.get(StatsCapability.INSTANCE, caster).ifPresent(stats -> {
					stats.getStatus().setEvasionLockTicks(0);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(caster), caster);
				});
				NetworkHandler.sendToTrackingEntityAndSelf(
						new TriggerAnimationS2C(caster.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0), caster);
				it.remove();
			}
		}
	}

	@SubscribeEvent
	public static void onLivingAttack(LivingAttackEvent event) {
		if (event.getEntity().level().isClientSide()) return;
		if (event.getSource().getEntity() == null) return;
		ActiveEvasion activeEvasion = ACTIVE.get(event.getEntity().getUUID());
		if (activeEvasion != null && !"sleep_recovery".equals(activeEvasion.techniqueId)) {
			event.setCanceled(true);
		} else if (hasLostTrackOf(event.getEntity(), event.getSource().getEntity())) {
			event.getEntity().getPersistentData().remove(AFTERIMAGE_LOST_UNTIL_TAG);
		}
	}

	@SubscribeEvent
	public static void onLivingHurt(LivingHurtEvent event) {
		if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		ActiveEvasion active = ACTIVE.get(player.getUUID());
		if (active == null || !"sleep_recovery".equals(active.techniqueId)) return;

		ACTIVE.remove(player.getUUID());
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			stats.getStatus().setEvasionLockTicks(0);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
		NetworkHandler.sendToTrackingEntityAndSelf(
				new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0), player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		BLINDED_MOBS.remove(player);
		if (ACTIVE.remove(player.getUUID()) != null) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> stats.getStatus().setEvasionLockTicks(0));
		}
	}
}
