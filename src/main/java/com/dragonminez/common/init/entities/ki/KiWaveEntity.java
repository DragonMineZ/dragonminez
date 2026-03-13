package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiWaveEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIXED_YAW = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIXED_PITCH = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);

    private static final float MAX_RANGE = 300.0F;

    public KiWaveEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public KiWaveEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_WAVE.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;

        this.entityData.set(FIXED_YAW, owner.getYRot());
        this.entityData.set(FIXED_PITCH, owner.getXRot());

        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());

        Vec3 look = owner.getLookAngle();
        Vec3 startPos = owner.getEyePosition().add(look.scale(0.4));
        this.setPos(startPos.x, startPos.y, startPos.z);

        this.setKiSpeed(1.2F);
        this.setSize(1.0F);

        level.playSound(
                null,
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                MainSounds.KI_KAME_FIRE.get(),
                SoundSource.PLAYERS,
                0.5F,
                0.8F + (this.random.nextFloat() * 0.2F)
        );
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEAM_LENGTH, 0.0F);
        this.entityData.define(FIXED_YAW, 0.0F);
        this.entityData.define(FIXED_PITCH, 0.0F);
    }

    public float getBeamLength() { return this.entityData.get(BEAM_LENGTH); }
    private void setBeamLength(float len) { this.entityData.set(BEAM_LENGTH, len); }
    public float getFixedYaw() { return this.entityData.get(FIXED_YAW); }
    public float getFixedPitch() { return this.entityData.get(FIXED_PITCH); }

    @Override
    public void tick() {
        this.baseTick();
        this.setDeltaMovement(0, 0, 0);

        if (!this.level().isClientSide) {
            Vec3 startPos = this.position();
            Vec3 dir = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            float currentLen = this.getBeamLength();

            if (this.tickCount % 5 == 0) {
                Vec3 tipPos = startPos.add(dir.scale(currentLen));

                this.level().playSound(null, tipPos.x, tipPos.y, tipPos.z, MainSounds.KI_KAME_FIRE.get(), SoundSource.HOSTILE, 0.5F, 1.0F);

                this.level().playSound(null, startPos.x, startPos.y, startPos.z, MainSounds.KI_KAME_FIRE.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
            }

            float targetLen = currentLen + this.getKiSpeed();
            Vec3 endPosRay = startPos.add(dir.scale(MAX_RANGE));

            HitResult hitResult = this.level().clip(new ClipContext(
                    startPos,
                    endPosRay,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));

            double distToWall = MAX_RANGE;

            if (hitResult.getType() != HitResult.Type.MISS) {
                distToWall = hitResult.getLocation().distanceTo(startPos);

                if (hitResult.getType() == HitResult.Type.BLOCK && targetLen >= distToWall) {
                    explodeAndDie(hitResult.getLocation());
                    return;
                }
                distToWall += 0.1D;
            }

            if (targetLen > distToWall) {
                targetLen = (float) distToWall;
            }

            this.setBeamLength(targetLen);

            damageEntitiesInBeam(startPos, dir, targetLen);

            if (this.tickCount > 100) {
                this.discard();
            }
        }
        else {
            spawnWaveParticles();
            spawnOriginSplash();
        }


        this.onKiTick();
    }


    private void spawnOriginSplash() {
        if (this.tickCount % 5 == 0) {

            double colorInt = (double) this.getColorBorde();
            double mySize = (double) this.getSize();

            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            this.level().addParticle(
                    MainParticles.KI_SPLASH_WAVE.get(),
                    x, y, z,
                    colorInt,
                    mySize,
                    0.0D
            );
        }
    }

    private void spawnWaveParticles() {
        float yaw = this.getFixedYaw();
        float pitch = this.getFixedPitch();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 startPos = this.position();
        float length = this.getBeamLength();

        float[] rgbMain = ColorUtils.rgbIntToFloat(this.getColor());

        if (length > 1.0F) {
            for(int i=0; i<2; i++) {
                double dist = this.random.nextDouble() * length;
                double spread = 0.5D;
                Vec3 pos = startPos.add(dir.scale(dist)).add(
                        (this.random.nextDouble() - 0.5) * spread,
                        (this.random.nextDouble() - 0.5) * spread,
                        (this.random.nextDouble() - 0.5) * spread
                );

                this.level().addParticle(MainParticles.KI_TRAIL.get(),
                        pos.x, pos.y, pos.z,
                        rgbMain[0], rgbMain[1], rgbMain[2]);
            }
        }
    }


    private void damageEntitiesInBeam(Vec3 start, Vec3 dir, float length) {
        Vec3 end = start.add(dir.scale(length));
        double searchRadius = this.getSize() * 1.0;
        AABB searchBox = new AABB(start, end).inflate(searchRadius);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchBox);

        int hitInterval = 20;

        for (LivingEntity target : targets) {
            if (!this.shouldDamage(target)) continue;
            if (target.is(this.getOwner())) continue;

            if (target.invulnerableTime > 0) continue;

            float hitPrecision = this.getSize() / 2.0F;
            AABB targetBox = target.getBoundingBox().inflate(hitPrecision);

            boolean intersects = targetBox.clip(start, end).isPresent() || targetBox.contains(start);

            if (intersects) {
				boolean wasHurt = target.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage());

                if (wasHurt) {
                    target.invulnerableTime = hitInterval;

                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double colorData = (double) this.getColorBorde();
                        double sizeData = (double) this.getSize();

                        serverLevel.sendParticles(
                                MainParticles.KI_SPLASH_WAVE.get(),
                                target.getX(),
                                target.getY() + (target.getBbHeight() / 2.0),
                                target.getZ(),
                                0,
                                colorData,
                                sizeData,
                                0.0D,
                                1.0D
                        );
                    }
                }
            }
        }
    }

    private void explodeAndDie(Vec3 pos) {
        boolean shouldDestroyBlocks = true;
        float radius = this.getSize() * 1.5F;

        AABB area = new AABB(pos, pos).inflate(radius);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : entities) {
            if (this.shouldDamage(target)) {
                double dist = target.distanceToSqr(pos);
                if (dist <= radius * radius) {
					target.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), this.getKiDamage());
                }
            }
        }

        this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1.0, 0.0, 0.0);
        this.level().playSound(null, pos.x, pos.y, pos.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 1.0F);

        Level.ExplosionInteraction interaction = shouldDestroyBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        this.level().explode(this, this.damageSources().explosion(this, this.getOwner()), null, pos.x, pos.y, pos.z, radius, false, interaction, false);
        this.discard();
    }
}