package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.fluid.SimpleFluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MainFluids {

	public static final DeferredRegister<FluidType> FLUID_TYPE_REGISTER =
			DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Reference.MOD_ID);

	public static final DeferredRegister<Fluid> FLUIDS_REGISTER =
			DeferredRegister.create(ForgeRegistries.FLUIDS, Reference.MOD_ID);

	public static final RegistryObject<FluidType> HEALING_FLUID_TYPE = FLUID_TYPE_REGISTER.register("healing_fluid_type",
			() -> new SimpleFluid(
					0xa4c977,
					FluidType.Properties.create().lightLevel(5).canDrown(false)));

	public static final RegistryObject<FlowingFluid> SOURCE_HEALING = FLUIDS_REGISTER.register("healing_fluid",
			() -> new ForgeFlowingFluid.Source(MainFluids.HEALING_FLUID_PROPERTIES));

	public static final RegistryObject<FlowingFluid> FLOWING_HEALING = FLUIDS_REGISTER.register("flowing_healing_fluid",
			() -> new ForgeFlowingFluid.Flowing(MainFluids.HEALING_FLUID_PROPERTIES));

	public static final ForgeFlowingFluid.Properties HEALING_FLUID_PROPERTIES
			= new ForgeFlowingFluid.Properties(MainFluids.HEALING_FLUID_TYPE, SOURCE_HEALING, FLOWING_HEALING)
			.block(MainBlocks.HEALING_LIQUID)
			.bucket(MainItems.HEALING_BUCKET);

	public static final RegistryObject<FluidType> NAMEK_FLUID_TYPE = FLUID_TYPE_REGISTER.register("namek_water_fluid_type",
			() -> new SimpleFluid(
					0xaef359,
					FluidType.Properties.create().lightLevel(5).density(1000).viscosity(1000)));

	public static final RegistryObject<FlowingFluid> SOURCE_NAMEK = FLUIDS_REGISTER.register("namek_water_fluid",
			() -> new ForgeFlowingFluid.Source(MainFluids.NAMEK_FLUID_PROPERTIES));

	public static final RegistryObject<FlowingFluid> FLOWING_NAMEK = FLUIDS_REGISTER.register("flowing_namek_water_fluid",
			() -> new ForgeFlowingFluid.Flowing(MainFluids.NAMEK_FLUID_PROPERTIES));

	public static final ForgeFlowingFluid.Properties NAMEK_FLUID_PROPERTIES
			= new ForgeFlowingFluid.Properties(MainFluids.NAMEK_FLUID_TYPE, SOURCE_NAMEK, FLOWING_NAMEK)
			.block(MainBlocks.NAMEK_WATER_LIQUID)
			.bucket(MainItems.NAMEK_WATER_BUCKET);

	public static void register(IEventBus bus) {
		FLUID_TYPE_REGISTER.register(bus);
		FLUIDS_REGISTER.register(bus);
	}
}
