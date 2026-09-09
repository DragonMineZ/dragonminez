package com.dragonminez.client.gui.tournament;

import com.dragonminez.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public final class TournamentOverlay {

	private static final ResourceLocation DMZ_FONT =
			ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	private static final int TICKS_PER_STEP = 20;
	private static final int FIGHT_TICKS = 25;

	private static int ticksRemaining = 0;
	private static int totalSeconds = 0;

	private TournamentOverlay() {}

	public static void startCountdown(int seconds) {
		totalSeconds = Math.max(1, seconds);
		ticksRemaining = totalSeconds * TICKS_PER_STEP + FIGHT_TICKS;
	}

	public static void clear() {
		ticksRemaining = 0;
	}

	public static void tick() {
		if (ticksRemaining <= 0) return;

		int before = ticksRemaining;
		ticksRemaining--;

		if (ticksRemaining == FIGHT_TICKS) {
			playBell(1.6F);
		} else if (before > FIGHT_TICKS && (ticksRemaining - FIGHT_TICKS) % TICKS_PER_STEP == 0) {
			playBell(1.0F);
		}
	}

	private static void playBell(float pitch) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		mc.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, pitch);
	}

	public static final IGuiOverlay OVERLAY = TournamentOverlay::render;

	private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
		if (ticksRemaining <= 0) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;

		boolean fighting = ticksRemaining <= FIGHT_TICKS;
		Component text;
		int colour;

		if (fighting) {
			text = Component.translatable("tournament.dragonminez.fight");
			colour = 0xFFFF5555;
		} else {
			int secondsLeft = ((ticksRemaining - FIGHT_TICKS) + TICKS_PER_STEP - 1) / TICKS_PER_STEP;
			text = Component.literal(Integer.toString(Math.max(1, secondsLeft)));
			colour = 0xFFFFD700;
		}

		Component styled = text.copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));

		graphics.pose().pushPose();
		graphics.pose().translate(width / 2.0F, height / 3.0F, 0.0F);
		graphics.pose().scale(fighting ? 4.0F : 5.0F, fighting ? 4.0F : 5.0F, 1.0F);

		int textWidth = mc.font.width(styled);
		graphics.drawString(mc.font, styled, -textWidth / 2, -mc.font.lineHeight / 2, colour, true);

		graphics.pose().popPose();
	}
}
