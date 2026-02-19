package com.dragonminez.server.commands;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.SyncSagasS2C;
import com.dragonminez.common.network.S2C.SyncServerConfigS2C;
import com.dragonminez.common.network.S2C.SyncWishesS2C;
import com.dragonminez.common.quest.SagaManager;
import com.dragonminez.common.wish.WishManager;
import com.dragonminez.server.storage.StorageManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ReloadCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("dmzreload")
				.requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.RELOAD))
				.executes(ctx -> executeReload(ctx.getSource()))
		);
	}

	private static int executeReload(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		source.sendSystemMessage(Component.translatable("command.dragonminez.reload.start"));

		try {
			ConfigManager.clearServerSync();
			ConfigManager.reload();
			StorageManager.reload();
			SagaManager.loadSagas(server);
			WishManager.loadWishes(server);
			int syncedPlayers = 0;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				NetworkHandler.sendToPlayer(new SyncServerConfigS2C(ConfigManager.getServerConfig(), ConfigManager.getSkillsConfig(),
						ConfigManager.getAllForms(), ConfigManager.getAllRaceStats(), ConfigManager.getAllRaceCharacters()), player);
				NetworkHandler.sendToPlayer(new SyncSagasS2C(SagaManager.getAllSagas()), player);
				NetworkHandler.sendToPlayer(new SyncWishesS2C(WishManager.getAllWishes()), player);
				syncedPlayers++;
			}

			int finalSyncedPlayers = syncedPlayers;
			source.sendSuccess(() -> Component.translatable("command.dragonminez.reload.success"), true);
			source.sendSuccess(() -> Component.translatable("command.dragonminez.reload.sync", finalSyncedPlayers), true);

		} catch (Exception e) {
			source.sendFailure(Component.translatable("command.dragonminez.reload.error" + e.getMessage()));
			e.printStackTrace();
		}

		return 1;
	}
}