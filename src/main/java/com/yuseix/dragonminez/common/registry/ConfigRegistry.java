package com.yuseix.dragonminez.common.registry;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.config.GeneralConfigHandler;
import com.yuseix.dragonminez.core.common.config.ConfigManager;
import com.yuseix.dragonminez.core.common.config.event.RegisterConfigHandlerEvent;
import com.yuseix.dragonminez.core.common.config.model.ConfigType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConfigRegistry {

    public static final String GENERAL = "general";

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ConfigRegistry::onRegisterEvent);
    }

    @SubscribeEvent
    public static void onRegisterEvent(RegisterConfigHandlerEvent event) {
        if (event.type() == ConfigType.STATIC) {
            ConfigRegistry.registerStatic();
        } else if (event.type() == ConfigType.RUNTIME) {
            ConfigRegistry.registerDynamic();
        }
    }

    private static void registerStatic() {
    }

    private static void registerDynamic() {
        ConfigManager.INSTANCE.register(new GeneralConfigHandler());
    }
}
