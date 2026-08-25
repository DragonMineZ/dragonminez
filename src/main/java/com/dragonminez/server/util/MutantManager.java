package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.data.MutantSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MutantManager {
	public static final String EFFECT_NAME = "mutant";

	private MutantManager() {}

	private static GeneralServerConfig.MutantConfig config() {
		GeneralServerConfig serverConfig = ConfigManager.getServerConfig();
		return serverConfig != null ? serverConfig.getMutant() : null;
	}

	public static String legendaryGroupName() {
		GeneralServerConfig.MutantConfig cfg = config();
		return cfg != null ? cfg.getLegendaryGroupName() : "legendaryforms";
	}

	public static boolean isMutant(StatsData data) {
		return data != null && data.getEffects().hasEffect(EFFECT_NAME);
	}

	public static void grant(ServerPlayer player, StatsData data) {
		if (player == null || data == null) return;
		if (!data.getEffects().hasEffect(EFFECT_NAME)) {
			data.getEffects().addEffect(EFFECT_NAME, 1.0, -1);
		}
		MutantSavedData saved = MutantSavedData.get(player.getServer());
		saved.addHolder(player.getUUID());
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		player.sendSystemMessage(Component.translatable("message.dragonminez.mutant.gained"));
	}

	public static void revoke(ServerPlayer player, StatsData data) {
		if (player == null || data == null) return;
		data.getEffects().removeEffect(EFFECT_NAME);
		player.removeEffect(MainEffects.MUTANT.get());

		MutantSavedData saved = MutantSavedData.get(player.getServer());
		saved.removeHolder(player.getUUID());

		String group = data.getCharacter().getActiveFormGroup();
		boolean inLegendaryForm = group != null && group.equalsIgnoreCase(legendaryGroupName());
		boolean hasSkill = data.getSkills().getSkillLevel("legendaryforms") > 0;
		if (inLegendaryForm && !hasSkill) {
			data.getCharacter().clearActiveForm(player);
			player.removeEffect(MainEffects.TRANSFORMED.get());
			player.refreshDimensions();
		}

		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	public static void revokeOffline(MinecraftServer server, java.util.UUID playerId) {
		if (server == null || playerId == null) return;
		MutantSavedData.get(server).removeHolder(playerId);
	}

	public static void reconcileHolder(ServerPlayer player, StatsData data) {
		if (player == null || data == null || player.getServer() == null) return;
		if (!isMutant(data)) return;
		MutantSavedData saved = MutantSavedData.get(player.getServer());
		if (!saved.isHolder(player.getUUID())) saved.addHolder(player.getUUID());
	}

	public static void rollForPlayer(ServerPlayer player, StatsData data) {
		if (player == null || data == null || player.getServer() == null) return;
		GeneralServerConfig.MutantConfig cfg = config();
		if (cfg == null || !cfg.getEnabled()) return;
		if (!data.getStatus().isHasCreatedCharacter() || !data.getStatus().isAlive()) return;

		MutantSavedData saved = MutantSavedData.get(player.getServer());
		if (saved.isHolder(player.getUUID()) || isMutant(data)) return;
		if (saved.count() >= cfg.getMaxHolders()) return;

		if (player.getServer().overworld().getRandom().nextDouble() < cfg.getChance()) {
			grant(player, data);
		}
	}

	public static void runLottery(MinecraftServer server) {
		if (server == null) return;
		GeneralServerConfig.MutantConfig cfg = config();
		if (cfg == null || !cfg.getEnabled()) return;

		MutantSavedData saved = MutantSavedData.get(server);
		int maxHolders = cfg.getMaxHolders();
		if (saved.count() >= maxHolders) return;

		List<ServerPlayer> candidates = new ArrayList<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
			if (data == null) continue;
			if (!data.getStatus().isHasCreatedCharacter()) continue;
			if (!data.getStatus().isAlive()) continue;
			if (saved.isHolder(player.getUUID()) || isMutant(data)) continue;
			candidates.add(player);
		}
		if (candidates.isEmpty()) return;

		Collections.shuffle(candidates);
		int picks = Math.min(cfg.getPlayersPerRoll(), candidates.size());
		double chance = cfg.getChance();

		for (int i = 0; i < picks; i++) {
			if (saved.count() >= maxHolders) break;
			ServerPlayer chosen = candidates.get(i);
			if (server.overworld().getRandom().nextDouble() < chance) {
				StatsProvider.get(StatsCapability.INSTANCE, chosen).ifPresent(data -> grant(chosen, data));
			}
		}
	}
}
