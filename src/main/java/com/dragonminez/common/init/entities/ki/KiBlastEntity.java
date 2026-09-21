package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.combat.util.MultipartTargeting;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.particles.KiSheddingParticle;
import com.dragonminez.common.init.particles.KiTrailParticle;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiBlastEntity extends AbstractKiProjectile {

    private boolean hasSpawnedSplash = false;

    public static final int RENDER_SOUL_PUNISHER = 4;
    public static final int RENDER_FAKE_MOON = 11;
    public static final int RENDER_ASSAULT_RAIN = 12;
    public static final int RENDER_BLASTER_METEOR = 13;
    private static final int BLASTER_METEOR_SHOTS = 8;
    private static final int BLASTER_METEOR_AIMED_SHOTS = 2;
    private static final double BLASTER_METEOR_RISE_SPEED = 0.11D;
    private static final double BLASTER_METEOR_MAX_RISE = 4.0D;
    private double blasterMeteorRisen;
    public static final int ASSAULT_RAIN_DELAY = 10;
    private static final int ASSAULT_RAIN_RISE_LIFE = 12;
    private static final int ASSAULT_RAIN_DROP_LIFE = 60;
    private static final int ASSAULT_RAIN_RISERS = 3;
    private static final int ASSAULT_RAIN_DROPS = 4;
    private static final float ASSAULT_RAIN_RISE_SPEED = 1.8F;
    private static final double ASSAULT_RAIN_RADIUS = 6.0D;
    private static final double ASSAULT_RAIN_HEIGHT = 22.0D;
    private static final double ASSAULT_RAIN_AIM_RANGE = 40.0D;
    private static final double ASSAULT_RAIN_SLANT = 0.15D;
    private transient int volleyTargetId = -1;
    private transient boolean assaultRainRiser = false;
    private static final double FAKE_MOON_CLIMB_BLOCKS = 30.0D;
    private static final double FAKE_MOON_CLIMB_SPEED = 1.0D;
    private static final int FAKE_MOON_GLOW_TICKS = 20 * 20;
    private transient double fakeMoonStartY = Double.NaN;
    private transient int fakeMoonFrozenTick = -1;

    private boolean isDetonating = false;
    private float currentDetonationRadius = 0.0F;
    private float maxDetonationRadius = 0.0F;
    private static final float MIN_BLAST_CRATER = 2.5F;

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> OFFSET_X = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Y = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Z = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_CONTROLLABLE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_PARKED = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> PARKED_DISTANCE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> STRETCH = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);

    private static final int DESTRUCTION_STRETCH_START = 14;
    private static final int DESTRUCTION_SPREAD_START = 26;
    private static final int DESTRUCTION_GROW_END = 50;
    private static final float DESTRUCTION_MAX_STRETCH = 1.9F;
    private static final double DESTRUCTION_SPREAD_ACCEL = 0.012D;

    private boolean destructionBall;
    private int destructionAge;
    private float destructionStartSize;
    private float destructionEndSize;
    private Vec3 destructionSpread = Vec3.ZERO;

    public KiBlastEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public KiBlastEntity(Level level, LivingEntity owner) {
        this(MainEntities.KI_BLAST.get(), level);
        this.setOwner(owner);
        this.setKiType(getKiRenderType());
    }

    @Override
    public int getMaxHits() {
        return Math.max(1, this.firingWindowTicks() / 20);
    }

    @Override
    public ClashRole getClashRole() {
        return this.getKiRenderType() == 0 ? ClashRole.MINOR : ClashRole.NONE;
    }

    private float calcCenterOffsetY(float sphereSize) {
        return -(sphereSize / 2.0F);
    }

    private float calcForwardOffset(LivingEntity owner, float sphereSize) {
        float ownerHalfWidth = owner.getBbWidth() / 2.0F;
        float sphereRadius = sphereSize / 2.0F;
        return ownerHalfWidth + sphereRadius + 0.1F;
    }

    public void setupKiBlastPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(1);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(100);
        this.setCastOffsets(0.0f, calcCenterOffsetY(size) + 0.5f, calcForwardOffset(owner, size));
        updatePositionRelativeToOwner(owner);

        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupKiBlastPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size) {
        this.setupKiBlastPlayer(owner, damage, speed, color, colorBorder, 0xFFFFFF, size);
    }

    public void setupKiBlastPlayer(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupKiBlastPlayer(owner, damage, speed, color, color, 0xFFFFFF, size);
    }

    public void setupSoulPunisherPlayer(LivingEntity owner, float damage, float speed, int color, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(RENDER_SOUL_PUNISHER);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(100);
        this.setCastOffsets(0.0f, 0.0F, 2.0F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupFakeMoonPlayer(LivingEntity owner, float speed, int color, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(RENDER_FAKE_MOON);
        this.setSize(size);
        this.setKiDamage(0.0F);
        this.setKiSpeed(speed);
        this.setColors(color, color, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(60);
        this.setCastOffsets(0.0F, 0.5F, 0.5F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    private boolean isSoulPunisher() {
        return "soul_punisher".equals(this.getTechniqueId());
    }

    private float soulPunisherDamage(Entity target, float baseDamage) {
        boolean reduced = false;
        if (target instanceof Player targetPlayer) {
            var resolved = StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).resolve();
            if (resolved.isPresent() && resolved.get().getResources().getAlignment() >= 41) reduced = true;
        }
        if (!reduced && this.getOwner() instanceof Player ownerPlayer
                && TargetHelper.getRelation(ownerPlayer, target) == TargetHelper.Relation.NEUTRAL) {
            reduced = true;
        }
        return reduced ? baseDamage * 0.25F : baseDamage;
    }

    public void setupKiLargeBlastPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(2);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(40);
        this.setCastOffsets(0.0F, 5.5F, 0.0F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupKiLargeBlastPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size) {
        this.setupKiLargeBlastPlayer(owner, damage, speed, color, colorBorder, 0xFFFFFF, size);
    }

    public void setupKiLargeBlastPlayer(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupKiLargeBlastPlayer(owner, damage, speed, color, color, 0xFFFFFF, size);
    }

    public void setupInvertedKiBlastPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(3);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(40);
        this.setCastOffsets(0.0f, -0.5F, 0.5F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupInvertedKiBlastPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size) {
        this.setupInvertedKiBlastPlayer(owner, damage, speed, color, colorBorder, 0xFFFFFF, size);
    }

    public void setupInvertedKiBlastPlayer(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupInvertedKiBlastPlayer(owner, damage, speed, color, color, 0xFFFFFF, size);
    }

    public void setupKiSoulsPlayer(LivingEntity owner, float damage, float speed, int color, int colorOutline) {
        this.setOwner(owner);
        this.setKiRenderType(4);
        this.setSize(0.6F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(color, color, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(40);
        this.setCastOffsets(0.0f, -0.5F, 0.5F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupKiSoulsPlayer(LivingEntity owner, float damage, float speed, int color) {
        this.setupKiSoulsPlayer(owner, damage, speed, color, 0xFFFFFF);
    }

    public void setupKiGenkiPlayer(LivingEntity owner, float damage, float speed, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(5);
        this.setSize(size);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0xC4FFFD, 0x00F8FF, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(100);
        this.setCastOffsets(0.0F, 5.5F, 0.0F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupKiGenkiPlayer(LivingEntity owner, float damage, float speed, float size) {
        this.setupKiGenkiPlayer(owner, damage, speed, 0xFFFFFF, size);
    }

    public void setupKiNovaPlayer(LivingEntity owner, float damage, float speed, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(6);
        this.setSize(size);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0xFF7438, 0xC92620, colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(100);
        this.setCastOffsets(0.0F, 2.5F, 0.0F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupKiNovaPlayer(LivingEntity owner, float damage, float speed, float size) {
        this.setupKiNovaPlayer(owner, damage, speed, 0x800E0E, size);
    }

    public void setupKiNovaCoolerPlayer(LivingEntity owner, float damage, float speed, float size) {
        this.setOwner(owner);
        this.setKiRenderType(6);
        this.setSize(size);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0xFF3866, 0xA3143A, 0x4A0316);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(100);
        this.setCastOffsets(0.0F, 2.5F, 0.0F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }

    }



    public void setupKiDeathBallPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(7);
        this.setSize(size);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(color, ColorUtils.darkenColor(colorBorder, 0.5f), colorOutline);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(60);
        this.setCastOffsets(0.0F, 5.5F, 0.0F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupKiDeathBallPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size) {
        this.setupKiDeathBallPlayer(owner, damage, speed, color, colorBorder, 0xFFFFFF, size);
    }

    public void setupKiDeathBallPlayer(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupKiDeathBallPlayer(owner, damage, speed, color, color, 0xFFFFFF, size);
    }

    public void setupSokidanPlayer(LivingEntity owner, float damage, float speed, int color, int colorOutline, float size) {
        this.setOwner(owner);
        this.setKiRenderType(8);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color, colorOutline);
        this.setControllable(true);
        this.setFiring(false);
        this.setMaxLife(99999);
        this.setCastTime(40);
        this.setCastOffsets(0.0F, 0.5F, 0.5F);
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupSokidanPlayer(LivingEntity owner, float damage, float speed, int color, float size) {
        this.setupSokidanPlayer(owner, damage, speed, color, 0xFFFFFF, size);
    }

    public void setupKiVolleyPlayer(LivingEntity owner, float damage, float speed, int color, int colorOutline, int castTime, float size) {
        this.setOwner(owner);
        this.setKiRenderType(9);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color, colorOutline);
        this.setFiring(false);
        this.setCastTime(8);
        this.setMaxLife(castTime + 100);
        this.setCastOffsets(0.0f, 0f, 0.7f);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
        
    }

    public void setupKiVolleyPlayer(LivingEntity owner, float damage, float speed, int color, int castTime, float size) {
        this.setupKiVolleyPlayer(owner, damage, speed, color, 0xFFFFFF, castTime, size);
    }

    public void setupKiSmall(LivingEntity owner, float damage, float speed, int color, int colorOutline) {
        this.setOwner(owner);
        this.setKiRenderType(0);
        this.setSize(0.8F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(color, color, colorOutline);
        this.setFiring(true);
        this.setCastTime(0);
        this.setMaxLife(100);
        this.setCastOffsets(0.0f, -0.5F, 0.5F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupKiSmall(LivingEntity owner, float damage, float speed, int color) {
        this.setupKiSmall(owner, damage, speed, color, 0xFFFFFF);
    }

    public void setupKiBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(1);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 100);
        this.setCastOffsets(0.0f, -0.5F, 0.5F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public float getStretch() {
        return this.entityData.get(STRETCH);
    }

    public void configureDestructionGrowth(float endSize, Vec3 spreadDirection) {
        this.destructionBall = true;
        this.destructionAge = 0;
        this.destructionStartSize = this.getSize();
        this.destructionEndSize = endSize;
        this.destructionSpread = spreadDirection == null ? Vec3.ZERO : spreadDirection;
    }

    private void tickDestructionBall() {
        this.destructionAge++;
        int age = this.destructionAge;

        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-5D) {
            this.setYRot((float) Math.toDegrees(Math.atan2(-motion.x, motion.z)));
            this.setXRot((float) -Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())));
        }

        if (age < DESTRUCTION_STRETCH_START) return;

        if (age < DESTRUCTION_SPREAD_START) {
            float t = (float) (age - DESTRUCTION_STRETCH_START) / (DESTRUCTION_SPREAD_START - DESTRUCTION_STRETCH_START);
            this.entityData.set(STRETCH, 1.0F + (DESTRUCTION_MAX_STRETCH - 1.0F) * t);
            return;
        }

        if (age <= DESTRUCTION_GROW_END) {
            float t = (float) (age - DESTRUCTION_SPREAD_START) / (DESTRUCTION_GROW_END - DESTRUCTION_SPREAD_START);
            float eased = t * t * (3.0F - 2.0F * t);
            this.setSize(this.destructionStartSize + (this.destructionEndSize - this.destructionStartSize) * eased);
            this.entityData.set(STRETCH, DESTRUCTION_MAX_STRETCH - (DESTRUCTION_MAX_STRETCH - 1.0F) * eased);
            this.setDeltaMovement(motion.add(this.destructionSpread.scale(DESTRUCTION_SPREAD_ACCEL)));
            this.hasImpulse = true;
        }
    }

    public void setupDestructionBall(LivingEntity owner, float damage, float speed, float size, int color, int colorBorder, int colorOutline) {
        this.setOwner(owner);
        this.setKiRenderType(6);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setCastTime(0);
        this.setMaxLife(220);
        this.setFiring(true);
    }

    public void setupKiBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size, int castTime) {
        this.setupKiBlast(owner, damage, speed, color, colorBorder, 0xFFFFFF, size, castTime);
    }

    public void setupKiBlast(LivingEntity owner, float damage, float speed, int color, float size, int castTime) {
        this.setupKiBlast(owner, damage, speed, color, color, 0xFFFFFF, size, castTime);
    }

    public void setupKiLargeBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(2);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 100);
        this.setCastOffsets(0.0f, 5.2F, 0.2F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupKiLargeBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size, int castTime) {
        this.setupKiLargeBlast(owner, damage, speed, color, colorBorder, 0xFFFFFF, size, castTime);
    }

    public void setupKiLargeBlast(LivingEntity owner, float damage, float speed, int color, float size, int castTime) {
        this.setupKiLargeBlast(owner, damage, speed, color, color, 0xFFFFFF, size, castTime);
    }

    public void setupInvertedKiBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(3);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 100);
        this.setCastOffsets(0.0f, -0.5F, 0.5F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupInvertedKiBlast(LivingEntity owner, float damage, float speed, int color, int colorBorder, float size, int castTime) {
        this.setupInvertedKiBlast(owner, damage, speed, color, colorBorder, 0xFFFFFF, size, castTime);
    }

    public void setupInvertedKiBlast(LivingEntity owner, float damage, float speed, int color, float size, int castTime) {
        this.setupInvertedKiBlast(owner, damage, speed, color, color, 0xFFFFFF, size, castTime);
    }

    public void setupKiSouls(LivingEntity owner, float damage, float speed, int color, int colorOutline, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(4);
        this.setSize(0.8F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(color, color, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 100);
        this.setCastOffsets(0.0f, -0.5F, 0.5F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupKiSouls(LivingEntity owner, float damage, float speed, int color, int castTime) {
        this.setupKiSouls(owner, damage, speed, color, 0xFFFFFF, castTime);
    }

    public void setupKiGenki(LivingEntity owner, float damage, float speed, int colorOutline, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(5);
        this.setSize(5.0F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0x30FFF1, 0x00F8FF, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 200);
        this.setCastOffsets(0.0F, 5.5F, 0.0F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupKiGenki(LivingEntity owner, float damage, float speed, int castTime) {
        this.setupKiGenki(owner, damage, speed, 0xFFFFFF, castTime);
    }

    public void setupKiNova(LivingEntity owner, float damage, float speed, int colorOutline, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(6);
        this.setSize(5.0F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(0x9E0000, 0x9E0000, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 200);
        this.setCastOffsets(0.0F, 5.5F, 0.0F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupKiNova(LivingEntity owner, float damage, float speed, int castTime) {
        this.setupKiNova(owner, damage, speed, 0xFFFFFF, castTime);
    }

    public void setupKiDeathBall(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(7);
        this.setSize(2.5F);
        this.setKiSpeed(speed);
        this.setKiDamage(damage);
        this.setColors(color, ColorUtils.darkenColor(colorBorder, 0.5f), colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 150);
        this.setCastOffsets(0.0F, 2.5F, 0.0F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupKiDeathBall(LivingEntity owner, float damage, float speed, int color, int colorBorder, int castTime) {
        this.setupKiDeathBall(owner, damage, speed, color, colorBorder, 0xFFFFFF, castTime);
    }

    public void setupKiDeathBall(LivingEntity owner, float damage, float speed, int color, int castTime) {
        this.setupKiDeathBall(owner, damage, speed, color, color, 0xFFFFFF, castTime);
    }

    public void setupSokidan(LivingEntity owner, float damage, float speed, int color, int colorOutline, float size, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(8);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color, colorOutline);
        this.setControllable(true);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + 500);
        this.setCastOffsets(0.0F, 0.5F, 0.5F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setupSokidan(LivingEntity owner, float damage, float speed, int color, float size, int castTime) {
        this.setupSokidan(owner, damage, speed, color, 0xFFFFFF, size, castTime);
    }

    public void setupKiVolley(LivingEntity owner, float damage, float speed, int color, int colorOutline, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(9);
        this.setSize(0.0F);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime*2);
        this.setCastOffsets(0.0f, 0.2f, 0.5f);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
        
    }

    public void setupKiVolley(LivingEntity owner, float damage, float speed, int color, int castTime) {
        this.setupKiVolley(owner, damage, speed, color, 0xFFFFFF, castTime);
    }

    public void setupKiAirVolley(LivingEntity owner, float damage, float speed, int color, int colorOutline, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(10);
        this.setSize(0.0F);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, color, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime*2);
        this.setCastOffsets(0.0f, 1.5f, 0.0f);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    public void setupAssaultRain(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size, int castTime, int fireTicks) {
        this.setOwner(owner);
        this.setKiRenderType(RENDER_ASSAULT_RAIN);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(castTime + fireTicks);
        this.setCastOffsets(-0.4F, 1.3F, 0.0F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    //ACA TERMINAN LOS METODOS PARA NPCS

    public void setupAssaultRainPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, float size, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(RENDER_ASSAULT_RAIN);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(99999);
        this.setCastOffsets(-0.4F, 1.3F, 0.0F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    private static AABB effectiveBounds(LivingEntity owner) {
        AABB box = owner.getBoundingBox();
        if (owner.isMultipartEntity() && owner.getParts() != null) {
            for (net.minecraftforge.entity.PartEntity<?> part : owner.getParts()) {
                if (part != null) box = box.minmax(part.getBoundingBox());
            }
        }
        return box;
    }

    private static float blasterMeteorSizeFor(LivingEntity owner) {
        AABB bounds = effectiveBounds(owner);
        float height = (float) bounds.getYsize();
        float width = (float) Math.max(bounds.getXsize(), bounds.getZsize());
        float diagonal = (float) Math.sqrt(height * height + width * width);
        return Math.max(height * 1.7F, diagonal * 1.25F);
    }

    public void setupBlasterMeteor(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline, int castTime, int fireTicks) {
        float size = blasterMeteorSizeFor(owner);
        this.setOwner(owner);
        this.setKiRenderType(RENDER_BLASTER_METEOR);
        this.setSize(size);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(color, colorBorder, colorOutline);
        this.setFiring(false);
        this.setCastTime(castTime);
        this.setMaxLife(fireTicks <= 0 ? 99999 : castTime + fireTicks);
        this.setCastOffsets(0.0F, -0.4F * size / ownerScaleOf(owner), 0.0F);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
        updatePositionRelativeToOwner(owner);
        if (!this.level().isClientSide) { this.level().addFreshEntity(this); }
    }

    public void setVolleyTarget(int targetId) {
        this.volleyTargetId = targetId;
    }

    private boolean isAnchoredVolley() {
        int type = this.getKiRenderType();
        return type == RENDER_ASSAULT_RAIN || type == RENDER_BLASTER_METEOR;
    }

    @Override
    protected float castClearanceRadius() {
        return this.isAnchoredVolley() ? 0.0F : super.castClearanceRadius();
    }

    public void toggleSokidanControl() {
        if (this.isControllable()) {
            boolean currentMode = this.isParked();
            this.setParked(!currentMode);

            if (this.isParked()) {
                if (this.getOwner() instanceof LivingEntity owner) {
                    float dist = (float) this.position().distanceTo(owner.getEyePosition());
                    this.setParkedDistance(dist);
                }
                this.setDeltaMovement(0, 0, 0);
            } else {
                if (this.getOwner() instanceof LivingEntity owner) {
                    Vec3 look = owner.getLookAngle();
                    this.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, this.getKiSpeed(), 0.0F);
                }
            }
            //this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.5F, 2.0F);
        }
    }

    public void fireHability(int finalMaxLife) {
        this.setFiring(true);
        this.setMaxLife(this.tickCount + finalMaxLife);
        this.setFireTick(this.tickCount);

        if (this.getOwner() instanceof LivingEntity livingOwner) {

            if (this.getKiRenderType() == RENDER_BLASTER_METEOR) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.PLAYERS, 0.8F, 1.2F);
                if (this.getOwner() instanceof Player) this.triggerAnimationPacket("_fire");
                return;
            }

            if (this.getKiRenderType() == RENDER_ASSAULT_RAIN) {
                this.setMaxLife(this.tickCount + finalMaxLife + ASSAULT_RAIN_DELAY);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.7F, 0.8F);
                if (this.getOwner() instanceof Player) this.triggerAnimationPacket("_fire");
                return;
            }

            if (this.getKiRenderType() == 9) {
                // The barrage emitter stays anchored to the caster (it doesn't fly), so we skip the trajectory
                // logic below — but still fire the "_fire" animation (ki.barrage_fire) the same as every other ki.
                if (this.getOwner() instanceof Player) this.triggerAnimationPacket("_fire");
                return;
            }

            Vec3 eyePos = livingOwner.getEyePosition();
            Vec3 lookDir = livingOwner.getLookAngle();
            double reach = 100.0D;
            Vec3 endPos = eyePos.add(lookDir.scale(reach));

            BlockHitResult blockHit = this.level().clip(new net.minecraft.world.level.ClipContext(
                    eyePos, endPos,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    livingOwner
            ));

            if (blockHit.getType() != BlockHitResult.Type.MISS) {
                endPos = blockHit.getLocation();
                reach = eyePos.distanceTo(endPos);
            }

            AABB searchBox = livingOwner.getBoundingBox().expandTowards(lookDir.scale(reach)).inflate(1.0D);
            for (Entity entity : this.level().getEntities(livingOwner, searchBox, e -> !e.isSpectator() && e.isPickable())) {
                AABB hitbox = entity.getBoundingBox().inflate(0.3D);
                java.util.Optional<Vec3> hit = hitbox.clip(eyePos, endPos);

                if (hit.isPresent()) {
                    double dist = eyePos.distanceTo(hit.get());
                    if (dist < reach) {
                        reach = dist;
                        endPos = hit.get();
                    }
                }
            }

            Vec3 kiPos = new Vec3(this.getX(), this.getVisualCenterY(), this.getZ());
            Vec3 newTrajectory = endPos.subtract(kiPos).normalize();

            this.shoot(newTrajectory.x, newTrajectory.y, newTrajectory.z, this.getKiSpeed(), 0.0F);
            this.hasImpulse = true;

            SoundEvent fireSound;
            if ("burning_attack".equals(this.getTechniqueId())) {
                fireSound = MainSounds.KI_BURNING_FIRE.get();
            } else if (this.getKiRenderType() == 5) {
                fireSound = MainSounds.KI_SPIRITBOMB_FIRE.get();
            } else if (this.getKiRenderType() == 6) {
                fireSound = MainSounds.KI_SUPERNOVA_FIRE.get();
            } else {
                fireSound = MainSounds.KIBLAST_ATTACK.get();
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), fireSound, SoundSource.PLAYERS, 0.7F, 1.0F);
        }

        if (this.getOwner() instanceof Player) this.triggerAnimationPacket("_fire");
    }

    private void finalizeSetupAndShoot(LivingEntity owner, float speed) {
        this.setOwner(owner);

        if (this.getCastTime() <= 0) {
            Vec3 lookDir = owner.getLookAngle();
            Vec3 spawnPos = owner.getEyePosition().add(lookDir.scale(0.5D));
            this.setPos(spawnPos.x, spawnPos.y - 0.2D, spawnPos.z);
            this.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, speed, 0.0F);

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.1F, 1.0F + (this.random.nextFloat() * 0.2F));
        } else {
            updatePositionRelativeToOwner(owner);
        }

        if (!this.level().isClientSide) {
            this.level().addFreshEntity(this);
        }
    }

    @Override
    public void tick() {
        if (!this.isFiring() && this.getMaxLife() != 99999 && this.tickCount >= this.getCastTime()) {
            this.fireHability(this.getMaxLife() - this.tickCount);
        }

        boolean isFiring = this.isFiring();

        if ((!isFiring && this.getCastTime() > 0) || this.getKiRenderType() == 9 || this.getKiRenderType() == 10 || this.isAnchoredVolley()) {
            var owner = this.getOwner();
            if (owner instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
                updatePositionRelativeToOwner(livingOwner);
                this.setDeltaMovement(0, 0, 0);
            } else if (!this.level().isClientSide) {
                this.discard();
                return;
            }
        }

        super.tick();

        if (!isFiring && this.getCastTime() > 0) {
            this.setDeltaMovement(0, 0, 0);
        }
    }

    @Override
    protected void onKiTick() {
        if (!this.level().isClientSide && this.getOwner() == null) {
            this.discard();
            return;
        }

        boolean isCasting = !this.isFiring();
        int type = this.getKiRenderType();
        Entity ownerEntity = this.getOwner();

        if (type == RENDER_BLASTER_METEOR) {
            if (ownerEntity instanceof LivingEntity owner && owner.isAlive()) {
                this.tickBlasterMeteor(owner, isCasting);
            } else if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }

        if (type == RENDER_ASSAULT_RAIN) {
            if (ownerEntity instanceof LivingEntity owner && owner.isAlive()) {
                this.tickAssaultRain(owner, isCasting);
            } else if (!this.level().isClientSide) {
                this.discard();
            }
            return;
        }

        if (type == 9 || type == 10) {
            if (ownerEntity instanceof LivingEntity owner && owner.isAlive()) {

                if (type == 10) {
                    if (isCasting) {
                        owner.setDeltaMovement(0, 0.05D, 0);
                    } else {
                        owner.setDeltaMovement(0, 0, 0);
                    }
                } else {
                    owner.setDeltaMovement(0, 0, 0);
                }

                owner.fallDistance = 0.0F;
                owner.hasImpulse = true;

                if (!this.level().isClientSide) {
                    if (!isCasting) {
                        if (type == 9) {
                            // Xenoverse-style max Volley: a dense, rapid stream of blasts converging toward where the
                            // caster aims. Fast cadence (every 2 ticks) + tight spread (focused, not a scattered
                            // shotgun) + full flight speed sell the "super rush" look.
                            if (this.tickCount % 2 == 0) {
                                for (int i = 0; i < 5; i++) {
                                    KiBlastEntity bullet = new KiBlastEntity(this.level(), owner);
                                    bullet.setupKiSmall(owner, this.getKiDamage(), this.getKiSpeed(), this.getColor());
                                    bullet.setTechniqueId(this.getTechniqueId());

                                    bullet.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, this.getKiSpeed(), 6.0F);
                                    this.level().addFreshEntity(bullet);
                                }

                                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                        MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.1F, 1.6F + (this.random.nextFloat() * 0.5F));
                            }
                        } else if (type == 10) {
                            if (this.tickCount % 2 == 0) {
                                for (int i = 0; i < 10; i++) {
                                    KiBlastEntity bullet = new KiBlastEntity(this.level(), owner);
                                    bullet.setupKiSmall(owner, this.getKiDamage(), this.getKiSpeed(), this.getColor());
                                    bullet.setTechniqueId(this.getTechniqueId());

                                    double spawnX = owner.getX();
                                    double spawnY = owner.getY() + (owner.getBbHeight() / 2.0D);
                                    double spawnZ = owner.getZ();
                                    bullet.setPos(spawnX, spawnY, spawnZ);

                                    float randomPitch = (this.random.nextFloat() * 180.0F) - 90.0F;
                                    float randomYaw = this.random.nextFloat() * 360.0F;

                                    bullet.shootFromRotation(owner, randomPitch, randomYaw, 0.0F, this.getKiSpeed() / 3, 0.0F);
                                    this.level().addFreshEntity(bullet);
                                }

                                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                        MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.15F, 1.2F + (this.random.nextFloat() * 0.4F));
                            }
                        }
                    }

                    if (this.tickCount >= this.getMaxLife()) {
                        this.discard();
                    }
                }
            } else if (!this.level().isClientSide) {
                this.discard();
            }

            return;
        }

        if (type == RENDER_FAKE_MOON) {
            if (!this.level().isClientSide) {
                if (this.isFiring()) {
                    if (Double.isNaN(this.fakeMoonStartY)) this.fakeMoonStartY = this.getY();
                    if (this.fakeMoonFrozenTick < 0) {
                        if (this.getY() - this.fakeMoonStartY < FAKE_MOON_CLIMB_BLOCKS) {
                            this.setDeltaMovement(0.0D, FAKE_MOON_CLIMB_SPEED, 0.0D);
                        } else {
                            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                            this.fakeMoonFrozenTick = this.tickCount;
                            this.setParked(true);
                        }
                    } else {
                        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                        if (this.tickCount - this.fakeMoonFrozenTick >= FAKE_MOON_GLOW_TICKS) {
                            this.discard();
                        }
                    }
                } else {
                    this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                }
            }
            return;
        }

        if (!isCasting && this.isParked() && ownerEntity instanceof LivingEntity owner) {
            Vec3 eyePos = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();

            Vec3 targetPos = eyePos.add(look.scale(this.getParkedDistance()));
            Vec3 diff = targetPos.subtract(this.position());

            if (diff.lengthSqr() > 0.02) {
                double followSpeed = this.getKiSpeed() * 1.5;
                this.setDeltaMovement(diff.normalize().scale(Math.min(diff.length(), followSpeed)));
            } else {
                this.setDeltaMovement(0, 0, 0);
                this.setPos(targetPos.x, targetPos.y, targetPos.z);
            }

            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }

        if (!this.level().isClientSide) {
            if(isCasting){
                if (type == 5 && this.tickCount == 1) {
                    this.playSound(MainSounds.KI_SPIRITBOMB_CHARGE.get(), 0.7F, 1.0F);
                }  else if (type == 6 && this.tickCount == 1) {
                    this.playSound(MainSounds.KI_SUPERNOVA_CHARGE.get(), 0.7F, 1.0F);
                }
            } else {
                if (this.isDetonating) {
                    this.processDetonation();
                    return;
                }

                if (this.destructionBall) this.tickDestructionBall();

                if (type == 5 || type == 6) { // Genkidama o Supernova
                    if (this.tickCount % 20 == 0) {
                        if (this.destroyBlocksInPath()) {
                            this.setDeltaMovement(this.getDeltaMovement().scale(0.95D));
                        }
                        if (this.getDeltaMovement().lengthSqr() < 0.01D) {
                            this.explodeAndDie();
                            return;
                        }
                    }
                }

                if (!this.isSoulPunisher() && !this.assaultRainRiser && this.tickCount % 10 == 0) {
                    pulseAreaDamage();
                }
            }

            if (this.tickCount >= this.getMaxLife()) {
                if (this.isSoulPunisher() || this.assaultRainRiser) this.discard();
                else this.explodeAndDie();
                return;
            }
        }

        if (this.level().isClientSide) {
            float scale = this.getSize();
            float[] borderColor = this.getRgbColorBorder();
            float pr = borderColor[0], pg = borderColor[1], pb = borderColor[2];

            if (type == 4) { // Ki Souls
                float speed = (float)this.tickCount * 0.5f;
                pr = (float)(Math.sin(speed) * 0.5 + 0.5);
                pg = (float)(Math.sin(speed + 2.0944) * 0.5 + 0.5);
                pb = (float)(Math.sin(speed + 4.1888) * 0.5 + 0.5);
            }

            if (type >= 1 && !isCasting) {
                for (int i = 0; i < 3; i++) {
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
                            this.getX() + dx, this.getY() + (this.getBbHeight() / 2.0) + dy, this.getZ() + dz,
                            vx, vy, vz
                    );

                    if (p instanceof KiTrailParticle trail) {
                        trail.setKiColor(pr, pg, pb);
                        trail.setKiScale(scale);
                    }
                }
            }

            if (type == 2 || type == 5 || type == 6) {
                for (int i = 0; i < 10; i++) {
                    double absDist = scale * 3;
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double sx = Math.cos(angle) * absDist;
                    double sz = Math.sin(angle) * absDist;
                    double sy = (this.random.nextDouble() - 0.5) * 2.0 * absDist;

                    Particle p = Minecraft.getInstance().particleEngine.createParticle(
                            MainParticles.KI_SHEDDING.get(),
                            this.getX() + sx, this.getY() + (this.getBbHeight() / 2) + sy, this.getZ() + sz,
                            -sx * 0.15, -sy * 0.15, -sz * 0.15
                    );

                    if (p instanceof KiSheddingParticle kiParticle) {
                        kiParticle.setKiColor(borderColor[0], borderColor[1], borderColor[2]);
                    }
                }
            }
        }

        if (this.level().isClientSide && !hasSpawnedSplash) {
            if (type != 0) {
                float[] rgb = this.getRgbColorBorder();
                this.level().addParticle(
                        MainParticles.KI_SPLASH.get(),
                        this.getX(), this.getY() + (this.getBbHeight() / 2.0), this.getZ(),
                        rgb[0], rgb[1], rgb[2]
                );
            }
            this.hasSpawnedSplash = true;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST_TIME, 0);
        this.entityData.define(STRETCH, 1.0F);
        this.entityData.define(OFFSET_X, 0.0F);
        this.entityData.define(OFFSET_Y, 0.0F);
        this.entityData.define(OFFSET_Z, 0.0F);

        this.entityData.define(IS_CONTROLLABLE, false);
        this.entityData.define(IS_PARKED, false);
        this.entityData.define(PARKED_DISTANCE, 0.0F);

        this.entityData.define(IS_FIRING, false);

    }

    public void setCastTime(int ticks) { this.entityData.set(CAST_TIME, ticks); }
    public int getCastTime() { return this.entityData.get(CAST_TIME); }
    public void setCastOffsets(float offsetX, float offsetY, float offsetZ) {
        this.entityData.set(OFFSET_X, offsetX);
        this.entityData.set(OFFSET_Y, offsetY);
        this.entityData.set(OFFSET_Z, offsetZ);
    }
    public void setControllable(boolean controllable) { this.entityData.set(IS_CONTROLLABLE, controllable); }
    public boolean isControllable() { return this.entityData.get(IS_CONTROLLABLE); }
    public void setParked(boolean parked) { this.entityData.set(IS_PARKED, parked); }
    public boolean isParked() { return this.entityData.get(IS_PARKED); }
    public void setParkedDistance(float dist) { this.entityData.set(PARKED_DISTANCE, dist); }
    public float getParkedDistance() { return this.entityData.get(PARKED_DISTANCE); }
    public boolean isFiring() { return this.entityData.get(IS_FIRING); }
    public void setFiring(boolean firing) { this.entityData.set(IS_FIRING, firing); }


    private void tickAssaultRain(LivingEntity owner, boolean isCasting) {
        owner.setDeltaMovement(0, 0, 0);
        owner.fallDistance = 0.0F;
        owner.hasImpulse = true;

        if (this.level().isClientSide) return;

        if (!isCasting && this.tickCount % 2 == 0) {
            Vec3 center = this.resolveAssaultRainCenter(owner);
            Vec3 source = new Vec3(this.getX(), this.getY() + (this.getBbHeight() / 2.0D), this.getZ());
            Vec3 toward = new Vec3(center.x - source.x, 0.0D, center.z - source.z);
            toward = toward.lengthSqr() < 1.0E-4D ? Vec3.ZERO : toward.normalize();

            if (this.tickCount < this.getMaxLife() - ASSAULT_RAIN_DELAY) {
                for (int i = 0; i < ASSAULT_RAIN_RISERS; i++) {
                    Vec3 riseDir = new Vec3(
                            toward.x * 0.25D + this.random.nextGaussian() * 0.22D,
                            1.0D,
                            toward.z * 0.25D + this.random.nextGaussian() * 0.22D).normalize();
                    this.spawnVolleyShot(owner, source, riseDir.scale(ASSAULT_RAIN_RISE_SPEED), 0.0F, true);
                }

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.15F, 1.4F + (this.random.nextFloat() * 0.5F));
            }

            if (this.tickCount - this.getFireTick() >= ASSAULT_RAIN_DELAY) {
                float fallSpeed = Mth.clamp(this.getKiSpeed() * 1.6F, 2.0F, 3.0F);
                Vec3 fallDir = new Vec3(toward.x * ASSAULT_RAIN_SLANT, -1.0D, toward.z * ASSAULT_RAIN_SLANT).normalize();
                double height = this.assaultRainHeight(center);

                for (int i = 0; i < ASSAULT_RAIN_DROPS; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    double dist = ASSAULT_RAIN_RADIUS * Math.sqrt(this.random.nextDouble());
                    Vec3 ground = center.add(Math.cos(angle) * dist, 0.0D, Math.sin(angle) * dist);
                    Vec3 spawn = ground.subtract(fallDir.scale(height / -fallDir.y));
                    this.spawnVolleyShot(owner, spawn, fallDir.scale(fallSpeed), this.getKiDamage(), false);
                }
            }
        }

        if (this.tickCount >= this.getMaxLife()) {
            this.discard();
        }
    }

    private void tickBlasterMeteor(LivingEntity owner, boolean isCasting) {
        double rise = 0.0D;
        if (isCasting && this.blasterMeteorRisen < BLASTER_METEOR_MAX_RISE) {
            rise = Math.min(BLASTER_METEOR_RISE_SPEED, BLASTER_METEOR_MAX_RISE - this.blasterMeteorRisen);
            this.blasterMeteorRisen += rise;
        }
        owner.setDeltaMovement(0, rise, 0);
        owner.fallDistance = 0.0F;
        owner.hasImpulse = true;

        if (this.level().isClientSide) return;

        if (!isCasting && this.tickCount % 2 == 0) {
            Vec3 center = new Vec3(owner.getX(), effectiveBounds(owner).getCenter().y, owner.getZ());
            double radius = this.getSize() * 0.5D;
            float shotSpeed = Math.max(1.0F, this.getKiSpeed() * 0.8F);
            Vec3 aimDir = this.resolveBlasterMeteorAim(owner, center);

            for (int i = 0; i < BLASTER_METEOR_SHOTS; i++) {
                Vec3 dir;
                if (i < BLASTER_METEOR_AIMED_SHOTS) {
                    dir = aimDir.add(this.random.nextGaussian() * 0.12D, this.random.nextGaussian() * 0.12D, this.random.nextGaussian() * 0.12D).normalize();
                } else {
                    double y = this.random.nextDouble() * 2.0D - 1.0D;
                    double theta = this.random.nextDouble() * Math.PI * 2.0D;
                    double ring = Math.sqrt(1.0D - y * y);
                    dir = new Vec3(ring * Math.cos(theta), y, ring * Math.sin(theta));
                }
                this.spawnVolleyShot(owner, center.add(dir.scale(radius)), dir.scale(shotSpeed), this.getKiDamage(), false);
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.15F, 1.0F + (this.random.nextFloat() * 0.4F));
        }

        if (this.tickCount >= this.getMaxLife()) {
            this.discard();
        }
    }

    private Vec3 resolveBlasterMeteorAim(LivingEntity owner, Vec3 center) {
        LivingEntity aimed = null;
        if (this.volleyTargetId >= 0 && this.level().getEntity(this.volleyTargetId) instanceof LivingEntity locked && locked.isAlive()) {
            aimed = locked;
        } else if (owner instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            aimed = mob.getTarget();
        }
        if (aimed == null) return owner.getLookAngle();

        Vec3 toTarget = aimed.position().add(0.0D, aimed.getBbHeight() / 2.0D, 0.0D).subtract(center);
        return toTarget.lengthSqr() < 1.0E-4D ? owner.getLookAngle() : toTarget.normalize();
    }

    private void spawnVolleyShot(LivingEntity owner, Vec3 pos, Vec3 velocity, float damage, boolean riser) {
        KiBlastEntity shot = new KiBlastEntity(this.level(), owner);
        shot.setKiRenderType(0);
        shot.setSize(0.8F);
        shot.setKiSpeed((float) velocity.length());
        shot.setKiDamage(damage);
        if (this.random.nextBoolean()) shot.setColors(this.getColor(), this.getColorBorder(), this.getColorOutline());
        else shot.setColors(this.getColor(), this.getColorOutline(), this.getColorBorder());
        shot.setFiring(true);
        shot.setCastTime(0);
        shot.setMaxLife(riser ? ASSAULT_RAIN_RISE_LIFE : ASSAULT_RAIN_DROP_LIFE);
        shot.assaultRainRiser = riser;
        if (!riser) {
            shot.setArmorPenetration(this.getArmorPenetration());
            shot.setTechniqueId(this.getTechniqueId());
        }
        shot.setPos(pos.x, pos.y, pos.z);
        shot.setDeltaMovement(velocity);
        shot.hasImpulse = true;
        this.level().addFreshEntity(shot);
    }

    private double assaultRainHeight(Vec3 center) {
        Vec3 from = center.add(0.0D, 1.5D, 0.0D);
        Vec3 to = center.add(0.0D, ASSAULT_RAIN_HEIGHT, 0.0D);
        BlockHitResult ceiling = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (ceiling.getType() == HitResult.Type.MISS) return ASSAULT_RAIN_HEIGHT;
        return Math.max(3.0D, ceiling.getLocation().y - center.y - 1.0D);
    }

    private Vec3 resolveAssaultRainCenter(LivingEntity owner) {
        if (this.volleyTargetId >= 0 && this.level().getEntity(this.volleyTargetId) instanceof LivingEntity locked && locked.isAlive()) {
            return locked.position();
        }
        if (owner instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            return mob.getTarget().position();
        }

        Vec3 eyePos = owner.getEyePosition();
        Vec3 lookDir = owner.getLookAngle();
        Vec3 endPos = eyePos.add(lookDir.scale(ASSAULT_RAIN_AIM_RANGE));

        BlockHitResult blockHit = this.level().clip(new ClipContext(eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        if (hitBlock) endPos = blockHit.getLocation();

        AABB searchBox = owner.getBoundingBox().expandTowards(endPos.subtract(eyePos)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), owner, eyePos, endPos, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e.isPickable(), 0.3F);
        if (entityHit != null) return entityHit.getEntity().position();
        if (hitBlock) return endPos;

        BlockHitResult ground = this.level().clip(new ClipContext(endPos, endPos.subtract(0.0D, 64.0D, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        return ground.getType() != HitResult.Type.MISS ? ground.getLocation() : endPos;
    }

    private void updatePositionRelativeToOwner(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 right = look.cross(worldUp).normalize();

        // Usar worldUp directamente en vez de right.cross(look)
        Vec3 offset = castOffset(owner, right, worldUp, look,
                this.entityData.get(OFFSET_X), this.entityData.get(OFFSET_Y), this.entityData.get(OFFSET_Z));  // <-- cambio aquí

        double centerX = owner.getX();
        double centerY = this.getKiRenderType() == RENDER_BLASTER_METEOR
                ? effectiveBounds(owner).getCenter().y
                : owner.getY() + (owner.getBbHeight() / 2.0D);
        double centerZ = owner.getZ();

        Vec3 newPos = new Vec3(centerX, centerY, centerZ).add(offset);
        this.setPos(newPos.x, newPos.y, newPos.z);

        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    private void pulseAreaDamage() {
        AABB area = this.getBoundingBox().inflate(5.0D);
        List<LivingEntity> nearby = MultipartTargeting.collectTargets(this.level(), area);

        Vec3 center = new Vec3(this.getX(), this.getVisualCenterY(), this.getZ());
        double radius = this.getSize() / 2.0D;

        for (LivingEntity target : nearby) {
            if (MultipartTargeting.withinRadius(target, center, radius + 5.0D)) {
                if (this.shouldDamage(target)) {
                    boolean wasHit = this.applyDamageOrHeal(target, this.getDamagePerHit());
                    if (wasHit) this.onSuccessfulHit(target);
                }
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        boolean isCasting = !this.isFiring();

        if (isCasting) {
            return;
        }

        if (this.getKiRenderType() == RENDER_FAKE_MOON || this.isAnchoredVolley()) {
            return;
        }

        if (this.assaultRainRiser) {
            return;
        }

        if (this.isSoulPunisher()) {
            if (!this.level().isClientSide) {
                Entity targetEntity = pResult.getEntity();
                if (this.shouldDamage(targetEntity)) {
                    float dealt = this.soulPunisherDamage(targetEntity, this.getKiDamage());
                    boolean wasHit = this.applyDamageOrHeal(targetEntity, dealt);
                    if (wasHit) {
                        this.onSuccessfulHit(targetEntity);
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    MainParticles.KI_SPLASH_WAVE.get(),
                                    targetEntity.getX(), targetEntity.getY() + (targetEntity.getBbHeight() / 2.0), targetEntity.getZ(),
                                    0, (double) this.getColorBorder(), (double) this.getSize(), 0.0D, 1.0D
                            );
                        }
                    }
                }
                this.discard();
            }
            return;
        }

        super.onHitEntity(pResult);

        if (!this.level().isClientSide) {
            Entity targetEntity = pResult.getEntity();

            if (this.shouldDamage(targetEntity)) {
                boolean wasHit = this.applyDamageOrHeal(targetEntity, this.getKiDamage());

                if (wasHit) {
                    this.onSuccessfulHit(targetEntity);
                    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double colorData = (double) this.getColorBorder();
                        double sizeData = (double) this.getSize();
                        double pX = targetEntity.getX();
                        double pY = targetEntity.getY() + (targetEntity.getBbHeight() / 2.0);
                        double pZ = targetEntity.getZ();

                        serverLevel.sendParticles(
                                MainParticles.KI_SPLASH_WAVE.get(),
                                pX, pY, pZ,
                                0, colorData, sizeData, 0.0D, 1.0D
                        );
                    }
                }
            }

            if (this.isControllable()) {
                return;
            }


            int type = this.getKiRenderType();
            if (type == 5 || type == 6) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.85D));
                if (this.getDeltaMovement().lengthSqr() < 0.01D) {
                    explodeAndDie();
                }
            } else {
                explodeAndDie();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        boolean isCasting = !this.isFiring();

        if (isCasting) {
            return;
        }

        int type = this.getKiRenderType();
        if (type == RENDER_FAKE_MOON || this.isAnchoredVolley()) {
            return;
        }

        if (this.isSoulPunisher() || this.assaultRainRiser) {
            if (!this.level().isClientSide) this.discard();
            return;
        }

        if (type == 5 || type == 6) {
        } else {
            super.onHitBlock(pResult);
            if (!this.level().isClientSide) {
                explodeAndDie();
            }
        }
    }

    private void explodeAndDie() {
        if (this.isRemoved() || this.isDetonating) return;

        int type = this.getKiRenderType();
        double centerY = this.getVisualCenterY();

        if ((type == 5 || type == 6) && !this.level().isClientSide) {
            this.isDetonating = true;
            this.maxDetonationRadius = Math.max(this.getSize() * 1.5F, MIN_BLAST_CRATER * 2.0F);
            this.currentDetonationRadius = 0.0F;
            this.setDeltaMovement(0, 0, 0);

            AABB damageArea = new AABB(this.getX(), centerY, this.getZ(), this.getX(), centerY, this.getZ()).inflate(this.maxDetonationRadius);            List<LivingEntity> targets = MultipartTargeting.collectTargets(this.level(), damageArea);
            for (LivingEntity target : targets) {
                if (this.shouldDamage(target)) {
                    boolean wasHit = this.applyDamageOrHeal(target, this.getKiDamage());
                    if (wasHit) this.onSuccessfulHit(target);
                }
            }

            float visualParticleSize = this.maxDetonationRadius * 1.8F;
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        MainParticles.KI_EXPLOSION.get(),
                        this.getX(), centerY, this.getZ(),
                        0, (double) visualParticleSize, 0.0D, 0.0D, 1.0D
                );
                serverLevel.playSound(null, this.getX(), centerY, this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 5.0F, 0.6F);

                KiExplosionVisualEntity explosionVisual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), this.level());
                explosionVisual.setPos(this.getX(), centerY, this.getZ());
                explosionVisual.setupExplosion(this.getColor(), this.getColorBorder(), this.getColorOutline(), this.getSize() * 0.9F);
                this.level().addFreshEntity(explosionVisual);
            }
            return;
        }

        float explosionRadius = Math.max(this.getSize() * 0.5F, MIN_BLAST_CRATER);
        float visualParticleSize = explosionRadius * 1.8F;

        AABB damageArea = new AABB(this.getX(), centerY, this.getZ(), this.getX(), centerY, this.getZ()).inflate(explosionRadius);
        List<LivingEntity> targets = MultipartTargeting.collectTargets(this.level(), damageArea);
        for (LivingEntity target : targets) {
            if (this.shouldDamage(target)) {
                boolean wasHit = this.applyDamageOrHeal(target, this.getKiDamage());
                if (wasHit) this.onSuccessfulHit(target);
            }
        }

        if (!this.level().isClientSide) {
            BlockPos center = BlockPos.containing(this.getX(), centerY, this.getZ());

            this.carveKiSphere(center, this.scaledDestructionRadius(explosionRadius), 2);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        MainParticles.KI_EXPLOSION.get(),
                        this.getX(), centerY, this.getZ(),
                        0, (double) visualParticleSize, 0.0D, 0.0D, 1.0D
                );
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 5.0F, 0.6F);

                KiExplosionVisualEntity explosionVisual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), this.level());
                explosionVisual.setPos(this.getX(), centerY, this.getZ());
                explosionVisual.setupExplosion(this.getColor(), this.getColorBorder(), this.getColorOutline(), this.getSize() * 0.25F);
                this.level().addFreshEntity(explosionVisual);
            }
        }
        this.discard();
    }

    private void processDetonation() {
        float prevRadius = this.currentDetonationRadius;
        this.currentDetonationRadius += 2.0F;

        float scaledRadius = this.scaledDestructionRadius(this.currentDetonationRadius);
        float scaledPrevRadius = this.scaledDestructionRadius(prevRadius);

        this.carveKiSphere(BlockPos.containing(this.getX(), this.getVisualCenterY(), this.getZ()),
                scaledPrevRadius, scaledRadius, 2);

        if (this.currentDetonationRadius >= this.maxDetonationRadius) {
            this.discard();
        }
    }

    private boolean destroyBlocksInPath() {
        return this.eatKiSphere(BlockPos.containing(this.getX(), this.getVisualCenterY(), this.getZ()),
                this.scaledDestructionRadius(this.getSize() * 1.2F));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("CastTime", getCastTime());
        pCompound.putFloat("OffsetX", this.entityData.get(OFFSET_X));
        pCompound.putFloat("OffsetY", this.entityData.get(OFFSET_Y));
        pCompound.putFloat("OffsetZ", this.entityData.get(OFFSET_Z));

        pCompound.putBoolean("IsControllable", this.isControllable());
        pCompound.putBoolean("IsParked", this.isParked());
        pCompound.putFloat("ParkedDistance", this.getParkedDistance());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("CastTime")) setCastTime(pCompound.getInt("CastTime"));
        if (pCompound.contains("OffsetX")) this.entityData.set(OFFSET_X, pCompound.getFloat("OffsetX"));
        if (pCompound.contains("OffsetY")) this.entityData.set(OFFSET_Y, pCompound.getFloat("OffsetY"));
        if (pCompound.contains("OffsetZ")) this.entityData.set(OFFSET_Z, pCompound.getFloat("OffsetZ"));

        if (pCompound.contains("IsControllable")) setControllable(pCompound.getBoolean("IsControllable"));
        if (pCompound.contains("IsParked")) setParked(pCompound.getBoolean("IsParked"));
        if (pCompound.contains("ParkedDistance")) setParkedDistance(pCompound.getFloat("ParkedDistance"));
    }



    private double getVisualCenterY() {
        return this.getY() + (this.getSize() / 2.0);
    }
}
