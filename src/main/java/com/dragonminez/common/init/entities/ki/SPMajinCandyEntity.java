package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiTrailParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class SPMajinCandyEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(SPMajinCandyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(SPMajinCandyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(SPMajinCandyEntity.class, EntityDataSerializers.INT);

    public SPMajinCandyEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setNoGravity(true);
    }

    public SPMajinCandyEntity(Level level, LivingEntity owner) {
        super(MainEntities.SP_MAJIN_CANDY.get(), level);
        this.setOwner(owner);
        this.setNoGravity(true);
    }

    @Override
    public int getMaxHits() {
        return 1;
    }

    public void setupCandyBeam(LivingEntity owner, float damage, float speed, int castTime) {
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);

        this.setCastTime(castTime);
        this.setFiring(false);
        this.setTargetId(-1);

        this.setPos(owner.getX(), owner.getY() + owner.getEyeHeight() * 0.8, owner.getZ());
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

        if (!(this.getOwner() instanceof LivingEntity owner) || !owner.isAlive()) {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        boolean isFiring = this.isFiring();

        if (!isFiring && this.tickCount >= this.getCastTime()) {
            this.setFiring(true);
            isFiring = true;

            if (!this.level().isClientSide) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 1.5F, 1.2F);

                LivingEntity nearest = findNearestTarget(owner, 20.0D);
                if (nearest != null) {
                    this.setTargetId(nearest.getId());
                }
            }
        }

        if (!isFiring) {
            this.setPos(owner.getX(), owner.getY() + owner.getEyeHeight() * 0.8, owner.getZ());

            double preserveGravity = owner.getDeltaMovement().y < 0 ? owner.getDeltaMovement().y : 0;
            owner.setDeltaMovement(0, preserveGravity, 0);
            owner.hasImpulse = true;

            if (owner instanceof Player player) {
                player.xxa = 0.0F;
                player.zza = 0.0F;
            }

            if (this.level().isClientSide) {
                float[] rgb = ColorUtils.rgbIntToFloat(0xFF66CC);

                for (int i = 0; i < 4; i++) {
                    double radius = 3.0 + this.random.nextDouble() * 1.5;
                    double theta = this.random.nextDouble() * 2 * Math.PI;
                    double phi = Math.acos(2 * this.random.nextDouble() - 1);

                    double offsetX = radius * Math.sin(phi) * Math.cos(theta);
                    double offsetY = radius * Math.cos(phi);
                    double offsetZ = radius * Math.sin(phi) * Math.sin(theta);

                    double spawnX = this.getX() + offsetX;
                    double spawnY = this.getY() + offsetY;
                    double spawnZ = this.getZ() + offsetZ;

                    double vx = this.getX() - spawnX;
                    double vy = this.getY() - spawnY;
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
        } else {
            this.setPos(owner.getX(), owner.getY() + owner.getEyeHeight() * 0.8, owner.getZ());

            LivingEntity target = getTargetEntity();

            if (target != null && target.isAlive() && target.distanceTo(owner) <= 25.0D) {
                owner.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());

                if (!this.level().isClientSide) {
                    if (this.tickCount % 5 == 0) {
                        target.hurt(MainDamageTypes.kiblast(this.level(), this, owner), this.getKiDamage());

                        target.addEffect(new MobEffectInstance(MainEffects.CANDY.get(), 200, 0, false, true));
                    }
                }
            } else {
                if (!this.level().isClientSide) {
                    this.discard();
                }
            }
        }

        if (this.tickCount >= this.getCastTime() + 60) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide) {
            Entity hitEntity = result.getEntity();
            Entity owner = this.getOwner();

            if (hitEntity instanceof LivingEntity target && target != owner) {
                target.hurt(MainDamageTypes.kiblast(this.level(), this, (LivingEntity) owner), this.getKiDamage());

                target.addEffect(new MobEffectInstance(MainEffects.CANDY.get(), 100, 0, false, true));
                this.discard();
            }
        }
    }

    private LivingEntity findNearestTarget(LivingEntity owner, double range) {
        AABB area = owner.getBoundingBox().inflate(range);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, area);

        LivingEntity nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (LivingEntity target : targets) {
            if (shouldDamage(target) && !target.is(owner)) {
                double distance = owner.distanceToSqr(target);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = target;
                }
            }
        }
        return nearest;
    }

    public LivingEntity getTargetEntity() {
        int id = this.getTargetId();
        if (id != -1) {
            Entity entity = this.level().getEntity(id);
            if (entity instanceof LivingEntity) {
                return (LivingEntity) entity;
            }
        }
        return null;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST_TIME, 0);
        this.entityData.define(IS_FIRING, false);
        this.entityData.define(TARGET_ID, -1);
    }

    public int getCastTime() { return this.entityData.get(CAST_TIME); }
    public void setCastTime(int ticks) { this.entityData.set(CAST_TIME, ticks); }

    public boolean isFiring() { return this.entityData.get(IS_FIRING); }
    public void setFiring(boolean firing) { this.entityData.set(IS_FIRING, firing); }

    public int getTargetId() { return this.entityData.get(TARGET_ID); }
    public void setTargetId(int id) { this.entityData.set(TARGET_ID, id); }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("CastTime", this.getCastTime());
        pCompound.putBoolean("IsFiring", this.isFiring());
        pCompound.putInt("TargetId", this.getTargetId());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("CastTime")) this.setCastTime(pCompound.getInt("CastTime"));
        if (pCompound.contains("IsFiring")) this.setFiring(pCompound.getBoolean("IsFiring"));
        if (pCompound.contains("TargetId")) this.setTargetId(pCompound.getInt("TargetId"));
    }
}