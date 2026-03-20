package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiLightningParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiLaserEntity extends AbstractKiProjectile{

    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIXED_YAW = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIXED_PITCH = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> OFFSET_X = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Y = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Z = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(KiLaserEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float MAX_RANGE = 250.0F;

    public KiLaserEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public KiLaserEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_LASER.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;

        this.entityData.set(FIXED_YAW, owner.getYRot());
        this.entityData.set(FIXED_PITCH, owner.getXRot());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());

        Vec3 look = owner.getLookAngle();
        Vec3 startPos = owner.getEyePosition().add(look.scale(0.5));
        this.setPos(startPos.x, startPos.y, startPos.z);

        this.setKiSpeed(1.5F);
        this.setSize(1.5F);


    }

    //SETUPS ENTIDADES

    public void setupKiLaser(LivingEntity owner, float damage, float speed, int color, int colorBorder, int castTime) {
        this.setKiRenderType(0);
        this.setSize(1.0f);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder);

        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 80);
        this.setCastOffsets(0.3F, -0.1F, 0.5F);

        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    public void setupKiLaser(LivingEntity owner, float damage, float speed, int color, int castTime) {
        this.setupKiLaser(owner, damage, speed, color, color, castTime);
    }

    public void setupKiDodonpa(LivingEntity owner, float damage, float speed, int castTime) {
        this.setKiRenderType(0);
        this.setSize(0.5f);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(0xFFEB7A, 0xFFE657);

        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 80);
        this.setCastOffsets(0.3F, -0.1F, 0.5F);

        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    public void setupKiMakkankosanpo(LivingEntity owner, float damage, float speed, int castTime) {
        this.setKiRenderType(1);
        this.setSize(1.0f);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(0xFFE657, 0xFFE657);

        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 120); // El Makankosappo suele llegar más lejos
        this.setCastOffsets(0.3F, -0.1F, 0.5F);

        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    // SETUP PLAYERS
    public void setupKiLaserPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder) {
        this.setKiRenderType(0);
        this.setSize(1.0f);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder);

        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(20);
        this.setCastOffsets(0.3F, -0.1F, 0.5F);
        updatePositionRelativeToOwner(owner);
    }

    public void setupKiMakkankosanpoPlayer(LivingEntity owner, float damage, float speed){
        this.setKiRenderType(1);
        this.setSize(1.0f);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(0xFFE657, 0xFFE657);

        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(40); // Tiempo visual
        this.setCastOffsets(0.3F, -0.1F, 0.5F);
        updatePositionRelativeToOwner(owner);
    }

    public void setupKiDodonpaPlayer(LivingEntity owner, float damage, float speed) {
        this.setKiRenderType(0);
        this.setSize(0.5f);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(0xFFEB7A, 0xFFE657);

        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(20);
        this.setCastOffsets(0.3F, -0.1F, 0.5F);

        updatePositionRelativeToOwner(owner);
    }


    public void fireHability(int finalMaxLife) {
        this.setFiring(true);
        this.setMaxLife(this.tickCount + finalMaxLife);
        if (this.getOwner() instanceof LivingEntity livingOwner) {
            updatePositionRelativeToOwner(livingOwner);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_LASER.get(), SoundSource.PLAYERS, 0.4F, 1.0F + (this.random.nextFloat() * 0.2F));
        }
    }


    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEAM_LENGTH, 0.0F);
        this.entityData.define(FIXED_YAW, 0.0F);
        this.entityData.define(FIXED_PITCH, 0.0F);
        this.entityData.define(CAST_TIME, 0);
        this.entityData.define(OFFSET_X, 0.0F);
        this.entityData.define(OFFSET_Y, 0.0F);
        this.entityData.define(OFFSET_Z, 0.0F);
        this.entityData.define(IS_FIRING, false);
    }

    public float getBeamLength() {return this.entityData.get(BEAM_LENGTH);}
    private void setBeamLength(float len) {this.entityData.set(BEAM_LENGTH, len);}
    public float getFixedYaw() {return this.entityData.get(FIXED_YAW);}
    public float getFixedPitch() {return this.entityData.get(FIXED_PITCH);}
    public void setCastTime(int ticks) { this.entityData.set(CAST_TIME, ticks); }
    public int getCastTime() { return this.entityData.get(CAST_TIME); }
    public void setCastOffsets(float x, float y, float z) {
        this.entityData.set(OFFSET_X, x);
        this.entityData.set(OFFSET_Y, y);
        this.entityData.set(OFFSET_Z, z);
    }
    public boolean isFiring() { return this.entityData.get(IS_FIRING); }
    public void setFiring(boolean firing) { this.entityData.set(IS_FIRING, firing); }

    @Override
    public void tick() {
        this.baseTick();

        if (!this.isFiring() && this.getMaxLife() != 99999 && this.tickCount >= this.getCastTime()) {
            this.fireHability(this.getMaxLife() - this.tickCount);
        }

        boolean isFiring = this.isFiring();

        if (!isFiring) {
            if (this.getOwner() instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
                updatePositionRelativeToOwner(livingOwner);
                this.setDeltaMovement(0, 0, 0);

                this.entityData.set(FIXED_YAW, livingOwner.getYRot());
                this.entityData.set(FIXED_PITCH, livingOwner.getXRot());
                this.setYRot(livingOwner.getYRot());
                this.setXRot(livingOwner.getXRot());
            } else if (!this.level().isClientSide) {
                this.discard();
                return;
            }
        } else {
            this.setDeltaMovement(0, 0, 0);

            if (!this.level().isClientSide) {
                Vec3 startPos = this.position();
                Vec3 dir = Vec3.directionFromRotation(this.getFixedPitch(), this.getFixedYaw());

                float currentLen = this.getBeamLength();
                float targetLen = currentLen + this.getKiSpeed();

                Vec3 endPosRay = startPos.add(dir.scale(MAX_RANGE));

                HitResult hitResult = this.level().clip(new ClipContext(
                        startPos, endPosRay, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

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

                if (this.tickCount > this.getMaxLife()) {
                    this.discard();
                    return;
                }
            }
        }

        if (this.level().isClientSide) {
            spawnLaserParticles();
        }

        this.onKiTick();
    }


    private void updatePositionRelativeToOwner(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 offset = right.scale(this.entityData.get(OFFSET_X)).add(up.scale(this.entityData.get(OFFSET_Y))).add(look.scale(this.entityData.get(OFFSET_Z)));
        Vec3 newPos = owner.getEyePosition().add(offset);
        this.setPos(newPos.x, newPos.y, newPos.z);
    }


    private void spawnLaserParticles() {
        float yaw = this.getFixedYaw();
        float pitch = this.getFixedPitch();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 startPos = this.position();
        float length = this.getBeamLength();
        float scale = this.getSize();

        float[] rgbMain = ColorUtils.rgbIntToFloat(this.getColor());
        float[] rgbBorder = this.getRgbColorBorder();

        if (this.tickCount == 1) {
            this.level().addParticle(MainParticles.KI_SPLASH.get(),
                    startPos.x, startPos.y, startPos.z,
                    rgbBorder[0], rgbBorder[1], rgbBorder[2]);


            if (this.getKiRenderType() == 1) { //rayos makkankosanpo
                this.spawnInitialLightning();
            }

        }

        for (int i = 0; i < 3; i++) {
            double absDist = scale * 2.0;

            double theta = this.random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * this.random.nextDouble() - 1);

            double sx = absDist * Math.sin(phi) * Math.cos(theta);
            double sy = absDist * Math.sin(phi) * Math.sin(theta);
            double sz = absDist * Math.cos(phi);

            double vx = -sx * 0.15;
            double vy = -sy * 0.15;
            double vz = -sz * 0.15;

            Particle p = Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_SHEDDING.get(),
                    startPos.x + sx,
                    startPos.y + sy,
                    startPos.z + sz,
                    vx, vy, vz
            );

            if (p instanceof com.dragonminez.common.init.particles.KiSheddingParticle kiParticle) {
                kiParticle.setKiColor(rgbBorder[0], rgbBorder[1], rgbBorder[2]);
            }
        }

        if (length > 0.5F) {
            Vec3 tipPos = startPos.add(dir.scale(length));

            for (int i = 0; i < 3; i++) {
                double radius = scale * 0.8;

                double theta = this.random.nextDouble() * 2 * Math.PI;
                double phi = Math.acos(2 * this.random.nextDouble() - 1);

                double dx = radius * Math.sin(phi) * Math.cos(theta);
                double dy = radius * Math.sin(phi) * Math.sin(theta);
                double dz = radius * Math.cos(phi);

                double vx = dx * 0.25;
                double vy = dy * 0.25;
                double vz = dz * 0.25;

                Particle p = Minecraft.getInstance().particleEngine.createParticle(
                        MainParticles.KI_TRAIL.get(),
                        tipPos.x + dx,
                        tipPos.y + dy,
                        tipPos.z + dz,
                        vx, vy, vz
                );

                if (p instanceof com.dragonminez.common.init.particles.KiTrailParticle trail) {
                    trail.setKiColor(rgbBorder[0], rgbBorder[1], rgbBorder[2]);
                    trail.setKiScale(scale * 0.6F);
                }
            }
        }
    }

    private void spawnInitialLightning() {
        float scale = this.getSize();
        float[] rgbMain = ColorUtils.rgbIntToFloat(this.getColor());
        float[] rgbBorder = ColorUtils.rgbIntToFloat(0xEF00FF);
        Vec3 startPos = this.position();

        int lightningCount = 25;
        for (int i = 0; i < lightningCount; i++) {
            float[] chosenColor = (i % 2 == 0) ? rgbMain : rgbBorder;
            spawnLightningAt(startPos, scale * 2.5F, chosenColor);
        }
    }

    private void spawnLightningAt(Vec3 pos, float scaleRadius, float[] rgb) {
        double offsetX = (this.random.nextDouble() - 0.5D) * scaleRadius * 2.0D;
        double offsetY = (this.random.nextDouble() - 0.5D) * scaleRadius * 2.0D;
        double offsetZ = (this.random.nextDouble() - 0.5D) * scaleRadius * 2.0D;

        double vx = offsetX * 0.1D;
        double vy = offsetY * 0.1D;
        double vz = offsetZ * 0.1D;

        Particle p = Minecraft.getInstance().particleEngine.createParticle(
                MainParticles.KI_LIGHTNING.get(),
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                vx, vy, vz
        );

        if (p instanceof KiLightningParticle lightning) {
            lightning.setLightningColor(rgb[0], rgb[1], rgb[2]);
            float randomScale = scaleRadius * 0.8F + (this.random.nextFloat() * scaleRadius * 0.5F);
            lightning.setLightningScale(randomScale);
        }
    }

    private void damageEntitiesInBeam(Vec3 start, Vec3 dir, float length) {
        Vec3 end = start.add(dir.scale(length));
        double searchRadius = this.getSize() * 0.5;
        AABB searchBox = new AABB(start, end).inflate(searchRadius);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchBox);

        int hitInterval = 10;

        for (LivingEntity target : targets) {
            if (!this.shouldDamage(target)) continue;
            if (target.is(this.getOwner())) continue;
            if (target.invulnerableTime > 0) continue;

            float hitPrecision = this.getSize() / 3.0F;
            AABB targetBox = target.getBoundingBox().inflate(hitPrecision);
            var hit = targetBox.clip(start, end);

            if (hit.isPresent() || targetBox.contains(start)) {
                boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit());

                if (wasHit) {
                    this.onSuccessfulHit(target);
                    target.invulnerableTime = hitInterval;

                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double colorData = (double) this.getColor();
                        double sizeData = (double) this.getSize();
                        serverLevel.sendParticles(
                                MainParticles.KI_SPLASH_WAVE.get(),
                                target.getX(), target.getY() + (target.getBbHeight() / 2.0), target.getZ(),
                                0, colorData, sizeData, 0.0D, 1.0D
                        );
                    }
                }
            }
        }
    }

    private void explodeAndDie(Vec3 pos) {
        boolean shouldDestroyBlocks = true;
        float radius = this.getSize();
        AABB area = new AABB(pos, pos).inflate(radius);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : entities) {
            if (this.shouldDamage(target)) {
                double dist = target.distanceToSqr(pos);
                if (dist <= radius * radius) {
                    boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit());
                    if (wasHit) this.onSuccessfulHit(target);
                }
            }
        }

        this.level().addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1.0, 0.0, 0.0);
        this.level().playSound(null, pos.x, pos.y, pos.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.7F);
        Level.ExplosionInteraction interaction = shouldDestroyBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        this.level().explode(this, this.damageSources().explosion(this, this.getOwner()), null, pos.x, pos.y, pos.z, radius, false, interaction, false);
        this.discard();
    }
}