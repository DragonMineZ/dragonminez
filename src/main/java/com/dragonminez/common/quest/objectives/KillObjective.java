package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

@Getter
public class KillObjective extends QuestObjective {
	public enum SpawnMode {
		QUEST,
		NATURAL
	}

	public enum CountMode {
		QUEST_SPAWNED_ONLY,
		ANY_MATCHING
	}

	private final String entityId;
	private final int count;
	private final double health;
	private final double meleeDamage;
	private final double kiDamage;
	private final SpawnMode spawnMode;
	private final CountMode countMode;

	public KillObjective(EntityType<?> entityType, int count, double health, double meleeDamage, double kiDamage) {
		this(entityType, count, health, meleeDamage, kiDamage, SpawnMode.QUEST, CountMode.QUEST_SPAWNED_ONLY);
	}

	public KillObjective(EntityType<?> entityType, int count, double health, double meleeDamage, double kiDamage,
						 SpawnMode spawnMode, CountMode countMode) {
		super(ObjectiveType.KILL, count);
		this.entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString();
		this.count = count;
		this.health = health;
		this.meleeDamage = meleeDamage;
		this.kiDamage = kiDamage;
		this.spawnMode = spawnMode != null ? spawnMode : SpawnMode.QUEST;
		this.countMode = countMode != null ? countMode : CountMode.QUEST_SPAWNED_ONLY;
	}

	@Override
	public boolean checkProgress(Object... params) {
		if (params.length > 0 && params[0] instanceof Entity entity) {
			EntityType<?> requiredType = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityId));
			if (entity.getType().equals(requiredType)) {
				addProgress(1);
				return isCompleted();
			}
		}
		return false;
	}
}
