package com.dragonminez.server.world.raid;

import com.dragonminez.Reference;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidEvents {

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel().isClientSide) return;
		if (event.getLevel() instanceof ServerLevel level) {
			RaidManager.tick(level);
		}
	}
}
