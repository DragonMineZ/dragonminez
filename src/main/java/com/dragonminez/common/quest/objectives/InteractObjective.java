package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class InteractObjective extends QuestObjective {
    private final String entityTypeId;
    private final String entityName;

    public InteractObjective(String description, EntityType<?> entityType, String entityName) {
        super(ObjectiveType.INTERACT, description, 1);
        this.entityTypeId = entityType != null ? BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString() : null;
        this.entityName = entityName;
    }

    public String getEntityTypeId() {
        return entityTypeId;
    }

    public String getEntityName() {
        return entityName;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof Entity entity) {
            EntityType<?> requiredType = entityTypeId != null ? BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entityTypeId)) : null;
            if (requiredType == null || entity.getType().equals(requiredType)) {
                if (entityName == null || entity.getName().getString().equals(entityName)) {
                    setProgress(1);
                    return true;
                }
            }
        }
        return false;
    }
}
