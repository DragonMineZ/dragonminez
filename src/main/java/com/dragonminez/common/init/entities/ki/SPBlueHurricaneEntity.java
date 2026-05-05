package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.particle.Particle;
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

public class SPBlueHurricaneEntity extends AbstractKiProjectile implements GeoEntity {

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(SPBlueHurricaneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(SPBlueHurricaneEntity.class, EntityDataSerializers.BOOLEAN);

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
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    public void setupHurricane(LivingEntity owner, float damage, float speed, int castTime) {
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

        if (!isFiring && this.tickCount >= this.getCastTime()) {
            this.setFiring(true);
            isFiring = true;

            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
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
                float[] rgb = ColorUtils.rgbIntToFloat(0x3F58FC);

                for (int i = 0; i < 4; i++) {
                    double radius = 4.0 + this.random.nextDouble() * 2.0;
                    double theta = this.random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * this.random.nextDouble() - 1);

                    double offsetX = radius * Math.sin(phi) * Math.cos(theta);
                    double offsetY = radius * Math.cos(phi) + 1.0;
                    double offsetZ = radius * Math.sin(phi) * Math.sin(theta);

                    double spawnX = this.getX() + offsetX;
                    double spawnY = this.getY() + offsetY;
                    double spawnZ = this.getZ() + offsetZ;

                    double vx = this.getX() - spawnX;
                    double vy = (this.getY() + 1.0) - spawnY;
                    double vz = this.getZ() - spawnZ;

                    Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                            MainParticles.KI_TRAIL.get(),
                            spawnX, spawnY, spawnZ,
                            vx * 0.15D, vy * 0.15D, vz * 0.15D
                    );

                    if (p instanceof KiTrailParticle trail) {
                        trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                        trail.setKiScale(1.5f + this.random.nextFloat() * 1.5f);
                    }
                }
            }
        }

        else {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
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

            if (!this.level().isClientSide) {
                if (this.tickCount % 10 == 0) {
                    pulseDamage();
                }
            }
        }

        if (this.tickCount >= this.getCastTime() + 140) {
            this.discard();
        }
    }

    private void pulseDamage() {
        AABB area = this.getBoundingBox().inflate(4.5D, 9.0D, 4.5D); // Aumenté el radio a 4.5 para que jale desde más lejos

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (shouldDamage(target) && !target.is(this.getOwner())) {

                target.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage());

                double dx = this.getX() - target.getX();
                double dz = this.getZ() - target.getZ();

                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 0) {
                    dx /= distance;
                    dz /= distance;
                }

                target.setDeltaMovement(dx * 0.4, 0.5D, dz * 0.4);
                target.hasImpulse = true;
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

    private PlayState predicate(AnimationState<SPBlueHurricaneEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("fire"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}