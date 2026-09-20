package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.combat.util.MultipartTargeting;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SPBlueHurricaneEntity extends AbstractKiProjectile {

    public static final String STRIKE_ID = "blue_hurricane";
    public static final int FIRING_WINDOW = 140;
    public static final int DEFAULT_COLOR_CORE = 0x7CC8FF;
    public static final int DEFAULT_COLOR_BORDER = 0x1F6BFF;
    public static final int DEFAULT_COLOR_OUTLINE = 0x0B2AB8;

    private static final double VORTEX_HEIGHT = 13.0D;
    private static final double OWNER_MAX_SPEED = 0.07D;
    private static final double OWNER_MAX_FALL_SPEED = 0.6D;

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(SPBlueHurricaneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(SPBlueHurricaneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FIRING_TICKS = SynchedEntityData.defineId(SPBlueHurricaneEntity.class, EntityDataSerializers.INT);

    public SPBlueHurricaneEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public SPBlueHurricaneEntity(Level level, LivingEntity owner) {
        super(MainEntities.SP_BLUE_HURRICANE.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
    }

    @Override
    public int getMaxHits() {
        return Math.max(1, this.getFiringTicks() / CONTINUOUS_HIT_INTERVAL);
    }

    public void setupHurricane(LivingEntity owner, float damage, float speed, int castTime) {
        this.setupHurricane(owner, damage, speed, castTime, FIRING_WINDOW);
    }

    public void setupHurricane(LivingEntity owner, float damage, float speed, int castTime, int firingTicks) {
        this.setFiringTicks(firingTicks);
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(DEFAULT_COLOR_CORE, DEFAULT_COLOR_BORDER, DEFAULT_COLOR_OUTLINE);

        this.setCastTime(castTime);
        this.setFiring(false);

        this.setPos(owner.getX(), owner.getY(), owner.getZ());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());

        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    @Override
    public void tick() {
        this.baseTick();

        Entity owner = this.getOwner();

        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        boolean isFiring = this.isFiring();

        if (!isFiring && this.tickCount >= this.getCastTime()) {
            this.setFiring(true);
            isFiring = true;

            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
            }
        }

        if (!isFiring) {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());

            if (owner instanceof Player) {
                this.slowOwner(owner);
            } else {
                double preserveGravity = owner.getDeltaMovement().y < 0 ? owner.getDeltaMovement().y : 0;
                owner.setDeltaMovement(0, preserveGravity, 0);
                owner.hasImpulse = true;
            }

            if (this.level().isClientSide) {
                float[] rgb = ColorUtils.rgbIntToFloat(this.getColorBorder());

                for (int i = 0; i < 4; i++) {
                    double radius = 4.0 + this.random.nextDouble() * 2.0;
                    double theta = this.random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * this.random.nextDouble() - 1);

                    double offsetX = radius * Math.sin(phi) * Math.cos(theta);
                    double offsetY = radius * Math.cos(phi) + 1.0;
                    double offsetZ = radius * Math.sin(phi) * Math.sin(theta);

                    double spawnX = this.getX() + offsetX;
                    double spawnY = this.getY() + offsetY;
                    double spawnZ = this.getZ() + offsetZ;

                    double vx = this.getX() - spawnX;
                    double vy = (this.getY() + 1.0) - spawnY;
                    double vz = this.getZ() - spawnZ;

                    Particle p = net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(
                            MainParticles.KI_TRAIL.get(),
                            spawnX, spawnY, spawnZ,
                            vx * 0.15D, vy * 0.15D, vz * 0.15D
                    );

                    if (p instanceof KiTrailParticle trail) {
                        trail.setKiColor(rgb[0], rgb[1], rgb[2]);
                        trail.setKiScale(1.5f + this.random.nextFloat() * 1.5f);
                    }
                }
            }
        }

        else {
            this.setPos(owner.getX(), owner.getY(), owner.getZ());
            this.setBoundingBox(this.getDimensions(this.getPose()).makeBoundingBox(this.position()));

            if (owner instanceof Player) this.slowOwner(owner);

            if (this.level().isClientSide) {
                float[] rgb = ColorUtils.rgbIntToFloat(this.getColorBorder());

                for (int i = 0; i < 4; i++) {
                    double v = this.random.nextDouble() * 0.85D;
                    double angle = this.random.nextDouble() * 2.0D * Math.PI;
                    double radius = 1.5D + 2.5D * Math.pow(v, 1.35D) + 1.3D * Math.exp(-v * 9.0D);

                    this.level().addParticle(
                            MainParticles.KI_TRAIL.get(),
                            this.getX() + Math.cos(angle) * radius,
                            this.getY() + v * VORTEX_HEIGHT,
                            this.getZ() + Math.sin(angle) * radius,
                            rgb[0], rgb[1], rgb[2]
                        );
                }
            }

            if (!this.level().isClientSide) {
                if (this.tickCount % 10 == 0) {
                    pulseDamage();
                }
            }
        }

        if (this.tickCount >= this.getCastTime() + this.getFiringTicks()) {
            this.discard();
        }
    }

    private void slowOwner(Entity owner) {
        Vec3 motion = owner.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double scale = horizontal > OWNER_MAX_SPEED ? OWNER_MAX_SPEED / horizontal : 1.0D;
        double y = Mth.clamp(motion.y, -OWNER_MAX_FALL_SPEED, OWNER_MAX_SPEED);
        if (scale < 1.0D || y != motion.y) owner.setDeltaMovement(motion.x * scale, y, motion.z * scale);
    }

    private void pulseDamage() {
        AABB area = this.getBoundingBox().inflate(4.5D, 9.0D, 4.5D); // Aumenté el radio a 4.5 para que jale desde más lejos

        List<LivingEntity> targets = MultipartTargeting.collectTargets(this.level(), area);

        for (LivingEntity target : targets) {
            if (shouldDamage(target) && !target.is(this.getOwner())) {

                // Damage is gated to ~1s pulses; the vortex pull still applies every call.
                if (this.getOwner() instanceof Player playerOwner) {
                    if (target.invulnerableTime <= 0 && target.hurt(MainDamageTypes.strikeAttack(this.level(), playerOwner, STRIKE_ID), this.getDamagePerHit())) {
                        target.invulnerableTime = CONTINUOUS_HIT_INTERVAL;
                    }
                } else {
                    this.applyContinuousDamage(target);
                }

                double dx = this.getX() - target.getX();
                double dz = this.getZ() - target.getZ();

                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 0) {
                    dx /= distance;
                    dz /= distance;
                }

                target.setDeltaMovement(dx * 0.4, 0.5D, dz * 0.4);
                target.hasImpulse = true;
                target.hurtMarked = true;
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST_TIME, 0);
        this.entityData.define(IS_FIRING, false);
        this.entityData.define(FIRING_TICKS, FIRING_WINDOW);
    }

    public int getCastTime() { return this.entityData.get(CAST_TIME); }
    public void setCastTime(int ticks) { this.entityData.set(CAST_TIME, ticks); }
    public boolean isFiring() { return this.entityData.get(IS_FIRING); }
    public void setFiring(boolean firing) { this.entityData.set(IS_FIRING, firing); }
    public int getFiringTicks() { return this.entityData.get(FIRING_TICKS); }
    public void setFiringTicks(int ticks) { this.entityData.set(FIRING_TICKS, Math.max(CONTINUOUS_HIT_INTERVAL, ticks)); }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("CastTime", this.getCastTime());
        pCompound.putBoolean("IsFiring", this.isFiring());
        pCompound.putInt("FiringTicks", this.getFiringTicks());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("CastTime")) this.setCastTime(pCompound.getInt("CastTime"));
        if (pCompound.contains("IsFiring")) this.setFiring(pCompound.getBoolean("IsFiring"));
        if (pCompound.contains("FiringTicks")) this.setFiringTicks(pCompound.getInt("FiringTicks"));
    }
}