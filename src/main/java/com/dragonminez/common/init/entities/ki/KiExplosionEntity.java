package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class KiExplosionEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CAST_EXPLOSION = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.INT);
    public static final int DURATION = 240;

    public KiExplosionEntity(EntityType<? extends KiExplosionEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public KiExplosionEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_EXPLOSION.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    public void setupExplosion(LivingEntity owner, float damage, int colorMain, int colorBorder) {
        this.setup(owner, damage, 0.1F, 0.0f, colorMain, colorBorder);
        this.setMaxRadius(7.0f);
        this.entityData.set(OWNER_ID, owner.getId());
        this.updatePositionToOwner(owner);
    }

    public void setupKiExplosion(LivingEntity owner, float damage, int colorMain, int colorBorder, int castTime) {
        this.setSize(2.0F); // aca es hasta que escalado crecera mientras castea
        this.setMaxRadius(5.0f); //el rango máximo cuando termina el casteo
        this.setColors(colorMain, colorBorder);
        this.setKiDamage(damage);
        this.entityData.set(OWNER_ID, owner.getId());
        this.setMaxLife(castTime*2);
        this.setCastExplosion(castTime);
        this.updatePositionToOwner(owner);
        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MAX_RADIUS, 15.0f);
        this.entityData.define(OWNER_ID, -1);
        this.entityData.define(CAST_EXPLOSION, 100);
    }

    @Override
    public void tick() {
        this.baseTick();

        Entity owner = this.getOwner();

        if (owner == null) {
            int ownerId = this.entityData.get(OWNER_ID);
            if (ownerId != -1) {
                owner = this.level().getEntity(ownerId);
            }
        }
        if (owner != null && owner.isAlive()) {
            updatePositionToOwner(owner);
        } else {
            if (!this.level().isClientSide) this.discard();
        }

        if (this.tickCount >= this.getMaxLife()) {
            this.discard();
            return;
        }

        this.onKiTick();
    }

    private void updatePositionToOwner(Entity owner) {
        double x = owner.getX();
        double y = owner.getY() + (owner.getBbHeight() * 0.5F);
        double z = owner.getZ();

        this.setPos(x, y, z);
        this.setDeltaMovement(0, 0, 0);

        this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));
    }

    @Override
    protected void onKiTick() {
        float maxRad = this.getMaxRadius();
        float baseSize = this.getSize();
        int castTime = this.getCastExplosion();
        float expansionTime = 10.0F;

        Entity owner = this.getOwner();
        if (owner != null && owner.isAlive()) {
            owner.setDeltaMovement(owner.getDeltaMovement().x, 0, owner.getDeltaMovement().z);
            owner.fallDistance = 0.0F;
            owner.hasImpulse = true;

            double startY = owner.getY();
            float halfCastTime = castTime / 2.0F;
            float targetHeightRise = 1.5F;

            if (this.tickCount <= halfCastTime) {
                double riseSpeed = (double) targetHeightRise / halfCastTime;
                owner.setPos(owner.getX(), owner.getY() + riseSpeed, owner.getZ());
            } else {
            }
        }

        float currentRadius;

        if (this.tickCount <= castTime) {
            currentRadius = baseSize / 2.0F;
        } else if (this.tickCount <= castTime + expansionTime) {
            float progress = (this.tickCount - castTime) / expansionTime;
            currentRadius = (baseSize / 2.0F) + ((maxRad - (baseSize / 2.0F)) * progress);
        } else {
            currentRadius = maxRad;
        }

        if (!this.level().isClientSide) {
            if (this.tickCount == 1) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.HOSTILE, 0.2F, 1.0F);
            }
            if (this.tickCount == castTime) {
                createCrater(maxRad*2);
            }

            if (this.tickCount >= castTime) {
                int activeTicks = this.tickCount - castTime;

                if (activeTicks == 0 || activeTicks % 70 == 0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 0.2F, 1.2F);
                }

                if (this.tickCount % 20 == 0) {
                    pulseDamage(currentRadius);
                }
            }
        } else {
            spawnParticles(currentRadius);
        }
    }

    private void pulseDamage(float radius) {
        float damageRadius = radius * 1.2F;

        AABB area = new AABB(
                this.getX() - damageRadius, this.getY() - damageRadius, this.getZ() - damageRadius,
                this.getX() + damageRadius, this.getY() + damageRadius, this.getZ() + damageRadius
        );

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (this.shouldDamage(target)) {
                boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit());

                if (wasHit && !this.isHeal()) {
                    this.onSuccessfulHit(target);
                    double dx = target.getX() - this.getX();
                    double dz = target.getZ() - this.getZ();
                    target.knockback(0.2D, -dx, -dz);
                } else if (wasHit && this.isHeal()) {
                    this.onSuccessfulHit(target);
                }
            }
        }
    }

    @Override
    public boolean shouldDamage(Entity target) {
        return super.shouldDamage(target);
    }

    private void spawnParticles(float radius) {
        float[] rgbBorder = ColorUtils.rgbIntToFloat(this.getColorBorde());
        float[] rgbCore = ColorUtils.rgbIntToFloat(this.getColor());

        double floorY = this.getY() + 0.1D;
        int castTime = this.getCastExplosion();

        if (this.tickCount % 10 == 0) {
            float massiveScale = radius * 1.4F;

            spawnSplashRingAt(this.getX(), floorY, this.getZ(), massiveScale, rgbCore);
        }

        if (this.tickCount <= castTime) {
            int particlesPerTick = 4;
            float gatherRadius = this.getMaxRadius() * 1.5F;

            for (int i = 0; i < particlesPerTick; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 2.0 * gatherRadius;
                double offsetY = (this.random.nextDouble() - 0.5) * 2.0 * gatherRadius;
                double offsetZ = (this.random.nextDouble() - 0.5) * 2.0 * gatherRadius;

                double px = this.getX() + offsetX;
                double py = this.getY() + (this.getBbHeight() / 2.0F) + offsetY;
                double pz = this.getZ() + offsetZ;

                double speed = 0.12D;
                double vx = -offsetX * speed;
                double vy = -offsetY * speed;
                double vz = -offsetZ * speed;

                spawnAbsorbTrailAt(px, py, pz, vx, vy, vz, rgbBorder);
            }

            int dustPerTick = 10;
            float dustSpread = this.getMaxRadius() * 0.3F;

//            for (int i = 0; i < dustPerTick; i++) {
//                double angle = this.random.nextDouble() * Math.PI * 2.0;
//                double distance = this.random.nextDouble() * dustSpread;
//
//                double px = this.getX() + (Math.cos(angle) * distance);
//                double pz = this.getZ() + (Math.sin(angle) * distance);
//
//                double upwardForce = 0.1D + (this.random.nextDouble() * 0.2D);
//                double outwardForce = 0.05D;
//                double vx = Math.cos(angle) * outwardForce; // Se aleja del centro en X
//                double vy = upwardForce;                    // Se eleva en Y
//                double vz = Math.sin(angle) * outwardForce; // Se aleja del centro en Z
//
//                this.level().addParticle(
//                        (SimpleParticleType) MainParticles.DUST.get(),
//                        px, floorY, pz,
//                        vx, vy, vz
//                );
//            }
        }
    }

    private void spawnSplashRingAt(double x, double y, double z, float scale, float[] rgb) {
        Particle p = Minecraft.getInstance().particleEngine.createParticle(
                MainParticles.KI_EXPLOSION_SPLASH.get(),
                x, y, z,
                0.0D, 0.0D, 0.0D
        );

        if (p instanceof com.dragonminez.common.init.particles.KiExplosionSplashParticle splash) {
            splash.setSplashColor(rgb[0], rgb[1], rgb[2]);
            splash.setSplashScale(scale);
        }
    }

    private void spawnAbsorbTrailAt(double x, double y, double z, double vx, double vy, double vz, float[] rgb) {
        Particle p = Minecraft.getInstance().particleEngine.createParticle(
                MainParticles.KI_TRAIL.get(),
                x, y, z,
                vx, vy, vz
        );

        if (p instanceof KiTrailParticle trail) {
            trail.setColor(rgb[0], rgb[1], rgb[2]);
            trail.setKiScale(1.0f);
        }
    }

    private void createCrater(float radius) {
        if (this.level().isClientSide) return;

        BlockPos center = this.blockPosition();

        if (!MainGameRules.canKiGrief(this.level(), center, this.getOwner())) {
            return;
        }

        int r = (int) Math.ceil(radius);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    if (pos.distToCenterSqr(this.position()) <= radius * radius) {
                        BlockState state = this.level().getBlockState(pos);

                        if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0) {
                            this.level().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    public void setMaxRadius(float radius) { this.entityData.set(MAX_RADIUS, radius); }
    public float getMaxRadius() { return this.entityData.get(MAX_RADIUS); }
    public void setCastExplosion(int ticks) { this.entityData.set(CAST_EXPLOSION, ticks); }
    public int getCastExplosion() { return this.entityData.get(CAST_EXPLOSION); }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("MaxRadius", getMaxRadius());
        pCompound.putInt("CastExplosion", getCastExplosion());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("MaxRadius")) setMaxRadius(pCompound.getFloat("MaxRadius"));
        if (pCompound.contains("CastExplosion")) setCastExplosion(pCompound.getInt("CastExplosion"));
    }
}