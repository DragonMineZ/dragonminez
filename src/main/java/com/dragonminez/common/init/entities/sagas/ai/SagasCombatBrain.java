package com.dragonminez.common.init.entities.sagas.ai;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.AiTier;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.KiSkill;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.LocomotionMode;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity.SkillRole;
import net.minecraft.util.RandomSource;

import java.util.List;

public final class SagasCombatBrain {

    public static final double MELEE_RANGE = 4.5D;
    public static final double MID_RANGE = 12.0D;
    public static final double OUT_RANGE = 28.0D;

    public static final int COMBO_RECOVERY = 7;
    private static final int[] STUN_COMBOS = {1, 3, 8};
    private static final int[] PRESSURE_COMBOS = {0, 8};
    private static final int[] HEAVY_COMBOS = {3, 1};

    public enum Type { MELEE, APPROACH, TELEPORT, CAST, COMBO, HOLD }

    public static final class Intent {
        public final Type type;
        public final KiSkill skill;
        public final int comboId;
        public final LocomotionMode locomotion;

        private Intent(Type type, KiSkill skill, int comboId, LocomotionMode locomotion) {
            this.type = type;
            this.skill = skill;
            this.comboId = comboId;
            this.locomotion = locomotion;
        }

        public static Intent melee() { return new Intent(Type.MELEE, null, -1, LocomotionMode.WALK); }
        public static Intent approach(LocomotionMode mode) { return new Intent(Type.APPROACH, null, -1, mode); }
        public static Intent teleport() { return new Intent(Type.TELEPORT, null, -1, LocomotionMode.RUN); }
        public static Intent hold() { return new Intent(Type.HOLD, null, -1, LocomotionMode.IDLE); }
        public static Intent cast(KiSkill skill) { return new Intent(Type.CAST, skill, -1, LocomotionMode.IDLE); }
        public static Intent combo(int comboId) { return new Intent(Type.COMBO, null, comboId, LocomotionMode.WALK); }
    }

    private SagasCombatBrain() {}

    public static Intent decide(CombatContext ctx) {
        DBSagasEntity self = ctx.self;
        RandomSource rnd = self.getRandom();
        boolean advanced = self.getAiTier() == AiTier.ADVANCED;
        double d = ctx.dist3D;

        if (ctx.selfHpPct < 0.25F && ctx.comboReady && !ctx.targetApproaching() && hasCombo(self, COMBO_RECOVERY)) {
            return Intent.combo(COMBO_RECOVERY);
        }

        if (advanced && ctx.targetCasting) {
            if (d <= MELEE_RANGE) {
                if (ctx.comboReady) return Intent.combo(chooseCombo(self, STUN_COMBOS, rnd));
                return Intent.melee();
            }
            return Intent.approach(LocomotionMode.WALK_SLOW);
        }

        if (advanced && (ctx.targetHelpless() || ctx.targetTransforming)) {
            if (d <= MELEE_RANGE && ctx.comboReady) return Intent.combo(chooseCombo(self, HEAVY_COMBOS, rnd));
            List<KiSkill> burst = ctx.readyByRole(SkillRole.HITSCAN, SkillRole.GUARD_BREAK);
            if (!burst.isEmpty()) return Intent.cast(pick(burst, rnd));
        }

        if (ctx.targetBlocking) {
            List<KiSkill> guardBreak = ctx.readyByRole(SkillRole.GUARD_BREAK);
            if (!guardBreak.isEmpty() && d <= OUT_RANGE) return Intent.cast(pick(guardBreak, rnd));
            if (d <= MELEE_RANGE && ctx.comboReady) return Intent.combo(chooseCombo(self, PRESSURE_COMBOS, rnd));
            return Intent.melee();
        }

        if (d > OUT_RANGE) {
            return reposition(ctx, rnd);
        }

        if (d > MID_RANGE) {
            if (ctx.targetApproaching()) {
                if (!ctx.hasReadyRole(SkillRole.HITSCAN)) {
                    List<KiSkill> ranged = ctx.readyByRole(SkillRole.RANGED_TRAVEL);
                    if (!ranged.isEmpty() && roll(rnd, 0.6F)) return Intent.cast(pick(ranged, rnd));
                }
                return Intent.approach(LocomotionMode.RUN);
            }
            List<KiSkill> ranged = ctx.readyByRole(SkillRole.RANGED_TRAVEL, SkillRole.ZONING);
            if (!ranged.isEmpty()) return Intent.cast(pick(ranged, rnd));
            return reposition(ctx, rnd);
        }

        if (d > MELEE_RANGE) {
            List<KiSkill> mid = ctx.readyByRole(SkillRole.HITSCAN, SkillRole.PROJECTILE_FAST);
            float castChance = advanced ? 0.55F : 0.65F;
            if (!mid.isEmpty() && roll(rnd, castChance)) return Intent.cast(pick(mid, rnd));
            return Intent.approach(LocomotionMode.RUN);
        }

        if (ctx.comboReady && roll(rnd, 0.6F)) return Intent.combo(chooseCombo(self, PRESSURE_COMBOS, rnd));
        return Intent.melee();
    }

    private static Intent reposition(CombatContext ctx, RandomSource rnd) {
        if (ctx.wildSenseReady && roll(rnd, 0.4F)) return Intent.teleport();
        if (ctx.dashReady && roll(rnd, 0.5F)) return Intent.approach(LocomotionMode.DASH);
        return Intent.approach(LocomotionMode.RUN);
    }

    private static boolean hasCombo(DBSagasEntity self, int comboId) {
        int[] allowed = self.getAllowedCombos();
        if (allowed == null) return false;
        for (int a : allowed) if (a == comboId) return true;
        return false;
    }

    private static int chooseCombo(DBSagasEntity self, int[] preferred, RandomSource rnd) {
        int[] allowed = self.getAllowedCombos();
        if (allowed != null && allowed.length > 0) {
            for (int pref : preferred) {
                for (int a : allowed) if (a == pref) return pref;
            }
            return allowed[rnd.nextInt(allowed.length)];
        }
        return -1;
    }

    private static KiSkill pick(List<KiSkill> options, RandomSource rnd) {
        return options.get(rnd.nextInt(options.size()));
    }

    private static boolean roll(RandomSource rnd, float chance) {
        return rnd.nextFloat() < chance;
    }
}
