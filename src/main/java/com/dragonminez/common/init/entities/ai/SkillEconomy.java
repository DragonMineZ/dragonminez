package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.KiSkill;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.KiSkillType;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.SkillRole;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.Tier;

import java.util.ArrayDeque;
import java.util.function.Supplier;

public final class SkillEconomy {

    public enum Category { BEAM, HITSCAN, PROJECTILE, BURST, DEFENSIVE, BLIND, COMBO }

    private final Supplier<AiProfile> profile;
    private float heat;
    private final long[] categoryLockUntil = new long[Category.values().length];
    private long strongLockUntil;
    private int consecutiveCasts;
    private long forcedMeleeUntil;
    private final ArrayDeque<Long> mobilityUses = new ArrayDeque<>();
    private long lastTick = Long.MIN_VALUE;

    public SkillEconomy(Supplier<AiProfile> profile) {
        this.profile = profile;
    }

    public static Category categoryOf(SkillRole role) {
        return switch (role) {
            case RANGED_TRAVEL -> Category.BEAM;
            case HITSCAN -> Category.HITSCAN;
            case PROJECTILE_FAST, ZONING -> Category.PROJECTILE;
            case GUARD_BREAK, AOE_BURST -> Category.BURST;
            case DEFENSIVE -> Category.DEFENSIVE;
            case BLIND -> Category.BLIND;
        };
    }

    public static float heatCost(Tier tier) {
        return switch (tier) {
            case WEAK -> 1.0F;
            case MEDIUM -> 1.6F;
            case STRONG -> 2.6F;
        };
    }

    private static Tier tierOfSkill(KiSkill skill) {
        KiSkillType type = KiSkillType.fromId(skill.id);
        return type != null ? type.getTier() : Tier.MEDIUM;
    }

    public void tick(long now) {
        if (this.lastTick != Long.MIN_VALUE) {
            long elapsed = Math.max(0L, Math.min(40L, now - this.lastTick));
            this.heat = Math.max(0.0F, this.heat - this.profile.get().heatDecayPerSecond * (elapsed / 20.0F));
        }
        this.lastTick = now;
    }

    public float heat() {
        return this.heat;
    }

    public float heatFraction() {
        float max = this.profile.get().heatMax;
        return max <= 0.0F ? 1.0F : Math.min(1.0F, this.heat / max);
    }

    public boolean inForcedMelee(long now) {
        return now < this.forcedMeleeUntil;
    }

    public boolean categoryLocked(Category category, long now) {
        return now < this.categoryLockUntil[category.ordinal()];
    }

    public boolean strongLocked(long now) {
        return now < this.strongLockUntil;
    }

    public boolean canCast(KiSkill skill, long now) {
        return this.canCast(skill, now, false);
    }

    public boolean canCast(KiSkill skill, long now, boolean defensiveReaction) {
        AiProfile p = this.profile.get();
        Tier tier = tierOfSkill(skill);
        if (this.heat + heatCost(tier) > p.heatMax) return false;
        if (this.categoryLocked(categoryOf(skill.role), now)) return false;
        if (tier == Tier.STRONG && this.strongLocked(now)) return false;
        if (defensiveReaction) return true;
        if (this.inForcedMelee(now)) return false;
        return this.consecutiveCasts < p.maxConsecutiveCasts;
    }

    public void onCast(KiSkill skill, long now) {
        AiProfile p = this.profile.get();
        Tier tier = tierOfSkill(skill);
        this.heat += heatCost(tier);
        this.categoryLockUntil[categoryOf(skill.role).ordinal()] = now + p.categoryLockout;
        if (tier == Tier.STRONG) this.strongLockUntil = now + p.strongLockout;
        this.consecutiveCasts++;
        if (this.consecutiveCasts >= p.maxConsecutiveCasts) {
            this.forcedMeleeUntil = now + p.forcedMeleeWindow;
            this.consecutiveCasts = 0;
        }
    }

    public boolean canCombo(Tier tier, long now) {
        AiProfile p = this.profile.get();
        if (this.heat + heatCost(tier) * 0.7F > p.heatMax) return false;
        if (this.categoryLocked(Category.COMBO, now)) return false;
        return !(tier == Tier.STRONG && this.strongLocked(now));
    }

    public void onCombo(Tier tier, long now) {
        AiProfile p = this.profile.get();
        this.heat += heatCost(tier) * 0.7F;
        this.categoryLockUntil[Category.COMBO.ordinal()] = now + Math.round(p.categoryLockout * 0.6F);
        if (tier == Tier.STRONG) this.strongLockUntil = now + p.strongLockout;
        this.consecutiveCasts = 0;
    }

    public void onMeleeHit() {
        this.consecutiveCasts = 0;
        this.heat = Math.max(0.0F, this.heat - 0.25F);
    }

    public boolean canUseMobility(long now) {
        AiProfile p = this.profile.get();
        if (p.mobilityMaxUses <= 0) return false;
        this.pruneMobility(now);
        return this.mobilityUses.size() < p.mobilityMaxUses;
    }

    public void onMobilityUse(long now) {
        this.mobilityUses.addLast(now);
        this.pruneMobility(now);
    }

    private void pruneMobility(long now) {
        long window = this.profile.get().mobilityWindow;
        while (!this.mobilityUses.isEmpty() && now - this.mobilityUses.peekFirst() > window) {
            this.mobilityUses.pollFirst();
        }
    }

    public void refund(float heatAmount) {
        this.heat = Math.max(0.0F, this.heat - heatAmount);
    }

    public void inherit(SkillEconomy other) {
        this.heat = other.heat * 0.5F;
        System.arraycopy(other.categoryLockUntil, 0, this.categoryLockUntil, 0, this.categoryLockUntil.length);
        this.strongLockUntil = other.strongLockUntil;
        this.mobilityUses.addAll(other.mobilityUses);
    }

    public static Tier comboTier(int comboId) {
        return DBSagasEntity.ComboType.tierOf(comboId);
    }
}
