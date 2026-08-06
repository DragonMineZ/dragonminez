package com.dragonminez.common.util;

import com.dragonminez.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/** 1.21 AttributeModifier helpers (UUID name pair replaced by ResourceLocation id). */
public final class AttributeMods {
	private AttributeMods() {}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
	}

	/** Stable id derived from legacy UUID constants so existing logic maps cleanly. */
	public static ResourceLocation id(UUID uuid) {
		return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "uuid_" + uuid.toString().replace("-", ""));
	}

	public static AttributeModifier of(String path, double amount, AttributeModifier.Operation op) {
		return new AttributeModifier(id(path), amount, op);
	}

	public static AttributeModifier of(UUID uuid, String legacyName, double amount, AttributeModifier.Operation op) {
		return new AttributeModifier(id(uuid), amount, op);
	}
}
