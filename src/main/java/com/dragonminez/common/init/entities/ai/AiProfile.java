package com.dragonminez.common.init.entities.ai;

public final class AiProfile {

    public enum SkillPick { HIGHEST_COOLDOWN, ROLE_FIT, TACTICAL }

    public final AiTier tier;

    public final int decisionInterval;
    public final int reactionDelay;
    public final int threatScanInterval;

    public final boolean zanzoken;
    public final boolean dash;
    public final boolean proactiveTeleport;
    public final boolean strafe;
    public final boolean kite;
    public final boolean flank;
    public final boolean memory;
    public final boolean leadAim;
    public final boolean defensiveBarrier;
    public final boolean punishCommittedCast;
    public final boolean readsGuard;
    public final boolean prePositionsForCharge;

    public final double dodgeMinOriginDistance;
    public final float dodgeMinCharge;
    public final float dodgeChance;
    public final float dodgeMinDamageFraction;
    public final float panicDamageFraction;
    public final boolean saveMobility;

    public final float castCommitChance;
    public final int postCastLockout;
    public final int globalActionLockout;
    public final int categoryLockout;
    public final int strongLockout;

    public final float heatMax;
    public final float heatDecayPerSecond;
    public final int maxConsecutiveCasts;
    public final int forcedMeleeWindow;

    public final SkillPick skillPick;
    public final float decisionNoise;

    public final int mobilityWindow;
    public final int mobilityMaxUses;
    public final float zanzokenCooldownMultiplier;

    public final float aimLeadFactor;
    public final float aimErrorDegrees;
    public final int targetRetention;
    public final int backstepCooldown;

    private AiProfile(Builder b) {
        this.tier = b.tier;
        this.decisionInterval = b.decisionInterval;
        this.reactionDelay = b.reactionDelay;
        this.threatScanInterval = b.threatScanInterval;
        this.zanzoken = b.zanzoken;
        this.dash = b.dash;
        this.proactiveTeleport = b.proactiveTeleport;
        this.strafe = b.strafe;
        this.kite = b.kite;
        this.flank = b.flank;
        this.memory = b.memory;
        this.leadAim = b.leadAim;
        this.defensiveBarrier = b.defensiveBarrier;
        this.punishCommittedCast = b.punishCommittedCast;
        this.readsGuard = b.readsGuard;
        this.prePositionsForCharge = b.prePositionsForCharge;
        this.dodgeMinOriginDistance = b.dodgeMinOriginDistance;
        this.dodgeMinCharge = b.dodgeMinCharge;
        this.dodgeChance = b.dodgeChance;
        this.dodgeMinDamageFraction = b.dodgeMinDamageFraction;
        this.panicDamageFraction = b.panicDamageFraction;
        this.saveMobility = b.saveMobility;
        this.castCommitChance = b.castCommitChance;
        this.postCastLockout = b.postCastLockout;
        this.globalActionLockout = b.globalActionLockout;
        this.categoryLockout = b.categoryLockout;
        this.strongLockout = b.strongLockout;
        this.heatMax = b.heatMax;
        this.heatDecayPerSecond = b.heatDecayPerSecond;
        this.maxConsecutiveCasts = b.maxConsecutiveCasts;
        this.forcedMeleeWindow = b.forcedMeleeWindow;
        this.skillPick = b.skillPick;
        this.decisionNoise = b.decisionNoise;
        this.mobilityWindow = b.mobilityWindow;
        this.mobilityMaxUses = b.mobilityMaxUses;
        this.zanzokenCooldownMultiplier = b.zanzokenCooldownMultiplier;
        this.aimLeadFactor = b.aimLeadFactor;
        this.aimErrorDegrees = b.aimErrorDegrees;
        this.targetRetention = b.targetRetention;
        this.backstepCooldown = b.backstepCooldown;
    }

    private static final AiProfile NOVICE = new Builder(AiTier.NOVICE)
            .timing(10, 12, 4)
            .abilities(false, false, false, false, false, false, false, false, false, false, false, false)
            .dodging(15.0D, 1.0F, 0.6F, 0.0F, 1.0F, false)
            .economy(0.55F, 120, 60, 170, 420)
            .heat(4.0F, 0.25F, 1, 100)
            .picking(SkillPick.HIGHEST_COOLDOWN, 0.35F)
            .mobility(600, 0, 0.0F)
            .aim(0.0F, 8.0F, 220, 160)
            .build();

    private static final AiProfile SMART = new Builder(AiTier.SMART)
            .timing(6, 7, 2)
            .abilities(true, true, true, true, false, false, false, false, false, false, true, false)
            .dodging(0.0D, 0.0F, 0.75F, 0.04F, 0.30F, false)
            .economy(0.6F, 90, 50, 140, 320)
            .heat(5.0F, 0.35F, 2, 70)
            .picking(SkillPick.ROLE_FIT, 0.2F)
            .mobility(600, 1, 1.5F)
            .aim(0.0F, 4.0F, 120, 120)
            .build();

    private static final AiProfile ELITE = new Builder(AiTier.ELITE)
            .timing(4, 4, 1)
            .abilities(true, true, true, true, true, true, true, true, true, true, true, true)
            .dodging(0.0D, 0.0F, 0.9F, 0.05F, 0.25F, true)
            .economy(0.75F, 70, 40, 120, 260)
            .heat(7.0F, 0.5F, 3, 50)
            .picking(SkillPick.TACTICAL, 0.1F)
            .mobility(600, 2, 1.0F)
            .aim(0.8F, 1.5F, 80, 100)
            .build();

    private static final AiProfile EXPERT = new Builder(AiTier.EXPERT)
            .timing(3, 2, 1)
            .abilities(true, true, true, true, true, true, true, true, true, true, true, true)
            .dodging(0.0D, 0.0F, 0.97F, 0.04F, 0.20F, true)
            .economy(0.9F, 50, 30, 100, 200)
            .heat(9.0F, 0.65F, 3, 40)
            .picking(SkillPick.TACTICAL, 0.05F)
            .mobility(600, 3, 0.8F)
            .aim(1.0F, 0.0F, 60, 80)
            .build();

    public static AiProfile of(AiTier tier) {
        return switch (tier) {
            case NOVICE -> NOVICE;
            case SMART -> SMART;
            case ELITE -> ELITE;
            case EXPERT -> EXPERT;
        };
    }

    private static final class Builder {
        private final AiTier tier;
        private int decisionInterval, reactionDelay, threatScanInterval;
        private boolean zanzoken, dash, proactiveTeleport, strafe, kite, flank, memory, leadAim, defensiveBarrier,
                punishCommittedCast, readsGuard, prePositionsForCharge;
        private double dodgeMinOriginDistance;
        private float dodgeMinCharge, dodgeChance, dodgeMinDamageFraction, panicDamageFraction;
        private boolean saveMobility;
        private float castCommitChance;
        private int postCastLockout, globalActionLockout, categoryLockout, strongLockout;
        private float heatMax, heatDecayPerSecond;
        private int maxConsecutiveCasts, forcedMeleeWindow;
        private SkillPick skillPick;
        private float decisionNoise;
        private int mobilityWindow, mobilityMaxUses;
        private float zanzokenCooldownMultiplier;
        private float aimLeadFactor, aimErrorDegrees;
        private int targetRetention, backstepCooldown;

        private Builder(AiTier tier) {
            this.tier = tier;
        }

        private Builder timing(int decisionInterval, int reactionDelay, int threatScanInterval) {
            this.decisionInterval = decisionInterval;
            this.reactionDelay = reactionDelay;
            this.threatScanInterval = threatScanInterval;
            return this;
        }

        private Builder abilities(boolean zanzoken, boolean dash, boolean proactiveTeleport, boolean strafe, boolean kite,
                                  boolean flank, boolean memory, boolean leadAim, boolean defensiveBarrier,
                                  boolean punishCommittedCast, boolean readsGuard, boolean prePositionsForCharge) {
            this.zanzoken = zanzoken;
            this.dash = dash;
            this.proactiveTeleport = proactiveTeleport;
            this.strafe = strafe;
            this.kite = kite;
            this.flank = flank;
            this.memory = memory;
            this.leadAim = leadAim;
            this.defensiveBarrier = defensiveBarrier;
            this.punishCommittedCast = punishCommittedCast;
            this.readsGuard = readsGuard;
            this.prePositionsForCharge = prePositionsForCharge;
            return this;
        }

        private Builder dodging(double minOriginDistance, float minCharge, float chance, float minDamageFraction,
                                float panicFraction, boolean saveMobility) {
            this.dodgeMinOriginDistance = minOriginDistance;
            this.dodgeMinCharge = minCharge;
            this.dodgeChance = chance;
            this.dodgeMinDamageFraction = minDamageFraction;
            this.panicDamageFraction = panicFraction;
            this.saveMobility = saveMobility;
            return this;
        }

        private Builder economy(float castCommitChance, int postCastLockout, int globalActionLockout,
                                int categoryLockout, int strongLockout) {
            this.castCommitChance = castCommitChance;
            this.postCastLockout = postCastLockout;
            this.globalActionLockout = globalActionLockout;
            this.categoryLockout = categoryLockout;
            this.strongLockout = strongLockout;
            return this;
        }

        private Builder heat(float heatMax, float heatDecayPerSecond, int maxConsecutiveCasts, int forcedMeleeWindow) {
            this.heatMax = heatMax;
            this.heatDecayPerSecond = heatDecayPerSecond;
            this.maxConsecutiveCasts = maxConsecutiveCasts;
            this.forcedMeleeWindow = forcedMeleeWindow;
            return this;
        }

        private Builder picking(SkillPick skillPick, float decisionNoise) {
            this.skillPick = skillPick;
            this.decisionNoise = decisionNoise;
            return this;
        }

        private Builder mobility(int window, int maxUses, float zanzokenCooldownMultiplier) {
            this.mobilityWindow = window;
            this.mobilityMaxUses = maxUses;
            this.zanzokenCooldownMultiplier = zanzokenCooldownMultiplier;
            return this;
        }

        private Builder aim(float leadFactor, float errorDegrees, int targetRetention, int backstepCooldown) {
            this.aimLeadFactor = leadFactor;
            this.aimErrorDegrees = errorDegrees;
            this.targetRetention = targetRetention;
            this.backstepCooldown = backstepCooldown;
            return this;
        }

        private AiProfile build() {
            return new AiProfile(this);
        }
    }
}
