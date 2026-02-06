package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiDiscEntity extends AbstractKiProjectile {

    private boolean hasSpawnedSplash = false;

    public KiDiscEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.refreshDimensions();
    }

    public KiDiscEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_DISC.get(), level);
        this.setOwner(owner);

        Vec3 look = owner.getLookAngle();
        Vec3 spawnPos = owner.getEyePosition().add(look.scale(0.5));
        this.setPos(spawnPos.x, spawnPos.y - 0.2D, spawnPos.z);

        level.playSound(
                null,
                owner.getX(), owner.getY(), owner.getZ(),
                MainSounds.KI_DISK_CHARGE.get(),
                SoundSource.PLAYERS,
                0.5F,
                1.0F + (this.random.nextFloat() * 0.2F)
        );

        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        float scale = this.getSize();
        float width = 1.0F * scale;
        float height = Math.max(0.0625F * scale, 0.15F);
        return EntityDimensions.scalable(width, height);
    }


    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        this.refreshDimensions();
    }

    @Override
    protected void onKiTick() {
        if (!this.level().isClientSide && this.getOwner() == null) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            if (this.tickCount % 12 == 0 && this.tickCount > 0) {
                this.playSound(MainSounds.KI_DISK_CHARGE.get(), 0.3F, 1.2F);
            }
        }

        if (this.level().isClientSide) {
            float[] rgb = ColorUtils.rgbIntToFloat(this.getColor());
            double width = this.getBbWidth();

            for (int i = 0; i < 5; i++) {
                double offsetX = (this.random.nextDouble() - 0.5D) * width;
                double offsetZ = (this.random.nextDouble() - 0.5D) * width;
                double offsetY = (this.random.nextDouble() - 0.5D) * 0.1D;

                this.level().addParticle(
                        MainParticles.KI_TRAIL.get(),
                        this.getX() + offsetX,
                        this.getY() + (this.getBbHeight() / 2.0) + offsetY,
                        this.getZ() + offsetZ,
                        rgb[0], rgb[1], rgb[2]
                );
            }
        }

        if (this.level().isClientSide && !hasSpawnedSplash) {
            float[] rgb = ColorUtils.rgbIntToFloat(this.getColor());
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
        AABB area = this.getBoundingBox().inflate(0.5D, 0.2D, 0.5D);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : nearby) {
            if (this.shouldDamage(target)) {
                target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.getKiDamage() * 0.1F);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);

        if (!this.level().isClientSide) {
            Entity targetEntity = pResult.getEntity();

            if (this.shouldDamage(targetEntity)) {
                boolean wasHurt = targetEntity.hurt(this.damageSources().thrown(this, this.getOwner()), this.getKiDamage());

                if (wasHurt) {
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double colorData = (double) this.getColor();
                        double sizeData = (double) this.getBbWidth();

                        serverLevel.sendParticles(
                                MainParticles.KI_SPLASH_WAVE.get(),
                                targetEntity.getX(), targetEntity.getY() + (targetEntity.getBbHeight() / 2.0), targetEntity.getZ(),
                                0, colorData, sizeData, 0.0D, 1.0D
                        );
                    }
                }
            }

            // --- CAMBIO CLAVE ---
            // Eliminamos explodeAndDie().
            // Ahora el disco seguirá su camino atravesando a la entidad.
        }
    }
}