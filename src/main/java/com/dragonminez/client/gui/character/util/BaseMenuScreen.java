package com.dragonminez.client.gui.character.util;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.client.gui.character.*;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class BaseMenuScreen extends ScaledScreen {

	public record MenuTab(KeyMapping key, Supplier<Screen> factory) {}

	public static final List<MenuTab> SECONDARY_TABS = List.of(
			new MenuTab(KeyBinds.STATS_TAB_PARTY, PartyMenuScreen::new),
			new MenuTab(KeyBinds.STATS_TAB_SKILLS, SkillsMenuScreen::new),
			new MenuTab(KeyBinds.STATS_TAB_QUESTS, QuestTreeScreen::new),
			new MenuTab(KeyBinds.STATS_TAB_MINIGAMES, MinigamesScreen::new),
			new MenuTab(KeyBinds.STATS_TAB_CONFIG, ConfigMenuScreen::new)
	);

	protected static boolean GLOBAL_SWITCHING = false;
	protected boolean isSwitchingMenu = false;
	private static final ResourceLocation SCREEN_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/menubuttons.png");
	private static final long OPEN_ANIMATION_DURATION = 200;
	private static final long PANEL_ENTER_ANIMATION_DURATION = 520;
	private static final long PANEL_EXIT_ANIMATION_DURATION = 140;
	private static final int PANEL_SWITCH_DISTANCE = 190;
	private static final int TOP_PANEL_SWITCH_DISTANCE = 90;
	private static final long STATS_MENU_REOPEN_COOLDOWN_MS = 450L;
	private static long statsMenuReopenBlockedUntilMs = 0L;

	private enum TransitionState { NONE, OPENING, CLOSING }

	private long animationStartTime;
	private TransitionState transitionState = TransitionState.NONE;
	private boolean suppressOpenAnimationOnce = false;

	private enum PanelSwitchState { NONE, ENTERING, EXITING }

	private long panelSwitchAnimationStartTime;
	private PanelSwitchState panelSwitchState = PanelSwitchState.NONE;
	private Screen pendingSwitchScreen;

	protected float tooltipScrollY = 0;
	protected float targetTooltipScrollY = 0;

	protected BaseMenuScreen(Component title) {
		super(title);
	}

	@Override
	protected void init() {
		super.init();
		if (GLOBAL_SWITCHING) {
			GLOBAL_SWITCHING = false;
			transitionState = TransitionState.NONE;
			startPanelEnterTransition();
		} else if (!suppressOpenAnimationOnce) startOpenTransition();

		initNavigationButtons();
	}

	@Override
	public void tick() {
		super.tick();
		this.tooltipScrollY = Mth.lerp(0.5f, this.tooltipScrollY, this.targetTooltipScrollY);
		if (panelSwitchState == PanelSwitchState.ENTERING && getPanelSwitchProgress(getMinecraft().getPartialTick()) >= 1.0f) panelSwitchState = PanelSwitchState.NONE;

		if (panelSwitchState == PanelSwitchState.EXITING && getPanelSwitchProgress(getMinecraft().getPartialTick()) >= 1.0f) {
			panelSwitchState = PanelSwitchState.NONE;
			if (this.minecraft != null) {
				GLOBAL_SWITCHING = true;
				this.minecraft.setScreen(pendingSwitchScreen);
			}
		}

		if (transitionState == TransitionState.OPENING && getTransitionProgress(getMinecraft().getPartialTick()) >= 1.0f) transitionState = TransitionState.NONE;
		if (transitionState == TransitionState.CLOSING && getTransitionProgress(getMinecraft().getPartialTick()) >= 1.0f)
			if (this.minecraft != null) this.minecraft.setScreen(null);
	}

	protected void initNavigationButtons() {
		int centerX = getUiWidth() / 2;
		int bottomY = getUiHeight() - 30;

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX - 110, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(120, 0, 120, 20)
						.onPress(btn -> switchMenu(new PartyMenuScreen()))
						.sound(MainSounds.UI_MENU_SWITCH.get())
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX - 70, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(0, 0, 0, 20)
						.onPress(btn -> switchMenu(new CharacterStatsScreen()))
						.sound(MainSounds.UI_MENU_SWITCH.get())
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX - 30, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(20, 0, 20, 20)
						.onPress(btn -> switchMenu(new SkillsMenuScreen()))
						.sound(MainSounds.UI_MENU_SWITCH.get())
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX + 10, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(60, 0, 60, 20)
						.onPress(btn -> switchMenu(new QuestTreeScreen()))
						.sound(MainSounds.UI_MENU_SWITCH.get())
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX + 50, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(140, 0, 140, 20)
						.onPress(btn -> switchMenu(new MinigamesScreen()))
						.sound(MainSounds.UI_MENU_SWITCH.get())
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX + 90, bottomY)
						.size(20, 20)
						.texture(SCREEN_BUTTONS)
						.textureSize(20, 20)
						.textureCoords(100, 0, 100, 20)
						.onPress(btn -> switchMenu(new ConfigMenuScreen()))
						.sound(MainSounds.UI_MENU_SWITCH.get())
						.build()
		);
	}

	protected void switchMenu(Screen nextScreen) {
		if (this.minecraft != null && this.minecraft.screen != null && !(this.minecraft.screen.getClass().equals(nextScreen.getClass()))) {
			this.isSwitchingMenu = true;
			startPanelExitTransition(nextScreen);
		}
	}

	protected int calculateScrollOffset(double uiMouseY, int startY, int scrollBarHeight, int maxScrollValue) {
		float scrollPercent = (float) (uiMouseY - startY) / scrollBarHeight;
		scrollPercent = Mth.clamp(scrollPercent, 0.0f, 1.0f);
		return Math.round(scrollPercent * maxScrollValue);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public boolean isNotAnimating() {
		return transitionState == TransitionState.NONE || !(getTransitionProgress(getMinecraft().getPartialTick()) < 1.0f);
	}

	public static boolean isStatsMenuReopenBlocked() {
		return System.currentTimeMillis() < statsMenuReopenBlockedUntilMs;
	}

	protected void applyZoom(GuiGraphics graphics, float partialTick) {
		if (transitionState == TransitionState.NONE) return;

		float progress = getTransitionProgress(partialTick);
		if (progress >= 1.0f) return;

		float scale = transitionState == TransitionState.OPENING ? easeOutBack(progress) : easeOutBack(1.0f - progress);
		scale = Math.max(0.001f, scale);

		PoseStack pose = graphics.pose();
		int uiWidth = getUiWidth();
		int uiHeight = getUiHeight();
		pose.translate(uiWidth / 2.0, uiHeight / 2.0, 0);
		pose.scale(scale, scale, 1.0f);
		pose.translate(-uiWidth / 2.0, -uiHeight / 2.0, 0);
	}

	protected int getLeftPanelSwitchOffset(float partialTick) {
		if (panelSwitchState == PanelSwitchState.NONE) return 0;
		float p = getPanelSwitchProgress(partialTick);

		if (panelSwitchState == PanelSwitchState.ENTERING) {
			float eased = easeOutBack(p);
			return Math.round((eased - 1.0f) * PANEL_SWITCH_DISTANCE);
		}

		float eased = easeInBack(p);
		return Math.round(-eased * PANEL_SWITCH_DISTANCE);
	}

	protected int getRightPanelSwitchOffset(float partialTick) {
		if (panelSwitchState == PanelSwitchState.NONE) return 0;
		float p = getPanelSwitchProgress(partialTick);

		if (panelSwitchState == PanelSwitchState.ENTERING) {
			float eased = easeOutBack(p);
			return Math.round((1.0f - eased) * PANEL_SWITCH_DISTANCE);
		}

		float eased = easeInBack(p);
		return Math.round(eased * PANEL_SWITCH_DISTANCE);
	}

	protected int getTopPanelSwitchOffset(float partialTick) {
		if (panelSwitchState == PanelSwitchState.NONE) return 0;
		float p = getPanelSwitchProgress(partialTick);

		if (panelSwitchState == PanelSwitchState.ENTERING) {
			float eased = easeOutBack(p);
			return Math.round((eased - 1.0f) * TOP_PANEL_SWITCH_DISTANCE);
		}

		float eased = easeInBack(p);
		return Math.round(-eased * TOP_PANEL_SWITCH_DISTANCE);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (panelSwitchState == PanelSwitchState.EXITING) return true;
		if (transitionState == TransitionState.CLOSING) return true;

		int statsMenuKeyCode = KeyBinds.STATS_MENU.getKey().getValue();
		if (keyCode == statsMenuKeyCode) {
			onClose();
			return true;
		}
		if (keyCode == 256) {
			onClose();
			return true;
		}

		InputConstants.Key pressed = InputConstants.getKey(keyCode, scanCode);
		for (MenuTab tab : SECONDARY_TABS) {
			if (tab.key().isActiveAndMatches(pressed)) {
				switchMenu(tab.factory().get());
				return true;
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		startCloseTransition();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (panelSwitchState == PanelSwitchState.EXITING) return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (panelSwitchState == PanelSwitchState.EXITING) return true;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (panelSwitchState == PanelSwitchState.EXITING) return true;
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (panelSwitchState == PanelSwitchState.EXITING) return true;

		if (Screen.hasAltDown()) {
			this.targetTooltipScrollY += (float) (delta * 15.0);
			if (this.targetTooltipScrollY > 0) this.targetTooltipScrollY = 0;
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private void startPanelEnterTransition() {
		panelSwitchState = PanelSwitchState.ENTERING;
		panelSwitchAnimationStartTime = System.currentTimeMillis();
		pendingSwitchScreen = null;
	}

	private void startPanelExitTransition(Screen nextScreen) {
		if (panelSwitchState == PanelSwitchState.EXITING) return;
		pendingSwitchScreen = nextScreen;
		panelSwitchState = PanelSwitchState.EXITING;
		panelSwitchAnimationStartTime = System.currentTimeMillis();
	}

	private void startOpenTransition() {
		transitionState = TransitionState.OPENING;
		animationStartTime = System.currentTimeMillis();
	}

	private void startCloseTransition() {
		if (transitionState == TransitionState.CLOSING) return;
		long now = System.currentTimeMillis();
		transitionState = TransitionState.CLOSING;
		animationStartTime = now;
		statsMenuReopenBlockedUntilMs = Math.max(statsMenuReopenBlockedUntilMs, now + STATS_MENU_REOPEN_COOLDOWN_MS);
		while (KeyBinds.STATS_MENU.consumeClick()) {}
	}

	protected float getTransitionProgress(float partialTick) {
		long elapsed = System.currentTimeMillis() - animationStartTime;
		return Mth.clamp((elapsed + (partialTick * 50)) / (float) OPEN_ANIMATION_DURATION, 0.0f, 1.0f);
	}

	protected float getPanelSwitchProgress(float partialTick) {
		long elapsed = System.currentTimeMillis() - panelSwitchAnimationStartTime;
		long duration = panelSwitchState == PanelSwitchState.EXITING ? PANEL_EXIT_ANIMATION_DURATION : PANEL_ENTER_ANIMATION_DURATION;
		return Mth.clamp((elapsed + (partialTick * 50)) / (float) duration, 0.0f, 1.0f);
	}

	private float easeOutBack(float t) {
		float c1 = 1.70158f;
		float c3 = c1 + 1.0f;
		float p = t - 1.0f;
		return 1.0f + c3 * p * p * p + c1 * p * p;
	}

	private float easeInBack(float t) {
		float c1 = 1.70158f;
		float c3 = c1 + 1.0f;
		return c3 * t * t * t - c1 * t * t;
	}

	protected int getAdjustedModelScale(int baseScale) {
		var player = Minecraft.getInstance().player;
		if (player == null) return baseScale;

		final float[] inverseScale = {1.0f};
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			var character = stats.getCharacter();

			Float[] resolved = character.getResolvedModelScaling();
			float currentScale = (resolved[0] + resolved[1]) / 2.0f;

			if (currentScale > 1.0f) inverseScale[0] = 0.9375f / currentScale;
		});

		return (int) (baseScale * inverseScale[0]);
	}

	protected void rebuildWidgetsWithoutTransition() {
		suppressOpenAnimationOnce = true;
		rebuildWidgets();
		suppressOpenAnimationOnce = false;
	}
}