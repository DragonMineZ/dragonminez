package com.dragonminez.client.gui;

import com.dragonminez.Reference;
import com.dragonminez.client.beta.BetaAccessVerification;
import com.dragonminez.client.util.TextUtil;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BetaAccessVerificationScreen extends Screen {
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	private static final int CONTENT_WIDTH = 320;

	private final Screen parent;
	private final String minecraftUsername;
	private Component statusMessage = Component.empty();

	public BetaAccessVerificationScreen(Screen parent, String minecraftUsername) {
		super(Component.translatable("gui.dragonminez.beta_access.title"));
		this.parent = parent;
		this.minecraftUsername = minecraftUsername;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int buttonY = this.height / 2 + 34;

		this.addRenderableWidget(Button.builder(
						Component.translatable("gui.dragonminez.beta_access.verify_here"),
						button -> this.openVerificationUrl()
				)
				.bounds(centerX - 100, buttonY, 200, 20)
				.build());

		this.addRenderableWidget(Button.builder(
						CommonComponents.GUI_BACK,
						button -> this.onClose()
				)
				.bounds(centerX - 100, buttonY + 24, 200, 20)
				.build());
	}

	private void openVerificationUrl() {
		try {
			Util.getPlatform().openUri(BetaAccessVerification.buildVerificationUrl(this.minecraftUsername));
			this.statusMessage = Component.translatable("gui.dragonminez.beta_access.status.opened");
		} catch (IllegalArgumentException exception) {
			this.statusMessage = Component.translatable("gui.dragonminez.beta_access.status.invalid_username");
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(graphics);

		int centerX = this.width / 2;
		int y = this.height / 2 - 80;

		TextUtil.drawCenteredStringWithBorder(
				graphics,
				this.font,
				Component.translatable("gui.dragonminez.beta_access.title").withStyle(style -> style.withFont(DMZ_FONT)),
				centerX,
				y,
				0xFFFF5555
		);
		y += 24;

		TextUtil.drawCenteredStringWithBorder(
				graphics,
				this.font,
				Component.translatable("gui.dragonminez.beta_access.username", this.minecraftUsername),
				centerX,
				y,
				0xFFFFFFFF
		);
		y += 22;

		List<FormattedCharSequence> lines = this.font.split(
				Component.translatable("gui.dragonminez.beta_access.body"),
				CONTENT_WIDTH
		);
		for (FormattedCharSequence line : lines) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, line, centerX, y, 0xFFCCCCCC);
			y += this.font.lineHeight + 2;
		}

		if (!this.statusMessage.getString().isBlank()) {
			TextUtil.drawCenteredStringWithBorder(graphics, this.font, this.statusMessage, centerX, this.height / 2 + 10, 0xFF55FF55);
		}

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
