package com.dragonminez.common.quest.objectives;

import com.dragonminez.common.quest.QuestObjective;
import lombok.Getter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

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
	private final int textureVariant;
	private final int aiTier;
	private final boolean canTransform;

	public KillObjective(String entityId, int count, double health, double meleeDamage, double kiDamage,
						 SpawnMode spawnMode, CountMode countMode, int textureVariant, int aiTier, boolean canTransform) {
		super(ObjectiveType.KILL, count);
		this.entityId = entityId;
		this.count = count;
		this.health = health;
		this.meleeDamage = meleeDamage;
		this.kiDamage = kiDamage;
		this.spawnMode = spawnMode != null ? spawnMode : SpawnMode.QUEST;
		this.countMode = countMode != null ? countMode : CountMode.QUEST_SPAWNED_ONLY;
		this.textureVariant = textureVariant;
		this.aiTier = aiTier;
		this.canTransform = canTransform;
	}

	@Override
	public boolean checkProgress(Object... params) {
		if (params.length > 0 && params[0] instanceof Entity entity && matches(entity.getType())) {
			addProgress(1);
			return isCompleted();
		}
		return false;
	}

	public boolean isTag() {
		return entityId != null && entityId.startsWith("#");
	}

	public boolean matches(EntityType<?> type) {
		if (type == null || entityId == null) {
			return false;
		}
		try {
			if (isTag()) {
				TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(entityId.substring(1)));
				return type.builtInRegistryHolder().is(tag);
			}
			return type.equals(ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityId)));
		} catch (Exception e) {
			return false;
		}
	}

	@Nullable
	public EntityType<?> resolveEntityType() {
		try {
			if (isTag()) {
				TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(entityId.substring(1)));
				var tags = ForgeRegistries.ENTITY_TYPES.tags();
				if (tags != null) {
					for (EntityType<?> type : tags.getTag(tag)) {
						return type;
					}
				}
				return null;
			}
			return ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(entityId));
		} catch (Exception e) {
			return null;
		}
	}
}
