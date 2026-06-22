package com.dragonminez.mixin.client;

import com.dragonminez.client.gui.buttons.DiscordTitleButton;
import com.dragonminez.client.gui.buttons.IconButton;
import com.dragonminez.common.init.MainSounds;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.internal.BrandingControl;
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
	private static final String dragonminez$PATREON_URL = "https://www.patreon.com/cw/DragonMineZ";
	@Unique
	private static final ResourceLocation dragonminez$PATREON_LOGO = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/title/patreon_logo.png");
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
			MainSounds.MENU_MUSIC_30,
			MainSounds.MENU_MUSIC_31,
			MainSounds.MENU_MUSIC_32,
			MainSounds.MENU_MUSIC_33,
			MainSounds.MENU_MUSIC_34,
			MainSounds.MENU_MUSIC_35,
			MainSounds.MENU_MUSIC_36,
			MainSounds.MENU_MUSIC_37,
			MainSounds.MENU_MUSIC_38,
			MainSounds.MENU_MUSIC_39
	);
	@Unique
	private static final Random dragonminez$RANDOM = new Random();
	@Unique
	private static final int dragonminez$FIRST_SONG_GROUP_SPLIT = 33;
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
		Minecraft client = Minecraft.getInstance();
		client.getMusicManager().stopPlaying();

		if (client.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F || client.options.getSoundSourceVolume(SoundSource.MASTER) <= 0.0F) {
			return;
		}

		SoundManager soundManager = client.getSoundManager();
		if (dragonminez$currentMenuMusic != null && soundManager.isActive(dragonminez$currentMenuMusic)) return;
		if (dragonminez$MENU_PLAYLIST.isEmpty()) return;

		if (dragonminez$menuMusicIndex < 0) dragonminez$menuMusicIndex = dragonminez$pickFirstMenuMusicIndex();
		else if (advanceWhenInactive) {
			if (dragonminez$MENU_PLAYLIST.size() > 1) {
				int nextIndex;
				do nextIndex = dragonminez$RANDOM.nextInt(dragonminez$MENU_PLAYLIST.size());
				while (nextIndex == dragonminez$menuMusicIndex);
				dragonminez$menuMusicIndex = nextIndex;
			}
		}

		SoundEvent nextTrack = dragonminez$MENU_PLAYLIST.get(dragonminez$menuMusicIndex).get();
		dragonminez$currentMenuMusic = SimpleSoundInstance.forMusic(nextTrack);
		soundManager.play(dragonminez$currentMenuMusic);
	}

	@Unique
	private static int dragonminez$pickFirstMenuMusicIndex() {
		int playlistSize = dragonminez$MENU_PLAYLIST.size();
		int splitIndex = Math.min(dragonminez$FIRST_SONG_GROUP_SPLIT, playlistSize);
		if (splitIndex <= 0) return 0;
		if (splitIndex >= playlistSize) return dragonminez$RANDOM.nextInt(playlistSize);
		if (dragonminez$RANDOM.nextBoolean()) return dragonminez$RANDOM.nextInt(splitIndex);
		return splitIndex + dragonminez$RANDOM.nextInt(playlistSize - splitIndex);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void dragonminez$replaceRealmsButton(CallbackInfo info) {
		if (this.minecraft == null || this.minecraft.isDemo()) return;

		Button realmsButton = this.dragonminez$findButton("menu.online");
		if (realmsButton == null) return;

		this.removeWidget(realmsButton);
		int discordWidth = realmsButton.getWidth() - (20 + 4);
		Button dragonminez$discordButton = this.addRenderableWidget(new DiscordTitleButton(
				realmsButton.getX(),
				realmsButton.getY(),
				discordWidth,
				realmsButton.getHeight(),
				Component.translatable("gui.dragonminez.title.discord"),
				button -> this.dragonminez$openDiscordPrompt()
		));
		dragonminez$discordButton.setTooltip(Tooltip.create(Component.translatable("gui.dragonminez.title.discord.prompt")));
		dragonminez$discordButton.setTooltipDelay(120);

		Button dragonminez$patreonButton = this.addRenderableWidget(new IconButton(
				realmsButton.getX() + discordWidth + 4,
				realmsButton.getY() + (realmsButton.getHeight() - 20) / 2,
				20,
				20,
				dragonminez$PATREON_LOGO,
				12,
				32,
				Component.translatable("gui.dragonminez.title.patreon"),
				button -> this.dragonminez$openPatreonPrompt()
		));
		dragonminez$patreonButton.setTooltip(Tooltip.create(Component.translatable("gui.dragonminez.title.patreon.prompt")));
		dragonminez$patreonButton.setTooltipDelay(120);
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void dragonminez$renderNowPlaying(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (dragonminez$menuMusicIndex < 0 || this.minecraft == null) return;

		Font font = this.minecraft.font;
		int rightMargin = 2;
		int lineHeight = 10;

		Component songTitle = Component.translatable("item.dragonminez.music_disc_menu_music_" + (dragonminez$menuMusicIndex + 1));
		List<FormattedCharSequence> wrappedSong = font.split(songTitle, 130);

		int[] overCopyrightCount = {0};
		BrandingControl.forEachAboveCopyrightLine((idx, line) -> overCopyrightCount[0]++);
		int yPos = this.height - 20 - (overCopyrightCount[0] * lineHeight);

		int totalLines = 1 + wrappedSong.size();
		yPos -= (totalLines - 1) * lineHeight;

		Component nowPlaying = Component.translatable("gui.dragonminez.title.now_playing");
		guiGraphics.drawString(font, nowPlaying, this.width - font.width(nowPlaying) - rightMargin, yPos, 0xFFFFFF);
		yPos += lineHeight;

		for (FormattedCharSequence line : wrappedSong) {
			guiGraphics.drawString(font, line, this.width - font.width(line) - rightMargin, yPos, 0xAAAAAA);
			yPos += lineHeight;
		}
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
		if (this.minecraft == null) return;

		Screen titleScreen = this;
		this.minecraft.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) Util.getPlatform().openUri(dragonminez$DISCORD_URL);
					this.minecraft.setScreen(titleScreen);
				},
				Component.translatable("gui.dragonminez.title.discord.prompt"),
				Component.literal(dragonminez$DISCORD_URL),
				Component.translatable("gui.dragonminez.title.discord.open"),
				CommonComponents.GUI_CANCEL
		));
	}

	@Unique
	private void dragonminez$openPatreonPrompt() {
		if (this.minecraft == null) return;

		Screen titleScreen = this;
		this.minecraft.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) Util.getPlatform().openUri(dragonminez$PATREON_URL);
					this.minecraft.setScreen(titleScreen);
				},
				Component.translatable("gui.dragonminez.title.patreon.prompt"),
				Component.literal(dragonminez$PATREON_URL),
				Component.translatable("gui.dragonminez.title.patreon.open"),
				CommonComponents.GUI_CANCEL
		));
	}
}
