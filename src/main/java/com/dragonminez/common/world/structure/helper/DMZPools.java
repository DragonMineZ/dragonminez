package com.dragonminez.common.world.structure.helper;

import com.dragonminez.Reference;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class DMZPools {
	public static final ResourceKey<StructureTemplatePool> GOKU_HOUSE = createKey("goku_house");
	public static final ResourceKey<StructureTemplatePool> ROSHI_HOUSE = createKey("roshi_house");

	public static void bootstrap(BootstapContext<StructureTemplatePool> context) {
		Holder<StructureTemplatePool> empty = context.lookup(Registries.TEMPLATE_POOL).getOrThrow(Pools.EMPTY);

		context.register(GOKU_HOUSE, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:goku_house"), 1)),
				StructureTemplatePool.Projection.RIGID
		));

		context.register(ROSHI_HOUSE, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:roshi_house"), 1)),
				StructureTemplatePool.Projection.RIGID
		));
	}

	private static ResourceKey<StructureTemplatePool> createKey(String name) {
		return ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(Reference.MOD_ID, name));
	}
}