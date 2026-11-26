package com.dragonminez.common.init.entities.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PartyAwareTargetGoal extends TargetGoal {

    private final Supplier<AiProfile> profile;
    private final Predicate<LivingEntity> allowed;
    private final TargetingConditions conditions;
    private final int interval;
    private LivingEntity chosen;

    public PartyAwareTargetGoal(Mob mob, boolean mustSee) {
        this(mob, mustSee, AiTier.NOVICE::profile, null);
    }

    public PartyAwareTargetGoal(Mob mob, boolean mustSee, Supplier<AiProfile> profile, Predicate<LivingEntity> allowed) {
        super(mob, mustSee, false);
        this.profile = profile;
        this.allowed = allowed;
        this.interval = 10;
        this.conditions = TargetingConditions.forCombat().range(this.getFollowDistance());
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextInt(reducedTickDelay(this.interval)) != 0) return false;
        double range = this.getFollowDistance();
        this.chosen = TargetSelector.choose(this.mob, this.profile.get(), null, this.mob.level().getGameTime(), range,
                candidate -> (this.allowed == null || this.allowed.test(candidate))
                        && this.mob.canAttack(candidate, this.conditions.range(range)));
        return this.chosen != null && this.chosen != this.mob.getTarget();
    }

    @Override
    public void start() {
        this.mob.setTarget(this.chosen);
        super.start();
    }
}
