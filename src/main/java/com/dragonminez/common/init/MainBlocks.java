package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.init.block.custom.*;
import com.dragonminez.server.world.tree.NamekAjissaGrower;
import com.dragonminez.server.world.tree.NamekSacredGrower;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

@SuppressWarnings("unused")
public final class MainBlocks {

	public static final DeferredRegister<Block> BLOCK_REGISTER = DeferredRegister.create(BuiltInRegistries.BLOCK, Reference.MOD_ID);

	private static ToIntFunction<BlockState> litBlockEmission(int pLightValue) {
		return (IsThisOn) -> (Boolean) IsThisOn.getValue(BlockStateProperties.LIT) ? pLightValue : 0;
	}

	//BLOQUES
	public static final DeferredHolder<Block, ? extends Block> INVISIBLE_LADDER_BLOCK = registerBlock("invisible_ladder_block",
			() -> new ClimbableBlock(BlockBehaviour.Properties.of().noOcclusion().forceSolidOff()
					.strength(-1.0F, 3600000.0F).noLootTable()));
	public static final DeferredHolder<Block, ? extends Block> TIME_CHAMBER_BLOCK = registerBlock("time_chamber_block",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).strength(-1.0f,3600000.0F).sound(SoundType.BONE_BLOCK)));
	public static final DeferredHolder<Block, ? extends Block> OTHERWORLD_CLOUD = registerBlock("otherworld_cloud",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POWDER_SNOW).strength(-1.0f,3600000.0F).sound(SoundType.AZALEA)
					.noCollission().noLootTable()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_BLOCK = registerBlock("namek_block",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).sound(SoundType.BONE_BLOCK)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_GRASS_BLOCK = registerBlock("namek_grass_block",
			() -> new NamekGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).sound(SoundType.GRASS)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_GRASS_BLOCK = registerBlock("namek_sacred_grass_block",
			() -> new NamekSacredGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).sound(SoundType.GRASS)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DIRT = registerBlock("namek_dirt",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).sound(SoundType.ROOTED_DIRT)));
	public static final DeferredHolder<Block, ? extends Block> HELL_GROUND = registerBlock("hell_ground",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_PINK)
					.requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> HELL_STONE = registerBlock("hell_stone",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_PURPLE)
					.requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> HELL_DEEPSTONE = registerBlock("hell_deepstone",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.NETHER)
					.requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
	public static final DeferredHolder<Block, ? extends Block> MIGHT_TREE_WOOD = registerBlock("might_tree_wood",
			() -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PURPLE).instrument(NoteBlockInstrument.BASS)
					.strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.CHERRY_WOOD)
					.isValidSpawn((pState, pLevel, pPos, pType) -> false).pushReaction(PushReaction.BLOCK)));
	public static final DeferredHolder<Block, ? extends Block> MIGHT_TREE_ROOT = registerBlock("might_tree_root",
			() -> new MightTreeRootBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PURPLE)
					.strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().sound(SoundType.MANGROVE_ROOTS)
					.isValidSpawn((pState, pLevel, pPos, pType) -> false).pushReaction(PushReaction.BLOCK)));
	public static final DeferredHolder<Block, ? extends Block> MIGHT_TREE_LEAVES = registerBlock("might_tree_leaves",
			() -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN).strength(0.3F)
					.noLootTable().sound(SoundType.AZALEA_LEAVES)
					.isValidSpawn((pState, pLevel, pPos, pType) -> false)));
	public static final DeferredHolder<Block, ? extends Block> MIGHT_TREE_FRUIT_BLOCK = registerBlockOnly("might_tree_fruit_block",
			() -> new MightTreeFruitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.3F)
					.sound(SoundType.SHROOMLIGHT).lightLevel(pState -> 6).noOcclusion()
					.isValidSpawn((pState, pLevel, pPos, pType) -> false).pushReaction(PushReaction.DESTROY)));

	//Madera de Ajissa de Namek
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_SAPLING = registerBlock("namek_ajissa_sapling",
			() -> new NamekAjissaSaplingBlock(NamekAjissaGrower.INSTANCE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_LEAVES = registerBlock("namek_ajissa_leaves",
			() -> new FlammableLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).sound(SoundType.CHERRY_LEAVES)
					.isViewBlocking((pState, pReader, pPos) -> false).isSuffocating((pState, pReader, pPos) -> false)));
	public static final DeferredHolder<Block, ? extends Block> POTTED_AJISSA_SAPLING = registerBlock("potted_ajissa_sapling",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.NAMEK_AJISSA_SAPLING,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_LOG = registerBlock("namek_ajissa_log",
			() -> new NamekLogBlock());
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_WOOD = registerBlock("namek_ajissa_wood",
			() -> new FlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_STRIPPED_AJISSA_LOG = registerBlock("namek_stripped_ajissa_log",
			() -> new NamekStrippedLogBlock());
	public static final DeferredHolder<Block, ? extends Block> NAMEK_STRIPPED_AJISSA_WOOD = registerBlock("namek_stripped_ajissa_wood",
			() -> new FlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_PLANKS = registerBlock("namek_ajissa_planks",
			() -> new FlammableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_STAIRS = registerBlock("namek_ajissa_stairs",
			() -> new StairBlock(NAMEK_AJISSA_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_SLAB = registerBlock("namek_ajissa_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_FENCE = registerBlock("namek_ajissa_fence",
			() -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_FENCE_GATE = registerBlock("namek_ajissa_fence_gate",
			() -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_DOOR = registerBlock("namek_ajissa_door",
			() -> new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).strength(3.0F).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_TRAPDOOR = registerBlock("namek_ajissa_trapdoor",
			() -> new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).strength(3.0F).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_BUTTON = registerBlock("namek_ajissa_button",
			() -> new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_AJISSA_PRESSURE_PLATE = registerBlock("namek_ajissa_pressure_plate",
			() -> new PressurePlateBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).sound(SoundType.CHERRY_WOOD)));

	//Madera Sagrada de Namek
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_SAPLING = registerBlock("namek_sacred_sapling",
			() -> new SaplingBlock(NamekSacredGrower.INSTANCE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_LEAVES = registerBlock("namek_sacred_leaves",
			() -> new FlammableLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LEAVES).sound(SoundType.CHERRY_LEAVES)
					.isViewBlocking((pState, pReader, pPos) -> false).isSuffocating((pState, pReader, pPos) -> false)));
	public static final DeferredHolder<Block, ? extends Block> POTTED_SACRED_SAPLING = registerBlock("potted_sacred_sapling",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.NAMEK_SACRED_SAPLING,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_LOG = registerBlock("namek_sacred_log",
			() -> new NamekSacredLogBlock());
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_WOOD = registerBlock("namek_sacred_wood",
			() -> new FlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_WOOD).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_STRIPPED_SACRED_LOG = registerBlock("namek_stripped_sacred_log",
			() -> new NamekStrippedLogBlock());
	public static final DeferredHolder<Block, ? extends Block> NAMEK_STRIPPED_SACRED_WOOD = registerBlock("namek_stripped_sacred_wood",
			() -> new FlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_DARK_OAK_WOOD).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_PLANKS = registerBlock("namek_sacred_planks",
			() -> new FlammableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_STAIRS = registerBlock("namek_sacred_stairs",
			() -> new StairBlock(NAMEK_SACRED_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_STAIRS)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_SLAB = registerBlock("namek_sacred_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_SLAB)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_FENCE = registerBlock("namek_sacred_fence",
			() -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_FENCE_GATE = registerBlock("namek_sacred_fence_gate",
			() -> new FenceGateBlock(WoodType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_FENCE_GATE).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_DOOR = registerBlock("namek_sacred_door",
			() -> new DoorBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_DOOR).strength(3.0F).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_TRAPDOOR = registerBlock("namek_sacred_trapdoor",
			() -> new TrapDoorBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_TRAPDOOR).strength(3.0F).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_BUTTON = registerBlock("namek_sacred_button",
			() -> new ButtonBlock(BlockSetType.DARK_OAK, 30, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_BUTTON).sound(SoundType.CHERRY_WOOD)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_PRESSURE_PLATE = registerBlock("namek_sacred_pressure_plate",
			() -> new PressurePlateBlock(BlockSetType.DARK_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PRESSURE_PLATE).sound(SoundType.CHERRY_WOOD)));

	//Ores Nuevos
	public static final DeferredHolder<Block, ? extends Block> GETE_BLOCK = registerBlock("gete_block",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)));
	public static final DeferredHolder<Block, ? extends Block> GETE_ORE = registerBlock("gete_debris_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.ANCIENT_DEBRIS).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> GETE_STAR_HULL = registerBlock("gete_star_hull",
			() -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
					.strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK)
					.isValidSpawn((pState, pLevel, pPos, pType) -> false).pushReaction(PushReaction.BLOCK)));
	public static final DeferredHolder<Block, ? extends Block> GETE_STAR_CIRCUIT = registerBlock("gete_star_circuit",
			() -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
					.strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 12)
					.emissiveRendering((pState, pLevel, pPos) -> true)
					.isValidSpawn((pState, pLevel, pPos, pType) -> false).pushReaction(PushReaction.BLOCK)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_KIKONO_ORE = registerBlock("namek_kikono_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.ANCIENT_DEBRIS).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> KIKONO_BLOCK = registerBlock("kikono_block",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)));

	//FIN DE ITEMS SIN NECESIDADES ESPECIALES

	public static final DeferredHolder<Block, ? extends Block> NAMEK_STONE = registerBlock("namek_stone",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).requiresCorrectToolForDrops().sound(SoundType.STONE)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_STONE_SLAB = registerBlock("namek_stone_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_STONE_STAIRS = registerBlock("namek_stone_stairs",
			() -> new StairBlock(NAMEK_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_STONE_WALL = registerBlock("namek_stone_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_COBBLESTONE = registerBlock("namek_cobblestone",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).requiresCorrectToolForDrops().sound(SoundType.STONE)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_COBBLESTONE_SLAB = registerBlock("namek_cobblestone_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_SLAB).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_COBBLESTONE_STAIRS = registerBlock("namek_cobblestone_stairs",
			() -> new StairBlock(NAMEK_COBBLESTONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_STAIRS).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_COBBLESTONE_WALL = registerBlock("namek_cobblestone_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE = registerBlock("namek_deepslate",
			() -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_SLAB = registerBlock("namek_deepslate_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_SLAB).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_STAIRS = registerBlock("namek_deepslate_stairs",
			() -> new StairBlock(NAMEK_DEEPSLATE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_STAIRS).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_WALL = registerBlock("namek_deepslate_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_WALL).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_DIRT = registerBlock("rocky_dirt",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).sound(SoundType.GRAVEL)));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_STONE = registerBlock("rocky_stone",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_STONE_SLAB = registerBlock("rocky_stone_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_STONE_STAIRS = registerBlock("rocky_stone_stairs",
			() -> new StairBlock(ROCKY_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_STONE_WALL = registerBlock("rocky_stone_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_COBBLESTONE = registerBlock("rocky_cobblestone",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_COBBLESTONE_SLAB = registerBlock("rocky_cobblestone_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_SLAB).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_COBBLESTONE_STAIRS = registerBlock("rocky_cobblestone_stairs",
			() -> new StairBlock(ROCKY_COBBLESTONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_STAIRS).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> ROCKY_COBBLESTONE_WALL = registerBlock("rocky_cobblestone_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> SACRED_PLANET_GRASS_BLOCK = registerBlock("sacred_planet_grass_block",
			() -> new SacredPlanetGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).sound(SoundType.GRASS)));

	//Reino Demoníaco
	public static final DeferredHolder<Block, ? extends Block> MAKAI_STONE = registerBlock("makai_stone",
			() -> new DemonRealmBlocks.SkylitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_STONE_SLAB = registerBlock("makai_stone_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB).mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_STONE_STAIRS = registerBlock("makai_stone_stairs",
			() -> new StairBlock(MAKAI_STONE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_STONE_WALL = registerBlock("makai_stone_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL).mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_STRATA_STONE = registerBlock("makai_strata_stone",
			() -> new DemonRealmBlocks.SkylitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_GRASS_BLOCK = registerBlock("makai_grass_block",
			() -> new DemonRealmBlocks.SkylitSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.GRASS).strength(0.6F).sound(SoundType.GRASS)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_DIRT = registerBlock("makai_dirt",
			() -> new DemonRealmBlocks.SkylitSoilBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).sound(SoundType.ROOTED_DIRT)));
	public static final DeferredHolder<Block, ? extends Block> LILAC_SAND = registerBlock("lilac_sand",
			() -> new DemonRealmBlocks.SkylitSandBlock(0xCFA9C9, BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_PINK)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_SAND = registerBlock("makai_sand",
			() -> new DemonRealmBlocks.SkylitSandBlock(0xD7C381, BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.SAND)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_SHRUB = registerBlock("makai_shrub",
			() -> new DemonRealmBlocks.MakaiShrubBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FERN).mapColor(MapColor.COLOR_CYAN)));
	public static final DeferredHolder<Block, ? extends Block> MAKAI_BUSH = registerBlock("makai_bush",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES).mapColor(MapColor.COLOR_CYAN)
					.isViewBlocking((pState, pLevel, pPos) -> false).isSuffocating((pState, pLevel, pPos) -> false)));
	public static final DeferredHolder<Block, ? extends Block> ADOBE = registerBlock("adobe",
			() -> new DemonRealmBlocks.SkylitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICKS).mapColor(MapColor.COLOR_ORANGE)));
	public static final DeferredHolder<Block, ? extends Block> ADOBE_SLAB = registerBlock("adobe_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICK_SLAB).mapColor(MapColor.COLOR_ORANGE)));
	public static final DeferredHolder<Block, ? extends Block> ADOBE_STAIRS = registerBlock("adobe_stairs",
			() -> new StairBlock(ADOBE.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICK_STAIRS).mapColor(MapColor.COLOR_ORANGE)));
	public static final DeferredHolder<Block, ? extends Block> ADOBE_WALL = registerBlock("adobe_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICK_WALL).mapColor(MapColor.COLOR_ORANGE)));
	public static final DeferredHolder<Block, ? extends Block> ADOBE_BRICKS = registerBlock("adobe_bricks",
			() -> new DemonRealmBlocks.SkylitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MUD_BRICKS).mapColor(MapColor.TERRACOTTA_ORANGE)));
	public static final DeferredHolder<Block, ? extends Block> GAS_VENT = registerBlock("gas_vent",
			() -> new DemonRealmBlocks.GasVentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
	public static final DeferredHolder<Block, ? extends Block> DARK_SEA_CLOUD = registerBlock("dark_sea_cloud",
			() -> new DemonRealmBlocks.DarkSeaCloudBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POWDER_SNOW).mapColor(MapColor.COLOR_PURPLE)
					.strength(-1.0F, 3600000.0F).sound(SoundType.AZALEA).noCollission().noLootTable()
					.isValidSpawn((pState, pLevel, pPos, pType) -> false)));
	public static final DeferredHolder<Block, ? extends Block> TURQUOISE_ROCK = registerBlock("turquoise_rock",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> MAJILITE_ORE = registerBlock("majilite_ore",
			() -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).mapColor(MapColor.COLOR_CYAN).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> DEMON_ROCK = registerBlock("demon_rock",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE).mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_HORN = registerBlock("demon_horn",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE).mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_HORN_GLOW = registerBlock("demon_horn_glow",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE).mapColor(MapColor.COLOR_MAGENTA).lightLevel(pState -> 12)
					.emissiveRendering((pState, pLevel, pPos) -> true).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_BRICKS = registerBlock("demon_bricks",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICKS).mapColor(MapColor.NETHER).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_BRICK_SLAB = registerBlock("demon_brick_slab",
			() -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_SLAB).mapColor(MapColor.NETHER).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_BRICK_STAIRS = registerBlock("demon_brick_stairs",
			() -> new StairBlock(DEMON_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_STAIRS).mapColor(MapColor.NETHER).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_BRICK_WALL = registerBlock("demon_brick_wall",
			() -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICK_WALL).mapColor(MapColor.NETHER).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_MAGMA_ROCK = registerBlock("demon_magma_rock",
			() -> new MagmaBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MAGMA_BLOCK).mapColor(MapColor.NETHER).lightLevel(pState -> 6)));
	public static final DeferredHolder<Block, ? extends Block> RED_ASH = registerBlock("red_ash",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_SOIL).mapColor(MapColor.TERRACOTTA_RED).sound(SoundType.SAND)));
	public static final DeferredHolder<Block, ? extends Block> CRIMSON_CRYSTAL = registerBlock("crimson_crystal",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.COLOR_RED).lightLevel(pState -> 10).requiresCorrectToolForDrops()));
	public static final DeferredHolder<Block, ? extends Block> DEMON_REALM_CRUST = registerBlock("demon_realm_crust",
			() -> new DemonRealmBlocks.SkylitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).mapColor(MapColor.COLOR_GRAY).noLootTable()));
	public static final DeferredHolder<Block, ? extends Block> LIGHT_SHIELD = registerBlock("light_shield",
			() -> new DemonRealmBlocks.LightShieldBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
					.strength(-1.0F, 3600000.0F).noCollission().noOcclusion().noLootTable()
					.emissiveRendering((pState, pLevel, pPos) -> true)
					.sound(SoundType.AMETHYST).pushReaction(PushReaction.BLOCK)
					.isValidSpawn((pState, pLevel, pPos, pType) -> false)
					.isViewBlocking((pState, pLevel, pPos) -> false)
					.isSuffocating((pState, pLevel, pPos) -> false)));

	//Ores (Default) de Namek
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DIAMOND_ORE = registerBlock("namek_diamond_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_GOLD_ORE = registerBlock("namek_gold_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_IRON_ORE = registerBlock("namek_iron_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_LAPIS_ORE = registerBlock("namek_lapis_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_REDSTONE_ORE = registerBlock("namek_redstone_ore",
			() -> new RedStoneOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_COAL_ORE = registerBlock("namek_coal_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_EMERALD_ORE = registerBlock("namek_emerald_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_COPPER_ORE = registerBlock("namek_copper_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));

	//Ores (Default) de Deepslate de Namek
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_DIAMOND = registerBlock("namek_deepslate_diamond_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_GOLD = registerBlock("namek_deepslate_gold_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_GOLD_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_IRON = registerBlock("namek_deepslate_iron_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_LAPIS = registerBlock("namek_deepslate_lapis_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_LAPIS_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_REDSTONE = registerBlock("namek_deepslate_redstone_ore",
			() -> new RedStoneOreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_REDSTONE_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_COAL = registerBlock("namek_deepslate_coal_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_COAL_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_EMERALD = registerBlock("namek_deepslate_emerald_ore",
			() -> new DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_EMERALD_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_DEEPSLATE_COPPER = registerBlock("namek_deepslate_copper_ore",
			() -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_COPPER_ORE).requiresCorrectToolForDrops().strength(3.0F, 6.0F)));

	//Bloques Especiales
	public static final DeferredHolder<Block, ? extends Block> TIME_CHAMBER_PORTAL = registerBlock("time_chamber_portal",
			TimeChamberPortalBlock::new);
	//public static final DeferredHolder<Block, ? extends Block> GETE_FURNACE = registerBlock("gete_furnace",
	//        () -> new GeteFurnaceBlock(BlockBehaviour.Properties.of()
	//                .mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.5F)
	//                .lightLevel(litBlockEmission(13))));

	public static final DeferredHolder<Block, ? extends Block> KIKONO_STATION = registerBlock("kikono_station",
			() -> new KikonoStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE)
					.mapColor(MapColor.STONE).requiresCorrectToolForDrops().noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> FUEL_GENERATOR = registerBlock("fuel_generator",
			() -> new FuelGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE)
					.mapColor(MapColor.STONE).requiresCorrectToolForDrops().noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> ENERGY_CABLE = registerBlock("energy_cable",
			() -> new EnergyCableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
					.mapColor(MapColor.STONE).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> PATTERN_STATION = registerPatternStation();
	public static final DeferredHolder<Block, ? extends Block> GRAVITY_DEVICE = registerGravityDevice();

	private static DeferredHolder<Block, ? extends Block> registerPatternStation() {
		DeferredHolder<Block, ? extends Block> block = BLOCK_REGISTER.register("pattern_station",
				() -> new PatternStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
						.mapColor(MapColor.QUARTZ).noOcclusion()));
		MainItems.ITEM_REGISTER.register("pattern_station",
				() -> new com.dragonminez.common.init.item.PatternStationItem(block.get(), new Item.Properties()));
		return block;
	}

	private static DeferredHolder<Block, ? extends Block> registerGravityDevice() {
		DeferredHolder<Block, ? extends Block> block = BLOCK_REGISTER.register("gravity_device",
				() -> new GravityDeviceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
						.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).noOcclusion()));
		MainItems.ITEM_REGISTER.register("gravity_device",
				() -> new com.dragonminez.common.init.item.GravityDeviceItem(block.get(), new Item.Properties()));
		return block;
	}

	//Plantas Namek 1
	public static final DeferredHolder<Block, ? extends Block> NAMEK_GRASS = registerBlock("namek_grass",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> CHRYSANTHEMUM_FLOWER = registerBlock("chrysanthemum_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_CHRYSANTHEMUM_FLOWER = registerBlock("potted_chrysanthemum_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.CHRYSANTHEMUM_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> AMARYLLIS_FLOWER = registerBlock("amaryllis_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_AMARYLLIS_FLOWER = registerBlock("potted_amaryllis_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.AMARYLLIS_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> MARIGOLD_FLOWER = registerBlock("marigold_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_MARIGOLD_FLOWER = registerBlock("potted_marigold_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.MARIGOLD_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_BLUE_ORCHID).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> CATHARANTHUS_ROSEUS_FLOWER = registerBlock("catharanthus_roseus_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_CATHARANTHUS_ROSEUS_FLOWER = registerBlock("potted_catharanthus_roseus_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.CATHARANTHUS_ROSEUS_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ALLIUM).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> TRILLIUM_FLOWER = registerBlock("trillium_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.AZURE_BLUET)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_TRILLIUM_FLOWER = registerBlock("potted_trillium_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.TRILLIUM_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_AZURE_BLUET).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_FERN = registerBlock("namek_fern",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.FERN)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_NAMEK_FERN = registerBlock("potted_namek_fern",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.NAMEK_FERN,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_FERN).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> LOTUS_FLOWER = registerBlock("lotus_flower",
			() -> new NamekWaterlilyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_PAD)));

	//Plantas Namek Sacred
	public static final DeferredHolder<Block, ? extends Block> NAMEK_SACRED_GRASS = registerBlock("namek_sacred_grass",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> SACRED_CHRYSANTHEMUM_FLOWER = registerBlock("sacred_chrysanthemum_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_SACRED_CHRYSANTHEMUM_FLOWER = registerBlock("potted_sacred_chrysanthemum_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.SACRED_CHRYSANTHEMUM_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_DANDELION).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> SACRED_AMARYLLIS_FLOWER = registerBlock("sacred_amaryllis_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_SACRED_AMARYLLIS_FLOWER = registerBlock("potted_sacred_amaryllis_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.SACRED_AMARYLLIS_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> SACRED_MARIGOLD_FLOWER = registerBlock("sacred_marigold_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_SACRED_MARIGOLD_FLOWER = registerBlock("potted_sacred_marigold_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.SACRED_MARIGOLD_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_BLUE_ORCHID).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> SACRED_CATHARANTHUS_ROSEUS_FLOWER = registerBlock("sacred_catharanthus_roseus_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_SACRED_CATHARANTHUS_ROSEUS_FLOWER = registerBlock("potted_sacred_catharanthus_roseus_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.SACRED_CATHARANTHUS_ROSEUS_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ALLIUM).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> SACRED_TRILLIUM_FLOWER = registerBlock("sacred_trillium_flower",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.AZURE_BLUET)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_SACRED_TRILLIUM_FLOWER = registerBlock("potted_sacred_trillium_flower",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.SACRED_TRILLIUM_FLOWER,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_AZURE_BLUET).noOcclusion()));
	public static final DeferredHolder<Block, ? extends Block> SACRED_FERN = registerBlock("sacred_fern",
			() -> new NamekPlantsBlock(MobEffects.LUCK, 5 / 20.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.FERN)
					.noOcclusion().noCollission()));
	public static final DeferredHolder<Block, ? extends Block> POTTED_SACRED_FERN = registerBlock("potted_sacred_fern",
			() -> new FlowerPotBlock(() -> ((FlowerPotBlock) Blocks.FLOWER_POT), MainBlocks.SACRED_FERN,
					BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_FERN).noOcclusion()));

	//AGRICULTURA
	public static final DeferredHolder<Block, ? extends Block> OOZARU_ROOT_CROP = registerBlockOnly("oozaru_root_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.OOZARU_ROOT));
	public static final DeferredHolder<Block, ? extends Block> KATCHIN_SPROUT_CROP = registerBlockOnly("katchin_sprout_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.KATCHIN_SPROUT));
	public static final DeferredHolder<Block, ? extends Block> METEOR_FLOWER_CROP = registerBlockOnly("meteor_flower_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.METEOR_FLOWER));
	public static final DeferredHolder<Block, ? extends Block> AURA_LILY_CROP = registerBlockOnly("aura_lily_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.AURA_LILY));
	public static final DeferredHolder<Block, ? extends Block> HERMIT_FERN_CROP = registerBlockOnly("hermit_fern_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.HERMIT_FERN));
	public static final DeferredHolder<Block, ? extends Block> KAIOSHIN_FRUIT_CROP = registerBlockOnly("kaioshin_fruit_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.KAIOSHIN_FRUIT));
	public static final DeferredHolder<Block, ? extends Block> ZENKAI_LOTUS_CROP = registerBlockOnly("zenkai_lotus_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.ZENKAI_LOTUS));
	public static final DeferredHolder<Block, ? extends Block> NAMEK_MOSS_CROP = registerBlockOnly("namek_moss_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.NAMEK_MOSS, NAMEK_GRASS_BLOCK));

	public static final DeferredHolder<Block, ? extends Block> SENZU_BLUE_CROP = registerBlockOnly("senzu_blue_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.SENZU_BEAN_SEEDS_BLUE));
	public static final DeferredHolder<Block, ? extends Block> SENZU_RED_CROP = registerBlockOnly("senzu_red_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.SENZU_BEAN_SEEDS_RED));
	public static final DeferredHolder<Block, ? extends Block> SENZU_YELLOW_CROP = registerBlockOnly("senzu_yellow_crop",
			() -> new StatCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT), MainItems.SENZU_BEAN_SEEDS_YELLOW));

	public static final java.util.List<DeferredHolder<Block, ? extends Block>> STAT_CROPS = java.util.List.of(
			OOZARU_ROOT_CROP, KATCHIN_SPROUT_CROP, METEOR_FLOWER_CROP, AURA_LILY_CROP,
			HERMIT_FERN_CROP, KAIOSHIN_FRUIT_CROP, ZENKAI_LOTUS_CROP, NAMEK_MOSS_CROP,
			SENZU_BLUE_CROP, SENZU_RED_CROP, SENZU_YELLOW_CROP);

	//LIQUIDOS
	public static final DeferredHolder<Block, LiquidBlock> HEALING_LIQUID = BLOCK_REGISTER.register("healing_liquid_block",
			() -> new LiquidBlock(MainFluids.SOURCE_HEALING.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable().liquid().replaceable()));

    public static final DeferredHolder<Block, LiquidBlock> NAMEK_WATER_LIQUID = BLOCK_REGISTER.register("namek_water_liquid_block",
            () -> new LiquidBlock(MainFluids.SOURCE_NAMEK.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .noCollission()
                    .noLootTable()
                    .liquid()
                    .replaceable()
            ));

	private static final Map<String, Map<Integer, DeferredHolder<Block, ? extends Block>>> DRAGON_BALL_BLOCKS = registerDragonBallBlocks();

	public static final DeferredHolder<Block, ? extends Block> DBALL1_BLOCK = getDragonBallBlockOrThrow("earth", 1);
	public static final DeferredHolder<Block, ? extends Block> DBALL2_BLOCK = getDragonBallBlockOrThrow("earth", 2);
	public static final DeferredHolder<Block, ? extends Block> DBALL3_BLOCK = getDragonBallBlockOrThrow("earth", 3);
	public static final DeferredHolder<Block, ? extends Block> DBALL4_BLOCK = getDragonBallBlockOrThrow("earth", 4);
	public static final DeferredHolder<Block, ? extends Block> DBALL5_BLOCK = getDragonBallBlockOrThrow("earth", 5);
	public static final DeferredHolder<Block, ? extends Block> DBALL6_BLOCK = getDragonBallBlockOrThrow("earth", 6);
	public static final DeferredHolder<Block, ? extends Block> DBALL7_BLOCK = getDragonBallBlockOrThrow("earth", 7);

	public static final DeferredHolder<Block, ? extends Block> DBALL1_NAMEK_BLOCK = getDragonBallBlockOrThrow("namek", 1);
	public static final DeferredHolder<Block, ? extends Block> DBALL2_NAMEK_BLOCK = getDragonBallBlockOrThrow("namek", 2);
	public static final DeferredHolder<Block, ? extends Block> DBALL3_NAMEK_BLOCK = getDragonBallBlockOrThrow("namek", 3);
	public static final DeferredHolder<Block, ? extends Block> DBALL4_NAMEK_BLOCK = getDragonBallBlockOrThrow("namek", 4);
	public static final DeferredHolder<Block, ? extends Block> DBALL5_NAMEK_BLOCK = getDragonBallBlockOrThrow("namek", 5);
	public static final DeferredHolder<Block, ? extends Block> DBALL6_NAMEK_BLOCK = getDragonBallBlockOrThrow("namek", 6);
	public static final DeferredHolder<Block, ? extends Block> DBALL7_NAMEK_BLOCK = getDragonBallBlockOrThrow("namek", 7);

	private static Map<String, Map<Integer, DeferredHolder<Block, ? extends Block>>> registerDragonBallBlocks() {
		Map<String, Map<Integer, DeferredHolder<Block, ? extends Block>>> registered = new LinkedHashMap<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBootstrapBallSets()) {
			Map<Integer, DeferredHolder<Block, ? extends Block>> setBlocks = new LinkedHashMap<>();
			for (Map.Entry<Integer, String> entry : definition.getBlockRegistryNamesByStar().entrySet()) {
				int star = entry.getKey();
				String registryName = entry.getValue();
				DeferredHolder<Block, ? extends Block> block = registerBlockOnly(registryName,
						() -> new DragonBallBlock(createDragonBallProperties(definition), dragonBallTypeFromStar(star), definition.getId()));
				definition.setRegisteredBlock(star, block);
				setBlocks.put(star, block);
			}
			registered.put(definition.getId(), Map.copyOf(setBlocks));
		}
		return Map.copyOf(registered);
	}

	private static BlockBehaviour.Properties createDragonBallProperties(DragonBallSetDefinition definition) {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO)
				.strength(0.35F)
				.explosionResistance(3600000.0F)
				.noOcclusion()
				.sound(DragonBallBlock.SOUND)
				.lightLevel(state -> state.hasProperty(DragonBallBlock.GATHERED) && state.getValue(DragonBallBlock.GATHERED) ? 15 : 7);
	}

	private static DragonBallType dragonBallTypeFromStar(int star) {
		return switch (star) {
			case 1 -> DragonBallType.ONE_STAR;
			case 2 -> DragonBallType.TWO_STAR;
			case 3 -> DragonBallType.THREE_STAR;
			case 4 -> DragonBallType.FOUR_STAR;
			case 5 -> DragonBallType.FIVE_STAR;
			case 6 -> DragonBallType.SIX_STAR;
			case 7 -> DragonBallType.SEVEN_STAR;
			default -> throw new IllegalArgumentException("Unsupported dragon ball star count: " + star);
		};
	}

	public static DeferredHolder<Block, ? extends Block> getDragonBallBlockOrThrow(String setId, int star) {
		Map<Integer, DeferredHolder<Block, ? extends Block>> setBlocks = DRAGON_BALL_BLOCKS.get(setId);
		if (setBlocks == null || !setBlocks.containsKey(star)) {
			throw new IllegalArgumentException("No dragon ball block registered for set '" + setId + "' star " + star);
		}
		return setBlocks.get(star);
	}

	public static Map<Integer, DeferredHolder<Block, ? extends Block>> getDragonBallBlocks(String setId) {
		Map<Integer, DeferredHolder<Block, ? extends Block>> setBlocks = DRAGON_BALL_BLOCKS.get(setId);
		return setBlocks == null ? Map.of() : setBlocks;
	}

	private static DeferredHolder<Block, ? extends Block> registerBlock(String name, Supplier<Block> supplier) {
		DeferredHolder<Block, ? extends Block> registeredObject = BLOCK_REGISTER.register(name, supplier);
		MainItems.ITEM_REGISTER.register(name, () -> new BlockItem(registeredObject.get(), new Item.Properties()));
		return registeredObject;
	}

	private static DeferredHolder<Block, ? extends Block> registerBlockOnly(String name, Supplier<Block> supplier) {
		return BLOCK_REGISTER.register(name, supplier);
	}

	public static void register(IEventBus bus) {
		BLOCK_REGISTER.register(bus);
	}
}
