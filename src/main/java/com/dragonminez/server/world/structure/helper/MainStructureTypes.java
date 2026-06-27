package com.dragonminez.server.world.structure.helper;

import com.dragonminez.Reference;
import com.dragonminez.server.world.structure.TallJigsawStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class MainStructureTypes {
	public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
			DeferredRegister.create(Registries.STRUCTURE_TYPE, Reference.MOD_ID);

	public static final RegistryObject<StructureType<TallJigsawStructure>> TALL_JIGSAW =
			STRUCTURE_TYPES.register("tall_jigsaw", () -> () -> TallJigsawStructure.CODEC);

	public static void register(IEventBus eventBus) {
		STRUCTURE_TYPES.register(eventBus);
	}
}
