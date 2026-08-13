package com.dragonminez.server.world.structure.placement;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MainStructurePlacements {
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
        DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Reference.MOD_ID);

    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<UniqueNearSpawnPlacement>> UNIQUE_NEAR_SPAWN =
        PLACEMENTS.register("unique_near_spawn", () -> () -> UniqueNearSpawnPlacement.CODEC);

	public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<FixedStructurePlacement>> FIXED_PLACEMENT =
			PLACEMENTS.register("fixed_placement", () -> () -> FixedStructurePlacement.CODEC);

	public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<BiomeAwareUniquePlacement>> BIOME_AWARE_PLACEMENT =
			PLACEMENTS.register("biome_aware_placement", () -> () -> BiomeAwareUniquePlacement.CODEC);

	public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<WideRandomSpreadPlacement>> WIDE_RANDOM_SPREAD =
			PLACEMENTS.register("wide_random_spread", () -> () -> WideRandomSpreadPlacement.CODEC);

    public static void register(IEventBus eventBus) {
        PLACEMENTS.register(eventBus);
    }
}

