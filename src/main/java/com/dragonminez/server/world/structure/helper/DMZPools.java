package com.dragonminez.server.world.structure.helper;

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
	public static final ResourceKey<StructureTemplatePool> GOKU_HOUSE = createKey("goku_house"),
			ROSHI_HOUSE = createKey("roshi_house"), TIMECHAMBER = createKey("timechamber"),
			ELDER_GURU = createKey("elder_guru"), KAMILOOKOUT = createKey("kamilookout"),
			GERO_LAB = createKey("gero_lab"),
			GERO_LAB_STAIRS = createKey("gero_lab/stairs"),
			GERO_LAB_UNDERGROUND = createKey("gero_lab/underground");

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

		context.register(TIMECHAMBER, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:timechamber"), 1)),
				StructureTemplatePool.Projection.RIGID
		));

		context.register(ELDER_GURU, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:elder_guru"), 1)),
				StructureTemplatePool.Projection.RIGID
		));

		context.register(KAMILOOKOUT, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:kamilookout"), 1)),
				StructureTemplatePool.Projection.RIGID
		));

		context.register(GERO_LAB, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:gero_lab_surface"), 1)),
				StructureTemplatePool.Projection.RIGID
		));

		context.register(GERO_LAB_STAIRS, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:gero_lab_stairs"), 1)),
				StructureTemplatePool.Projection.RIGID
		));

		context.register(GERO_LAB_UNDERGROUND, new StructureTemplatePool(
				empty,
				ImmutableList.of(Pair.of(StructurePoolElement.single("dragonminez:gero_lab_underground"), 1)),
				StructureTemplatePool.Projection.RIGID
		));
	}

	private static ResourceKey<StructureTemplatePool> createKey(String name) {
		return ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, name));
	}
}