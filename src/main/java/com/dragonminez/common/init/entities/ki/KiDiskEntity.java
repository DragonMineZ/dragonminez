package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.*;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiDiskEntity extends AbstractKiProjectile {

    private boolean hasSpawnedSplash = false;

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(KiDiskEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> OFFSET_X = SynchedEntityData.defineId(KiDiskEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Y = SynchedEntityData.defineId(KiDiskEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Z = SynchedEntityData.defineId(KiDiskEntity.class, EntityDataSerializers.FLOAT);

    public KiDiskEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.refreshDimensions();
    }

    public KiDiskEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_DISC.get(), level);
        this.setOwner(owner);

        Vec3 look = owner.getLookAngle();
        Vec3 spawnPos = owner.getEyePosition().add(look.scale(0.5));
        this.setPos(spawnPos.x, spawnPos.y - 0.2D, spawnPos.z);

        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                MainSounds.KI_DISK_CHARGE.get(), SoundSource.PLAYERS, 0.5F, 1.0F + (this.random.nextFloat() * 0.2F));
        this.refreshDimensions();
    }

    public void setupKiDisk(LivingEntity owner, float damage, float speed, int color, float size, int castTime) {
        this.setOwner(owner);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color);
        this.setMaxLife(castTime * 2);
        this.setCastTime(castTime);
        this.setCastOffsets(0.4F, 0.7F, 0.2F);

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    public void setupKiDiskPlayer(LivingEntity owner, float damage, float speed, int color, float size, int maxLife) {
        this.setOwner(owner);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color);
        this.setMaxLife(maxLife);
        this.setCastTime(0);
        this.setCastOffsets(0.4F, 0.7F, 0.2F);
        this.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, this.getKiSpeed(), 0.0F);
    }

    @Override
    public void tick() {
        super.tick();

        int castTime = this.getCastTime();
        boolean isCasting = castTime > 0 && this.tickCount <= castTime;

        if (isCasting) {
            if (this.getOwner() instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
                updatePositionRelativeToOwner(livingOwner);
                this.setDeltaMovement(0, 0, 0);

                this.setXRot(-90.0F);
            } else if (!this.level().isClientSide) {
                this.discard();
            }
        } else if (castTime > 0 && this.tickCount == castTime + 1) {
            if (this.getOwner() instanceof LivingEntity livingOwner) {
                this.shootFromRotation(livingOwner, livingOwner.getXRot(), livingOwner.getYRot(), 0.0F, this.getKiSpeed(), 0.0F);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_DISK_CHARGE.get(), SoundSource.PLAYERS, 0.7F, 1.5F);
            }
        }
    }

    private void updatePositionRelativeToOwner(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();
        Vec3 offset = right.scale(this.entityData.get(OFFSET_X)).add(up.scale(this.entityData.get(OFFSET_Y))).add(look.scale(this.entityData.get(OFFSET_Z)));
        Vec3 newPos = owner.getEyePosition().add(offset);
        this.setPos(newPos.x, newPos.y, newPos.z);
    }

    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        float scale = this.getSize();
        float width = 1.0F * scale;
        float height = Math.max(0.0625F * scale, 0.15F);
        return EntityDimensions.scalable(width, height);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        this.refreshDimensions();
    }

    @Override
    protected void onKiTick() {
        if (!this.level().isClientSide && this.getOwner() == null) {
            this.discard();
            return;
        }

        int castTime = this.getCastTime();
        boolean isCasting = castTime > 0 && this.tickCount <= castTime;

        if (!this.level().isClientSide) {
            if (!isCasting) {
                if (this.tickCount % 12 == 0) {
                    this.playSound(MainSounds.KI_DISK_CHARGE.get(), 0.3F, 1.2F);
                }
                if (this.tickCount % 10 == 0) {
                    pulseAreaDamage();
                }
            }
        }

        if (this.level().isClientSide) {
            float[] rgb = ColorUtils.rgbIntToFloat(this.getColor());
            float scale = this.getSize();

            if (isCasting) {
                for (int i = 0; i < 4; i++) {
                    double spawnRadius = scale * 2.5D;
                    double theta = this.random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * this.random.nextDouble() - 1);

                    double dx = spawnRadius * Math.sin(phi) * Math.cos(theta);
                    double dy = spawnRadius * Math.sin(phi) * Math.sin(theta);
                    double dz = spawnRadius * Math.cos(phi);

                    double vx = -dx * 0.15D;
                    double vy = -dy * 0.15D;
                    double vz = -dz * 0.15D;

                    Particle p = Minecraft.getInstance().particleEngine.createParticle(
                            MainParticles.KI_TRAIL.get(),
                            this.getX() + dx,
                            this.getY() + (this.getBbHeight() / 2.0) + dy,
                            this.getZ() + dz,
                            vx, vy, vz
                    );

                    if (p instanceof KiTrailParticle trail) {
                        trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                        trail.setKiScale(scale * 0.3f);
                    }
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double radius = scale * 0.8D;

                    double dx = Math.cos(angle) * radius;
                    double dz = Math.sin(angle) * radius;
                    double dy = (this.random.nextDouble() - 0.5D) * 0.1D;

                    double vx = -this.getDeltaMovement().x * 0.3;
                    double vy = -this.getDeltaMovement().y * 0.3;
                    double vz = -this.getDeltaMovement().z * 0.3;

                    Particle p = Minecraft.getInstance().particleEngine.createParticle(
                            MainParticles.KI_TRAIL.get(),
                            this.getX() + dx,
                            this.getY() + (this.getBbHeight() / 2.0) + dy,
                            this.getZ() + dz,
                            vx, vy, vz
                    );

                    if (p instanceof KiTrailParticle trail) {
                        trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                        trail.setKiScale(scale * 0.6f);
                    }
                }
            }

            if (!hasSpawnedSplash) {
                this.level().addParticle(
                        MainParticles.KI_SPLASH.get(),
                        this.getX(), this.getY() + (this.getBbHeight() / 2.0), this.getZ(),
                        rgb[0], rgb[1], rgb[2]
                );
                this.hasSpawnedSplash = true;
            }
        }
    }

    private void pulseAreaDamage() {
        AABB area = this.getBoundingBox().inflate(0.5D, 0.2D, 0.5D);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : nearby) {
            if (this.shouldDamage(target)) {
                boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit());
                if (wasHit) this.onSuccessfulHit(target);
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST_TIME, 0);
        this.entityData.define(OFFSET_X, 0.0F);
        this.entityData.define(OFFSET_Y, 0.0F);
        this.entityData.define(OFFSET_Z, 0.0F);
    }

    public void setCastTime(int ticks) { this.entityData.set(CAST_TIME, ticks); }
    public int getCastTime() { return this.entityData.get(CAST_TIME); }
    public void setCastOffsets(float x, float y, float z) {
        this.entityData.set(OFFSET_X, x);
        this.entityData.set(OFFSET_Y, y);
        this.entityData.set(OFFSET_Z, z);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("CastTime", getCastTime());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("CastTime")) setCastTime(pCompound.getInt("CastTime"));
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);

        if (!this.level().isClientSide) {
            Entity targetEntity = pResult.getEntity();
            if (this.shouldDamage(targetEntity)) {
                boolean wasHit = this.applyDamageOrHeal(targetEntity, this.getDamagePerHit());

                if (wasHit) {
                    this.onSuccessfulHit(targetEntity);
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double colorData = (double) this.getColor();
                        double sizeData = (double) this.getBbWidth();
                        serverLevel.sendParticles(
                                MainParticles.KI_SPLASH_WAVE.get(),
                                targetEntity.getX(), targetEntity.getY() + (targetEntity.getBbHeight() / 2.0), targetEntity.getZ(),
                                0, colorData, sizeData, 0.0D, 1.0D
                        );
                    }
                }
            }
        }
    }
}