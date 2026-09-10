package com.dragonminez.client.render.effects;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.AuraModeC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class AuraModeState {
	private AuraModeState() {}

	public static boolean localPreference() {
		var config = ConfigManager.getUserConfig();
		return config != null && Boolean.TRUE.equals(config.getAura3DPersonal());
	}

	public static boolean entityPreference() {
		var config = ConfigManager.getUserConfig();
		return config != null && Boolean.TRUE.equals(config.getAura3DEntities());
	}

	public static boolean isAura3D(Player player) {
		if (player == null) return false;
		if (player == Minecraft.getInstance().player) return localPreference();
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		return stats != null && stats.getCharacter().isAura3D();
	}

	public static void pushLocalPreference() {
		if (Minecraft.getInstance().player == null) return;
		NetworkHandler.sendToServer(new AuraModeC2S(localPreference()));
	}

	public static void reconcileLocal(Player player) {
		Minecraft mc = Minecraft.getInstance();
		if (player == null || player != mc.player) return;
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;
		if (stats.getCharacter().isAura3D() == localPreference()) return;
		pushLocalPreference();
	}
}
