package com.dragonminez.common.combat.clash;

import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BeamClash {
    public static final float BURST_PER_PERFECT_PRESS = 1.0f;
    public static final float MOMENTUM_DECAY = 0.94f;

    private static final float DRIFT_PER_TICK = 0.010f;
    private static final float STR_FLOOR = 0.6f;
    private static final float STR_SPAN = 0.95f;
    private static final float MIN_TRACTION = 0.35f;
    private static final float WIN_THRESHOLD = 0.8f;
    private static final int IDLE_DISSOLVE_TICKS = 140;
    private static final int MAX_DURATION = 600;
    private static final int WINNER_BREAKTHROUGH_TICKS = 60;
    public static final int LOSER_EXHAUST_TICKS = 70;

    public enum Result { ONGOING, A_WINS, B_WINS, DISSOLVED }

    private final ClashParticipant a;
    private final ClashParticipant b;
    private final long startGameTime;
    private final Level level;
    private final long startNanos = System.nanoTime();
    private float biasT;
    private int age = 0;
    private Vec3 clashPoint;
    @Getter
    private boolean ended = false;

    public BeamClash(ClashParticipant a, ClashParticipant b, long startGameTime) {
        this.a = a;
        this.b = b;
        this.startGameTime = startGameTime;
        this.level = a.beam().level();
        this.biasT = 0.5f;
        this.clashPoint = midpointOfTips(a.beam().getClashBeamLength(), b.beam().getClashBeamLength());
    }

    public ClashParticipant a() {
        return a;
    }

    public ClashParticipant b() {
        return b;
    }

    public long startGameTime() {
        return startGameTime;
    }

    public float realElapsedTicks() {
        return (System.nanoTime() - startNanos) / 50_000_000.0f;
    }

    public Level level() {
        return level;
    }

    public Vec3 clashPoint() {
        return clashPoint;
    }

    public boolean involves(AbstractKiProjectile beam) {
        return a.beam() == beam || b.beam() == beam;
    }

    public boolean involvesOwner(java.util.UUID ownerId) {
        return a.owner().getUUID().equals(ownerId) || b.owner().getUUID().equals(ownerId);
    }

    public ClashParticipant participantFor(java.util.UUID ownerId) {
        if (a.owner().getUUID().equals(ownerId)) return a;
        if (b.owner().getUUID().equals(ownerId)) return b;
        return null;
    }

    public Result tick() {
        if (ended) return Result.DISSOLVED;
        if (!a.isStillFiring() || !b.isStillFiring()) {
            return Result.DISSOLVED;
        }
        age++;

        a.freezeOwner();
        b.freezeOwner();

        a.tickMeter(age);
        b.tickMeter(age);

        double pa = a.statPower();
        double pb = b.statPower();
        float strengthA = (float) (pa / (pa + pb));
        float strengthB = 1.0f - strengthA;

        float tracA = Mth.clamp(2.0f * biasT, MIN_TRACTION, 1.0f);
        float tracB = Mth.clamp(2.0f * (1.0f - biasT), MIN_TRACTION, 1.0f);

        float pushA = a.momentum() * (STR_FLOOR + STR_SPAN * strengthA) * tracA;
        float pushB = b.momentum() * (STR_FLOOR + STR_SPAN * strengthB) * tracB;

        biasT += DRIFT_PER_TICK * (pushA - pushB);
        biasT = Mth.clamp(biasT, 0.0f, 1.0f);

        applyLock();

        if (biasT >= WIN_THRESHOLD) return Result.A_WINS;
        if (biasT <= 1.0f - WIN_THRESHOLD) return Result.B_WINS;
        if (Math.min(a.idleTicks(), b.idleTicks()) >= IDLE_DISSOLVE_TICKS) return Result.DISSOLVED;
        if (age >= MAX_DURATION) {
            if (biasT > 0.5f) return Result.A_WINS;
            if (biasT < 0.5f) return Result.B_WINS;
            return Result.DISSOLVED;
        }
        return Result.ONGOING;
    }

    public static float visualBias(float bias) {
        return Mth.clamp((bias - (1.0f - WIN_THRESHOLD)) / (2.0f * WIN_THRESHOLD - 1.0f), 0.0f, 1.0f);
    }

    private void applyLock() {
        Vec3 originA = a.origin();
        Vec3 originB = b.origin();
        double gap = originA.distanceTo(originB);

        float visual = visualBias(biasT);
        float lockA = (float) (gap * visual);
        float lockB = (float) (gap * (1.0f - visual));

        a.beam().setClashLock(lockA, b.owner().getUUID());
        b.beam().setClashLock(lockB, a.owner().getUUID());

        keepAlive(a.beam());
        keepAlive(b.beam());

        clashPoint = midpointOfTips(lockA, lockB);
    }

    private Vec3 midpointOfTips(float lengthA, float lengthB) {
        Vec3 tipA = a.origin().add(a.direction().scale(Math.max(0.0f, lengthA)));
        Vec3 tipB = b.origin().add(b.direction().scale(Math.max(0.0f, lengthB)));
        return tipA.add(tipB).scale(0.5);
    }

    private static void keepAlive(AbstractKiProjectile beam) {
        beam.setMaxLife(beam.tickCount + 40);
    }

    public void resolve(Result result) {
        if (ended) return;
        ended = true;

        ClashParticipant winner = result == Result.A_WINS ? a : b;
        ClashParticipant loser = result == Result.A_WINS ? b : a;

        winner.beam().clearClashLock();
        winner.beam().setMaxLife(winner.beam().tickCount + WINNER_BREAKTHROUGH_TICKS);

        loser.beam().clearClashLock();
        if (!loser.beam().isRemoved()) {
            loser.beam().discard();
        }

        winner.unfreezeOwner();
        loser.unfreezeOwner();
        exhaust(loser);
    }

    private static void exhaust(ClashParticipant loser) {
        var owner = loser.owner();
        owner.removeEffect(MainEffects.STUN.get());
        owner.addEffect(new MobEffectInstance(MainEffects.STUN.get(), LOSER_EXHAUST_TICKS, 0, false, false, true));
        if (owner instanceof ServerPlayer player) {
            StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
                data.getStatus().setStunEffect(true);
                data.getStatus().setBlocking(false);
            });
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        }
    }

    public void dissolve() {
        if (ended) return;
        ended = true;
        a.beam().clearClashLock();
        b.beam().clearClashLock();
        a.unfreezeOwner();
        b.unfreezeOwner();
    }

    public float advantageFor(LivingEntity owner) {
        float visual = visualBias(biasT);
        if (owner.getUUID().equals(a.owner().getUUID())) return visual;
        return 1.0f - visual;
    }
}
