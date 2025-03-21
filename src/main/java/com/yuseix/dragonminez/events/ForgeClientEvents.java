package com.yuseix.dragonminez.events;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.network.C2S.ConfigValuesC2S;
import com.yuseix.dragonminez.network.C2S.MenuC2S;
import com.yuseix.dragonminez.network.ModMessages;
import com.yuseix.dragonminez.stats.DMZStatsCapabilities;
import com.yuseix.dragonminez.stats.DMZStatsProvider;
import com.yuseix.dragonminez.utils.Keys;
import com.yuseix.dragonminez.world.DragonBallGenProvider;
import com.yuseix.dragonminez.world.NamekDragonBallGenProvider;
import com.yuseix.dragonminez.worldgen.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonMineZ.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEvents {

	@SubscribeEvent
	public static void onKeyInput(InputEvent.Key event) {
		if (Keys.STATS_MENU.consumeClick()) {
			ModMessages.sendToServer(new MenuC2S("stats"));
		}
		if (Keys.UTILITY_PANEL.consumeClick()) {
			ModMessages.sendToServer(new MenuC2S("utility"));
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		if (player instanceof LocalPlayer) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				ModMessages.sendToServer(new ConfigValuesC2S());
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		if (player instanceof LocalPlayer) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				ModMessages.sendToServer(new ConfigValuesC2S());
			});
		}
	}

	@SubscribeEvent
	public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		Player player = event.getEntity();
		if (player instanceof LocalPlayer) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, player).ifPresent(playerstats -> {
				ModMessages.sendToServer(new ConfigValuesC2S());
			});
		}
	}

	// Solo cancela el render Vanilla si el jugador creó su personaje
	@SubscribeEvent
	public static void RenderHealthBar(RenderGuiOverlayEvent.Pre event) {
		if (Minecraft.getInstance().player != null) {
			DMZStatsProvider.getCap(DMZStatsCapabilities.INSTANCE, Minecraft.getInstance().player).ifPresent(playerstats -> {
				boolean isDmzUser = playerstats.getBoolean("dmzuser");
				if (isDmzUser) {
					if (VanillaGuiOverlay.PLAYER_HEALTH.type() == event.getOverlay()) {
						event.setCanceled(true);
					}
				}
			});
		}
	}
}
