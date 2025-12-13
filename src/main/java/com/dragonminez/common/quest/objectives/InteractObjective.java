package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class InteractObjective extends QuestObjective {
    private final EntityType<?> entityType;
    private final String entityName;

    public InteractObjective(String description, EntityType<?> entityType, String entityName) {
        super(ObjectiveType.INTERACT, description, 1);
        this.entityType = entityType;
        this.entityName = entityName;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof Entity entity) {
            if (entityType == null || entity.getType().equals(entityType)) {
                if (entityName == null || entity.getName().getString().equals(entityName)) {
                    setProgress(1);
                    return true;
                }
            }
        }
        return false;
    }
}

