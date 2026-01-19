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

public class AlternativeHUD {
	private static final ResourceLocation hud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/alternativehud.png"),
			xvhud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/xenoversehud.png"),
			racialIcons = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/racial_icons.png");

	private static volatile float currentHPBarWidth = 0;
	private static volatile float currentKiBarWidth = 0;
	private static volatile float currentStmBarWidth = 0;
	private static final float LERP_SPEED = 0.25f;

	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	public static final IGuiOverlay HUD_ALTERNATIVE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (Minecraft.getInstance().options.renderDebug || Minecraft.getInstance().player == null) return;
		if (!ConfigManager.getUserConfig().getHud().isAlternativeHud()) return;


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
				if (resources.getActionCharge() < 20) formRelease = 20 + resources.getActionCharge();
				else formRelease = resources.getActionCharge();
				String raceName = character.getRaceName();
				String auraColor = character.getAuraColor();

				float currentHP = Minecraft.getInstance().player.getHealth();
				int currentKi = resources.getCurrentEnergy();
				int currentStm = resources.getCurrentStamina();

				float targetHPBarWidth = (currentHP / maxHP) * 76;
				float targetKiBarWidth = (currentKi / (float) maxKi) * 76;
				float targetStmBarWidth = (currentStm / (float) maxStm) * 76;

				currentHPBarWidth = lerp(currentHPBarWidth, targetHPBarWidth, partialTicks);
				currentKiBarWidth = lerp(currentKiBarWidth, targetKiBarWidth, partialTicks);
				currentStmBarWidth = lerp(currentStmBarWidth, targetStmBarWidth, partialTicks);

				if (currentHP == maxHP) currentHPBarWidth = 76;
				if (currentKi == maxKi) currentKiBarWidth = 76;
				if (currentStm == maxStm) currentStmBarWidth = 76;

				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				RenderSystem.setShader(GameRenderer::getPositionTexShader);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				int midX = width / 2;
				int bottomY = height;
				int leftBaseX = midX - 91;
				int rightBaseX = midX + 10;

				int row1Y = bottomY - 39;
				int row2Y = bottomY - 49;

				int hpX = leftBaseX + ConfigManager.getUserConfig().getHud().getHealthBarPosX();
				int hpY = row1Y + ConfigManager.getUserConfig().getHud().getHealthBarPosY();

				int kiX = leftBaseX + ConfigManager.getUserConfig().getHud().getEnergyBarPosX();
				int kiY = row2Y + ConfigManager.getUserConfig().getHud().getEnergyBarPosY();

				int stmX = rightBaseX + ConfigManager.getUserConfig().getHud().getStaminaBarPosX();
				int stmY = row2Y + ConfigManager.getUserConfig().getHud().getStaminaBarPosY();

				float userScale = 1.8f;

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(hpX, hpY, 0);
				guiGraphics.pose().scale(userScale - 0.5f, userScale - 0.5f, 1.0f);

				RenderSystem.setShaderTexture(0, hud);
				guiGraphics.blit(hud, - 18, - 17, 0, 0, 83, 9, 128, 128);

				int hpTextureV = (currentHP < maxHP * 0.33) ? 33 : (currentHP < maxHP * 0.66) ? 22 : 11;
				guiGraphics.blit(hud, - 16, - 14, 2, hpTextureV, 7 + (int) currentHPBarWidth, 5, 128, 128);

				drawTinyText(guiGraphics, powerRelease + "%", -20, 24, ColorUtils.hexToInt("#FACAF7"));
				drawBarValues(guiGraphics, currentHP, maxHP, 24, -14);

				guiGraphics.pose().popPose();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(kiX, kiY, 0);
				guiGraphics.pose().scale(userScale - 0.5f, userScale - 0.5f, 1.0f);

				RenderSystem.setShaderTexture(0, hud);
				guiGraphics.blit(hud, - 18, - 11, 0, 44, 83, 9, 128, 128);

				float[] auraRgb = ColorUtils.hexToRgb(auraColor);
				RenderSystem.setShaderColor(auraRgb[0], auraRgb[1], auraRgb[2], 1.0f);
				guiGraphics.blit(hud, - 15,  - 8, 3, 61, 7 + (int) currentKiBarWidth, 4, 128, 128);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				drawBarValues(guiGraphics, currentKi, maxKi, 24, -8);

				guiGraphics.pose().scale(userScale - 0.3f, userScale - 0.3f, 1.0f);
				drawFormIcon(guiGraphics, formRelease, -40, -8);
				drawRacialIcon(guiGraphics, raceName, powerRelease, -40, -8);

				guiGraphics.pose().popPose();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(stmX, stmY, 0);
				guiGraphics.pose().scale(userScale - 0.5f, userScale - 0.5f, 1.0f);

				RenderSystem.setShaderTexture(0, hud);
				guiGraphics.blit(hud, - 12, - 11, 0, 72, 83, 9, 128, 128);
				guiGraphics.blit(hud, - 10, - 8, 2, 90, -5 + (int) currentStmBarWidth, 4, 128, 128);
				guiGraphics.blit(hud, 65, - 8, 77, 90, 4, 4, 128, 128);

				drawBarValues(guiGraphics, currentStm, maxStm, 29, -8);

				guiGraphics.pose().popPose();
			}
		});
	};

	private static void drawBarValues(GuiGraphics guiGraphics, float current, float max, int x, int y) {
		if (ConfigManager.getUserConfig().getHud().isAdvancedDescription()) {
			boolean percentage = ConfigManager.getUserConfig().getHud().isAdvancedDescriptionPercentage();
			String text;
			if (percentage) {
				text = String.format("%.0f", (current / max) * 100) + "%";
			} else {
				text = numberFormat.format((int) current) + " / " + numberFormat.format((int) max);
			}
			drawTinyText(guiGraphics, text, x, y, ColorUtils.hexToInt("#FFFFFF"));
		}
	}

	private static void drawRacialIcon(GuiGraphics guiGraphics, String raceName, int powerRelease, int x, int y) {
		RenderSystem.setShaderTexture(0, racialIcons);
		List<String> loadedRaces = ConfigManager.getLoadedRaces();
		int raceIndex = Math.max(0, loadedRaces.indexOf(raceName.toLowerCase()));
		int iconU = 1 + (raceIndex * 17);
		guiGraphics.blit(racialIcons, raceName.equalsIgnoreCase("majin") ? x + 6 : x + 7, y + 4, iconU, 1, 16, 16);
		float fillRatio = powerRelease / 100.0f;
		int fillHeight = (int) (16 * fillRatio);
		if (fillHeight > 0) {
			guiGraphics.blit(racialIcons, x+7, y +4+ (16 - fillHeight), iconU, 18 + (16 - fillHeight), 16, fillHeight);
		}
		RenderSystem.enableBlend();
		RenderSystem.setShaderTexture(0, xvhud);
		guiGraphics.blit(xvhud, x, y, 218, 100, 26, 27);
		RenderSystem.disableBlend();
	}

	private static void drawFormIcon(GuiGraphics guiGraphics, int formRelease, int x, int y) {
		RenderSystem.setShaderTexture(0, xvhud);
		float fillFormRatio = formRelease / 100.0f;
		int fillFormHeight = (int) (17 * fillFormRatio);
		if (fillFormHeight > 0) {
			guiGraphics.blit(xvhud, x + 2, y + 12 + (17 - fillFormHeight), 220, 130 + (17 - fillFormHeight), 26, fillFormHeight);
		}
	}

	private static void drawTinyText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
		drawStringWithBorder(guiGraphics, text, x * 2, y * 2, color);
		guiGraphics.pose().popPose();
	}

	private static float lerp(float start, float end, float delta) {
		float change = (end - start) * LERP_SPEED * delta;
		if (Math.abs(end - start) <= 1) return end;
		return start + change;
	}

	private static void drawStringWithBorder(GuiGraphics guiGraphics, String text, int x, int y, int color) {
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x - 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x + 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y - 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y + 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y, color);
	}
}