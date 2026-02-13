package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class SPBlueHurricaneEntity extends AbstractKiProjectile implements GeoEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);


    public SPBlueHurricaneEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public SPBlueHurricaneEntity(Level level, LivingEntity owner) {
        super(MainEntities.SP_BLUE_HURRICANE.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        this.baseTick();

        Entity owner = this.getOwner();

        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setDeltaMovement(0, 0, 0);
        this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));

        if (this.level().isClientSide) {

            float[] rgb = ColorUtils.rgbIntToFloat(0x3F58FC);

            for (int i = 0; i < 10; i++) {

                double offsetX = (this.random.nextDouble() - 1.0D) * this.getBbWidth();
                double offsetY = (this.random.nextDouble() - 1.0D) * this.getBbHeight() * 5;
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

        if (this.tickCount >= 140) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            if (this.tickCount % 10 == 0) {
                pulseDamage();
            }
        }
    }

    private void pulseDamage() {
        AABB area = this.getBoundingBox().inflate(3.5D, 9.0D, 3.5D);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (shouldDamage(target)) {
				target.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage());

                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();

                target.setDeltaMovement(dx * 0.4, 0.35, dz * 0.4);
                target.hasImpulse = true;
            }
        }
    }


    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<SPBlueHurricaneEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("fire"));
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
