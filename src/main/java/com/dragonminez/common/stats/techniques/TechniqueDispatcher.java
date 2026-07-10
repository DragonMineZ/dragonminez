package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TechniqueDispatcher {

	public static boolean executeKiAttack(LivingEntity owner, Level level, KiAttackData data, StatsData statsData, float chargeMultiplier) {
		if (level.isClientSide) return false;

		boolean isInitialSpawn = chargeMultiplier < 0.5f;
		float clampedCharge = Mth.clamp(chargeMultiplier, 0.5f, 2.0f);
		float damageCharge = (isInitialSpawn && data.isInstantCast()) ? 1.0f : clampedCharge;

		float realDamage = (float) (statsData.getKiDamage() * data.getDamageMultiplier() * data.getConfiguredDamageMultiplier() * damageCharge * data.getOutputMultiplier());
		int maxLife = resolvePlayerMaxLifeTicks(data, clampedCharge);
		boolean isHeal = (data.getEffectiveUtility() == KiAttackData.Utility.HEAL);
		int kiTypeOrdinal = data.getKiType().ordinal();

		LivingEntity homingTarget = resolveHomingTarget(owner, level, data, statsData);
		int homingTargetId = homingTarget != null ? homingTarget.getId() : -1;
		if (homingTarget != null && owner instanceof Player) statsData.getTechniques().setHomingTargetId(homingTargetId);

        if (!isInitialSpawn) {
            List<AbstractKiProjectile> activeKis = getChargingKiEntities(owner, level);

            if (!activeKis.isEmpty()) {
                for (AbstractKiProjectile activeKi : activeKis) {
                    if (activeKi instanceof KiWaveEntity wave) {
                        wave.setKiDamage(realDamage);
                        wave.fireHability(maxLife);
                    } else if (activeKi instanceof KiBlastEntity blast) {
                        blast.setKiDamage(realDamage);
                        blast.fireHability(maxLife);
                        int renderType = blast.getKiRenderType();
                        if (renderType != 2 && renderType != 5 && renderType != 6 && renderType != 7) {
                            Vec3 lookBlast = owner.getLookAngle();
                            blast.setDeltaMovement(lookBlast.scale(data.getSpeed()));
                        }
                    } else if (activeKi instanceof KiLaserEntity laser) {
                        laser.setKiDamage(realDamage);
                        laser.fireHability(maxLife);
                    } else if (activeKi instanceof KiDiskEntity disk) {
                        disk.setKiDamage(realDamage);
                        disk.fireHability(maxLife);
                    } else if (activeKi instanceof KiExplosionEntity explosion) {
                        explosion.setKiDamage(realDamage);
                        explosion.fireHability(maxLife);
                    } else if (activeKi instanceof KiBarrierEntity barrier) {
                        barrier.setKiDamage(realDamage);
                        barrier.fireHability(maxLife);
                    } else if (activeKi instanceof KiAreaEntity area) {
                        area.setKiDamage(realDamage);
                        area.fireHability(maxLife);
                    }
                }
                return true;
            }
        }

        switch (data.getKiType()) {
            case SMALL_BALL:
                if (!isInitialSpawn) return true;

                KiBlastEntity smallBall = new KiBlastEntity(level, owner);

                smallBall.setOwner(owner);
                smallBall.setKiType(kiTypeOrdinal);
                smallBall.setKiRenderType(0);
                smallBall.setSize(data.getSize());
                smallBall.setKiSpeed(data.getSpeed());
                smallBall.setKiDamage(realDamage);
                smallBall.setColors(data.getColorInterior(), data.getColorExterior(), data.getColorOutline());
                smallBall.setCastTime(0);
                smallBall.setMaxLife(maxLife);
                smallBall.setTechniqueId(data.getId());
                smallBall.setArmorPenetration(data.getArmorPenetration());
                smallBall.setHeal(isHeal);
                smallBall.setHomingTarget(homingTargetId);
                smallBall.setFiring(true);

                Vec3 lookSmall = owner.getLookAngle();
                Vec3 spawnPos = owner.getEyePosition().add(lookSmall.scale(0.5D));
                smallBall.setPos(spawnPos.x, spawnPos.y - 0.2D, spawnPos.z);
                smallBall.setDeltaMovement(lookSmall.scale(data.getSpeed()));
                smallBall.setYRot(owner.getYRot());
                smallBall.setXRot(owner.getXRot());

                if (!level.isClientSide) {
                    smallBall.playInitialSound(com.dragonminez.common.init.MainSounds.KIBLAST_ATTACK.get());
                    level.addFreshEntity(smallBall);
                }
                break;
            case MEDIUM_BALL:
                KiBlastEntity medBall = new KiBlastEntity(level, owner);
                if ("soul_punisher".equals(data.getId())) {
                    medBall.setupSoulPunisherPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorOutline(), data.getSize());
                } else if ("fake_moon".equals(data.getId())) {
                    medBall.setupFakeMoonPlayer(owner, data.getSpeed(), data.getColorInterior(), data.getColorOutline(), data.getSize());
                } else if ("sokidan".equals(data.getId())) {
                    medBall.setupSokidanPlayer(owner, realDamage, data.getSpeed(), 0xF7F723, 0xF7B736, data.getSize());
                    medBall.setColors(0xFCFC5D, 0xF7F723, 0xF7B736);
                } else {
                    medBall.setupKiBlastPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize());
                }
                medBall.setColorOutline("sokidan".equals(data.getId()) ? 0xF7B736 : data.getColorOutline());
                medBall.setKiType(kiTypeOrdinal);
                medBall.setTechniqueId(data.getId());
                medBall.setArmorPenetration(data.getArmorPenetration());
                medBall.setHeal(isHeal);
                medBall.setHomingTarget(homingTargetId);

                if (!level.isClientSide) level.addFreshEntity(medBall);
                break;
            case GIANT_BALL:
                KiBlastEntity giantBall = new KiBlastEntity(level, owner);
                if ("spiritbomb".equals(data.getId())) {
                    giantBall.setupKiGenkiPlayer(owner, realDamage, data.getSpeed());
                } else if ("supernova".equals(data.getId())) {
                    giantBall.setupKiNovaPlayer(owner, realDamage, data.getSpeed());
                } else if ("supernova_cooler".equals(data.getId())) {
                    giantBall.setupKiNovaCoolerPlayer(owner, realDamage, data.getSpeed());
                } else if ("death_ball".equals(data.getId())) {
                    giantBall.setupKiDeathBallPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior());
                    giantBall.setColorOutline(data.getColorOutline());
                } else {
                    giantBall.setupKiLargeBlastPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize());
                    giantBall.setColorOutline(data.getColorOutline());
                }
                giantBall.setKiType(kiTypeOrdinal);
                giantBall.setTechniqueId(data.getId());
                giantBall.setArmorPenetration(data.getArmorPenetration());
                giantBall.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(giantBall);
                break;
            case WAVE:
                KiWaveEntity wave = new KiWaveEntity(level, owner);
                if ("kamehameha".equals(data.getId())) {
                    wave.setupKiHamePlayer(owner, realDamage, data.getSpeed(), data.getSize());
                } else if ("galick_gun".equals(data.getId())) {
                    wave.setupKiGalickGunPlayer(owner, realDamage, data.getSpeed(), data.getSize());
                } else if ("final_flash".equals(data.getId())) {
                    wave.setupFinalFlashPlayer(owner, realDamage, data.getSpeed(), data.getSize());
                } else if ("masenko".equals(data.getId())) {
                    wave.setupKiMasenkoPlayer(owner, realDamage, data.getSpeed(), data.getSize());
                } else {
                    wave.setupKiWavePlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize());
                    wave.setColorOutline(data.getColorOutline());
                }
                wave.setKiType(kiTypeOrdinal);
                wave.setTechniqueId(data.getId());
                wave.setArmorPenetration(data.getArmorPenetration());
                wave.setHeal(isHeal);
                wave.setHomingTarget(homingTargetId);

                if (isInitialSpawn) {
                    wave.setFiring(false);
                    wave.setMaxLife(99999);
                } else wave.setFiring(true);

                if (!level.isClientSide) level.addFreshEntity(wave);
                break;
            case LASER:
                KiLaserEntity laser = new KiLaserEntity(level, owner);
                if ("death_beam".equals(data.getId())) {
                    laser.setupKiLaserPlayer(owner, realDamage, data.getSpeed(), 0xFF59FF, 0xD859FF);
                    laser.setColorOutline(0x9238F2);
                } else {
                    laser.setupKiLaserPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior());
                    laser.setColorOutline(data.getColorOutline());
                }
                laser.setKiType(kiTypeOrdinal);
                laser.setTechniqueId(data.getId());
                laser.setArmorPenetration(data.getArmorPenetration());
                laser.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(laser);
                break;
            case BEAM:
                KiLaserEntity beam = new KiLaserEntity(level, owner);
                if ("makkanko".equals(data.getId())) {
                    beam.setupKiMakkankosanpoPlayer(owner, realDamage, data.getSpeed());
                    beam.setKiType(kiTypeOrdinal);
                    beam.setTechniqueId(data.getId());
                    beam.setArmorPenetration(data.getArmorPenetration());
                    beam.setHeal(isHeal);
                } else {
                    beam.setupKiBeamPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getColorOutline());
                    beam.setKiType(kiTypeOrdinal);
                    beam.setTechniqueId(data.getId());
                    beam.setArmorPenetration(data.getArmorPenetration());
                    beam.setHeal(isHeal);
                }

                    if (!level.isClientSide) level.addFreshEntity(beam);
                break;
            case DISK:
                if ("kienzan_doble".equals(data.getId())) {

                    KiDiskEntity diskRight = new KiDiskEntity(level, owner);
                    diskRight.setupKiDiskPlayer(owner, realDamage, data.getSpeed()*1.5F, data.getColorInterior(), data.getSize());
                    diskRight.setColors(data.getColorInterior(), data.getColorExterior(), data.getColorOutline());
                    diskRight.setCastOffsets(0.8F, 0.7F, 0.2F);
                    diskRight.setKiType(kiTypeOrdinal);
                    diskRight.setTechniqueId(data.getId());
                    diskRight.setArmorPenetration(data.getArmorPenetration());
                    diskRight.setHeal(isHeal);
                    diskRight.setHomingTarget(homingTargetId);

                    KiDiskEntity diskLeft = new KiDiskEntity(level, owner);
                    diskLeft.setupKiDiskPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getSize());
                    diskLeft.setColors(data.getColorInterior(), data.getColorExterior(), data.getColorOutline());
                    diskLeft.setCastOffsets(-0.8F, 0.7F, 0.2F);
                    diskLeft.setKiType(kiTypeOrdinal);
                    diskLeft.setTechniqueId(data.getId());
                    diskLeft.setArmorPenetration(data.getArmorPenetration());
                    diskLeft.setHeal(isHeal);
                    diskLeft.setHomingTarget(homingTargetId);

                    if (!level.isClientSide) {
                        level.addFreshEntity(diskRight);
                        level.addFreshEntity(diskLeft);
                    }
                } else if ("kienzan".equals(data.getId())) {
                    KiDiskEntity disk = new KiDiskEntity(level, owner);
                    disk.setupKiDiskPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getSize());
                    disk.setColors(0xfffb7d, data.getColorExterior(), 0xFFFFFF);
                    disk.setKiType(kiTypeOrdinal);
                    disk.setTechniqueId(data.getId());
                    disk.setArmorPenetration(data.getArmorPenetration());
                    disk.setHeal(isHeal);
                    disk.setHomingTarget(homingTargetId);

                    if (!level.isClientSide) level.addFreshEntity(disk);
                } else {
                    KiDiskEntity disk = new KiDiskEntity(level, owner);
                    disk.setupKiDiskPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getSize());
                    disk.setColors(data.getColorInterior(), data.getColorExterior(), data.getColorOutline());
                    disk.setKiType(kiTypeOrdinal);
                    disk.setTechniqueId(data.getId());
                    disk.setArmorPenetration(data.getArmorPenetration());
                    disk.setHeal(isHeal);
                    disk.setHomingTarget(homingTargetId);

                    if (!level.isClientSide) level.addFreshEntity(disk);
                }
                break;
            case SHIELD:
                KiBarrierEntity barrier = new KiBarrierEntity(level, owner);
                barrier.setupBarrierPlayer(owner, realDamage, data.getSize(), data.getColorInterior(), data.getColorExterior());
                barrier.setColorOutline(data.getColorOutline());
                barrier.setKiType(kiTypeOrdinal);
                barrier.setTechniqueId(data.getId());
                barrier.setArmorPenetration(data.getArmorPenetration());
                barrier.setHeal(isHeal);
                // A HEAL shield aimed at a friendly target wraps that ally instead of the caster.
                if (isHeal && homingTarget != null) barrier.setShieldHost(homingTarget.getId());

                if (!level.isClientSide) level.addFreshEntity(barrier);
                break;
            case EXPLOSION:
                KiExplosionEntity explosion = new KiExplosionEntity(level, owner);

                if ("final_explosion".equals(data.getId())) {
                    explosion.setupExplosionPlayer(owner, realDamage, data.getSize(), 0xFFFA99, 0xFCF56A);
                    explosion.setColorOutline(0xFFFFFC);
                } else {
                    explosion.setupExplosionPlayer(owner, realDamage, data.getSize(), data.getColorInterior(), data.getColorExterior());
                    explosion.setColorOutline(data.getColorOutline());
                }


                explosion.setKiType(kiTypeOrdinal);
                explosion.setTechniqueId(data.getId());
                explosion.setArmorPenetration(data.getArmorPenetration());
                explosion.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(explosion);
                break;
            case AREA:
                if (isInitialSpawn) {
                    KiAreaEntity area = new KiAreaEntity(level, owner);

                    area.setupAreaPlayer(
                            owner,
                            realDamage,
                            data.getSize() * 1.5F,
                            data.getColorInterior(),
                            data.getColorExterior(),
                            data.getColorOutline()
                    );

                    area.setKiType(kiTypeOrdinal);
                    area.setTechniqueId(data.getId());
                    area.setArmorPenetration(data.getArmorPenetration());
                    area.setHeal(isHeal);

                    area.setFiring(false);
                    area.setMaxLife(99999);

                    if (!level.isClientSide) level.addFreshEntity(area);
                }
                break;
            case BARRAGE:
                if (isInitialSpawn) {
                    KiBlastEntity volley = new KiBlastEntity(level, owner);

                    volley.setupKiVolleyPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), 40);
                    volley.setColors(data.getColorInterior(), data.getColorExterior(), data.getColorOutline());

                    volley.setKiType(kiTypeOrdinal);
                    volley.setTechniqueId(data.getId());
                    volley.setArmorPenetration(data.getArmorPenetration());
                    volley.setHeal(isHeal);

                    volley.setFiring(false);
                    volley.setMaxLife(99999);

                    if (!level.isClientSide) level.addFreshEntity(volley);
                }
                break;
            default:
                KiBlastEntity defaultBlast = new KiBlastEntity(level, owner);
                defaultBlast.setupKiBlastPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize());
                defaultBlast.setColorOutline(data.getColorOutline());
                defaultBlast.setKiType(kiTypeOrdinal);
                defaultBlast.setTechniqueId(data.getId());
                defaultBlast.setArmorPenetration(data.getArmorPenetration());
                defaultBlast.setHeal(isHeal);
                break;
        }
        return true;
    }

    private static AbstractKiProjectile getChargingKiEntity(LivingEntity owner, Level level) {
        List<AbstractKiProjectile> nearby = level.getEntitiesOfClass(AbstractKiProjectile.class, owner.getBoundingBox().inflate(30.0D));
        for (AbstractKiProjectile ki : nearby)
            if (ki.getOwner() != null && ki.getOwner().getUUID().equals(owner.getUUID()))
                if (!ki.isFiring()) return ki;
        return null;
    }

    private static List<AbstractKiProjectile> getChargingKiEntities(LivingEntity owner, Level level) {
        List<AbstractKiProjectile> chargingKis = new ArrayList<>();
        List<AbstractKiProjectile> nearby = level.getEntitiesOfClass(AbstractKiProjectile.class, owner.getBoundingBox().inflate(30.0D));
        for (AbstractKiProjectile ki : nearby) {
            if (ki.getOwner() != null && ki.getOwner().getUUID().equals(owner.getUUID()) && !ki.isFiring()) {
                chargingKis.add(ki);
            }
        }
        return chargingKis;
    }

    private static int resolvePlayerMaxLifeTicks(KiAttackData data, float chargeMultiplier) {
		int base = switch (data.getKiType()) {
			case WAVE, LASER, BEAM -> 80;
			case GIANT_BALL, EXPLOSION -> 120;
			case DISK -> 70;
			case BARRAGE -> 50;
			default -> 90;
		};
		return Math.max(20, (int) (base * chargeMultiplier));
	}

	public static boolean isFiringKiAttack(Player player) {
		List<AbstractKiProjectile> projectiles = player.level().getEntitiesOfClass(AbstractKiProjectile.class, player.getBoundingBox().inflate(32.0D));
		for (AbstractKiProjectile ki : projectiles) if (ki.getOwner() != null && ki.getOwner().getUUID().equals(player.getUUID())) if (ki.isFiring()) return true;
		return false;
	}

	public static boolean isMovementRestrictedKiAttack(Player player, StatsData data) {
		if (isChargingRestrictedTechniqueType(getCurrentChargingKiType(data), true)) return true;
		return hasOwnedProjectileWithRestriction(player, true);
	}

	public static boolean isActionRestrictedKiAttack(Player player, StatsData data) {
		if (isChargingRestrictedTechniqueType(getCurrentChargingKiType(data), false)) return true;
		return hasOwnedProjectileWithRestriction(player, false);
	}

	public static KiAttackData.KiType getChargingKiType(StatsData data) {
		return getCurrentChargingKiType(data);
	}

	private static KiAttackData.KiType getCurrentChargingKiType(StatsData data) {
		if (data == null) return null;
		String chargingTechniqueId = data.getTechniques().getChargingTechniqueId();
		if (chargingTechniqueId == null || chargingTechniqueId.isEmpty()) return null;

		TechniqueData chargingTechnique = data.getTechniques().getUnlockedTechniques().get(chargingTechniqueId);
		if (chargingTechnique instanceof KiAttackData kiAttackData) return kiAttackData.getKiType();

		return null;
	}

    private static boolean hasOwnedProjectileWithRestriction(Player player, boolean movementRestriction) {
        List<AbstractKiProjectile> projectiles = player.level().getEntitiesOfClass(AbstractKiProjectile.class, player.getBoundingBox().inflate(32.0D));
        for (AbstractKiProjectile ki : projectiles) {
            if (ki.getOwner() == null || !ki.getOwner().getUUID().equals(player.getUUID())) continue;
            if (isProjectileRestrictedType(ki.getKiType(), movementRestriction)) return true;
            // Lock the caster's position while actively firing a *technique* attack that is still anchored
            // to them (continuous beams, on-body area/barrier, and the launch instant of thrown attacks).
            // Scoped to movement only, and released as soon as the shot flies off (distance grows) — so it
            // is "only while firing". Excludes the basic ki blast, which carries no technique id.
            // Excludes LASER: it's an instant, stationary beam meant to be fired freely without pinning the caster.
            if (movementRestriction && ki.isFiring()
                    && ki.getKiType() != AbstractKiProjectile.KiType.LASER
                    && ki.getTechniqueId() != null && !ki.getTechniqueId().isEmpty()
                    && ki.distanceToSqr(player) <= 9.0D) {
                return true;
            }
        }
        return false;
    }

	private static boolean isChargingRestrictedTechniqueType(KiAttackData.KiType kiType, boolean movementRestriction) {
		if (kiType == null) return false;
		return switch (kiType) {
			case GIANT_BALL, WAVE, BEAM, EXPLOSION, BARRAGE -> true;
			case SHIELD, AREA -> !movementRestriction;
			case SMALL_BALL, MEDIUM_BALL, LASER, DISK -> false;
		};
	}

	private static boolean isProjectileRestrictedType(AbstractKiProjectile.KiType kiType, boolean movementRestriction) {
		return switch (kiType) {
			case GIANT_BALL, WAVE, BEAM, EXPLOSION, BARRAGE -> true;
			case SHIELD, AREA -> !movementRestriction;
			case SMALL_BALL, MEDIUM_BALL, DISK, LASER -> false;
		};
	}

	public static boolean restrictsMovementWhileCharging(KiAttackData.KiType type) {
		return isChargingRestrictedTechniqueType(type, true);
	}

	public static LivingEntity resolveHomingTarget(LivingEntity owner, Level level, KiAttackData data, StatsData statsData) {
		if (!(owner instanceof Player player) || level.isClientSide) return null;
		boolean heal = data.getEffectiveUtility() == KiAttackData.Utility.HEAL;

		int lockedId = statsData.getTechniques().getHomingTargetId();
		LivingEntity locked = resolveLiving(level, lockedId);
		if (locked != null) return targetingAllowed(player, locked, heal) ? locked : null;

		if (heal) {
			LivingEntity looked = lookTarget(player, level, HOMING_RANGE);
			if (looked != null && targetingAllowed(player, looked, true)) return looked;
		}
		return null;
	}

	private static final double HOMING_RANGE = 30.0;

	private static boolean targetingAllowed(Player attacker, LivingEntity target, boolean heal) {
		TargetHelper.Relation relation = TargetHelper.getRelation(attacker, target);
		return heal ? relation != TargetHelper.Relation.HOSTILE
		            : relation != TargetHelper.Relation.FRIENDLY;
	}

	private static LivingEntity resolveLiving(Level level, int id) {
		if (id < 0 || !(level instanceof ServerLevel serverLevel)) return null;
		return (serverLevel.getEntity(id) instanceof LivingEntity living && living.isAlive()) ? living : null;
	}

	private static LivingEntity lookTarget(Player player, Level level, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0F);
		Vec3 end = eye.add(view.scale(range));
		AABB box = player.getBoundingBox().expandTowards(view.scale(range)).inflate(1.0);

		LivingEntity best = null;
		double bestDist = range * range;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
				x -> x != player && x.isAlive() && x.isPickable() && !x.isSpectator())) {
			AABB hitbox = e.getBoundingBox().inflate(0.3);
			Optional<Vec3> hit = hitbox.clip(eye, end);
			if (hit.isPresent()) {
				double d = eye.distanceToSqr(hit.get());
				if (d < bestDist) {
					best = e;
					bestDist = d;
				}
			}
		}
		return best;
	}
}
