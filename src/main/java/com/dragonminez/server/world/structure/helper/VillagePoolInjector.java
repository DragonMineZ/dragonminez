package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.mixin.common.StructureTemplatePoolAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class VillagePoolInjector {

	public static void injectAll(MinecraftServer server) {
		Registry<StructureTemplatePool> pools = server.registryAccess()
				.registryOrThrow(Registries.TEMPLATE_POOL);
		Registry<StructureProcessorList> procs = server.registryAccess()
				.registryOrThrow(Registries.PROCESSOR_LIST);

		Holder<StructureProcessorList> empty = procs.getHolderOrThrow(
				ResourceKey.create(Registries.PROCESSOR_LIST, new ResourceLocation("minecraft", "empty")));

		inject(pools, empty, "minecraft:village/plains/houses", "dragonminez:cc_villager", 20);
	}

	private static void inject(Registry<StructureTemplatePool> pools, Holder<StructureProcessorList> processors,
							   String poolId, String pieceId, int weight) {
		StructureTemplatePool pool = pools.get(new ResourceLocation(poolId));
		if (pool == null) {
			LogUtil.warn(Env.SERVER, "VillagePoolInjector: pool '" + poolId + "' not found, skipping '" + pieceId + "'.");
			return;
		}

		StructurePoolElement piece = StructurePoolElement
				.single(pieceId, processors)
				.apply(StructureTemplatePool.Projection.RIGID);

		StructureTemplatePoolAccessor accessor = (StructureTemplatePoolAccessor) pool;

		for (int i = 0; i < weight; i++) {
			accessor.getTemplates().add(piece);
		}

		LogUtil.info(Env.SERVER, "VillagePoolInjector: added '" + pieceId + "' to '" + poolId + "' (weight " + weight + ").");
	}
}
