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

// IMPORTANTE: Asegúrate de añadir el import de tu KiTrailParticle aquí
// import com.dragonminez.client.particle.KiTrailParticle;

import java.util.List;

public class SPDragonFistEntity extends AbstractKiProjectile implements GeoEntity {

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(SPDragonFistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(SPDragonFistEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = new SingletonAnimatableInstanceCache(this);

    public SPDragonFistEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public SPDragonFistEntity(Level level, LivingEntity owner) {
        super(MainEntities.SP_DRAGON_FIST.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
    }

    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    public void setupDragonFist(LivingEntity owner, float damage, float speed, int castTime) {
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);

        this.setCastTime(castTime);
        this.setFiring(false);

        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());

        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    @Override
    public void tick() {
        this.baseTick();

        Entity owner = this.getOwner();

        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        boolean isFiring = this.isFiring();
        int ticksSinceFire = this.tickCount - this.getCastTime();

        if (!isFiring && this.tickCount >= this.getCastTime()) {
            this.setFiring(true);
            isFiring = true;

            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
                launchEnemies();
            }
        }

        if (!isFiring) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());

            double preserveGravity = owner.getDeltaMovement().y < 0 ? owner.getDeltaMovement().y : 0;
            owner.setDeltaMovement(0, preserveGravity, 0);
            owner.hasImpulse = true;

            if (owner instanceof Player player) {
                player.xxa = 0.0F;
                player.zza = 0.0F;
            }

            if (this.level().isClientSide) {
                spawnAdvancedKiParticles(owner, true);
            }
        }
        else {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
            this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));

            if (owner instanceof Player player) {
                player.xxa = 0.0F;
                player.zza = 0.0F;
            }

            if (ticksSinceFire < 10) {
                owner.setDeltaMovement(0, 0, 0);
                owner.hasImpulse = true;
            }
            else if (ticksSinceFire < 25) {
                owner.setDeltaMovement(0, 3.0D, 0);
                owner.hasImpulse = true;
                owner.fallDistance = 0;
            }
            else if (ticksSinceFire < 60) {
                owner.setDeltaMovement(0, 0.04D, 0);
                owner.hasImpulse = true;
                owner.fallDistance = 0;
                suspendEnemiesInAir();
            }

            if (this.level().isClientSide) {
                spawnAdvancedKiParticles(owner, false);
            }

            if (!this.level().isClientSide) {
                if (ticksSinceFire % 10 == 0 && ticksSinceFire > 15) {
                    pulseDamage();
                }
            }
        }

        if (this.tickCount >= this.getCastTime() + 60) {
            this.discard();
        }
    }

    private void spawnAdvancedKiParticles(Entity owner, boolean isAbsorbing) {
        float[] rgb = ColorUtils.rgbIntToFloat(0xFFF61F);
        int particleAmount = isAbsorbing ? 4 : 8;

        for (int i = 0; i < particleAmount; i++) {
            double dx, dy, dz, vx, vy, vz;
            float scale;

            if (isAbsorbing) {
                double radius = owner.getBbWidth() * 4.0;
                dx = (this.random.nextDouble() - 0.5) * radius * 2;
                dy = (this.random.nextDouble() - 0.5) * owner.getBbHeight() * 2;
                dz = (this.random.nextDouble() - 0.5) * radius * 2;

                vx = -dx * 0.15D;
                vy = -dy * 0.15D;
                vz = -dz * 0.15D;

                scale = 1.0f + this.random.nextFloat() * 1.5f;
            } else {
                dx = (this.random.nextDouble() - 0.5) * owner.getBbWidth();
                dy = (this.random.nextDouble() - 0.5) * owner.getBbHeight();
                dz = (this.random.nextDouble() - 0.5) * owner.getBbWidth();

                vx = dx * 2.0D;
                vy = -0.5D + (this.random.nextDouble() - 0.5) * 0.5D;
                vz = dz * 2.0D;

                scale = 2.0f + this.random.nextFloat() * 2.0f;
            }

            net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_TRAIL.get(),
                    this.getX() + dx, this.getY() + (owner.getBbHeight() / 2.0) + dy, this.getZ() + dz,
                    vx, vy, vz
            );

            if (p instanceof KiTrailParticle trail) {
                trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                trail.setKiScale(scale);
            }
        }
    }

    private void launchEnemies() {
        AABB area = this.getBoundingBox().inflate(8.0D, 4.0D, 8.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (shouldDamage(target) && !target.is(this.getOwner())) {
                target.setDeltaMovement(0, 3.2D, 0);
                target.hasImpulse = true;
            }
        }
    }

    private void suspendEnemiesInAir() {
        AABB area = this.getBoundingBox().inflate(5.0D, 25.0D, 8.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (shouldDamage(target) && !target.is(this.getOwner())) {
                target.setDeltaMovement(target.getDeltaMovement().x * 0.5, 0, target.getDeltaMovement().z * 0.5);
                target.hasImpulse = true;
                target.fallDistance = 0;
            }
        }
    }

    private void pulseDamage() {
        AABB area = this.getBoundingBox().inflate(8.0D, 25.0D, 8.0D);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (shouldDamage(target) && !target.is(this.getOwner())) {
                target.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage());
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST_TIME, 0);
        this.entityData.define(IS_FIRING, false);
    }

    public int getCastTime() { return this.entityData.get(CAST_TIME); }
    public void setCastTime(int ticks) { this.entityData.set(CAST_TIME, ticks); }
    public boolean isFiring() { return this.entityData.get(IS_FIRING); }
    public void setFiring(boolean firing) { this.entityData.set(IS_FIRING, firing); }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("CastTime", this.getCastTime());
        pCompound.putBoolean("IsFiring", this.isFiring());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("CastTime")) this.setCastTime(pCompound.getInt("CastTime"));
        if (pCompound.contains("IsFiring")) this.setFiring(pCompound.getBoolean("IsFiring"));
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