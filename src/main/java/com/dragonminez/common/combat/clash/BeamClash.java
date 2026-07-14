package com.dragonminez.common.combat.clash;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class BeamClash {

    public static final float SWEEP_RATE = 0.010f;
    public static final float SWEET_LOW = 0.78f;
    public static final float SWEET_HIGH = 0.96f;
    public static final float OFF_SPOT_EFFICIENCY = 0.18f;
    public static final float BURST_PER_PERFECT_PRESS = 0.6f;
    public static final float MOMENTUM_DECAY = 0.96f;

    private static final float DRIFT_PER_TICK = 0.005f;
    private static final float STR_FLOOR = 0.6f;
    private static final float STR_SPAN = 0.95f;
    private static final float MIN_TRACTION = 0.35f;
    private static final float WIN_THRESHOLD = 0.8f;
    private static final int IDLE_DISSOLVE_TICKS = 100;
    private static final int MAX_DURATION = 600;
    private static final int WINNER_BREAKTHROUGH_TICKS = 60;

    public enum Result { ONGOING, A_WINS, B_WINS, DISSOLVED }

    private final ClashParticipant a;
    private final ClashParticipant b;
    private float biasT;
    private int age = 0;
    @Getter
    private boolean ended = false;

    public BeamClash(ClashParticipant a, ClashParticipant b) {
        this.a = a;
        this.b = b;
        this.biasT = 0.5f;
    }

    public ClashParticipant a() {
        return a;
    }

    public ClashParticipant b() {
        return b;
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

    /**
     * Advances the clash one tick: keeps the beams alive and locked at the clash point,
     */
    public Result tick() {
        if (ended) return Result.DISSOLVED;
        if (!a.isStillFiring() || !b.isStillFiring()) {
            return Result.DISSOLVED;
        }
        age++;

        // Keep both fighters rooted in place for the cinematic momento papu.
        a.freezeOwner();
        b.freezeOwner();

        a.tickMeter();
        b.tickMeter();

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

    private void applyLock() {
        Vec3 originA = a.origin();
        Vec3 originB = b.origin();
        double gap = originA.distanceTo(originB);

        float lockA = (float) (gap * biasT);
        float lockB = (float) (gap * (1.0f - biasT));

        a.beam().setClashLock(lockA, b.owner().getUUID());
        b.beam().setClashLock(lockB, a.owner().getUUID());

        keepAlive(a.beam());
        keepAlive(b.beam());
    }

    private static void keepAlive(AbstractKiProjectile beam) {
        // Push the death tick forward so a locked beam never auto-expires mid-clash.
        beam.setMaxLife(beam.tickCount + 40);
    }

    /**
     * Releases both beams. The loser's beam is discarded and the winner's beam is
     * unlocked and given fresh life to surge forward and connect (using its own normal
     * damage/explosion logic against the now-vulnerable loser).
     */
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

        // Hand AI control back to the NPCs.
        winner.unfreezeOwner();
        loser.unfreezeOwner();
    }

    /** Cleanly ends a clash with no winner (a beam stopped firing or an owner left). */
    public void dissolve() {
        if (ended) return;
        ended = true;
        a.beam().clearClashLock();
        b.beam().clearClashLock();
        a.unfreezeOwner();
        b.unfreezeOwner();
    }

	/** Advantage of the given owner in [0,1]; > 0.5 means winning. */
    public float advantageFor(LivingEntity owner) {
        if (owner.getUUID().equals(a.owner().getUUID())) return biasT;
        return 1.0f - biasT;
    }
}
