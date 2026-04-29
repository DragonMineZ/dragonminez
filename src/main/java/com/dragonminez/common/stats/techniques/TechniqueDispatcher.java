package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class TechniqueDispatcher {

	public static boolean executeKiAttack(LivingEntity owner, Level level, KiAttackData data, StatsData statsData, float chargeMultiplier) {
		if (level.isClientSide) return false;

		boolean isInitialSpawn = chargeMultiplier < 0.5f;
		float clampedCharge = Mth.clamp(chargeMultiplier, 0.5f, 2.0f);

		double cost = data.getCalculatedCost() * clampedCharge;
		if (isInitialSpawn) if (statsData.getResources().getCurrentEnergy() < (data.getCalculatedCost() * 0.5)) return false;
		else {
			if (statsData.getResources().getCurrentEnergy() < cost) return false;
			statsData.getResources().setCurrentEnergy((int) (statsData.getResources().getCurrentEnergy() - cost));
		}

		float realDamage = (float) (statsData.getKiDamage() * data.getDamageMultiplier() * clampedCharge);
		int maxLife = resolvePlayerMaxLifeTicks(data, clampedCharge);
		boolean isHeal = (data.getUtility() == KiAttackData.Utility.HEAL);

        if (!isInitialSpawn) {
            AbstractKiProjectile activeKi = getChargingKiEntity(owner, level);

            if (activeKi instanceof KiWaveEntity wave) {
                wave.setKiDamage(realDamage);
                wave.fireHability(maxLife);
                return true;
			} else if (activeKi instanceof KiBlastEntity blast) {
				blast.setKiDamage(realDamage);
				blast.fireHability(maxLife);
				Vec3 lookBlast = owner.getLookAngle();
				blast.setDeltaMovement(lookBlast.scale(data.getSpeed()));
				return true;
            } else if (activeKi instanceof KiLaserEntity laser) {
                laser.setKiDamage(realDamage);
                laser.fireHability(maxLife);
                return true;
            }else if (activeKi instanceof KiDiskEntity disk) {
                disk.setKiDamage(realDamage);
                disk.fireHability(maxLife);
                return true;
            }else if (activeKi instanceof KiExplosionEntity explosion) {
                explosion.setKiDamage(realDamage);
                explosion.fireHability(maxLife);
                return true;
            }else if (activeKi instanceof KiBarrierEntity barrier) {
                barrier.setKiDamage(realDamage);
                barrier.fireHability(maxLife);
                return true;
            }

        }

		switch (data.getKiType()) {
			case SMALL_BALL:
				if (!isInitialSpawn) return true;

				double instantCost = data.getCalculatedCost();
				if (statsData.getResources().getCurrentEnergy() < instantCost) return false;

				statsData.getResources().setCurrentEnergy((int) (statsData.getResources().getCurrentEnergy() - instantCost));

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
                    //wave.setupKiWavePlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize());
                } else if ("galick_gun".equals(data.getId())) {
                    wave.setupKiGalickGunPlayer(owner, realDamage, data.getSpeed(), data.getSize());
                } else if ("final_flash".equals(data.getId())) {
                    wave.setupFinalFlashPlayer(owner, realDamage, data.getSpeed(), data.getSize());
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
                KiDiskEntity disk = new KiDiskEntity(level, owner);
                disk.setupKiDiskPlayer(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getSize());
                disk.setTechniqueId(data.getId());
                disk.setArmorPenetration(data.getArmorPenetration());
                disk.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(disk);
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
			case AREA: //Cambiar este luego xd
                KiExplosionEntity areaDrop = new KiExplosionEntity(level, owner);
                areaDrop.setupExplosionPlayer(owner, realDamage, data.getSize() * 1.5F, data.getColorInterior(), data.getColorExterior());
                areaDrop.setTechniqueId(data.getId());
                areaDrop.setArmorPenetration(data.getArmorPenetration());
                areaDrop.setHeal(isHeal);

                if (!level.isClientSide) level.addFreshEntity(areaDrop);
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
                } else {
                    AbstractKiProjectile activeKi = getChargingKiEntity(owner, level);
                    if (activeKi instanceof KiBlastEntity blast) {
                        blast.setKiDamage(realDamage);
                        blast.fireHability(maxLife);
                        return true;
                    }
                }
                return true;
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
        List<AbstractKiProjectile> nearby = level.getEntitiesOfClass(
                AbstractKiProjectile.class,
                owner.getBoundingBox().inflate(25.0D)
        );
        for (AbstractKiProjectile ki : nearby) {
            if (ki.getOwner() != null && ki.getOwner().getUUID().equals(owner.getUUID())) {
                if (ki instanceof KiWaveEntity wave && !wave.isFiring()) {
                    return wave;
                }
                if (ki instanceof KiBlastEntity blast && !blast.isFiring()) {
                    return blast;
                }
                if (ki instanceof KiLaserEntity laser && !laser.isFiring()) return laser;
                if (ki instanceof KiDiskEntity disk && !disk.isFiring()) return disk;
                if (ki instanceof KiExplosionEntity explosion && !explosion.isFiring()) return explosion;
                if (ki instanceof KiBarrierEntity barrier && !barrier.isFiring()) return barrier;
            }
        }
        return null;
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
}