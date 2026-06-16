package com.dragonminez.common.init.entities.ki;

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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KiBlastEntity extends AbstractKiProjectile {

    private boolean hasSpawnedSplash = false;

    public static final int RENDER_SOUL_PUNISHER = 4;
    public static final int RENDER_FAKE_MOON = 11;
    private static final double FAKE_MOON_CLIMB_BLOCKS = 30.0D;
    private static final double FAKE_MOON_CLIMB_SPEED = 1.0D;
    private static final int FAKE_MOON_GLOW_TICKS = 20 * 20;
    private transient double fakeMoonStartY = Double.NaN;
    private transient int fakeMoonFrozenTick = -1;

    private boolean isDetonating = false;
    private float currentDetonationRadius = 0.0F;
    private float maxDetonationRadius = 0.0F;

    private static final EntityDataAccessor<Integer> CAST_TIME = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> OFFSET_X = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Y = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> OFFSET_Z = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_CONTROLLABLE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_PARKED = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> PARKED_DISTANCE = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(KiBlastEntity.class, EntityDataSerializers.BOOLEAN);

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
        this.setCastOffsets(0.0f, 0.0F, 2.0F);
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

    public void setupKiGenkiPlayer(LivingEntity owner, float damage, float speed, int colorOutline) {
        this.setOwner(owner);
        this.setKiRenderType(5);
        this.setSize(5.0F);
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

    public void setupKiGenkiPlayer(LivingEntity owner, float damage, float speed) {
        this.setupKiGenkiPlayer(owner, damage, speed, 0xFFFFFF);
    }

    public void setupKiNovaPlayer(LivingEntity owner, float damage, float speed, int colorOutline) {
        this.setOwner(owner);
        this.setKiRenderType(6);
        this.setSize(5.0F);
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

    public void setupKiNovaPlayer(LivingEntity owner, float damage, float speed) {
        this.setupKiNovaPlayer(owner, damage, speed, 0x800E0E);
    }

    public void setupKiDeathBallPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder, int colorOutline) {
        this.setOwner(owner);
        this.setKiRenderType(7);
        this.setSize(2.5F);
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

    public void setupKiDeathBallPlayer(LivingEntity owner, float damage, float speed, int color, int colorBorder) {
        this.setupKiDeathBallPlayer(owner, damage, speed, color, colorBorder, 0xFFFFFF);
    }

    public void setupKiDeathBallPlayer(LivingEntity owner, float damage, float speed, int color) {
        this.setupKiDeathBallPlayer(owner, damage, speed, color, color, 0xFFFFFF);
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

    public void setupKiVolleyPlayer(LivingEntity owner, float damage, float speed, int color, int colorOutline, int castTime) {
        this.setOwner(owner);
        this.setKiRenderType(9);
        this.setSize(0.4F);
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

    public void setupKiVolleyPlayer(LivingEntity owner, float damage, float speed, int color, int castTime) {
        this.setupKiVolleyPlayer(owner, damage, speed, color, 0xFFFFFF, castTime);
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

    //ACA TERMINAN LOS METODOS PARA NPCS

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

            if (this.getKiRenderType() == 9) {
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

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.5F, 1.0F + (this.random.nextFloat() * 0.2F));
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

        if ((!isFiring && this.getCastTime() > 0) || this.getKiRenderType() == 9 || this.getKiRenderType() == 10) {
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
                            if (this.tickCount % 4 == 0) {
                                for (int i = 0; i < 5; i++) {
                                    KiBlastEntity bullet = new KiBlastEntity(this.level(), owner);
                                    bullet.setupKiSmall(owner, this.getKiDamage(), this.getKiSpeed(), this.getColor());

                                    bullet.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, this.getKiSpeed() / 2, 15.0F);
                                    this.level().addFreshEntity(bullet);
                                }

                                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                                        MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 0.1F, 1.5F + (this.random.nextFloat() * 0.5F));
                            }
                        } else if (type == 10) {
                            if (this.tickCount % 2 == 0) {
                                for (int i = 0; i < 10; i++) {
                                    KiBlastEntity bullet = new KiBlastEntity(this.level(), owner);
                                    bullet.setupKiSmall(owner, this.getKiDamage(), this.getKiSpeed(), this.getColor());

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
            if (!isCasting) {
                if (this.isDetonating) {
                    this.processDetonation();
                    return;
                }

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

                if (!this.isSoulPunisher() && this.tickCount % 10 == 0) {
                    pulseAreaDamage();
                }
            }

            if (this.tickCount >= this.getMaxLife()) {
                if (this.isSoulPunisher()) this.discard();
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


    private void updatePositionRelativeToOwner(LivingEntity owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();

        Vec3 offset = right.scale(this.entityData.get(OFFSET_X))
                .add(up.scale(this.entityData.get(OFFSET_Y)))
                .add(look.scale(this.entityData.get(OFFSET_Z)));

        double centerX = owner.getX();
        double centerY = owner.getY() + (owner.getBbHeight() / 2.0D);
        double centerZ = owner.getZ();
        Vec3 hitboxCenter = new Vec3(centerX, centerY, centerZ);

        Vec3 newPos = hitboxCenter.add(offset);
        this.setPos(newPos.x, newPos.y, newPos.z);

        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    private void pulseAreaDamage() {
        AABB area = this.getBoundingBox().inflate(5.0D);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, area);

        Vec3 center = new Vec3(this.getX(), this.getVisualCenterY(), this.getZ());
        double radius = this.getSize() / 2.0D;
        double radiusSq = radius * radius;

        for (LivingEntity target : nearby) {
            if (target.distanceToSqr(center) <= radiusSq + 25.0D) {
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

        if (this.getKiRenderType() == RENDER_FAKE_MOON) {
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
        if (type == RENDER_FAKE_MOON) {
            return;
        }

        if (this.isSoulPunisher()) {
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
            this.maxDetonationRadius = (this.getSize() / 2.0F) * 2.5F;
            this.currentDetonationRadius = 0.0F;
            this.setDeltaMovement(0, 0, 0);

            AABB damageArea = new AABB(this.getX(), centerY, this.getZ(), this.getX(), centerY, this.getZ()).inflate(this.maxDetonationRadius);            List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, damageArea);
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
                explosionVisual.setupExplosion(this.getColor(), this.getColorBorder(), this.getColorOutline(), this.getSize() * 1.2F);
                this.level().addFreshEntity(explosionVisual);
            }
            return;
        }

        float explosionRadius = (this.getSize() / 2.0F) * 1.2F;
        float visualParticleSize = explosionRadius * 1.8F;

        AABB damageArea = new AABB(this.getX(), centerY, this.getZ(), this.getX(), centerY, this.getZ()).inflate(explosionRadius);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, damageArea);
        for (LivingEntity target : targets) {
            if (this.shouldDamage(target)) {
                boolean wasHit = this.applyDamageOrHeal(target, this.getKiDamage());
                if (wasHit) this.onSuccessfulHit(target);
            }
        }

        if (!this.level().isClientSide) {
            BlockPos center = BlockPos.containing(this.getX(), centerY, this.getZ());

            int blockRadius = Math.round(explosionRadius);
            for (int x = -blockRadius; x <= blockRadius; x++) {
                for (int y = -blockRadius; y <= blockRadius; y++) {
                    for (int z = -blockRadius; z <= blockRadius; z++) {
                        if (x * x + y * y + z * z <= explosionRadius * explosionRadius) {
                            BlockPos targetPos = center.offset(x, y, z);
                            if (this.level().getBlockState(targetPos).getExplosionResistance(this.level(), targetPos, null) < 1000) {
                                this.setKiBlockToAir(targetPos, 2);
                            }
                        }
                    }
                }
            }

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
                explosionVisual.setupExplosion(this.getColor(), this.getColorBorder(), this.getColorOutline(), this.getSize());
                this.level().addFreshEntity(explosionVisual);
            }
        }
        this.discard();
    }

    private void processDetonation() {
        float prevRadius = this.currentDetonationRadius;
        this.currentDetonationRadius += 2.0F;

        int bRad = Math.round(this.currentDetonationRadius);
        BlockPos center = BlockPos.containing(this.getX(), this.getVisualCenterY(), this.getZ());
        Level level = this.level();

        float radSq = this.currentDetonationRadius * this.currentDetonationRadius;
        float prevRadSq = prevRadius * prevRadius;

        for (int x = -bRad; x <= bRad; x++) {
            for (int y = -bRad; y <= bRad; y++) {
                for (int z = -bRad; z <= bRad; z++) {
                    float distSq = x * x + y * y + z * z;

                    if (distSq <= radSq && distSq > prevRadSq) {
                        BlockPos targetPos = center.offset(x, y, z);
                        if (!level.getBlockState(targetPos).isAir() && level.getBlockState(targetPos).getExplosionResistance(level, targetPos, null) < 1000) {
                            this.setKiBlockToAir(targetPos, 2);
                        }
                    }
                }
            }
        }

        if (this.currentDetonationRadius >= this.maxDetonationRadius) {
            this.discard();
        }
    }

    private boolean destroyBlocksInPath() {
        boolean hitSomething = false;
        float eatRadius = this.getSize() / 2.0f;
        int bRad = Math.round(eatRadius);
        BlockPos center = BlockPos.containing(this.getX(), this.getVisualCenterY(), this.getZ());
        Level level = this.level();

        for (int x = -bRad; x <= bRad; x++) {
            for (int y = -bRad; y <= bRad; y++) {
                for (int z = -bRad; z <= bRad; z++) {
                    if (x * x + y * y + z * z <= eatRadius * eatRadius) {
                        BlockPos targetPos = center.offset(x, y, z);

                        if (!level.getBlockState(targetPos).isAir() && level.getBlockState(targetPos).getExplosionResistance(level, targetPos, null) < 1000 && this.destroyKiBlock(targetPos, false)) {
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

        if (hitSomething && !this.level().isClientSide) {
            KiExplosionVisualEntity explosionVisual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), this.level());
            explosionVisual.setPos(this.getX(), this.getVisualCenterY(), this.getZ());
            explosionVisual.setupExplosion(this.getColor(), this.getColorBorder(), this.getColorOutline(), this.getSize());
            this.level().addFreshEntity(explosionVisual);
        }

        return hitSomething;
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
