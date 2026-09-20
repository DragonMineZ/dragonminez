package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.client.util.TextUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Status;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class XenoverseHUD {
	private static final ResourceLocation hud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/xenoversehud.png");
	private static final ResourceLocation racialIcons = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/racial_icons.png");

	private static final HudBarAnimator HP_BAR = new HudBarAnimator();
	private static final HudBarAnimator KI_BAR = new HudBarAnimator();
	private static final HudBarAnimator STM_BAR = new HudBarAnimator();
	private static final HudSmoother POWER_RELEASE = new HudSmoother(0.10f, 0.05f);
	private static volatile float lastSeenMaxHP = -1.0f;
	private static volatile float lastSeenMaxKi = -1;
	private static volatile float lastSeenMaxStm = -1;
	private static final float HP_BAR_MAX_WIDTH = 137.0f;
	private static final float KI_BAR_MAX_WIDTH = 114.0f;
	private static final float STM_BAR_MAX_WIDTH = 85.0f;
	private static final float SURGE_TINT = 0.45f;
	private static final int STM_LABEL_WIDTH = 14;
	private static float mirrorWidth = -1.0f;
	private static final HudStatNumberAnimator HP_NUMBER = new HudStatNumberAnimator(HudStatNumberAnimator.StatKind.HEALTH);
	private static final HudStatNumberAnimator KI_NUMBER = new HudStatNumberAnimator(HudStatNumberAnimator.StatKind.KI);
	private static final HudStatNumberAnimator STM_NUMBER = new HudStatNumberAnimator(HudStatNumberAnimator.StatKind.STAMINA);

	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	public static final IGuiOverlay HUD_XENOVERSE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (HudStyle.current() != HudStyle.LEGACY_1) return;
		HudLayout.Box box = HudLayout.resolve(HudElement.MAIN, width, height);
		if (!box.visible() && !HudLayout.isPreview()) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			Character character = data.getCharacter();
			Status status = data.getStatus();
			Resources resources = data.getResources();

			if (status.isHasCreatedCharacter()) {
				float maxHP = Math.max(1.0f, (float) mc.player.getAttributeValue(Attributes.MAX_HEALTH));
				float maxKi = Math.max(1, data.getMaxEnergy());
				float maxStm = Math.max(1, data.getMaxStamina());

				int powerRelease = resources.getPowerRelease();
				int formRelease = resources.getActionCharge() < 10 ? 10 + resources.getActionCharge() : resources.getActionCharge();
				String raceName = character.getRaceName();
				String auraColor = character.getAuraColor();

				FormConfig.FormData formData = character.getActiveStackForm() != null && !character.getActiveStackForm().isEmpty() ? character.getActiveStackFormData() :
						character.getActiveForm() != null && !character.getActiveForm().isEmpty() ? character.getActiveFormData() : null;

				if (formData != null && formData.getAuraColor() != null && !formData.getAuraColor().isEmpty()) auraColor = formData.getAuraColor();

				float currentHP = mc.player.getHealth();
				float currentKi = resources.getCurrentEnergy();
				float currentStm = resources.getCurrentStamina();

				float hpFraction = Mth.clamp(currentHP / maxHP, 0.0f, 1.0f);
				float kiFraction = Mth.clamp(currentKi / (float) maxKi, 0.0f, 1.0f);
				float stmFraction = Mth.clamp(currentStm / (float) maxStm, 0.0f, 1.0f);

				if (lastSeenMaxHP != maxHP) { HP_BAR.reset(hpFraction); lastSeenMaxHP = maxHP; }
				if (lastSeenMaxKi != maxKi) { KI_BAR.reset(kiFraction); lastSeenMaxKi = maxKi; }
				if (lastSeenMaxStm != maxStm) { STM_BAR.reset(stmFraction); lastSeenMaxStm = maxStm; }

				HP_BAR.update(hpFraction);
				KI_BAR.update(kiFraction);
				STM_BAR.update(stmFraction);
				float displayPowerRelease = POWER_RELEASE.update(powerRelease);

				float currentHPBarWidth = HP_BAR.frontFraction() * HP_BAR_MAX_WIDTH;
				float currentKiBarWidth = KI_BAR.frontFraction() * KI_BAR_MAX_WIDTH;
				float currentStmBarWidth = STM_BAR.frontFraction() * STM_BAR_MAX_WIDTH;

				RenderSystem.enableBlend();
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				float finalScale = box.scale();
				mirrorWidth = box.mirrored() ? box.width() / box.scale() : -1.0f;
				float anchorX = box.x();
				float anchorY = box.y();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(anchorX, anchorY, 0);
				guiGraphics.pose().scale(finalScale, finalScale, 1.0f);

				part(guiGraphics, hud, 0, 0, 184, 10, 56, 25);

				part(guiGraphics, hud, 31, 13, 14, 2, 141, 9);
				int hpV = (currentHP < maxHP * 0.33) ? 48 : (currentHP < maxHP * 0.66) ? 35 : 21;
				drawHpChip(guiGraphics, 32, 15, 15, hpV, currentHPBarWidth, HP_BAR.ghostFraction() * HP_BAR_MAX_WIDTH, HP_BAR.gapType(), 5);
				part(guiGraphics, hud, 32, 15, 15, hpV, currentHPBarWidth, 5);

				part(guiGraphics, hud, 28, 21, 8, 65, 118, 8);
				float[] auraRgb = ColorUtils.hexToRgb(auraColor);
				RenderSystem.setShaderColor(auraRgb[0], auraRgb[1], auraRgb[2], 1.0f);
				part(guiGraphics, hud, 29, 23, 9, 81, currentKiBarWidth, 4);

				float surgeFraction = SurgeBarState.fraction(data);
				if (surgeFraction > 0.0f) {
					float surgeWidth = surgeFraction * KI_BAR_MAX_WIDTH;
					if (surgeWidth > 0.0f) {
						RenderSystem.setShaderColor(auraRgb[0] * SURGE_TINT, auraRgb[1] * SURGE_TINT, auraRgb[2] * SURGE_TINT, 1.0f);
						part(guiGraphics, hud, 29, 23, 9, 81, surgeWidth, 4);
					}
				}

				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				if (mirrorWidth >= 0.0f) {
					HudRender.blit(guiGraphics, hud, mirrorWidth - 28 - STM_LABEL_WIDTH, 28, 9, 105, STM_LABEL_WIDTH, 7, 256, 256);
					part(guiGraphics, hud, 28 + STM_LABEL_WIDTH, 28, 9 + STM_LABEL_WIDTH, 105, 100 - STM_LABEL_WIDTH, 7);
				} else {
					part(guiGraphics, hud, 28, 28, 9, 105, 100, 7);
				}
				part(guiGraphics, hud, 43, 29, 24, 121, currentStmBarWidth, 5);

				List<String> loadedRaces = ConfigManager.getDefaultRaces();
				int raceIndex = Math.max(0, loadedRaces.indexOf(raceName.toLowerCase()));
				int iconU = 1 + (raceIndex * 17);
				boolean isMajin = raceName.equalsIgnoreCase("majin");
				boolean isCustomRace = !loadedRaces.contains(raceName.toLowerCase());

				int raceY = isMajin ? 12 : 13;
				part(guiGraphics, racialIcons, 15, raceY, isCustomRace ? 103 : iconU, 1, 16, 16);

				int fillHeight = (int) (16 * (Math.min(displayPowerRelease, 100.0f) / 100.0f));
				if (fillHeight > 0) part(guiGraphics, racialIcons, 15, raceY + (16 - fillHeight), isCustomRace ? 103 : iconU, 18 + (16 - fillHeight), 16, fillHeight);

				part(guiGraphics, hud, 8, 8, 218, 100, 26, 27);
				int fillFormHeight = (int) (17 * (formRelease / 100.0f));
				if (fillFormHeight > 0) part(guiGraphics, hud, 10, 20 + (17 - fillFormHeight), 220, 130 + (17 - fillFormHeight), 26, fillFormHeight);

				drawScaledText(guiGraphics, Math.round(displayPowerRelease) + "%", mirrorX(7), 32, 0.5f, ColorUtils.hexToInt("#FACAF7"));

				if (ConfigManager.getUserConfig().getAdvancedDescription()) {
					boolean showPercent = ConfigManager.getUserConfig().getAdvancedDescriptionPercentage();
					float tickTime = mc.player.tickCount + partialTicks;

					String hpText = showPercent ? String.format("%.0f%%", (currentHP / maxHP) * 100) : numberFormat.format(Math.round((double) currentHP)) + " / " + numberFormat.format(Math.round((double) maxHP));
					drawAnimatedScaledText(guiGraphics, HP_NUMBER, hpText, displayValue(currentHP, maxHP, showPercent), tickTime, mirrorX(100), 15, 0.5f);

					String kiText = showPercent ? String.format("%.0f%%", (currentKi / (float) maxKi) * 100) : numberFormat.format(Math.round((double) currentKi)) + " / " + numberFormat.format(Math.round((double) maxKi));
					drawAnimatedScaledText(guiGraphics, KI_NUMBER, kiText, displayValue(currentKi, maxKi, showPercent), tickTime, mirrorX(90), 23, 0.5f);

					String stmText = showPercent ? String.format("%.0f%%", (currentStm / (float) maxStm) * 100) : numberFormat.format(Math.round((double) currentStm)) + " / " + numberFormat.format(Math.round((double) maxStm));
					drawAnimatedScaledText(guiGraphics, STM_NUMBER, stmText, displayValue(currentStm, maxStm, showPercent), tickTime, mirrorX(80), 30, 0.5f);
				}

				guiGraphics.pose().popPose();
			}
		});
	}

	private static void drawHpChip(GuiGraphics guiGraphics, int x, int y, int u, int v, float front, float ghost, HudBarAnimator.GapType gap, int height) {
		if (gap == HudBarAnimator.GapType.NONE) return;
		float start = Math.min(front, ghost);
		float chipWidth = Math.max(front, ghost) - start;
		if (chipWidth <= 0.0f) return;

		if (gap == HudBarAnimator.GapType.DAMAGE) RenderSystem.setShaderColor(1.0f, 0.24f, 0.24f, 1.0f);
		else RenderSystem.setShaderColor(0.34f, 1.0f, 0.42f, 1.0f);
		part(guiGraphics, hud, x + start, y, u + start, v, chipWidth, height);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static void part(GuiGraphics guiGraphics, ResourceLocation texture, float x, float y, float u, float v, float width, float height) {
		if (mirrorWidth < 0.0f) HudRender.blit(guiGraphics, texture, x, y, u, v, width, height, 256, 256);
		else HudRender.blit(guiGraphics, texture, mirrorWidth - x - width, y, u + width, v, width, height, -width, height, 256, 256);
	}

	private static int mirrorX(int x) {
		return mirrorWidth < 0.0f ? x : Math.round(mirrorWidth - x);
	}

	private static void drawScaledText(GuiGraphics guiGraphics, String text, int x, int y, float scale, int color) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0);
		guiGraphics.pose().scale(scale, scale, 1.0f);
		TextUtil.drawCenteredStringWithBorder(guiGraphics, Minecraft.getInstance().font, text, 0, 0, color);
		guiGraphics.pose().popPose();
	}

	private static void drawAnimatedScaledText(GuiGraphics guiGraphics, HudStatNumberAnimator animator, String text, float value, float tickTime, int x, int y, float scale) {
		HudStatNumberAnimator.RenderState state = animator.update(text, value, tickTime);
		if (state.isHidden()) return;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x + state.offsetX(), y + state.offsetY(), 0);
		guiGraphics.pose().scale(scale, scale, 1.0f);
		drawFadingString(guiGraphics, text, 0, 0, withAlpha(state.rgbColor(), state.alpha()), true);
		guiGraphics.pose().popPose();
	}

	private static void drawFadingString(GuiGraphics guiGraphics, String text, int x, int y, int color, boolean centered) {
		int alpha = (color >>> 24) & 0xFF;
		if (alpha <= 2) return;
		int borderCol = alpha << 24;
		var font = Minecraft.getInstance().font;
		int dx = centered ? -font.width(text) / 2 : 0;

		guiGraphics.drawString(font, text, x + dx - 1, y, borderCol, false);
		guiGraphics.drawString(font, text, x + dx + 1, y, borderCol, false);
		guiGraphics.drawString(font, text, x + dx, y - 1, borderCol, false);
		guiGraphics.drawString(font, text, x + dx, y + 1, borderCol, false);
		guiGraphics.drawString(font, text, x + dx, y, color, false);
	}

	private static float displayValue(float current, float max, boolean showPercent) {
		return showPercent ? Math.round((current / max) * 100.0f) : Math.round(current);
	}

	private static int withAlpha(int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
		return (alphaChannel << 24) | (rgb & 0xFFFFFF);
	}
}