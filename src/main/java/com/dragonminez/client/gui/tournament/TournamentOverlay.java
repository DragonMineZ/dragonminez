package com.dragonminez.client.gui.tournament;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.QuestNoticeHUD;
import com.dragonminez.client.gui.quest.StoryToast;
import com.dragonminez.common.network.TournamentPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public final class TournamentOverlay {

	private static final ResourceLocation DMZ_FONT =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int FIGHT_TICKS = 30;
	private static final int GOLD = 0xFFFFD700;
	private static final int RED = 0xFFFF5555;

	private static TournamentPackets.PhaseS2C.Phase phase;
	private static int ticksRemaining;
	private static int totalTicks;
	private static int fightTicks;
	private static int lastSecond;
	private static boolean fighter;
	private static Component left = Component.empty();
	private static Component right = Component.empty();

	private TournamentOverlay() {}

	public static void onPhase(TournamentPackets.PhaseS2C msg) {
		if (msg.getPhase() == TournamentPackets.PhaseS2C.Phase.CLEAR) {
			clear();
			return;
		}

		boolean changed = phase != msg.getPhase();
		phase = msg.getPhase();
		fighter = msg.isFighter();
		left = msg.getLeft();
		right = msg.getRight();
		ticksRemaining = Math.max(0, msg.getSeconds()) * 20;
		if (changed || ticksRemaining > totalTicks) totalTicks = Math.max(1, ticksRemaining);
		lastSecond = secondsLeft();
		if (changed) fightTicks = 0;

		if (fighter && phase != TournamentPackets.PhaseS2C.Phase.NEXT_ROUND) {
			QuestNoticeHUD.clearLive();
		} else {
			QuestNoticeHUD.setLive(Component.translatable("tournament.dragonminez.title"),
					TournamentOverlay::noticeText, TournamentOverlay::progress,
					fighter ? StoryToast.Tone.FAILURE : StoryToast.Tone.PROGRESS);
		}
	}

	public static boolean isActive() {
		return phase != null;
	}

	public static void clear() {
		phase = null;
		ticksRemaining = 0;
		fightTicks = 0;
		QuestNoticeHUD.clearLive();
	}

	public static void tick() {
		if (phase == null) return;

		if (fightTicks > 0) {
			fightTicks--;
			if (fightTicks == 0) clear();
			return;
		}
		if (ticksRemaining <= 0) return;

		ticksRemaining--;
		int second = secondsLeft();
		if (second != lastSecond) {
			lastSecond = second;
			if (fighter && second > 0 && phase == TournamentPackets.PhaseS2C.Phase.COUNTDOWN) {
				play(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F);
			}
		}
		if (ticksRemaining == 0 && phase == TournamentPackets.PhaseS2C.Phase.COUNTDOWN) {
			fightTicks = FIGHT_TICKS;
			if (fighter) play(SoundEvents.ENDER_DRAGON_GROWL, 1.0F);
		}
	}

	private static int secondsLeft() {
		return (ticksRemaining + 19) / 20;
	}

	private static float progress() {
		return totalTicks <= 0 ? 0.0f : ticksRemaining / (float) totalTicks;
	}

	private static Component noticeText() {
		if (phase == null) return Component.empty();
		if (fightTicks > 0) return Component.translatable("tournament.dragonminez.notice.fight", left, right);
		int seconds = Math.max(1, secondsLeft());
		return switch (phase) {
			case NEXT_ROUND -> fighter
					? Component.translatable("tournament.dragonminez.notice.accept", seconds)
					: Component.translatable("tournament.dragonminez.notice.waiting_accept", left, seconds);
			case PREVIEW -> Component.translatable("tournament.dragonminez.notice.preview", left, right, seconds);
			case COUNTDOWN -> Component.translatable("tournament.dragonminez.notice.countdown", left, right, seconds);
			default -> Component.empty();
		};
	}

	private static void play(SoundEvent sound, float pitch) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		mc.player.playSound(sound, 1.0F, pitch);
	}

	public static final IGuiOverlay OVERLAY = TournamentOverlay::render;

	private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
		if (phase == null || !fighter || phase == TournamentPackets.PhaseS2C.Phase.NEXT_ROUND) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;
		if (mc.screen instanceof TournamentBracketScreen) return;

		float centreX = width / 2.0F;
		float baseY = height / 3.0F;

		if (fightTicks > 0) {
			drawTitle(graphics, Component.translatable("tournament.dragonminez.fight"), centreX, baseY, 4.0F, RED);
			return;
		}

		int seconds = Math.max(1, secondsLeft());
		switch (phase) {
			case COUNTDOWN -> drawTitle(graphics, Component.literal(Integer.toString(seconds)), centreX, baseY, 5.0F, GOLD);
			case PREVIEW -> drawTitle(graphics, Component.translatable("tournament.dragonminez.title.preview", seconds),
					centreX, baseY, 2.0F, GOLD);
			default -> {
			}
		}
	}

	private static void drawTitle(GuiGraphics graphics, Component text, float centreX, float y, float scale, int colour) {
		Minecraft mc = Minecraft.getInstance();
		Component styled = text.copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
		graphics.pose().pushPose();
		graphics.pose().translate(centreX, y, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		int textWidth = mc.font.width(styled);
		graphics.drawString(mc.font, styled, -textWidth / 2, -mc.font.lineHeight / 2, colour, true);
		graphics.pose().popPose();
	}
}
