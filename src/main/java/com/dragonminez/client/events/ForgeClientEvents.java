package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.HairEditorScreen;
import com.dragonminez.client.gui.RaceSelectionScreen;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.gui.CharacterStatsScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEvents {

	@SubscribeEvent
	public static void RenderHealthBar(RenderGuiOverlayEvent.Pre event) {
		if (Minecraft.getInstance().player != null) {
			StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
				if (data.getStatus().hasCreatedCharacter()) {
					if (VanillaGuiOverlay.PLAYER_HEALTH.type() == event.getOverlay()) {
						event.setCanceled(true);
					}
				}
			});
		}
	}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null) {
            return;
        }

		if (KeyBinds.TRANSFORM_KEY.consumeClick()) {
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
				mc.setScreen(new HairEditorScreen(null, data.getCharacter()));
			});
		}

        if (KeyBinds.OPEN_CHARACTER_MENU.consumeClick()) {
            StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
                if (data.getStatus().hasCreatedCharacter()) {
                    int oldGuiScale = mc.options.guiScale().get();
                    mc.options.guiScale().set(3);
                    mc.resizeDisplay();
                    mc.setScreen(new CharacterStatsScreen(oldGuiScale));
                } else {
                    mc.setScreen(new RaceSelectionScreen(null, data.getCharacter()));
                }
            });
        }
    }

    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 10;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigManager.clearServerSync();
    }
}

