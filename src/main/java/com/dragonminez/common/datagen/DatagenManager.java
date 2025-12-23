package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DatagenManager {

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
		ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

		generator.addProvider(event.includeServer(), new DMZRecipeProvider(packOutput));
		generator.addProvider(event.includeServer(), DMZLootTableProvider.create(packOutput));
		generator.addProvider(event.includeServer(), new DMZBlockStateProvider(packOutput, existingFileHelper));
		generator.addProvider(event.includeServer(), new DMZItemModelProvider(packOutput, existingFileHelper));

		DMZBlockTagGenerator blockTagGenerator = generator.addProvider(event.includeServer(),
				new DMZBlockTagGenerator(packOutput, lookupProvider, existingFileHelper));

		generator.addProvider(event.includeServer(), new DMZItemTagGenerator(packOutput, lookupProvider,
				blockTagGenerator.contentsGetter(), existingFileHelper));

		DMZWorldGenProvider worldGenProvider = generator.addProvider(event.includeServer(),
				new DMZWorldGenProvider(packOutput, lookupProvider));

		generator.addProvider(event.includeServer(),
				new DMZBiomeTagGenerator(packOutput, worldGenProvider.getRegistryProvider(), existingFileHelper));

		generator.addProvider(event.includeServer(), new DMZAdvancementsProvider(packOutput, lookupProvider));
	}
}
