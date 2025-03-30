package com.yuseix.dragonminez.common.registry;

import com.yuseix.dragonminez.common.Reference;
import com.yuseix.dragonminez.common.config.GeneralConfigHandler;
import com.yuseix.dragonminez.core.common.config.ConfigManager;
import com.yuseix.dragonminez.core.common.config.event.RegisterConfigHandlerEvent;
import com.yuseix.dragonminez.core.common.config.model.ConfigType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConfigRegistry {

    public static final String GENERAL = "general";

    // I know this is not needed, i just like to have this kind of functions to know what i'm calling
    // in the main class.
    public static void init(){}

    @SubscribeEvent
    public static void onRegisterEvent(RegisterConfigHandlerEvent event) {
        if(event.type() == ConfigType.STATIC) {
            ConfigRegistry.registerStatic();
        } else if(event.type() == ConfigType.RUNTIME) {
            ConfigRegistry.registerDynamic();
        }
    }

    private static void registerStatic() {
    }

    private static void registerDynamic() {
        ConfigManager.INSTANCE.register(new GeneralConfigHandler());
    }
}
