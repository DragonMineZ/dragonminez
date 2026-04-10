package com.dragonminez.mixin.client;

import com.dragonminez.client.gui.buttons.DiscordTitleButton;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	@Unique
	private static boolean dragonminez$hasPlayedMusic = false;
	@Unique
	private static final String dragonminez$DISCORD_URL = "https://discord.dragonminez.com";

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void dragonminez$playMenuMusic(CallbackInfo info) {
		if (!dragonminez$hasPlayedMusic) {
			SoundManager soundManager = Minecraft.getInstance().getSoundManager();
			soundManager.stop(null, SoundSource.MUSIC);
			dragonminez$hasPlayedMusic = true;
			soundManager.play(SimpleSoundInstance.forMusic(MainSounds.MENU_MUSIC.get()));
		}
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void dragonminez$replaceRealmsButton(CallbackInfo info) {
		if (this.minecraft == null || this.minecraft.isDemo()) {
			return;
		}

		Button realmsButton = this.dragonminez$findButton("menu.online");
		if (realmsButton == null) {
			return;
		}

		this.removeWidget(realmsButton);
		Button dragonminez$discordButton = this.addRenderableWidget(new DiscordTitleButton(
				realmsButton.getX(),
				realmsButton.getY(),
				realmsButton.getWidth(),
				realmsButton.getHeight(),
				Component.translatable("gui.dragonminez.title.discord"),
				button -> this.dragonminez$openDiscordPrompt()
		));
		dragonminez$discordButton.setTooltip(Tooltip.create(Component.translatable("gui.dragonminez.title.discord.prompt")));
		dragonminez$discordButton.setTooltipDelay(120);
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

		Screen titleScreen = this;
		this.minecraft.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						Util.getPlatform().openUri(dragonminez$DISCORD_URL);
					}
					this.minecraft.setScreen(titleScreen);
				},
				Component.translatable("gui.dragonminez.title.discord.prompt"),
				Component.literal(dragonminez$DISCORD_URL),
				Component.translatable("gui.dragonminez.title.discord.open"),
				CommonComponents.GUI_CANCEL
		));
	}
}
