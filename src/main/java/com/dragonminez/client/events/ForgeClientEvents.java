package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.crowdin.CrowdinManager;
import com.dragonminez.client.gui.UtilityMenuScreen;
import com.dragonminez.client.gui.SpacePodScreen;
import com.dragonminez.client.gui.character.BaseMenuScreen;
import com.dragonminez.client.gui.character.CharacterCustomizationScreen;
import com.dragonminez.client.gui.character.RaceSelectionScreen;
import com.dragonminez.client.gui.quest.StoryNotificationManager;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.shader.TransformationPostShaderManager;
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.gui.character.CharacterStatsScreen;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.SpacePodEntity;
import com.dragonminez.common.network.C2S.AcknowledgeStoryIntroC2S;
import com.dragonminez.common.network.C2S.SokidanControlC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.mixin.client.MinecraftAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ForgeClientEvents {
	public static boolean isHasCreatedCharacterCache = false;
	private static String lastLang = "";
	private static boolean introToastShownThisSession = false;
	private static boolean pendingCharacterCreationReopen = false;
	private static int characterCreationOpenCooldownTicks = 0;
	private static final int CHARACTER_CREATION_OPEN_COOLDOWN = 8;

	@SubscribeEvent
	public static void RenderHealthBar(RenderGuiOverlayEvent.Pre event) {
		if (Minecraft.getInstance().player != null) {
			if (isHasCreatedCharacterCache) {
				if (VanillaGuiOverlay.PLAYER_HEALTH.type() == event.getOverlay()) {
					event.setCanceled(true);
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
		TextureCounter.clearCache();
		if (Minecraft.getInstance().player == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
			isHasCreatedCharacterCache = data.getStatus().isHasCreatedCharacter();
		});
	}

	@SubscribeEvent
	public static void onKeyInput(InputEvent.Key event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		if (KeyBinds.STATS_MENU.consumeClick()) {
			if (mc.screen instanceof BaseMenuScreen) return;
			if (mc.screen != null) return;
			if (!BaseMenuScreen.isStatsMenuReopenBlocked()) {
				StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
					if (data.getStatus().isHasCreatedCharacter()) {
						mc.setScreen(new CharacterStatsScreen());
					} else {
						mc.setScreen(new RaceSelectionScreen(data.getCharacter()));
					}
					mc.player.playSound(MainSounds.UI_MENU_SWITCH.get());
				});
			}
			return;
		}

		if (mc.screen != null) return;

		if (KeyBinds.SPACEPOD_MENU.consumeClick() && mc.player.isPassenger() && mc.player.getVehicle() instanceof SpacePodEntity) {
			mc.setScreen(new SpacePodScreen());
			mc.player.playSound(MainSounds.UI_MENU_SWITCH.get());
		}

        while (KeyBinds.SECOND_FUNCTION_KEY.consumeClick()) {
            NetworkHandler.sendToServer(new SokidanControlC2S());
        }
	}

	private static int tickCounter = 0;
	private static final int UPDATE_INTERVAL = 10;

	public static void requestCharacterCreationReopen() {
		if (!ConfigManager.getServerConfig().getGameplay().getForceCharacterCreation()) return;
		pendingCharacterCreationReopen = true;
	}

	public static void markCharacterCreatedLocally() {
		isHasCreatedCharacterCache = true;
		pendingCharacterCreationReopen = false;
		characterCreationOpenCooldownTicks = CHARACTER_CREATION_OPEN_COOLDOWN;
	}

	private static boolean isCharacterCreationScreen(Screen screen) {
		return screen instanceof RaceSelectionScreen || screen instanceof CharacterCustomizationScreen;
	}

	private static boolean openCharacterCreationScreen(Minecraft mc) {
		if (mc.player == null) return false;
		if (!ConfigManager.getServerConfig().getGameplay().getForceCharacterCreation()) return false;
		if (isHasCreatedCharacterCache) return false;
		if (characterCreationOpenCooldownTicks > 0) return false;
		final boolean[] opened = {false};
		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.isDataLoaded()) return;
			if (data.getStatus().isHasCreatedCharacter()) return;
			if (isCharacterCreationScreen(mc.screen)) return;
			if (mc.screen instanceof PauseScreen) return;
			mc.setScreen(new RaceSelectionScreen(data.getCharacter()));
			characterCreationOpenCooldownTicks = CHARACTER_CREATION_OPEN_COOLDOWN;
			opened[0] = true;
		});
		return opened[0];
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;

		Minecraft mc = Minecraft.getInstance();
		TransformationPostShaderManager.tick();
		if (mc.player == null || mc.level == null) return;
		if (characterCreationOpenCooldownTicks > 0) characterCreationOpenCooldownTicks--;
		handleUtilityMenuHold(mc);

		if (pendingCharacterCreationReopen && mc.screen == null) {
			if (isHasCreatedCharacterCache) pendingCharacterCreationReopen = false;
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
				if (data.getStatus().isHasCreatedCharacter()) {
					isHasCreatedCharacterCache = true;
					pendingCharacterCreationReopen = false;
				}
			});
			if (openCharacterCreationScreen(mc)) pendingCharacterCreationReopen = false;
		}

		if (mc.options.keyAttack.isDown()) {
			if (mc.player.getAttackStrengthScale(0.5F) >= 1.0F) {
				((MinecraftAccessor) mc).setAttackCooldown(0);
			}
		}

		if (mc.screen == null) {
			openCharacterCreationScreen(mc);
		}

		tickCounter++;
		if (tickCounter >= UPDATE_INTERVAL) {
			tickCounter = 0;
			StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
				if (isHasCreatedCharacterCache != data.getStatus().isHasCreatedCharacter()) {
					isHasCreatedCharacterCache = data.getStatus().isHasCreatedCharacter();
				}

				if (!data.isDataLoaded()) return;
				if (data.getStatus().isHasCreatedCharacter()) return;
				if (introToastShownThisSession) return;
				if (data.getPlayerQuestData().isIntroPromptShown()) return;

				StoryNotificationManager.pushIntroHint();
				NetworkHandler.sendToServer(new AcknowledgeStoryIntroC2S());
				introToastShownThisSession = true;
			});
		}

		String current = mc.options.languageCode;
		if (!current.equals(lastLang)) {
			lastLang = current;
			if (CrowdinManager.isLiveTranslationsEnabled()) {
				CrowdinManager.fetchLanguage(current);
			}
		}
	}

	private static void handleUtilityMenuHold(Minecraft mc) {
		boolean utilityHeld = isUtilityMenuKeyHeld(mc);

		if (mc.screen instanceof UtilityMenuScreen utilityScreen) {
			if (!utilityHeld) utilityScreen.startClosingAnimation();
			return;
		}

		if (!utilityHeld) return;
		if (mc.screen != null) return;
		if (UtilityMenuScreen.isUtilityMenuReopenBlocked()) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;
			mc.setScreen(new UtilityMenuScreen());
			mc.player.playSound(MainSounds.UI_MENU_SWITCH.get());
		});
	}

	private static boolean isUtilityMenuKeyHeld(Minecraft mc) {
		if (mc == null || mc.getWindow() == null) return false;
		InputConstants.Key key = KeyBinds.UTILITY_MENU.getKey();
		long window = mc.getWindow().getWindow();

		if (key.getType() == InputConstants.Type.KEYSYM) {
			return InputConstants.isKeyDown(window, key.getValue());
		}
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
		}
		return false;
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			TransformationPostShaderManager.flushMaskAndApplyUniforms(event.getPartialTick(), event.getPoseStack(), event.getCamera(), event.getFrustum());
		}
	}

	@SubscribeEvent
	public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
		ConfigManager.clearServerSync();
		DMZRendererCache.clear();
		TextureCounter.clearCache();
		introToastShownThisSession = false;
		pendingCharacterCreationReopen = false;
		characterCreationOpenCooldownTicks = 0;
	}

	@SubscribeEvent
	public static void onPreRenderCrosshair(RenderGuiOverlayEvent.Pre event) {
		if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
			Minecraft client = Minecraft.getInstance();
			if (client != null && ((Minecraft_DMZ) client).hasTargetsInReach()) RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
		}
	}

	@SubscribeEvent
	public static void onPostRenderCrosshair(RenderGuiOverlayEvent.Post event) {
		if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
