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
        this.setKiDamage(damage);

        // Spawn the entity at the owner's position so it is tracked/synced correctly from tick 0.
        // Without this it is added at (0,0,0), outside client tracking range, and never reaches the client.
        Vec3 spawnPos = owner.position().add(Vec3.directionFromRotation(pitch, yaw).normalize().scale(1.5D));
        this.setPos(spawnPos.x, owner.getY(), spawnPos.z);
        this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));

        // Bind the sound to the caster so it follows them during the forward charge instead of
        // staying fixed at the launch position and fading as the player moves away.
        this.level().playSound(null, owner, MainSounds.DRAGON_FIST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

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

        // Offset the fist ahead of the caster, but clip that offset against blocks so it doesn't
        // poke through a wall the caster is pressed against.
        Vec3 dragonBase = owner.position().add(0, owner.getBbHeight() * 0.5D, 0);
        Vec3 dragonPos = this.clipAgainstBlocks(dragonBase, dragonBase.add(this.fixedDirection.scale(1.5D)));
        this.setPos(dragonPos.x, owner.getY(), dragonPos.z);
        this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));

        if (currentTick < this.getMaxLife()) {
            owner.setDeltaMovement(this.fixedDirection.scale(2.5D).add(0, 0.1D, 0));
            owner.hasImpulse = true;
            owner.fallDistance = 0;

            if (currentTick % 10 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }

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
            if (target.invulnerableTime <= 0) {
                boolean hit = target.hurt(MainDamageTypes.strikeAttack(this.level(), owner, "dragon_fist"), this.getDamagePerHit());

                if (hit) {
                    target.invulnerableTime = CONTINUOUS_HIT_INTERVAL;
                    this.onSuccessfulHit(target);
                    this.applyStrikeStun(target);
                }
            }

            // Lock the target onto the caster's position each tick, but never teleport it through a
            // wall to get there: if a block sits between them (e.g. the caster grabbed it from the
            // far side of an obsidian wall) it is held on its own side instead of passing through.
            this.holdTargetAtCaster(target, owner.position());
            target.setDeltaMovement(0, 0, 0);
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