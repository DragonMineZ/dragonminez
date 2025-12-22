package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.database.DatabaseManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class DatabaseSyncHandler {

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity().level().isClientSide) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {

			if (ConfigManager.getServerConfig().getStorage().getStorageType()
					== GeneralServerConfig.StorageConfig.StorageType.DATABASE) {

				CompoundTag dbData = DatabaseManager.loadPlayer(player.getUUID());
				if (dbData != null) {
					data.load(dbData);
				}
			}

			if (!data.getQuestData().isSagaUnlocked("saiyan_saga")) {
				data.getQuestData().unlockSaga("saiyan_saga");
			}

			NetworkHandler.sendToPlayer(new StatsSyncS2C(player), player);
		});
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity().level().isClientSide) return;

		if (ConfigManager.getServerConfig().getStorage().getStorageType()
				== GeneralServerConfig.StorageConfig.StorageType.DATABASE) {

			if (event.getEntity() instanceof ServerPlayer player) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
					DatabaseManager.savePlayer(player.getUUID(), player.getName().getString(), stats.save());
				});
			}
		}
	}
}
