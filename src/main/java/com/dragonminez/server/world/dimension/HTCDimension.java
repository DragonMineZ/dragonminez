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

public class HTCDimension {
	public static final ResourceKey<Level> HTC_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "time_chamber"));
	public static final ResourceKey<DimensionType> HTC_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "time_chamber"));

	public static void bootstrap(BootstapContext<DimensionType> context) {
		context.register(HTC_TYPE, new DimensionType(
				OptionalLong.of(7500),
				true,
				false,
				false,
				false,
				100.0,
				true,
				true,
				-16,
				96,
				96,
				BlockTags.INFINIBURN_OVERWORLD,
				CustomSpecialEffects.HTC_EFFECT,
				0.0f,
				new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
		));
	}
}