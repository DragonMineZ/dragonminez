package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BabaReviveService {

	private static final int[] WARNING_SECONDS = {1800, 600, 300, 60, 30, 10, 5};

	private BabaReviveService() {
	}

	public static boolean isEnabled() {
		return ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()
				&& ConfigManager.getServerConfig().getGameplay().getBabaTempReturnEnabled();
	}

	public static void handleDeadInteract(ServerPlayer player, StatsData data) {
		int cooldownTicks = data.getCooldowns().getCooldown(Cooldowns.REVIVE_BABA);
		if (cooldownTicks > 0) {
			player.displayClientMessage(Component.translatable("message.dragonminez.baba_return.cooldown",
					formatSeconds(Math.max(1, cooldownTicks / 20))), true);
			return;
		}

		int limit = ConfigManager.getServerConfig().getGameplay().getBabaTempReturnLimit();
		if (limit > 0 && data.getStatus().getTempReturnsUsed() >= limit) {
			player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.limit"));
			return;
		}

		int seconds = ConfigManager.getServerConfig().getGameplay().getBabaTempReturnSeconds();
		data.getStatus().setTempReturnTimer(seconds * 20);
		data.getStatus().setTempReturnsUsed(data.getStatus().getTempReturnsUsed() + 1);
		teleportToLivingWorld(player);
		player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.granted", formatSeconds(seconds)));
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	/** Called once per second from TickHandler while the timer is active. */
	public static void tickTempReturn(ServerPlayer player, StatsData data) {
		int timer = data.getStatus().getTempReturnTimer();
		if (timer <= 0) return;

		if (player.serverLevel().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
			data.getStatus().setTempReturnTimer(0);
			return;
		}

		int drained = (int) Math.round(20 * timeDrainMultiplier(data));
		int remaining = Math.max(0, timer - Math.max(20, drained));
		data.getStatus().setTempReturnTimer(remaining);

		if (remaining <= 0) {
			endTempReturn(player, data);
			return;
		}

		int secondsBefore = timer / 20;
		int secondsAfter = remaining / 20;
		for (int warning : WARNING_SECONDS) {
			if (secondsAfter <= warning && secondsBefore > warning) {
				player.displayClientMessage(Component.translatable("message.dragonminez.baba_return.time_left",
						formatSeconds(secondsAfter)), true);
				break;
			}
		}
	}

	private static double timeDrainMultiplier(StatsData data) {
		double multiplier = 1.0;
		FormConfig.FormData form = data.getCharacter().getActiveFormData();
		FormConfig.FormData stackForm = data.getCharacter().getActiveStackFormData();
		if (form != null) multiplier += Math.max(0, form.getOtherworldTimeDrain() - 1.0);
		if (stackForm != null) multiplier += Math.max(0, stackForm.getOtherworldTimeDrain() - 1.0);
		return multiplier;
	}

	private static void endTempReturn(ServerPlayer player, StatsData data) {
		data.getStatus().setTempReturnTimer(0);
		player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.expired"));
		if (!player.isSpectator() && !player.isCreative()) {
			ServerLevel otherworld = player.getServer().getLevel(OtherworldDimension.OTHERWORLD_KEY);
			if (otherworld != null) player.teleportTo(otherworld, 0, 41, 10, 0, 0);
		}
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	private static void teleportToLivingWorld(ServerPlayer player) {
		ServerLevel respawnLevel = player.getServer().getLevel(player.getRespawnDimension());
		BlockPos respawnPos = player.getRespawnPosition();
		if (respawnLevel != null && respawnPos != null
				&& !respawnLevel.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
			Optional<Vec3> spot = ServerPlayer.findRespawnPositionAndUseSpawnBlock(
					respawnLevel, respawnPos, player.getRespawnAngle(), player.isRespawnForced(), false);
			if (spot.isPresent()) {
				Vec3 pos = spot.get();
				player.teleportTo(respawnLevel, pos.x, pos.y, pos.z, player.getRespawnAngle(), 0);
				return;
			}
		}
		ServerLevel overworld = player.getServer().overworld();
		BlockPos spawn = overworld.getSharedSpawnPos();
		int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
		player.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0);
	}

	private static String formatSeconds(int totalSeconds) {
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
		return String.format("%d:%02d", minutes, seconds);
	}
}
