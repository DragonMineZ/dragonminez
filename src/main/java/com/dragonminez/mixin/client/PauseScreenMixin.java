package com.dragonminez.mixin.client;

import com.dragonminez.client.gui.buttons.DiscordTitleButton;
import com.dragonminez.client.gui.buttons.PatreonTitleButton;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
	@Unique
	private static final String dragonminez$DISCORD_URL = "https://discord.dragonminez.com";
	@Unique
	private static final String dragonminez$PATREON_URL = "https://www.patreon.com/cw/DragonMineZ";

	protected PauseScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void dragonminez$replaceFeedbackButtons(CallbackInfo info) {
		Button feedbackButton = this.dragonminez$findButton("menu.sendFeedback");
		Button reportButton = this.dragonminez$findButton("menu.reportBugs");
		if (feedbackButton == null || reportButton == null) {
			return;
		}

		int rowX = feedbackButton.getX();
		int rowY = feedbackButton.getY();
		int rowHeight = feedbackButton.getHeight();
		int rowWidth = reportButton.getX() + reportButton.getWidth() - feedbackButton.getX();
		int halfWidth = (rowWidth - 4) / 2;

		this.removeWidget(feedbackButton);
		this.removeWidget(reportButton);

		Button dragonminez$discordButton = this.addRenderableWidget(new DiscordTitleButton(
				rowX,
				rowY,
				halfWidth,
				rowHeight,
				Component.translatable("gui.dragonminez.title.discord"),
				button -> this.dragonminez$openDiscordPrompt()
		));
		dragonminez$discordButton.setTooltip(Tooltip.create(Component.translatable("gui.dragonminez.title.discord.prompt")));
		dragonminez$discordButton.setTooltipDelay(120);

		Button dragonminez$patreonButton = this.addRenderableWidget(new PatreonTitleButton(
				rowX + halfWidth + 4,
				rowY,
				halfWidth,
				rowHeight,
				Component.translatable("gui.dragonminez.title.patreon"),
				button -> this.dragonminez$openPatreonPrompt()
		));
		dragonminez$patreonButton.setTooltip(Tooltip.create(Component.translatable("gui.dragonminez.title.patreon.prompt")));
		dragonminez$patreonButton.setTooltipDelay(120);
	}

	@Unique
	private Button dragonminez$findButton(String translationKey) {
		String expectedText = Component.translatable(translationKey).getString();
		for (GuiEventListener child : this.children()) {
			if (child instanceof Button button && expectedText.equals(button.getMessage().getString())) {
				return button;
			}
		}
		return null;
	}

	@Unique
	private void dragonminez$openDiscordPrompt() {
		if (this.minecraft == null) {
			return;
		}

		Screen pauseScreen = this;
		this.minecraft.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						Util.getPlatform().openUri(dragonminez$DISCORD_URL);
					}
					this.minecraft.setScreen(pauseScreen);
				},
				Component.translatable("gui.dragonminez.title.discord.prompt"),
				Component.literal(dragonminez$DISCORD_URL),
				Component.translatable("gui.dragonminez.title.discord.open"),
				CommonComponents.GUI_CANCEL
		));
	}

	@Unique
	private void dragonminez$openPatreonPrompt() {
		if (this.minecraft == null) {
			return;
		}

		Screen pauseScreen = this;
		this.minecraft.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						Util.getPlatform().openUri(dragonminez$PATREON_URL);
					}
					this.minecraft.setScreen(pauseScreen);
				},
				Component.translatable("gui.dragonminez.title.patreon.prompt"),
				Component.literal(dragonminez$PATREON_URL),
				Component.translatable("gui.dragonminez.title.patreon.open"),
				CommonComponents.GUI_CANCEL
		));
	}
}
