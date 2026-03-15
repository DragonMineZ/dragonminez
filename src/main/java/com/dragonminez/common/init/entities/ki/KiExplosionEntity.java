package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
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
import net.minecraft.world.phys.AABB;

import java.util.List;

public class KiExplosionEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CAST_EXPLOSION = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.INT);
    public static final int DURATION = 240;
    public static final int GROW_TIME = 100;

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

    public void setupKiExplosion(LivingEntity owner, float damage, int colorMain, int colorBorder) {
        this.setSize(2.0F);
        this.setMaxRadius(5.0f);
        this.setColors(colorMain, colorBorder);
        this.setKiDamage(damage);
        this.entityData.set(OWNER_ID, owner.getId());
        this.setMaxLife(200);
        this.setCastExplosion(100);
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
                        MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            }

            if (this.tickCount >= castTime) {
                int activeTicks = this.tickCount - castTime;

                if (activeTicks == 0 || activeTicks % 70 == 0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 1.0F, 1.2F);
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

        if (this.tickCount % 10 == 0) {
            float massiveScale = radius * 1.4F;

            spawnSplashRingAt(this.getX(), floorY, this.getZ(), massiveScale, rgbCore);
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