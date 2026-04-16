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

public class AlternativeHUD {
	private static final ResourceLocation hud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/alternativehud.png");
	private static final ResourceLocation xvhud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/xenoversehud.png");
	private static final ResourceLocation racialIcons = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/racial_icons.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static volatile float currentHPBarWidth = 0;
	private static volatile float currentKiBarWidth = 0;
	private static volatile float currentStmBarWidth = 0;
	private static volatile float lastSeenMaxHP = -1.0f;
	private static volatile int lastSeenMaxKi = -1;
	private static volatile int lastSeenMaxStm = -1;
	private static final float LERP_SPEED = 0.25f;
	private static final float BAR_MAX_WIDTH = 76.0f;

	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	public static final IGuiOverlay HUD_ALTERNATIVE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (!ConfigManager.getUserConfig().getHud().getAlternativeHud()) return;

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

				if (formData != null && formData.getAuraColor() != null && !formData.getAuraColor().isEmpty()) {
					auraColor = formData.getAuraColor();
				}

				float currentHP = mc.player.getHealth();
				float currentKi = resources.getCurrentEnergy();
				float currentStm = resources.getCurrentStamina();

				float targetHPBarWidth = Mth.clamp(currentHP / maxHP, 0.0f, 1.0f) * BAR_MAX_WIDTH;
				float targetKiBarWidth = Mth.clamp(currentKi / (float) maxKi, 0.0f, 1.0f) * BAR_MAX_WIDTH;
				float targetStmBarWidth = Mth.clamp(currentStm / (float) maxStm, 0.0f, 1.0f) * BAR_MAX_WIDTH;

				if (lastSeenMaxHP != maxHP) { currentHPBarWidth = targetHPBarWidth; lastSeenMaxHP = maxHP; }
				if (lastSeenMaxKi != maxKi) { currentKiBarWidth = targetKiBarWidth; lastSeenMaxKi = maxKi; }
				if (lastSeenMaxStm != maxStm) { currentStmBarWidth = targetStmBarWidth; lastSeenMaxStm = maxStm; }

				currentHPBarWidth = lerp(currentHPBarWidth, targetHPBarWidth, partialTicks);
				currentKiBarWidth = lerp(currentKiBarWidth, targetKiBarWidth, partialTicks);
				currentStmBarWidth = lerp(currentStmBarWidth, targetStmBarWidth, partialTicks);

				RenderSystem.enableBlend();
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				float hudScale = 1.0f; // Escala vanilla
				int globalAnchorX = width / 2;
				int globalAnchorY = height;

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(globalAnchorX, globalAnchorY, 0);
				guiGraphics.pose().scale(hudScale, hudScale, 1.0f);

				// Coordenadas idénticas a las barras vanilla
				float baseHpX = -91.0f; float baseHpY = -39.0f;
				float baseKiX = -91.0f; float baseKiY = -49.0f;
				float baseStmX = 10.0f; float baseStmY = -39.0f;

				float hpOffX = ConfigManager.getUserConfig().getHud().getHealthBarPosX() / hudScale;
				float hpOffY = ConfigManager.getUserConfig().getHud().getHealthBarPosY() / hudScale;
				float kiOffX = ConfigManager.getUserConfig().getHud().getEnergyBarPosX() / hudScale;
				float kiOffY = ConfigManager.getUserConfig().getHud().getEnergyBarPosY() / hudScale;
				float stmOffX = ConfigManager.getUserConfig().getHud().getStaminaBarPosX() / hudScale;
				float stmOffY = ConfigManager.getUserConfig().getHud().getStaminaBarPosY() / hudScale;

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(baseHpX + hpOffX, baseHpY + hpOffY, 0);
				guiGraphics.blit(hud, 0, 0, 0, 0, 83, 9, 128, 128);
				int hpTextureV = (currentHP < maxHP * 0.33) ? 33 : (currentHP < maxHP * 0.66) ? 22 : 11;
				guiGraphics.blit(hud, 2, 3, 2, hpTextureV, 7 + (int) currentHPBarWidth, 5, 128, 128);
				drawTinyText(guiGraphics, powerRelease + "%", -4, 41, ColorUtils.hexToInt("#FACAF7"));
				drawBarValues(guiGraphics, currentHP, maxHP, 42, 3);
				guiGraphics.pose().popPose();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(baseKiX + kiOffX, baseKiY + kiOffY, 0);
				guiGraphics.blit(hud, 0, 0, 0, 44, 83, 9, 128, 128);
				float[] auraRgb = ColorUtils.hexToRgb(auraColor);
				RenderSystem.setShaderColor(auraRgb[0], auraRgb[1], auraRgb[2], 1.0f);
				guiGraphics.blit(hud, 3, 3, 3, 61, 7 + (int) currentKiBarWidth, 4, 128, 128);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				drawBarValues(guiGraphics, currentKi, maxKi, 42, 3);
				drawRacialIcon(guiGraphics, raceName, Math.min(powerRelease, 100), -22, 3);
				drawFormIcon(guiGraphics, formRelease, -22, 3);
				guiGraphics.pose().popPose();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(baseStmX + stmOffX, baseStmY + stmOffY, 0);
				guiGraphics.blit(hud, 0, 0, 0, 72, 83, 9, 128, 128);
				guiGraphics.blit(hud, 2, 3, 2, 90, -5 + (int) currentStmBarWidth, 4, 128, 128);
				guiGraphics.blit(hud, 77, 3, 77, 90, 4, 4, 128, 128);
				drawBarValues(guiGraphics, currentStm, maxStm, 41, 3);
				guiGraphics.pose().popPose();

				guiGraphics.pose().popPose();
			}
		});
	};

	private static void drawBarValues(GuiGraphics guiGraphics, float current, float max, int x, int y) {
		if (ConfigManager.getUserConfig().getHud().getAdvancedDescription()) {
			boolean pct = ConfigManager.getUserConfig().getHud().getAdvancedDescriptionPercentage();
			String text = pct ? String.format("%.0f%%", (current / max) * 100) : numberFormat.format((int) current) + " / " + numberFormat.format((int) max);
			drawTinyText(guiGraphics, text, x, y, ColorUtils.hexToInt("#FFFFFF"));
		}
	}

	private static void drawRacialIcon(GuiGraphics guiGraphics, String raceName, int powerRelease, int x, int y) {
		List<String> loadedRaces = ConfigManager.getDefaultRaces();
		int raceIndex = Math.max(0, loadedRaces.indexOf(raceName.toLowerCase()));
		int iconU = 1 + (raceIndex * 17);
		boolean isCustomRace = !loadedRaces.contains(raceName.toLowerCase());
		int fillHeight = (int) (16 * (powerRelease / 100.0f));

		guiGraphics.blit(racialIcons, x + 7, y + 4, isCustomRace ? 103 : iconU, 1, 16, 16, 256, 256);
		if (fillHeight > 0) guiGraphics.blit(racialIcons, x + 7, y + 4 + (16 - fillHeight), isCustomRace ? 103 : iconU, 18 + (16 - fillHeight), 16, fillHeight, 256, 256);

		RenderSystem.enableBlend();
		guiGraphics.blit(xvhud, x, y, 218, 100, 26, 27, 256, 256);
		RenderSystem.disableBlend();
	}

	private static void drawFormIcon(GuiGraphics guiGraphics, int formRelease, int x, int y) {
		int fillFormHeight = (int) (17 * (formRelease / 100.0f));
		if (fillFormHeight > 0) guiGraphics.blit(xvhud, x + 2, y + 12 + (17 - fillFormHeight), 220, 130 + (17 - fillFormHeight), 26, fillFormHeight, 256, 256);
	}

	private static void drawTinyText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, 0);
		guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
		drawStringWithBorder(guiGraphics, text, 0, 0, color);
		guiGraphics.pose().popPose();
	}

	private static float lerp(float start, float end, float delta) {
		float change = (end - start) * LERP_SPEED * delta;
		return Math.abs(end - start) <= 1 ? end : start + change;
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