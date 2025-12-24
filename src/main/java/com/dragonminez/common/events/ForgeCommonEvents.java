package com.dragonminez.common.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.server.commands.*;
import com.dragonminez.server.database.DatabaseManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEvents {
	private static final List<String> ALLOWED_USERNAMES = Arrays.asList(
			"Dev", "ImYuseix", "ezShokkoh", "Toji71", "KyoSleep", "InmortalPx",

			"iLalox", "Athrizel", "InmoYT",

			"grillo78", "Ducco123", "EsePibe01"
	);

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		String version = Reference.VERSION;
		if (version.contains("-beta") || version.contains("-alpha")) {
			Player player = event.getEntity();
			String username = player.getGameProfile().getName();

			if (!ALLOWED_USERNAMES.contains(username)) {
				LogUtil.error(Env.SERVER, "The user {} is not allowed to play the mod's beta. The game session will now be terminated.", username);
				throw new IllegalStateException("DMZ: Username not allowed to start gameplay!");
			}
		}
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		DatabaseManager.init();
	}

	@SubscribeEvent
	public void onServerStopping(ServerStoppingEvent event) {
		DatabaseManager.close();
	}

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
