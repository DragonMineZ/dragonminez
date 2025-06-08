package com.dragonminez.mod.common.registry;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.config.GeneralConfigHandler;
import com.dragonminez.core.common.config.ConfigManager;
import com.dragonminez.core.common.config.event.RegisterConfigHandlerEvent;
import com.dragonminez.mod.server.config.dimensions.DimensionConfigHandler;
import com.dragonminez.mod.server.config.food.FoodConfigHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConfigRegistry {

  public static final String GENERAL = "general";
  public static final String FOOD = "food";
  public static final String DIMENSION = "dimension";

  public static void init() {
    MinecraftForge.EVENT_BUS.addListener(ConfigRegistry::onRegisterEvent);
  }

  @SubscribeEvent
  public static void onRegisterEvent(RegisterConfigHandlerEvent event) {
    ConfigRegistry.registerStatic();
    ConfigRegistry.registerDynamic();
  }

  private static void registerStatic() {
  }

  private static void registerDynamic() {
    ConfigManager.INSTANCE.register(new GeneralConfigHandler());
    ConfigManager.INSTANCE.register(new FoodConfigHandler());
    ConfigManager.INSTANCE.register(new DimensionConfigHandler());
  }
}
