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
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	@Unique
	private static final String dragonminez$DISCORD_URL = "https://discord.dragonminez.com";
	@Unique
	private static final List<RegistryObject<SoundEvent>> dragonminez$MENU_PLAYLIST = List.of(
			MainSounds.MENU_MUSIC_1,
			MainSounds.MENU_MUSIC_2,
			MainSounds.MENU_MUSIC_3,
			MainSounds.MENU_MUSIC_4,
			MainSounds.MENU_MUSIC_5,
			MainSounds.MENU_MUSIC_6,
			MainSounds.MENU_MUSIC_7,
			MainSounds.MENU_MUSIC_8,
			MainSounds.MENU_MUSIC_9,
			MainSounds.MENU_MUSIC_10,
			MainSounds.MENU_MUSIC_11,
			MainSounds.MENU_MUSIC_12,
			MainSounds.MENU_MUSIC_13,
			MainSounds.MENU_MUSIC_14,
			MainSounds.MENU_MUSIC_15,
			MainSounds.MENU_MUSIC_16,
			MainSounds.MENU_MUSIC_17,
			MainSounds.MENU_MUSIC_18,
			MainSounds.MENU_MUSIC_19,
			MainSounds.MENU_MUSIC_20,
			MainSounds.MENU_MUSIC_21,
			MainSounds.MENU_MUSIC_22,
			MainSounds.MENU_MUSIC_23,
			MainSounds.MENU_MUSIC_24,
			MainSounds.MENU_MUSIC_25,
			MainSounds.MENU_MUSIC_26,
			MainSounds.MENU_MUSIC_27,
			MainSounds.MENU_MUSIC_28,
			MainSounds.MENU_MUSIC_29,
			MainSounds.MENU_MUSIC_30
	);
	@Unique
	private static final Random dragonminez$RANDOM = new Random();
	@Unique
	private static int dragonminez$menuMusicIndex = -1;
	@Unique
	private static SoundInstance dragonminez$currentMenuMusic;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void dragonminez$playMenuMusic(CallbackInfo info) {
		dragonminez$ensureMenuMusicPlaying(false);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void dragonminez$loopMenuMusic(CallbackInfo info) {
		dragonminez$ensureMenuMusicPlaying(true);
	}

	@Unique
	private void dragonminez$ensureMenuMusicPlaying(boolean advanceWhenInactive) {
		SoundManager soundManager = Minecraft.getInstance().getSoundManager();
		if (dragonminez$currentMenuMusic != null && soundManager.isActive(dragonminez$currentMenuMusic)) {
			return;
		}
		if (dragonminez$MENU_PLAYLIST.isEmpty()) {
			return;
		}
		if (dragonminez$menuMusicIndex < 0) {
			dragonminez$menuMusicIndex = dragonminez$RANDOM.nextInt(dragonminez$MENU_PLAYLIST.size());
		} else if (advanceWhenInactive) {
			if (dragonminez$MENU_PLAYLIST.size() > 1) {
				int nextIndex;
				do {
					nextIndex = dragonminez$RANDOM.nextInt(dragonminez$MENU_PLAYLIST.size());
				} while (nextIndex == dragonminez$menuMusicIndex);
				dragonminez$menuMusicIndex = nextIndex;
			}
		}
		SoundEvent nextTrack = dragonminez$MENU_PLAYLIST.get(dragonminez$menuMusicIndex).get();
		dragonminez$currentMenuMusic = SimpleSoundInstance.forMusic(nextTrack);
		soundManager.play(dragonminez$currentMenuMusic);
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
