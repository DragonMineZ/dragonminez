package com.dragonminez.server.world.dimension;

import com.dragonminez.Reference;
import com.dragonminez.server.world.gen.OtherworldGeneration;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalLong;

public class OtherworldDimension {
	public static final ResourceKey<Level> OTHERWORLD_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "otherworld"));
	public static final ResourceKey<DimensionType> OTHERWORLD_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "otherworld"));

	public static final Vec3 SPIRIT_ARRIVAL = new Vec3(0.5, 150, -44.5);

	public static void teleportToSpiritArrival(ServerPlayer player) {
		ServerLevel otherworld = player.getServer().getLevel(OTHERWORLD_KEY);
		if (otherworld != null) player.teleportTo(otherworld, SPIRIT_ARRIVAL.x, SPIRIT_ARRIVAL.y, SPIRIT_ARRIVAL.z, 0, 0);
	}

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
				OtherworldGeneration.WORLD_BOTTOM,
				OtherworldGeneration.WORLD_TOP - OtherworldGeneration.WORLD_BOTTOM,
				OtherworldGeneration.WORLD_TOP - OtherworldGeneration.WORLD_BOTTOM,
				BlockTags.INFINIBURN_OVERWORLD,
				CustomSpecialEffects.OTHERWORLD_EFFECTS,
				0.35f,
				new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
		));
	}
}
