package com.dragonminez.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;

public class DMZLootTableProvider {
	public static net.minecraft.data.loot.LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		return new net.minecraft.data.loot.LootTableProvider(output, Set.of(), List.of(
				new net.minecraft.data.loot.LootTableProvider.SubProviderEntry(regs -> new DMZBlockLootTables(regs), LootContextParamSets.BLOCK)
		), registries);
	}
}
