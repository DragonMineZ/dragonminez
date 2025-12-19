package com.dragonminez.common.datagen;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DMZBlockStateProvider extends net.minecraftforge.client.model.generators.BlockStateProvider {
	public DMZBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
		super(output, Reference.MOD_ID, existingFileHelper);
	}

	@Override
	public void registerStatesAndModels() {
		//Bloques
		blockWithItem(MainBlocks.NAMEK_BLOCK);
		blockWithItem(MainBlocks.NAMEK_DIRT);
		grassBlock(MainBlocks.NAMEK_GRASS_BLOCK);
		grassBlock(MainBlocks.NAMEK_SACRED_GRASS_BLOCK);
		blockWithItem(MainBlocks.NAMEK_STONE);
		blockWithItem(MainBlocks.NAMEK_COBBLESTONE);
		blockWithItem(MainBlocks.ROCKY_DIRT);
		blockWithItem(MainBlocks.ROCKY_STONE);
		blockWithItem(MainBlocks.ROCKY_COBBLESTONE);
		blockWithItem(MainBlocks.TIME_CHAMBER_BLOCK);
		axisBlock(((RotatedPillarBlock) MainBlocks.NAMEK_DEEPSLATE.get()), blockTexture(MainBlocks.NAMEK_DEEPSLATE.get()), blockTexture(MainBlocks.NAMEK_DEEPSLATE.get()));
		blockWithItem(MainBlocks.TIME_CHAMBER_PORTAL);
		blockWithItem(MainBlocks.OTHERWORLD_CLOUD);

		//Madera de Namek
		blockWithItem(MainBlocks.NAMEK_AJISSA_PLANKS);
		leavesBlock(MainBlocks.NAMEK_AJISSA_LEAVES);
		blockWithItem(MainBlocks.NAMEK_AJISSA_LOG);
		blockWithItem(MainBlocks.NAMEK_STRIPPED_AJISSA_LOG);
		blockWithItem(MainBlocks.NAMEK_AJISSA_WOOD);
		blockWithItem(MainBlocks.NAMEK_STRIPPED_AJISSA_WOOD);

		blockWithItem(MainBlocks.NAMEK_SACRED_PLANKS);
		leavesBlock(MainBlocks.NAMEK_SACRED_LEAVES);
		blockWithItem(MainBlocks.NAMEK_SACRED_LOG);
		blockWithItem(MainBlocks.NAMEK_STRIPPED_SACRED_LOG);
		blockWithItem(MainBlocks.NAMEK_SACRED_WOOD);
		blockWithItem(MainBlocks.NAMEK_STRIPPED_SACRED_WOOD);

		saplingBlock(MainBlocks.NAMEK_AJISSA_SAPLING);
		saplingBlock(MainBlocks.NAMEK_SACRED_SAPLING);

		//Ores Nuevos
		blockWithItem(MainBlocks.GETE_BLOCK);
		blockWithItem(MainBlocks.NAMEK_KIKONO_ORE);
		blockWithItem(MainBlocks.KIKONO_BLOCK);

		//Ores (Default) de Namek
		blockWithItem(MainBlocks.NAMEK_DIAMOND_ORE);
		blockWithItem(MainBlocks.NAMEK_GOLD_ORE);
		blockWithItem(MainBlocks.NAMEK_IRON_ORE);
		blockWithItem(MainBlocks.NAMEK_LAPIS_ORE);
		blockWithItem(MainBlocks.NAMEK_REDSTONE_ORE);
		blockWithItem(MainBlocks.NAMEK_COAL_ORE);
		blockWithItem(MainBlocks.NAMEK_EMERALD_ORE);
		blockWithItem(MainBlocks.NAMEK_COPPER_ORE);

		//Ores (Default) de Deepslate de Namek
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_DIAMOND);
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_GOLD);
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_IRON);
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_LAPIS);
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_REDSTONE);
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_COAL);
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_EMERALD);
		blockWithItem(MainBlocks.NAMEK_DEEPSLATE_COPPER);

		//BlockEntities
		//blockWithItem(MainBlocks.GETE_FURNACE);


		// Variantes de bloques convencionales
		stairsBlock(((StairBlock) MainBlocks.NAMEK_AJISSA_STAIRS.get()), blockTexture(MainBlocks.NAMEK_AJISSA_PLANKS.get()));
		slabBlock(((SlabBlock) MainBlocks.NAMEK_AJISSA_SLAB.get()), blockTexture(MainBlocks.NAMEK_AJISSA_PLANKS.get()), blockTexture(MainBlocks.NAMEK_AJISSA_PLANKS.get()));
		buttonBlock(((ButtonBlock) MainBlocks.NAMEK_AJISSA_BUTTON.get()), blockTexture(MainBlocks.NAMEK_AJISSA_PLANKS.get()));
		pressurePlateBlock(((PressurePlateBlock) MainBlocks.NAMEK_AJISSA_PRESSURE_PLATE.get()), blockTexture(MainBlocks.NAMEK_AJISSA_PLANKS.get()));
		fenceBlock(((FenceBlock) MainBlocks.NAMEK_AJISSA_FENCE.get()), blockTexture(MainBlocks.NAMEK_AJISSA_PLANKS.get()));
		fenceGateBlock(((FenceGateBlock) MainBlocks.NAMEK_AJISSA_FENCE_GATE.get()), blockTexture(MainBlocks.NAMEK_AJISSA_PLANKS.get()));
		doorBlockWithRenderType(((DoorBlock) MainBlocks.NAMEK_AJISSA_DOOR.get()), modLoc("block/namek_ajissa_door_bottom"), modLoc("block/namek_ajissa_door_top"), "cutout");
		trapdoorBlockWithRenderType(((TrapDoorBlock) MainBlocks.NAMEK_AJISSA_TRAPDOOR.get()), modLoc("block/namek_ajissa_trapdoor"), true, "cutout");

		stairsBlock(((StairBlock) MainBlocks.NAMEK_SACRED_STAIRS.get()), blockTexture(MainBlocks.NAMEK_SACRED_PLANKS.get()));
		slabBlock(((SlabBlock) MainBlocks.NAMEK_SACRED_SLAB.get()), blockTexture(MainBlocks.NAMEK_SACRED_PLANKS.get()), blockTexture(MainBlocks.NAMEK_SACRED_PLANKS.get()));
		buttonBlock(((ButtonBlock) MainBlocks.NAMEK_SACRED_BUTTON.get()), blockTexture(MainBlocks.NAMEK_SACRED_PLANKS.get()));
		pressurePlateBlock(((PressurePlateBlock) MainBlocks.NAMEK_SACRED_PRESSURE_PLATE.get()), blockTexture(MainBlocks.NAMEK_SACRED_PLANKS.get()));
		fenceBlock(((FenceBlock) MainBlocks.NAMEK_SACRED_FENCE.get()), blockTexture(MainBlocks.NAMEK_SACRED_PLANKS.get()));
		fenceGateBlock(((FenceGateBlock) MainBlocks.NAMEK_SACRED_FENCE_GATE.get()), blockTexture(MainBlocks.NAMEK_SACRED_PLANKS.get()));
		doorBlockWithRenderType(((DoorBlock) MainBlocks.NAMEK_SACRED_DOOR.get()), modLoc("block/namek_sacred_door_bottom"), modLoc("block/namek_sacred_door_top"), "cutout");
		trapdoorBlockWithRenderType(((TrapDoorBlock) MainBlocks.NAMEK_SACRED_TRAPDOOR.get()), modLoc("block/namek_sacred_trapdoor"), true, "cutout");

		stairsBlock(((StairBlock) MainBlocks.NAMEK_STONE_STAIRS.get()), blockTexture(MainBlocks.NAMEK_STONE.get()));
		slabBlock(((SlabBlock) MainBlocks.NAMEK_STONE_SLAB.get()), blockTexture(MainBlocks.NAMEK_STONE.get()), blockTexture(MainBlocks.NAMEK_STONE.get()));
		wallBlock(((WallBlock) MainBlocks.NAMEK_STONE_WALL.get()), blockTexture(MainBlocks.NAMEK_STONE.get()));
		stairsBlock(((StairBlock) MainBlocks.NAMEK_COBBLESTONE_STAIRS.get()), blockTexture(MainBlocks.NAMEK_COBBLESTONE.get()));
		slabBlock(((SlabBlock) MainBlocks.NAMEK_COBBLESTONE_SLAB.get()), blockTexture(MainBlocks.NAMEK_COBBLESTONE.get()), blockTexture(MainBlocks.NAMEK_COBBLESTONE.get()));
		wallBlock(((WallBlock) MainBlocks.NAMEK_COBBLESTONE_WALL.get()), blockTexture(MainBlocks.NAMEK_COBBLESTONE.get()));
		stairsBlock(((StairBlock) MainBlocks.NAMEK_DEEPSLATE_STAIRS.get()), blockTexture(MainBlocks.NAMEK_DEEPSLATE.get()));
		slabBlock(((SlabBlock) MainBlocks.NAMEK_DEEPSLATE_SLAB.get()), blockTexture(MainBlocks.NAMEK_DEEPSLATE.get()), blockTexture(MainBlocks.NAMEK_DEEPSLATE.get()));
		wallBlock(((WallBlock) MainBlocks.NAMEK_DEEPSLATE_WALL.get()), blockTexture(MainBlocks.NAMEK_DEEPSLATE.get()));
		stairsBlock(((StairBlock) MainBlocks.ROCKY_STONE_STAIRS.get()), blockTexture(MainBlocks.ROCKY_STONE.get()));
		slabBlock(((SlabBlock) MainBlocks.ROCKY_STONE_SLAB.get()), blockTexture(MainBlocks.ROCKY_STONE.get()), blockTexture(MainBlocks.ROCKY_STONE.get()));
		wallBlock(((WallBlock) MainBlocks.ROCKY_STONE_WALL.get()), blockTexture(MainBlocks.ROCKY_STONE.get()));
		stairsBlock(((StairBlock) MainBlocks.ROCKY_COBBLESTONE_STAIRS.get()), blockTexture(MainBlocks.ROCKY_COBBLESTONE.get()));
		slabBlock(((SlabBlock) MainBlocks.ROCKY_COBBLESTONE_SLAB.get()), blockTexture(MainBlocks.ROCKY_COBBLESTONE.get()), blockTexture(MainBlocks.ROCKY_COBBLESTONE.get()));
		wallBlock(((WallBlock) MainBlocks.ROCKY_COBBLESTONE_WALL.get()), blockTexture(MainBlocks.ROCKY_COBBLESTONE.get()));
	}

	private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
		simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
	}
	
	private void grassBlock(RegistryObject<Block> blockRegistryObject) {
		String path = ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath();
		ResourceLocation bottom = new ResourceLocation(Reference.MOD_ID, "block/" + path + "_down");
		ResourceLocation top = new ResourceLocation(Reference.MOD_ID, "block/" + path + "_top");
		ResourceLocation side = new ResourceLocation(Reference.MOD_ID, "block/" + path + "_side");
		
		simpleBlockWithItem(blockRegistryObject.get(), 
			models().cubeBottomTop(path, side, bottom, top));
	}
	
	private void blockItem(RegistryObject<Block> blockRegistryObject) {
		simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile(Reference.MOD_ID +
				":block/" + ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath()));
	}
	private void leavesBlock(RegistryObject<Block> blockRegistryObject) {
		simpleBlockWithItem(blockRegistryObject.get(), models().singleTexture(ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath(),
				new ResourceLocation("minecraft:block/leaves"), "all", blockTexture(blockRegistryObject.get())).renderType("cutout"));
	}
	private void saplingBlock(RegistryObject<Block> blockRegistryObject) {
		simpleBlock(blockRegistryObject.get(),
				models().cross(ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath(), blockTexture(blockRegistryObject.get())).renderType("cutout"));
	}
}
