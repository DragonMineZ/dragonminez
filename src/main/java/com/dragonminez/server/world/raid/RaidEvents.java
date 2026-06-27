package com.dragonminez.server.world.raid;

import com.dragonminez.Reference;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class RaidEvents {

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
		if (event.level instanceof ServerLevel level) {
			RaidManager.tick(level);
		}
	}
}
