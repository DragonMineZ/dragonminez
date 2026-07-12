package com.dragonminez.server.events.players.combat;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.ki.KiExplosionVisualEntity;
import com.dragonminez.common.init.entities.ki.KiWaveEntity;
import com.dragonminez.common.init.entities.ki.OzaruFistEntity;
import com.dragonminez.common.init.entities.ki.SPDragonFistEntity;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

	private static final Map<UUID, PendingStrike> PENDING = new HashMap<>();
	private static final Map<UUID, ActiveStrike> ACTIVE = new HashMap<>();
	private static final Map<UUID, RecentHit> RECENTLY_DAMAGED = new HashMap<>();
	// When a strike locks onto a multipart giant, this remembers which hitbox part the player is
	// fighting so the whole strike faces/aims at that part instead of the giant's feet.
	private static final Map<UUID, Integer> STRIKE_ANCHOR_PART = new HashMap<>();

	public static void requestStrike(ServerPlayer player, int preferredTargetId) {
		if (player.level().isClientSide) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			if (!stats.getStatus().isHasCreatedCharacter()) return;
			if (stats.getStatus().isStunned()) return;
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

			if (immediateTarget != null) {
				// dragon_fist must not rotate the player's view toward the enemy.
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
			} else {
				dashForward(player, isFlying);
				PENDING.put(player.getUUID(), pending);
			}
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
		STRIKE_ANCHOR_PART.remove(id);
		ActiveStrike active = ACTIVE.remove(id);
		if (active != null && event.getEntity() instanceof ServerPlayer attacker) {
			clearVictimStrikeLock(attacker, null, active.targetId());
			stopVictimAnimation(attacker, null, active.targetId());
		}
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

		LivingEntity target = resolveLiving(player, active.targetId());

		if (target == null || !target.isAlive() || !player.isAlive()) {
			endStrike(player, target, active);
			return;
		}

        if ("dragon_fist".equals(active.techniqueId())) {

            if (active.ticksElapsed() == 5) {
                SPDragonFistEntity dragonFist = new SPDragonFistEntity(player.level(), player);
                dragonFist.setupDragonFist(player, (float) active.totalDamage(), 1.0f);

                try {
                    dragonFist.setStrikeStun(active.durationTicks() / 2, active.targetId());
                } catch (Exception e) {
                }
            }

            if (active.ticksElapsed() >= active.durationTicks()) {

                if (!player.level().isClientSide) {
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
                kamehameha.setBlockDestructionEnabled(false); // cosmetic blast — must not grief terrain

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
				target.setDeltaMovement(pushDir.x * 2.5, 0.4, pushDir.z * 2.5);
				target.hurtMarked = true;

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

		if ("super_god_fist".equals(active.techniqueId())) {

			int nextTick = active.ticksElapsed() + 1;

			if (nextTick < 14) {
				faceStrikeTarget(player, target);
				if (target instanceof ServerPlayer targetPlayer) {
					faceEntity(targetPlayer, player);
				} else {
					faceEntity(target, player);
				}
			}

			player.invulnerableTime = 20;

			if (nextTick <= 12) {
				target.invulnerableTime = 20;

				double dist = player.distanceTo(target);
				if (dist > 1.5) {
					Vec3 dir = target.position().subtract(player.position()).normalize();
					double dashSpeed = 1.5;
					player.setDeltaMovement(dir.x * dashSpeed, player.getDeltaMovement().y, dir.z * dashSpeed);
					player.hurtMarked = true;
				} else {
					freezeEntity(player);
				}
				freezeEntity(target);
			}
			else if (nextTick == 13) {
				target.invulnerableTime = 20;
				freezeEntity(player);
				freezeEntity(target);
			}
			else if (nextTick == 14) {
				target.invulnerableTime = 0;

				applyStrikeDamage(player, target, active.totalDamage(), active.techniqueId(), true);
				grantKillXpIfNeeded(player, target, active.techniqueId());

				player.level().playSound(
						null, target.getX(), target.getY(), target.getZ(),
						MainSounds.CRITICO2.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						2.5F,
						0.7F
				);

				if (!player.level().isClientSide) {
					try {
						KiExplosionVisualEntity impact = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), player.level());
						impact.setPos(target.getX(), target.getY() + (target.getBbHeight() * 0.5), target.getZ());
						impact.setupExplosion(0xFFFFFF, 0xF5C527, 0.5F);
						player.level().addFreshEntity(impact);
					} catch (Exception e) {
					}
					spawnSuperGodFistImpactParticles(player.serverLevel(), target);
				}

				Vec3 pushDir = player.getLookAngle().normalize();
				double knockbackPower = 4.0;
				target.setDeltaMovement(pushDir.x * knockbackPower, 0.6, pushDir.z * knockbackPower);
				target.hurtMarked = true;

				playStrikeKnockbackAnimation(target);

				MomentumImpactHandler.CollisionImpactType impactType = target.onGround() || pushDir.y < -0.5
						? MomentumImpactHandler.CollisionImpactType.GROUND
						: MomentumImpactHandler.CollisionImpactType.WALL;
				MomentumImpactHandler.registerCollisionImpact(target, impactType, (float) (active.totalDamage() * IMPACT_DAMAGE_RATIO), pushDir);

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

			if (nextTick % active.hitIntervalTicks() == 0 && nextTick < 30) {
				applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId(), false);

				player.level().playSound(
						null, target.getX(), target.getY(), target.getZ(),
						MainSounds.GOLPE1.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						1.0F,
						0.8F + (player.getRandom().nextFloat() * 0.4F)
				);
			}

			if (nextTick >= 30) {
				applyStrikeDamage(player, target, active.finalDamage(), active.techniqueId(), true);
				grantKillXpIfNeeded(player, target, active.techniqueId());

				player.level().playSound(
						null, target.getX(), target.getY(), target.getZ(),
						MainSounds.CRITICO2.get(),
						net.minecraft.sounds.SoundSource.PLAYERS,
						2.0F,
						1.0F
				);

				Vec3 pushDir = player.getLookAngle().normalize();
				double upwardForce = 1.5;
				double forwardForce = 0.5;

				target.setDeltaMovement(pushDir.x * forwardForce, upwardForce, pushDir.z * forwardForce);
				target.hurtMarked = true;

				playStrikeKnockbackAnimation(target);

				MomentumImpactHandler.registerCollisionImpact(target, MomentumImpactHandler.CollisionImpactType.GROUND, (float) (active.totalDamage() * IMPACT_DAMAGE_RATIO), new Vec3(0, 1, 0));

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
				target.setDeltaMovement(pushDir.x * 1.5, 0.4, pushDir.z * 1.5);
				target.hurtMarked = true;
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
				target.setDeltaMovement(pushDir.x * 3.5, 0.2, pushDir.z * 3.5);
				target.hurtMarked = true;
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
			freezeEntity(target);
			faceStrikeTarget(player, target);
			if (target instanceof ServerPlayer targetPlayer) {
				faceEntity(targetPlayer, player);
			}
			player.invulnerableTime = 20;

			int wolfTick = active.ticksElapsed() + 1;
			int wolfDuration = active.durationTicks();

			if (wolfTick % active.hitIntervalTicks() == 0 && wolfTick < wolfDuration) {
				applyStrikeDamage(player, target, active.perHitDamage(), active.techniqueId(), false);
			}

			if (wolfTick < wolfDuration - 3 && wolfTick % 4 == 0) {
				spawnWolfFangJab(player, target, wolfTick);
			}

			if (wolfTick >= wolfDuration) {
				applyStrikeDamage(player, target, active.finalDamage(), active.techniqueId(), true);
				grantKillXpIfNeeded(player, target, active.techniqueId());

				double sx = target.getX();
				double sy = target.getY() + target.getBbHeight() * 0.5;
				double sz = target.getZ();

				// Finishing blow: heavy punch impact + explosion boom + a full-throated roar.
				player.level().playSound(null, sx, sy, sz,
						MainSounds.CRITICO2.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.7F);
				player.level().playSound(null, sx, sy, sz,
						MainSounds.KI_EXPLOSION_IMPACT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2.5F, 1.0F);
				player.level().playSound(null, sx, sy, sz,
						MainSounds.OOZARU_GROWL_PLAYER.get(), net.minecraft.sounds.SoundSource.PLAYERS, 3.0F, 1.15F);

				if (!player.level().isClientSide) {
					spawnWolfFangFinalParticles(player.serverLevel(), target);
				}

				applyKnockback(player, target, active.totalDamage());
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

			// dragon_fist must not rotate the attacker's view toward the enemy.
			if (!"dragon_fist".equals(pending.techniqueId())) {
				faceStrikeTarget(player, target);
			}
			if (target instanceof ServerPlayer targetPlayer) {
				faceEntity(targetPlayer, player);
			}
			playStrikeAnimation(player, pending.animationId());
		});
	}

	private static void endStrike(ServerPlayer player, LivingEntity target, ActiveStrike active) {
		ACTIVE.remove(player.getUUID());
		STRIKE_ANCHOR_PART.remove(player.getUUID());
		setStrikeLocked(player, false);
		clearVictimStrikeLock(player, target, active.targetId());
		stopStrikeAnimation(player);
		stopVictimAnimation(player, target, active.targetId());

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			String cooldownKey = getTechniqueCooldownKey(active.techniqueId());
			stats.getCooldowns().setCooldown(cooldownKey, active.cooldownTicks());
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

		playStrikeHitAnimation(target);
		target.hurt(MainDamageTypes.strikeAttack(player.level(), player, techniqueId), (float) damage);
		RECENTLY_DAMAGED.put(player.getUUID(), new RecentHit(target.getUUID(), System.currentTimeMillis()));

		double finalDamage = damage;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techniqueId);
			if (tech instanceof StrikeAttackData strike) {
				int xpGain = strike.getXpGainPerHit();
				if (xpGain > 0) stats.getTechniques().addExperienceToTechnique(techniqueId, xpGain);
			}
			DynamicGrowthService.markCombat(stats);
			DynamicGrowthService.awardStrike(player, stats, target, finalDamage);
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

	/**
	 * Xenoverse-style impact burst for super_god_fist's connecting blow. All cosmetic, server-driven so every
	 * nearby client sees it. Mixes the mod's themed punch/spark particles (colour is packed into the delta args,
	 * count 0 = one particle per call) with a vanilla crit + explosion burst so the hit reads at any distance.
	 */
	private static void spawnSuperGodFistImpactParticles(ServerLevel level, LivingEntity target) {
		double x = target.getX();
		double y = target.getY() + target.getBbHeight() * 0.5;
		double z = target.getZ();

		// Themed punch flash (white) — the same particle NPCs throw on a landed hit.
		level.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, 1.0, 1.0, 1.0, 1.0);

		// Golden sparks scattering off the impact point, matching the flash's border colour (0xF5C527).
		for (int i = 0; i < 8; i++) {
			double ox = (level.random.nextDouble() - 0.5) * 0.8;
			double oy = (level.random.nextDouble() - 0.5) * 0.8;
			double oz = (level.random.nextDouble() - 0.5) * 0.8;
			level.sendParticles(MainParticles.SPARKS.get(), x + ox, y + oy, z + oz, 0, 0.96, 0.77, 0.15, 1.0);
		}

		// Vanilla crit spread + a small explosion puff for a punchy, instantly-readable impact.
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, x, y, z, 18, 0.4, 0.4, 0.4, 0.6);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, x, y, z, 2, 0.15, 0.15, 0.15, 0.0);
	}

	private static void spawnWolfFangJab(ServerPlayer player, LivingEntity target, int beat) {
		if (!(player.level() instanceof ServerLevel level)) return;

		double x = target.getX();
		double y = target.getY() + target.getBbHeight() * 0.6;
		double z = target.getZ();

		net.minecraft.sounds.SoundEvent[] punches = {
				MainSounds.GOLPE1.get(), MainSounds.GOLPE2.get(), MainSounds.GOLPE3.get(),
				MainSounds.GOLPE4.get(), MainSounds.GOLPE5.get(), MainSounds.GOLPE6.get()
		};
		net.minecraft.sounds.SoundEvent punch = punches[Math.floorMod(beat / 4, punches.length)];
		level.playSound(null, x, y, z, punch, net.minecraft.sounds.SoundSource.PLAYERS,
				1.0F, 1.1F + (level.random.nextFloat() * 0.3F));

		level.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, 0.30, 0.62, 1.0, 1.0);

		for (int i = 0; i < 4; i++) {
			double ox = (level.random.nextDouble() - 0.5) * 0.7;
			double oy = (level.random.nextDouble() - 0.5) * 0.7;
			double oz = (level.random.nextDouble() - 0.5) * 0.7;
			level.sendParticles(MainParticles.SPARKS.get(), x + ox, y + oy, z + oz, 0, 0.25, 0.55, 1.0, 1.0);
		}

		level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, x, y, z, 6, 0.3, 0.3, 0.3, 0.5);
	}

	private static void spawnWolfFangFinalParticles(ServerLevel level, LivingEntity target) {
		double x = target.getX();
		double y = target.getY() + target.getBbHeight() * 0.5;
		double z = target.getZ();

		level.sendParticles(MainParticles.PUNCH_PARTICLE.get(), x, y, z, 0, 0.30, 0.62, 1.0, 1.0);

		for (int i = 0; i < 90; i++) {
			double dirX = level.random.nextDouble() - 0.5;
			double dirY = level.random.nextDouble() - 0.5;
			double dirZ = level.random.nextDouble() - 0.5;
			double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
			if (len < 1.0E-4) continue;
			double radius = 0.5 + level.random.nextDouble() * 2.5;
			double px = x + (dirX / len) * radius;
			double py = y + (dirY / len) * radius;
			double pz = z + (dirZ / len) * radius;
			level.sendParticles(MainParticles.SPARKS.get(), px, py, pz, 0, 0.25, 0.55, 1.0, 1.0);
		}

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

	/** Faces the attacker at the locked multipart hitbox (its center) when there is one, else at the target. */
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

	private record RecentHit(UUID targetId, long timestamp) {}
}