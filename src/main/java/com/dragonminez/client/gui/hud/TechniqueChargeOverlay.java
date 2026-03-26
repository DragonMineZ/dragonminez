package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TechniqueChargeOverlay {
	private static final ResourceLocation CHARGE_HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/kicharge_hud.png");
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

			int x = (width - 223) / 2;
			int y = height - 72;
			float lerped = currentChargePercent + (targetChargePercent - currentChargePercent) * 0.25f * partialTicks;
			if (Math.abs(lerped - targetChargePercent) <= 0.5f) lerped = targetChargePercent;

			currentChargePercent = Math.max(0.0f, Math.min(200.0f, lerped));
			float fillRatio = Mth.clamp(currentChargePercent / 200.0f, 0.0f, 1.0f);
			int fillPixels = Math.round(148 * fillRatio);

			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, CHARGE_HUD_TEXTURE);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(x, y, 0);
			guiGraphics.pose().scale(1.5f, 1.5f, 1.0f);
			guiGraphics.blit(CHARGE_HUD_TEXTURE, 0, 0, 0, 0, 148, 14, 256, 256);

			if (fillPixels > 0) {
				int kiColor = kiAttack.getColorExterior();
				float r = ((kiColor >> 16) & 0xFF) / 255.0f;
				float g = ((kiColor >> 8) & 0xFF) / 255.0f;
				float b = (kiColor & 0xFF) / 255.0f;
				RenderSystem.setShaderColor(r, g, b, 1.0f);
				guiGraphics.blit(CHARGE_HUD_TEXTURE, 0, 0, 0, 14, fillPixels, 14, 256, 256);
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			}

			guiGraphics.pose().popPose();
		});
	};
}

