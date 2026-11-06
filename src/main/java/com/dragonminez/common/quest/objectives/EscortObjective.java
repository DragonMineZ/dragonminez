package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import javax.annotation.Nullable;

/**
 * Escort a spawned NPC to a destination. The escort follows the quest controller; it reaching
 * the target completes the objective, its death fails the quest. Runtime state lives in
 * QuestFieldSessions. JSON: {@code {"type": "ESCORT", "entity": "minecraft:villager",
 * "x": 120, "y": 70, "z": -40, "radius": 6, "health": 40}}
 */
public class EscortObjective extends QuestObjective {
	private final String entityId;
	private final BlockPos targetPos;
	private final int radius;
	private final double escortHealth;
	@Nullable
	private final String dimension;

	public EscortObjective(String entityId, BlockPos targetPos, int radius, double escortHealth) {
		this(entityId, targetPos, radius, escortHealth, null);
	}

	public EscortObjective(String entityId, BlockPos targetPos, int radius, double escortHealth, @Nullable String dimension) {
		super(ObjectiveType.ESCORT, 1);
		this.entityId = entityId;
		this.targetPos = targetPos;
		this.radius = Math.max(1, radius);
		this.escortHealth = escortHealth;
		this.dimension = dimension == null || dimension.isBlank() ? null : dimension.trim();
	}

	@Nullable
	public EntityType<?> resolveEntityType() {
		try {
			return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(entityId)).orElse(null);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean checkProgress(Object... params) {
		return isCompleted();
	}

	public String getEntityId() {
		return this.entityId;
	}

	public BlockPos getTargetPos() {
		return this.targetPos;
	}

	public int getRadius() {
		return this.radius;
	}

	public double getEscortHealth() {
		return this.escortHealth;
	}

	@Nullable
	public String getDimension() {
		return this.dimension;
	}
}
