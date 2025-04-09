package com.yuseix.dragonminez.common.init;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.init.blocks.entity.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class MainBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES_REGISTER =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Reference.MOD_ID);

    //DRAGON BALLS - TIERRA
    public static final RegistryObject<BlockEntityType<Dball1BlockEntity>> DBALL1_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball1_block_entity", () ->
                    BlockEntityType.Builder.of(Dball1BlockEntity::new, MainBlocks.DBALL1_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball2BlockEntity>> DBALL2_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball2_block_entity", () ->
                    BlockEntityType.Builder.of(Dball2BlockEntity::new, MainBlocks.DBALL2_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball3BlockEntity>> DBALL3_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball3_block_entity", () ->
                    BlockEntityType.Builder.of(Dball3BlockEntity::new, MainBlocks.DBALL3_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball4BlockEntity>> DBALL4_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball4_block_entity", () ->
                    BlockEntityType.Builder.of(Dball4BlockEntity::new, MainBlocks.DBALL4_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball5BlockEntity>> DBALL5_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball5_block_entity", () ->
                    BlockEntityType.Builder.of(Dball5BlockEntity::new, MainBlocks.DBALL5_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball6BlockEntity>> DBALL6_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball6_block_entity", () ->
                    BlockEntityType.Builder.of(Dball6BlockEntity::new, MainBlocks.DBALL6_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball7BlockEntity>> DBALL7_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball7_block_entity", () ->
                    BlockEntityType.Builder.of(Dball7BlockEntity::new, MainBlocks.DBALL7_BLOCK.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<Dball1NamekBlockEntity>> DBALL1_NAMEK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball1_namek_block_entity", () ->
                    BlockEntityType.Builder.of(Dball1NamekBlockEntity::new, MainBlocks.DBALL1_NAMEK_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball2NamekBlockEntity>> DBALL2_NAMEK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball2_namek_block_entity", () ->
                    BlockEntityType.Builder.of(Dball2NamekBlockEntity::new, MainBlocks.DBALL2_NAMEK_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball3NamekBlockEntity>> DBALL3_NAMEK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball3_namek_block_entity", () ->
                    BlockEntityType.Builder.of(Dball3NamekBlockEntity::new, MainBlocks.DBALL3_NAMEK_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball4NamekBlockEntity>> DBALL4_NAMEK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball4_namek_block_entity", () ->
                    BlockEntityType.Builder.of(Dball4NamekBlockEntity::new, MainBlocks.DBALL4_NAMEK_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball5NamekBlockEntity>> DBALL5_NAMEK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball5_namek_block_entity", () ->
                    BlockEntityType.Builder.of(Dball5NamekBlockEntity::new, MainBlocks.DBALL5_NAMEK_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball6NamekBlockEntity>> DBALL6_NAMEK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball6_namek_block_entity", () ->
                    BlockEntityType.Builder.of(Dball6NamekBlockEntity::new, MainBlocks.DBALL6_NAMEK_BLOCK.get())
                            .build(null));
    public static final RegistryObject<BlockEntityType<Dball7NamekBlockEntity>> DBALL7_NAMEK_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES_REGISTER.register("dball7_namek_block_entity", () ->
                    BlockEntityType.Builder.of(Dball7NamekBlockEntity::new, MainBlocks.DBALL7_NAMEK_BLOCK.get())
                            .build(null));

    //ORES (por ahora solo el Horno)
    //public static final RegistryObject<BlockEntityType<GeteFurnaceBlockEntity>> GETE_FURNACE_BLOCK_ENTITY =
    //        BLOCK_ENTITY_TYPES_REGISTER.register("gete_furnace_block_entity", () ->
    //                BlockEntityType.Builder.of(GeteFurnaceBlockEntity::new, MainBlocks.GETE_FURNACE.get())
    //                        .build(null));
    public static final RegistryObject<BlockEntityType<KikonoArmorStationBlockEntity>> KIKONO_ARMOR_STATION_BE =
            BLOCK_ENTITY_TYPES_REGISTER.register("kikono_armor_station_be", () ->
                    BlockEntityType.Builder.of(KikonoArmorStationBlockEntity::new, MainBlocks.KIKONO_ARMOR_STATION.get())
                            .build(null));


    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES_REGISTER.register(bus);
    }

    public static final Set<Class<? extends BlockEntity>> ALL_DBALL_ENTITY_CLASSES = new HashSet<>(Arrays.asList(
            Dball1BlockEntity.class,
            Dball2BlockEntity.class,
            Dball3BlockEntity.class,
            Dball4BlockEntity.class,
            Dball5BlockEntity.class,
            Dball6BlockEntity.class,
            Dball7BlockEntity.class
    ));
}
