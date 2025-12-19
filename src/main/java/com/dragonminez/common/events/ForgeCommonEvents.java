package com.dragonminez.common.events;

import com.dragonminez.Reference;
import com.dragonminez.common.commands.EffectsCommand;
import com.dragonminez.common.commands.PartyCommand;
import com.dragonminez.common.commands.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ForgeCommonEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StatsCommand.register(event.getDispatcher());
        PointsCommand.register(event.getDispatcher());
        SkillsCommand.register(event.getDispatcher());
        EffectsCommand.register(event.getDispatcher());
        PartyCommand.register(event.getDispatcher());
        BonusCommand.register(event.getDispatcher());
        LocateCommand.register(event.getDispatcher());
		StoryCommand.register(event.getDispatcher());
    }
}

