package com.dragonminez.server.world.dimension;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.OptionalLong;

public class NamekDimension {
	public static final ResourceKey<Level> NAMEK_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek"));
	public static final ResourceKey<DimensionType> NAMEK_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "namek"));

	public static void bootstrap(BootstapContext<DimensionType> context) {
		context.register(NAMEK_TYPE, new DimensionType(
				OptionalLong.of(7500),
				true,
				false,
				false,
				true,
				3.0,
				true,
				true,
				-64,
				384,
				384,
				BlockTags.INFINIBURN_OVERWORLD,
				CustomSpecialEffects.NAMEK_EFFECTS,
				0.0f,
				new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
		));
	}
}