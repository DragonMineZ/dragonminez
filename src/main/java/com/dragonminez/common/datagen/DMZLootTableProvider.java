package com.dragonminez.common.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class DMZLootTableProvider {
	public static net.minecraft.data.loot.LootTableProvider create(PackOutput output) {
		return new net.minecraft.data.loot.LootTableProvider(output, Set.of(), List.of(
				new net.minecraft.data.loot.LootTableProvider.SubProviderEntry(DMZBlockLootTables::new, LootContextParamSets.BLOCK)
		));
	}
}
