package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
import com.dragonminez.client.clash.ClientBeamClashState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class BeamClashOverlay {

	private static final ResourceLocation BAR_TEXTURE =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/kicharge_hud.png");

	private static final int TEX = 256;
	private static final int BAR_W = 148;
	private static final int BAR_H = 14;
	private static final int FRAME_V = 0;
	private static final int FILL_V = 14;

	private static final float SCALE = 1.5f;
	private static final int ROW_GAP = 6;

	private static final int FOE_COLOR = 0xE0443B;
	private static final int SWEET_OVERLAY = 0x7038E04B;
	private static final int MARKER_CORE = 0xFFFFFFFF;

	public static final IGuiOverlay HUD_BEAM_CLASH = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.renderDebug) return;
		if (!ClientBeamClashState.isActive()) return;

		float advantage = Mth.clamp(ClientBeamClashState.advantage(), 0.0f, 1.0f);
		float phase = Mth.clamp(ClientBeamClashState.meterPhase(), 0.0f, 1.0f);
		float sweetLow = Mth.clamp(ClientBeamClashState.sweetLow(), 0.0f, 1.0f);
		float sweetHigh = Mth.clamp(ClientBeamClashState.sweetHigh(), 0.0f, 1.0f);
		int beamColor = ClientBeamClashState.beamColor() & 0xFFFFFF;

		int originX = Math.round((width - BAR_W * SCALE) / 2.0f);
		int originY = height / 2 + 18;
		int tugRow = 0;
		int qteRow = BAR_H + ROW_GAP;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, BAR_TEXTURE);

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(originX, originY, 0);
		guiGraphics.pose().scale(SCALE, SCALE, 1.0f);

		// --- TUG-OF-WAR BAR ("who's winning") ---
		setColor(1f, 1f, 1f, 1f);
		guiGraphics.blit(BAR_TEXTURE, 0, tugRow, 0, FRAME_V, BAR_W, BAR_H, TEX, TEX);

		int split = Math.round(BAR_W * advantage);
		// your share, tinted by the beam color
		setColorHex(beamColor);
		guiGraphics.blit(BAR_TEXTURE, 0, tugRow, 0, FILL_V, split, BAR_H, TEX, TEX);
		// opponent's share, tinted red
		setColorHex(FOE_COLOR);
		guiGraphics.blit(BAR_TEXTURE, split, tugRow, split, FILL_V, BAR_W - split, BAR_H, TEX, TEX);
		setColor(1f, 1f, 1f, 1f);

		// --- QTE SWEEP BAR ---
		guiGraphics.blit(BAR_TEXTURE, 0, qteRow, 0, FRAME_V, BAR_W, BAR_H, TEX, TEX);
		// the marker sweeping across, tinted by the beam color
		int markerX = Math.round((BAR_W - 3) * phase);
		setColorHex(beamColor);
		guiGraphics.blit(BAR_TEXTURE, markerX, qteRow, markerX, FILL_V, 3, BAR_H, TEX, TEX);
		setColor(1f, 1f, 1f, 1f);

		// overlay highlights (fills respect the current pose transform)
		int sweetX1 = Math.round(BAR_W * sweetLow);
		int sweetX2 = Math.round(BAR_W * sweetHigh);
		guiGraphics.fill(sweetX1, qteRow, sweetX2, qteRow + BAR_H, SWEET_OVERLAY);
		guiGraphics.fill(markerX, qteRow - 2, markerX + 3, qteRow + BAR_H + 2, MARKER_CORE);
		// center reference line on the tug bar
		guiGraphics.fill(BAR_W / 2, tugRow - 1, BAR_W / 2 + 1, tugRow + BAR_H + 1, 0xFFFFFFFF);

		guiGraphics.pose().popPose();

		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		// label above the bars
		Component label = Component.translatable("hud." + Reference.MOD_ID + ".beam_clash");
		int labelWidth = mc.font.width(label);
		guiGraphics.drawString(mc.font, label, (width - labelWidth) / 2, originY - 12, 0xFFFFFF55, true);
	};

	private static void setColor(float r, float g, float b, float a) {
		RenderSystem.setShaderColor(r, g, b, a);
	}

	private static void setColorHex(int rgb) {
		RenderSystem.setShaderColor(
				((rgb >> 16) & 0xFF) / 255.0f,
				((rgb >> 8) & 0xFF) / 255.0f,
				(rgb & 0xFF) / 255.0f,
				1.0f);
	}
}
