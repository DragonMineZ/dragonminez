package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class KiBlastEntity extends AbstractKiProjectile{

    private boolean hasSpawnedFlash = false;
    private boolean hasSpawnedSplash = false;

    public KiBlastEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public KiBlastEntity(Level level, LivingEntity owner) {
        this(MainEntities.KI_BLAST.get(), level);
        this.setOwner(owner);
    }

    @Override
    protected void onKiTick() {

        if (this.level().isClientSide) {

            float[] rgb = ColorUtils.rgbIntToFloat(this.getColorBorde());

            for (int i = 0; i < 10; i++) {

                double offsetX = (this.random.nextDouble() - 1.0D) * this.getBbWidth();
                double offsetY = (this.random.nextDouble() - 1.0D) * this.getBbHeight();
                double offsetZ = (this.random.nextDouble() - 1.0D) * this.getBbWidth();

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

            float[] rgb = ColorUtils.rgbIntToFloat(this.getColorBorde());

            this.level().addParticle(
                    MainParticles.KI_SPLASH.get(),
                    this.getX(), this.getY() + (this.getBbHeight() / 2.0), this.getZ(),
                    rgb[0], rgb[1], rgb[2]
            );

            this.hasSpawnedSplash = true;
        }

        if (this.level().isClientSide && !hasSpawnedFlash) {
            this.level().addParticle(
                    MainParticles.KI_FLASH.get(),
                    this.getX(), this.getY(), this.getZ(),
                    (double) this.getId(), 0.0D, 0.0D
            );
            this.hasSpawnedFlash = true;
        }

        if (!this.level().isClientSide && this.tickCount % 60 == 0) { //damage cada 3 segundos
            pulseAreaDamage();
        }
    }

    private void pulseAreaDamage() {
        double radius = this.getSize();
        AABB area = this.getBoundingBox().inflate(radius);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : nearby) {
            if (this.shouldDamage(target)) {
                target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.getKiDamage() * 0.2F);
            }
        }
    }


    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if (!this.level().isClientSide) {
            pResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), this.getKiDamage());
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
        boolean shouldDestroyBlocks = ConfigManager.getServerConfig().getGameplay().isKiDestroyBlocks();
        Level.ExplosionInteraction interaction = shouldDestroyBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), 3.0F, interaction);
        this.discard();
    }
}
