package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Escort a spawned NPC to a destination. The escort follows the quest controller; it reaching
 * the target completes the objective, its death fails the quest. Runtime state lives in
 * QuestFieldSessions. JSON: {@code {"type": "ESCORT", "entity": "minecraft:villager",
 * "x": 120, "y": 70, "z": -40, "radius": 6, "health": 40}}
 */
@Getter
public class EscortObjective extends QuestObjective {

	private final String entityId;
	private final BlockPos targetPos;
	private final int radius;
	private final double escortHealth;

	public EscortObjective(String entityId, BlockPos targetPos, int radius, double escortHealth) {
		super(ObjectiveType.ESCORT, 1);
		this.entityId = entityId;
		this.targetPos = targetPos;
		this.radius = Math.max(1, radius);
		this.escortHealth = escortHealth;
	}

	@Nullable
	public EntityType<?> resolveEntityType() {
		try {
			return ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityId));
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean checkProgress(Object... params) {
		return isCompleted();
	}
}
