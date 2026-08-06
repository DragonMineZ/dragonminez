package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.dragonball.DragonBallDefinitions;
import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.common.init.block.entity.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

public final class MainBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES_REGISTER =
			DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Reference.MOD_ID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DragonBallBlockEntity>> DRAGON_BALL_BLOCK_ENTITY =
			BLOCK_ENTITY_TYPES_REGISTER.register("dragon_ball", () ->
					BlockEntityType.Builder.of((pos, state) -> {
						DragonBallBlock block = (DragonBallBlock) state.getBlock();
						return new DragonBallBlockEntity(pos, state, block.getBallType(), block.getBallSetId());
					}, getAllDragonBallBlocks()).build(null));

	private static Block[] getAllDragonBallBlocks() {
		List<Block> blocks = new ArrayList<>();
		for (DragonBallSetDefinition definition : DragonBallDefinitions.getBallSets()) {
			for (int star : definition.getStars()) {
				Block block = definition.getBlockForStar(star);
				if (block != null) blocks.add(block);
			}
		}
		return blocks.toArray(Block[]::new);
	}

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KikonoStationBlockEntity>> KIKONO_STATION_BE =
			BLOCK_ENTITY_TYPES_REGISTER.register("kikono_station", () ->
					BlockEntityType.Builder.of(KikonoStationBlockEntity::new,
							MainBlocks.KIKONO_STATION.get()
					).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelGeneratorBlockEntity>> FUEL_GENERATOR_BE =
			BLOCK_ENTITY_TYPES_REGISTER.register("fuel_generator", () ->
					BlockEntityType.Builder.of(FuelGeneratorBlockEntity::new,
							MainBlocks.FUEL_GENERATOR.get()
					).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravityDeviceBlockEntity>> GRAVITY_DEVICE_BE =
			BLOCK_ENTITY_TYPES_REGISTER.register("gravity_device", () ->
					BlockEntityType.Builder.of(GravityDeviceBlockEntity::new,
							MainBlocks.GRAVITY_DEVICE.get()
					).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE_BE =
			BLOCK_ENTITY_TYPES_REGISTER.register("energy_cable", () ->
					BlockEntityType.Builder.of(EnergyCableBlockEntity::new,
							MainBlocks.ENERGY_CABLE.get()
					).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TimeChamberPortalBlockEntity>> TIME_CHAMBER_PORTAL =
			BLOCK_ENTITY_TYPES_REGISTER.register("time_chamber_portal", () ->
					BlockEntityType.Builder.of(TimeChamberPortalBlockEntity::new,
							MainBlocks.TIME_CHAMBER_PORTAL.get()
					).build(null));

	public static void register(IEventBus bus) {
		BLOCK_ENTITY_TYPES_REGISTER.register(bus);
	}
}
