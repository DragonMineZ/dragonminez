package com.dragonminez.common.combat.clash;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * One side of a beam clash: a firing beam, its owner, and that owner's live
 * struggle state (auto-filling QTE meter + accumulated momentum). Players drive
 * the meter with real key presses; NPCs drive it with a virtual presser.
 *
 * <p>Clash power is the firing attack's own damage ({@link AbstractKiProjectile#getKiDamage()}),
 * i.e. the technique's strength — not the owner's raw stats — so the stronger technique wins.
 */
public class ClashParticipant {

    private final AbstractKiProjectile beam;
    private final LivingEntity owner;
    @Getter
    private final boolean npc;
    private final boolean wasNoAi; // NPC AI state before the clash, restored on release
    private final double statPower;

    /** NPC timing accuracy in [0,1], derived from battle power. Unused for players. */
    private final float npcAccuracy;

    // --- live QTE state ---
    private float meterPhase;     // sweeps 0 -> 1 repeatedly
    private float prevMeterPhase;
    private float momentum;       // recent struggle intensity, decays each tick
    private int idleTicks;        // ticks since this side last pressed

    public ClashParticipant(AbstractKiProjectile beam, LivingEntity owner) {
        this.beam = beam;
        this.owner = owner;
        this.npc = !(owner instanceof ServerPlayer);
        this.wasNoAi = owner instanceof Mob mob && mob.isNoAi();
        this.statPower = Math.max(1.0, beam.getKiDamage());
        this.npcAccuracy = resolveNpcAccuracy(this.statPower, this.npc);
        // Stagger the two meters so both players don't pulse in perfect lockstep.
        this.meterPhase = owner.getRandom().nextFloat();
        this.prevMeterPhase = this.meterPhase;
    }

    private static float resolveNpcAccuracy(double attackPower, boolean npc) {
        if (!npc) return 0.0f;
        // Stronger attacks come from stronger fighters, who time their struggle better.
        double t = Math.log10(attackPower + 1.0) / 3.0; // ~0..1 across damage 1..1000
        return (float) Math.min(0.92, 0.45 + t * 0.45);
    }

    public AbstractKiProjectile beam() {
        return beam;
    }

    public LivingEntity owner() {
        return owner;
    }

	public double statPower() {
        return statPower;
    }

    public float meterPhase() {
        return meterPhase;
    }

    public float momentum() {
        return momentum;
    }

    public int idleTicks() {
        return idleTicks;
    }

    /** Origin of the beam (fixed while firing, since the owner is movement-locked). */
    public Vec3 origin() {
        return beam.position();
    }

    /** Normalized firing direction of the beam. */
    public Vec3 direction() {
        return Vec3.directionFromRotation(beam.getClashPitch(), beam.getClashYaw());
    }

    public boolean isStillFiring() {
        return owner.isAlive() && !beam.isRemoved() && beam.isClashableBeam();
    }

    /**
     * Freezes an NPC owner in place for the cinematic clash: disables its AI (so it can't
     * path/charge/teleport toward the opponent) and zeroes any residual motion. Players are
     * already movement-locked client-side, so this is a no-op for them.
     */
    public void freezeOwner() {
        if (owner instanceof Mob mob) {
            if (!mob.isNoAi()) mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setDeltaMovement(0, 0, 0);
            mob.hasImpulse = true;
        }
    }

    /** Restores the NPC owner's AI to its pre-clash state. */
    public void unfreezeOwner() {
        if (owner instanceof Mob mob) {
            mob.setNoAi(wasNoAi);
        }
    }

    /**
     * Advances the auto-filling meter one tick and decays momentum. For NPCs, also
     * runs the virtual presser. Returns true when the meter wrapped this tick.
     */
    public boolean tickMeter() {
        this.prevMeterPhase = this.meterPhase;
        this.meterPhase += BeamClash.SWEEP_RATE;
        this.momentum *= BeamClash.MOMENTUM_DECAY;
        this.idleTicks++;

        boolean wrapped = false;
        if (this.meterPhase >= 1.0f) {
            this.meterPhase -= 1.0f;
            wrapped = true;
        }

        if (this.npc) {
            tickNpcPress();
        }
        return wrapped;
    }

    /** NPC presses once per sweep, right as the marker crosses the sweet-spot center. */
    private void tickNpcPress() {
        float center = (BeamClash.SWEET_LOW + BeamClash.SWEET_HIGH) * 0.5f;
        boolean crossedCenter = prevMeterPhase < center && meterPhase >= center && meterPhase >= prevMeterPhase;
        if (crossedCenter) {
            float jitter = 0.7f + owner.getRandom().nextFloat() * 0.3f;
            addBurst(npcAccuracy * jitter);
            this.idleTicks = 0;
        }
    }

    /**
     * Scores a player's key press against the current meter position and feeds the
     * result into momentum. One scoring opportunity per sweep: any press consumes the
     * sweep by resetting the meter, so mistimed spam is self-punishing.
     */
    public void registerPlayerPress() {
        float efficiency = scoreEfficiency(this.meterPhase);
        addBurst(efficiency);
        this.idleTicks = 0;
        // Consume the sweep regardless of accuracy.
        this.meterPhase = 0.0f;
        this.prevMeterPhase = 0.0f;
    }

    /** 1.0 at the center of the sweet-spot, tapering to 0 at its edges, 0 outside. */
    public static float scoreEfficiency(float phase) {
        if (phase < BeamClash.SWEET_LOW || phase > BeamClash.SWEET_HIGH) return 0.0f;
        float center = (BeamClash.SWEET_LOW + BeamClash.SWEET_HIGH) * 0.5f;
        float half = (BeamClash.SWEET_HIGH - BeamClash.SWEET_LOW) * 0.5f;
        float closeness = 1.0f - Math.abs(phase - center) / half;
        return Math.max(0.0f, closeness);
    }

    private void addBurst(float efficiency) {
        this.momentum += efficiency * BeamClash.BURST_PER_PERFECT_PRESS;
    }
}
