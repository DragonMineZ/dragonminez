package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class DMZBlockTagGenerator extends BlockTagsProvider {
	public DMZBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
								@Nullable ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(HolderLookup.@NotNull Provider pProvider) {
		this.tag(BlockTags.FROG_PREFER_JUMP_TO)
				.add(MainBlocks.LOTUS_FLOWER.get());
		this.tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS)
				.add(MainBlocks.LOTUS_FLOWER.get());
		this.tag(BlockTags.CLIMBABLE)
				.add(MainBlocks.INVISIBLE_LADDER_BLOCK.get());

		//Tags para los Bloques
		this.tag(BlockTags.NEEDS_STONE_TOOL)
				.add(MainBlocks.NAMEK_IRON_ORE.get())
				.add(MainBlocks.NAMEK_COPPER_ORE.get())
				.add(MainBlocks.NAMEK_LAPIS_ORE.get())
				.add(MainBlocks.NAMEK_COAL_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_IRON.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_COPPER.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_COAL.get());

		this.tag(BlockTags.NEEDS_IRON_TOOL)
				.add(MainBlocks.NAMEK_DIAMOND_ORE.get())
				.add(MainBlocks.NAMEK_GOLD_ORE.get())
				.add(MainBlocks.NAMEK_REDSTONE_ORE.get())
				.add(MainBlocks.NAMEK_EMERALD_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_GOLD.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get());

		this.tag(BlockTags.NEEDS_DIAMOND_TOOL)
		/*add.(MainBlocks.EJEMPLO.get())*/;

		this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL)
				.add(MainBlocks.GETE_ORE.get())
				.add(MainBlocks.GETE_BLOCK.get())
				.add(MainBlocks.NAMEK_KIKONO_ORE.get())
				.add(MainBlocks.KIKONO_BLOCK.get());
				//.add(MainBlocks.KIKONO_ARMOR_STATION.get());

		this.tag(BlockTags.MINEABLE_WITH_AXE)
				.add(MainBlocks.NAMEK_AJISSA_LOG.get())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get())
				.add(MainBlocks.NAMEK_AJISSA_WOOD.get())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get())
				.add(MainBlocks.NAMEK_AJISSA_PLANKS.get())
				.add(MainBlocks.NAMEK_AJISSA_DOOR.get())
				.add(MainBlocks.NAMEK_AJISSA_TRAPDOOR.get())
				.add(MainBlocks.NAMEK_AJISSA_SLAB.get())
				.add(MainBlocks.NAMEK_AJISSA_STAIRS.get())
				.add(MainBlocks.NAMEK_AJISSA_FENCE.get())
				.add(MainBlocks.NAMEK_AJISSA_FENCE_GATE.get())
				.add(MainBlocks.NAMEK_SACRED_LOG.get())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get())
				.add(MainBlocks.NAMEK_SACRED_WOOD.get())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get())
				.add(MainBlocks.NAMEK_SACRED_PLANKS.get())
				.add(MainBlocks.NAMEK_SACRED_DOOR.get())
				.add(MainBlocks.NAMEK_SACRED_TRAPDOOR.get())
				.add(MainBlocks.NAMEK_SACRED_SLAB.get())
				.add(MainBlocks.NAMEK_SACRED_STAIRS.get())
				.add(MainBlocks.NAMEK_SACRED_FENCE.get())
				.add(MainBlocks.NAMEK_SACRED_FENCE_GATE.get());

		this.tag(BlockTags.MINEABLE_WITH_HOE)
				.add(MainBlocks.NAMEK_AJISSA_LEAVES.get())
				.add(MainBlocks.NAMEK_SACRED_LEAVES.get());

		this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.add(MainBlocks.NAMEK_BLOCK.get())
				.add(MainBlocks.NAMEK_STONE.get())
				.add(MainBlocks.NAMEK_STONE_SLAB.get())
				.add(MainBlocks.NAMEK_STONE_STAIRS.get())
				.add(MainBlocks.NAMEK_STONE_WALL.get())
				.add(MainBlocks.NAMEK_COBBLESTONE.get())
				.add(MainBlocks.NAMEK_COBBLESTONE_SLAB.get())
				.add(MainBlocks.NAMEK_COBBLESTONE_STAIRS.get())
				.add(MainBlocks.NAMEK_DEEPSLATE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_SLAB.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_STAIRS.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_WALL.get())
				.add(MainBlocks.ROCKY_STONE.get())
				.add(MainBlocks.ROCKY_STONE_SLAB.get())
				.add(MainBlocks.ROCKY_STONE_STAIRS.get())
				.add(MainBlocks.ROCKY_STONE_WALL.get())
				.add(MainBlocks.ROCKY_COBBLESTONE.get())
				.add(MainBlocks.ROCKY_COBBLESTONE_SLAB.get())
				.add(MainBlocks.ROCKY_COBBLESTONE_STAIRS.get())
				.add(MainBlocks.ROCKY_COBBLESTONE_WALL.get())
				.add(MainBlocks.DBALL1_BLOCK.get())
				.add(MainBlocks.DBALL2_BLOCK.get())
				.add(MainBlocks.DBALL3_BLOCK.get())
				.add(MainBlocks.DBALL4_BLOCK.get())
				.add(MainBlocks.DBALL5_BLOCK.get())
				.add(MainBlocks.DBALL6_BLOCK.get())
				.add(MainBlocks.DBALL7_BLOCK.get())
				.add(MainBlocks.DBALL1_NAMEK_BLOCK.get())
				.add(MainBlocks.DBALL2_NAMEK_BLOCK.get())
				.add(MainBlocks.DBALL3_NAMEK_BLOCK.get())
				.add(MainBlocks.DBALL4_NAMEK_BLOCK.get())
				.add(MainBlocks.DBALL5_NAMEK_BLOCK.get())
				.add(MainBlocks.DBALL6_NAMEK_BLOCK.get())
				.add(MainBlocks.DBALL7_NAMEK_BLOCK.get())
				.add(MainBlocks.NAMEK_COAL_ORE.get())
				.add(MainBlocks.NAMEK_IRON_ORE.get())
				.add(MainBlocks.NAMEK_COPPER_ORE.get())
				.add(MainBlocks.NAMEK_LAPIS_ORE.get())
				.add(MainBlocks.NAMEK_GOLD_ORE.get())
				.add(MainBlocks.NAMEK_DIAMOND_ORE.get())
				.add(MainBlocks.NAMEK_REDSTONE_ORE.get())
				.add(MainBlocks.NAMEK_EMERALD_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_COAL.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_IRON.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_COPPER.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_GOLD.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get())
				.add(MainBlocks.GETE_ORE.get())
				.add(MainBlocks.GETE_BLOCK.get())
				.add(MainBlocks.NAMEK_KIKONO_ORE.get())
				.add(MainBlocks.KIKONO_BLOCK.get());

		this.tag(BlockTags.MINEABLE_WITH_SHOVEL)
				.add(MainBlocks.NAMEK_GRASS_BLOCK.get())
				.add(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get())
				.add(MainBlocks.NAMEK_DIRT.get());

		this.tag(BlockTags.LOGS)
				.add(MainBlocks.NAMEK_AJISSA_LOG.get())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get())
				.add(MainBlocks.NAMEK_SACRED_LOG.get())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get());

		this.tag(BlockTags.LOGS_THAT_BURN)
				.add(MainBlocks.NAMEK_AJISSA_LOG.get())
				.add(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get())
				.add(MainBlocks.NAMEK_SACRED_LOG.get())
				.add(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get());

		this.tag(BlockTags.PLANKS)
				.add(MainBlocks.NAMEK_AJISSA_PLANKS.get())
				.add(MainBlocks.NAMEK_SACRED_PLANKS.get());

		this.tag(BlockTags.WOODEN_BUTTONS)
				.add(MainBlocks.NAMEK_AJISSA_BUTTON.get())
				.add(MainBlocks.NAMEK_SACRED_BUTTON.get());

		this.tag(BlockTags.BUTTONS)
				.add(MainBlocks.NAMEK_AJISSA_BUTTON.get())
				.add(MainBlocks.NAMEK_SACRED_BUTTON.get());

		this.tag(BlockTags.LEAVES)
				.add(MainBlocks.NAMEK_AJISSA_LEAVES.get())
				.add(MainBlocks.NAMEK_SACRED_LEAVES.get());

		this.tag(BlockTags.WOODEN_DOORS)
				.add(MainBlocks.NAMEK_AJISSA_DOOR.get())
				.add(MainBlocks.NAMEK_SACRED_DOOR.get());

		this.tag(BlockTags.DOORS)
				.add(MainBlocks.NAMEK_AJISSA_DOOR.get())
				.add(MainBlocks.NAMEK_SACRED_DOOR.get());

		this.tag(BlockTags.WOODEN_SLABS)
				.add(MainBlocks.NAMEK_AJISSA_SLAB.get())
				.add(MainBlocks.NAMEK_SACRED_SLAB.get());

		this.tag(BlockTags.SLABS)
				.add(MainBlocks.NAMEK_AJISSA_SLAB.get())
				.add(MainBlocks.NAMEK_SACRED_SLAB.get())
				.add(MainBlocks.NAMEK_STONE_SLAB.get())
				.add(MainBlocks.NAMEK_COBBLESTONE_SLAB.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_SLAB.get())
				.add(MainBlocks.ROCKY_STONE_SLAB.get())
				.add(MainBlocks.ROCKY_COBBLESTONE_SLAB.get());

		this.tag(BlockTags.WOODEN_STAIRS)
				.add(MainBlocks.NAMEK_AJISSA_STAIRS.get())
				.add(MainBlocks.NAMEK_SACRED_STAIRS.get());

		this.tag(BlockTags.STAIRS)
				.add(MainBlocks.NAMEK_AJISSA_STAIRS.get())
				.add(MainBlocks.NAMEK_SACRED_STAIRS.get())
				.add(MainBlocks.NAMEK_STONE_STAIRS.get())
				.add(MainBlocks.NAMEK_COBBLESTONE_STAIRS.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_STAIRS.get())
				.add(MainBlocks.ROCKY_STONE_STAIRS.get())
				.add(MainBlocks.ROCKY_COBBLESTONE_STAIRS.get());

		this.tag(BlockTags.WOODEN_TRAPDOORS)
				.add(MainBlocks.NAMEK_AJISSA_TRAPDOOR.get())
				.add(MainBlocks.NAMEK_SACRED_TRAPDOOR.get());

		this.tag(BlockTags.TRAPDOORS)
				.add(MainBlocks.NAMEK_AJISSA_TRAPDOOR.get())
				.add(MainBlocks.NAMEK_SACRED_TRAPDOOR.get());

		this.tag(BlockTags.WOODEN_FENCES)
				.add(MainBlocks.NAMEK_AJISSA_FENCE.get())
				.add(MainBlocks.NAMEK_SACRED_FENCE.get());

		this.tag(BlockTags.FENCES)
				.add(MainBlocks.NAMEK_AJISSA_FENCE.get())
				.add(MainBlocks.NAMEK_SACRED_FENCE.get());

		this.tag(BlockTags.WOODEN_PRESSURE_PLATES)
				.add(MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE.get())
				.add(MainBlocks.NAMEK_SACRED_PRESSURE_PLATE.get());

		this.tag(BlockTags.PRESSURE_PLATES)
				.add(MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE.get())
				.add(MainBlocks.NAMEK_SACRED_PRESSURE_PLATE.get());

		this.tag(BlockTags.WALLS)
				.add(MainBlocks.NAMEK_STONE_WALL.get())
				.add(MainBlocks.NAMEK_COBBLESTONE_WALL.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_WALL.get())
				.add(MainBlocks.ROCKY_STONE_WALL.get())
				.add(MainBlocks.ROCKY_COBBLESTONE_WALL.get());

		this.tag(BlockTags.ANIMALS_SPAWNABLE_ON)
				.add(MainBlocks.NAMEK_GRASS_BLOCK.get())
				.add(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get())
				.add(MainBlocks.NAMEK_DIRT.get());

		this.tag(BlockTags.FROGS_SPAWNABLE_ON)
				.add(MainBlocks.NAMEK_GRASS_BLOCK.get())
				.add(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get());

		this.tag(BlockTags.BEACON_BASE_BLOCKS)
				.add(MainBlocks.GETE_BLOCK.get())
				.add(MainBlocks.KIKONO_BLOCK.get());

		this.tag(BlockTags.DIRT)
				.add(MainBlocks.NAMEK_DIRT.get())
				.add(MainBlocks.NAMEK_GRASS_BLOCK.get())
				.add(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get())
				.add(MainBlocks.ROCKY_DIRT.get());

		this.tag(BlockTags.COAL_ORES)
				.add(MainBlocks.NAMEK_COAL_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_COAL.get());

		this.tag(BlockTags.IRON_ORES)
				.add(MainBlocks.NAMEK_IRON_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_IRON.get());

		this.tag(BlockTags.COPPER_ORES)
				.add(MainBlocks.NAMEK_COPPER_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_COPPER.get());

		this.tag(BlockTags.LAPIS_ORES)
				.add(MainBlocks.NAMEK_LAPIS_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get());

		this.tag(BlockTags.GOLD_ORES)
				.add(MainBlocks.NAMEK_GOLD_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_GOLD.get());

		this.tag(BlockTags.DIAMOND_ORES)
				.add(MainBlocks.NAMEK_DIAMOND_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get());

		this.tag(BlockTags.REDSTONE_ORES)
				.add(MainBlocks.NAMEK_REDSTONE_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get());

		this.tag(BlockTags.EMERALD_ORES)
				.add(MainBlocks.NAMEK_EMERALD_ORE.get())
				.add(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get());

		this.tag(BlockTags.FLOWERS)
				.add(MainBlocks.CHRYSANTHEMUM_FLOWER.get())
				.add(MainBlocks.MARIGOLD_FLOWER.get())
				.add(MainBlocks.AMARYLLIS_FLOWER.get())
				.add(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get())
				.add(MainBlocks.TRILLIUM_FLOWER.get())
				.add(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get())
				.add(MainBlocks.SACRED_MARIGOLD_FLOWER.get())
				.add(MainBlocks.SACRED_AMARYLLIS_FLOWER.get())
				.add(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get())
				.add(MainBlocks.SACRED_TRILLIUM_FLOWER.get());

		this.tag(BlockTags.FLOWER_POTS)
				.add(MainBlocks.POTTED_CHRYSANTHEMUM_FLOWER.get())
				.add(MainBlocks.POTTED_MARIGOLD_FLOWER.get())
				.add(MainBlocks.POTTED_AMARYLLIS_FLOWER.get())
				.add(MainBlocks.POTTED_CATHARANTHUS_ROSEUS_FLOWER.get())
				.add(MainBlocks.POTTED_TRILLIUM_FLOWER.get())
				.add(MainBlocks.POTTED_SACRED_CHRYSANTHEMUM_FLOWER.get())
				.add(MainBlocks.POTTED_SACRED_MARIGOLD_FLOWER.get())
				.add(MainBlocks.POTTED_AMARYLLIS_FLOWER.get())
				.add(MainBlocks.POTTED_CATHARANTHUS_ROSEUS_FLOWER.get())
				.add(MainBlocks.POTTED_TRILLIUM_FLOWER.get());

		this.tag(BlockTags.SAPLINGS)
				.add(MainBlocks.NAMEK_SACRED_SAPLING.get())
				.add(MainBlocks.NAMEK_AJISSA_SAPLING.get());

		this.tag(BlockTags.VALID_SPAWN)
				.add(MainBlocks.NAMEK_DIRT.get())
				.add(MainBlocks.NAMEK_GRASS_BLOCK.get())
				.add(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get())
				.add(MainBlocks.ROCKY_DIRT.get());
	}
}
