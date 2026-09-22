package com.dragonminez.server.events.players.combat;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.S2C.BossTelegraphS2C;
import com.dragonminez.common.network.S2C.DimensionalFistS2C;
import net.minecraft.sounds.SoundSource;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPBlueHurricaneEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ClawSlashVfxS2C;
import com.dragonminez.common.network.S2C.ImpactBurstVfxS2C;
import com.dragonminez.common.network.S2C.KiBurstVfxS2C;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.racial.RacialStatUtil;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.entity.PartEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StrikeAttackHandler {
	private static final int CONNECT_WINDOW_TICKS = 10;
	private static final double CONNECT_RANGE = 4.0;
	private static final double CONE_RANGE = 6.0;
	private static final double CONE_RANGE_FLY = 12.0;
	private static final double CONE_HALF_ANGLE_COS = 0.5;
	private static final double DASH_BASE_DISTANCE = 4.0;
	private static final double DASH_DISTANCE_SCALE = 0.3;
	private static final double KNOCKBACK_FORCE = 1.8;
	private static final double FINAL_HIT_RATIO = 0.35;
	private static final double IMPACT_DAMAGE_RATIO = 0.20;
	private static final int HIT_INTERVAL_TICKS = 10;
	private static final long RECENT_HIT_WINDOW_MS = 10_000L;
	private static final String STRIKE_HIT_ANIM = "base.flyback";
	private static final String STRIKE_KNOCKBACK_ANIM = "base.flyback";
	private static final String GOD_FIST_HURT_ANIM = "base.hurt_supergodfist";
	private static final String HURT_LEFT_ANIM = "base.hurt_left";
	private static final String HURT_RIGHT_ANIM = "base.hurt_right";
	private static final java.util.Set<String> FLINCH_STRIKES = java.util.Set.of(
			"meteor", "wolf_fang", "kaioken_attack", "deadly_dance", "deadly_dance_vegetto",
			"shining_sword_attack");
	private static final String HURT_TOP_ANIM = "base.hurt_top";
	private static final String HURT_TOP2_ANIM = "base.hurt_top2";
	private static final String HURT_DOWN_ANIM = "base.hurt_down";
	private static final String SPIRIT_CANNON_ID = "spirit_breaking_cannon";
	private static final java.util.Set<String> POSE_DRIVEN_STRIKES = java.util.Set.of(SPIRIT_CANNON_ID);
	private static final int SPIRIT_CANNON_KNEE_TICK = 6;
	private static final int SPIRIT_CANNON_UPPERCUT_TICK = 13;
	private static final int SPIRIT_CANNON_LAUNCH_TICK = 24;
	private static final int SPIRIT_CANNON_ELBOW_TICK = 40;
	private static final int SPIRIT_CANNON_END_TICK = 47;
	private static final int SPIRIT_CANNON_LIFT_TICKS = 12;
	private static final double SPIRIT_CANNON_LIFT_SPEED = 0.45;
	private static final double SPIRIT_CANNON_SLAM_POWER = 2.8;
	private static final int SPIRIT_CANNON_COLOR_PRIMARY = 0xF3E4FF;
	private static final int SPIRIT_CANNON_COLOR_SECONDARY = 0xA63EF0;
	private static final int SPIRIT_CANNON_BURST_TICKS = 14;
	private static final float SPIRIT_CANNON_HIT_BURST_SCALE = 3.2F;
	private static final float SPIRIT_CANNON_LAUNCH_BURST_SCALE = 4.0F;
	private static final float SPIRIT_CANNON_ELBOW_BURST_SCALE = 5.0F;
	private static final float SPIRIT_CANNON_GROUND_BURST_SCALE = 6.0F;
	private static final String OOZARU_SLAM_ID = "oozaru_slam";
	private static final String DIM_PUNCH_ID = "dimensional_punch";
	private static final int DIM_PUNCH_DURATION = 70;
	private static final int[] DIM_PUNCH_MARK_TICKS = {3, 18, 33};
	private static final int[] DIM_PUNCH_HIT_TICKS = {15, 30, 45};
	private static final double DIM_PUNCH_RADIUS = 3.0;
	private static final double DIM_PUNCH_RANGE = 26.0;
	private static final int DIM_PUNCH_COLOR = 0xC451FF;
	private static final String DIM_SLASH_ID = "dimensional_sword_attack";
	private static final int DIM_SLASH_DURATION = 41;
	private static final int[] DIM_SLASH_TICKS = {8, 20, 32};
	private static final double DIM_SLASH_RANGE = 40.0;
	private static final double DIM_SLASH_SPEED = 1.2;
	private static final double DIM_SLASH_HIT_RADIUS = 2.2;
	private static final float DIM_SLASH_SCALE = 2.8F;
	private static final int DIM_SLASH_COLOR = 0xFF1E2D;
	private static final int SLAM_IMPACT_TICK = 2;
	private static final int SLAM_DURATION_TICKS = 15;
	private static final float SLAM_SHAKE_RADIUS = 6.0F;
	private static final int SLAM_DUST_POINTS_PER_BLOCK = 10;
	private static final int SLAM_DUST_MAX_POINTS = 56;
	private static final double SLAM_RING_MAX_RADIUS = 5.0;
	private static final int SLAM_RING_TICKS = 10;
	private static final double SLAM_RING_DAMAGE_RATIO = 0.5;
	private static final double SLAM_RING_HEIGHT = 1.5;
	private static final double SLAM_RING_KNOCKBACK = 0.6;
	private static final Map<UUID, SlamRing> SLAM_RINGS = new HashMap<>();
	private static final int SLAM_DEBRIS_PER_BLOCK = 4;
	private static final double SLAM_DEBRIS_SPREAD = 0.75;
	private static final float SLAM_UNBLOCK_PITCH = 0.55F;
	private static final float SLAM_PARRY_PITCH = 0.85F;
	private static final double FRONT_REACH = 1.3;
	private static final float SHOCKWAVE_HIT_SCALE = 1.8F;
	private static final float SHOCKWAVE_FINAL_SCALE = 4.5F;
	private static final int SHOCKWAVE_HIT_TICKS = 7;
	private static final int SHOCKWAVE_FINAL_TICKS = 13;
	private static final int WOLF_FANG_CLAW_COLOR = 0x2F5BFF;
	private static final float WOLF_FANG_JAB_CLAW_SCALE = 1.7F;
	private static final int WOLF_FANG_JAB_CLAW_TICKS = 8;
	private static final float WOLF_FANG_FINAL_CLAW_SCALE = 3.4F;
	private static final int WOLF_FANG_FINAL_CLAW_TICKS = 16;
	private static final int WOLF_FANG_FINAL_CLAWS = 6;
	private static final int GOD_FIST_COLOR_PRIMARY = 0xFFD23A;
	private static final int GOD_FIST_COLOR_SECONDARY = 0xFF4A12;
	private static final float GOD_FIST_BURST_SCALE = 5.5F;
	private static final int GOD_FIST_BURST_TICKS = 18;
	private static final int GUM_PUNCH_IMPACT_TICK = 4;
	private static final int GUM_PUNCH_END_TICK = 12;
	private static final double GUM_PUNCH_KNOCKBACK = 5.0;
	private static final int GUM_PUNCH_COLOR_PRIMARY = 0xFF82F3;
	private static final int GUM_PUNCH_COLOR_SECONDARY = 0xFF1AEC;
	private static final float GUM_PUNCH_BURST_SCALE = 4.0F;
	private static final int GUM_PUNCH_BURST_TICKS = 14;
	private static final int DEADLY_DANCE_COLOR_PRIMARY = 0xFFE23A;
	private static final int DEADLY_DANCE_COLOR_SECONDARY = 0xFFA800;
	private static final int DEADLY_DANCE_VEGETTO_COLOR_SECONDARY = 0x2FA8FF;
	private static final float DEADLY_DANCE_HIT_BURST_SCALE = 1.7F;
	private static final int DEADLY_DANCE_HIT_BURST_TICKS = 7;
	private static final float DEADLY_DANCE_FINAL_BURST_SCALE = 4.8F;
	private static final int DEADLY_DANCE_FINAL_BURST_TICKS = 16;
	private static final int BLUE_HURRICANE_CAST_TICKS = 15;
	private static final java.util.Set<String> TARGETLESS_STRIKES = java.util.Set.of(
			"dragon_fist", "deadly_dance", "deadly_dance_vegetto", "super_god_fist", "wolf_fang", "gum_punch");

	private static final Map<UUID, PendingStrike> PENDING = new HashMap<>();
	private static final Map<UUID, ActiveStrike> ACTIVE = new HashMap<>();
	private static final Map<UUID, RecentHit> RECENTLY_DAMAGED = new HashMap<>();
	private static final Map<UUID, Integer> STRIKE_ANCHOR_PART = new HashMap<>();
	private static final Map<UUID, HurtPose> STRIKE_HURT_VICTIM = new HashMap<>();
	private static final Map<UUID, Boolean> FLINCH_SIDE = new HashMap<>();

	public static void requestStrike(ServerPlayer player, int preferredTargetId) {
		if (player.level().isClientSide) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (!stats.getStatus().isHasCreatedCharacter()) return;
			if (stats.getStatus().isStunned()) return;
			if (PENDING.containsKey(player.getUUID()) || ACTIVE.containsKey(player.getUUID())) return;

			TechniqueData selected = stats.getTechniques().getSelectedTechnique();
			if (!(selected instanceof StrikeAttackData strike)) return;

			if (stats.getSkills().getSkillLevel("kicontrol") <= 0 || stats.getResources().getPowerRelease() < 5 || (!player.getMainHandItem().isEmpty() && !DIM_SLASH_ID.equals(strike.getId()))) return;

			String cooldownKey = getTechniqueCooldownKey(strike.getId());
			if (stats.getCooldowns().hasCooldown(cooldownKey)) return;
			// One strike at a time: owning several of them must not mean chaining them.
			if (stats.getCooldowns().hasCooldown(Cooldowns.STRIKE_GLOBAL)) return;

			double cost = strike.getCalculatedCost(stats);
			if (stats.getResources().getCurrentEnergy() < cost) return;

			stats.getResources().removeEnergy((int) Math.ceil(cost));
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);

			if (OOZARU_SLAM_ID.equals(strike.getId())) {
				MinecraftForge.EVENT_BUS.post(new DMZEvent.StrikeAttackCastEvent(player, stats, strike));
				startSlam(player, stats, strike);
				return;
			}

			if (DIM_PUNCH_ID.equals(strike.getId())) {
				MinecraftForge.EVENT_BUS.post(new DMZEvent.StrikeAttackCastEvent(player, stats, strike));
				startDimensionalPunch(player, stats, strike, preferredTargetId);
				return;
			}

			if (DIM_SLASH_ID.equals(strike.getId())) {
				MinecraftForge.EVENT_BUS.post(new DMZEvent.StrikeAttackCastEvent(player, stats, strike));
				startDimensionalSlash(player, stats, strike, preferredTargetId);
				return;
			}

			boolean isFlying = stats.getSkills().isSkillActive("fly");
			double coneRange = isFlying ? CONE_RANGE_FLY : CONE_RANGE;

			LivingEntity immediateTarget = findConeTarget(player, coneRange, preferredTargetId);

			PendingStrike pending = new PendingStrike(
					player.getUUID(),
					immediateTarget != null ? immediateTarget.getUUID() : null,
					strike.getId(),
					strike.getAnimationId(),
					strike.getDurationTicks(),
					strike.getActualCooldown(),
					cost,
					CONNECT_WINDOW_TICKS
			);

			MinecraftForge.EVENT_BUS.post(new DMZEvent.StrikeAttackCastEvent(player, stats, strike));

			if (SPBlueHurricaneEntity.STRIKE_ID.equals(strike.getId())) {
				startTargetlessStrike(player, stats, strike, pending, false);
				return;
			}

			if (immediateTarget != null) {
				boolean faceTarget = !"dragon_fist".equals(strike.getId());
				PartEntity<?> hitPart = nearestPartInSight(player, coneRange);
					if (hitPart != null && hitPart.getParent() == immediateTarget) {
						teleportToPartFront(player, hitPart, immediateTarget, faceTarget);
					} else {
						teleportToTargetFront(player, immediateTarget, faceTarget);
					}
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
						MainSounds.TP_SHORT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
				startStrike(player, immediateTarget, pending);
			} else if (TARGETLESS_STRIKES.contains(strike.getId())) {
				startTargetlessStrike(player, stats, strike, pending, true);
			} else {
				dashForward(player, isFlying);
				PENDING.put(player.getUUID(), pending);
			}
		});
	}
	public static boolean interrupt(ServerPlayer player) {
		if (player == null || player.level().isClientSide) return false;

		UUID id = player.getUUID();
		boolean interrupted = false;

		PendingStrike pending = PENDING.get(id);
		if (pending != null) {
			failPending(player, pending);
			interrupted = true;
		}

		ActiveStrike active = ACTIVE.get(id);
		if (active != null) {
			LivingEntity target = resolveLiving(player, active.targetId());
			endStrike(player, target, active, true);
			interrupted = true;
		}

		if (interrupted) discardStrikeProjectiles(player);
		return interrupted;
	}

	/** Removes the fist entities this player still owns, so an interrupted strike stops hitting. */
	private static void discardStrikeProjectiles(ServerPlayer player) {
		for (AbstractKiProjectile projectile : player.level().getEntitiesOfClass(AbstractKiProjectile.class,
				player.getBoundingBox().inflate(48.0))) {
			if (!(projectile instanceof SPDragonFistEntity) && !(projectile instanceof OzaruFistEntity) && !(projectile instanceof SPBlueHurricaneEntity)) continue;
			if (projectile.getOwner() != player) continue;
			projectile.discard();
		}
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
		STRIKE_ANCHOR_PART.remove(id);
		SLAM_RINGS.remove(id);
		ActiveStrike active = ACTIVE.remove(id);
		if (active != null && event.getEntity() instanceof ServerPlayer attacker) {
			clearVictimStrikeLock(attacker, null, active.targetId());
			stopVictimAnimation(attacker, null, active.targetId());
			stopStrikeHurtAnimation(attacker);
		}
		STRIKE_HURT_VICTIM.remove(id);
		FLINCH_SIDE.remove(id);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (stats.getStatus().isStrikeLocked()) {
				stats.getStatus().setStrikeLocked(false);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			}
		});
	}

	private static void clearVictimStrikeLock(ServerPlayer player, LivingEntity target, UUID targetId) {
		if (targetId != null && player.getServer() != null) {
			ServerPlayer victim = player.getServer().getPlayerList().getPlayer(targetId);
			if (victim != null) {
				setStrikeLocked(victim, false);
				return;
			}
		}
		if (target instanceof ServerPlayer serverTarget) setStrikeLocked(serverTarget, false);
	}

	private static void stopVictimAnimation(ServerPlayer player, LivingEntity target, UUID targetId) {
		if (targetId != null && player.getServer() != null) {
			ServerPlayer victim = player.getServer().getPlayerList().getPlayer(targetId);
			if (victim != null) {
				NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(victim.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), victim);
				return;
			}
		}
		if (target instanceof ServerPlayer serverTarget) {
			NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(serverTarget.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), serverTarget);
		}
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
        try {
            processActiveInternal(player);
        } catch (Exception e) {
            LogUtil.error(Env.SERVER, "Error procesando ActiveStrike de " + player.getName().getString() + ", forzando cierre", e);
            ActiveStrike active = ACTIVE.get(player.getUUID());
            if (active != null) {
                LivingEntity target = resolveLiving(player, active.targetId());
                endStrike(player, target, active);
            }
        }
    }

	private static void processActiveInternal(ServerPlayer player) {
		ActiveStrike active = ACTIVE.get(player.getUUID());
		if (active == null) return;

		if (OOZARU_SLAM_ID.equals(active.techniqueId())) {
			processSlam(player, active);
			return;
		}

		if (DIM_PUNCH_ID.equals(active.techniqueId())) {
			processDimensionalPunch(player, active);
			return;
		}

		if (DIM_SLASH_ID.equals(active.techniqueId())) {
			processDimensionalSlash(player, active);
			return;
		}

		boolean targetless = active.targetId() == null;
		LivingEntity target = targetless ? null : resolveLiving(player, active.targetId());

		if (!player.isAlive() || (!targetless && (target == null || !target.isAlive()))) {
			endStrike(player, target, active);
			return;
		}

		boolean stunned = player.hasEffect(MainEffects.STUN.get())
				|| StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(stats -> stats.getStatus().isKnockedDown())
				.orElse(false);
		if (stunned) {
			endStrike(player, target, active, true);
			discardStrikeProjectiles(player);
			return;
		}

        if (SPBlueHurricaneEntity.STRIKE_ID.equals(active.techniqueId())) {
            if (active.ticksElapsed() == 0) {
                int firingTicks = Math.max(20, active.durationTicks() - BLUE_HURRICANE_CAST_TICKS);
                SPBlueHurricaneEntity hurricane = new SPBlueHurricaneEntity(player.level(), player);
                hurricane.setupHurricane(player, (float) active.totalDamage(), 1.0f, BLUE_HURRICANE_CAST_TICKS, firingTicks);
            }

            if (active.ticksElapsed() >= active.durationTicks()) {
                endStrike(player, null, active);
            } else {
                ACTIVE.put(player.getUUID(), active.withTicksElapsed(active.ticksElapsed() + 1));
            }
            return;
        }

        if ("dragon_fist".equals(active.techniqueId())) {

            if (active.ticksElapsed() == 5) {
                SPDragonFistEntity dragonFist = new SPDragonFistEntity(player.level(), player);
                dragonFist.setupDragonFist(player, (float) active.totalDamage(), 1.0f);

                if (active.targetId() != null) {
                    try {
                        dragonFist.setStrikeStun(active.durationTicks() / 2, active.targetId());
                    } catch (Exception e) {
                    }
                }
            }

            if (active.ticksElapsed() >= active.durationTicks()) {

                if (target != null && !player.level().isClientSide) {
                    try {
                        KiExplosionVisualEntity explosion = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), player.level());
                        explosion.setPos(target.getX(), target.getY() + 1.0, target.getZ());
                        explosion.setupExplosion(0xFFD700, 0xFF8C00, 5.0F);
                        player.level().addFreshEntity(explosion);
                    } catch (Exception e) {
                    }
                }

                endStrike(player, target, active);
            } else {
                ACTIVE.put(player.getUUID(), active.withTicksElapsed(active.ticksElapsed() + 1));
            }
            return;
        }

        if ("oozaru_fist".equals(active.techniqueId())) {
            int currentTick = active.ticksElapsed();

            if (currentTick < 10) {
                Vec3 lookDownPos = player.getEyePosition().add(0, -10.0, 0);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, lookDownPos);
                player.setXRot(90.0F);

                freezeEntity(player);
                freezeEntity(target);
            }

            else if (currentTick == 10) {
                Vec3 lookDownPos = player.getEyePosition().add(0, -10.0, 0);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, lookDownPos);
                player.setXRot(90.0F);

                KiWaveEntity kamehameha = new KiWaveEntity(player.level(), player);
                kamehameha.setupKiHame(player, (float) active.totalDamage() * 0.2F, 2.0F, 0.5F, 5);
                kamehameha.setFiring(true);
                kamehameha.setMaxLife(15);
                kamehameha.setBlockDestructionEnabled(false);

                player.level().addFreshEntity(kamehameha);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.KI_KAME_FIRE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
            }

            else if (currentTick > 10 && currentTick < 20) {
                faceStrikeTarget(player, target);
                freezeEntity(target);
            }

            else if (currentTick == 20) {
                faceStrikeTarget(player, target);

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.OOZARU_GROWL_PLAYER.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);

                OzaruFistEntity ozaruFist = new OzaruFistEntity(player.level(), player);
                ozaruFist.setupOzaruFist(player, (float) active.totalDamage(), 1.0f);

                try {
                    ozaruFist.setStrikeStun(active.durationTicks() / 2, active.targetId());
                } catch (Exception e) {
                }
            }

            if (currentTick >= active.durationTicks()) {

                if (!player.level().isClientSide) {
                    try {
                        KiExplosionVisualEntity explosion = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), player.level());
                        explosion.setPos(target.getX(), target.getY() + 1.0, target.getZ());
                        explosion.setupExplosion(0xFFFFFF, 0x73FFEE, 3.0F);
                        player.level().addFreshEntity(explosion);
                    } catch (Exception e) {
                    }
                }

                endStrike(player, target, active);
            } else {
                ACTIVE.put(player.getUUID(), active.withTicksElapsed(currentTick + 1));
            }
            return;
        }


		if ("meteor".equals(active.techniqueId())) {
			if (target instanceof ServerPlayer targetPlayer) {
				faceEntity(targetPlayer, player);
			} else {
				faceEntity(target, player);
			}

			player.invulnerableTime = 20;

			Vec3 lookVec = Vec3.directionFromRotation(0, player.getYRot()).normalize();

			double advanceSpeed = 0.25;
			player.setDeltaMovement(lookVec.x * advanceSpeed, player.getDeltaMovement().y, lookVec.z * advanceSpeed);
			player.hurtMarked = true;

			double distance = 1.5;
			double targetX = player.getX() + lookVec.x * distance;
			double targetY = player.getY();
			double targetZ = player.getZ() + lookVec.z * distance;

			target.setPos(targetX, targetY, targetZ);
			target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
			target.hurtMarked = true;

			int nextTick = active.ticksElapsed() + 1;

			if (nextTick % active.hitIntervalTicks() == 0 && nextTick < active.durationTicks()) {
				applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId(), false);

				player.level().playSound(
						null, target.getX(), target.getY(), target.getZ(),
						MainSounds.GOLPE1.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						1.0F,
						0.8F + (player.getRandom().nextFloat() * 0.4F)
				);
			}

			if (nextTick >= active.durationTicks()) {
				applyStrikeDamage(player, target, active.finalDamage(), active.techniqueId(), true);
				grantKillXpIfNeeded(player, target, active.techniqueId());

				player.level().playSound(
						null, target.getX(), target.getY(), target.getZ(),
						MainSounds.CRITICO1.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						2.0F,
						1.0F
				);

				Vec3 pushDir = player.getLookAngle().normalize();
				KnockbackHelper.apply(target, new Vec3(pushDir.x * 2.5, 0.4, pushDir.z * 2.5));

				playStrikeKnockbackAnimation(target);

				MomentumImpactHandler.CollisionImpactType impactType = target.onGround() || pushDir.y < -0.5
						? MomentumImpactHandler.CollisionImpactType.GROUND
						: MomentumImpactHandler.CollisionImpactType.WALL;
				MomentumImpactHandler.registerCollisionImpact(target, impactType, (float) (active.totalDamage() * IMPACT_DAMAGE_RATIO), pushDir);

				endStrike(player, target, active);
				return;
			}

			ACTIVE.put(player.getUUID(), active.withTicksElapsed(nextTick));
			return;
		}

		if ("gum_punch".equals(active.techniqueId())) {
			int nextTick = active.ticksElapsed() + 1;

			player.invulnerableTime = 20;
			freezeEntity(player);

			if (nextTick < GUM_PUNCH_IMPACT_TICK && target != null) {
				faceStrikeTarget(player, target);
				target.invulnerableTime = 20;
				freezeEntity(target);
			}

			if (nextTick == GUM_PUNCH_IMPACT_TICK) {
				LivingEntity victim = strikeVictim(player, target);
				Vec3 impactPos = strikeImpactPoint(player, victim, 0.6);

				if (victim != null) {
					victim.invulnerableTime = 0;
					applyStrikeDamage(player, victim, strikeHitDamage(player, target, victim, active.totalDamage()), active.techniqueId(), true);
					grantKillXpIfNeeded(player, victim, active.techniqueId());
				}

				player.level().playSound(
						null, impactPos.x, impactPos.y, impactPos.z,
						MainSounds.CRITICO1.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						2.0F,
						0.7F
				);

				NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(impactPos, player.getLookAngle(),
						GUM_PUNCH_BURST_SCALE, GUM_PUNCH_COLOR_PRIMARY, GUM_PUNCH_COLOR_SECONDARY, false, GUM_PUNCH_BURST_TICKS), player);

				if (victim != null) {
					Vec3 pushDir = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
					KnockbackHelper.apply(victim, new Vec3(pushDir.x * GUM_PUNCH_KNOCKBACK, 0.5, pushDir.z * GUM_PUNCH_KNOCKBACK));

					playStrikeKnockbackAnimation(victim);

					MomentumImpactHandler.CollisionImpactType impactType = victim.onGround()
							? MomentumImpactHandler.CollisionImpactType.GROUND
							: MomentumImpactHandler.CollisionImpactType.WALL;
					MomentumImpactHandler.registerCollisionImpact(victim, impactType, (float) (active.totalDamage() * IMPACT_DAMAGE_RATIO), pushDir);
				}
			}

			if (nextTick >= GUM_PUNCH_END_TICK) {
				endStrike(player, target, active);
				return;
			}

			ACTIVE.put(player.getUUID(), active.withTicksElapsed(nextTick));
			return;
		}

		if ("super_god_fist".equals(active.techniqueId())) {

			int nextTick = active.ticksElapsed() + 1;

			if (nextTick < 14 && target != null) {
				faceStrikeTarget(player, target);
				if (target instanceof ServerPlayer targetPlayer) {
					faceEntity(targetPlayer, player);
				} else {
					faceEntity(target, player);
				}
			}

			player.invulnerableTime = 20;

			if (nextTick <= 12) {
				if (target != null) {
					target.invulnerableTime = 20;

					double dist = player.distanceTo(target);
					if (dist > 1.5) {
						Vec3 dir = target.position().subtract(player.position()).normalize();
						double dashSpeed = 1.5;
						player.setDeltaMovement(dir.x * dashSpeed, player.getDeltaMovement().y, dir.z * dashSpeed);
						player.hurtMarked = true;
						spawnSuperGodFistTrail(player);
					} else {
						freezeEntity(player);
					}
					freezeEntity(target);
				} else if (findFrontVictim(player) != null) {
					freezeEntity(player);
				} else {
					Vec3 dir = Vec3.directionFromRotation(0, player.getYRot()).normalize();
					double dashSpeed = 1.5;
					player.setDeltaMovement(dir.x * dashSpeed, player.getDeltaMovement().y, dir.z * dashSpeed);
					player.hurtMarked = true;
					spawnSuperGodFistTrail(player);
				}
			}
			else if (nextTick == 13) {
				if (target != null) {
					target.invulnerableTime = 20;
					freezeEntity(target);
				}
				freezeEntity(player);
			}
			else if (nextTick == 14) {
				LivingEntity victim = strikeVictim(player, target);
				Vec3 impactPos = strikeImpactPoint(player, victim, 0.5);

				if (victim != null) {
					victim.invulnerableTime = 0;
					applyStrikeDamage(player, victim, strikeHitDamage(player, target, victim, active.totalDamage()), active.techniqueId(), true);
					grantKillXpIfNeeded(player, victim, active.techniqueId());
				}

				player.level().playSound(
						null, impactPos.x, impactPos.y, impactPos.z,
						MainSounds.CRITICO2.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						2.5F,
						0.7F
				);

				NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(impactPos, player.getLookAngle(),
						GOD_FIST_BURST_SCALE, GOD_FIST_COLOR_PRIMARY, GOD_FIST_COLOR_SECONDARY, false, GOD_FIST_BURST_TICKS), player);

				if (victim != null) {
					Vec3 pushDir = player.getLookAngle().normalize();
					double knockbackPower = 4.0;
					KnockbackHelper.apply(victim, new Vec3(pushDir.x * knockbackPower, 0.6, pushDir.z * knockbackPower));

					playStrikeKnockbackAnimation(victim);

					MomentumImpactHandler.CollisionImpactType impactType = victim.onGround() || pushDir.y < -0.5
							? MomentumImpactHandler.CollisionImpactType.GROUND
							: MomentumImpactHandler.CollisionImpactType.WALL;
					MomentumImpactHandler.registerCollisionImpact(victim, impactType, (float) (active.totalDamage() * IMPACT_DAMAGE_RATIO), pushDir);

					playVictimHurtPose(player, victim, GOD_FIST_HURT_ANIM, DBSagasEntity.HURT_ANIM_GODFIST);
				}

				freezeEntity(player);
			}
			else if (nextTick < 25) {
				freezeEntity(player);
			}

			if (nextTick >= 35) {
				endStrike(player, target, active);
				return;
			}

			ACTIVE.put(player.getUUID(), active.withTicksElapsed(nextTick));
			return;
		}

		if ("deadly_dance".equals(active.techniqueId()) || "deadly_dance_vegetto".equals(active.techniqueId())) {

			boolean vegetto = "deadly_dance_vegetto".equals(active.techniqueId());

			if (target instanceof ServerPlayer targetPlayer) {
				faceEntity(targetPlayer, player);
			} else if (target != null) {
				faceEntity(target, player);
			}

			player.invulnerableTime = 20;

			Vec3 lookVec = Vec3.directionFromRotation(0, player.getYRot()).normalize();

			double distance = 1.5;
			double targetX = player.getX() + lookVec.x * distance;
			double targetY = player.getY();
			double targetZ = player.getZ() + lookVec.z * distance;

			boolean blocked = player.horizontalCollision || (target != null && !canOccupy(target, targetX, targetY, targetZ));

			if (!blocked) {
				double advanceSpeed = 0.25;
				player.setDeltaMovement(lookVec.x * advanceSpeed, player.getDeltaMovement().y, lookVec.z * advanceSpeed);
				player.hurtMarked = true;

				if (target != null) {
					target.setPos(targetX, targetY, targetZ);
					target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
					target.hurtMarked = true;
				}
			} else {
				player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
				player.hurtMarked = true;
				if (target != null) {
					target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
					target.hurtMarked = true;
				}
			}

			int nextTick = active.ticksElapsed() + 1;

			if (nextTick % active.hitIntervalTicks() == 0 && nextTick < 30) {
				LivingEntity victim = strikeVictim(player, target);
				if (victim != null) {
					applyStrikeDamage(player, victim, strikeHitDamage(player, target, victim, active.perHitDamage()), active.techniqueId(), false);
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(strikeImpactPoint(player, victim, 0.6), lookVec,
						DEADLY_DANCE_HIT_BURST_SCALE, DEADLY_DANCE_COLOR_PRIMARY, deadlyDanceSecondaryColor(vegetto), vegetto,
						DEADLY_DANCE_HIT_BURST_TICKS), player);

				Vec3 soundPos = strikeImpactPoint(player, victim, 0.0);
				player.level().playSound(
						null, soundPos.x, soundPos.y, soundPos.z,
						MainSounds.GOLPE1.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						1.0F,
						0.8F + (player.getRandom().nextFloat() * 0.4F)
				);
			}

			if (nextTick >= 30) {
				LivingEntity victim = strikeVictim(player, target);
				if (victim != null) {
					applyStrikeDamage(player, victim, strikeHitDamage(player, target, victim, active.finalDamage()), active.techniqueId(), true);
					grantKillXpIfNeeded(player, victim, active.techniqueId());
				}

				NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(strikeImpactPoint(player, victim, 0.5),
						new Vec3(lookVec.x * 0.35, 1.0, lookVec.z * 0.35),
						DEADLY_DANCE_FINAL_BURST_SCALE, DEADLY_DANCE_COLOR_PRIMARY, deadlyDanceSecondaryColor(vegetto), vegetto,
						DEADLY_DANCE_FINAL_BURST_TICKS), player);

				Vec3 soundPos = strikeImpactPoint(player, victim, 0.0);
				player.level().playSound(
						null, soundPos.x, soundPos.y, soundPos.z,
						MainSounds.CRITICO2.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						2.0F,
						1.0F
				);

				if (victim != null) {
					Vec3 pushDir = player.getLookAngle().normalize();
					double upwardForce = 1.5;
					double forwardForce = 0.5;

					KnockbackHelper.apply(victim, new Vec3(pushDir.x * forwardForce, upwardForce, pushDir.z * forwardForce));

					playStrikeKnockbackAnimation(victim);

					MomentumImpactHandler.registerCollisionImpact(victim, MomentumImpactHandler.CollisionImpactType.GROUND, (float) (active.totalDamage() * IMPACT_DAMAGE_RATIO), new Vec3(0, 1, 0));
				}

				endStrike(player, target, active);
				return;
			}

			ACTIVE.put(player.getUUID(), active.withTicksElapsed(nextTick));
			return;
		}

		if (SPIRIT_CANNON_ID.equals(active.techniqueId())) {

			int nextTick = active.ticksElapsed() + 1;
			double hitDamage = active.totalDamage() * (1.0 - FINAL_HIT_RATIO) / 3.0;

			player.invulnerableTime = 20;
			player.fallDistance = 0.0F;

			if (nextTick <= SPIRIT_CANNON_LAUNCH_TICK) {
				faceStrikeTarget(player, target);
				faceEntity(target, player);
			}

			if (nextTick < SPIRIT_CANNON_KNEE_TICK) {
				target.invulnerableTime = 20;
				double dist = player.distanceTo(target);
				if (dist > 1.6) {
					Vec3 dir = target.position().subtract(player.position()).normalize();
					player.setDeltaMovement(dir.x * 1.4, player.getDeltaMovement().y, dir.z * 1.4);
					player.hurtMarked = true;
				} else {
					freezeEntity(player);
				}
				freezeEntity(target);
			}
			else if (nextTick == SPIRIT_CANNON_KNEE_TICK) {
				spiritCannonHit(player, target, hitDamage, false, 0.45, SPIRIT_CANNON_HIT_BURST_SCALE,
						MainSounds.GOLPE4.get(), 1.0F);
				playVictimHurtPose(player, target, HURT_TOP2_ANIM, DBSagasEntity.HURT_ANIM_TOP2);
				freezeEntity(player);
				freezeEntity(target);
			}
			else if (nextTick == SPIRIT_CANNON_UPPERCUT_TICK) {
				spiritCannonHit(player, target, hitDamage, false, 0.8, SPIRIT_CANNON_HIT_BURST_SCALE,
						MainSounds.GOLPE5.get(), 1.15F);
				playVictimHurtPose(player, target, HURT_TOP2_ANIM, DBSagasEntity.HURT_ANIM_TOP2);
				freezeEntity(player);
				freezeEntity(target);
			}
			else if (nextTick == SPIRIT_CANNON_LAUNCH_TICK) {
				spiritCannonHit(player, target, hitDamage, false, 0.55, SPIRIT_CANNON_LAUNCH_BURST_SCALE,
						MainSounds.CRITICO2.get(), 1.25F);
				playVictimHurtPose(player, target, HURT_TOP_ANIM, DBSagasEntity.HURT_ANIM_TOP);
				KnockbackHelper.apply(target, new Vec3(0.0, 1.2, 0.0));
				freezeEntity(player);
			}
			else if (nextTick == SPIRIT_CANNON_ELBOW_TICK) {
				spiritCannonHit(player, target, active.totalDamage() * FINAL_HIT_RATIO, true, 0.9,
						SPIRIT_CANNON_ELBOW_BURST_SCALE, MainSounds.CRITICO1.get(), 0.8F);
				grantKillXpIfNeeded(player, target, active.techniqueId());
				playVictimHurtPose(player, target, HURT_DOWN_ANIM, DBSagasEntity.HURT_ANIM_DOWN);
				KnockbackHelper.apply(target, new Vec3(0.0, -SPIRIT_CANNON_SLAM_POWER, 0.0));
				MomentumImpactHandler.registerCollisionImpact(target, MomentumImpactHandler.CollisionImpactType.GROUND,
						(float) (active.totalDamage() * IMPACT_DAMAGE_RATIO), new Vec3(0.0, -1.0, 0.0),
						SPIRIT_CANNON_COLOR_PRIMARY, SPIRIT_CANNON_COLOR_SECONDARY, SPIRIT_CANNON_GROUND_BURST_SCALE);
				freezeEntity(player);
			}
			else if (nextTick < SPIRIT_CANNON_LAUNCH_TICK) {
				target.invulnerableTime = 20;
				freezeEntity(player);
				freezeEntity(target);
			}
			else if (nextTick < SPIRIT_CANNON_ELBOW_TICK) {
				target.invulnerableTime = 20;
				boolean lifting = nextTick <= SPIRIT_CANNON_LAUNCH_TICK + SPIRIT_CANNON_LIFT_TICKS;

				if (lifting) {
					KnockbackHelper.apply(target, new Vec3(0.0, SPIRIT_CANNON_LIFT_SPEED, 0.0));
					player.setDeltaMovement(0.0, SPIRIT_CANNON_LIFT_SPEED, 0.0);
				} else {
					KnockbackHelper.apply(target, new Vec3(0.0, 0.04, 0.0));
					player.setDeltaMovement(0.0, 0.04, 0.0);
				}

				target.fallDistance = 0.0F;
				player.hurtMarked = true;
			}
			else if (nextTick < SPIRIT_CANNON_END_TICK) {
				player.setDeltaMovement(0.0, 0.04, 0.0);
				player.hurtMarked = true;
			}

			if (nextTick >= SPIRIT_CANNON_END_TICK) {
				endStrike(player, target, active);
				return;
			}

			ACTIVE.put(player.getUUID(), active.withTicksElapsed(nextTick));
			return;
		}

		if ("kaioken_attack".equals(active.techniqueId())) {

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				stats.getStatus().setAuraActive(true);

				if (active.ticksElapsed() % 10 == 0) {
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				}
			});

			int nextTick = active.ticksElapsed() + 1;

			if (nextTick < 20) {
				faceStrikeTarget(player, target);
				if (target instanceof ServerPlayer targetPlayer) {
					faceEntity(targetPlayer, player);
				} else {
					faceEntity(target, player);
				}
			}

			player.invulnerableTime = 20;

			if (nextTick < 10) {
				double dist = player.distanceTo(target);
				if (dist > 1.5) {
					Vec3 dir = target.position().subtract(player.position()).normalize();
					player.setDeltaMovement(dir.scale(1.5));
					player.hurtMarked = true;
				} else {
					freezeEntity(player);
				}
				freezeEntity(target);
			}
			else if (nextTick == 10) {
				applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId(), false);
				player.level().playSound(null, target.getX(), target.getY(), target.getZ(), MainSounds.GOLPE1.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.0F);

				Vec3 pushDir = player.getLookAngle().normalize();
				KnockbackHelper.apply(target, new Vec3(pushDir.x * 1.5, 0.4, pushDir.z * 1.5));
				freezeEntity(player);
			}
			else if (nextTick < 15) {
				Vec3 dir = target.position().subtract(player.position()).normalize();
				player.setDeltaMovement(dir.scale(2.5));
				player.hurtMarked = true;
			}
			else if (nextTick == 15) {
				applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId(), false);
				player.level().playSound(null, target.getX(), target.getY(), target.getZ(), MainSounds.CRITICO2.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.2F);

				freezeEntity(target);
				freezeEntity(player);
			}
			else if (nextTick < 20) {
				freezeEntity(target);
				freezeEntity(player);
			}
			else if (nextTick == 20) {
				applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId(), false);
				player.level().playSound(null, target.getX(), target.getY(), target.getZ(), MainSounds.CRITICO2.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.8F);

				Vec3 pushDir = player.getLookAngle().normalize();
				KnockbackHelper.apply(target, new Vec3(pushDir.x * 3.5, 0.2, pushDir.z * 3.5));
				playStrikeKnockbackAnimation(target);

				freezeEntity(player);
			}
			else if (nextTick < 34) {
				freezeEntity(player);
				if (nextTick == 21) {
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.KI_EXPLOSION_CHARGE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
				}
			}
			else if (nextTick == 34) {
				freezeEntity(player);

				applyStrikeDamage(player, target, active.finalDamage() * 0.1, active.techniqueId(), false);

				KiWaveEntity kamehameha = new KiWaveEntity(player.level(), player);
				kamehameha.setupKiHame(player, (float) active.finalDamage() * 0.9F, 2.0F, 1.0F, 10);
				kamehameha.setFiring(true);
				kamehameha.setMaxLife(40);
				player.level().addFreshEntity(kamehameha);

				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.KI_KAME_FIRE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
			}
			else if (nextTick < 50) {
				freezeEntity(player);
			}
			else if (nextTick >= 50) {
				grantKillXpIfNeeded(player, target, active.techniqueId());

				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					stats.getStatus().setAuraActive(false);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
				});

				endStrike(player, target, active);
				return;
			}

			ACTIVE.put(player.getUUID(), active.withTicksElapsed(nextTick));
			return;
		}

		if ("wolf_fang".equals(active.techniqueId())) {
			freezeEntity(player);
			if (target != null) {
				freezeEntity(target);
				faceStrikeTarget(player, target);
				if (target instanceof ServerPlayer targetPlayer) {
					faceEntity(targetPlayer, player);
				}
			}
			player.invulnerableTime = 20;

			int wolfTick = active.ticksElapsed() + 1;
			int wolfDuration = active.durationTicks();

			if (wolfTick % active.hitIntervalTicks() == 0 && wolfTick < wolfDuration) {
				LivingEntity victim = strikeVictim(player, target);
				if (victim != null) {
					applyStrikeDamage(player, victim, strikeHitDamage(player, target, victim, active.perHitDamage()), active.techniqueId(), false);
				}
			}

			if (wolfTick < wolfDuration - 3 && wolfTick % 4 == 0) {
				spawnWolfFangJab(player, strikeImpactPoint(player, strikeVictim(player, target), 0.6), wolfTick);
			}

			if (wolfTick >= wolfDuration) {
				LivingEntity victim = strikeVictim(player, target);
				if (victim != null) {
					applyStrikeDamage(player, victim, strikeHitDamage(player, target, victim, active.finalDamage()), active.techniqueId(), true);
					grantKillXpIfNeeded(player, victim, active.techniqueId());
				}

				Vec3 finalPos = strikeImpactPoint(player, victim, 0.5);
				double sx = finalPos.x;
				double sy = finalPos.y;
				double sz = finalPos.z;

				player.level().playSound(null, sx, sy, sz,
						MainSounds.CRITICO2.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.7F);
				player.level().playSound(null, sx, sy, sz,
						MainSounds.KI_EXPLOSION_IMPACT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.5F, 1.0F);
				player.level().playSound(null, sx, sy, sz,
						MainSounds.OOZARU_GROWL_PLAYER.get(), net.minecraft.sounds.SoundSource.PLAYERS, 3.0F, 1.15F);

				NetworkHandler.sendToTrackingEntityAndSelf(new ClawSlashVfxS2C(finalPos.x, finalPos.y, finalPos.z,
						WOLF_FANG_FINAL_CLAW_SCALE, WOLF_FANG_CLAW_COLOR, WOLF_FANG_FINAL_CLAW_TICKS, WOLF_FANG_FINAL_CLAWS, true), player);
				if (victim == null) {
					NetworkHandler.sendToTrackingEntityAndSelf(new ShockwaveVfxS2C(finalPos.x, finalPos.y, finalPos.z,
							SHOCKWAVE_FINAL_SCALE, shockwaveColor(active.techniqueId()), SHOCKWAVE_FINAL_TICKS), player);
				}

				if (victim != null) applyKnockback(player, victim, active.totalDamage());
				endStrike(player, target, active);
				return;
			}

			ACTIVE.put(player.getUUID(), active.withTicksElapsed(wolfTick));
			return;
		}

		freezeEntity(player);
		freezeEntity(target);
		faceStrikeTarget(player, target);
		if (target instanceof ServerPlayer targetPlayer) {
			faceEntity(targetPlayer, player);
		}

		int nextTick = active.ticksElapsed() + 1;
		if (nextTick % active.hitIntervalTicks() == 0 && nextTick < active.durationTicks()) {
			applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId(), false);
		}

		if (nextTick >= active.durationTicks()) {
			applyStrikeDamage(player, target, active.finalDamage(), active.techniqueId(), true);
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

			DMZEvent.DamageModifyEvent modifyEvent =
					new DMZEvent.DamageModifyEvent(player, target, totalDamage, 0.0,
							DMZEvent.DamageSourceType.STRIKE);
			totalDamage = MinecraftForge.EVENT_BUS.post(modifyEvent) ? 0.0 : Math.max(0.0, modifyEvent.getAmount());

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
			// Grant invulnerability the instant the strike locks on, before the next tick's
			// processActive runs, so the target can't land a free hit during the engage window.
			player.invulnerableTime = 20;
			MinecraftForge.EVENT_BUS.post(
					new DMZEvent.StrikeAttackFireEvent(player, stats, strike, target));

			applyStrikeDamage(player, target, perHitDamage, pending.techniqueId(), false);
			//teleportToTargetFront(player, target);
			setStrikeLocked(player, true);
			setStrikeLocked(target, true);

			PartEntity<?> anchorPart = nearestPartInSight(player, CONE_RANGE_FLY);
			if (anchorPart != null && anchorPart.getParent() == target) {
				STRIKE_ANCHOR_PART.put(player.getUUID(), anchorPart.getId());
			} else {
				STRIKE_ANCHOR_PART.remove(player.getUUID());
			}

			if (!"dragon_fist".equals(pending.techniqueId())) {
				faceStrikeTarget(player, target);
			}
			if (target instanceof ServerPlayer targetPlayer) {
				faceEntity(targetPlayer, player);
			}
			playStrikeAnimation(player, pending.animationId());
		});
	}

	private static void startTargetlessStrike(ServerPlayer player, com.dragonminez.common.stats.StatsData stats, StrikeAttackData strike, PendingStrike pending, boolean lockPlayer) {
		double totalDamage = stats.getStrikeDamage() * strike.getDamageMultiplier() * Math.max(0.0,
				ConfigManager.getTechniqueConfig().getStrikeConfig(strike.getId()).getDamageMultiplier());

		int durationTicks = Math.max(20, pending.durationTicks());
		int hitCount = Math.max(1, (int) Math.ceil(durationTicks / (double) HIT_INTERVAL_TICKS));

		ActiveStrike active = new ActiveStrike(
				player.getUUID(),
				null,
				pending.techniqueId(),
				pending.animationId(),
				durationTicks,
				pending.cooldownTicks(),
				totalDamage,
				(totalDamage * (1.0 - FINAL_HIT_RATIO)) / hitCount,
				totalDamage * FINAL_HIT_RATIO,
				HIT_INTERVAL_TICKS,
				0
		);
		ACTIVE.put(player.getUUID(), active);
		STRIKE_ANCHOR_PART.remove(player.getUUID());
		player.invulnerableTime = 20;
		if (lockPlayer) setStrikeLocked(player, true);
		playStrikeAnimation(player, pending.animationId());
	}

	private static LivingEntity strikeVictim(ServerPlayer player, LivingEntity target) {
		return target != null ? target : findFrontVictim(player);
	}

	private static LivingEntity findFrontVictim(ServerPlayer player) {
		Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
		AABB area = player.getBoundingBox().move(forward.x * FRONT_REACH, 0.0, forward.z * FRONT_REACH).inflate(0.6, 0.2, 0.6);

		LivingEntity closest = null;
		double closestDist = Double.MAX_VALUE;
		for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, area,
				e -> e != player && e.isAlive() && !e.isSpectator() && TargetHelper.canAttack(player, e, CONE_RANGE))) {
			double dist = player.distanceToSqr(candidate);
			if (dist < closestDist) {
				closest = candidate;
				closestDist = dist;
			}
		}
		return closest;
	}

	private static double strikeHitDamage(ServerPlayer player, LivingEntity target, LivingEntity victim, double damage) {
		if (target != null) return damage;
		DMZEvent.DamageModifyEvent modifyEvent = new DMZEvent.DamageModifyEvent(
				player, victim, damage, 0.0, DMZEvent.DamageSourceType.STRIKE);
		return MinecraftForge.EVENT_BUS.post(modifyEvent) ? 0.0 : Math.max(0.0, modifyEvent.getAmount());
	}

	private static Vec3 strikeImpactPoint(ServerPlayer player, LivingEntity victim, double heightRatio) {
		if (victim != null) return new Vec3(victim.getX(), victim.getY() + victim.getBbHeight() * heightRatio, victim.getZ());
		Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
		return new Vec3(player.getX() + forward.x * FRONT_REACH,
				player.getY() + player.getBbHeight() * heightRatio,
				player.getZ() + forward.z * FRONT_REACH);
	}

	private static void spawnSuperGodFistTrail(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 motion = player.getDeltaMovement();
		Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);

		for (int i = 0; i < 4; i++) {
			Vec3 point = center.subtract(motion.scale(i / 4.0));
			level.sendParticles(MainParticles.SPARKS.get(), point.x, point.y, point.z, 0, 0.96, 0.77, 0.15, 1.0);
		}
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 2, 0.15, 0.3, 0.15, 0.0);
	}

	private static void startSlam(ServerPlayer player, com.dragonminez.common.stats.StatsData stats, StrikeAttackData strike) {
		double totalDamage = stats.getStrikeDamage() * strike.getDamageMultiplier() * Math.max(0.0,
				ConfigManager.getTechniqueConfig().getStrikeConfig(strike.getId()).getDamageMultiplier());

		ActiveStrike active = new ActiveStrike(
				player.getUUID(),
				null,
				strike.getId(),
				strike.getAnimationId(),
				SLAM_DURATION_TICKS,
				strike.getActualCooldown(),
				totalDamage,
				totalDamage,
				totalDamage,
				SLAM_DURATION_TICKS,
				0
		);
		ACTIVE.put(player.getUUID(), active);
		setStrikeLocked(player, true);
		playStrikeAnimation(player, strike.getAnimationId());
	}

	private static final java.util.Map<UUID, Vec3> DIM_PUNCH_MARKS = new java.util.HashMap<>();

	private static void markDimensionalPortal(ServerPlayer player, LivingEntity target, int punchIndex) {

		double y = target.getY();
		Vec3 mark = new Vec3(target.getX(), y, target.getZ());
		DIM_PUNCH_MARKS.put(player.getUUID(), mark);

		int openTicks = DIM_PUNCH_HIT_TICKS[punchIndex] - DIM_PUNCH_MARK_TICKS[punchIndex];
		boolean fromAbove = player.getRandom().nextInt(3) == 0;
		float fistYaw = player.getRandom().nextFloat() * 360.0F;
		float fistPitch = fromAbove ? 90.0F : -5.0F + player.getRandom().nextFloat() * 25.0F;
		double fistY = mark.y + (fromAbove ? 0.3D : target.getBbHeight() * 0.55D);

		NetworkHandler.sendToTrackingEntityAndSelf(new BossTelegraphS2C(mark.x, mark.y, mark.z,
				(float) DIM_PUNCH_RADIUS, DIM_PUNCH_COLOR, openTicks, -1), player);
		NetworkHandler.sendToTrackingEntityAndSelf(new DimensionalFistS2C(player.getId(),
				mark.x, fistY, mark.z, fistYaw, fistPitch, punchIndex == 1, openTicks), player);
	}

	private static void fireDimensionalPunch(ServerPlayer player, ActiveStrike active) {
		if (!(player.level() instanceof ServerLevel serverLevel)) return;

		Vec3 mark = DIM_PUNCH_MARKS.get(player.getUUID());
		if (mark == null) return;

		AABB area = new AABB(mark.x - DIM_PUNCH_RADIUS, mark.y - 2.0, mark.z - DIM_PUNCH_RADIUS,
				mark.x + DIM_PUNCH_RADIUS, mark.y + 4.0, mark.z + DIM_PUNCH_RADIUS);

		for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
			if (victim == player || victim.isAlliedTo(player)) continue;

			double dx = victim.getX() - mark.x;
			double dz = victim.getZ() - mark.z;
			if (Math.sqrt(dx * dx + dz * dz) > DIM_PUNCH_RADIUS) continue;

			applyStrikeDamage(player, victim, active.perHitDamage(), active.techniqueId(), false);
		}

		Vec3 center = mark.add(0.0, 1.0, 0.0);
		Vec3 dir = center.subtract(player.position()).normalize();

		NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(center, dir,
				(float) DIM_PUNCH_RADIUS * 1.5F, DIM_PUNCH_COLOR, 0xFFD700, true, 14), player);

		serverLevel.playSound(null, center.x, center.y, center.z,
				MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.PLAYERS, 2.2F, 1.35F);
	}

	private static void startDimensionalPunch(ServerPlayer player, com.dragonminez.common.stats.StatsData stats,
											  StrikeAttackData strike, int preferredTargetId) {
		double totalDamage = stats.getStrikeDamage() * strike.getDamageMultiplier() * Math.max(0.0,
				ConfigManager.getTechniqueConfig().getStrikeConfig(strike.getId()).getDamageMultiplier());
		double perHit = totalDamage / DIM_PUNCH_HIT_TICKS.length;

		LivingEntity target = findConeTarget(player, DIM_PUNCH_RANGE, preferredTargetId);

		ActiveStrike active = new ActiveStrike(
				player.getUUID(),
				target != null ? target.getUUID() : null,
				strike.getId(),
				strike.getAnimationId(),
				DIM_PUNCH_DURATION,
				strike.getActualCooldown(),
				totalDamage,
				perHit,
				perHit,
				DIM_PUNCH_DURATION,
				0
		);
		ACTIVE.put(player.getUUID(), active);
		setStrikeLocked(player, true);
		playStrikeAnimation(player, strike.getAnimationId());
	}

	private static void processDimensionalPunch(ServerPlayer player, ActiveStrike active) {
		boolean stunned = player.hasEffect(MainEffects.STUN.get())
				|| StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(stats -> stats.getStatus().isKnockedDown())
				.orElse(false);
		if (!player.isAlive() || stunned) {
			endStrike(player, null, active, stunned);
			return;
		}

		freezeEntity(player);

		int tick = active.ticksElapsed();
		LivingEntity target = active.targetId() == null ? null : resolveLiving(player, active.targetId());

		for (int i = 0; i < DIM_PUNCH_MARK_TICKS.length; i++) {
			if (tick != DIM_PUNCH_MARK_TICKS[i] || target == null) continue;
			markDimensionalPortal(player, target, i);
		}
		for (int hit : DIM_PUNCH_HIT_TICKS) {
			if (tick != hit) continue;
			fireDimensionalPunch(player, active);
		}

		if (tick + 1 >= active.durationTicks()) {
			endStrike(player, target, active);
			return;
		}
		ACTIVE.put(player.getUUID(), active.withTicksElapsed(tick + 1));
	}

	private static void startDimensionalSlash(ServerPlayer player, com.dragonminez.common.stats.StatsData stats,
											  StrikeAttackData strike, int preferredTargetId) {
		double totalDamage = stats.getStrikeDamage() * strike.getDamageMultiplier() * Math.max(0.0,
				ConfigManager.getTechniqueConfig().getStrikeConfig(strike.getId()).getDamageMultiplier());
		double perCut = totalDamage / DIM_SLASH_TICKS.length;

		LivingEntity target = findConeTarget(player, DIM_SLASH_RANGE, preferredTargetId);

		ActiveStrike active = new ActiveStrike(
				player.getUUID(),
				target != null ? target.getUUID() : null,
				strike.getId(),
				strike.getAnimationId(),
				DIM_SLASH_DURATION,
				strike.getActualCooldown(),
				totalDamage,
				perCut,
				perCut,
				DIM_SLASH_DURATION,
				0
		);
		ACTIVE.put(player.getUUID(), active);
		setStrikeLocked(player, true);
		playStrikeAnimation(player, strike.getAnimationId());
	}

	private static void processDimensionalSlash(ServerPlayer player, ActiveStrike active) {
		boolean stunned = player.hasEffect(MainEffects.STUN.get())
				|| StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(stats -> stats.getStatus().isKnockedDown())
				.orElse(false);
		if (!player.isAlive() || stunned) {
			endStrike(player, null, active, stunned);
			return;
		}

		freezeEntity(player);

		int tick = active.ticksElapsed();
		LivingEntity target = active.targetId() == null ? null : resolveLiving(player, active.targetId());

		for (int i = 0; i < DIM_SLASH_TICKS.length; i++) {
			if (tick != DIM_SLASH_TICKS[i]) continue;
			launchDimensionalSlash(player, target, active, i);
		}

		if (tick + 1 >= active.durationTicks()) {
			endStrike(player, target, active);
			return;
		}
		ACTIVE.put(player.getUUID(), active.withTicksElapsed(tick + 1));
	}

	private static void launchDimensionalSlash(ServerPlayer player, LivingEntity target, ActiveStrike active, int index) {
		if (!(player.level() instanceof ServerLevel serverLevel)) return;

		Vec3 look = player.getLookAngle();
		Vec3 origin = player.getEyePosition().add(0.0, -0.35, 0.0).add(look.scale(1.2));
		Vec3 aim = target != null && target.isAlive()
				? target.getBoundingBox().getCenter().subtract(origin)
				: look;

		double damage = active.perHitDamage();
		String techniqueId = active.techniqueId();

		com.dragonminez.common.combat.util.SwordSlashManager.launch(serverLevel, player, origin, aim,
				com.dragonminez.common.combat.util.SwordSlashManager.rollFor(index), DIM_SLASH_SCALE, DIM_SLASH_COLOR,
				DIM_SLASH_SPEED, DIM_SLASH_RANGE, DIM_SLASH_HIT_RADIUS, victim -> {
					if (victim.isAlliedTo(player) || !TargetHelper.canAttack(player, victim, DIM_SLASH_RANGE + 8.0)) return false;
					applyStrikeDamage(player, victim, damage, techniqueId, false);
					return true;
				});

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				MainSounds.KI_BEAM_FIRE.get(), SoundSource.PLAYERS, 1.4F, 1.7F);
	}

	private static void processSlam(ServerPlayer player, ActiveStrike active) {
		boolean stunned = player.hasEffect(MainEffects.STUN.get())
				|| StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(stats -> stats.getStatus().isKnockedDown())
				.orElse(false);
		if (!player.isAlive() || stunned) {
			endStrike(player, null, active, stunned);
			return;
		}

		freezeEntity(player);

		int tick = active.ticksElapsed();
		if (tick == SLAM_IMPACT_TICK) slamGround(player, active);
		else if (tick > SLAM_IMPACT_TICK && tick <= SLAM_IMPACT_TICK + SLAM_RING_TICKS) expandSlamRing(player, active, tick - SLAM_IMPACT_TICK);

		if (tick + 1 >= active.durationTicks()) {
			endStrike(player, null, active);
			return;
		}
		ACTIVE.put(player.getUUID(), active.withTicksElapsed(tick + 1));
	}

	private static void slamGround(ServerPlayer player, ActiveStrike active) {
		if (!(player.level() instanceof ServerLevel level)) return;

		AABB hitbox = player.getBoundingBox();
		Vec3 forward = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
		double reach = player.getBbWidth();
		AABB area = hitbox.move(forward.x * reach, 0.0, forward.z * reach);
		Vec3 impact = new Vec3(area.getCenter().x, hitbox.minY, area.getCenter().z);

		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, area,
				e -> e != player && e.isAlive() && !e.isSpectator()
						&& TargetHelper.canAttack(player, e, CONE_RANGE));

		java.util.Set<UUID> hit = new java.util.HashSet<>();
		for (LivingEntity victim : victims) {
			hit.add(victim.getUUID());
			DMZEvent.DamageModifyEvent modifyEvent = new DMZEvent.DamageModifyEvent(
					player, victim, active.totalDamage(), 0.0, DMZEvent.DamageSourceType.STRIKE);
			double damage = MinecraftForge.EVENT_BUS.post(modifyEvent) ? 0.0 : Math.max(0.0, modifyEvent.getAmount());
			applyStrikeDamage(player, victim, damage, active.techniqueId(), true);
			grantKillXpIfNeeded(player, victim, active.techniqueId());
		}
		SLAM_RINGS.put(player.getUUID(), new SlamRing(impact, hit));

		level.playSound(null, impact.x, impact.y, impact.z,
				MainSounds.UNBLOCK.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, SLAM_UNBLOCK_PITCH);
		level.playSound(null, impact.x, impact.y, impact.z,
				MainSounds.PARRY.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.6F, SLAM_PARRY_PITCH);
		NetworkHandler.sendToTrackingEntityAndSelf(new KiBurstVfxS2C(player.getId(), false, SLAM_SHAKE_RADIUS), player);
		spawnSlamRingDust(level, impact, Math.max(area.getXsize(), area.getZsize()) * 0.5);
		spawnSlamDebris(level, area, impact);
	}

	private static void expandSlamRing(ServerPlayer player, ActiveStrike active, int ringTick) {
		SlamRing ring = SLAM_RINGS.get(player.getUUID());
		if (ring == null || !(player.level() instanceof ServerLevel level)) return;

		Vec3 center = ring.center();
		double radius = SLAM_RING_MAX_RADIUS * ringTick / SLAM_RING_TICKS;
		double innerRadius = SLAM_RING_MAX_RADIUS * (ringTick - 1) / SLAM_RING_TICKS;
		spawnSlamRingDust(level, center, radius);

		AABB reach = new AABB(center.x - radius - 1.0, center.y - SLAM_RING_HEIGHT, center.z - radius - 1.0,
				center.x + radius + 1.0, center.y + SLAM_RING_HEIGHT, center.z + radius + 1.0);

		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, reach,
				e -> e != player && e.isAlive() && !e.isSpectator() && !ring.hit().contains(e.getUUID())
						&& TargetHelper.canAttack(player, e, SLAM_RING_MAX_RADIUS + CONE_RANGE));

		for (LivingEntity victim : candidates) {
			double dx = victim.getX() - center.x;
			double dz = victim.getZ() - center.z;
			double distance = Math.sqrt(dx * dx + dz * dz);
			double halfWidth = victim.getBbWidth() * 0.5;
			if (distance + halfWidth < innerRadius || distance - halfWidth > radius) continue;

			ring.hit().add(victim.getUUID());
			double damage = strikeHitDamage(player, null, victim, active.totalDamage() * SLAM_RING_DAMAGE_RATIO);
			applyStrikeDamage(player, victim, damage, active.techniqueId(), true);
			grantKillXpIfNeeded(player, victim, active.techniqueId());

			Vec3 push = distance > 1.0E-3 ? new Vec3(dx / distance, 0.0, dz / distance) : Vec3.ZERO;
			KnockbackHelper.apply(victim, new Vec3(push.x * SLAM_RING_KNOCKBACK, 0.35, push.z * SLAM_RING_KNOCKBACK));
		}
	}

	private static void spawnSlamRingDust(ServerLevel level, Vec3 center, double radius) {
		int points = Math.min(SLAM_DUST_MAX_POINTS, Math.max(12, (int) Math.ceil(radius * SLAM_DUST_POINTS_PER_BLOCK)));
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0 * i / points;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			level.sendParticles(MainParticles.DUST.get(),
					center.x + cos * radius, center.y + 0.1, center.z + sin * radius,
					0, cos * 0.08, 0.02, sin * 0.08, 1.0);
		}
	}

	private static void spawnSlamDebris(ServerLevel level, AABB area, Vec3 impact) {
		AABB debrisArea = area.inflate(SLAM_DEBRIS_SPREAD, 0.0, SLAM_DEBRIS_SPREAD);
		int groundY = net.minecraft.util.Mth.floor(impact.y - 0.01);
		int minX = net.minecraft.util.Mth.floor(debrisArea.minX);
		int maxX = net.minecraft.util.Mth.floor(debrisArea.maxX);
		int minZ = net.minecraft.util.Mth.floor(debrisArea.minZ);
		int maxZ = net.minecraft.util.Mth.floor(debrisArea.maxZ);

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, groundY, z);
				net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
				if (state.isAir() || state.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) continue;

				net.minecraft.core.particles.BlockParticleOption option =
						new net.minecraft.core.particles.BlockParticleOption(MainParticles.FLYING_BLOCK.get(), state);

				for (int i = 0; i < SLAM_DEBRIS_PER_BLOCK; i++) {
					double px = x + level.random.nextDouble();
					double pz = z + level.random.nextDouble();
					double py = groundY + 1.05;

					Vec3 outward = new Vec3(px - impact.x, 0.0, pz - impact.z);
					if (outward.lengthSqr() < 1.0E-4) {
						double angle = level.random.nextDouble() * Math.PI * 2.0;
						outward = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
					}
					outward = outward.normalize();

					double speed = 0.15 + level.random.nextDouble() * 0.2;
					double lift = 0.35 + level.random.nextDouble() * 0.3;
					level.sendParticles(option, px, py, pz, 0, outward.x * speed, lift, outward.z * speed, 1.0);
				}
			}
		}
	}

	private static void endStrike(ServerPlayer player, LivingEntity target, ActiveStrike active) {
		endStrike(player, target, active, false);
	}

	private static void endStrike(ServerPlayer player, LivingEntity target, ActiveStrike active, boolean interrupted) {
		ACTIVE.remove(player.getUUID());
		STRIKE_ANCHOR_PART.remove(player.getUUID());
		SLAM_RINGS.remove(player.getUUID());
		setStrikeLocked(player, false);
		clearVictimStrikeLock(player, target, active.targetId());
		stopStrikeAnimation(player);
		stopVictimAnimation(player, target, active.targetId());
		stopStrikeHurtAnimation(player);
		FLINCH_SIDE.remove(player.getUUID());

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			String cooldownKey = getTechniqueCooldownKey(active.techniqueId());
			int cooldown = interrupted
					? Math.max(1, active.cooldownTicks() / 2)
					: active.cooldownTicks();
			stats.getCooldowns().setCooldown(cooldownKey, cooldown);
			applyGlobalStrikeCooldown(stats, 1.0f);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}

	private static void failPending(ServerPlayer player, PendingStrike pending) {
		PENDING.remove(player.getUUID());
		STRIKE_ANCHOR_PART.remove(player.getUUID());
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			String cooldownKey = getTechniqueCooldownKey(pending.techniqueId());
			int halfCooldown = Math.max(1, pending.cooldownTicks() / 2);
			stats.getCooldowns().setCooldown(cooldownKey, halfCooldown);
			applyGlobalStrikeCooldown(stats, 0.5f);
			stats.getResources().addEnergy((int) Math.ceil(pending.energyCost() * 0.4));
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		});
	}

	private static void applyGlobalStrikeCooldown(com.dragonminez.common.stats.StatsData stats, float ratio) {
		int ticks = Math.round(ConfigManager.getCombatConfig().getStrikeGlobalCooldownTicks() * ratio);
		if (ticks <= 0) return;
		stats.getCooldowns().setCooldown(Cooldowns.STRIKE_GLOBAL, ticks);
	}

	private static LivingEntity resolvePreferredTarget(ServerPlayer player, int targetId) {
		if (targetId <= 0) return null;
		if (!(player.level().getEntity(targetId) instanceof LivingEntity living)) return null;
		if (!living.isAlive()) return null;
		if (player.distanceTo(living) > CONNECT_RANGE) return null;
		if (!player.hasLineOfSight(living)) return null;
		if (!TargetHelper.canAttack(player, living, CONNECT_RANGE)) return null;
		return living;
	}

	private static LivingEntity resolveTargetForPending(ServerPlayer player, PendingStrike pending) {
		LivingEntity preferred = pending.preferredTargetId() != null ? resolveLiving(player, pending.preferredTargetId()) : null;
		if (preferred != null && player.distanceTo(preferred) <= CONNECT_RANGE && player.hasLineOfSight(preferred)
				&& TargetHelper.canAttack(player, preferred, CONNECT_RANGE)) return preferred;
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
				e -> e != player && e.isAlive() && e.isPickable() && TargetHelper.canAttack(player, e, range));

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
		if (closest == null) closest = nearestPartParentInSight(player, range);
		return Optional.ofNullable(closest);
	}

	private static LivingEntity nearestPartParentInSight(ServerPlayer player, double range) {
		PartEntity<?> part = nearestPartInSight(player, range);
		return part != null && part.getParent() instanceof LivingEntity parent ? parent : null;
	}

	private static PartEntity<?> nearestPartInSight(ServerPlayer player, double range) {
		if (!(player.level() instanceof ServerLevel level)) return null;
		Vec3 eyePos = player.getEyePosition();
		Vec3 endPos = eyePos.add(player.getViewVector(1.0F).scale(range));

		PartEntity<?> best = null;
		double bestDist = range * range;
		for (PartEntity<?> part : level.getPartEntities()) {
			if (!(part.getParent() instanceof LivingEntity parent) || !parent.isAlive()) continue;
			if (!TargetHelper.canAttack(player, parent, range)) continue;
			if (player.distanceTo(part) > range + 8.0) continue;
			if (!player.hasLineOfSight(part)) continue;

			AABB box = part.getBoundingBox().inflate(part.getPickRadius());
			if (box.contains(eyePos)) return part;

			Optional<Vec3> hit = box.clip(eyePos, endPos);
			if (hit.isPresent()) {
				double dist = eyePos.distanceToSqr(hit.get());
				if (dist < bestDist) {
					best = part;
					bestDist = dist;
				}
			}
		}
		return best;
	}

	private static void teleportToPartFront(ServerPlayer player, PartEntity<?> part, LivingEntity parent, boolean faceTarget) {
		Vec3 center = part.getBoundingBox().getCenter();
		Vec3 look = parent.getLookAngle();
		if (look.horizontalDistanceSqr() < 1.0E-6) look = player.getLookAngle();

		double distance = 1.3 + part.getBbWidth() * 0.5;
		Vec3 teleportPos = center.subtract(look.scale(distance));
		player.teleportTo(teleportPos.x, center.y - player.getEyeHeight(), teleportPos.z);
		if (faceTarget) player.lookAt(EntityAnchorArgument.Anchor.EYES, center);
	}

	private static void dashForward(ServerPlayer player, boolean isFlying) {
		double speedMultiplier = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1;
		double distance = DASH_BASE_DISTANCE * speedMultiplier;
		if (isFlying) distance *= 3.0;
		Vec3 direction = Vec3.directionFromRotation(0, player.getYRot()).normalize();
		Vec3 velocity = direction.scale(distance * DASH_DISTANCE_SCALE);
		double yVel = player.onGround() ? 0.35 : 0.2;
		player.setDeltaMovement(player.getDeltaMovement().add(velocity.x, yVel, velocity.z));
		player.hurtMarked = true;
	}

	private static LivingEntity findConeTarget(ServerPlayer player, double range, int preferredTargetId) {
		if (preferredTargetId > 0) {
			LivingEntity pref = TargetHelper.resolveHittable(TargetHelper.getEntityOrPart(player.level(), preferredTargetId)) instanceof LivingEntity l ? l : null;
			if (pref != null && pref.isAlive() && player.distanceTo(pref) <= range
					&& isInFrontCone(player, pref) && player.hasLineOfSight(pref)
					&& TargetHelper.canAttack(player, pref, range)) {
				return pref;
			}
		}

		AABB searchBox = player.getBoundingBox().inflate(range);
		List<LivingEntity> candidates = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
				e -> e != player && e.isAlive() && e.isPickable()
						&& TargetHelper.canAttack(player, e, range)
						&& player.distanceTo(e) <= range
						&& isInFrontCone(player, e)
						&& player.hasLineOfSight(e)));

		if (candidates.isEmpty()) return nearestPartParentInSight(player, range);
		if (candidates.size() == 1) return candidates.get(0);

		RecentHit recent = RECENTLY_DAMAGED.get(player.getUUID());
		if (recent != null && (System.currentTimeMillis() - recent.timestamp()) <= RECENT_HIT_WINDOW_MS) {
			for (LivingEntity e : candidates) {
				if (e.getUUID().equals(recent.targetId())) return e;
			}
		}

		Vec3 eyePos = player.getEyePosition();
		Vec3 viewVec = player.getViewVector(1.0F);
		Vec3 endPos = eyePos.add(viewVec.scale(range));

		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity e : candidates) {
			AABB bb = e.getBoundingBox().inflate(e.getPickRadius());
			Optional<Vec3> hit = bb.clip(eyePos, endPos);
			double dist;
			if (bb.contains(eyePos)) {
				dist = 0.0;
			} else if (hit.isPresent()) {
				dist = eyePos.distanceToSqr(hit.get());
			} else {
				dist = eyePos.distanceToSqr(e.getEyePosition());
			}
			if (dist < bestDist) {
				best = e;
				bestDist = dist;
			}
		}
		return best;
	}

	private static boolean isInFrontCone(ServerPlayer player, LivingEntity target) {
		Vec3 look = player.getLookAngle();
		Vec3 toTarget = target.getEyePosition().subtract(player.getEyePosition()).normalize();
		return look.dot(toTarget) >= CONE_HALF_ANGLE_COS;
	}

	private static void teleportToTargetFront(ServerPlayer player, LivingEntity target, boolean faceTarget) {
		Vec3 targetPos = target.position();
		Vec3 targetLook = target.getLookAngle();
		Vec3 teleportPos = targetPos.subtract(targetLook.scale(1.3));

		player.teleportTo(teleportPos.x, targetPos.y, teleportPos.z);

		if (faceTarget) player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

	}

	private static void applyStrikeDamage(ServerPlayer player, LivingEntity target, double damage, String techniqueId, boolean isFinalHit) {
		if (damage <= 0) return;
		if (!isFinalHit) if (target.getHealth() - damage <= 1.0F) damage = Math.max(0.01F, target.getHealth() - 1.0F);

		playStrikeHitAnimation(player, target, techniqueId);
		spawnHitShockwave(player, target, techniqueId, isFinalHit);
		target.hurt(MainDamageTypes.strikeAttack(player.level(), player, techniqueId), (float) damage);
		RECENTLY_DAMAGED.put(player.getUUID(), new RecentHit(target.getUUID(), System.currentTimeMillis()));

		double finalDamage = damage;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techniqueId);
			if (tech instanceof StrikeAttackData strike) {
				int xpGain = RacialStatUtil.applyTechniqueXpBonus(stats, strike.getXpGainPerHit());
				if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(techniqueId, xpGain);
			}
			DynamicGrowthService.markCombat(stats);
			DynamicGrowthService.awardStrike(player, stats, target, finalDamage);
		});
	}

	private static void spawnHitShockwave(ServerPlayer player, LivingEntity target, String techniqueId, boolean isFinalHit) {
		if (OOZARU_SLAM_ID.equals(techniqueId)) return;

		float sizeFactor = Math.max(1.0F, target.getBbHeight() / 1.8F);
		float scale = (isFinalHit ? SHOCKWAVE_FINAL_SCALE : SHOCKWAVE_HIT_SCALE) * sizeFactor;
		int lifetime = isFinalHit ? SHOCKWAVE_FINAL_TICKS : SHOCKWAVE_HIT_TICKS;

		Vec3 center = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
		Vec3 toAttacker = new Vec3(player.getX() - target.getX(), 0.0, player.getZ() - target.getZ());
		if (toAttacker.lengthSqr() > 1.0E-4) center = center.add(toAttacker.normalize().scale(target.getBbWidth() * 0.5));

		NetworkHandler.sendToTrackingEntityAndSelf(
				new ShockwaveVfxS2C(center.x, center.y, center.z, scale, shockwaveColor(techniqueId), lifetime), player);
	}

	private static int shockwaveColor(String techniqueId) {
		return switch (techniqueId) {
			case "wolf_fang" -> 0x4D9EFF;
			case "deadly_dance" -> 0xFFE23A;
			case "deadly_dance_vegetto" -> DEADLY_DANCE_VEGETTO_COLOR_SECONDARY;
			case "super_god_fist" -> 0xF5C527;
			case "kaioken_attack" -> 0xFF5A3A;
			case SPIRIT_CANNON_ID -> SPIRIT_CANNON_COLOR_SECONDARY;
			case "gum_punch" -> 0xFF82F3;
			default -> 0xFFF3D6;
		};
	}

	private static void grantKillXpIfNeeded(ServerPlayer player, LivingEntity target, String techniqueId) {
		if (target == null || target.isAlive()) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techniqueId);
			if (tech instanceof StrikeAttackData strike) {
				int xpGain = RacialStatUtil.applyTechniqueXpBonus(stats, strike.getXpGainPerKill());
				if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(techniqueId, xpGain);
			}
		});
	}

	private static void applyKnockback(ServerPlayer player, LivingEntity target, double totalDamage) {
		Vec3 dir = target.position().subtract(player.position()).normalize();
		if (dir.lengthSqr() < 1.0E-6) dir = player.getLookAngle();
		KnockbackHelper.apply(target, dir.scale(KNOCKBACK_FORCE));
		playStrikeKnockbackAnimation(target);

		MomentumImpactHandler.CollisionImpactType impactType = target.onGround() || dir.y < -0.5
				? MomentumImpactHandler.CollisionImpactType.GROUND
				: MomentumImpactHandler.CollisionImpactType.WALL;
		MomentumImpactHandler.registerCollisionImpact(target, impactType, (float) (totalDamage * IMPACT_DAMAGE_RATIO), dir);
	}

	private static int deadlyDanceSecondaryColor(boolean vegetto) {
		return vegetto ? DEADLY_DANCE_VEGETTO_COLOR_SECONDARY : DEADLY_DANCE_COLOR_SECONDARY;
	}

	private static boolean canOccupy(LivingEntity entity, double x, double y, double z) {
		AABB moved = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
		return entity.level().noCollision(entity, moved);
	}

	private static void spawnWolfFangJab(ServerPlayer player, Vec3 point, int beat) {
		if (!(player.level() instanceof ServerLevel level)) return;

		double x = point.x;
		double y = point.y;
		double z = point.z;

		SoundEvent[] punches = {
				MainSounds.GOLPE1.get(), MainSounds.GOLPE2.get(), MainSounds.GOLPE3.get(),
				MainSounds.GOLPE4.get(), MainSounds.GOLPE5.get(), MainSounds.GOLPE6.get()
		};
        SoundEvent punch = punches[Math.floorMod(beat / 4, punches.length)];
		level.playSound(null, x, y, z, punch, net.minecraft.sounds.SoundSource.PLAYERS,
				1.0F, 1.1F + (level.random.nextFloat() * 0.3F));

		NetworkHandler.sendToTrackingEntityAndSelf(new ClawSlashVfxS2C(x, y, z,
				WOLF_FANG_JAB_CLAW_SCALE, WOLF_FANG_CLAW_COLOR, WOLF_FANG_JAB_CLAW_TICKS, 1, false), player);
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

	private static void faceStrikeTarget(ServerPlayer player, LivingEntity target) {
		Integer partId = STRIKE_ANCHOR_PART.get(player.getUUID());
		if (partId != null) {
			var anchor = TargetHelper.getEntityOrPart(player.level(), partId);
			if (anchor instanceof PartEntity<?> part && part.getParent() == target) {
				player.lookAt(EntityAnchorArgument.Anchor.EYES, part.getBoundingBox().getCenter());
				player.setYHeadRot(player.getYRot());
				return;
			}
		}
		faceEntity(player, target);
	}

	private static void spiritCannonHit(ServerPlayer player, LivingEntity target, double damage, boolean finalHit,
			double heightRatio, float burstScale, SoundEvent sound, float pitch) {
		target.invulnerableTime = 0;
		applyStrikeDamage(player, target, damage, SPIRIT_CANNON_ID, finalHit);

		Vec3 impactPos = new Vec3(target.getX(), target.getY() + target.getBbHeight() * heightRatio, target.getZ());
		player.level().playSound(null, impactPos.x, impactPos.y, impactPos.z, sound, SoundSource.PLAYERS, 2.0F, pitch);
		NetworkHandler.sendToTrackingEntityAndSelf(new ImpactBurstVfxS2C(impactPos, player.getLookAngle(),
				burstScale, SPIRIT_CANNON_COLOR_PRIMARY, SPIRIT_CANNON_COLOR_SECONDARY, false, SPIRIT_CANNON_BURST_TICKS), player);
	}

	private static void playStrikeHitAnimation(ServerPlayer player, LivingEntity target, String techniqueId) {
		if (POSE_DRIVEN_STRIKES.contains(techniqueId)) return;
		if (!(target instanceof ServerPlayer serverPlayer)) return;

		String anim = STRIKE_HIT_ANIM;

		if (FLINCH_STRIKES.contains(techniqueId)) {
			boolean fromLeft = !Boolean.TRUE.equals(FLINCH_SIDE.get(player.getUUID()));
			FLINCH_SIDE.put(player.getUUID(), fromLeft);
			anim = fromLeft ? HURT_LEFT_ANIM : HURT_RIGHT_ANIM;
			STRIKE_HURT_VICTIM.put(player.getUUID(), new HurtPose(serverPlayer.getId(), null));
		}

		NetworkHandler.sendToTrackingEntityAndSelf(
				new TriggerAnimationS2C(serverPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, anim),
				serverPlayer
		);
	}

	private static void playVictimHurtPose(ServerPlayer player, LivingEntity victim, String playerAnim, String sagaAnim) {
		if (victim == null || victim.isDeadOrDying()) return;

		if (victim instanceof ServerPlayer victimPlayer) {
			NetworkHandler.sendToTrackingEntityAndSelf(
					new TriggerAnimationS2C(victimPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 0, -1, playerAnim),
					victimPlayer
			);
		} else if (victim instanceof DBSagasEntity saga) {
			saga.triggerAnim(DBSagasEntity.HURT_CONTROLLER, sagaAnim);
		} else {
			return;
		}

		STRIKE_HURT_VICTIM.put(player.getUUID(), new HurtPose(victim.getId(), sagaAnim));
	}

	private static void stopStrikeHurtAnimation(ServerPlayer player) {
		HurtPose pose = STRIKE_HURT_VICTIM.remove(player.getUUID());
		if (pose == null) return;

		if (!(player.level().getEntity(pose.victimId()) instanceof LivingEntity victim)) return;

		if (victim instanceof ServerPlayer victimPlayer) {
			NetworkHandler.sendToTrackingEntityAndSelf(
					new TriggerAnimationS2C(victimPlayer.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""),
					victimPlayer
			);
		} else if (victim instanceof DBSagasEntity saga && pose.sagaAnim() != null) {
			saga.stopTriggeredAnimation(DBSagasEntity.HURT_CONTROLLER, pose.sagaAnim());
		}
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

	private record HurtPose(int victimId, String sagaAnim) {}

	private record RecentHit(UUID targetId, long timestamp) {}

	private record SlamRing(Vec3 center, java.util.Set<UUID> hit) {}
}