package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StrikeAttackHandler {
	private static final int CONNECT_WINDOW_TICKS = 10;
	private static final double CONNECT_RANGE = 4.0;
	private static final double DASH_BASE_DISTANCE = 4.0;
	private static final double DASH_DISTANCE_SCALE = 0.3;
	private static final double KNOCKBACK_FORCE = 1.8;
	private static final double FINAL_HIT_RATIO = 0.35;
	private static final double IMPACT_DAMAGE_RATIO = 0.20;
	private static final int HIT_INTERVAL_TICKS = 10;
	private static final String STRIKE_HIT_ANIM = "base.flyback";
	private static final String STRIKE_KNOCKBACK_ANIM = "base.flyback";

	private static final Map<UUID, PendingStrike> PENDING = new HashMap<>();
	private static final Map<UUID, ActiveStrike> ACTIVE = new HashMap<>();

	public static void requestStrike(ServerPlayer player, int preferredTargetId) {
		if (player.level().isClientSide) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (!stats.getStatus().isHasCreatedCharacter()) return;
			if (stats.getStatus().isStunned() || stats.getStatus().isStrikeLocked() || stats.getStatus().isKnockedDown()) return;
			if (PENDING.containsKey(player.getUUID()) || ACTIVE.containsKey(player.getUUID())) return;

			TechniqueData selected = stats.getTechniques().getSelectedTechnique();
			if (!(selected instanceof StrikeAttackData strike)) return;

			if (stats.getSkills().getSkillLevel("kicontrol") <= 0 || stats.getResources().getPowerRelease() < 5 || !player.getMainHandItem().isEmpty()) return;

			String cooldownKey = getTechniqueCooldownKey(strike.getId());
			if (stats.getCooldowns().hasCooldown(cooldownKey)) return;

			double cost = strike.getCalculatedCost(stats);
			if (stats.getResources().getCurrentEnergy() < cost) return;

			stats.getResources().removeEnergy((int) Math.ceil(cost));
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

			LivingEntity preferredTarget = resolvePreferredTarget(player, preferredTargetId);
			PendingStrike pending = new PendingStrike(
					player.getUUID(),
					preferredTarget != null ? preferredTarget.getUUID() : null,
					strike.getId(),
					strike.getAnimationId(),
					strike.getDurationTicks(),
					strike.getActualCooldown(),
					cost,
					CONNECT_WINDOW_TICKS
			);
			PENDING.put(player.getUUID(), pending);

			dashForward(player);
		});
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
		if (!(event.player instanceof ServerPlayer player)) return;

		processPending(player);
		processActive(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID id = event.getEntity().getUUID();
		PENDING.remove(id);
		ACTIVE.remove(id);
	}

	private static void processPending(ServerPlayer player) {
		PendingStrike pending = PENDING.get(player.getUUID());
		if (pending == null) return;

		if (pending.ticksRemaining() <= 0) {
			failPending(player, pending);
			return;
		}

		LivingEntity target = resolveTargetForPending(player, pending);
		if (target != null) {
			PENDING.remove(player.getUUID());
			startStrike(player, target, pending);
			return;
		}

		PENDING.put(player.getUUID(), pending.withTicksRemaining(pending.ticksRemaining() - 1));
	}

    private static void processActive(ServerPlayer player) {
        ActiveStrike active = ACTIVE.get(player.getUUID());
        if (active == null) return;

        LivingEntity target = resolveLiving(player, active.targetId());

        if (target == null || !target.isAlive() || !player.isAlive()) {
            endStrike(player, target, active);
            return;
        }

        if ("dragon_fist".equals(active.techniqueId())) {
            if (active.ticksElapsed() == 0) {
                faceEntity(player, target);
            }

            if (active.ticksElapsed() == 5) {
                SPDragonFistEntity dragonFist = new SPDragonFistEntity(player.level(), player);
                dragonFist.setupDragonFist(player, (float) active.totalDamage(), 1.0f);
            }

            if (active.ticksElapsed() >= active.durationTicks()) {
                endStrike(player, target, active);
            } else {
                ACTIVE.put(player.getUUID(), active.withTicksElapsed(active.ticksElapsed() + 1));
            }
            return;
        }

        freezeEntity(player);
        freezeEntity(target);
        faceEntity(player, target);
        if (target instanceof ServerPlayer targetPlayer) {
            faceEntity(targetPlayer, player);
        }

        int nextTick = active.ticksElapsed() + 1;
        if (nextTick % active.hitIntervalTicks() == 0 && nextTick < active.durationTicks()) {
            applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId());
        }

        if (nextTick >= active.durationTicks()) {
            applyStrikeDamage(player, target, active.finalDamage(), active.techniqueId());
            grantKillXpIfNeeded(player, target, active.techniqueId());
            applyKnockback(player, target, active.totalDamage());
            endStrike(player, target, active);
            return;
        }

        ACTIVE.put(player.getUUID(), active.withTicksElapsed(nextTick));
    }

	private static void startStrike(ServerPlayer player, LivingEntity target, PendingStrike pending) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(pending.techniqueId());
			if (!(tech instanceof StrikeAttackData strike)) {
				failPending(player, pending);
				return;
			}

			double totalDamage = stats.getStrikeDamage() * strike.getDamageMultiplier() * Math.max(0.0,
					ConfigManager.getTechniqueConfig().getStrikeConfig(strike.getId()).getDamageMultiplier());
			int durationTicks = Math.max(20, pending.durationTicks());
			int hitCount = Math.max(1, (int) Math.ceil(durationTicks / (double) HIT_INTERVAL_TICKS));
			double perHitDamage = (totalDamage * (1.0 - FINAL_HIT_RATIO)) / hitCount;
			double finalDamage = totalDamage * FINAL_HIT_RATIO;

			ActiveStrike active = new ActiveStrike(
					player.getUUID(),
					target.getUUID(),
					pending.techniqueId(),
					pending.animationId(),
					durationTicks,
					pending.cooldownTicks(),
					totalDamage,
					perHitDamage,
					finalDamage,
					HIT_INTERVAL_TICKS,
					0
			);
			ACTIVE.put(player.getUUID(), active);

			applyStrikeDamage(player, target, perHitDamage, pending.techniqueId());
			//teleportToTargetFront(player, target);
			setStrikeLocked(player, true);
			setStrikeLocked(target, true);
			faceEntity(player, target);
			if (target instanceof ServerPlayer targetPlayer) {
				faceEntity(targetPlayer, player);
			}
			playStrikeAnimation(player, pending.animationId());
		});
	}

	private static void endStrike(ServerPlayer player, LivingEntity target, ActiveStrike active) {
		ACTIVE.remove(player.getUUID());
		setStrikeLocked(player, false);
		if (target != null) setStrikeLocked(target, false);
		stopStrikeAnimation(player);

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			String cooldownKey = getTechniqueCooldownKey(active.techniqueId());
			stats.getCooldowns().setCooldown(cooldownKey, active.cooldownTicks());
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}

	private static void failPending(ServerPlayer player, PendingStrike pending) {
		PENDING.remove(player.getUUID());
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			String cooldownKey = getTechniqueCooldownKey(pending.techniqueId());
			int halfCooldown = Math.max(1, pending.cooldownTicks() / 2);
			stats.getCooldowns().setCooldown(cooldownKey, halfCooldown);
			stats.getResources().addEnergy((int) Math.ceil(pending.energyCost() * 0.4));
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}

	private static LivingEntity resolvePreferredTarget(ServerPlayer player, int targetId) {
		if (targetId <= 0) return null;
		if (!(player.level().getEntity(targetId) instanceof LivingEntity living)) return null;
		if (!living.isAlive()) return null;
		if (player.distanceTo(living) > CONNECT_RANGE) return null;
		if (!player.hasLineOfSight(living)) return null;
		return living;
	}

	private static LivingEntity resolveTargetForPending(ServerPlayer player, PendingStrike pending) {
		LivingEntity preferred = pending.preferredTargetId() != null ? resolveLiving(player, pending.preferredTargetId()) : null;
		if (preferred != null && player.distanceTo(preferred) <= CONNECT_RANGE && player.hasLineOfSight(preferred)) return preferred;
		return findTargetInFront(player, CONNECT_RANGE).orElse(null);
	}

	private static LivingEntity resolveLiving(ServerPlayer player, UUID id) {
		if (id == null) return null;
		return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64.0))
				.stream()
				.filter(e -> e.getUUID().equals(id))
				.findFirst()
				.orElse(null);
	}

	private static Optional<LivingEntity> findTargetInFront(ServerPlayer player, double range) {
		Vec3 eyePos = player.getEyePosition();
		Vec3 viewVec = player.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(viewVec.scale(range));
		AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D);

		List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
				e -> e != player && e.isAlive() && e.isPickable());

		LivingEntity closest = null;
		double closestDist = range * range;

		for (LivingEntity e : list) {
			AABB axisalignedbb = e.getBoundingBox().inflate(e.getPickRadius());
			Optional<Vec3> hit = axisalignedbb.clip(eyePos, endPos);
			if (e.isInvisible() || e.isInvisibleTo(player) || !player.hasLineOfSight(e)) continue;

			if (axisalignedbb.contains(eyePos)) {
				if (closestDist >= 0.0D) {
					closest = e;
					closestDist = 0.0D;
				}
			} else if (hit.isPresent()) {
				double dist = eyePos.distanceToSqr(hit.get());
				if (dist < closestDist) {
					closest = e;
					closestDist = dist;
				}
			}
		}
		return Optional.ofNullable(closest);
	}

	private static void dashForward(ServerPlayer player) {
		double speedMultiplier = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1;
		double distance = DASH_BASE_DISTANCE * speedMultiplier;
		Vec3 direction = Vec3.directionFromRotation(0, player.getYRot()).normalize();
		Vec3 velocity = direction.scale(distance * DASH_DISTANCE_SCALE);
		double yVel = player.onGround() ? 0.35 : 0.2;
		player.setDeltaMovement(player.getDeltaMovement().add(velocity.x, yVel, velocity.z));
		player.hurtMarked = true;
	}

    private static void teleportToTargetFront(ServerPlayer player, LivingEntity target) {
        Vec3 targetPos = target.position();
        Vec3 targetLook = target.getLookAngle();
        Vec3 teleportPos = targetPos.subtract(targetLook.scale(1.3));

        player.teleportTo(teleportPos.x, targetPos.y, teleportPos.z);

        player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

    }

	private static void applyStrikeDamage(ServerPlayer player, LivingEntity target, double damage, String techniqueId) {
		if (damage <= 0) return;
		playStrikeHitAnimation(target);
		target.hurt(MainDamageTypes.strikeAttack(player.level(), player), (float) damage);

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techniqueId);
			if (tech instanceof StrikeAttackData strike) {
				int xpGain = strike.getXpGainPerHit();
				if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(techniqueId, xpGain);
			}
		});
	}

	private static void grantKillXpIfNeeded(ServerPlayer player, LivingEntity target, String techniqueId) {
		if (target == null || target.isAlive()) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techniqueId);
			if (tech instanceof StrikeAttackData strike) {
				int xpGain = strike.getXpGainPerKill();
				if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(techniqueId, xpGain);
			}
		});
	}

	private static void applyKnockback(ServerPlayer player, LivingEntity target, double totalDamage) {
		Vec3 dir = target.position().subtract(player.position()).normalize();
		if (dir.lengthSqr() < 1.0E-6) dir = player.getLookAngle();
		target.setDeltaMovement(dir.scale(KNOCKBACK_FORCE));
		target.hurtMarked = true;
		playStrikeKnockbackAnimation(target);

		MomentumImpactHandler.CollisionImpactType impactType = target.onGround() || dir.y < -0.5
				? MomentumImpactHandler.CollisionImpactType.GROUND
				: MomentumImpactHandler.CollisionImpactType.WALL;
		MomentumImpactHandler.registerCollisionImpact(target, impactType, (float) (totalDamage * IMPACT_DAMAGE_RATIO), dir);
	}

	private static void playStrikeAnimation(ServerPlayer player, String animationId) {
		if (animationId == null || animationId.isEmpty()) return;
		NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, animationId), player);
	}

	private static void stopStrikeAnimation(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
	}

	private static void freezeEntity(LivingEntity entity) {
		entity.setDeltaMovement(Vec3.ZERO);
		entity.hurtMarked = true;
	}

	private static void faceEntity(LivingEntity source, LivingEntity target) {
		if (source == null || target == null) return;
		source.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
		source.setYHeadRot(source.getYRot());
	}

	private static void playStrikeHitAnimation(LivingEntity target) {
		if (!(target instanceof ServerPlayer serverPlayer)) return;
		NetworkHandler.sendToTrackingEntityAndSelf(
				new TriggerAnimationS2C(serverPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, STRIKE_HIT_ANIM),
				serverPlayer
		);
	}

	private static void playStrikeKnockbackAnimation(LivingEntity target) {
		if (!(target instanceof ServerPlayer serverPlayer)) return;
		NetworkHandler.sendToTrackingEntityAndSelf(
				new TriggerAnimationS2C(serverPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, STRIKE_KNOCKBACK_ANIM),
				serverPlayer
		);
	}

	private static void setStrikeLocked(LivingEntity entity, boolean locked) {
		if (entity instanceof ServerPlayer serverPlayer) {
			StatsProvider.get(StatsCapability.INSTANCE, serverPlayer).ifPresent(stats -> {
				stats.getStatus().setStrikeLocked(locked);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
			});
		}
	}

	private static String getTechniqueCooldownKey(String techniqueId) {
		return "TechniqueCooldown_" + techniqueId;
	}

	private record PendingStrike(UUID playerId, UUID preferredTargetId, String techniqueId, String animationId,
									 int durationTicks, int cooldownTicks, double energyCost, int ticksRemaining) {
		private PendingStrike withTicksRemaining(int ticksRemaining) {
			return new PendingStrike(playerId, preferredTargetId, techniqueId, animationId, durationTicks, cooldownTicks, energyCost, ticksRemaining);
		}
	}

	private record ActiveStrike(UUID playerId, UUID targetId, String techniqueId, String animationId,
								   int durationTicks, int cooldownTicks, double totalDamage, double perHitDamage,
								   double finalDamage, int hitIntervalTicks, int ticksElapsed) {
		private ActiveStrike withTicksElapsed(int ticksElapsed) {
			return new ActiveStrike(playerId, targetId, techniqueId, animationId, durationTicks, cooldownTicks, totalDamage, perHitDamage, finalDamage, hitIntervalTicks, ticksElapsed);
		}
	}
}
