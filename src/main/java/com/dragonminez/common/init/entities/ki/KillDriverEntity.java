package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

public class KillDriverEntity extends AbstractKiProjectile {

    private static final EntityDataAccessor<Float> RING_YAW = SynchedEntityData.defineId(KillDriverEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RING_PITCH = SynchedEntityData.defineId(KillDriverEntity.class, EntityDataSerializers.FLOAT);

    public static final String TECHNIQUE_ID = "kill_driver";
    public static final int COLOR_MAIN = 0xFFF3A0;
    public static final int COLOR_BORDER = 0xFFC400;
    public static final int COLOR_OUTLINE = 0xE08A00;
    public static final float RADIUS = 4.0F;
    public static final float FORM_SCALE = 0.35F;
    public static final float TUBE_RATIO = 0.11F;
    public static final int GROW_TICKS = 12;

    private static final int FLIGHT_LIFE = 80;
    private static final int HOLD_TIMEOUT = 60;
    private static final int PLAYER_HOLD_LIFE = 99999;
    private static final int PLAYER_RAISE_START = 30;
    private static final int PLAYER_RAISE_END = 38;
    private static final int PLAYER_FLIGHT_MIN = 40;
    private static final int PLAYER_FLIGHT_MAX = 100;
    private static final float PLAYER_MIN_SPEED = 0.6F;
    private static final double AIM_REACH = 100.0D;
    private static final double EXPLOSION_RADIUS = 5.5D;
    private static final double CONTACT_MARGIN = 0.3D;

    private int flightTicks;
    private boolean exploded;
    private float fullRadius = RADIUS;
    private int lockTargetId = -1;

    public KillDriverEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setKiType(KiType.EXPLOSION);
        this.setNoGravity(true);
    }

    public KillDriverEntity(Level level, LivingEntity owner) {
        this(MainEntities.KILL_DRIVER.get(), level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RING_YAW, 0.0F);
        this.entityData.define(RING_PITCH, 0.0F);
    }

    @Override
    public int getMaxHits() {
        return 1;
    }

    @Override
    public boolean isMovementRestrictedType() {
        return false;
    }

    public static Vec3 holdPoint(LivingEntity holder, Vec3 facing, float raise, float radius) {
        double chest = holder.getBbHeight() * 0.65D;
        double hands = holder.getBbHeight() * 1.15D;
        double height = Mth.lerp(raise, chest, hands + radius);
        double ahead = Mth.lerp(raise, 1.0D, 0.3D);
        return holder.position().add(facing.scale(ahead)).add(0.0D, height, 0.0D);
    }

    public void setup(LivingEntity owner, float damage, int colorMain, int colorBorder, int colorOutline) {
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setSize(RADIUS);
        this.setColors(colorMain, colorBorder, colorOutline);
        this.setFiring(false);
        this.setMaxLife(FLIGHT_LIFE);
        this.setBlockDestructionEnabled(false);
    }

    public void setupPlayer(LivingEntity owner, float damage, float speed, int colorMain, int colorBorder, int colorOutline, float radiusScale) {
        this.setup(owner, damage, colorMain, colorBorder, colorOutline);
        this.fullRadius = RADIUS * radiusScale;
        this.setRadius(this.fullRadius * FORM_SCALE);
        this.setKiSpeed(speed);
        this.setMaxLife(PLAYER_HOLD_LIFE);
        this.followOwner(owner);
    }

    public void setLockTarget(int entityId) {
        this.lockTargetId = entityId;
    }

    public void setRadius(float radius) {
        if (Math.abs(this.getSize() - radius) > 1.0E-3F) this.setSize(radius);
    }

    public void hold(Vec3 center, Vec3 facing) {
        this.setPos(center.x, center.y, center.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.setRingFacing(facing);
    }

    public void launch(Vec3 direction, float speed) {
        Vec3 dir = direction.lengthSqr() < 1.0E-6D ? this.ringNormal() : direction.normalize();
        this.setRingFacing(dir);
        this.setKiSpeed(speed);
        this.setDeltaMovement(dir.scale(speed));
        this.setFiring(true);
        this.flightTicks = 0;
        this.hasImpulse = true;
        this.playSound(MainSounds.KI_BEAM_FIRE.get(), 2.5F, 0.8F);
    }

    public void fireHability(int finalMaxLife) {
        this.setRadius(this.fullRadius);
        this.setFireTick(this.tickCount);
        Vec3 aim = this.getOwner() instanceof LivingEntity owner ? this.aimPoint(owner).subtract(this.position()) : this.ringNormal();
        this.launch(aim, Math.max(PLAYER_MIN_SPEED, this.getKiSpeed()));
        this.setMaxLife(Mth.clamp(finalMaxLife, PLAYER_FLIGHT_MIN, PLAYER_FLIGHT_MAX));
        if (this.getOwner() instanceof Player) this.triggerAnimationPacket("_fire");
    }

    public float getRingYaw() {
        return this.entityData.get(RING_YAW);
    }

    public float getRingPitch() {
        return this.entityData.get(RING_PITCH);
    }

    public float getGrowth(float partialTick) {
        return Mth.clamp((this.tickCount + partialTick) / GROW_TICKS, 0.0F, 1.0F);
    }

    private boolean isPlayerCharging() {
        return !this.isFiring() && this.getMaxLife() == PLAYER_HOLD_LIFE;
    }

    private void followOwner(LivingEntity owner) {
        float raise = Mth.clamp((this.tickCount - PLAYER_RAISE_START) / (float) (PLAYER_RAISE_END - PLAYER_RAISE_START), 0.0F, 1.0F);
        if (!this.level().isClientSide) this.setRadius(this.fullRadius * Mth.lerp(raise, FORM_SCALE, 1.0F));
        Vec3 facing = Vec3.directionFromRotation(0.0F, owner.getYRot());
        Vec3 center = holdPoint(owner, facing, raise, this.getSize());
        this.setPos(center.x, center.y, center.z);
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) this.setRingFacing(facing);
    }

    private Vec3 aimPoint(LivingEntity owner) {
        if (this.lockTargetId >= 0 && this.level().getEntity(this.lockTargetId) instanceof LivingEntity locked && locked.isAlive()) {
            return locked.getBoundingBox().getCenter();
        }

        Vec3 eye = owner.getEyePosition();
        Vec3 end = eye.add(owner.getLookAngle().scale(AIM_REACH));
        BlockHitResult blockHit = this.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(owner, eye, end,
                owner.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D),
                entity -> entity != this && !entity.isSpectator() && entity.isPickable(), eye.distanceToSqr(end));
        return entityHit != null ? entityHit.getLocation() : end;
    }

    private void setRingFacing(Vec3 facing) {
        Vec3 dir = facing.normalize();
        this.entityData.set(RING_YAW, (float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        this.entityData.set(RING_PITCH, (float) Math.toDegrees(-Math.asin(Mth.clamp(dir.y, -1.0D, 1.0D))));
    }

    private Vec3 ringNormal() {
        return Vec3.directionFromRotation(this.getRingPitch(), this.getRingYaw());
    }

    @Override
    public void tick() {
        boolean charging = this.isPlayerCharging();
        if (charging) {
            if (this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
                this.followOwner(owner);
            } else if (!this.level().isClientSide) {
                this.discard();
                return;
            }
        }

        super.tick();

        if (charging) this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void onKiTick() {
        if (this.level().isClientSide || this.exploded) return;

        if (!this.isFiring()) {
            if (this.isPlayerCharging()) return;
            Entity owner = this.getOwner();
            if (!(owner instanceof LivingEntity living) || !living.isAlive() || this.tickCount > HOLD_TIMEOUT) this.discard();
            return;
        }

        if (++this.flightTicks > this.getMaxLife()) {
            this.explode();
            return;
        }

        if (this.touchesVictim()) this.explode();
    }

    private boolean touchesVictim() {
        Vec3 center = this.position();
        Vec3 normal = this.ringNormal();
        double radius = this.getSize();
        double tube = radius * TUBE_RATIO;
        AABB search = new AABB(center, center).inflate(radius + tube + 2.0D);

        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, search)) {
            if (!victim.isAlive() || victim.isSpectator() || !this.shouldDamage(victim)) continue;

            Vec3 offset = victim.getBoundingBox().getCenter().subtract(center);
            double axial = offset.dot(normal);
            double radial = offset.subtract(normal.scale(axial)).length();
            double reach = Math.max(victim.getBbWidth(), victim.getBbHeight()) * 0.5D;

            if (Math.abs(axial) <= tube + reach + CONTACT_MARGIN && radial <= radius + tube + reach) return true;
        }
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        if (!this.level().isClientSide && this.isFiring() && this.shouldDamage(pResult.getEntity())) this.explode();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        if (!this.level().isClientSide && this.isFiring()) this.explode();
    }

    private void explode() {
        if (this.exploded || !(this.level() instanceof ServerLevel serverLevel)) return;
        this.exploded = true;

        Vec3 center = this.position();
        float damage = this.getKiDamage();
        AABB area = new AABB(center, center).inflate(EXPLOSION_RADIUS);

        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!victim.isAlive() || victim.isSpectator() || !this.shouldDamage(victim)) continue;
            Vec3 offset = victim.getBoundingBox().getCenter().subtract(center);
            if (offset.lengthSqr() > EXPLOSION_RADIUS * EXPLOSION_RADIUS) continue;

            victim.invulnerableTime = 0;
            if (this.applyDamageOrHeal(victim, damage)) this.onSuccessfulHit(victim);

            Vec3 push = offset.lengthSqr() < 1.0E-4D ? this.getDeltaMovement() : offset;
            push = push.normalize().scale(1.3D);
            victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.55D, push.z));
            victim.hasImpulse = true;
            victim.hurtMarked = true;
        }

        KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), serverLevel);
        visual.setupExplosion(this.getColor(), this.getColorBorder(), this.getColorOutline(), this.getSize() * 0.9F);
        visual.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        serverLevel.addFreshEntity(visual);

        NetworkHandler.sendToTrackingEntity(new ShockwaveVfxS2C(center.x, center.y, center.z,
                (float) EXPLOSION_RADIUS * 1.6F, this.getColorBorder(), 18), this);
        serverLevel.playSound(null, center.x, center.y, center.z, MainSounds.KI_EXPLOSION_IMPACT.get(),
                this.getSoundSource(), 3.0F, 0.8F);

        this.discard();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(this.getSize() * 1.5F + 2.0F);
    }
}
