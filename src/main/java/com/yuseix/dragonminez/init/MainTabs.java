package com.yuseix.dragonminez.init;

import com.yuseix.dragonminez.DragonMineZ;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class MainTabs {

    //Creative Mode Tabs
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DragonMineZ.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BLOQUES_TAB = CREATIVE_TABS_REGISTER.register("dragonminez_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(MainBlocks.NAMEK_BLOCK.get()))

                    .title(Component.translatable("itemGroup.dragonminez.blocks"))

                    .displayItems((parameters, output) -> {
                        //Dragon Balls
                        output.accept(MainItems.DBALL1_BLOCK_ITEM.get());
                        output.accept(MainItems.DBALL2_BLOCK_ITEM.get());
                        output.accept(MainItems.DBALL3_BLOCK_ITEM.get());
                        output.accept(MainItems.DBALL4_BLOCK_ITEM.get());
                        output.accept(MainItems.DBALL5_BLOCK_ITEM.get());
                        output.accept(MainItems.DBALL6_BLOCK_ITEM.get());
                        output.accept(MainItems.DBALL7_BLOCK_ITEM.get());

                        //Other Blocks
                        output.accept(MainBlocks.NAMEK_BLOCK.get());
                        output.accept(MainBlocks.NAMEK_GRASS_BLOCK.get());
                        output.accept(MainBlocks.NAMEK_DIRT.get());
                        output.accept(MainBlocks.NAMEK_GRASS.get());

                    }).build());

    public static final RegistryObject<CreativeModeTab> ITEMS_TAB = CREATIVE_TABS_REGISTER.register("dragonminez_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(MainItems.POTHALA_RIGHT.get()))

                    .title(Component.translatable("itemGroup.dragonminez.items"))

                    .displayItems((parameters, output) -> {
                        output.accept(MainItems.POTHALA_RIGHT.get());
                        output.accept(MainItems.POTHALA_LEFT.get());
                        output.accept(MainItems.GREEN_POTHALA_RIGHT.get());
                        output.accept(MainItems.GREEN_POTHALA_LEFT.get());
                        output.accept(MainItems.CAPSULA_ROJA.get());
                        output.accept(MainItems.CAPSULA_VERDE.get());
                        output.accept(MainItems.CAPSULA_ANARANJADA.get());
                        output.accept(MainItems.CAPSULA_AZUL.get());
                        output.accept(MainItems.CAPSULA_MORADA.get());
                        output.accept(MainItems.COMIDA_DINO_RAW.get());
                        output.accept(MainItems.COMIDA_DINO_COOKED.get());
                        output.accept(MainItems.SENZU_BEAN.get());
                    }).build());

    public static void register(IEventBus bus) {
        CREATIVE_TABS_REGISTER.register(bus);
    }
}
