package com.dragonminez.server.world.structure.placement;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class MainStructurePlacements {
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
        DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Reference.MOD_ID);

    public static final RegistryObject<StructurePlacementType<UniqueNearSpawnPlacement>> UNIQUE_NEAR_SPAWN =
        PLACEMENTS.register("unique_near_spawn", () -> () -> UniqueNearSpawnPlacement.CODEC);

	public static final RegistryObject<StructurePlacementType<FixedStructurePlacement>> FIXED_PLACEMENT =
			PLACEMENTS.register("fixed_placement", () -> () -> FixedStructurePlacement.CODEC);

	public static final RegistryObject<StructurePlacementType<BiomeAwareUniquePlacement>> BIOME_AWARE_PLACEMENT =
			PLACEMENTS.register("biome_aware_placement", () -> () -> BiomeAwareUniquePlacement.CODEC);

    public static void register(IEventBus eventBus) {
        PLACEMENTS.register(eventBus);
    }
}

