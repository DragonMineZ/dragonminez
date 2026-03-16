package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.*;
import com.dragonminez.common.init.particles.KiLightningParticle;
import com.dragonminez.common.init.particles.KiSheddingParticle;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiWaveEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIXED_YAW = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIXED_PITCH = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> CAST_WAVE = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CAST_SIZE = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> OFFSET_X = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Y = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Z = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> CONTINUOUS_FOLLOW = SynchedEntityData.defineId(KiWaveEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float MAX_RANGE = 300.0F; // uff

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
                null, owner.getX(), owner.getY(), owner.getZ(),
                MainSounds.KI_KAME_FIRE.get(), SoundSource.PLAYERS,
                0.1F, 0.8F + (this.random.nextFloat() * 0.2F)
        );
    }

    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    public void setupKiWave(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size, int castTime) {
        this.setKiRenderType(0);

        this.setSize(size);
        this.setCastSize(size / 2);

        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder);

        this.setMaxLife(castTime*2);
        this.setCastWave(castTime);

        this.playInitialSound(MainSounds.KI_KAME_FIRE.get());

        this.setCastOffsets(0.4F, 0.6F, 0.0F);
        updatePositionRelativeToOwner(owner, true);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    public void setupKiWave(LivingEntity owner, float damage, float speed, int color, float size, int castTime) {
        this.setupKiWave(owner, damage, speed, color, color, size, castTime);
    }

    public void setupKiHame(LivingEntity owner, float damage, float speed, float size, int castTime) {
        this.setKiRenderType(1);

        this.setSize(size);
        this.setCastSize(size / 2);

        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(0x4FF7FF, 0x4FF7FF);

        this.setMaxLife(castTime*2);
        this.setCastWave(castTime);
        this.playInitialSound(MainSounds.KI_KAME_FIRE.get());

        this.setCastOffsets(0.4F, 0.6F, 0.0F);
        updatePositionRelativeToOwner(owner, true);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    public void setupKiGalickGun(LivingEntity owner, float damage, float speed, float size, int castTime) {
        this.setKiRenderType(2);

        this.setSize(size);
        this.setCastSize(size / 2);

        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(0xCE10E3, 0xAE10E3);

        this.setMaxLife(castTime*2);
        this.setCastWave(castTime);
        this.playInitialSound(MainSounds.KI_KAME_FIRE.get());

        this.setCastOffsets(0.4F, 0.6F, 0.0F);
        updatePositionRelativeToOwner(owner, true);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    private void updatePositionRelativeToOwner(LivingEntity owner, boolean isCasting) {
        Vec3 look = owner.getLookAngle();
        Vec3 newPos;

        if (isCasting) {
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = right.cross(look).normalize();

            Vec3 offset = right.scale(this.entityData.get(OFFSET_X))
                    .add(up.scale(this.entityData.get(OFFSET_Y)))
                    .add(look.scale(this.entityData.get(OFFSET_Z)));

            newPos = owner.getEyePosition().add(offset);
        } else {
            double chestY = owner.getY() + (owner.getBbHeight() / 2.0F);

            newPos = new Vec3(owner.getX(), chestY, owner.getZ()).add(look.scale(0.8D));
        }

        this.setPos(newPos.x, newPos.y, newPos.z);

        this.entityData.set(FIXED_YAW, owner.getYRot());
        this.entityData.set(FIXED_PITCH, owner.getXRot());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BEAM_LENGTH, 0.0F);
        this.entityData.define(FIXED_YAW, 0.0F);
        this.entityData.define(FIXED_PITCH, 0.0F);
        this.entityData.define(CAST_WAVE, 100);
        this.entityData.define(CAST_SIZE, 1.0F);
        this.entityData.define(OFFSET_X, 0.0F);
        this.entityData.define(OFFSET_Y, 0.0F);
        this.entityData.define(OFFSET_Z, 0.0F);

        this.entityData.define(CONTINUOUS_FOLLOW, false);
    }

    public float getBeamLength() { return this.entityData.get(BEAM_LENGTH); }
    private void setBeamLength(float len) { this.entityData.set(BEAM_LENGTH, len); }
    public float getFixedYaw() { return this.entityData.get(FIXED_YAW); }
    public float getFixedPitch() { return this.entityData.get(FIXED_PITCH); }
    public int getCastWave() { return this.entityData.get(CAST_WAVE); }
    public void setCastWave(int ticks) { this.entityData.set(CAST_WAVE, ticks); }
    public float getCastSize() { return this.entityData.get(CAST_SIZE); }
    public void setCastSize(float size) { this.entityData.set(CAST_SIZE, size); }
    public void setCastOffsets(float offsetX, float offsetY, float offsetZ) {
        this.entityData.set(OFFSET_X, offsetX);
        this.entityData.set(OFFSET_Y, offsetY);
        this.entityData.set(OFFSET_Z, offsetZ);
    }
    public boolean isContinuousFollow() { return this.entityData.get(CONTINUOUS_FOLLOW); }
    public void setContinuousFollow(boolean follow) { this.entityData.set(CONTINUOUS_FOLLOW, follow); }

    @Override
    public void tick() {
        this.baseTick();
        this.setDeltaMovement(0, 0, 0);

        int castTime = this.getCastWave();
        boolean isCasting = this.tickCount <= castTime;

        var owner = this.getOwner();
        if (owner instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
            if (isCasting) {
                updatePositionRelativeToOwner(livingOwner, true);
            } else {
                if (this.isContinuousFollow()) {
                    updatePositionRelativeToOwner(livingOwner, false);
                } else {
                    if (this.tickCount == castTime + 1) {
                        updatePositionRelativeToOwner(livingOwner, false);
                    }
                }
            }
        } else if (!this.level().isClientSide) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {

            if (isCasting) {
                if (this.tickCount == 1) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
                }
            } else {
                Vec3 startPos = this.position();
                Vec3 dir = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
                float currentLen = this.getBeamLength();
                float currentSpeed = this.getKiSpeed();

                if (this.tickCount == castTime + 1) {
                    this.level().playSound(null, startPos.x, startPos.y, startPos.z, MainSounds.KI_KAME_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }

                if ((this.tickCount - castTime) % 5 == 0) {
                    Vec3 tipPosForSound = startPos.add(dir.scale(currentLen));
                    this.level().playSound(null, tipPosForSound.x, tipPosForSound.y, tipPosForSound.z, MainSounds.KI_KAME_FIRE.get(), SoundSource.HOSTILE, 0.1F, 1.0F);
                }

                float targetLen = currentLen + currentSpeed;
                Vec3 tipPos = startPos.add(dir.scale(targetLen));

                this.destroyBlocksAtTip(tipPos);

                HitResult hitResult = this.level().clip(new ClipContext(
                        startPos.add(dir.scale(currentLen)), tipPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
                ));

                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockPos hitPos = ((BlockHitResult)hitResult).getBlockPos();
                    if (this.level().getBlockState(hitPos).getExplosionResistance(this.level(), hitPos, null) >= 1000) {
                        targetLen = (float) hitResult.getLocation().distanceTo(startPos);
                        currentSpeed = 0.0F;
                        this.setKiSpeed(currentSpeed);
                    }
                }

                this.setBeamLength(targetLen);
                damageEntitiesInBeam(startPos, dir, targetLen);

                if (currentSpeed < 0.05F || this.tickCount > this.getMaxLife()) {
                    explodeAndDie(startPos.add(dir.scale(targetLen)));
                    return;
                }
            }
        } else {
            if (!isCasting) {
                spawnWaveParticles();
                spawnOriginSplash();
                spawnLightningParticles(false);
            } else {
                spawnLightningParticles(true);
            }
        }

        this.onKiTick();
    }

    private void spawnLightningParticles(boolean isCasting) {
        if (this.getKiRenderType() != 2) return;

        float length = this.getBeamLength();
        float yaw = this.getFixedYaw();
        float pitch = this.getFixedPitch();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);

        Vec3 startPos = this.position();
        Vec3 endPos = startPos.add(dir.scale(length));

        float scale = this.getSize();
        float[] borderColor = ColorUtils.rgbIntToFloat(this.getColorBorde());

        int rayosPorTick = 8;

        for (int i = 0; i < rayosPorTick; i++) {
            if (isCasting) {
                if (this.random.nextFloat() < 1.00F) {
                    spawnLightningAt(startPos, scale * 1.5F, borderColor);
                    spawnLightningAt(startPos, scale * 1.5F, ColorUtils.darkenColor(borderColor, 0.5F));
                }
            } else {
                if (length > 1.0F && this.random.nextFloat() < 1.00F) {
                    spawnLightningAt(endPos, scale * 3.0F, borderColor);
                    spawnLightningAt(endPos, scale * 3.0F, ColorUtils.darkenColor(borderColor, 0.5F));
                }
            }
        }
    }

    private void spawnLightningAt(Vec3 pos, float scaleRadius, float[] rgb) {
        double offsetX = (this.random.nextDouble() - 0.5D) * scaleRadius * 1.8D;
        double offsetY = (this.random.nextDouble() - 0.5D) * scaleRadius * 1.8D;
        double offsetZ = (this.random.nextDouble() - 0.5D) * scaleRadius * 1.8D;

        double vx = offsetX * 0.1D;
        double vy = offsetY * 0.1D;
        double vz = offsetZ * 0.1D;

        net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
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

    private boolean destroyBlocksAtTip(Vec3 tipPos) {
        if (!MainGameRules.canKiGrief(this.level(), BlockPos.containing(tipPos), this.getOwner())) {
            return false;
        }

        boolean hitSomething = false;
        float eatRadius = this.getSize() * 1.5F;
        int bRad = Math.round(eatRadius);
        BlockPos center = BlockPos.containing(tipPos);
        Level level = this.level();

        for (int x = -bRad; x <= bRad; x++) {
            for (int y = -bRad; y <= bRad; y++) {
                for (int z = -bRad; z <= bRad; z++) {
                    if (x * x + y * y + z * z <= eatRadius * eatRadius) {
                        BlockPos targetPos = center.offset(x, y, z);
                        if (!level.getBlockState(targetPos).isAir() && level.getBlockState(targetPos).getExplosionResistance(level, targetPos, null) < 1000) {
                            level.destroyBlock(targetPos, false);
                            hitSomething = true;
                            if (level instanceof ServerLevel serverLevel) {
                                if (this.random.nextFloat() < 0.25F) {
                                    serverLevel.sendParticles(
                                            ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                            targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                                            1, 0.5D, 0.5D, 0.5D, 0.05D
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
        return hitSomething;
    }

    private void spawnOriginSplash() {
        if (this.tickCount % 5 == 0) {
            double colorInt = (double) this.getColorBorde();
            double mySize = (double) this.getSize();
            this.level().addParticle(MainParticles.KI_SPLASH_WAVE.get(), this.getX(), this.getY(), this.getZ(), colorInt, mySize, 0.0D);
        }
    }

    private void spawnWaveParticles() {
        float length = this.getBeamLength();
        if (length <= 1.0F) return;

        float yaw = this.getFixedYaw();
        float pitch = this.getFixedPitch();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 startPos = this.position();
        Vec3 tipPos = startPos.add(dir.scale(length));

        float scale = this.getSize();
        float[] borderColor = ColorUtils.rgbIntToFloat(this.getColorBorde());
        float pr = borderColor[0], pg = borderColor[1], pb = borderColor[2];

        for (int i = 0; i < 4; i++) {
            double radius = scale * 1.2;
            double theta = this.random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * this.random.nextDouble() - 1);
            double dx = radius * Math.sin(phi) * Math.cos(theta);
            double dy = radius * Math.sin(phi) * Math.sin(theta);
            double dz = radius * Math.cos(phi);
            double vx = dx * 0.15;
            double vy = dy * 0.15;
            double vz = dz * 0.15;

            net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_TRAIL.get(),
                    tipPos.x + dx, tipPos.y + dy, tipPos.z + dz,
                    vx, vy, vz
            );

            if (p instanceof KiTrailParticle trail) {
                trail.setKiColor(pr, pg, pb);
                trail.setKiScale(scale);
            }
        }

        for (int i = 0; i < 5; i++) {
            double absDist = scale * 2.0;
            double angle = this.random.nextDouble() * Math.PI * 2;
            double sx = Math.cos(angle) * absDist;
            double sz = Math.sin(angle) * absDist;
            double sy = (this.random.nextDouble() - 0.5) * 2.0 * absDist;

            net.minecraft.client.particle.Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_SHEDDING.get(),
                    startPos.x + sx, startPos.y + sy, startPos.z + sz,
                    -sx * 0.15, -sy * 0.15, -sz * 0.15
            );

            if (p instanceof KiSheddingParticle kiParticle) {
                kiParticle.setKiColor(borderColor[0], borderColor[1], borderColor[2]);
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
                boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit());

                if (wasHit) {
                    this.onSuccessfulHit(target);
                    target.invulnerableTime = hitInterval;
                    this.setKiSpeed(this.getKiSpeed() * 0.75F);

                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double colorData = (double) this.getColorBorde();
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
        float explosionRadius = this.getSize() * 2.5F;

        AABB damageArea = new AABB(pos, pos).inflate(explosionRadius);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, damageArea);
        for (LivingEntity target : targets) {
            if (this.shouldDamage(target)) {
                boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit());
                if (wasHit) this.onSuccessfulHit(target);
            }
        }

        if (!this.level().isClientSide) {
            BlockPos center = BlockPos.containing(pos);

            if (MainGameRules.canKiGrief(this.level(), center, this.getOwner())) {
                int blockRadius = Math.round(explosionRadius);
                for (int x = -blockRadius; x <= blockRadius; x++) {
                    for (int y = -blockRadius; y <= blockRadius; y++) {
                        for (int z = -blockRadius; z <= blockRadius; z++) {
                            if (x * x + y * y + z * z <= explosionRadius * explosionRadius) {
                                BlockPos targetPos = center.offset(x, y, z);
                                if (this.level().getBlockState(targetPos).getExplosionResistance(this.level(), targetPos, null) < 1000) {
                                    this.level().setBlock(targetPos, Blocks.AIR.defaultBlockState(), 2);
                                }
                            }
                        }
                    }
                }
            }

            float visualParticleSize = explosionRadius * 1.8F;
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        MainParticles.KI_EXPLOSION.get(),
                        pos.x, pos.y, pos.z,
                        0, (double) visualParticleSize, 0.0D, 0.0D, 1.0D
                );
                serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 5.0F, 0.6F);

                KiExplosionVisualEntity explosionVisual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), this.level());
                explosionVisual.setPos(pos.x, pos.y - 0.5, pos.z);
                explosionVisual.setupExplosion(this.getColorBorde(), this.getSize() * 2.5F);
                this.level().addFreshEntity(explosionVisual);
            }
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("CastWave", getCastWave());
        pCompound.putFloat("CastSize", getCastSize());
        pCompound.putFloat("OffsetX", this.entityData.get(OFFSET_X));
        pCompound.putFloat("OffsetY", this.entityData.get(OFFSET_Y));
        pCompound.putFloat("OffsetZ", this.entityData.get(OFFSET_Z));
        pCompound.putBoolean("ContinuousFollow", isContinuousFollow());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("CastWave")) setCastWave(pCompound.getInt("CastWave"));
        if (pCompound.contains("CastSize")) setCastSize(pCompound.getFloat("CastSize"));
        if (pCompound.contains("OffsetX")) this.entityData.set(OFFSET_X, pCompound.getFloat("OffsetX"));
        if (pCompound.contains("OffsetY")) this.entityData.set(OFFSET_Y, pCompound.getFloat("OffsetY"));
        if (pCompound.contains("OffsetZ")) this.entityData.set(OFFSET_Z, pCompound.getFloat("OffsetZ"));
        if (pCompound.contains("ContinuousFollow")) setContinuousFollow(pCompound.getBoolean("ContinuousFollow"));
    }
}