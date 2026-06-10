package com.dragonminez.common.init.entities.ki;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.passives.ClassPassives;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public abstract class AbstractKiProjectile extends Projectile {

    private static final EntityDataAccessor<Integer> COLOR_MAIN = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_BORDER = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR_OUTLINE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.FLOAT);
    //0 = Small, 1 = Blast, 2 = Large Blast
    private static final EntityDataAccessor<Integer> KI_BALL_RENDER_TYPE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TECHNIQUE_ID = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ARMOR_PENETRATION = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_HEAL = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MAX_LIFE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KI_TYPE = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FIRE_TICK = SynchedEntityData.defineId(AbstractKiProjectile.class, EntityDataSerializers.INT);

    private transient float[] cachedColorMainRgb;
    private transient float[] cachedColorBorderRgb;
    private transient float[] cachedColorOutlineRgb;

    private transient float kiLifetimeDrainPerTick = 0.0f;
    private transient float kiDrainAccumulator = 0.0f;

    public void setKiLifetimeDrainPerTick(float perTick) { this.kiLifetimeDrainPerTick = Math.max(0.0f, perTick); }

    @Getter
    private transient float clashLockedLength = -1.0F; // -1 = not clash-locked
    @Getter
    private transient UUID clashOpponentId = null;

    private transient int strikeStunTicks = 0;
    private transient UUID strikeStunExcludeId = null;

    public void setStrikeStun(int ticks, UUID excludeMainTargetId) {
        this.strikeStunTicks = Math.max(0, ticks);
        this.strikeStunExcludeId = excludeMainTargetId;
    }

    protected void applyStrikeStun(Entity target) {
        if (this.strikeStunTicks <= 0) return;
        if (!(target instanceof LivingEntity living)) return;
        if (living == this.getOwner()) return;
        if (living instanceof Player && this.strikeStunExcludeId != null && living.getUUID().equals(this.strikeStunExcludeId)) return;
        living.addEffect(new MobEffectInstance(MainEffects.STUN.get(), this.strikeStunTicks, 0, false, false, true));
    }

    public AbstractKiProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noCulling = true;
    }

    public abstract int getMaxHits();

    public enum ClashRole { NONE, MAJOR, MINOR }

    public ClashRole getClashRole() {
        return ClashRole.NONE;
    }

    public boolean isMinorClashAttack() {
        return this.getClashRole() == ClashRole.MINOR;
    }

    public boolean isClashableBeam() {
        return this.getClashRole() == ClashRole.MAJOR && this.isFiring();
    }

    /** Yaw of the fired beam axis (clash test for head-on opposition). */
    public float getClashYaw() {
        return this.getYRot();
    }

    /** Pitch of the fired beam axis. */
    public float getClashPitch() {
        return this.getXRot();
    }

    /** Current rendered length of the beam from its origin. */
    public float getClashBeamLength() {
        return 0.0F;
    }

    /** Locks this beam to a fixed length at the clash point. Length < 0 means unlocked. */
    public void setClashLock(float lockedLength, java.util.UUID opponentId) {
        this.clashLockedLength = lockedLength;
        this.clashOpponentId = opponentId;
    }

    public void clearClashLock() {
        this.clashLockedLength = -1.0F;
        this.clashOpponentId = null;
    }

    public boolean isClashLocked() {
        return this.clashLockedLength >= 0.0F;
    }

	public float getDamagePerHit() {
        return this.getKiDamage() / Math.max(1.0F, (float)this.getMaxHits());
    }

    public void setup(LivingEntity owner, float damage, float size, float speed, int colorMain, int colorBorder, int colorOutline) {
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setSize(size);
        this.setKiSpeed(speed);
        this.setColors(colorMain, colorBorder, colorOutline);
        
    }

    public void setup(LivingEntity owner, float damage, float size, float speed, int colorMain, int colorBorder) {
        this.setup(owner, damage, size, speed, colorMain, colorBorder, 0xFFFFFF);
    }

    public boolean shouldDamage(Entity target) {
        if (target == this) return false;
        if (target instanceof AbstractKiProjectile kiProj && kiProj.getOwner() == this.getOwner()) return false;
        if (target == this.getOwner() || target.is(this.getOwner())) return this.isHeal();

        if (this.getOwner() instanceof Player playerOwner) {
            TargetHelper.Relation relation = TargetHelper.getRelation(playerOwner, target);

            if (this.isHeal()) return relation == TargetHelper.Relation.FRIENDLY;
            else {
                if (relation == TargetHelper.Relation.FRIENDLY) return false;

                if (relation == TargetHelper.Relation.NEUTRAL) {
                    Vec3 viewVector = playerOwner.getViewVector(1.0F).normalize();
                    Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2.0, 0).subtract(playerOwner.getEyePosition()).normalize();

                    double dotProduct = viewVector.dot(toTarget);
                    return dotProduct > 0.95;
                }

                return true;
            }
        } else if (this.getOwner() instanceof LivingEntity ownerLiving && target instanceof LivingEntity targetLiving) {
            if (this.isHeal()) return ownerLiving.isAlliedTo(targetLiving);
            else return !ownerLiving.isAlliedTo(targetLiving);
        }

        return !this.isHeal();
    }

    public void playInitialSound(SoundEvent sound) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.PLAYERS, 0.1F, 0.8F + (this.random.nextFloat() * 0.2F));
    }

    public boolean applyDamageOrHeal(Entity target, float amount) {
        if (target instanceof LivingEntity livingTarget) {
            if (this.isHeal()) {
                if (livingTarget.getHealth() < livingTarget.getMaxHealth()) {
                    livingTarget.heal(amount);
                    return true;
                }
                return false;
            } else {
                if (!this.level().isClientSide && this.getOwner() instanceof Player ownerPlayer) {
                    float[] mod = { amount };
                    StatsProvider.get(StatsCapability.INSTANCE, ownerPlayer).ifPresent(stats -> {
                        mod[0] *= (float) stats.getKiAttackDamageModifier();
                        double bonusDmg = ownerPlayer.getPersistentData().getDouble("dmz_human_ki_bonus_dmg");
                        if (bonusDmg > 0.0) {
                            mod[0] += (float) bonusDmg;
                            ownerPlayer.getPersistentData().remove("dmz_human_ki_bonus_dmg");
                        }
                    });
                    amount = mod[0];
                }
                return livingTarget.hurt(MainDamageTypes.kiblast(this.level(), this, this.getOwner()), amount);
            }
        }
        return false;
    }

    protected Entity getKiGriefingSource() {
        Entity owner = this.getOwner();
        return owner != null ? owner : this;
    }

    protected boolean canKiDestroyBlock(BlockPos pos) {
        return MainGameRules.canKiGrief(this.level(), pos, this.getKiGriefingSource());
    }

    protected boolean destroyKiBlock(BlockPos pos, boolean dropBlock) {
        if (!this.canKiDestroyBlock(pos)) return false;
        return this.level().destroyBlock(pos, dropBlock);
    }

    protected boolean setKiBlockToAir(BlockPos pos, int flags) {
        if (!this.canKiDestroyBlock(pos)) return false;
        return this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
    }

    protected Level.ExplosionInteraction getKiExplosionInteraction(BlockPos pos) {
        return this.canKiDestroyBlock(pos) ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
    }

    public void onSuccessfulHit(Entity target) {
        if (!this.level().isClientSide && this.getOwner() instanceof net.minecraft.world.entity.player.Player player) {
            String techId = this.getTechniqueId();
            if (techId != null && !techId.isEmpty()) {
                StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
                    TechniqueData tech = stats.getTechniques().getUnlockedTechniques().get(techId);
                    if (tech instanceof KiAttackData kiAttackData) {
                        stats.getTechniques().addExperienceToTechnique(techId, kiAttackData.getXpGainPerHit());
                        applySecondaryEffect(target, kiAttackData, stats);
                    } else if (tech != null) {
                        stats.getTechniques().addExperienceToTechnique(techId, 1);
                    }
                });
            }
        }
    }

    private void applySecondaryEffect(Entity target, KiAttackData kiAttack, StatsData casterStats) {
        if (!kiAttack.hasValidSecondaryEffect()) return;
        if (!(target instanceof LivingEntity living)) return;
        KiAttackData.AffectedStat affected = kiAttack.getAffectedStat();
        if (affected == null) return;

        boolean isBuff = kiAttack.getSecondaryEffectType() == KiAttackData.SecondaryEffectType.BUFF;
        if (!secondaryRelationAllows(target, isBuff)) return;

        StatsProvider.get(StatsCapability.INSTANCE, living).ifPresent(targetStats -> {
            double magnitude = kiAttack.getSecondaryIntensity() / 100.0;
            double factor = kiAttack.getSecondaryEffectType() == KiAttackData.SecondaryEffectType.BUFF ? magnitude : -magnitude;

            double durationMult = ClassPassives.get(casterStats).secondaryDurationMultiplier(casterStats, kiAttack);
            int durationTicks = Math.max(1, (int) Math.round(kiAttack.getSecondaryDuration() * 20 * durationMult));

            targetStats.getSecondaryStatEffects().apply(affected.name(), factor, durationTicks);
        });
    }

    private boolean secondaryRelationAllows(Entity target, boolean isBuff) {
        Entity owner = this.getOwner();
        if (target == owner) return isBuff;
        if (owner instanceof Player playerOwner) {
            TargetHelper.Relation relation = TargetHelper.getRelation(playerOwner, target);
            return isBuff ? relation != TargetHelper.Relation.HOSTILE
                          : relation != TargetHelper.Relation.FRIENDLY;
        }
        if (owner instanceof LivingEntity ownerLiving && target instanceof LivingEntity targetLiving) {
            boolean allied = ownerLiving.isAlliedTo(targetLiving);
            return isBuff ? allied : !allied;
        }
        return false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR_MAIN, 0xFFFFFF);
        this.entityData.define(COLOR_BORDER, 0xFFFFFF);
        this.entityData.define(COLOR_OUTLINE, 0xFFFFFF);
        this.entityData.define(DAMAGE, 5.0f);
        this.entityData.define(SIZE, 1.0f);
        this.entityData.define(SPEED, 1.0f);
        this.entityData.define(KI_BALL_RENDER_TYPE, 1);
        this.entityData.define(TECHNIQUE_ID, "");
        this.entityData.define(ARMOR_PENETRATION, 0);
        this.entityData.define(MAX_LIFE, 100);
        this.entityData.define(IS_HEAL, false);
        this.entityData.define(KI_TYPE, KiType.SMALL_BALL.ordinal());
        this.entityData.define(IS_FIRING, false);
        this.entityData.define(FIRE_TICK, -1);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 movement = this.getDeltaMovement();
        double nextX = this.getX() + movement.x;
        double nextY = this.getY() + movement.y;
        double nextZ = this.getZ() + movement.z;
        this.setPos(nextX, nextY, nextZ);

        ProjectileUtil.rotateTowardsMovement(this, 0.2F);

        if (!this.level().isClientSide) {
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onHit(hitResult);
            }
        }

        if (!this.level().isClientSide && this.isFiring() && this.kiLifetimeDrainPerTick > 0.0f
                && this.getOwner() instanceof Player ownerPlayer) {
            this.kiDrainAccumulator += this.kiLifetimeDrainPerTick;
            int whole = (int) this.kiDrainAccumulator;
            if (whole > 0) {
                this.kiDrainAccumulator -= whole;
                final int drain = whole;
                StatsProvider.get(StatsCapability.INSTANCE, ownerPlayer)
                        .ifPresent(stats -> stats.getResources().removeEnergy(drain));
            }
        }

        this.onKiTick();

    }

    public enum KiType { SMALL_BALL, MEDIUM_BALL, GIANT_BALL, WAVE, LASER, BEAM, DISK, EXPLOSION, SHIELD, BARRAGE, AREA }

    public KiType getKiType() {
        return KiType.values()[this.entityData.get(KI_TYPE)];
    }

    public void setKiType(KiType type) {
        this.entityData.set(KI_TYPE, type.ordinal());
    }

    public void setKiType(int type) {
        this.entityData.set(KI_TYPE, type);
    }

    protected void onKiTick() {}

    public void setColors(int colorMain, int colorBorder) {
        this.setColors(colorMain, colorBorder, 0xFFFFFF);
    }

    public void setColors(int main, int border, int outline) {
        this.entityData.set(COLOR_MAIN, main);
        this.entityData.set(COLOR_BORDER, border);
        this.entityData.set(COLOR_OUTLINE, outline);
        this.cachedColorMainRgb = ColorUtils.rgbIntToFloat(main);
        this.cachedColorBorderRgb = ColorUtils.rgbIntToFloat(border);
        this.cachedColorOutlineRgb = ColorUtils.rgbIntToFloat(outline);
    }

    public void setColorOutline(int outline) {
        this.entityData.set(COLOR_OUTLINE, outline);
        this.cachedColorOutlineRgb = ColorUtils.rgbIntToFloat(outline);
    }

    public int getColor() { return this.entityData.get(COLOR_MAIN); }
    public int getColorBorder() { return this.entityData.get(COLOR_BORDER); }
    public int getColorOutline() { return this.entityData.get(COLOR_OUTLINE); }

    public float[] getRgbColorMain() {
        if (this.cachedColorMainRgb == null) this.cachedColorMainRgb = ColorUtils.rgbIntToFloat(this.getColor());
        return this.cachedColorMainRgb;
    }

    public float[] getRgbColorBorder() {
        if (this.cachedColorBorderRgb == null) this.cachedColorBorderRgb = ColorUtils.rgbIntToFloat(this.getColorBorder());
        return this.cachedColorBorderRgb;
    }

    public float[] getRgbColorOutline() {
        if (this.cachedColorOutlineRgb == null) this.cachedColorOutlineRgb = ColorUtils.rgbIntToFloat(this.getColorOutline());
        return this.cachedColorOutlineRgb;
    }

    public void setKiDamage(float damage) { this.entityData.set(DAMAGE, damage); }
    public float getKiDamage() { return this.entityData.get(DAMAGE); }
    public void setSize(float size) { this.entityData.set(SIZE, size); this.refreshDimensions();}
    public float getSize() { return this.entityData.get(SIZE); }
    public void setKiSpeed(float speed) { this.entityData.set(SPEED, speed); }
    public float getKiSpeed() { return this.entityData.get(SPEED); }
    public void setKiRenderType(int type) { this.entityData.set(KI_BALL_RENDER_TYPE, type); }
    public int getKiRenderType() { return this.entityData.get(KI_BALL_RENDER_TYPE); }
    public String getTechniqueId() { return this.entityData.get(TECHNIQUE_ID); }
    public void setTechniqueId(String id) {
        this.setTechniqueIdInternal(id, true);
    }

    private void setTechniqueIdInternal(String id, boolean triggerAnimation) {
        this.entityData.set(TECHNIQUE_ID, id);
        if (!triggerAnimation || id.isEmpty() || this.level().isClientSide || this.isFiring()) return;
        this.triggerAnimationPacket("_cast");
    }
    public int getArmorPenetration() { return this.entityData.get(ARMOR_PENETRATION); }
    public void setArmorPenetration(int pen) { this.entityData.set(ARMOR_PENETRATION, pen); }
    public boolean isHeal() { return this.entityData.get(IS_HEAL); }
    public void setHeal(boolean heal) { this.entityData.set(IS_HEAL, heal); }
    public void setMaxLife(int lifeInTicks) {this.entityData.set(MAX_LIFE, lifeInTicks); }
    public int getMaxLife() {return this.entityData.get(MAX_LIFE); }

    public boolean isFiring() { return this.entityData.get(IS_FIRING); }
    public void setFiring(boolean firing) { this.entityData.set(IS_FIRING, firing); }
    public int getFireTick() { return this.entityData.get(FIRE_TICK); }
    public void setFireTick(int tick) { this.entityData.set(FIRE_TICK, tick); }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("ColorMain", getColor());
        pCompound.putInt("ColorBorder", getColorBorder());
        pCompound.putInt("ColorOutline", getColorOutline());
        pCompound.putFloat("Damage", getKiDamage());
        pCompound.putFloat("Size", getSize());
        pCompound.putFloat("Speed", getKiSpeed());
        pCompound.putString("TechniqueId", getTechniqueId());
        pCompound.putInt("ArmorPenetration", getArmorPenetration());
        pCompound.putBoolean("IsHeal", isHeal());
        pCompound.putBoolean("IsFiring", isFiring());
        pCompound.putInt("FireTick", getFireTick());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("ColorMain")) {
            int outline = pCompound.contains("ColorOutline") ? pCompound.getInt("ColorOutline") : 0xFFFFFF;
            this.setColors(pCompound.getInt("ColorMain"), pCompound.getInt("ColorBorder"), outline);
        }
        if (pCompound.contains("Damage")) setKiDamage(pCompound.getFloat("Damage"));
        if (pCompound.contains("Size")) setSize(pCompound.getFloat("Size"));
        if (pCompound.contains("Speed")) setKiSpeed(pCompound.getFloat("Speed"));
        if (pCompound.contains("TechniqueId")) setTechniqueIdInternal(pCompound.getString("TechniqueId"), false);
        if (pCompound.contains("ArmorPenetration")) setArmorPenetration(pCompound.getInt("ArmorPenetration"));
        if (pCompound.contains("IsHeal")) setHeal(pCompound.getBoolean("IsHeal"));
        if (pCompound.contains("IsFiring")) setFiring(pCompound.getBoolean("IsFiring"));
        if (pCompound.contains("FireTick")) setFireTick(pCompound.getInt("FireTick"));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (SIZE.equals(pKey)) {
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pPose) {
        return super.getDimensions(pPose).scale(this.getSize());
    }

    public boolean isMovementRestrictedType() {
        return switch (getKiType()) {
            case GIANT_BALL, WAVE, BEAM, EXPLOSION, BARRAGE -> true;
            default -> false;
        };
    }

    public void triggerAnimationPacket(String suffix) {
        if (!this.level().isClientSide && this.getOwner() instanceof ServerPlayer sp) {
            String techId = this.getTechniqueId();
            if (techId != null && !techId.isEmpty()) {
                StatsProvider.get(StatsCapability.INSTANCE, sp).ifPresent(data -> {
                    TechniqueData tech = data.getTechniques().getUnlockedTechniques().get(techId);
                    if (tech instanceof KiAttackData kiData) {
                        String fullAnim = kiData.getAnimationPrefix() + suffix;
                        int hold = this.isMovementRestrictedType() ? 1 : 0;
                        NetworkHandler.sendToTrackingEntityAndSelf(new TriggerAnimationS2C(sp.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, hold, -1, fullAnim), sp);
                    }
                });
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> pDataValues) {
        super.onSyncedDataUpdated(pDataValues);

        if (SIZE.equals(pDataValues)) {
            this.refreshDimensions();
        }
    }

}
