package com.dragonminez.server.world.dimension;

import com.dragonminez.Reference;
import com.dragonminez.server.world.gen.DemonRealmGeneration;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalLong;

public class DemonRealmDimension {
	public static final ResourceKey<Level> DEMON_REALM_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "demon_realm"));
	public static final ResourceKey<DimensionType> DEMON_REALM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "demon_realm"));

	public static void bootstrap(BootstapContext<DimensionType> context) {
		int height = DemonRealmGeneration.WORLD_TOP - DemonRealmGeneration.WORLD_BOTTOM;
		context.register(DEMON_REALM_TYPE, new DimensionType(
				OptionalLong.of(6000),
				true,
				false,
				false,
				false,
				1.0,
				false,
				false,
				DemonRealmGeneration.WORLD_BOTTOM,
				height,
				height,
				BlockTags.INFINIBURN_OVERWORLD,
				CustomSpecialEffects.DEMON_REALM_EFFECTS,
				0.8f,
				new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
		));
	}
}
