package com.dragonminez.server.world.structure.processor;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MainStructureProcessors {
	public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
			DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, Reference.MOD_ID);

	public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<FoundationProcessor>> FOUNDATION =
			PROCESSORS.register("foundation", () -> () -> FoundationProcessor.CODEC);

	public static void register(IEventBus eventBus) {
		PROCESSORS.register(eventBus);
	}
}
