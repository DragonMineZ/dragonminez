package com.dragonminez.common.combat.clash;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class ClashParticipant {
    private static final int TIMING_SAMPLE_COUNT = 6;

    private static final float BOT_SPREAD_THRESHOLD = 0.12f;
    private static final float BOT_DAMPEN = 0.4f;

    private static final int STRIKE_LIMIT = 4;

    private static final float MARKER_MISMATCH = 0.06f;

    private static final float MAX_LAG_TICKS = 12.0f;
    private static final float LAG_GRACE_TICKS = 2.0f;

    private final AbstractKiProjectile beam;
    private final LivingEntity owner;
    @Getter
    private final boolean npc;
    private final boolean wasNoAi;
    private final double statPower;
    private final float npcAccuracy;
    private final long meterSeed;

    private final float[] recentPressOffsets = new float[TIMING_SAMPLE_COUNT];
    private int pressSampleCount;
    private int pressWriteIndex;

    private int consumedCycle = -1;
    private float lastPressTime = -Float.MAX_VALUE;
    private int strikes;
    private boolean dampened;

    private float momentum;
    private int idleTicks;

    public ClashParticipant(AbstractKiProjectile beam, LivingEntity owner) {
        this.beam = beam;
        this.owner = owner;
        this.npc = !(owner instanceof ServerPlayer);
        this.wasNoAi = owner instanceof Mob mob && mob.isNoAi();
        this.statPower = Math.max(1.0, beam.getKiDamage());
        this.npcAccuracy = resolveNpcAccuracy(this.statPower, this.npc);
        this.meterSeed = owner.getRandom().nextLong();
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

    public long meterSeed() {
        return meterSeed;
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

    public void tickMeter(int age) {
        this.momentum *= BeamClash.MOMENTUM_DECAY;
        this.idleTicks++;
        if (this.npc) tickNpcPress(age);
    }

    private void tickNpcPress(int age) {
        ClashMeter.Sample now = ClashMeter.sample(meterSeed, age);
        if (now.cycle().index() <= consumedCycle || now.grade() == ClashMeter.Grade.MISS) return;

        ClashMeter.Sample next = ClashMeter.sample(meterSeed, age + 1);
        boolean closest = next.cycle().index() != now.cycle().index() || next.distance() >= now.distance();
        if (!closest) return;
        consumedCycle = now.cycle().index();
        float jitter = 0.7f + owner.getRandom().nextFloat() * 0.3f;
        addBurst(now.efficiency() * npcAccuracy * jitter);
        this.idleTicks = 0;
    }

    public ClashMeter.Grade registerPlayerPress(float claimedTime, float claimedMarker, float serverTime, float realElapsed, int latencyMs) {
        float tolerance = Math.min(MAX_LAG_TICKS, Math.max(0, latencyMs) / 50.0f + LAG_GRACE_TICKS);
        float earliest = serverTime - tolerance;
        float latest = Math.max(serverTime, realElapsed) + 1.0f + LAG_GRACE_TICKS;

        float pressTime = claimedTime;
        if (Float.isNaN(pressTime) || Float.isInfinite(pressTime)) {
            strike("unreadable press time");
            pressTime = serverTime;
        } else if (pressTime < earliest || pressTime > latest) {
            strike("press stamped " + (pressTime - serverTime) + " ticks from server time, tolerance " + tolerance);
            pressTime = Mth.clamp(pressTime, earliest, latest);
        }
        if (pressTime <= lastPressTime) return null;
        lastPressTime = pressTime;
        this.idleTicks = 0;

        ClashMeter.Sample sample = ClashMeter.sample(meterSeed, pressTime);
        if (Float.isNaN(claimedMarker) || Math.abs(sample.marker() - claimedMarker) > MARKER_MISMATCH) {
            strike("marker " + claimedMarker + " does not match the simulated " + sample.marker());
        }
        if (sample.cycle().index() <= consumedCycle) return null;
        consumedCycle = sample.cycle().index();

        if (sample.grade() != ClashMeter.Grade.MISS) {
            float signedOffset = (sample.marker() - sample.cycle().center()) / sample.cycle().halfWidth();
            float efficiency = sample.efficiency() * botConsistencyPenalty(signedOffset);
            if (dampened) efficiency *= BOT_DAMPEN;
            addBurst(efficiency);
        }
        return sample.grade();
    }

    private void strike(String reason) {
        strikes++;
        if (strikes == STRIKE_LIMIT) {
            dampened = true;
            LogUtil.warn(Env.SERVER, "Beam clash: repeated implausible input from " + owner.getScoreboardName()
                    + " (" + reason + "); dampening their presses for the rest of this clash");
        }
    }

    private float botConsistencyPenalty(float signedOffset) {
        recentPressOffsets[pressWriteIndex] = signedOffset;
        pressWriteIndex = (pressWriteIndex + 1) % TIMING_SAMPLE_COUNT;
        if (pressSampleCount < TIMING_SAMPLE_COUNT) {
            pressSampleCount++;
            return 1.0f;
        }
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float p : recentPressOffsets) {
            if (Math.abs(p) > 1.0f) return 1.0f;
            min = Math.min(min, p);
            max = Math.max(max, p);
        }
        return (max - min) < BOT_SPREAD_THRESHOLD ? BOT_DAMPEN : 1.0f;
    }

    private void addBurst(float efficiency) {
        this.momentum += efficiency * BeamClash.BURST_PER_PERFECT_PRESS;
    }
}
