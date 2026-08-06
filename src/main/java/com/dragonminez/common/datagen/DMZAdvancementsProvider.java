package com.dragonminez.common.datagen;

import com.dragonminez.common.init.MainItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Minimal 1.21 advancement provider stub so the main sources compile.
 * Full advancement tree can be restored after the NeoForge port is green; shipped JSONs remain authoritative until then.
 */
@SuppressWarnings("unused")
public class DMZAdvancementsProvider extends AdvancementProvider {
	public DMZAdvancementsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, List.of(new DMZAdvancements()));
	}

	private static class DMZAdvancements implements AdvancementSubProvider {
		@Override
		public void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> consumer) {
			Advancement.Builder.advancement()
					.display(
							MainItems.DBALL4_BLOCK_ITEM.get(),
							Component.translatable("advancements.dragonminez.root.title"),
							Component.translatable("advancements.dragonminez.root.description"),
							ResourceLocation.fromNamespaceAndPath("dragonminez", "textures/block/rocky_stone.png"),
							AdvancementType.TASK, true, true, false
					)
					.addCriterion("tick", PlayerTrigger.TriggerInstance.tick())
					.save(consumer, "dragonminez:root");
		}
	}
}
