package com.dragonminez.common.init.entities.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

public class EnemyMeleeBrainGoal extends Goal {

    private static final double ALLY_RADIUS = 16.0D;
    private static final double THREAT_RADIUS = 32.0D;
    private static final int UNREACHABLE_GIVE_UP = 100;
    private static final int FLEE_TICKS = 60;
    private static final int FLEE_COOLDOWN = 600;

    private final PathfinderMob mob;
    private final double speed;
    private final boolean humanoid;
    private final boolean retreatsWhenHurt;
    private final boolean canReachAir;
    private final Supplier<AiProfile> profile;
    private final CombatMemory memory = new CombatMemory();

    private int attackCooldown;
    private int repathTimer;
    private int strafeTicks;
    private int strafeDir = 1;
    private int fleeTicks;
    private long lastFleeTick = -1L;
    private int unreachableTicks;
    private int sidestepCooldown;
    private int threatScan;
    private int allyRefresh;
    private List<Mob> allies = new ArrayList<>();

    public EnemyMeleeBrainGoal(PathfinderMob mob, double speed, boolean humanoid, boolean retreatsWhenHurt, boolean canReachAir) {
        this(mob, speed, humanoid, retreatsWhenHurt, canReachAir, AiTier.NOVICE::profile);
    }

    public EnemyMeleeBrainGoal(PathfinderMob mob, double speed, boolean humanoid, boolean retreatsWhenHurt, boolean canReachAir,
                               Supplier<AiProfile> profile) {
        this.mob = mob;
        this.speed = speed;
        this.humanoid = humanoid;
        this.retreatsWhenHurt = retreatsWhenHurt;
        this.canReachAir = canReachAir;
        this.profile = profile;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) return false;
        return this.mob.isWithinRestriction(target.blockPosition());
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        this.mob.setAggressive(true);
        this.repathTimer = 0;
        this.attackCooldown = 0;
        this.unreachableTicks = 0;
    }

    @Override
    public void stop() {
        LivingEntity target = this.mob.getTarget();
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) this.mob.setTarget(null);
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        this.strafeTicks = 0;
        this.fleeTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;
        long now = this.mob.level().getGameTime();
        AiProfile p = this.profile.get();
        RandomSource rnd = this.mob.getRandom();
        this.memory.resolve(now);

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.sidestepCooldown > 0) this.sidestepCooldown--;
        if (--this.threatScan <= 0) {
            this.threatScan = p.threatScanInterval;
            if (this.sidestepCooldown <= 0) this.checkThreats(p, rnd);
        }

        if (this.retreatsWhenHurt && this.fleeTicks <= 0 && this.mob.getHealth() < this.mob.getMaxHealth() * 0.2F
                && (this.lastFleeTick < 0 || now - this.lastFleeTick > FLEE_COOLDOWN)) {
            this.fleeTicks = FLEE_TICKS;
            this.lastFleeTick = now;
            this.mob.getNavigation().stop();
        }
        if (this.fleeTicks > 0) {
            this.fleeTicks--;
            if (--this.repathTimer <= 0 || this.mob.getNavigation().isDone()) {
                this.repathTimer = 5;
                Vec3 away = new Vec3(this.mob.getX() - target.getX(), 0.0D, this.mob.getZ() - target.getZ());
                if (away.lengthSqr() < 1.0E-4D) away = this.mob.getLookAngle().reverse();
                away = away.normalize().scale(8.0D);
                this.mob.getNavigation().moveTo(this.mob.getX() + away.x, this.mob.getY(), this.mob.getZ() + away.z, this.speed * 1.3D);
            }
            return;
        }

        double yDiff = target.getY() - this.mob.getY();
        if (!this.canReachAir && yDiff > 5.0D) {
            if (++this.unreachableTicks > UNREACHABLE_GIVE_UP) {
                this.unreachableTicks = 0;
                this.mob.getNavigation().stop();
                this.mob.setTarget(null);
                return;
            }
        } else {
            this.unreachableTicks = 0;
        }

        if (--this.allyRefresh <= 0) {
            this.allyRefresh = 20;
            this.allies = SquadCoordinator.allies(this.mob, ALLY_RADIUS);
        }

        double distSqr = this.mob.distanceToSqr(target);
        double reachSqr = this.attackReachSqr(target);

        if (this.strafeTicks > 0) {
            this.strafeTicks--;
            this.faceTarget(target);
            this.mob.getMoveControl().strafe(0.0F, this.strafeDir * 0.5F);
        } else if (--this.repathTimer <= 0 || this.mob.getNavigation().isDone()) {
            this.repathTimer = 4 + rnd.nextInt(4);
            Vec3 dest = null;
            if (!this.allies.isEmpty() && distSqr > reachSqr * 2.0D) {
                dest = SquadCoordinator.flankPoint(this.mob, target, this.allies, Math.max(1.5D, Math.sqrt(reachSqr) * 0.8D));
            }
            if (dest != null) this.mob.getNavigation().moveTo(dest.x, dest.y, dest.z, this.speed);
            else this.mob.getNavigation().moveTo(target, this.speed);
        }

        if (this.attackCooldown > 0) this.attackCooldown--;
        if (this.attackCooldown <= 0 && distSqr <= reachSqr && this.mob.getSensing().hasLineOfSight(target)) {
            this.attackCooldown = this.attackInterval(rnd);
            this.mob.swing(InteractionHand.MAIN_HAND);
            boolean hit = this.mob.doHurtTarget(target);
            this.memory.recordMeleeResult(hit, now);
            if (this.humanoid && p.strafe && rnd.nextFloat() < 0.5F) {
                this.strafeTicks = 8 + rnd.nextInt(6);
                this.strafeDir = rnd.nextBoolean() ? 1 : -1;
                this.mob.getNavigation().stop();
            }
        }
    }

    private void checkThreats(AiProfile p, RandomSource rnd) {
        List<IncomingThreat> threats = ThreatScanner.scan(this.mob, THREAT_RADIUS, id -> 1.0F);
        for (IncomingThreat threat : threats) {
            if (threat.isPending() || !threat.sidestepWorks) continue;
            if (threat.originDistance < p.dodgeMinOriginDistance || threat.charge < p.dodgeMinCharge) continue;
            if (rnd.nextFloat() >= p.dodgeChance) continue;
            this.sidestep(threat.approachDir, rnd);
            this.sidestepCooldown = 60;
            return;
        }
    }

    private void sidestep(Vec3 approachDir, RandomSource rnd) {
        Vec3 dir = approachDir != null ? new Vec3(approachDir.x, 0.0D, approachDir.z) : Vec3.ZERO;
        if (dir.lengthSqr() < 1.0E-4D) dir = new Vec3(this.mob.getLookAngle().x, 0.0D, this.mob.getLookAngle().z);
        if (dir.lengthSqr() < 1.0E-4D) dir = new Vec3(1.0D, 0.0D, 0.0D);
        dir = dir.normalize();
        Vec3 left = new Vec3(-dir.z, 0.0D, dir.x);
        Vec3 right = left.reverse();
        boolean leftFree = this.mob.level().noCollision(this.mob, this.mob.getBoundingBox().move(left.scale(2.0D)));
        boolean rightFree = this.mob.level().noCollision(this.mob, this.mob.getBoundingBox().move(right.scale(2.0D)));
        Vec3 side;
        if (leftFree && rightFree) side = rnd.nextBoolean() ? left : right;
        else if (leftFree) side = left;
        else if (rightFree) side = right;
        else return;
        this.mob.getNavigation().stop();
        this.mob.setDeltaMovement(side.scale(0.9D).add(0.0D, this.mob.onGround() ? 0.3D : 0.05D, 0.0D));
        this.mob.hurtMarked = true;
        if (this.mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, this.mob.getX(), this.mob.getY() + 0.2D, this.mob.getZ(), 5, 0.2D, 0.05D, 0.2D, 0.02D);
        }
    }

    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.mob.setYRot(yaw);
        this.mob.setYBodyRot(yaw);
        this.mob.setYHeadRot(yaw);
    }

    private double attackReachSqr(LivingEntity target) {
        double w = this.mob.getBbWidth();
        return w * 2.0D * w * 2.0D + target.getBbWidth();
    }

    private int attackInterval(RandomSource rnd) {
        return 20 + rnd.nextInt(8);
    }
}
