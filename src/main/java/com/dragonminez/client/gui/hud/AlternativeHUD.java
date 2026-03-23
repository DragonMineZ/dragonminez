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
import net.minecraft.client.renderer.GameRenderer;
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
	private static final ResourceLocation hud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/alternativehud.png"),
			xvhud = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/xenoversehud.png"),
			racialIcons = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/racial_icons.png");
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
				float maxHP = (float) mc.player.getAttributeValue(Attributes.MAX_HEALTH);
				if (maxHP <= 0.0f) maxHP = mc.player.getMaxHealth();
				maxHP = Math.max(1.0f, maxHP);

				int maxKi = Math.max(1, data.getMaxEnergy());
				int maxStm = Math.max(1, data.getMaxStamina());
				int powerRelease = resources.getPowerRelease();
				int formRelease;
				if (resources.getActionCharge() < 10) formRelease = 10 + resources.getActionCharge();
				else formRelease = resources.getActionCharge();
				String raceName = character.getRaceName();
				String auraColor = character.getAuraColor();

				FormConfig.FormData formData = null;
				if (character.getActiveStackForm() != null && !character.getActiveStackForm().isEmpty()) {
					formData = character.getActiveStackFormData();
				} else if (character.getActiveForm() != null && !character.getActiveForm().isEmpty()) {
					formData = character.getActiveFormData();
				}
				if (formData != null && formData.getAuraColor() != null && !formData.getAuraColor().isEmpty()) {
					auraColor = formData.getAuraColor();
				}

				float currentHP = mc.player.getHealth();
				int currentKi = resources.getCurrentEnergy();
				int currentStm = resources.getCurrentStamina();

				float hpRatio = Mth.clamp(currentHP / maxHP, 0.0f, 1.0f);
				float kiRatio = Mth.clamp(currentKi / (float) maxKi, 0.0f, 1.0f);
				float stmRatio = Mth.clamp(currentStm / (float) maxStm, 0.0f, 1.0f);

				float targetHPBarWidth = hpRatio * BAR_MAX_WIDTH;
				float targetKiBarWidth = kiRatio * BAR_MAX_WIDTH;
				float targetStmBarWidth = stmRatio * BAR_MAX_WIDTH;

				if (lastSeenMaxHP != maxHP) {
					currentHPBarWidth = targetHPBarWidth;
					lastSeenMaxHP = maxHP;
				}
				if (lastSeenMaxKi != maxKi) {
					currentKiBarWidth = targetKiBarWidth;
					lastSeenMaxKi = maxKi;
				}
				if (lastSeenMaxStm != maxStm) {
					currentStmBarWidth = targetStmBarWidth;
					lastSeenMaxStm = maxStm;
				}

				currentHPBarWidth = lerp(currentHPBarWidth, targetHPBarWidth, partialTicks);
				currentKiBarWidth = lerp(currentKiBarWidth, targetKiBarWidth, partialTicks);
				currentStmBarWidth = lerp(currentStmBarWidth, targetStmBarWidth, partialTicks);

				if (currentHP >= maxHP - 0.001f) currentHPBarWidth = BAR_MAX_WIDTH;
				if (currentKi == maxKi) currentKiBarWidth = BAR_MAX_WIDTH;
				if (currentStm == maxStm) currentStmBarWidth = BAR_MAX_WIDTH;

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
				guiGraphics.blit(hud, -18, -17, 0, 0, 83, 9, 128, 128);

				int hpTextureV = (currentHP < maxHP * 0.33) ? 33 : (currentHP < maxHP * 0.66) ? 22 : 11;
				guiGraphics.blit(hud, -16, -14, 2, hpTextureV, 7 + Math.min((int) currentHPBarWidth, (int) BAR_MAX_WIDTH), 5, 128, 128);

				drawTinyText(guiGraphics, powerRelease + "%", -20, 24, ColorUtils.hexToInt("#FACAF7"));
				drawBarValues(guiGraphics, currentHP, maxHP, 24, -14);

				guiGraphics.pose().popPose();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(kiX, kiY, 0);
				guiGraphics.pose().scale(userScale - 0.5f, userScale - 0.5f, 1.0f);

				RenderSystem.setShaderTexture(0, hud);
				guiGraphics.blit(hud, -18, -11, 0, 44, 83, 9, 128, 128);

				float[] auraRgb = ColorUtils.hexToRgb(auraColor);
				RenderSystem.setShaderColor(auraRgb[0], auraRgb[1], auraRgb[2], 1.0f);
				guiGraphics.blit(hud, -15, -8, 3, 61, 7 + Math.min((int) currentKiBarWidth, (int) BAR_MAX_WIDTH), 4, 128, 128);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

				drawBarValues(guiGraphics, currentKi, maxKi, 24, -8);

				guiGraphics.pose().scale(userScale - 0.3f, userScale - 0.3f, 1.0f);

				drawRacialIcon(guiGraphics, raceName, Math.min(powerRelease, 100), -40, -8);
				drawFormIcon(guiGraphics, formRelease, -40, -8);

				guiGraphics.pose().popPose();

				guiGraphics.pose().pushPose();
				guiGraphics.pose().translate(stmX, stmY, 0);
				guiGraphics.pose().scale(userScale - 0.5f, userScale - 0.5f, 1.0f);

				RenderSystem.setShaderTexture(0, hud);
				guiGraphics.blit(hud, -12, -11, 0, 72, 83, 9, 128, 128);
				guiGraphics.blit(hud, -10, -8, 2, 90, -5 + Math.min((int) currentStmBarWidth, (int) BAR_MAX_WIDTH), 4, 128, 128);
				guiGraphics.blit(hud, 65, -8, 77, 90, 4, 4, 128, 128);

				drawBarValues(guiGraphics, currentStm, maxStm, 29, -8);
				drawStringWithBorder(guiGraphics, powerRelease + "%", -115, 3, ColorUtils.hexToInt("#FACAF7"));

				guiGraphics.pose().popPose();
			}
		});
	};

	private static void drawBarValues(GuiGraphics guiGraphics, float current, float max, int x, int y) {
		if (ConfigManager.getUserConfig().getHud().getAdvancedDescription()) {
			boolean percentage = ConfigManager.getUserConfig().getHud().getAdvancedDescriptionPercentage();
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
		List<String> loadedRaces = ConfigManager.getDefaultRaces();
		int raceIndex = Math.max(0, loadedRaces.indexOf(raceName.toLowerCase()));
		int iconU = 1 + (raceIndex * 17);
		boolean isCustomRace = !loadedRaces.contains(raceName.toLowerCase());
		guiGraphics.blit(racialIcons, raceName.equalsIgnoreCase("majin") ? x + 6 : x + 7, y + 4, isCustomRace ? 103 : iconU, 1, 16, 16);
		float fillRatio = powerRelease / 100.0f;
		int fillHeight = (int) (16 * fillRatio);
		if (fillHeight > 0) {
			guiGraphics.blit(racialIcons, x + 7, y + 4 + (16 - fillHeight), isCustomRace ? 103 : iconU, 18 + (16 - fillHeight), 16, fillHeight);
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
//
//        if (fillFormHeight > 0) {
//            guiGraphics.blit(xvhud, x + 10, y + 20 + (17 - fillFormHeight), 220, 130 + (17 - fillFormHeight), 26, fillFormHeight);
//        }
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
		MutableComponent dmzText = txt(text);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x - 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x + 1, y, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x, y - 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x, y + 1, 0x000000);
		guiGraphics.drawCenteredString(Minecraft.getInstance().font, dmzText, x, y, color);
	}

	private static MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}