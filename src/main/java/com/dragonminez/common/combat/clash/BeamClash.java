package com.dragonminez.common.combat.clash;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Live state for a single beam clash between two firing beams. The clash point sits
 * on the segment between the two beam origins at fraction {@link #biasT} (0 = side A's
 * origin, 1 = side B's origin). Each tick the meters advance, momentum + ki stats are
 * blended into a net force, and the clash point drifts toward the weaker side. When it
 * reaches a side's origin (or the duration cap is hit) that side loses.
 */
public class BeamClash {

    // --- QTE meter tuning ---
    public static final float SWEEP_RATE = 0.0125f;            // phase units per tick (~4s per sweep at 20 tps)
    public static final float SWEET_LOW = 0.78f;               // sweet-spot window near the top of the sweep
    public static final float SWEET_HIGH = 0.96f;
    public static final float BURST_PER_PERFECT_PRESS = 1.0f;  // momentum from a centered press
    public static final float MOMENTUM_DECAY = 0.93f;          // momentum bleed decay per tick

    // --- tug-of-war tuning ---
    private static final float BASE_DRIFT = 0.20f;             // constant struggle so stats decide a stalemate
    private static final float DRIFT_K = 0.0012f;              // how fast biasT moves per unit net force
    private static final float EDGE = 0.12f;                   // biasT within EDGE of an origin = that side loses
    private static final int MAX_DURATION = 1000;              // hard cap (~50s) before stats decide
    private static final int WINNER_BREAKTHROUGH_TICKS = 60;   // life granted to the winning beam to surge through

    public enum Result { ONGOING, A_WINS, B_WINS, DISSOLVED }

    private final ClashParticipant a;
    private final ClashParticipant b;
    private float biasT = 0.5f;
    private int age = 0;
    @Getter
    private boolean ended = false;

    public BeamClash(ClashParticipant a, ClashParticipant b) {
        this.a = a;
        this.b = b;
        // Start slightly biased by relative stat power so a much stronger fighter opens ahead.
        float statFracA = (float) (a.statPower() / (a.statPower() + b.statPower()));
        this.biasT = Mth.clamp(0.5f + (statFracA - 0.5f) * 0.4f, EDGE + 0.05f, 1.0f - EDGE - 0.05f);
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

        // Net force: a constant base + recent struggle momentum, each scaled by a
        // normalized stat multiplier (1.0 = evenly matched, up to ~2.0 for dominance).
        float statFracA = (float) (a.statPower() / (a.statPower() + b.statPower()));
        float statMultA = statFracA * 2.0f;
        float statMultB = (1.0f - statFracA) * 2.0f;

        float forceA = (BASE_DRIFT + a.momentum()) * statMultA;
        float forceB = (BASE_DRIFT + b.momentum()) * statMultB;

        // Positive net force pushes the clash point toward B (biasT -> 1), i.e. A is winning.
        biasT += DRIFT_K * (forceA - forceB);
        biasT = Mth.clamp(biasT, 0.0f, 1.0f);

        // Keep both beams locked to meet exactly at the clash point and prevent expiry.
        applyLock();

        if (biasT >= 1.0f - EDGE) return Result.A_WINS;
        if (biasT <= EDGE) return Result.B_WINS;
        if (age >= MAX_DURATION) return biasT >= 0.5f ? Result.A_WINS : Result.B_WINS;
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
