package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiSheddingParticle;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class KiBlastEntity extends AbstractKiProjectile {

	private boolean hasSpawnedSplash = false;

	public KiBlastEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public KiBlastEntity(Level level, LivingEntity owner) {
		this(MainEntities.KI_BLAST.get(), level);
		this.setOwner(owner);
		level.playSound(
				null,
				owner.getX(),
				owner.getY(),
				owner.getZ(),
				MainSounds.KIBLAST_ATTACK.get(),
				SoundSource.PLAYERS,
				0.1F,
				1.0F + (this.random.nextFloat() * 0.2F)
		);
	}

    public void setupKiSmall(LivingEntity owner, float damage, float speed, int color) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(0); //SmallBall
        this.setSize(0.6F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(color, color);

    }

    public void setupKiBlast(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupKiBlast(owner,damage, speed, color, color, size);
    }

    public void setupKiBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(1); //Blast normal
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder);
    }

    public void setupKiLargeBlast(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupKiLargeBlast(owner, damage, speed, color, color, size);
    }

    public void setupKiLargeBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(2); //Large Blast
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder);
    }

    public void setupInvertedKiBlast(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupInvertedKiBlast(owner,damage, speed, color, color, size);
    }

    public void setupInvertedKiBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(3); //Blast normal
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder);
    }

    public void setupKiSouls(LivingEntity owner, float damage, float speed, int color) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(4); //Castigador de almas
        this.setSize(0.6F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(color, color);
    }

    public void setupKiGenki(LivingEntity owner, float damage, float speed) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(5);
        this.setSize(5.0F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0x30FFF1, 0x00F8FF);
    }

    public void setupKiNova(LivingEntity owner, float damage, float speed) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(6);
        this.setSize(5.0F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0x9E0000, 0x9E0000);
    }
    public void setupKiDeathBall(LivingEntity owner, float damage, float speed) {
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setKiRenderType(7);
        this.setSize(2.5F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0x9B1ADB, 0x5C018A);
    }

	@Override
	protected void onKiTick() {

		if (!this.level().isClientSide && this.getOwner() == null) {
			this.discard();
			return;
		}

        if (!this.level().isClientSide) {
            if (this.tickCount % 20 == 0 && this.tickCount > 0) {
                //Reemplazar por sonido en loop
                //this.playSound(MainSounds.KIBLAST_ATTACK.get(), 0.5F, 1.0F);
            }

            if (this.tickCount >= 100) {
                this.explodeAndDie();
                return;
            }

            if (this.tickCount % 10 == 0) {
                pulseAreaDamage();
            }
        }

        if (this.level().isClientSide) {
            int type = this.getKiRenderType();
            float scale = this.getSize();
            float[] borderColor = ColorUtils.rgbIntToFloat(this.getColorBorde());

            float pr = borderColor[0], pg = borderColor[1], pb = borderColor[2];
            if (type == 4) {
                float speed = (float)this.tickCount * 0.5f;
                pr = (float)(Math.sin(speed) * 0.5 + 0.5);
                pg = (float)(Math.sin(speed + 2.0944) * 0.5 + 0.5); // Desfase para arcoiris
                pb = (float)(Math.sin(speed + 4.1888) * 0.5 + 0.5);
            }

            //Partículas saliendo
            if (type >= 1) {
                for (int i = 0; i < 3; i++) {

                    double radius = scale*1.2;

                    double theta = this.random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * this.random.nextDouble() - 1);

                    double dx = radius * Math.sin(phi) * Math.cos(theta);
                    double dy = radius * Math.sin(phi) * Math.sin(theta);
                    double dz = radius * Math.cos(phi);

                    double vx = dx * 0.15;
                    double vy = dy * 0.15;
                    double vz = dz * 0.15;

                    net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                            MainParticles.KI_TRAIL.get(),
                            this.getX() + dx,
                            this.getY() + (this.getBbHeight() / 2.0) + dy,
                            this.getZ() + dz,
                            vx, vy, vz
                    );

                    if (p instanceof KiTrailParticle trail) {
                        trail.setKiColor(pr, pg, pb);

                        trail.setKiScale((float) scale);
                    }
                }
            }

            //Absorción
            if (type == 2 || type == 5 || type == 6 ) {

                for (int i = 0; i < 10; i++) {
                    double absDist = scale * 3;

                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double sx = Math.cos(angle) * absDist;
                    double sz = Math.sin(angle) * absDist;

                    double sy = (this.random.nextDouble() - 0.5) * 2.0 * absDist;

                    Particle p = Minecraft.getInstance().particleEngine.createParticle(
                            MainParticles.KI_SHEDDING.get(),
                            this.getX() + sx, this.getY() + (this.getBbHeight() / 2) + sy, this.getZ() + sz,
                            -sx * 0.15, -sy * 0.15, -sz * 0.15
                    );

                    if (p instanceof KiSheddingParticle kiParticle) {
                        kiParticle.setKiColor(borderColor[0], borderColor[1], borderColor[2]);
                    }
                }
            }
        }

		if (this.level().isClientSide && !hasSpawnedSplash) {

			float[] rgb = ColorUtils.rgbIntToFloat(this.getColorBorde());

			this.level().addParticle(
					MainParticles.KI_SPLASH.get(),
					this.getX(), this.getY() + (this.getBbHeight() / 2.0), this.getZ(),
					rgb[0], rgb[1], rgb[2]
			);

			this.hasSpawnedSplash = true;
		}

		if (!this.level().isClientSide && this.tickCount % 10 == 0) {
			pulseAreaDamage();
		}
	}

	private void pulseAreaDamage() {
		double radius = this.getSize();
		AABB area = this.getBoundingBox().inflate(radius);
		List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, area);

		for (LivingEntity target : nearby) {
			if (this.shouldDamage(target)) {
				target.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage() * 0.2F);
			}
		}
	}


    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);

        if (!this.level().isClientSide) {
            Entity targetEntity = pResult.getEntity();

            if (this.shouldDamage(targetEntity)) {
                boolean wasHurt = targetEntity.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage());

                if (wasHurt && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {

                    double colorData = (double) this.getColorBorde();
                    double sizeData = (double) this.getSize();

                    double pX = targetEntity.getX();
                    double pY = targetEntity.getY() + (targetEntity.getBbHeight() / 2.0);
                    double pZ = targetEntity.getZ();

                    serverLevel.sendParticles(
                            MainParticles.KI_SPLASH_WAVE.get(),
                            pX, pY, pZ,
                            0,
                            colorData,
                            sizeData,
                            0.0D,
                            1.0D
                    );
                }
            }
            explodeAndDie();
        }
    }

	@Override
	protected void onHitBlock(BlockHitResult pResult) {
		super.onHitBlock(pResult);
		if (!this.level().isClientSide) {
			explodeAndDie();
		}
	}

    private void explodeAndDie() {
        if (this.isRemoved()) return;

        float explosionRadius = this.getSize() * 1.4F;
        float visualParticleSize = explosionRadius * 1.8F;

        AABB damageArea = this.getBoundingBox().inflate(explosionRadius);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, damageArea);
        for (LivingEntity target : targets) {
            if (this.shouldDamage(target)) {
                target.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage() * 1.5F);
            }
        }

        if (!this.level().isClientSide) {
            if (MainGameRules.canKiGrief(this.level(), this.blockPosition(), this.getOwner())) {
                int blockRadius = Math.round(explosionRadius);
                for (int x = -blockRadius; x <= blockRadius; x++) {
                    for (int y = -blockRadius; y <= blockRadius; y++) {
                        for (int z = -blockRadius; z <= blockRadius; z++) {
                            if (x * x + y * y + z * z <= explosionRadius * explosionRadius) {
                                BlockPos targetPos = this.blockPosition().offset(x, y, z);
                                if (this.level().getBlockState(targetPos).getExplosionResistance(this.level(), targetPos, null) < 1000) {
                                    this.level().destroyBlock(targetPos, false);
                                }
                            }
                        }
                    }
                }
            }

            // 3. EFECTOS VISUALES FINALES
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        MainParticles.KI_EXPLOSION.get(),
                        this.getX(), this.getY(), this.getZ(),
                        0, (double) visualParticleSize, 0.0D, 0.0D, 1.0D
                );
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 5.0F, 0.6F);
            }
        }
        this.discard();
    }


}
