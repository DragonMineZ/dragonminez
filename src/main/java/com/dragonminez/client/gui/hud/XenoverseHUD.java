package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Status;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static volatile float currentHPBarWidth = 0;
	private static volatile float currentKiBarWidth = 0;
	private static volatile float currentStmBarWidth = 0;
	private static volatile float lastSeenMaxHP = -1.0f;
	private static volatile int lastSeenMaxKi = -1;
	private static volatile int lastSeenMaxStm = -1;
	private static final float LERP_SPEED = 0.25f;
	private static final float HP_BAR_MAX_WIDTH = 137.0f;
	private static final float KI_BAR_MAX_WIDTH = 114.0f;
	private static final float STM_BAR_MAX_WIDTH = 85.0f;

	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	public static final IGuiOverlay HUD_XENOVERSE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (ConfigManager.getUserConfig().getAlternativeHud()) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			Character character = data.getCharacter();
			Status status = data.getStatus();
			Resources resources = data.getResources();

			if (status.isHasCreatedCharacter()) {
				float maxHP = Math.max(1.0f, (float) mc.player.getAttributeValue(Attributes.MAX_HEALTH));
				int maxKi = Math.max(1, data.getMaxEnergy());
				int maxStm = Math.max(1, data.getMaxStamina());

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

				float targetHPBarWidth = Mth.clamp(currentHP / maxHP, 0.0f, 1.0f) * HP_BAR_MAX_WIDTH;
				float targetKiBarWidth = Mth.clamp(currentKi / (float) maxKi, 0.0f, 1.0f) * KI_BAR_MAX_WIDTH;
				float targetStmBarWidth = Mth.clamp(currentStm / (float) maxStm, 0.0f, 1.0f) * STM_BAR_MAX_WIDTH;

				if (lastSeenMaxHP != maxHP) { currentHPBarWidth = targetHPBarWidth; lastSeenMaxHP = maxHP; }
				if (lastSeenMaxKi != maxKi) { currentKiBarWidth = targetKiBarWidth; lastSeenMaxKi = maxKi; }
				if (lastSeenMaxStm != maxStm) { currentStmBarWidth = targetStmBarWidth; lastSeenMaxStm = maxStm; }

				currentHPBarWidth += (targetHPBarWidth - currentHPBarWidth) * LERP_SPEED * partialTicks;
				currentKiBarWidth += (targetKiBarWidth - currentKiBarWidth) * LERP_SPEED * partialTicks;
				currentStmBarWidth += (targetStmBarWidth - currentStmBarWidth) * LERP_SPEED * partialTicks;

				if (Math.abs(currentHPBarWidth - targetHPBarWidth) <= 1) currentHPBarWidth = targetHPBarWidth;
				if (Math.abs(currentKiBarWidth - targetKiBarWidth) <= 1) currentKiBarWidth = targetKiBarWidth;
				if (Math.abs(currentStmBarWidth - targetStmBarWidth) <= 1) currentStmBarWidth = targetStmBarWidth;

				RenderSystem.enableBlend();
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				float baseScale = 2.25f;
				float baseWidth = 184.0f;
				float maxAllowedWidth = width * 0.50f;
				float finalScale = Math.min(baseScale, maxAllowedWidth / baseWidth);

				int anchorX = ConfigManager.getUserConfig().getXenoverseHudPosX();
				int anchorY = ConfigManager.getUserConfig().getXenoverseHudPosY();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(anchorX, anchorY, 0);
				guiGraphics.pose().scale(finalScale, finalScale, 1.0f);

				guiGraphics.blit(hud, 0, 0, 184, 10, 56, 25, 256, 256);

				guiGraphics.blit(hud, 31, 13, 14, 2, 141, 9, 256, 256);
				int hpV = (currentHP < maxHP * 0.33) ? 48 : (currentHP < maxHP * 0.66) ? 35 : 21;
				guiGraphics.blit(hud, 32, 15, 15, hpV, (int) currentHPBarWidth, 5, 256, 256);

				guiGraphics.blit(hud, 28, 21, 8, 65, 118, 8, 256, 256);
				float[] auraRgb = ColorUtils.hexToRgb(auraColor);
				RenderSystem.setShaderColor(auraRgb[0], auraRgb[1], auraRgb[2], 1.0f);
				guiGraphics.blit(hud, 29, 23, 9, 81, (int) currentKiBarWidth, 4, 256, 256);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				guiGraphics.blit(hud, 28, 28, 9, 105, 100, 7, 256, 256);
				guiGraphics.blit(hud, 43, 29, 24, 121, (int) currentStmBarWidth, 5, 256, 256);

				List<String> loadedRaces = ConfigManager.getDefaultRaces();
				int raceIndex = Math.max(0, loadedRaces.indexOf(raceName.toLowerCase()));
				int iconU = 1 + (raceIndex * 17);
				boolean isMajin = raceName.equalsIgnoreCase("majin");
				boolean isCustomRace = !loadedRaces.contains(raceName.toLowerCase());

				int raceY = isMajin ? 12 : 13;
				guiGraphics.blit(racialIcons, 15, raceY, isCustomRace ? 103 : iconU, 1, 16, 16, 256, 256);

				int fillHeight = (int) (16 * (Math.min(powerRelease, 100) / 100.0f));
				if (fillHeight > 0) guiGraphics.blit(racialIcons, 15, raceY + (16 - fillHeight), isCustomRace ? 103 : iconU, 18 + (16 - fillHeight), 16, fillHeight, 256, 256);

				guiGraphics.blit(hud, 8, 8, 218, 100, 26, 27, 256, 256);
				int fillFormHeight = (int) (17 * (formRelease / 100.0f));
				if (fillFormHeight > 0) guiGraphics.blit(hud, 10, 20 + (17 - fillFormHeight), 220, 130 + (17 - fillFormHeight), 26, fillFormHeight, 256, 256);

				drawScaledText(guiGraphics, powerRelease + "%", 7, 32, 0.5f, ColorUtils.hexToInt("#FACAF7"));

				if (ConfigManager.getUserConfig().getAdvancedDescription()) {
					boolean showPercent = ConfigManager.getUserConfig().getAdvancedDescriptionPercentage();

					String hpText = showPercent ? String.format("%.0f%%", (currentHP / maxHP) * 100) : numberFormat.format((int) currentHP) + " / " + numberFormat.format((int) maxHP);
					drawScaledText(guiGraphics, hpText, 100, 15, 0.5f, ColorUtils.hexToInt("#FFFFFF"));

					String kiText = showPercent ? String.format("%.0f%%", (currentKi / (float) maxKi) * 100) : numberFormat.format(currentKi) + " / " + numberFormat.format(maxKi);
					drawScaledText(guiGraphics, kiText, 90, 23, 0.5f, ColorUtils.hexToInt("#FFFFFF"));

					String stmText = showPercent ? String.format("%.0f%%", (currentStm / (float) maxStm) * 100) : numberFormat.format(currentStm) + " / " + numberFormat.format(maxStm);
					drawScaledText(guiGraphics, stmText, 80, 30, 0.5f, ColorUtils.hexToInt("#FFFFFF"));
				}

				guiGraphics.pose().popPose();
			}
		});
	};

	private static void drawScaledText(GuiGraphics guiGraphics, String text, int x, int y, float scale, int color) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0);
		guiGraphics.pose().scale(scale, scale, 1.0f);
		drawStringWithBorder(guiGraphics, text, 0, 0, color);
		guiGraphics.pose().popPose();
	}

	private static void drawStringWithBorder(GuiGraphics guiGraphics, String text, int x, int y, int color) {
		MutableComponent dmzText = Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x - 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x + 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x, y - 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x, y + 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x, y, color);
	}
}