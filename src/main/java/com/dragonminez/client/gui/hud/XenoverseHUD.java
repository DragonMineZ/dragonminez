package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.Character;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class XenoverseHUD {
	private static final ResourceLocation hud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/xenoversehud.png"),
			racialIcons = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/racial_icons.png");

	private static volatile float currentHPBarWidth = 0;
	private static volatile float currentKiBarWidth = 0;
	private static volatile float currentStmBarWidth = 0;
	private static final float LERP_SPEED = 0.25f;

	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	public static final IGuiOverlay HUD_XENOVERSE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (Minecraft.getInstance().options.renderDebug || Minecraft.getInstance().player == null) return;
		if (ConfigManager.getUserConfig().getHud().isAlternativeHud()) return;
		StatsProvider.get(StatsCapability.INSTANCE, Minecraft.getInstance().player).ifPresent(data -> {
			Character character = data.getCharacter();
			Status status = data.getStatus();
			Resources resources = data.getResources();

			if (status.hasCreatedCharacter()) {
				float maxHP = Minecraft.getInstance().player.getMaxHealth();
				int maxKi = data.getMaxEnergy();
				int maxStm = data.getMaxStamina();
				int powerRelease = resources.getPowerRelease();
				int formRelease;
				if (resources.getActionCharge() < 10) formRelease = 10 + resources.getActionCharge();
				else formRelease = resources.getActionCharge();
				String raceName = character.getRaceName();
				String auraColor = character.getAuraColor();

				float currentHP = Minecraft.getInstance().player.getHealth();
				int currentKi = resources.getCurrentEnergy();
				int currentStm = resources.getCurrentStamina();

				float targetHPBarWidth = (currentHP / maxHP) * 137;
				float targetKiBarWidth = (currentKi / (float) maxKi) * 114;
				float targetStmBarWidth = (currentStm / (float) maxStm) * 85;

				float lerpedHPWidth = currentHPBarWidth + (targetHPBarWidth - currentHPBarWidth) * LERP_SPEED * partialTicks;
				float lerpedKiWidth = currentKiBarWidth + (targetKiBarWidth - currentKiBarWidth) * LERP_SPEED * partialTicks;
				float lerpedStmWidth = currentStmBarWidth + (targetStmBarWidth - currentStmBarWidth) * LERP_SPEED * partialTicks;

				currentHPBarWidth = lerpedHPWidth;
				currentKiBarWidth = lerpedKiWidth;
				currentStmBarWidth = lerpedStmWidth;

				if (currentHP == maxHP) {
					currentHPBarWidth = 137;
				} else if (Math.abs(currentHPBarWidth - targetHPBarWidth) <= 1) {
					currentHPBarWidth = targetHPBarWidth;
				}

				if (currentKi == maxKi) {
					currentKiBarWidth = 114;
				} else if (Math.abs(currentKiBarWidth - targetKiBarWidth) <= 1) {
					currentKiBarWidth = targetKiBarWidth;
				}

				if (currentStm == maxStm) {
					currentStmBarWidth = 85;
				} else if (Math.abs(currentStmBarWidth - targetStmBarWidth) <= 1) {
					currentStmBarWidth = targetStmBarWidth;
				}

				RenderSystem.enableBlend();
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				RenderSystem.setShaderTexture(0, hud);

				guiGraphics.pose().pushPose();
				float configScale = 1.8f;
				float correctionFactor = HUDManager.getScaleFactor();
				float finalScale = configScale * correctionFactor;
				guiGraphics.pose().scale(finalScale, finalScale, 1.0f);

				int initialX = ConfigManager.getUserConfig().getHud().getXenoverseHudPosX();
				int initialY = ConfigManager.getUserConfig().getHud().getXenoverseHudPosY();

				// Nimbus Background
				guiGraphics.blit(hud, initialX, initialY, 184, 10, 56, 25);

				// HP Bar
				guiGraphics.blit(hud, initialX + 31, initialY + 13, 14, 2, 141, 9);
				if (currentHP < maxHP * 0.33) {
					guiGraphics.blit(hud, initialX + 32, initialY + 15, 15, 48, (int) currentHPBarWidth, 5);
				} else if (currentHP < maxHP * 0.66) {
					guiGraphics.blit(hud, initialX + 32, initialY + 15, 15, 35, (int) currentHPBarWidth, 5);
				} else {
					guiGraphics.blit(hud, initialX + 32, initialY + 15, 15, 21, (int) currentHPBarWidth, 5);
				}

				// Ki Bar with Aura Color
				guiGraphics.blit(hud, initialX + 28, initialY + 21, 8, 65, 118, 8);

				float[] auraRgb = ColorUtils.hexToRgb(auraColor);
				RenderSystem.setShaderColor(auraRgb[0], auraRgb[1], auraRgb[2], 1.0f);
				guiGraphics.blit(hud, initialX + 29, initialY + 23, 9, 81, (int) currentKiBarWidth, 4);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				// Stamina Bar
				guiGraphics.blit(hud, initialX + 28, initialY + 28, 9, 105, 100, 7);
				guiGraphics.blit(hud, initialX + 43, initialY + 29, 24, 121, (int) currentStmBarWidth, 5);

				// Racial Icon / Power Release
				RenderSystem.setShaderTexture(0, racialIcons);
				List<String> loadedRaces = ConfigManager.getLoadedRaces();
				int raceIndex = loadedRaces.indexOf(raceName.toLowerCase());

				if (raceIndex < 0) {
					raceIndex = 0;
				}

				int iconU = 1 + (raceIndex * 17);
				int baseIconV = 1;

				boolean isMajin = raceName.equalsIgnoreCase("majin");

				guiGraphics.blit(racialIcons, initialX + 15, isMajin ? (initialY + 12) : (initialY + 13), iconU, baseIconV, 16, 16);

				float fillRatio = powerRelease / 100.0f;
				int fillHeight = (int) (16 * fillRatio);

				if (fillHeight > 0) {
					int fillIconV = 18;
					int screenY = (isMajin ? (initialY + 12) : (initialY + 13)) + (16 - fillHeight);
					int textureV = fillIconV + (16 - fillHeight);
					guiGraphics.blit(racialIcons, initialX + 15, screenY, iconU, textureV, 16, fillHeight);
				}

				// Radar & Form Release
				RenderSystem.setShaderTexture(0, hud);
				guiGraphics.blit(hud, initialX + 8, initialY + 8, 218, 100, 26, 27);
				float fillFormRatio = formRelease / 100.0f;
				int fillFormHeight = (int) (17 * fillFormRatio);
				if (fillFormHeight > 0) {
					guiGraphics.blit(hud, initialX + 10, initialY + 20 + (17 - fillFormHeight), 220, 130 + (17 - fillFormHeight), 26, fillFormHeight);
				}

				float finalTextScale = 0.5f;

				guiGraphics.pose().pushPose();
				guiGraphics.pose().scale(finalTextScale, finalTextScale, 1.0f);

				drawStringWithBorder(guiGraphics, powerRelease + "%", (int)((initialX + 7) / finalTextScale), (int)((initialY + 32) / finalTextScale), ColorUtils.hexToInt("#FACAF7"));

				if (ConfigManager.getUserConfig().getHud().isAdvancedDescription()) {
					boolean showPercent = ConfigManager.getUserConfig().getHud().isAdvancedDescriptionPercentage();

					String hpText = showPercent ? String.format("%.0f", (currentHP / maxHP) * 100) + "%" : numberFormat.format((int) currentHP) + " / " + numberFormat.format((int) maxHP);
					drawStringWithBorder(guiGraphics, hpText, (int)((initialX + 100) / finalTextScale), (int)((initialY + 15) / finalTextScale), ColorUtils.hexToInt("#FFFFFF"));

					String kiText = showPercent ? String.format("%.0f", (currentKi / (float) maxKi) * 100) + "%" : numberFormat.format(currentKi) + " / " + numberFormat.format(maxKi);
					drawStringWithBorder(guiGraphics, kiText, (int)((initialX + 90) / finalTextScale), (int)((initialY + 23) / finalTextScale), ColorUtils.hexToInt("#FFFFFF"));

					String stmText = showPercent ? String.format("%.0f", (currentStm / (float) maxStm) * 100) + "%" : numberFormat.format(currentStm) + " / " + numberFormat.format(maxStm);
					drawStringWithBorder(guiGraphics, stmText, (int)((initialX + 80) / finalTextScale), (int)((initialY + 30) / finalTextScale), ColorUtils.hexToInt("#FFFFFF"));
				}

				guiGraphics.pose().popPose();
				guiGraphics.pose().popPose();
			}
		});
	};

	private static void drawStringWithBorder(GuiGraphics guiGraphics, String text, int x, int y, int color) {
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x - 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x + 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y - 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y + 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y, color);
	}
}
