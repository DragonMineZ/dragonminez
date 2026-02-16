package com.dragonminez.common.init.entities.goals;

import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

public class NamekDefendVillageGoal extends TargetGoal {
    private final NamekWarriorEntity warrior;
    private LivingEntity villageAggressor;
    private final TargetingConditions attackStrategy = TargetingConditions.forCombat().range(64.0D);

    public NamekDefendVillageGoal(NamekWarriorEntity pWarrior) {
        super(pWarrior, false, true);
        this.warrior = pWarrior;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        AABB aabb = this.warrior.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        List<Villager> list = this.warrior.level().getEntitiesOfClass(Villager.class, aabb);

        for(Villager villager : list) {
            LivingEntity lastHurtBy = villager.getLastHurtByMob();
            if (lastHurtBy != null && this.warrior.canAttack(lastHurtBy, this.attackStrategy)) {
                this.villageAggressor = lastHurtBy;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        this.warrior.setTarget(this.villageAggressor);
        super.start();
    }
}