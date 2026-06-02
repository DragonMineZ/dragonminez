package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TechniqueChargeOverlay {
	private static final ResourceLocation CHARGE_HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/kicharge_hud.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static volatile float currentChargePercent = 0.0f;
	private static float lastCeilingSeen = 55.0f;
	private static long ceiling200StartMs = 0L;
	private static final long MAX_TEXT_SHOW_MS = 2000L;
	private static final long MAX_TEXT_FADE_MS = 600L;

	public static final IGuiOverlay HUD_TECHNIQUE_CHARGE = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) return;

			float targetChargePercent = data.getTechniques().getTechniqueChargePercent();
			if (targetChargePercent <= 0.0f && !data.getTechniques().isTechniqueCharging()) {
				currentChargePercent = 0.0f;
				return;
			}

			TechniqueData selectedTechnique = data.getTechniques().getSelectedTechnique();
			if (!(selectedTechnique instanceof KiAttackData kiAttack)) return;
			if (kiAttack.isInstantCast() || selectedTechnique.getCastTime() < 1) return;

			int x = (width - 223) / 2;
			int y = height - 72;
			float lerped = currentChargePercent + (targetChargePercent - currentChargePercent) * 0.25f * partialTicks;
			if (Math.abs(lerped - targetChargePercent) <= 0.5f) lerped = targetChargePercent;

			currentChargePercent = Math.max(0.0f, Math.min(200.0f, lerped));

			float normalFillRatio = Mth.clamp(currentChargePercent / 100.0f, 0.0f, 1.0f);
			int normalPixels = Math.round(148 * normalFillRatio);

			float overchargeRatio = Mth.clamp((currentChargePercent - 100.0f) / 100.0f, 0.0f, 1.0f);
			int overchargePixels = Math.round(148 * overchargeRatio);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, CHARGE_HUD_TEXTURE);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(x, y, 0);
			guiGraphics.pose().scale(1.5f, 1.5f, 1.0f);
			guiGraphics.blit(CHARGE_HUD_TEXTURE, 0, 0, 0, 0, 148, 14, 256, 256);

			int kiColor = kiAttack.getColorExterior();
			float r = ((kiColor >> 16) & 0xFF) / 255.0f;
			float g = ((kiColor >> 8) & 0xFF) / 255.0f;
			float b = (kiColor & 0xFF) / 255.0f;

			if (normalPixels > 0) {
				RenderSystem.setShaderColor(r, g, b, 1.0f);
				guiGraphics.blit(CHARGE_HUD_TEXTURE, 0, 0, 0, 14, normalPixels, 14, 256, 256);
			}

			if (overchargePixels > 0) {
				RenderSystem.setShaderColor(r * 0.5f, g * 0.5f, b * 0.5f, 1.0f);
				guiGraphics.blit(CHARGE_HUD_TEXTURE, 0, 0, 0, 14, overchargePixels, 14, 256, 256);
			}

			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			guiGraphics.pose().popPose();

			float ceiling = data.getTechniques().getChargeTierCeiling();
			drawChargeStatus(guiGraphics, mc.font, targetChargePercent, ceiling, width, y);
		});
	};

	private static void drawChargeStatus(net.minecraft.client.gui.GuiGraphics guiGraphics, Font font, float percent, float ceiling, int width, int barY) {
		boolean atMax = ceiling >= 200.0f;
		if (atMax && lastCeilingSeen < 200.0f) ceiling200StartMs = System.currentTimeMillis();
		lastCeilingSeen = ceiling;

		MutableComponent top;
		float topAlpha = 1.0f;
		if (atMax) {
			top = tr("technique.charge.to_200");
			long elapsed = System.currentTimeMillis() - ceiling200StartMs;
			if (elapsed >= MAX_TEXT_SHOW_MS + MAX_TEXT_FADE_MS) topAlpha = 0.0f;
			else if (elapsed > MAX_TEXT_SHOW_MS) topAlpha = 1.0f - (elapsed - MAX_TEXT_SHOW_MS) / (float) MAX_TEXT_FADE_MS;
		} else top = ceiling < 100.0f ? tr("technique.charge.to_100") : tr("technique.charge.to_150");

		MutableComponent bottom;
		int bottomColor;
		if (percent < 50.0f) {
			bottom = tr("technique.charge.cancel");
			bottomColor = 0xFFB0B0B0;
		} else if (atMax) {
			bottom = tr("technique.charge.fire_now");
			bottomColor = 0xFFFFD200;
		} else {
			bottom = null;
			bottomColor = 0;
		}

		int bottomY = barY - 4 - font.lineHeight;
		int topY = (bottom != null) ? bottomY - 2 - font.lineHeight : bottomY;

		if (topAlpha > 0.02f) {
			int alpha = (int) (topAlpha * 255.0f) & 0xFF;
			guiGraphics.drawString(font, top, (width - font.width(top)) / 2, topY, (alpha << 24) | 0xFFFFFF, true);
		}
		if (bottom != null) guiGraphics.drawString(font, bottom, (width - font.width(bottom)) / 2, bottomY, bottomColor, true);
	}

	private static MutableComponent tr(String key) {
		return Component.translatable(key).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}