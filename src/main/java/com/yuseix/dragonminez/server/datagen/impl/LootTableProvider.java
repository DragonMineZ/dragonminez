package com.yuseix.dragonminez.server.datagen.impl;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class LootTableProvider {
    public static net.minecraft.data.loot.LootTableProvider create(PackOutput output) {
        return new net.minecraft.data.loot.LootTableProvider(output, Set.of(), List.of(
                new net.minecraft.data.loot.LootTableProvider.SubProviderEntry(BlockLootTables::new, LootContextParamSets.BLOCK)
        ));
    }
}
