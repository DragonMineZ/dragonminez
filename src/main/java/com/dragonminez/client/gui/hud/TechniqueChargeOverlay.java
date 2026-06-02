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
			if (kiAttack.isInstantCast()) return;

			float scale = 1.125f;
			int barW = Math.round(148 * scale);
			int x = (width - barW) / 2;
			int y = height - 72;
			float lerped = currentChargePercent + (targetChargePercent - currentChargePercent) * 0.20f;
			if (Math.abs(lerped - targetChargePercent) <= 0.3f) lerped = targetChargePercent;

			currentChargePercent = Math.max(0.0f, Math.min(200.0f, lerped));

			float normalFillRatio = Mth.clamp(currentChargePercent / 100.0f, 0.0f, 1.0f);
			int normalPixels = Math.round(148 * normalFillRatio);

			float overMax = Math.max(1.0f, KiAttackData.OVERCHARGE_MAX_PERCENT - 100.0f);
			float overchargeRatio = Mth.clamp((currentChargePercent - 100.0f) / overMax, 0.0f, 1.0f);
			int overchargePixels = Math.round(148 * overchargeRatio);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, CHARGE_HUD_TEXTURE);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(x, y, 0);
			guiGraphics.pose().scale(scale, scale, 1.0f);
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

			drawChargeStatus(guiGraphics, mc.font, currentChargePercent, width, y);
		});
	};

	private static void drawChargeStatus(net.minecraft.client.gui.GuiGraphics guiGraphics, Font font, float percent, int width, int barY) {
		boolean charging = percent < 100.0f - 0.01f;
		MutableComponent hint = charging ? tr("technique.charge.charging") : tr("technique.charge.overcharging");
		int color = charging ? 0xFFFFFFFF : 0xFFFFD200;

		MutableComponent pct = Component.literal(Math.round(percent) + "%").withStyle(Style.EMPTY.withFont(DMZ_FONT));
		int pctY = barY - 4 - font.lineHeight;
		int hintY = pctY - 2 - font.lineHeight;
		guiGraphics.drawString(font, hint, (width - font.width(hint)) / 2, hintY, color, true);
		guiGraphics.drawString(font, pct, (width - font.width(pct)) / 2, pctY, color, true);
	}

	private static MutableComponent tr(String key) {
		return Component.translatable(key).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}