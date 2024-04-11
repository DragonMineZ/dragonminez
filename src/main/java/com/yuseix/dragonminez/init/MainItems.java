package com.yuseix.dragonminez.init;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.items.custom.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MainItems {

    public static final Item.Properties properties = new Item.Properties();
    public static final DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, DragonMineZ.MOD_ID);

    //CAPSULAS
    public static final RegistryObject<Item> CAPSULA_ROJA =
            ITEM_REGISTER.register("capsula_roja", CapsulaRojaItem::new);
    public static final RegistryObject<Item> CAPSULA_VERDE =
            ITEM_REGISTER.register("capsula_verde", CapsulaVerdeItem::new);
    public static final RegistryObject<Item> CAPSULA_ANARANJADA =
            ITEM_REGISTER.register("capsula_anaranjada", CapsulaNaranjaItem::new);
    public static final RegistryObject<Item> CAPSULA_AZUL =
            ITEM_REGISTER.register("capsula_azul", CapsulaAzulItem::new);
    public static final RegistryObject<Item> CAPSULA_MORADA =
            ITEM_REGISTER.register("capsula_morada", CapsulaMoradaItem::new);

    //POTHALAS
    public static final RegistryObject<Item> POTHALA_RIGHT =
            ITEM_REGISTER.register("pothala_right", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));
    public static final RegistryObject<Item> POTHALA_LEFT =
            ITEM_REGISTER.register("pothala_left", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));
    public static final RegistryObject<Item> GREEN_POTHALA_RIGHT =
            ITEM_REGISTER.register("green_pothala_right", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));
    public static final RegistryObject<Item> GREEN_POTHALA_LEFT =
            ITEM_REGISTER.register("green_pothala_left", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));

    //DRAGON BALLS
    public static final RegistryObject<Item> DBALL1_BLOCK_ITEM = ITEM_REGISTER.register("dball1",
            () -> new DbBallBlockItem(MainBlocks.DBALL1_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    public static final RegistryObject<Item> DBALL2_BLOCK_ITEM = ITEM_REGISTER.register("dball2",
            () -> new DbBallBlockItem(MainBlocks.DBALL2_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    public static final RegistryObject<Item> DBALL3_BLOCK_ITEM = ITEM_REGISTER.register("dball3",
            () -> new DbBallBlockItem(MainBlocks.DBALL3_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    public static final RegistryObject<Item> DBALL4_BLOCK_ITEM = ITEM_REGISTER.register("dball4",
            () -> new DbBallBlockItem(MainBlocks.DBALL4_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    public static final RegistryObject<Item> DBALL5_BLOCK_ITEM = ITEM_REGISTER.register("dball5",
            () -> new DbBallBlockItem(MainBlocks.DBALL5_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    public static final RegistryObject<Item> DBALL6_BLOCK_ITEM = ITEM_REGISTER.register("dball6",
            () -> new DbBallBlockItem(MainBlocks.DBALL6_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    public static final RegistryObject<Item> DBALL7_BLOCK_ITEM = ITEM_REGISTER.register("dball7",
            () -> new DbBallBlockItem(MainBlocks.DBALL7_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    public static void register(IEventBus bus) {
        ITEM_REGISTER.register(bus);
    }
}
