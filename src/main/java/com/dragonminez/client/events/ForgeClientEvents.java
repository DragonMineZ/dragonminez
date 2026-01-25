package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.client.gui.HairEditorScreen;
import com.dragonminez.client.gui.SpacePodScreen;
import com.dragonminez.client.gui.character.RaceSelectionScreen;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.gui.character.CharacterStatsScreen;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.SpacePodEntity;
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
	public static boolean hasCreatedCharacterCache = false;

	@SubscribeEvent
	public static void RenderHealthBar(RenderGuiOverlayEvent.Pre event) {
		if (Minecraft.getInstance().player != null) {
			if (hasCreatedCharacterCache) {
				if (VanillaGuiOverlay.PLAYER_HEALTH.type() == event.getOverlay()) {
					event.setCanceled(true);}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
		if (Minecraft.getInstance().player == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
			hasCreatedCharacterCache = data.getStatus().hasCreatedCharacter();
		});
	}

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (KeyBinds.SPACEPOD_MENU.consumeClick()) {
            StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
                mc.setScreen(new HairEditorScreen(null, data.getCharacter()));
            });
        }

        if (KeyBinds.STATS_MENU.consumeClick()) {
			if (mc.player == null || mc.screen != null) return;
			int oldGuiScale = mc.options.guiScale().get();
			if (oldGuiScale != 3) {
				mc.options.guiScale().set(3);
				mc.resizeDisplay();
			}

			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
				if (data.getStatus().hasCreatedCharacter()) {
					mc.setScreen(new CharacterStatsScreen(oldGuiScale));
				} else {
					mc.setScreen(new RaceSelectionScreen(data.getCharacter(), oldGuiScale));
				}
				mc.player.playSound(MainSounds.UI_MENU_SWITCH.get());
			});
        }

		if (KeyBinds.SPACEPOD_MENU.consumeClick() && mc.player.isPassenger() && mc.player.getVehicle() instanceof SpacePodEntity) {
			mc.setScreen(new SpacePodScreen());
			mc.player.playSound(MainSounds.UI_MENU_SWITCH.get());
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

		if (KeyBinds.UTILITY_MENU.isDown()) {
			if (mc.screen == null) {
				StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
					if (!data.getStatus().hasCreatedCharacter()) return;
					mc.setScreen(new UtilityMenuScreen());
					mc.player.playSound(MainSounds.UI_MENU_SWITCH.get());
				});
			}
		}

        tickCounter++;
        if (tickCounter >= UPDATE_INTERVAL) {
            tickCounter = 0;
            StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
                if (hasCreatedCharacterCache != data.getStatus().hasCreatedCharacter()) {
                    hasCreatedCharacterCache = data.getStatus().hasCreatedCharacter();
                }
            });
        }
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigManager.clearServerSync();
    }
}
