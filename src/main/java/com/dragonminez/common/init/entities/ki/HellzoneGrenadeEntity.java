package com.dragonminez.common.init.entities.ki;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.util.MultipartTargeting;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainParticles;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ShockwaveVfxS2C;
import com.dragonminez.server.events.players.combat.HeldVictim;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HellzoneGrenadeEntity extends AbstractKiProjectile {

    public static final String TECHNIQUE_ID = "hellzone_grenade";
    public static final int FINALE_OUTLINE = 0xFF3B1F;

    private static final int CHARGE_LIFE = 99999;
    private static final int OPENING_SHOTS = 4;
    private static final int OPENING_INTERVAL = 2;
    private static final int SCATTER_START = 7;
    private static final int SCATTER_END = 28;
    private static final int HOVER_HOLD = 25;
    private static final int CLENCH_TICK = 36 + HOVER_HOLD;
    private static final int CONVERGE_TICK = 44 + HOVER_HOLD;
    private static final int FINALE_TICK = 52 + HOVER_HOLD;
    private static final int FINALE_CHAIN_TICKS = 7;
    private static final int END_TICK = 70 + HOVER_HOLD;

    private static final int MIN_ORBS = 18;
    private static final int MAX_ORBS = 48;
    private static final float MAX_CHARGE = 1.75F;
    private static final float OPENING_SHARE = 0.15F;
    private static final float CONVERGE_SHARE = 0.40F;
    private static final float FINALE_SHARE = 0.45F;

    private static final double AIM_REACH = 48.0D;
    private static final double ANCHOR_FOLLOW = 0.35D;
    private static final double BASE_SHELL_RADIUS = 3.5D;
    private static final double MAX_SHELL_RADIUS = 9.0D;
    private static final double SLOT_WALL_MARGIN = 0.8D;
    private static final double SLOT_REFRESH_DISTANCE = 0.75D;
    private static final double LOWEST_SLOT_Y = -0.45D;
    private static final double THROW_ARC = 0.9D;
    private static final float MIN_CRUISE = 1.3F;
    private static final float OPENING_SPEED = 2.2F;
    private static final float CONVERGE_SPEED = 1.6F;
    private static final float FINALE_CRATER = 2.5F;
    private static final int FINALE_BURSTS_PER_TICK = 3;

    private final List<Orb> orbs = new ArrayList<>();
    private final List<Orb> shell = new ArrayList<>();
    private final Deque<Vec3> rightSlots = new ArrayDeque<>();
    private final Deque<Vec3> leftSlots = new ArrayDeque<>();
    private int orbCount;
    private int targetId = -1;
    private Vec3 anchor;
    private Vec3 slotAnchor;
    private double shellRadius = BASE_SHELL_RADIUS;
    private boolean clenched;
    private boolean converged;
    private boolean finaleDone;
    private boolean handsFormed;
    private int sequenceTick;
    private Orb rightHandOrb;
    private Orb leftHandOrb;

    public HellzoneGrenadeEntity(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setKiType(KiType.BARRAGE);
        this.setNoGravity(true);
    }

    public HellzoneGrenadeEntity(Level level, LivingEntity owner) {
        this(MainEntities.HELLZONE_GRENADE.get(), level);
        this.setOwner(owner);
    }

    @Override
    public int getMaxHits() {
        return 1;
    }

    public void setupPlayer(LivingEntity owner, float damage, float speed, int colorMain, int colorBorder, int colorOutline, float orbSize) {
        this.setOwner(owner);
        this.setKiDamage(damage);
        this.setKiSpeed(speed);
        this.setColors(colorMain, colorBorder, colorOutline);
        this.setSize(orbSize);
        this.setFiring(false);
        this.setMaxLife(CHARGE_LIFE);
        this.followOwner(owner);
        this.playInitialSound(MainSounds.KI_EXPLOSION_CHARGE.get());
    }

    public boolean fireHability(float charge, int lockedTargetId) {
        if (!(this.getOwner() instanceof LivingEntity owner)) return false;

        LivingEntity target = findTarget(owner, lockedTargetId);
        if (target == null) target = this.chargeTarget(owner);
        if (target == null) return false;

        this.bindTarget(target);
        this.setFiring(true);
        this.setFireTick(this.tickCount);
        this.setMaxLife(END_TICK);

        float progress = Mth.clamp(charge / MAX_CHARGE, 0.0F, 1.0F);
        this.orbCount = Math.round(Mth.lerp(progress, (float) MIN_ORBS, (float) MAX_ORBS));
        this.buildSlots(owner);

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
        if (owner instanceof Player) this.triggerAnimationPacket("_fire");
        return true;
    }

    public static LivingEntity findTarget(LivingEntity owner, int lockedTargetId) {
        Level level = owner.level();
        if (lockedTargetId >= 0 && TargetHelper.resolveHittable(level.getEntity(lockedTargetId)) instanceof LivingEntity locked
                && isValidTarget(owner, locked)) {
            return locked;
        }

        Vec3 eye = owner.getEyePosition();
        Vec3 end = eye.add(owner.getLookAngle().scale(AIM_REACH));
        BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(owner, eye, end,
                owner.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D),
                entity -> !entity.isSpectator() && entity.isPickable()
                        && TargetHelper.resolveHittable(entity) instanceof LivingEntity living && isValidTarget(owner, living),
                eye.distanceToSqr(end));
        if (entityHit != null && TargetHelper.resolveHittable(entityHit.getEntity()) instanceof LivingEntity living) return living;
        return null;
    }

    private static boolean isValidTarget(LivingEntity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator()) return false;
        if (owner instanceof Player player) return TargetHelper.getRelation(player, target) != TargetHelper.Relation.FRIENDLY;
        return !owner.isAlliedTo(target);
    }

    public void setChargeTarget(LivingEntity target) {
        this.targetId = target.getId();
    }

    private LivingEntity chargeTarget(LivingEntity owner) {
        if (this.targetId < 0 || !(this.level().getEntity(this.targetId) instanceof LivingEntity stored)) return null;
        if (!isValidTarget(owner, stored) || stored.distanceToSqr(owner) > AIM_REACH * AIM_REACH) return null;
        return stored;
    }

    private void bindTarget(LivingEntity target) {
        this.targetId = target.getId();
        this.anchor = target.getBoundingBox().getCenter();
        double extent = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5D;
        this.shellRadius = Math.min(MAX_SHELL_RADIUS, BASE_SHELL_RADIUS + this.getSize() * 0.6D + extent);
    }

    private void buildSlots(LivingEntity owner) {
        Vec3 right = Vec3.directionFromRotation(0.0F, owner.getYRot() + 90.0F);
        List<Vec3> directions = new ArrayList<>(this.orbCount);
        double golden = Math.PI * (3.0D - Math.sqrt(5.0D));
        double spin = this.random.nextDouble() * Math.PI * 2.0D;

        for (int i = 0; i < this.orbCount; i++) {
            double y = Mth.lerp((i + 0.5D) / this.orbCount, 1.0D, LOWEST_SLOT_Y);
            double ring = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double theta = spin + golden * i;
            directions.add(new Vec3(Math.cos(theta) * ring, y, Math.sin(theta) * ring));
        }

        for (int i = directions.size() - 1; i > 0; i--) {
            int j = this.random.nextInt(i + 1);
            Vec3 swap = directions.get(i);
            directions.set(i, directions.get(j));
            directions.set(j, swap);
        }

        for (Vec3 direction : directions) {
            if (direction.dot(right) >= 0.0D) this.rightSlots.add(direction);
            else this.leftSlots.add(direction);
        }
    }

    private Vec3 nextSlot(boolean rightHand) {
        Deque<Vec3> preferred = rightHand ? this.rightSlots : this.leftSlots;
        Deque<Vec3> fallback = rightHand ? this.leftSlots : this.rightSlots;
        if (!preferred.isEmpty()) return preferred.poll();
        if (!fallback.isEmpty()) return fallback.poll();
        double theta = this.random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(theta), 0.3D, Math.sin(theta)).normalize();
    }

    private Vec3 slotOffset(Vec3 direction) {
        Vec3 desired = this.anchor.add(direction.scale(this.shellRadius));
        BlockHitResult wall = this.level().clip(new ClipContext(this.anchor, desired, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() == HitResult.Type.MISS) return direction.scale(this.shellRadius);
        double reach = Math.max(0.6D, this.anchor.distanceTo(wall.getLocation()) - SLOT_WALL_MARGIN);
        return direction.scale(reach);
    }

    private float cruiseSpeed() {
        return Math.max(MIN_CRUISE, this.getKiSpeed());
    }

    private void followOwner(LivingEntity owner) {
        this.setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.6D, owner.getZ());
        this.setDeltaMovement(Vec3.ZERO);
    }

    private Vec3 handPosition(LivingEntity owner, boolean rightHand) {
        Vec3 facing = Vec3.directionFromRotation(0.0F, owner.getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, owner.getYRot() + 90.0F);
        double side = owner.getBbWidth() * 0.5D + 0.3D;
        return owner.position()
                .add(0.0D, owner.getBbHeight() * 0.72D, 0.0D)
                .add(right.scale(rightHand ? side : -side))
                .add(facing.scale(0.6D));
    }

    private Orb spawnOrb(LivingEntity owner, boolean rightHand) {
        Orb orb = new Orb(this.level(), owner, this);
        orb.setSize(this.getSize());
        Vec3 hand = this.handPosition(owner, rightHand);
        orb.setPos(hand.x, hand.y - orb.getBbHeight() * 0.5D, hand.z);
        orb.setColors(this.getColor(), this.getColorBorder(), this.getColorOutline());
        orb.setKiSpeed(this.cruiseSpeed());
        orb.setKiDamage(this.getKiDamage());
        orb.setFiring(true);
        orb.setTechniqueId(this.getTechniqueId());
        orb.setArmorPenetration(this.getArmorPenetration());
        orb.setHeal(this.isHeal());
        this.level().addFreshEntity(orb);
        this.orbs.add(orb);
        return orb;
    }

    private void formHandOrbs(LivingEntity owner) {
        this.handsFormed = true;
        this.rightHandOrb = this.formHandOrb(owner, true);
        this.leftHandOrb = this.formHandOrb(owner, false);
    }

    private Orb formHandOrb(LivingEntity owner, boolean rightHand) {
        Orb orb = new Orb(this.level(), owner, this);
        orb.setColors(this.getColor(), this.getColorBorder(), this.getColorOutline());
        orb.setKiSpeed(this.cruiseSpeed());
        orb.setKiDamage(this.getKiDamage());
        orb.setArmorPenetration(this.getArmorPenetration());
        orb.setHeal(this.isHeal());
        orb.hold(rightHand, this.getSize());
        Vec3 hand = Orb.chargeHand(owner, rightHand);
        orb.setPos(hand.x, hand.y - orb.getBbHeight() * 0.5D, hand.z);
        this.level().addFreshEntity(orb);
        this.orbs.add(orb);
        return orb;
    }

    private Orb releaseHandOrb(boolean rightHand) {
        Orb orb = rightHand ? this.rightHandOrb : this.leftHandOrb;
        if (rightHand) this.rightHandOrb = null;
        else this.leftHandOrb = null;
        if (orb == null || orb.isRemoved()) return null;
        orb.release(this.getTechniqueId());
        return orb;
    }

    private void throwOpening(LivingEntity owner, int index) {
        boolean rightHand = index % 2 == 0;
        Orb orb = index < 2 ? this.releaseHandOrb(rightHand) : null;
        if (orb == null) orb = this.spawnOrb(owner, rightHand);
        float damage = this.getKiDamage() * OPENING_SHARE / OPENING_SHOTS;
        float speed = Math.max(OPENING_SPEED, this.cruiseSpeed());
        orb.dive(this.targetId, this.anchor, damage, speed, this.anchor.subtract(orb.center()), true);
        this.level().playSound(null, orb.getX(), orb.getY(), orb.getZ(), MainSounds.KIBLAST_ATTACK.get(),
                SoundSource.PLAYERS, 0.6F, 1.1F + this.random.nextFloat() * 0.3F);
    }

    private void scatter(LivingEntity owner, int tick) {
        int window = SCATTER_END - SCATTER_START + 1;
        int due = Math.min(this.orbCount, Mth.ceil((tick - SCATTER_START + 1) * this.orbCount / (float) window));
        Vec3 right = Vec3.directionFromRotation(0.0F, owner.getYRot() + 90.0F);

        while (this.shell.size() < due) {
            boolean rightHand = this.shell.size() % 2 == 0;
            Vec3 direction = this.nextSlot(rightHand);
            Orb orb = this.spawnOrb(owner, rightHand);
            orb.assignSlot(direction, this.slotOffset(direction), this.random.nextDouble() * Math.PI * 2.0D);

            Vec3 goal = this.anchor.add(orb.slotOffset);
            Vec3 toward = goal.subtract(orb.center());
            toward = toward.lengthSqr() < 1.0E-4D ? Vec3.directionFromRotation(0.0F, owner.getYRot()) : toward.normalize();
            Vec3 launch = toward.add(right.scale(rightHand ? THROW_ARC : -THROW_ARC))
                    .add(0.0D, 0.3D + this.random.nextDouble() * 0.4D, 0.0D).normalize();
            orb.seek(goal, launch.scale(this.cruiseSpeed()), this.cruiseSpeed());
            this.shell.add(orb);
        }

        if (tick % 2 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), MainSounds.KIBLAST_ATTACK.get(),
                    SoundSource.PLAYERS, 0.35F, 1.3F + this.random.nextFloat() * 0.5F);
        }
    }

    private void followTarget() {
        if (this.targetId < 0 || this.finaleDone) return;
        if (this.level().getEntity(this.targetId) instanceof LivingEntity target && target.isAlive()) {
            Vec3 center = target.getBoundingBox().getCenter();
            this.anchor = this.anchor.add(center.subtract(this.anchor).scale(ANCHOR_FOLLOW));
        }
    }

    private void steerShell(int tick) {
        if (this.slotAnchor == null || this.slotAnchor.distanceToSqr(this.anchor) > SLOT_REFRESH_DISTANCE * SLOT_REFRESH_DISTANCE) {
            this.slotAnchor = this.anchor;
            for (Orb orb : this.shell) {
                if (!orb.isRemoved()) orb.slotOffset = this.slotOffset(orb.slotDirection);
            }
        }

        for (Orb orb : this.shell) {
            if (orb.isRemoved() || orb.diving) continue;
            double bob = Math.sin(tick * 0.3D + orb.bobPhase) * 0.15D;
            orb.goal = this.anchor.add(orb.slotOffset).add(0.0D, bob, 0.0D);
        }
    }

    private void converge() {
        this.converged = true;
        float damage = this.getKiDamage() * CONVERGE_SHARE / Math.max(1, this.orbCount);
        for (Orb orb : this.shell) {
            if (orb.isRemoved()) continue;
            orb.dive(this.targetId, this.anchor, damage, CONVERGE_SPEED, Vec3.ZERO, false);
        }
        this.level().playSound(null, this.anchor.x, this.anchor.y, this.anchor.z, MainSounds.KI_BEAM_FIRE.get(),
                SoundSource.PLAYERS, 2.0F, 1.3F);
    }

    private void finale() {
        this.finaleDone = true;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Vec3 center = this.anchor;
        double radius = this.shellRadius + 1.0D;
        float damage = this.getKiDamage() * FINALE_SHARE;

        for (LivingEntity victim : MultipartTargeting.collectTargets(serverLevel, new AABB(center, center).inflate(radius))) {
            if (!victim.isAlive() || victim.isSpectator() || !this.shouldDamage(victim)) continue;
            if (!MultipartTargeting.withinRadius(victim, center, radius)) continue;

            victim.invulnerableTime = 0;
            if (this.applyDamageOrHeal(victim, damage)) this.onSuccessfulHit(victim);

            Vec3 push = victim.getBoundingBox().getCenter().subtract(center);
            push = push.lengthSqr() < 1.0E-4D ? Vec3.ZERO : push.normalize().scale(0.8D);
            victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.45D, push.z));
            victim.hasImpulse = true;
            victim.hurtMarked = true;
        }

        KiExplosionVisualEntity visual = new KiExplosionVisualEntity(MainEntities.KI_EXPLOSION_VISUAL.get(), serverLevel);
        visual.setupExplosion(this.getColor(), this.getColorOutline(), FINALE_OUTLINE, (float) this.shellRadius * 0.85F);
        visual.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
        serverLevel.addFreshEntity(visual);

        ShockwaveVfxS2C shockwave = new ShockwaveVfxS2C(center.x, center.y, center.z, (float) radius * 1.6F, this.getColorOutline(), 18);
        Entity carrier = this.targetId >= 0 ? serverLevel.getEntity(this.targetId) : null;
        if (carrier != null) NetworkHandler.sendToTrackingEntityAndSelf(shockwave, carrier);
        else NetworkHandler.sendToTrackingEntity(shockwave, this);

        serverLevel.playSound(null, center.x, center.y, center.z, MainSounds.KI_EXPLOSION_IMPACT.get(), SoundSource.PLAYERS, 3.0F, 0.75F);
        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0F, 0.7F);

        this.carveKiSphere(BlockPos.containing(center), this.scaledDestructionRadius(FINALE_CRATER), 2);

        for (Orb orb : this.shell) {
            if (!orb.isRemoved()) orb.discard();
        }
    }

    private void chainBursts() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        for (int i = 0; i < FINALE_BURSTS_PER_TICK; i++) {
            double theta = this.random.nextDouble() * Math.PI * 2.0D;
            double y = this.random.nextDouble() * 2.0D - 1.0D;
            double ring = Math.sqrt(1.0D - y * y);
            double reach = this.shellRadius * (0.25D + this.random.nextDouble() * 0.6D);
            double x = this.anchor.x + Math.cos(theta) * ring * reach;
            double z = this.anchor.z + Math.sin(theta) * ring * reach;
            double size = this.shellRadius * (0.55D + this.random.nextDouble() * 0.45D);
            serverLevel.sendParticles(MainParticles.KI_EXPLOSION.get(), x, this.anchor.y + y * reach, z, 0, size, 0.0D, 0.0D, 1.0D);
        }
        if (this.random.nextBoolean()) {
            serverLevel.playSound(null, this.anchor.x, this.anchor.y, this.anchor.z, MainSounds.KI_EXPLOSION_IMPACT.get(),
                    SoundSource.PLAYERS, 1.6F, 0.8F + this.random.nextFloat() * 0.3F);
        }
    }

    @Override
    public void tick() {
        if (this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
            this.followOwner(owner);
            if (!this.level().isClientSide && !this.isFiring() && !this.handsFormed) this.formHandOrbs(owner);
        } else if (!this.level().isClientSide) {
            this.discard();
            return;
        }

        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void onKiTick() {
        if (this.level().isClientSide || !this.isFiring() || this.anchor == null) return;
        if (!(this.getOwner() instanceof LivingEntity owner)) return;

        int tick = this.sequenceTick++;
        this.followTarget();

        if (tick < OPENING_SHOTS * OPENING_INTERVAL && tick % OPENING_INTERVAL == 0) this.throwOpening(owner, tick / OPENING_INTERVAL);
        if (tick >= SCATTER_START && tick <= SCATTER_END) this.scatter(owner, tick);
        if (!this.converged) this.steerShell(tick);

        if (!this.clenched && tick >= CLENCH_TICK) {
            this.clenched = true;
            if (owner instanceof Player) this.triggerAnimationPacket("_clench");
        }
        if (!this.converged && tick >= CONVERGE_TICK) this.converge();
        if (!this.finaleDone && tick >= FINALE_TICK) this.finale();
        if (this.finaleDone && tick < FINALE_TICK + FINALE_CHAIN_TICKS) this.chainBursts();
        if (tick >= END_TICK) this.discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            for (Orb orb : this.orbs) {
                if (!orb.isRemoved()) orb.discard();
            }
        }
        super.remove(reason);
    }

    public static class Orb extends AbstractKiProjectile {

        private static final EntityDataAccessor<Integer> HELD_HAND = SynchedEntityData.defineId(Orb.class, EntityDataSerializers.INT);

        private static final double HAND_MARGIN = 0.35D;
        private static final double HAND_HEIGHT = 0.42D;
        private static final float HELD_START_SCALE = 0.2F;
        private static final int GROW_TICKS = 8;
        private static final int MAX_AGE = 140;
        private static final int DIVE_TIMEOUT = 24;
        private static final double ARRIVE_GAIN = 0.3D;
        private static final double SEEK_STEER = 0.3D;
        private static final double DIVE_STEER = 0.55D;
        private static final double ARRIVE_DISTANCE = 0.6D;
        private static final float IMPACT_RADIUS = 1.3F;
        private static final int STAGGER_TICKS = 15;

        private HellzoneGrenadeEntity controller;
        private Vec3 goal;
        private Vec3 slotDirection = Vec3.ZERO;
        private Vec3 slotOffset = Vec3.ZERO;
        private double bobPhase;
        private float cruise = MIN_CRUISE;
        private boolean diving;
        private boolean stagger;
        private int diveTargetId = -1;
        private int diveTicks;
        private float hitDamage;
        private boolean exploded;
        private float fullSize;
        private int freeTicks;

        public Orb(EntityType<? extends Projectile> pEntityType, Level pLevel) {
            super(pEntityType, pLevel);
            this.setKiType(KiType.BARRAGE);
            this.setKiRenderType(0);
            this.setNoGravity(true);
        }

        public Orb(Level level, LivingEntity owner, HellzoneGrenadeEntity controller) {
            this(MainEntities.HELLZONE_GRENADE_ORB.get(), level);
            this.setOwner(owner);
            this.controller = controller;
        }

        @Override
        protected void defineSynchedData() {
            super.defineSynchedData();
            this.entityData.define(HELD_HAND, 0);
        }

        @Override
        public int getMaxHits() {
            return 1;
        }

        public Vec3 center() {
            return this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        }

        public boolean isHeld() {
            return this.entityData.get(HELD_HAND) != 0;
        }

        public static Vec3 chargeHand(LivingEntity owner, boolean rightHand) {
            float scale = ownerScaleOf(owner);
            Vec3 facing = Vec3.directionFromRotation(0.0F, owner.yBodyRot);
            Vec3 right = Vec3.directionFromRotation(0.0F, owner.yBodyRot + 90.0F);
            double side = owner.getBbWidth() * 0.5D + HAND_MARGIN * scale;
            return owner.position()
                    .add(0.0D, owner.getBbHeight() * HAND_HEIGHT, 0.0D)
                    .add(right.scale(rightHand ? side : -side))
                    .add(facing.scale(-0.05D * scale));
        }

        private void hold(boolean rightHand, float size) {
            this.entityData.set(HELD_HAND, rightHand ? 1 : -1);
            this.fullSize = size;
            this.setSize(size * HELD_START_SCALE);
            this.setFiring(false);
        }

        private void release(String techniqueId) {
            this.entityData.set(HELD_HAND, 0);
            if (this.fullSize > 0.0F) this.setSize(this.fullSize);
            this.setFiring(true);
            this.setTechniqueId(techniqueId);
        }

        @Override
        public void tick() {
            if (this.isHeld()) {
                if (this.getOwner() instanceof LivingEntity owner && owner.isAlive()) {
                    Vec3 hand = chargeHand(owner, this.entityData.get(HELD_HAND) > 0);
                    this.setPos(hand.x, hand.y - this.getBbHeight() * 0.5D, hand.z);
                    this.setDeltaMovement(Vec3.ZERO);
                } else if (!this.level().isClientSide) {
                    this.discard();
                    return;
                }
            }
            super.tick();
        }

        @Override
        public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pLerpSteps, boolean pTeleport) {
            if (this.isHeld()) return;
            super.lerpTo(pX, pY, pZ, pYRot, pXRot, pLerpSteps, pTeleport);
        }

        private void assignSlot(Vec3 direction, Vec3 offset, double phase) {
            this.slotDirection = direction;
            this.slotOffset = offset;
            this.bobPhase = phase;
        }

        private void seek(Vec3 target, Vec3 launch, float speed) {
            this.goal = target;
            this.cruise = speed;
            this.setDeltaMovement(launch);
            this.hasImpulse = true;
        }

        private void dive(int targetId, Vec3 fallback, float damage, float speed, Vec3 launch, boolean staggers) {
            this.diving = true;
            this.diveTargetId = targetId;
            this.goal = fallback;
            this.hitDamage = damage;
            this.cruise = speed;
            this.stagger = staggers;
            this.diveTicks = 0;
            if (launch.lengthSqr() > 1.0E-6D) this.setDeltaMovement(launch.normalize().scale(speed));
            this.hasImpulse = true;
        }

        @Override
        protected boolean canHitEntity(Entity target) {
            return this.diving && super.canHitEntity(target) && this.shouldDamage(target);
        }

        @Override
        protected void onHitEntity(EntityHitResult pResult) {
            if (!this.level().isClientSide && this.diving) this.explode();
        }

        @Override
        protected void onHitBlock(BlockHitResult pResult) {
            if (!this.level().isClientSide && this.diving) this.explode();
        }

        @Override
        protected void onKiTick() {
            if (this.level().isClientSide || this.exploded) return;
            if (this.controller == null || this.controller.isRemoved()) {
                this.discard();
                return;
            }

            if (this.isHeld()) {
                if (this.getSize() < this.fullSize) this.setSize(Math.min(this.fullSize, this.getSize() + this.fullSize / GROW_TICKS));
                return;
            }

            if (++this.freeTicks > MAX_AGE) {
                this.discard();
                return;
            }

            if (this.diving) this.tickDive();
            else this.tickSeek();
        }

        private void tickSeek() {
            if (this.goal == null) return;
            Vec3 toGoal = this.goal.subtract(this.center());
            double distance = toGoal.length();
            double speed = Math.min(this.cruise, distance * ARRIVE_GAIN);
            Vec3 desired = distance < 1.0E-3D ? Vec3.ZERO : toGoal.scale(speed / distance);
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(velocity.add(desired.subtract(velocity).scale(SEEK_STEER)));
        }

        private void tickDive() {
            Vec3 aim = this.goal;
            if (this.diveTargetId >= 0 && this.level().getEntity(this.diveTargetId) instanceof LivingEntity target && target.isAlive()) {
                aim = target.getBoundingBox().getCenter();
            }
            if (aim == null) {
                this.explode();
                return;
            }

            Vec3 toAim = aim.subtract(this.center());
            double distance = toAim.length();
            if (++this.diveTicks > DIVE_TIMEOUT || distance <= Math.max(ARRIVE_DISTANCE, this.cruise * 0.5D)) {
                this.explode();
                return;
            }

            Vec3 desired = toAim.scale(this.cruise / distance);
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(velocity.add(desired.subtract(velocity).scale(DIVE_STEER)));
        }

        private void explode() {
            if (this.exploded || !(this.level() instanceof ServerLevel serverLevel)) return;
            this.exploded = true;

            Vec3 center = this.center();
            double radius = IMPACT_RADIUS + this.getSize() * 0.5D;
            Entity owner = this.getOwner();

            for (LivingEntity victim : MultipartTargeting.collectTargets(serverLevel, new AABB(center, center).inflate(radius))) {
                if (!victim.isAlive() || victim.isSpectator() || !this.shouldDamage(victim)) continue;
                if (!MultipartTargeting.withinRadius(victim, center, radius)) continue;

                victim.invulnerableTime = 0;
                if (!this.applyDamageOrHeal(victim, this.hitDamage)) continue;
                this.onSuccessfulHit(victim);
                if (this.stagger && owner instanceof LivingEntity holder && HeldVictim.canHold(holder, victim)) {
                    victim.addEffect(new MobEffectInstance(MainEffects.STUN.get(), STAGGER_TICKS, 0, false, false, true));
                }
            }

            serverLevel.sendParticles(MainParticles.KI_EXPLOSION.get(), center.x, center.y, center.z, 0, this.getSize() * 2.4D, 0.0D, 0.0D, 1.0D);
            if (this.random.nextInt(3) == 0) {
                serverLevel.playSound(null, center.x, center.y, center.z, MainSounds.KI_EXPLOSION_IMPACT.get(),
                        SoundSource.PLAYERS, 1.2F, 0.9F + this.random.nextFloat() * 0.4F);
            }
            this.discard();
        }
    }
}
