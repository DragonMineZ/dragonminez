package com.yuseix.dragonminez.init.blocks.entity;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.MainBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public interface ModBlockEntities {

    DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DragonMineZ.MOD_ID);


    RegistryObject<BlockEntityType<Dball1BlockEntity>> DBALL1_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("dball1_block_entity", () ->
                    BlockEntityType.Builder.of(Dball1BlockEntity::new, MainBlocks.DBALL1_BLOCK.get())
                            .build(null));
    RegistryObject<BlockEntityType<Dball2BlockEntity>> DBALL2_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("dball2_block_entity", () ->
                    BlockEntityType.Builder.of(Dball2BlockEntity::new, MainBlocks.DBALL2_BLOCK.get())
                            .build(null));
    RegistryObject<BlockEntityType<Dball3BlockEntity>> DBALL3_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("dball3_block_entity", () ->
                    BlockEntityType.Builder.of(Dball3BlockEntity::new, MainBlocks.DBALL3_BLOCK.get())
                            .build(null));
    RegistryObject<BlockEntityType<Dball4BlockEntity>> DBALL4_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("dball4_block_entity", () ->
                    BlockEntityType.Builder.of(Dball4BlockEntity::new, MainBlocks.DBALL4_BLOCK.get())
                            .build(null));
    RegistryObject<BlockEntityType<Dball5BlockEntity>> DBALL5_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("dball5_block_entity", () ->
                    BlockEntityType.Builder.of(Dball5BlockEntity::new, MainBlocks.DBALL5_BLOCK.get())
                            .build(null));
    RegistryObject<BlockEntityType<Dball6BlockEntity>> DBALL6_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("dball6_block_entity", () ->
                    BlockEntityType.Builder.of(Dball6BlockEntity::new, MainBlocks.DBALL6_BLOCK.get())
                            .build(null));
    RegistryObject<BlockEntityType<Dball7BlockEntity>> DBALL7_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("dball7_block_entity", () ->
                    BlockEntityType.Builder.of(Dball7BlockEntity::new, MainBlocks.DBALL7_BLOCK.get())
                            .build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}