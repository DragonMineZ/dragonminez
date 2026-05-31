package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

public class SPDragonFistEntity extends AbstractKiProjectile implements GeoEntity {

    private static final EntityDataAccessor<Float> LOCKED_YAW = SynchedEntityData.defineId(SPDragonFistEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCKED_PITCH = SynchedEntityData.defineId(SPDragonFistEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);
    private Vec3 fixedDirection = null;

    public SPDragonFistEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public SPDragonFistEntity(Level level, LivingEntity owner) {
        super(MainEntities.SP_DRAGON_FIST.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    public void setupDragonFist(LivingEntity owner, float damage, float speed) {
        this.setup(owner, damage, 1.5F, speed, 0xFFD700, 0xFF8C00);
        this.setFiring(true);
        this.setMaxLife(40);

        float yaw = owner.getYHeadRot();
        float pitch = owner.getXRot();

        this.entityData.set(LOCKED_YAW, yaw);
        this.entityData.set(LOCKED_PITCH, pitch);

        this.setYRot(yaw);
        this.setXRot(pitch);

        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), MainSounds.DRAGON_FIST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    @Override
    public void tick() {
        this.baseTick();
        this.onKiTick();

        Entity owner = this.getOwner();

        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        if (this.fixedDirection == null) {
            float yaw = this.entityData.get(LOCKED_YAW);
            float pitch = this.entityData.get(LOCKED_PITCH);
            this.fixedDirection = Vec3.directionFromRotation(pitch, yaw).normalize();

            if (yaw == 0.0f && pitch == 0.0f) {
                this.fixedDirection = owner.getViewVector(1.0F).normalize();
            }
        }

        int currentTick = this.tickCount;

        Vec3 dragonPos = owner.position().add(this.fixedDirection.scale(1.5D));
        this.setPos(dragonPos.x, owner.getY(), dragonPos.z);
        this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));

        if (currentTick < this.getMaxLife()) {
            owner.setDeltaMovement(this.fixedDirection.scale(3.0D).add(0, 0.1D, 0));
            owner.hasImpulse = true;
            owner.fallDistance = 0;

            if (!this.level().isClientSide) {
                devastateEnemies(owner, this.fixedDirection);
            } else {
                spawnDragonAuraParticles(owner, this.fixedDirection);
            }
        } else {
            owner.setDeltaMovement(0, 0, 0);
            this.discard();
        }
    }

    private void devastateEnemies(Entity owner, Vec3 fixedDirection) {
        AABB hitbox = this.getBoundingBox().inflate(5.0D);
        List<Entity> targets = this.level().getEntities(this, hitbox, this::shouldDamage);

        for (Entity target : targets) {
            if (this.tickCount % 5 == 0) {
                if (this.applyDamageOrHeal(target, this.getKiDamage())) {
                    this.onSuccessfulHit(target);
                }
            }

            Vec3 pushVel = fixedDirection.scale(3.2D).add(0, 0.2D, 0);
            target.setDeltaMovement(pushVel);
            target.hasImpulse = true;
            target.fallDistance = 0;
        }
    }

    private void spawnDragonAuraParticles(Entity owner, Vec3 fixedDirection) {
        float[] rgb = this.getRgbColorMain();

        for (int i = 0; i < 10; i++) {
            double dx = (this.random.nextDouble() - 0.5) * 4.0D;
            double dy = (this.random.nextDouble() - 0.5) * 4.0D;
            double dz = (this.random.nextDouble() - 0.5) * 4.0D;

            double vx = -fixedDirection.x * 0.5D;
            double vy = (this.random.nextDouble() - 0.5) * 0.2D;
            double vz = -fixedDirection.z * 0.5D;

            float scale = 3.0f + this.random.nextFloat() * 2.0f;

            net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_TRAIL.get(),
                    this.getX() + dx, this.getY() + 1.0D + dy, this.getZ() + dz,
                    vx, vy, vz
            );

            if (p instanceof KiTrailParticle trail) {
                trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                trail.setKiScale(scale);
            }
        }
    }

    public float getLockedYaw() { return this.entityData.get(LOCKED_YAW); }
    public float getLockedPitch() { return this.entityData.get(LOCKED_PITCH); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LOCKED_YAW, 0.0F);
        this.entityData.define(LOCKED_PITCH, 0.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("LockedYaw", this.entityData.get(LOCKED_YAW));
        pCompound.putFloat("LockedPitch", this.entityData.get(LOCKED_PITCH));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("LockedYaw")) this.entityData.set(LOCKED_YAW, pCompound.getFloat("LockedYaw"));
        if (pCompound.contains("LockedPitch")) this.entityData.set(LOCKED_PITCH, pCompound.getFloat("LockedPitch"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<SPDragonFistEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}