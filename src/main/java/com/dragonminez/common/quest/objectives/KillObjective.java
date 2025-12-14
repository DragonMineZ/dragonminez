package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class KillObjective extends QuestObjective {
    private final EntityType<?> entityType;
    private final int count;

    public KillObjective(String description, EntityType<?> entityType, int count) {
        super(ObjectiveType.KILL, description, count);
        this.entityType = entityType;
        this.count = count;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof Entity entity) {
            if (entity.getType().equals(entityType)) {
                addProgress(1);
                return isCompleted();
            }
        }
        return false;
    }
}

