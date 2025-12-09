package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.common.init.block.entity.DragonBallBlockEntity;
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

	public static void register(IEventBus bus) {
		BLOCK_ENTITY_TYPES_REGISTER.register(bus);
	}
}

