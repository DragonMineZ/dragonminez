package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiBarrierEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> CURRENT_SIZE = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.FLOAT);

    private static final int GROW_DURATION = 25;
    private static final int MAX_LIFESPAN = 100;
    private static final float MAX_SIZE = 3.0F;

    public KiBarrierEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public KiBarrierEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_BARRIER.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;

        this.setPos(owner.getX(), owner.getY(), owner.getZ());

        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                MainSounds.KI_EXPLOSION_IMPACT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_SIZE, 0.1F);
    }

    public float getCurrentSize() {
        return this.entityData.get(CURRENT_SIZE);
    }

    public void setCurrentSize(float size) {
        this.entityData.set(CURRENT_SIZE, size);
    }

    @Override
    public void tick() {
        this.baseTick();

        Entity owner = this.getOwner();

        if (owner != null && owner.isAlive()) {
            double ownerCenterY = owner.getY() + owner.getBbHeight() * 0.5D;

            double barrierHalfHeight = this.getBbHeight() * 0.5D;

            this.setPos(owner.getX(), ownerCenterY - barrierHalfHeight, owner.getZ());

            this.setDeltaMovement(0, 0, 0);
        } else {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            if (this.tickCount <= GROW_DURATION) {
                float progress = (float) this.tickCount / (float) GROW_DURATION;
                float newSize = 0.1F + (MAX_SIZE * progress);

                this.setCurrentSize(newSize);
                this.setSize(newSize);
                this.refreshDimensions();
            }
        } else {
            this.refreshDimensions();
        }

        if (!this.level().isClientSide) {
            pushEntitiesAway();
        } else {
            spawnBarrierParticles();
        }

        if (this.tickCount >= MAX_LIFESPAN) {
            if (!this.level().isClientSide) {
                this.discard();
            }
        }
    }

    private void pushEntitiesAway() {
        float radius = this.getCurrentSize() * 0.8F;
        AABB area = this.getBoundingBox().inflate(0.3D);

        List<Entity> targets = this.level().getEntities(this, area);

        for (Entity target : targets) {
            if (target.is(this.getOwner())) continue;

            if (!(target instanceof LivingEntity) && !(target instanceof Projectile)) continue;

            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dy = target.getY() - (this.getY() + this.getBbHeight() * 0.5);

            Vec3 vec = new Vec3(dx, dy, dz).normalize().scale(1.5);

            target.setDeltaMovement(vec);
            target.hasImpulse = true;

            if (target instanceof Projectile) {
                target.remove(RemovalReason.DISCARDED);
            }
        }
    }

    private void spawnBarrierParticles() {
        float size = this.getCurrentSize();
        if (size < 0.2F) return;

        float[] rgb = ColorUtils.rgbIntToFloat(this.getColor());

        for (int i = 0; i < 4; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2;
            double phi = this.random.nextDouble() * Math.PI;

            double r = size / 2.0;

            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi);
            double z = r * Math.sin(phi) * Math.sin(theta);

            this.level().addParticle(
                    MainParticles.KI_TRAIL.get(),
                    this.getX() + x,
                    this.getY() + (this.getBbHeight() / 2.0) + y,
                    this.getZ() + z,
                    rgb[0], rgb[1], rgb[2]
            );
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        float size = this.getCurrentSize();
        return EntityDimensions.scalable(size, size);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (CURRENT_SIZE.equals(pKey)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
    }

    @Override
    protected void onKiTick() {
    }
}