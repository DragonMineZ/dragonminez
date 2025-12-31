package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
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

    public void setupExplosion(LivingEntity owner, float damage, int colorMain, int colorBorder) {
        this.setup(owner, damage, 0.1F, colorMain, colorBorder);

        this.setMaxRadius(7.0f);

        this.entityData.set(OWNER_ID, owner.getId());

        this.updatePositionToOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MAX_RADIUS, 15.0f);
        this.entityData.define(OWNER_ID, -1); // -1 = Sin dueño
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

        if (this.tickCount >= DURATION) {
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
        float currentRadius;
        if (this.tickCount <= GROW_TIME) {
            float progress = (float) this.tickCount / (float) GROW_TIME;
            currentRadius = maxRad * progress;
        } else {
            currentRadius = maxRad;
        }
        this.setSize(currentRadius * 2.0F);
        if (!this.level().isClientSide) {
            spawnParticles(currentRadius);

            if (this.tickCount == 1) {
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        MainSounds.KI_EXPLOSION_CHARGE.get(),
                        SoundSource.HOSTILE,
                        2.0F, 1.0F
                );
            }

            if (this.tickCount == GROW_TIME) {
                this.level().playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        MainSounds.KI_EXPLOSION_IMPACT.get(),
                        SoundSource.HOSTILE,
                        2.0F,
                        1.2F
                );
            }

            if (this.tickCount >= GROW_TIME) {
                if (this.tickCount % 20 == 0) {
                    pulseDamage(currentRadius);
                }
            }
        }
    }

    private void pulseDamage(float radius) {
        float damageRadius = radius * 1.8F;

        AABB area = new AABB(
                this.getX() - damageRadius, this.getY() - damageRadius, this.getZ() - damageRadius,
                this.getX() + damageRadius, this.getY() + damageRadius, this.getZ() + damageRadius
        );

        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (this.shouldDamage(target)) {
                target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.getKiDamage());

                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                target.knockback(0.2D, -dx, -dz); //empuje
            }
        }
    }

    @Override
    public boolean shouldDamage(Entity target) {
        Entity owner = this.getOwner();

        if (target == owner) return false;

        if (owner instanceof LivingEntity livingOwner) {
            if (livingOwner.isAlliedTo(target)) {
                return false;
            }
            if (target.getType() == livingOwner.getType()) {
                return false;
            }
        }
        return true;
    }

    private void spawnParticles(float radius) {
        if (this.level() instanceof ServerLevel serverLevel) {
            int color = this.getColor();
            if (this.tickCount % 10 == 0) {
                serverLevel.sendParticles((SimpleParticleType) MainParticles.KI_EXPLOSION_SPLASH.get(),
                        this.getX(), this.getY(), this.getZ(),
                        0,
                        (double) radius,
                        (double) this.getColorBorde(),
                        0.0,
                        1.0);
            }

            serverLevel.sendParticles((SimpleParticleType) MainParticles.KI_EXPLOSION_FLASH.get(),
                    this.getX(), this.getY(), this.getZ(), 0, (double) this.getId(), (double) color, (double) radius, 1.0);
        }
    }

    public void setMaxRadius(float radius) { this.entityData.set(MAX_RADIUS, radius); }
    public float getMaxRadius() { return this.entityData.get(MAX_RADIUS); }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("MaxRadius", getMaxRadius());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("MaxRadius")) setMaxRadius(pCompound.getFloat("MaxRadius"));
    }
}