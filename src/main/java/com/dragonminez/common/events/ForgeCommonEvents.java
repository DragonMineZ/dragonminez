package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.commands.StatsCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ForgeCommonEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StatsCommand.register(event.getDispatcher());
    }
}

