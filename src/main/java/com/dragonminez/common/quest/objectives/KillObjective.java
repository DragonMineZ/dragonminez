package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class KillObjective extends QuestObjective {
	private final String entityId;
	private final int count;
	private final double health;
	private final double meleeDamage;
	private final double kiDamage;

	public KillObjective(String description, EntityType<?> entityType, int count, double health, double meleeDamage, double kiDamage) {
		super(ObjectiveType.KILL, description, count);
		this.entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
		this.count = count;
		this.health = health;
		this.meleeDamage = meleeDamage;
		this.kiDamage = kiDamage;
	}

	public String getEntityId() {
		return entityId;
	}

	public int getCount() {
		return count;
	}

	public double getHealth() {
		return health;
	}

	public double getMeleeDamage() {
		return meleeDamage;
	}

	public double getKiDamage() {
		return kiDamage;
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