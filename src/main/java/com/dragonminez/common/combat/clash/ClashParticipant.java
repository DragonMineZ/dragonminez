package com.dragonminez.common.combat.clash;

import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class ClashParticipant {

    private final AbstractKiProjectile beam;
    private final LivingEntity owner;
    @Getter
    private final boolean npc;
    private final boolean wasNoAi;
    private final double statPower;

    private final float npcAccuracy;

    private static final int TIMING_SAMPLE_COUNT = 6;
    private static final float BOT_SPREAD_THRESHOLD = 0.02f;
    private static final float BOT_DAMPEN = 0.4f;
    private final float[] recentPressPhases = new float[TIMING_SAMPLE_COUNT];
    private int pressSampleCount;
    private int pressWriteIndex;

    private float meterPhase;
    private float prevMeterPhase;
    private float momentum;
    private int idleTicks;

    public ClashParticipant(AbstractKiProjectile beam, LivingEntity owner) {
        this.beam = beam;
        this.owner = owner;
        this.npc = !(owner instanceof ServerPlayer);
        this.wasNoAi = owner instanceof Mob mob && mob.isNoAi();
        this.statPower = Math.max(1.0, beam.getKiDamage());
        this.npcAccuracy = resolveNpcAccuracy(this.statPower, this.npc);
        this.meterPhase = owner.getRandom().nextFloat();
        this.prevMeterPhase = this.meterPhase;
    }

    private static float resolveNpcAccuracy(double attackPower, boolean npc) {
        if (!npc) return 0.0f;
        double t = Math.log10(attackPower + 1.0) / 3.0;
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

    public Vec3 origin() {
        return beam.position();
    }

    public Vec3 direction() {
        return Vec3.directionFromRotation(beam.getClashPitch(), beam.getClashYaw());
    }

    public boolean isStillFiring() {
        return owner.isAlive() && !beam.isRemoved() && beam.isClashableBeam();
    }

    public void freezeOwner() {
        if (owner instanceof Mob mob) {
            if (!mob.isNoAi()) mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setDeltaMovement(0, 0, 0);
            mob.hasImpulse = true;
        }
    }

    public void unfreezeOwner() {
        if (owner instanceof Mob mob) {
            mob.setNoAi(wasNoAi);
        }
    }

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

    private void tickNpcPress() {
        float center = (BeamClash.SWEET_LOW + BeamClash.SWEET_HIGH) * 0.5f;
        boolean crossedCenter = prevMeterPhase < center && meterPhase >= center && meterPhase >= prevMeterPhase;
        if (crossedCenter) {
            float jitter = 0.7f + owner.getRandom().nextFloat() * 0.3f;
            addBurst(npcAccuracy * jitter);
            this.idleTicks = 0;
        }
    }

    public void registerPlayerPress() {
        float phaseAtPress = this.meterPhase;
        float efficiency = scoreEfficiency(phaseAtPress) * botConsistencyPenalty(phaseAtPress);
        addBurst(efficiency);
        this.idleTicks = 0;
        this.meterPhase = 0.0f;
        this.prevMeterPhase = 0.0f;
    }

    private float botConsistencyPenalty(float phaseAtPress) {
        recentPressPhases[pressWriteIndex] = phaseAtPress;
        pressWriteIndex = (pressWriteIndex + 1) % TIMING_SAMPLE_COUNT;
        if (pressSampleCount < TIMING_SAMPLE_COUNT) {
            pressSampleCount++;
            return 1.0f;
        }
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float p : recentPressPhases) {
            if (p < BeamClash.SWEET_LOW || p > BeamClash.SWEET_HIGH) return 1.0f;
            min = Math.min(min, p);
            max = Math.max(max, p);
        }
        return (max - min) < BOT_SPREAD_THRESHOLD ? BOT_DAMPEN : 1.0f;
    }

    public static float scoreEfficiency(float phase) {
        if (phase < BeamClash.SWEET_LOW || phase > BeamClash.SWEET_HIGH) return BeamClash.OFF_SPOT_EFFICIENCY;
        float center = (BeamClash.SWEET_LOW + BeamClash.SWEET_HIGH) * 0.5f;
        float half = (BeamClash.SWEET_HIGH - BeamClash.SWEET_LOW) * 0.5f;
        float closeness = 1.0f - Math.abs(phase - center) / half;
        return BeamClash.OFF_SPOT_EFFICIENCY + (1.0f - BeamClash.OFF_SPOT_EFFICIENCY) * Math.max(0.0f, closeness);
    }

    private void addBurst(float efficiency) {
        this.momentum += efficiency * BeamClash.BURST_PER_PERFECT_PRESS;
    }
}
