package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiBarrierEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> CURRENT_SIZE = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FIRE_TICK = SynchedEntityData.defineId(KiBarrierEntity.class, EntityDataSerializers.INT);

    private static final int GROW_DURATION = 25;
    private static final int MAX_LIFESPAN = 100;
    private static final float MAX_SIZE = 3.0F;

    public KiBarrierEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public KiBarrierEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_BARRIER.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;

        this.centerOnOwner();

//        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
//                MainSounds.KI_EXPLOSION_IMPACT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public void setupKiBarrier(LivingEntity owner, int color, int colorBorder, int castTime) {
        this.setColors(color, colorBorder);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 100);

        this.setFiring(false);
        this.setFireTick(-1);

        if (!this.level().isClientSide) this.level().addFreshEntity(this);
    }

    // SETUP PLAYERS
    public void setupBarrierPlayer(LivingEntity owner, float damage, float size, int colorMain, int colorBorder) {
        this.setup(owner, damage, size, 0.0f, colorMain, colorBorder);

        this.setFiring(false);
        this.setFireTick(-1);
        this.setMaxLife(99999);
        this.setCastTime(40);
    }

    public void fireHability(int finalMaxLife) {
        this.setFiring(true);
        this.setFireTick(this.tickCount);
        this.setMaxLife(this.tickCount + finalMaxLife);

        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    MainSounds.KI_EXPLOSION_IMPACT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.2F);
        }
    }

    @Override
    public int getMaxHits() {
        return this.getMaxLife() / 20;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_SIZE, 0.1F);
        this.entityData.define(CAST_TIME, 0);
        this.entityData.define(IS_FIRING, false);
        this.entityData.define(FIRE_TICK, -1);
    }

    public float getCurrentSize() {
        return this.entityData.get(CURRENT_SIZE);
    }
    public void setCurrentSize(float size) {
        this.entityData.set(CURRENT_SIZE, size);
    }
    public void setCastTime(int ticks) { this.entityData.set(CAST_TIME, ticks); }
    public int getCastTime() { return this.entityData.get(CAST_TIME); }
    public boolean isFiring() { return this.entityData.get(IS_FIRING); }
    public void setFiring(boolean firing) { this.entityData.set(IS_FIRING, firing); }
    public int getFireTick() { return this.entityData.get(FIRE_TICK); }
    public void setFireTick(int tick) { this.entityData.set(FIRE_TICK, tick); }


    private void centerOnOwner() {
        if (this.getOwner() instanceof LivingEntity owner) {
            double bodyCenterY = owner.getY() + (owner.getBbHeight() / 2.0D);

            this.setPos(owner.getX(), bodyCenterY - (this.getBbHeight() * 0.5D), owner.getZ());
        }
    }

    @Override
    public void tick() {
        this.baseTick();

        // AUTO-DISPARO NPCs
        if (!this.isFiring() && this.getMaxLife() != 99999 && this.tickCount >= this.getCastTime()) {
            this.fireHability(this.getMaxLife() - this.tickCount);
        }

        if (this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
            centerOnOwner();
            this.setDeltaMovement(0, 0, 0);
        } else {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        boolean isFiring = this.isFiring();

        if (!this.level().isClientSide) {
            if (!isFiring) {
                this.setCurrentSize(0.1F);
            } else {
                int activeTicks = this.tickCount - this.getFireTick();
                float maxSize = this.getSize(); // Usa el tamaño real de la habilidad
                float progress = (float) activeTicks / 10.0F;
                float size = Math.min(maxSize, 1.0F + (maxSize * progress));
                this.setCurrentSize(size);

                pushEntitiesAway();
            }
        }

        if (this.level().isClientSide) {
            if (!isFiring) {
                spawnAbsorptionParticles();
            } else {
                spawnBarrierParticles();
            }
        }

        if (this.tickCount >= this.getMaxLife()) {
            if (!this.level().isClientSide) this.discard();
        }
    }

    private void pushEntitiesAway() {
        float radius = this.getCurrentSize() * 0.8F;
        AABB area = this.getBoundingBox().inflate(0.3D);

        List<Entity> targets = this.level().getEntities(this, area);

        for (Entity target : targets) {
            if (target.is(this.getOwner())) continue;
            if (!(target instanceof LivingEntity) && !(target instanceof Projectile)) continue;

            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dy = target.getY() - (this.getY() + this.getBbHeight() * 0.5);

            Vec3 vec = new Vec3(dx, dy, dz).normalize().scale(1.5);

            target.setDeltaMovement(vec);
            target.hasImpulse = true;

            if (target instanceof Projectile) {
                target.remove(RemovalReason.DISCARDED);
            }
        }
    }

    private void spawnBarrierParticles() {
        float size = this.getCurrentSize();
        if (size < 0.2F) return;
        float[] rgb = ColorUtils.rgbIntToFloat(this.getColor());

        for (int i = 0; i < 3; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2;
            double phi = this.random.nextDouble() * Math.PI;
            double r = size / 2.0;

            double dx = r * Math.sin(phi) * Math.cos(theta);
            double dy = r * Math.cos(phi);
            double dz = r * Math.sin(phi) * Math.sin(theta);

            Particle p = Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_TRAIL.get(),
                    this.getX() + dx, this.getY() + (this.getBbHeight() / 2.0) + dy, this.getZ() + dz,
                    dx * 0.1D, dy * 0.1D, dz * 0.1D
            );

            if (p instanceof KiTrailParticle trail) {
                trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                trail.setKiScale(size * 0.3f);
            }
        }
    }

    private void spawnAbsorptionParticles() {
        float[] rgb = ColorUtils.rgbIntToFloat(this.getColor());
        for (int i = 0; i < 3; i++) {
            double r = 2.5D;
            double theta = this.random.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * this.random.nextDouble() - 1);

            double dx = r * Math.sin(phi) * Math.cos(theta);
            double dy = r * Math.sin(phi) * Math.sin(theta);
            double dz = r * Math.cos(phi);

            Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                    MainParticles.KI_TRAIL.get(),
                    this.getX() + dx, this.getY() + (this.getBbHeight() * 0.5) + dy, this.getZ() + dz,
                    -dx * 0.15, -dy * 0.15, -dz * 0.15
            );

            if (p instanceof KiTrailParticle trail) {
                trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                trail.setKiScale(0.3f);
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        float size = this.getCurrentSize();
        return EntityDimensions.scalable(size, size);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (CURRENT_SIZE.equals(pKey)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
    }

    @Override
    protected void onKiTick() {
    }
}