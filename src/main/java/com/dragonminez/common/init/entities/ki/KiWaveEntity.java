package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
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

    private static final float MAX_RANGE = 200.0F;

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
        Vec3 startPos = owner.getEyePosition().add(look.scale(0.8)); // Sale un poco más adelante
        this.setPos(startPos.x, startPos.y, startPos.z);

        this.setKiSpeed(1.2F);
        this.setSize(2.5F);

        level.playSound(
                null,
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                MainSounds.KI_LASER.get(),
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

            if (this.tickCount > 100) { // Duración de la ola
                this.discard();
            }
        }
        else {
            spawnWaveParticles();
        }

        this.onKiTick();
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
                double spread = 0.5D; // Más dispersión
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
        double searchRadius = this.getSize() * 0.8; // Radio de búsqueda más generoso
        AABB searchBox = new AABB(start, end).inflate(searchRadius);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchBox);
        int hitInterval = 4; // Golpea un poco más rápido que el láser

        for (LivingEntity target : targets) {
            if (!this.shouldDamage(target)) continue;
            if (target.invulnerableTime > 10) continue;

            float hitPrecision = this.getSize() / 2.0F; // Hitbox más grande
            AABB targetBox = target.getBoundingBox().inflate(hitPrecision);

            var hit = targetBox.clip(start, end);

            if (hit.isPresent()) {
                float totalDps = this.getKiDamage();
                float hitsPerSecond = 20.0F / (float) hitInterval;
                float damagePerHit = totalDps / hitsPerSecond;

                target.invulnerableTime = 0;
                boolean wasHurt = target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), damagePerHit);
                if (wasHurt) {
                    target.invulnerableTime = hitInterval;
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
                    target.hurt(this.damageSources().explosion(this, this.getOwner()), this.getKiDamage());
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