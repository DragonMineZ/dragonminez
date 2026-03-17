package com.dragonminez.client.gui.character;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.ScaledScreen;
import com.dragonminez.client.gui.buttons.CustomTextureButton;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class BaseMenuScreen extends ScaledScreen {
	protected static boolean GLOBAL_SWITCHING = false;
	protected boolean isSwitchingMenu = false;
	private static final ResourceLocation SCREEN_BUTTONS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/buttons/menubuttons.png");
	private static final long OPEN_ANIMATION_DURATION = 200;
	private static final long PANEL_ENTER_ANIMATION_DURATION = 520;
	private static final long PANEL_EXIT_ANIMATION_DURATION = 140;
	private static final int PANEL_SWITCH_DISTANCE = 190;
	private static final int TOP_PANEL_SWITCH_DISTANCE = 90;

	private enum TransitionState { NONE, OPENING }

	private long animationStartTime;
	private TransitionState transitionState = TransitionState.NONE;

	private enum PanelSwitchState { NONE, ENTERING, EXITING }

	private long panelSwitchAnimationStartTime;
	private PanelSwitchState panelSwitchState = PanelSwitchState.NONE;
	private Screen pendingSwitchScreen;

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
		} else {
			startOpenTransition();
		}

		initNavigationButtons();
	}

	@Override
	public void tick() {
		super.tick();
		if (panelSwitchState == PanelSwitchState.ENTERING && getPanelSwitchProgress() >= 1.0f) {
			panelSwitchState = PanelSwitchState.NONE;
		}

		if (panelSwitchState == PanelSwitchState.EXITING && getPanelSwitchProgress() >= 1.0f) {
			panelSwitchState = PanelSwitchState.NONE;
			if (this.minecraft != null) {
				GLOBAL_SWITCHING = true;
				this.minecraft.setScreen(pendingSwitchScreen);
			}
		}

		if (transitionState == TransitionState.OPENING && getTransitionProgress() >= 1.0f) {
			transitionState = TransitionState.NONE;
		}
	}

	protected void initNavigationButtons() {
		int centerX = getUiWidth() / 2;
		int bottomY = getUiHeight() - 30;

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
						.onPress(btn -> switchMenu(new com.dragonminez.client.gui.quest.QuestTreeScreen()))
						.sound(MainSounds.UI_MENU_SWITCH.get())
						.build()
		);

		this.addRenderableWidget(
				new CustomTextureButton.Builder()
						.position(centerX + 50, bottomY)
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
		scrollPercent = net.minecraft.util.Mth.clamp(scrollPercent, 0.0f, 1.0f);
		return Math.round(scrollPercent * maxScrollValue);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public boolean isAnimating() {
		return transitionState == TransitionState.OPENING && getTransitionProgress() < 1.0f;
	}

	protected void applyZoom(GuiGraphics graphics) {
		if (transitionState != TransitionState.OPENING) return;

		float progress = getTransitionProgress();
		if (progress >= 1.0f) return;

		float scale = easeOutBack(progress);
		scale = Math.max(0.001f, scale);

		PoseStack pose = graphics.pose();
		int uiWidth = getUiWidth();
		int uiHeight = getUiHeight();
		pose.translate(uiWidth / 2.0, uiHeight / 2.0, 0);
		pose.scale(scale, scale, 1.0f);
		pose.translate(-uiWidth / 2.0, -uiHeight / 2.0, 0);
	}

	protected int getLeftPanelSwitchOffset() {
		if (panelSwitchState == PanelSwitchState.NONE) return 0;
		float p = getPanelSwitchProgress();
		if (panelSwitchState == PanelSwitchState.ENTERING) {
			float eased = easeOutBack(p);
			return Math.round((eased - 1.0f) * PANEL_SWITCH_DISTANCE);
		}
		float eased = easeInBack(p);
		return Math.round(-eased * PANEL_SWITCH_DISTANCE);
	}

	protected int getRightPanelSwitchOffset() {
		if (panelSwitchState == PanelSwitchState.NONE) return 0;
		float p = getPanelSwitchProgress();
		if (panelSwitchState == PanelSwitchState.ENTERING) {
			float eased = easeOutBack(p);
			return Math.round((1.0f - eased) * PANEL_SWITCH_DISTANCE);
		}
		float eased = easeInBack(p);
		return Math.round(eased * PANEL_SWITCH_DISTANCE);
	}

	protected int getTopPanelSwitchOffset() {
		if (panelSwitchState == PanelSwitchState.NONE) return 0;
		float p = getPanelSwitchProgress();
		if (panelSwitchState == PanelSwitchState.ENTERING) {
			float eased = easeOutBack(p);
			return Math.round((eased - 1.0f) * TOP_PANEL_SWITCH_DISTANCE);
		}
		float eased = easeInBack(p);
		return Math.round(-eased * TOP_PANEL_SWITCH_DISTANCE);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (panelSwitchState == PanelSwitchState.EXITING) {
			return true;
		}
		if (keyCode == 256) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(null);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (panelSwitchState == PanelSwitchState.EXITING) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (panelSwitchState == PanelSwitchState.EXITING) {
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (panelSwitchState == PanelSwitchState.EXITING) {
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (panelSwitchState == PanelSwitchState.EXITING) {
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

	private float getTransitionProgress() {
		long elapsed = System.currentTimeMillis() - animationStartTime;
		return net.minecraft.util.Mth.clamp(elapsed / (float) OPEN_ANIMATION_DURATION, 0.0f, 1.0f);
	}

	private float getPanelSwitchProgress() {
		long elapsed = System.currentTimeMillis() - panelSwitchAnimationStartTime;
		long duration = panelSwitchState == PanelSwitchState.EXITING
				? PANEL_EXIT_ANIMATION_DURATION
				: PANEL_ENTER_ANIMATION_DURATION;
		return net.minecraft.util.Mth.clamp(elapsed / (float) duration, 0.0f, 1.0f);
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
			var activeForm = character.getActiveFormData();

			float currentScale;
			if (activeForm != null) {
				Float[] formScaling = activeForm.getModelScaling();
				Float[] charScaling = character.getModelScaling();
				currentScale = (formScaling[0] * charScaling[0] + formScaling[1] * charScaling[1]) / 2.0f;
			} else {
				Float[] charScaling = character.getModelScaling();
				currentScale = (charScaling[0] + charScaling[1]) / 2.0f;
			}

			if (currentScale > 1.0f) inverseScale[0] = 0.9375f / currentScale;
		});

		return (int) (baseScale * inverseScale[0]);
	}
}
