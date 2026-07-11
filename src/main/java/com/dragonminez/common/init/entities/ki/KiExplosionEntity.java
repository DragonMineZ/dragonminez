package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.combat.util.MultipartTargeting;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiExplosionSplashParticle;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class KiExplosionEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CAST_EXPLOSION = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(KiExplosionEntity.class, EntityDataSerializers.INT);

    // Final Explosion "life sacrifice": on firing, the caster is drained down to FINAL_EXPLOSION_HP_FLOOR of
    // their max HP, and the blast's damage and radius scale up by how much life was actually spent. A near-dead
    // cast barely amplifies it; a full-HP cast is devastating to both the caster and everything nearby.
    private static final float FINAL_EXPLOSION_HP_FLOOR = 0.05F;   // caster is left at 5% HP
    private static final float FINAL_EXPLOSION_DAMAGE_GAIN = 1.8F; // up to ~2.7x damage at full HP
    private static final float FINAL_EXPLOSION_SIZE_GAIN = 0.8F;   // up to ~1.76x radius at full HP

    public KiExplosionEntity(EntityType<? extends KiExplosionEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setKiType(KiType.EXPLOSION);
    }

    public KiExplosionEntity(Level level, LivingEntity owner) {
        super(MainEntities.KI_EXPLOSION.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    public int getMaxHits() {
        return Math.max(1, this.firingWindowTicks() / 20);
    }

    // SETUP ITEMS & NPCS
    public void setupKiExplosion(LivingEntity owner, float damage, int colorMain, int colorBorder, int colorOutline, int castTime) {
        this.setSize(2.0F);
        this.setMaxRadius(10.0f);
        this.setColors(colorMain, colorBorder, colorOutline);
        this.setKiDamage(damage);
        this.entityData.set(OWNER_ID, owner.getId());
        this.setFiring(false);
        this.setFireTick(-1);
        this.setMaxLife(castTime + 100);
        this.setCastExplosion(castTime);
        this.updatePositionToOwner(owner);
        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    public void setupKiExplosion(LivingEntity owner, float damage, int colorMain, int colorBorder, int castTime) {
        this.setupKiExplosion(owner, damage, colorMain, colorBorder, 0xFFFFFF, castTime);
    }

    // SETUP PLAYERS
    public void setupExplosionPlayer(LivingEntity owner, float damage, float size, int colorMain, int colorBorder, int colorOutline) {
        this.setup(owner, damage, 2.0F, 0.0f, colorMain, colorBorder, colorOutline);
        this.setMaxRadius(size);
        this.entityData.set(OWNER_ID, owner.getId());
        this.setFiring(false);
        this.setFireTick(-1);
        this.setMaxLife(99999);
        this.setCastExplosion(40);
        this.updatePositionToOwner(owner);
        
    }

    public void setupExplosionPlayer(LivingEntity owner, float damage, float size, int colorMain, int colorBorder) {
        this.setupExplosionPlayer(owner, damage, size, colorMain, colorBorder, 0xFFFFFF);
    }

    public void fireHability(int finalMaxLife) {
        this.setFiring(true);
        this.setFireTick(this.tickCount);
        this.setMaxLife(this.tickCount + finalMaxLife);

        if (!this.level().isClientSide) {
            applyFinalExplosionSacrifice();
            createCrater(this.getMaxRadius()*1.2f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 0.7F, 1.2F);
        }

        if (this.getOwner() instanceof Player) this.triggerAnimationPacket("_fire");
    }

    /**
     * Final Explosion only: the caster burns their own life force to empower the blast. Drains the owner down to
     * {@link #FINAL_EXPLOSION_HP_FLOOR} of their max HP and scales both damage and radius by the fraction of max
     * HP that was actually consumed. Called once, server-side, at the moment the explosion fires. A caster already
     * at/below the floor sacrifices nothing and gets no bonus.
     */
    private void applyFinalExplosionSacrifice() {
        if (!"final_explosion".equals(this.getTechniqueId())) return;
        if (!(this.getOwner() instanceof LivingEntity ownerLiving) || !ownerLiving.isAlive()) return;

        float maxHp = ownerLiving.getMaxHealth();
        if (maxHp <= 0.0F) return;

        float floorHp = maxHp * FINAL_EXPLOSION_HP_FLOOR;
        float consumed = ownerLiving.getHealth() - floorHp;
        if (consumed <= 0.0F) return; // already at/below the floor: nothing to sacrifice, no bonus

        ownerLiving.setHealth(floorHp);

        float consumedFraction = consumed / maxHp; // 0 .. (1 - FINAL_EXPLOSION_HP_FLOOR)
        this.setKiDamage(this.getKiDamage() * (1.0F + consumedFraction * FINAL_EXPLOSION_DAMAGE_GAIN));
        this.setMaxRadius(this.getMaxRadius() * (1.0F + consumedFraction * FINAL_EXPLOSION_SIZE_GAIN));
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

        if (!this.isFiring() && this.getMaxLife() != 99999 && this.tickCount >= this.getCastExplosion()) {
            this.fireHability(this.getMaxLife() - this.tickCount);
        }

        Entity owner = this.getOwner();
        if (owner == null) {
            int ownerId = this.entityData.get(OWNER_ID);
            if (ownerId != -1) owner = this.level().getEntity(ownerId);
        }

        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) this.discard();
            return;
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
        int castTime = this.getCastExplosion();
        boolean isFiring = this.isFiring();

        Entity owner = this.getOwner();

        if (owner != null && owner.isAlive()) {
            owner.setDeltaMovement(0, 0, 0);
            owner.fallDistance = 0.0F;
            owner.hasImpulse = true;

            if (!isFiring && this.tickCount <= castTime / 2.0F) {
                double riseSpeed = 1.5D / (castTime / 2.0F);
                this.setPos(this.getX(), this.getY() + riseSpeed, this.getZ());
            }

            owner.setPos(this.getX(), this.getY() - (owner.getBbHeight() * 0.5F), this.getZ());
        }

        if (!this.level().isClientSide) {
            if (!isFiring) {
                if (this.tickCount == 1) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            MainSounds.KI_EXPLOSION_CHARGE.get(), SoundSource.HOSTILE, 0.7F, 1.0F);
                }
            } else {
                int activeTicks = this.tickCount - this.getFireTick();
                if (activeTicks % 70 == 0) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.HOSTILE, 0.2F, 1.2F);
                }
                if (activeTicks % 20 == 0) {
                    pulseDamage(maxRad);
                }
            }
        } else {
            spawnParticles(maxRad, isFiring);
        }
    }


    private void pulseDamage(float radius) {
        float damageRadius = radius*1.4f;

        AABB area = new AABB(
                this.getX() - damageRadius, this.getY() - damageRadius, this.getZ() - damageRadius,
                this.getX() + damageRadius, this.getY() + damageRadius, this.getZ() + damageRadius
        );

        List<LivingEntity> targets = MultipartTargeting.collectTargets(this.level(), area);

        for (LivingEntity target : targets) {
            if (this.shouldDamage(target)) {
                boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit() * 2.0f);

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

    private void spawnParticles(float maxRadius, boolean isFiring) {
        float[] rgbBorder = this.getRgbColorBorder();
        float[] rgbCore = ColorUtils.rgbIntToFloat(this.getColor());
        double floorY = this.getY() + 0.1D;

        if (isFiring) {
            if (this.tickCount % 10 == 0) {
                spawnSplashRingAt(this.getX(), floorY, this.getZ(), maxRadius * 1.4F, rgbCore);
            }
        } else {
            int particlesPerTick = 4;
            float gatherRadius = maxRadius * 1.3F;

            for (int i = 0; i < particlesPerTick; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 2.0 * gatherRadius;
                double offsetY = (this.random.nextDouble() - 0.5) * 2.0 * gatherRadius;
                double offsetZ = (this.random.nextDouble() - 0.5) * 2.0 * gatherRadius;

                double px = this.getX() + offsetX;
                double py = this.getY() + (this.getBbHeight() / 2.0F) + offsetY;
                double pz = this.getZ() + offsetZ;

                double speed = 0.12D;
                spawnAbsorbTrailAt(px, py, pz, -offsetX * speed, -offsetY * speed, -offsetZ * speed, rgbBorder);
            }
        }
    }

    private void spawnSplashRingAt(double x, double y, double z, float scale, float[] rgb) {
        Particle p = Minecraft.getInstance().particleEngine.createParticle(
                MainParticles.KI_EXPLOSION_SPLASH.get(),
                x, y, z,
                0.0D, 0.0D, 0.0D
        );

        if (p instanceof KiExplosionSplashParticle splash) {
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

        radius = this.scaledDestructionRadius(radius);
        BlockPos center = this.blockPosition();

        int r = (int) Math.ceil(radius);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    if (pos.distToCenterSqr(this.position()) <= radius * radius) {
                        BlockState state = this.level().getBlockState(pos);

                        if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0) {
                            this.setKiBlockToAir(pos, 3);
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
