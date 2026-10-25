package com.dragonminez.server.world.raid;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

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
			return location != null && location.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type));
		}

		ResourceLocation tagLocation = ResourceLocation.tryParse(entityId.substring(1));
		if (tagLocation == null) return false;

		return BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, tagLocation))
				.map(tag -> tag.stream().anyMatch(holder -> holder.value() == type))
				.orElse(false);
	}

	private static EntityType<?> resolveDirect(String entityId) {
		ResourceLocation location = ResourceLocation.tryParse(entityId);
		if (location == null) {
			LogUtil.warn(Env.SERVER, "Raid wave has a malformed entity id '{}'; skipping it", entityId);
			return null;
		}

		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(location);
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

		var tag = BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, location));
		if (tag.isEmpty()) return null;

		List<EntityType<?>> members = new ArrayList<>();
		for (Holder<EntityType<?>> holder : tag.get()) {
			members.add(holder.value());
		}

		if (members.isEmpty()) {
			LogUtil.warn(Env.SERVER, "Raid wave points at entity tag '#{}', which is empty; skipping it", tagId);
			return null;
		}
		return members.get(random.nextInt(members.size()));
	}
}
