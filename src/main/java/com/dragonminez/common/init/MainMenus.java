package com.dragonminez.common.init;

import com.dragonminez.Reference;
import com.dragonminez.common.init.menu.menutypes.FuelGeneratorMenu;
import com.dragonminez.common.init.menu.menutypes.GravityDeviceMenu;
import com.dragonminez.common.init.menu.menutypes.KikonoStationMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MainMenus {
	public static final DeferredRegister<MenuType<?>> MENUS =
			DeferredRegister.create(BuiltInRegistries.MENU, Reference.MOD_ID);

	public static final DeferredHolder<MenuType<?>, MenuType<FuelGeneratorMenu>> FUEL_GENERATOR_MENU =
			registerMenuType("fuel_generator_menu", FuelGeneratorMenu::new);

	public static final DeferredHolder<MenuType<?>, MenuType<KikonoStationMenu>> KIKONO_STATION_MENU =
			registerMenuType("kikono_station_menu", KikonoStationMenu::new);

	public static final DeferredHolder<MenuType<?>, MenuType<GravityDeviceMenu>> GRAVITY_DEVICE_MENU =
			registerMenuType("gravity_device_menu", GravityDeviceMenu::new);

	private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
		return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
	}

	public static void register(IEventBus eventBus) {
		MENUS.register(eventBus);
	}
}