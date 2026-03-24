package com.dragonminez.common.init.entities.goals;

import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class SagasUseSkillGoal extends Goal {
    private final DBSagasEntity entity;

    public SagasUseSkillGoal(DBSagasEntity entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.entity.getTarget() == null || this.entity.isCasting() || this.entity.isComboing() || this.entity.isEvading()) {
            return false;
        }

        return this.entity.hasSkillReady();
    }

    @Override
    public boolean canContinueToUse() {
        return this.entity.isCasting();
    }

    @Override
    public void start() {
        this.entity.startFirstAvailableSkill();
    }
}