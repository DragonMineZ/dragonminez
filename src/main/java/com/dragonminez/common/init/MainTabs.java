package com.dragonminez.common.init;

import com.dragonminez.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public final class MainTabs {

	//Creative Mode Tabs
	public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS_REGISTER =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Reference.MOD_ID);

	public static final RegistryObject<CreativeModeTab> BLOQUES_TAB = CREATIVE_TABS_REGISTER.register("dragonminez_blocks_tab",
			() -> CreativeModeTab.builder().icon(() -> new ItemStack(MainBlocks.DBALL4_BLOCK.get()))

					.title(Component.translatable("itemGroup.dragonminez.blocks"))
					.displayItems((parameters, output) -> MainBlocks.BLOCK_REGISTER.getEntries().forEach((block) -> {
						if (!block.getId().getPath().startsWith("namek_")) {
							if (!block.getId().getPath().startsWith("sacred_")) {
								if (!block.getId().getPath().endsWith("_flower")) {
									if (!block.getId().getPath().startsWith("potted_")) {
										if (!(block.getId().getPath().contains("gete"))) {
											if (!(block.get() instanceof LiquidBlock)) {
												if (!block.getId().getPath().contains("invisible"))
													output.accept(block.get().asItem());
											}
										}
									}
								}
							}
						}
					})).build()
	);

	public static final RegistryObject<CreativeModeTab> NAMEK_TAB = CREATIVE_TABS_REGISTER.register("dragonminez_namek_tab",
			() -> CreativeModeTab.builder().icon(() -> new ItemStack(MainBlocks.NAMEK_GRASS_BLOCK.get()))

					.title(Component.translatable("itemGroup.dragonminez.namek"))
					.displayItems((parameters, output) -> MainItems.ITEM_REGISTER.getEntries().forEach((item) -> {
						if (!item.getId().getPath().contains("bucket")) {
							if (item.getId().getPath().startsWith("namek_"))
								if (!item.getId().getPath().contains("spawn_egg"))
									output.accept(item.get().asItem());
							if (item.getId().getPath().startsWith("sacred_") && (!item.getId().getPath().endsWith("_flower")))
								output.accept(item.get().asItem());
							if (item.getId().getPath().endsWith("_flower") && (!item.getId().getPath().startsWith("potted_") && (!item.getId().getPath().contains("lotus"))))
								output.accept(item.get().asItem());
						}
					})).build()
	);

	public static final RegistryObject<CreativeModeTab> ITEMS_TAB = CREATIVE_TABS_REGISTER.register("dragonminez_items_tab",
			() -> CreativeModeTab.builder().icon(() -> new ItemStack(MainItems.POTHALA_RIGHT.get()))

					.title(Component.translatable("itemGroup.dragonminez.items"))
					.displayItems((parameters, output) -> MainItems.ITEM_REGISTER.getEntries().forEach((item) -> {
						if (!(item.get() instanceof BlockItem)) {
							if (!item.getId().getPath().contains("_armor_")) {
								output.accept(item.get().asItem());
							}
						}
					})).build()
	);

//	public static final RegistryObject<CreativeModeTab> ARMORS_TAB =  CREATIVE_TABS_REGISTER.register("dragonminez_armors_tab",
//			() -> CreativeModeTab.builder().icon(() -> new ItemStack(MainItems.BARDOCK_DBZ_ARMOR_CHESTPLATE.get()))
//
//					.title(Component.translatable("itemGroup.dragonminez.armors"))
//					.displayItems((parameters, output) -> MainItems.ITEM_REGISTER.getEntries().forEach((item) -> {
//						if (item.getId().getPath().contains("_armor_")) {
//							if (!item.getId().getPath().equals("kikono_armor_station")) {
//								output.accept(item.get().asItem());
//							}
//						}
//					})).build()
//	);

	public static final RegistryObject<CreativeModeTab> ORES_TAB = CREATIVE_TABS_REGISTER.register("dragonminez_ores_tab",
			() -> CreativeModeTab.builder().icon(() -> new ItemStack(MainBlocks.GETE_ORE.get()))

					.title(Component.translatable("itemGroup.dragonminez.ores"))
					.displayItems((parameters, output) -> MainItems.ITEM_REGISTER.getEntries().forEach((item) -> {
						if (item.getId().getPath().contains("_ore"))
							output.accept(item.get());
						if (item.getId().getPath().contains("gete") && (!item.getId().getPath().contains("furnace")))
							output.accept(item.get());
						if (item.getId().getPath().contains("kikono"))
							output.accept(item.get());
					})).build()
	);

	public static void register(IEventBus bus) {
		CREATIVE_TABS_REGISTER.register(bus);
	}
}
