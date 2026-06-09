package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
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

public class OzaruFistEntity extends AbstractKiProjectile implements GeoEntity {

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    public OzaruFistEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public OzaruFistEntity(Level level, LivingEntity owner) {
        super(MainEntities.SP_OZARU_FIST.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 5;
    }

    public void setupOzaruFist(LivingEntity owner, float damage, float speed) {
        this.setup(owner, damage, 1.5F, speed, 0xFFFFFF, 0x8B4513);
        this.setFiring(true);

        this.setMaxLife(30);

        this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), MainSounds.OOZARU_FIST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

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

        int currentTick = this.tickCount;

        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));

        if (currentTick < this.getMaxLife()) {
            double upwardForce = 1.8D;

            owner.setDeltaMovement(0, upwardForce, 0);
            owner.hasImpulse = true;
            owner.fallDistance = 0;

            if (!this.level().isClientSide) {
                devastateEnemies(owner, upwardForce);
            } else {
                spawnAuraParticles(owner);
            }
        } else {
            owner.setDeltaMovement(0, 0, 0);
            this.discard();
        }
    }

    private void devastateEnemies(Entity owner, double upwardForce) {
        AABB hitbox = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        List<Entity> targets = this.level().getEntities(this, hitbox, this::shouldDamage);

        for (Entity target : targets) {
            if (this.tickCount % 5 == 0) {
                if (this.applyDamageOrHeal(target, this.getKiDamage())) {
                    this.onSuccessfulHit(target);
                    this.applyStrikeStun(target);
                }
            }

            double pullX = (owner.getX() - target.getX()) * 0.3D;
            double pullZ = (owner.getZ() - target.getZ()) * 0.3D;

            double pushY = upwardForce * 1.1D;

            target.setDeltaMovement(pullX, pushY, pullZ);
            target.hasImpulse = true;
            target.fallDistance = 0;
        }
    }

    private void spawnAuraParticles(Entity owner) {
        float[] rgb = this.getRgbColorMain();

        for (int i = 0; i < 10; i++) {
            double dx = (this.random.nextDouble() - 0.5) * 4.0D;
            double dy = (this.random.nextDouble() - 0.5) * 4.0D;
            double dz = (this.random.nextDouble() - 0.5) * 4.0D;

            double vx = (this.random.nextDouble() - 0.5) * 0.2D;
            double vy = -1.5D;
            double vz = (this.random.nextDouble() - 0.5) * 0.2D;

            float scale = 3.0f + this.random.nextFloat() * 2.0f;

            net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_TRAIL.get(),
                    this.getX() + dx, this.getY() + dy, this.getZ() + dz,
                    vx, vy, vz
            );

            if (p instanceof KiTrailParticle trail) {
                trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                trail.setKiScale(scale);
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<OzaruFistEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}