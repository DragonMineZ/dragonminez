package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.init.entities.ki.*;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TechniqueDispatcher {

	public static void executeKiAttack(LivingEntity owner, Level level, KiAttackData data, StatsData statsData) {
		if (level.isClientSide) return;

		double cost = data.getCalculatedCost();
		if (statsData.getResources().getCurrentEnergy() < cost) {
			return;
		}

		statsData.getResources().setCurrentEnergy((int) (statsData.getResources().getCurrentEnergy() - cost));

		float realDamage = (float) (statsData.getKiDamage() * data.getDamageMultiplier());

		AbstractKiProjectile projectile = null;
		boolean isHeal = (data.getUtility() == KiAttackData.Utility.HEAL);

		switch (data.getKiType()) {
			case SMALL_BALL:
				KiBlastEntity smallBall = new KiBlastEntity(level, owner);
				smallBall.setupKiSmall(owner, realDamage, data.getSpeed(), data.getColorExterior());
				projectile = smallBall;
				break;
			case MEDIUM_BALL:
				KiBlastEntity medBall = new KiBlastEntity(level, owner);
				medBall.setupKiBlast(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize(), 40);
				projectile = medBall;
				break;
			case GIANT_BALL:
				KiBlastEntity giantBall = new KiBlastEntity(level, owner);
				giantBall.setupKiLargeBlast(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize(), 40);
				projectile = giantBall;
				break;
			case WAVE:
				KiWaveEntity wave = new KiWaveEntity(level, owner);
				wave.setKiDamage(realDamage);
				wave.setKiSpeed(data.getSpeed());
				wave.setSize(data.getSize());
				wave.setColors(data.getColorInterior(), data.getColorExterior());
				projectile = wave;
				break;
			case LASER:
				KiLaserEntity laser = new KiLaserEntity(level, owner);
				laser.setKiDamage(realDamage);
				laser.setKiSpeed(data.getSpeed());
				laser.setSize(data.getSize());
				laser.setColors(data.getColorInterior(), data.getColorExterior());
				projectile = laser;
				break;
			case DISK:
				KiDiskEntity disk = new KiDiskEntity(level, owner);
				disk.setKiDamage(realDamage);
				disk.setKiSpeed(data.getSpeed());
				disk.setSize(data.getSize());
				disk.setColors(data.getColorInterior(), data.getColorExterior());
				projectile = disk;
				break;
			case SHIELD: //Cambiar este luego xd
				KiBarrierEntity barrier = new KiBarrierEntity(level, owner);
				barrier.setColors(data.getColorInterior(), data.getColorExterior());
				barrier.setSize(data.getSize());
				barrier.setKiDamage(realDamage);
				projectile = barrier;
				break;
			case EXPLOSION:
				KiExplosionEntity explosion = new KiExplosionEntity(level, owner);
				explosion.setupExplosion(owner, realDamage, data.getColorInterior(), data.getColorExterior());
				explosion.setMaxRadius(data.getSize());
				projectile = explosion;
				break;
			case AREA: //Cambiar este luego xd
				KiExplosionEntity areaDrop = new KiExplosionEntity(level, owner);
				areaDrop.setupExplosion(owner, realDamage, data.getColorInterior(), data.getColorExterior());
				areaDrop.setMaxRadius(data.getSize() * 1.5F);
				projectile = areaDrop;
				break;
			case BARRAGE:
				Vec3 look = owner.getLookAngle();
				Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
				Vec3 up = right.cross(look).normalize();

				double[][] offsets = {{0,0}, {1.2, 0.5}, {-1.2, 0.5}, {0.8, -0.8}, {-0.8, -0.8}};
				for (int i = 0; i < offsets.length; i++) {
					KiBarrageEntity barrage = new KiBarrageEntity(level, owner);
					barrage.setup(owner, realDamage / offsets.length, data.getSize(), data.getSpeed(), data.getColorInterior(), data.getColorExterior());

					Vec3 spawnPos = owner.getEyePosition().add(look.scale(0.5))
							.add(right.scale(offsets[i][0]))
							.add(up.scale(offsets[i][1]));
					barrage.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
					barrage.setDeltaMovement(look.scale(data.getSpeed()));

					barrage.setTechniqueId(data.getId());
					barrage.setArmorPenetration(data.getArmorPenetration());
					barrage.setHeal(isHeal);
					level.addFreshEntity(barrage);
				}
				return;
			default:
				KiBlastEntity defaultBlast = new KiBlastEntity(level, owner);
				defaultBlast.setupKiBlast(owner, realDamage, data.getSpeed(), data.getColorInterior(), data.getColorExterior(), data.getSize(), 40);
				projectile = defaultBlast;
				break;
		}

		if (projectile != null) {
			projectile.setTechniqueId(data.getId());
			projectile.setArmorPenetration(data.getArmorPenetration());
			projectile.setHeal(isHeal);
			level.addFreshEntity(projectile);
		}
	}
}