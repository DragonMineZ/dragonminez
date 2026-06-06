package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TechniqueDispatcher {

	public static boolean executeKiAttack(LivingEntity owner, Level level, KiAttackData data, StatsData statsData, float chargeMultiplier) {
		if (level.isClientSide) return false;

		boolean isInitialSpawn = chargeMultiplier < 0.5f;
		float clampedCharge = Mth.clamp(chargeMultiplier, 0.5f, 2.0f);

		float realDamage = (float) (statsData.getKiDamage() * data.getDamageMultiplier() * data.getConfiguredDamageMultiplier() * clampedCharge);
		int maxLife = resolvePlayerMaxLifeTicks(data, clampedCharge);
		boolean isHeal = (data.getUtility() == KiAttackData.Utility.HEAL);

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
                        Vec3 lookBlast = owner.getLookAngle();
                        blast.setDeltaMovement(lookBlast.scale(data.getSpeed()));
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
                smallBall.setKiRenderType(0);
                smallBall.setSize(0.8F);
                smallBall.setKiSpeed(data.getSpeed());
                smallBall.setKiDamage(realDamage);
                smallBall.setColors(data.getColorExterior(), data.getColorExterior(), 0xFFFFFF);
                smallBall.setCastTime(0);
                smallBall.setMaxLife(maxLife);
                smallBall.setTechniqueId(data.getId());
                smallBall.setArmorPenetration(data.getArmorPenetration());
                smallBall.setHeal(isHeal);
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
                medBall.setupKiBlastPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize());
                medBall.setTechniqueId(data.getId());
                medBall.setArmorPenetration(data.getArmorPenetration());
                medBall.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(medBall);
                break;
            case GIANT_BALL:
                KiBlastEntity giantBall = new KiBlastEntity(level, owner);
                if ("spiritbomb".equals(data.getId())) {
                    giantBall.setupKiGenkiPlayer(owner, realDamage, data.getSpeed());
                } else if ("supernova".equals(data.getId()) || "supernova_cooler".equals(data.getId())) {
                    giantBall.setupKiNovaPlayer(owner, realDamage, data.getSpeed());
                } else if ("death_ball".equals(data.getId())) {
                    giantBall.setupKiDeathBallPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior());
                } else {
                    giantBall.setupKiLargeBlastPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize());
                }
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
                }
                wave.setTechniqueId(data.getId());
                wave.setArmorPenetration(data.getArmorPenetration());
                wave.setHeal(isHeal);

                if (isInitialSpawn) {
                    wave.setFiring(false);
                    wave.setMaxLife(99999);
                } else wave.setFiring(true);

                if (!level.isClientSide) level.addFreshEntity(wave);
                break;
            case LASER:
                KiLaserEntity laser = new KiLaserEntity(level, owner);
                laser.setupKiLaserPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior());
                laser.setTechniqueId(data.getId());
                laser.setArmorPenetration(data.getArmorPenetration());
                laser.setHeal(isHeal);
                if (!level.isClientSide) level.addFreshEntity(laser);
                break;
            case BEAM:
                KiLaserEntity beam = new KiLaserEntity(level, owner);
                beam.setupKiMakkankosanpoPlayer(owner, realDamage, data.getSpeed());
                beam.setTechniqueId(data.getId());
                beam.setArmorPenetration(data.getArmorPenetration());
                beam.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(beam);
                break;
            case DISK:
                if ("kienzan_doble".equals(data.getId())) {

                    KiDiskEntity diskRight = new KiDiskEntity(level, owner);
                    diskRight.setupKiDiskPlayer(owner, realDamage, data.getSpeed()*1.5F, data.getColorInterior(), data.getSize());
                    diskRight.setCastOffsets(0.8F, 0.7F, 0.2F);
                    diskRight.setTechniqueId(data.getId());
                    diskRight.setArmorPenetration(data.getArmorPenetration());
                    diskRight.setHeal(isHeal);

                    KiDiskEntity diskLeft = new KiDiskEntity(level, owner);
                    diskLeft.setupKiDiskPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getSize());
                    diskLeft.setCastOffsets(-0.8F, 0.7F, 0.2F);
                    diskLeft.setTechniqueId(data.getId());
                    diskLeft.setArmorPenetration(data.getArmorPenetration());
                    diskLeft.setHeal(isHeal);

                    if (!level.isClientSide) {
                        level.addFreshEntity(diskRight);
                        level.addFreshEntity(diskLeft);
                    }
                } else {
                    KiDiskEntity disk = new KiDiskEntity(level, owner);
                    disk.setupKiDiskPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getSize());
                    disk.setTechniqueId(data.getId());
                    disk.setArmorPenetration(data.getArmorPenetration());
                    disk.setHeal(isHeal);

                    if (!level.isClientSide) level.addFreshEntity(disk);
                }
                break;
            case SHIELD:
                KiBarrierEntity barrier = new KiBarrierEntity(level, owner);
                barrier.setupBarrierPlayer(owner, realDamage, data.getSize(), data.getColorInterior(), data.getColorExterior());
                barrier.setTechniqueId(data.getId());
                barrier.setArmorPenetration(data.getArmorPenetration());
                barrier.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(barrier);
                break;
            case EXPLOSION:
                KiExplosionEntity explosion = new KiExplosionEntity(level, owner);
                explosion.setupExplosionPlayer(owner, realDamage, data.getSize(), data.getColorInterior(), data.getColorExterior());
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
                            0xFFFFFF
                    );

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
		}
		return false;
	}

	private static boolean isChargingRestrictedTechniqueType(KiAttackData.KiType kiType, boolean movementRestriction) {
		if (kiType == null) return false;
		return switch (kiType) {
			case GIANT_BALL, WAVE, BEAM, EXPLOSION, BARRAGE -> true;
			case SHIELD, AREA -> !movementRestriction;
			case SMALL_BALL, MEDIUM_BALL, DISK, LASER -> false;
		};
	}

	private static boolean isProjectileRestrictedType(AbstractKiProjectile.KiType kiType, boolean movementRestriction) {
		return switch (kiType) {
			case GIANT_BALL, WAVE, BEAM, EXPLOSION, BARRAGE -> true;
			case SHIELD, AREA -> !movementRestriction;
			case SMALL_BALL, MEDIUM_BALL, DISK, LASER -> false;
		};
	}
}
