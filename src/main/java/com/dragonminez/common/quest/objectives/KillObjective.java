package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class KillObjective extends QuestObjective {
    private final String entityId;
    private final int count;

    public KillObjective(String description, EntityType<?> entityType, int count) {
        super(ObjectiveType.KILL, description, count);
        this.entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
        this.count = count;
    }

    public String getEntityId() {
        return entityId;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean checkProgress(Object... params) {
        if (params.length > 0 && params[0] instanceof Entity entity) {
            EntityType<?> requiredType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
            if (entity.getType().equals(requiredType)) {
                addProgress(1);
                return isCompleted();
            }
        }
        return false;
    }
}
