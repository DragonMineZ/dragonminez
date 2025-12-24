package com.dragonminez.common.events;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.server.commands.*;
import com.dragonminez.server.database.DatabaseManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEvents {
	private static final List<String> ALLOWED_USERNAMES = Arrays.asList(
			// Staff
			"Dev", "ImYuseix", "ezShokkoh", "Toji71", "KyoSleep", "InmortalPx",

			// Patreons
			"iLalox", "Athrizel", "InmoYT",

			// Beta Testers
			"grillo78", "Ducco123", "EsePibe01", "ImmortalHemp", "werff", "Lucasispedroche", "LemmeInPeace", "RemiBuster17", "Pokimons123", "Billufi"
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

	@SubscribeEvent
	public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
		Mob mob = event.getEntity();
		if (mob.getType().getCategory() == MobCategory.MONSTER) {
			List<MastersEntity> masters = mob.level().getEntitiesOfClass(MastersEntity.class,
					new AABB(mob.blockPosition()).inflate(40));

			if (!masters.isEmpty()) {
				event.setSpawnCancelled(true);
				event.setResult(Event.Result.DENY);
			}
		}
	}
}
