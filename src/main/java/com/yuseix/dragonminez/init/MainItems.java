package com.yuseix.dragonminez.init;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.init.items.custom.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public interface MainItems {

    Item.Properties properties = new Item.Properties();
    DeferredRegister<Item> ITEM_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, DragonMineZ.MOD_ID);

    //CAPSULAS
    RegistryObject<Item> CAPSULA_ROJA = ITEM_REGISTER.register("capsula_roja", CapsulaRojaItem::new);
    RegistryObject<Item> CAPSULA_VERDE = ITEM_REGISTER.register("capsula_verde", CapsulaVerdeItem::new);
    RegistryObject<Item> CAPSULA_ANARANJADA = ITEM_REGISTER.register("capsula_anaranjada", CapsulaNaranjaItem::new);
    RegistryObject<Item> CAPSULA_AZUL = ITEM_REGISTER.register("capsula_azul", CapsulaAzulItem::new);
    RegistryObject<Item> CAPSULA_MORADA = ITEM_REGISTER.register("capsula_morada", CapsulaMoradaItem::new);

    //POTHALAS
    RegistryObject<Item> POTHALA_RIGHT =
            ITEM_REGISTER.register("pothala_right", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));
    RegistryObject<Item> POTHALA_LEFT =
            ITEM_REGISTER.register("pothala_left", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));
    RegistryObject<Item> GREEN_POTHALA_RIGHT =
            ITEM_REGISTER.register("green_pothala_right", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));
    RegistryObject<Item> GREEN_POTHALA_LEFT =
            ITEM_REGISTER.register("green_pothala_left", () -> new Item(properties
                    .stacksTo(1)
                    .fireResistant()));

    //DRAGON BALLS
    RegistryObject<Item> DBALL1_BLOCK_ITEM = ITEM_REGISTER.register("dball1",
            () -> new DbBallBlockItem(MainBlocks.DBALL1_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    RegistryObject<Item> DBALL2_BLOCK_ITEM = ITEM_REGISTER.register("dball2",
            () -> new DbBallBlockItem(MainBlocks.DBALL2_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    RegistryObject<Item> DBALL3_BLOCK_ITEM = ITEM_REGISTER.register("dball3",
            () -> new DbBallBlockItem(MainBlocks.DBALL3_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    RegistryObject<Item> DBALL4_BLOCK_ITEM = ITEM_REGISTER.register("dball4",
            () -> new DbBallBlockItem(MainBlocks.DBALL4_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    RegistryObject<Item> DBALL5_BLOCK_ITEM = ITEM_REGISTER.register("dball5",
            () -> new DbBallBlockItem(MainBlocks.DBALL5_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    RegistryObject<Item> DBALL6_BLOCK_ITEM = ITEM_REGISTER.register("dball6",
            () -> new DbBallBlockItem(MainBlocks.DBALL6_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));
    RegistryObject<Item> DBALL7_BLOCK_ITEM = ITEM_REGISTER.register("dball7",
            () -> new DbBallBlockItem(MainBlocks.DBALL7_BLOCK.get(), properties
                    .stacksTo(1)
                    .fireResistant()
            ));

    //COMIDA
    RegistryObject<Item> SENZU_BEAN = ITEM_REGISTER.register("senzu_bean", SenzuBean::new);
    RegistryObject<Item> COMIDA_DINO_RAW = ITEM_REGISTER.register("comida_dino_raw", ComidaDinoRaw::new);
    RegistryObject<Item> COMIDA_DINO_COOKED = ITEM_REGISTER.register("comida_dino_cooked", ComidaDinoCooked::new);
}
