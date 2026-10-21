package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraftforge.registries.tags.ITagManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class RaidEntityResolver {

	private RaidEntityResolver() {}

	public static EntityType<?> resolve(String entityId, RandomSource random) {
		if (entityId == null || entityId.isBlank()) return null;

		return entityId.startsWith("#")
				? resolveFromTag(entityId.substring(1), random)
				: resolveDirect(entityId);
	}

	public static boolean matches(EntityType<?> type, String entityId) {
		if (type == null || entityId == null || entityId.isBlank()) return false;

		if (!entityId.startsWith("#")) {
			ResourceLocation location = ResourceLocation.tryParse(entityId);
			return location != null && location.equals(ForgeRegistries.ENTITY_TYPES.getKey(type));
		}

		ResourceLocation tagLocation = ResourceLocation.tryParse(entityId.substring(1));
		if (tagLocation == null) return false;

		ITagManager<EntityType<?>> tags = ForgeRegistries.ENTITY_TYPES.tags();
		return tags != null && tags.getTag(TagKey.create(Registries.ENTITY_TYPE, tagLocation)).contains(type);
	}

	private static EntityType<?> resolveDirect(String entityId) {
		ResourceLocation location = ResourceLocation.tryParse(entityId);
		if (location == null) {
			LogUtil.warn(Env.SERVER, "Raid wave has a malformed entity id '{}'; skipping it", entityId);
			return null;
		}

		EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(location);
		if (type == null) {
			LogUtil.warn(Env.SERVER, "Raid wave points at unknown entity '{}'; skipping it", entityId);
		}
		return type;
	}

	private static EntityType<?> resolveFromTag(String tagId, RandomSource random) {
		ResourceLocation location = ResourceLocation.tryParse(tagId);
		if (location == null) {
			LogUtil.warn(Env.SERVER, "Raid wave has a malformed entity tag '#{}'; skipping it", tagId);
			return null;
		}

		ITagManager<EntityType<?>> tags = ForgeRegistries.ENTITY_TYPES.tags();
		if (tags == null) return null;

		List<EntityType<?>> members = new ArrayList<>();
		for (EntityType<?> type : tags.getTag(TagKey.create(Registries.ENTITY_TYPE, location))) {
			members.add(type);
		}

		if (members.isEmpty()) {
			LogUtil.warn(Env.SERVER, "Raid wave points at entity tag '#{}', which is empty; skipping it", tagId);
			return null;
		}
		return members.get(random.nextInt(members.size()));
	}
}
