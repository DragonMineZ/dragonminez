package com.dragonminez.common.datagen;

import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainItems;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class DMZBlockLootTables extends BlockLootSubProvider {
	public DMZBlockLootTables() {
		super(Set.of(), FeatureFlags.REGISTRY.allFlags());
	}

	@Override
	protected void generate() {
		//Dragon Balls
		this.dropSelf(MainBlocks.DBALL1_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL2_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL3_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL4_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL5_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL6_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL7_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL1_NAMEK_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL2_NAMEK_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL3_NAMEK_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL4_NAMEK_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL5_NAMEK_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL6_NAMEK_BLOCK.get());
		this.dropSelf(MainBlocks.DBALL7_NAMEK_BLOCK.get());

		//Maderas + Bloques "dropSelf"
		this.dropSelf(MainBlocks.NAMEK_AJISSA_LOG.get());
		this.dropSelf(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG.get());
		this.dropSelf(MainBlocks.NAMEK_AJISSA_WOOD.get());
		this.dropSelf(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD.get());
		this.dropSelf(MainBlocks.NAMEK_AJISSA_PLANKS.get());
		this.add(MainBlocks.NAMEK_AJISSA_DOOR.get(), block -> createDoorTable(MainBlocks.NAMEK_AJISSA_DOOR.get()));
		this.dropSelf(MainBlocks.NAMEK_AJISSA_TRAPDOOR.get());
		this.add(MainBlocks.NAMEK_AJISSA_SLAB.get(), block -> createSlabItemTable(MainBlocks.NAMEK_AJISSA_SLAB.get()));
		this.dropSelf(MainBlocks.NAMEK_AJISSA_STAIRS.get());
		this.dropSelf(MainBlocks.NAMEK_AJISSA_FENCE.get());
		this.dropSelf(MainBlocks.NAMEK_AJISSA_FENCE_GATE.get());
		this.dropSelf(MainBlocks.NAMEK_AJISSA_BUTTON.get());
		this.dropSelf(MainBlocks.NAMEK_AJISSA_SAPLING.get());
		this.dropSelf(MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_LOG.get());
		this.dropSelf(MainBlocks.NAMEK_STRIPPED_SACRED_LOG.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_WOOD.get());
		this.dropSelf(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_PLANKS.get());
		this.add(MainBlocks.NAMEK_SACRED_DOOR.get(), block -> createDoorTable(MainBlocks.NAMEK_SACRED_DOOR.get()));
		this.dropSelf(MainBlocks.NAMEK_SACRED_TRAPDOOR.get());
		this.add(MainBlocks.NAMEK_SACRED_SLAB.get(), block -> createSlabItemTable(MainBlocks.NAMEK_SACRED_SLAB.get()));
		this.dropSelf(MainBlocks.NAMEK_SACRED_STAIRS.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_FENCE.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_FENCE_GATE.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_BUTTON.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_SAPLING.get());
		this.dropSelf(MainBlocks.NAMEK_SACRED_PRESSURE_PLATE.get());
		this.dropSelf(MainBlocks.NAMEK_BLOCK.get());
		this.dropSelf(MainBlocks.NAMEK_DIRT.get());
		this.dropSelf(MainBlocks.GETE_BLOCK.get());
		this.dropSelf(MainBlocks.KIKONO_BLOCK.get());
		this.dropSelf(MainBlocks.NAMEK_COBBLESTONE.get());
		this.dropSelf(MainBlocks.NAMEK_STONE_STAIRS.get());
		this.dropSelf(MainBlocks.NAMEK_STONE_SLAB.get());
		this.dropSelf(MainBlocks.NAMEK_STONE_WALL.get());
		this.dropSelf(MainBlocks.NAMEK_COBBLESTONE_STAIRS.get());
		this.dropSelf(MainBlocks.NAMEK_COBBLESTONE_SLAB.get());
		this.dropSelf(MainBlocks.NAMEK_COBBLESTONE_WALL.get());
		this.dropSelf(MainBlocks.NAMEK_DEEPSLATE.get());
		this.dropSelf(MainBlocks.NAMEK_DEEPSLATE_STAIRS.get());
		this.dropSelf(MainBlocks.NAMEK_DEEPSLATE_SLAB.get());
		this.dropSelf(MainBlocks.NAMEK_DEEPSLATE_WALL.get());
		this.dropSelf(MainBlocks.ROCKY_DIRT.get());
		this.dropSelf(MainBlocks.ROCKY_STONE_STAIRS.get());
		this.dropSelf(MainBlocks.ROCKY_STONE_SLAB.get());
		this.dropSelf(MainBlocks.ROCKY_STONE_WALL.get());
		this.dropSelf(MainBlocks.ROCKY_COBBLESTONE.get());
		this.dropSelf(MainBlocks.ROCKY_COBBLESTONE_STAIRS.get());
		this.dropSelf(MainBlocks.ROCKY_COBBLESTONE_SLAB.get());
		this.dropSelf(MainBlocks.ROCKY_COBBLESTONE_WALL.get());
		//this.dropSelf(MainBlocks.GETE_FURNACE.get());
		this.dropSelf(MainBlocks.GETE_ORE.get());
		this.dropSelf(MainBlocks.TIME_CHAMBER_BLOCK.get());
		//this.dropSelf(MainBlocks.KIKONO_ARMOR_STATION.get());


		//Bloques que Dropean otros items
		this.add(MainBlocks.NAMEK_DIAMOND_ORE.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_DIAMOND_ORE.get(), Items.DIAMOND));
		this.add(MainBlocks.NAMEK_GOLD_ORE.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_GOLD_ORE.get(), Items.RAW_GOLD));
		this.add(MainBlocks.NAMEK_IRON_ORE.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_IRON_ORE.get(), Items.RAW_IRON));
		this.add(MainBlocks.NAMEK_LAPIS_ORE.get(),
				block -> MultiOreDrop(MainBlocks.NAMEK_LAPIS_ORE.get(), Items.LAPIS_LAZULI));
		this.add(MainBlocks.NAMEK_REDSTONE_ORE.get(),
				block -> MultiOreDrop(MainBlocks.NAMEK_REDSTONE_ORE.get(), Items.REDSTONE));
		this.add(MainBlocks.NAMEK_COAL_ORE.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_COAL_ORE.get(), Items.COAL));
		this.add(MainBlocks.NAMEK_EMERALD_ORE.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_EMERALD_ORE.get(), Items.EMERALD));
		this.add(MainBlocks.NAMEK_COPPER_ORE.get(),
				block -> CopperOreDrop(MainBlocks.NAMEK_COPPER_ORE.get(), Items.RAW_COPPER));
		this.add(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_DEEPSLATE_DIAMOND.get(), Items.DIAMOND));
		this.add(MainBlocks.NAMEK_DEEPSLATE_GOLD.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_DEEPSLATE_GOLD.get(), Items.RAW_GOLD));
		this.add(MainBlocks.NAMEK_DEEPSLATE_IRON.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_DEEPSLATE_IRON.get(), Items.RAW_IRON));
		this.add(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get(),
				block -> MultiOreDrop(MainBlocks.NAMEK_DEEPSLATE_LAPIS.get(), Items.LAPIS_LAZULI));
		this.add(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get(),
				block -> MultiOreDrop(MainBlocks.NAMEK_DEEPSLATE_REDSTONE.get(), Items.REDSTONE));
		this.add(MainBlocks.NAMEK_DEEPSLATE_COAL.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_DEEPSLATE_COAL.get(), Items.COAL));
		this.add(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_DEEPSLATE_EMERALD.get(), Items.EMERALD));
		this.add(MainBlocks.NAMEK_DEEPSLATE_COPPER.get(),
				block -> CopperOreDrop(MainBlocks.NAMEK_DEEPSLATE_COPPER.get(), Items.RAW_COPPER));
		this.add(MainBlocks.NAMEK_KIKONO_ORE.get(),
				block -> SingleOreDrop(MainBlocks.NAMEK_KIKONO_ORE.get(), MainItems.KIKONO_SHARD.get()));

		//Bloques que se dropean si se rompen con Silk Touch
		this.add(MainBlocks.NAMEK_STONE.get(), block -> SilkTouchBlockDrop(MainBlocks.NAMEK_STONE.get(), MainBlocks.NAMEK_COBBLESTONE.get()));
		this.add(MainBlocks.ROCKY_STONE.get(), block -> SilkTouchBlockDrop(MainBlocks.ROCKY_STONE.get(), MainBlocks.ROCKY_COBBLESTONE.get()));

		//Vegetacion
		this.add(MainBlocks.NAMEK_AJISSA_LEAVES.get(),
				block -> createLeavesDrops(block, MainBlocks.NAMEK_AJISSA_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
		this.add(MainBlocks.NAMEK_SACRED_LEAVES.get(),
				block -> createLeavesDrops(block, MainBlocks.NAMEK_SACRED_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
		this.add(MainBlocks.NAMEK_GRASS_BLOCK.get(), block -> SilkTouchBlockDrop(MainBlocks.NAMEK_GRASS_BLOCK.get(), MainBlocks.NAMEK_DIRT.get()));
		this.add(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get(), block -> SilkTouchBlockDrop(MainBlocks.NAMEK_SACRED_GRASS_BLOCK.get(), MainBlocks.NAMEK_DIRT.get()));
		this.add(MainBlocks.NAMEK_GRASS.get(), block -> ShearsOnlyDrop(MainBlocks.NAMEK_GRASS.get()));
		this.add(MainBlocks.NAMEK_SACRED_GRASS.get(), block -> ShearsOnlyDrop(MainBlocks.NAMEK_SACRED_GRASS.get()));
		this.add(MainBlocks.NAMEK_FERN.get(), block -> ShearsOnlyDrop(MainBlocks.NAMEK_FERN.get()));
		this.add(MainBlocks.POTTED_NAMEK_FERN.get(), createPotFlowerItemTable(MainBlocks.NAMEK_FERN.get()));
		this.add(MainBlocks.SACRED_FERN.get(), block -> ShearsOnlyDrop(MainBlocks.SACRED_FERN.get()));
		this.add(MainBlocks.POTTED_SACRED_FERN.get(), createPotFlowerItemTable(MainBlocks.SACRED_FERN.get()));
		this.dropSelf(MainBlocks.CHRYSANTHEMUM_FLOWER.get());
		this.add(MainBlocks.POTTED_CHRYSANTHEMUM_FLOWER.get(), createPotFlowerItemTable(MainBlocks.CHRYSANTHEMUM_FLOWER.get()));
		this.dropSelf(MainBlocks.AMARYLLIS_FLOWER.get());
		this.add(MainBlocks.POTTED_AMARYLLIS_FLOWER.get(), createPotFlowerItemTable(MainBlocks.AMARYLLIS_FLOWER.get()));
		this.dropSelf(MainBlocks.MARIGOLD_FLOWER.get());
		this.add(MainBlocks.POTTED_MARIGOLD_FLOWER.get(), createPotFlowerItemTable(MainBlocks.MARIGOLD_FLOWER.get()));
		this.dropSelf(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get());
		this.add(MainBlocks.POTTED_CATHARANTHUS_ROSEUS_FLOWER.get(), createPotFlowerItemTable(MainBlocks.CATHARANTHUS_ROSEUS_FLOWER.get()));
		this.dropSelf(MainBlocks.TRILLIUM_FLOWER.get());
		this.add(MainBlocks.POTTED_TRILLIUM_FLOWER.get(), createPotFlowerItemTable(MainBlocks.TRILLIUM_FLOWER.get()));
		this.dropSelf(MainBlocks.LOTUS_FLOWER.get());
		this.dropSelf(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get());
		this.add(MainBlocks.POTTED_SACRED_CHRYSANTHEMUM_FLOWER.get(), createPotFlowerItemTable(MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER.get()));
		this.dropSelf(MainBlocks.SACRED_AMARYLLIS_FLOWER.get());
		this.add(MainBlocks.POTTED_SACRED_AMARYLLIS_FLOWER.get(), createPotFlowerItemTable(MainBlocks.SACRED_AMARYLLIS_FLOWER.get()));
		this.dropSelf(MainBlocks.SACRED_MARIGOLD_FLOWER.get());
		this.add(MainBlocks.POTTED_SACRED_MARIGOLD_FLOWER.get(), createPotFlowerItemTable(MainBlocks.SACRED_MARIGOLD_FLOWER.get()));
		this.dropSelf(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get());
		this.add(MainBlocks.POTTED_SACRED_CATHARANTHUS_ROSEUS_FLOWER.get(), createPotFlowerItemTable(MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER.get()));
		this.dropSelf(MainBlocks.SACRED_TRILLIUM_FLOWER.get());
		this.add(MainBlocks.POTTED_SACRED_TRILLIUM_FLOWER.get(), createPotFlowerItemTable(MainBlocks.SACRED_TRILLIUM_FLOWER.get()));
		this.add(MainBlocks.POTTED_AJISSA_SAPLING.get(), createPotFlowerItemTable(MainBlocks.NAMEK_AJISSA_SAPLING.get()));
		this.add(MainBlocks.POTTED_SACRED_SAPLING.get(), createPotFlowerItemTable(MainBlocks.NAMEK_SACRED_SAPLING.get()));
	}


	//Ores que dropeen más de un item (Ej: Lapis, Cobre)
	protected LootTable.Builder MultiOreDrop(Block pBlock, Item item) {
		return createSilkTouchDispatchTable(pBlock,
				this.applyExplosionDecay(pBlock,
						LootItem.lootTableItem(item)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 5.0F)))
								.apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))));
	}
	protected LootTable.Builder CopperOreDrop(Block pBlock, Item item) {
		return createSilkTouchDispatchTable(pBlock,
				this.applyExplosionDecay(pBlock,
						LootItem.lootTableItem(item)
								.apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F)))
								.apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))));
	}

	//Ores que dropeen un solo item (Ej: Diamante, Oro)
	protected LootTable.Builder SingleOreDrop(Block pBlock, Item item) {
		return createSilkTouchDispatchTable(pBlock,
				this.applyExplosionDecay(pBlock,
						LootItem.lootTableItem(item)
								.apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
								.apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))));
	}

	protected LootTable.Builder SilkTouchBlockDrop(Block pBlock, Block pDrop) {
		return LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(pBlock)
								.when(HAS_SILK_TOUCH))
						.add(LootItem.lootTableItem(pDrop)
								.when(HAS_NO_SILK_TOUCH)));
	}
	protected LootTable.Builder ShearsOnlyDrop(Block pBlock) {
		return LootTable.lootTable()
				.withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1))
						.add(LootItem.lootTableItem(pBlock)
								.when(HAS_SHEARS))
						.add(EmptyLootItem.emptyItem()
								.when(HAS_SHEARS.invert())));
	}

	@Override
	protected Iterable<Block> getKnownBlocks() {
		return MainBlocks.BLOCK_REGISTER.getEntries().stream().map(RegistryObject::get)::iterator;
	}
}
