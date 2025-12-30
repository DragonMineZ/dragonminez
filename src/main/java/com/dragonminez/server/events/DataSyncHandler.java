package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.server.storage.StorageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class DataSyncHandler {

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (event.getEntity() instanceof ServerPlayer player) {
			StorageManager.loadPlayer(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (event.getEntity() instanceof ServerPlayer player) {
			StorageManager.savePlayer(player);
		}
	}
}