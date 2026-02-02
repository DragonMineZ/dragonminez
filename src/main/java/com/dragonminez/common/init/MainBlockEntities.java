package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.common.init.block.entity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MainBlockEntities {

	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES_REGISTER =
			DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Reference.MOD_ID);

	public static final RegistryObject<BlockEntityType<DragonBallBlockEntity>> DRAGON_BALL_BLOCK_ENTITY =
			BLOCK_ENTITY_TYPES_REGISTER.register("dragon_ball", () ->
					BlockEntityType.Builder.of((pos, state) -> {
								DragonBallBlock block = (DragonBallBlock) state.getBlock();
								return new DragonBallBlockEntity(pos, state, block.getBallType(), block.isNamekian());
							},
							MainBlocks.DBALL1_BLOCK.get(),
							MainBlocks.DBALL2_BLOCK.get(),
							MainBlocks.DBALL3_BLOCK.get(),
							MainBlocks.DBALL4_BLOCK.get(),
							MainBlocks.DBALL5_BLOCK.get(),
							MainBlocks.DBALL6_BLOCK.get(),
							MainBlocks.DBALL7_BLOCK.get(),
							MainBlocks.DBALL1_NAMEK_BLOCK.get(),
							MainBlocks.DBALL2_NAMEK_BLOCK.get(),
							MainBlocks.DBALL3_NAMEK_BLOCK.get(),
							MainBlocks.DBALL4_NAMEK_BLOCK.get(),
							MainBlocks.DBALL5_NAMEK_BLOCK.get(),
							MainBlocks.DBALL6_NAMEK_BLOCK.get(),
							MainBlocks.DBALL7_NAMEK_BLOCK.get()
					).build(null));

	public static final RegistryObject<BlockEntityType<KikonoStationBlockEntity>> KIKONO_STATION_BE =
			BLOCK_ENTITY_TYPES_REGISTER.register("kikono_station", () ->
					BlockEntityType.Builder.of(KikonoStationBlockEntity::new,
							MainBlocks.KIKONO_STATION.get()
					).build(null));

	public static final RegistryObject<BlockEntityType<FuelGeneratorBlockEntity>> FUEL_GENERATOR_BE =
			BLOCK_ENTITY_TYPES_REGISTER.register("fuel_generator", () ->
					BlockEntityType.Builder.of(FuelGeneratorBlockEntity::new,
							MainBlocks.FUEL_GENERATOR.get()
					).build(null));

	public static final RegistryObject<BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE_BE =
			BLOCK_ENTITY_TYPES_REGISTER.register("energy_cable", () ->
					BlockEntityType.Builder.of(EnergyCableBlockEntity::new,
							MainBlocks.ENERGY_CABLE.get()
					).build(null));

	public static final RegistryObject<BlockEntityType<TimeChamberPortalBlockEntity>> TIME_CHAMBER_PORTAL =
			BLOCK_ENTITY_TYPES_REGISTER.register("time_chamber_portal", () ->
					BlockEntityType.Builder.of(TimeChamberPortalBlockEntity::new,
							MainBlocks.TIME_CHAMBER_PORTAL.get()
					).build(null));

	public static void register(IEventBus bus) {
		BLOCK_ENTITY_TYPES_REGISTER.register(bus);
	}
}

