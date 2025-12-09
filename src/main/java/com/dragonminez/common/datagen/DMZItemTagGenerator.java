package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DMZItemTagGenerator extends ItemTagsProvider {
	public DMZItemTagGenerator(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider,
							   CompletableFuture<TagLookup<Block>> pBlockTags, @Nullable ExistingFileHelper existingFileHelper) {
		super(pOutput, pLookupProvider, pBlockTags, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider pProvider) {
		this.tag(ItemTags.LOGS)
				.add(MainBlocks.NAMEK_AJISSA_LOG.get().asItem())
				.add(MainBlocks.NAMEK_AJISSA_WOOD.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get().asItem())
				.add(MainBlocks.NAMEK_SACRED_LOG.get().asItem())
				.add(MainBlocks.NAMEK_SACRED_WOOD.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get().asItem());

		this.tag(ItemTags.LOGS_THAT_BURN)
				.add(MainBlocks.NAMEK_AJISSA_LOG.get().asItem())
				.add(MainBlocks.NAMEK_AJISSA_WOOD.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get().asItem())
				.add(MainBlocks.NAMEK_SACRED_LOG.get().asItem())
				.add(MainBlocks.NAMEK_SACRED_WOOD.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get().asItem())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get().asItem());

		this.tag(ItemTags.TOOLS)
				.add(MainItems.ARMOR_CRAFTING_KIT.get());

		this.tag(Tags.Items.INGOTS)
				.add(MainItems.GETE_SCRAP.get())
				.add(MainItems.GETE_INGOT.get())
				.add(MainItems.KIKONO_SHARD.get())
				.add(MainItems.KIKONO_STRING.get())
				.add(MainItems.KIKONO_CLOTH.get());

		this.tag(ItemTags.PLANKS)
				.add(MainBlocks.NAMEK_AJISSA_PLANKS.get().asItem())
				.add(MainBlocks.NAMEK_SACRED_PLANKS.get().asItem());

		this.tag(ItemTags.LEAVES)
				.add(MainBlocks.NAMEK_AJISSA_LEAVES.get().asItem())
				.add(MainBlocks.NAMEK_SACRED_LEAVES.get().asItem());

		this.tag(ItemTags.STONE_CRAFTING_MATERIALS)
				.add(MainBlocks.NAMEK_STONE.get().asItem())
				.add(MainBlocks.NAMEK_COBBLESTONE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE.get().asItem());

		this.tag(ItemTags.STONE_TOOL_MATERIALS)
				.add(MainBlocks.NAMEK_STONE.get().asItem())
				.add(MainBlocks.NAMEK_COBBLESTONE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE.get().asItem());

		this.tag(ItemTags.COAL_ORES)
				.add(MainBlocks.NAMEK_COAL_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_COAL.get().asItem());

		this.tag(ItemTags.IRON_ORES)
				.add(MainBlocks.NAMEK_IRON_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_IRON.get().asItem());

		this.tag(ItemTags.GOLD_ORES)
				.add(MainBlocks.NAMEK_GOLD_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_GOLD.get().asItem());

		this.tag(ItemTags.REDSTONE_ORES)
				.add(MainBlocks.NAMEK_REDSTONE_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get().asItem());

		this.tag(ItemTags.LAPIS_ORES)
				.add(MainBlocks.NAMEK_LAPIS_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get().asItem());

		this.tag(ItemTags.DIAMOND_ORES)
				.add(MainBlocks.NAMEK_DIAMOND_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get().asItem());

		this.tag(ItemTags.EMERALD_ORES)
				.add(MainBlocks.NAMEK_EMERALD_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get().asItem());

		this.tag(ItemTags.COPPER_ORES)
				.add(MainBlocks.NAMEK_COPPER_ORE.get().asItem())
				.add(MainBlocks.NAMEK_DEEPSLATE_COPPER.get().asItem());

		this.tag(ItemTags.FLOWERS)
				.add(MainBlocks.CHRYSANTHEMUM_FLOWER.get().asItem())
				.add(MainBlocks.MARIGOLD_FLOWER.get().asItem())
				.add(MainBlocks.AMARYLLIS_FLOWER.get().asItem())
				.add(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get().asItem())
				.add(MainBlocks.TRILLIUM_FLOWER.get().asItem())
				.add(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get().asItem())
				.add(MainBlocks.SACRED_MARIGOLD_FLOWER.get().asItem())
				.add(MainBlocks.SACRED_AMARYLLIS_FLOWER.get().asItem())
				.add(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get().asItem())
				.add(MainBlocks.SACRED_TRILLIUM_FLOWER.get().asItem());

//		this.tag(ItemTags.SWORDS)
//				.add(MainItems.BACULO_SAGRADO.get())
//				.add(MainItems.TRUNKS_SWORD.get())
//				.add(MainItems.Z_SWORD.get())
//				.add(MainItems.KATANA_YAJIROBE.get());
	}
}
