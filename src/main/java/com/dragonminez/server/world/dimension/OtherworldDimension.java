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

public class OtherworldDimension {
	public static final ResourceKey<Level> OTHERWORLD_KEY = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Reference.MOD_ID, "otherworld"));
	public static final ResourceKey<DimensionType> OTHERWORLD_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, new ResourceLocation(Reference.MOD_ID, "otherworld"));

	public static void bootstrap(BootstapContext<DimensionType> context) {
		context.register(OTHERWORLD_TYPE, new DimensionType(
				OptionalLong.of(6000),
				true,
				false,
				false,
				false,
				10.0,
				false,
				false,
				0,
				320,
				320,
				BlockTags.INFINIBURN_OVERWORLD,
				CustomSpecialEffects.OTHERWORLD_EFFECTS,
				0.0f,
				new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
		));
	}
}
