package com.dragonminez.client.systems.worldboss;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ClientWorldBossPlayerState {
	private static final long CAST_STALE_MILLIS = 1500L;
	private static final long OVERLAY_MESSAGE_INTERVAL_MILLIS = 900L;

	private static boolean inFight;
	private static boolean knockedOut;
	private static int livesLeft;
	private static boolean casting;
	private static float castProgress;
	private static String castTargetName = "";
	private static boolean beingRevived;
	private static String reviverName = "";
	private static long receivedAtMillis;
	private static long lastOverlayMessageMillis;

	private ClientWorldBossPlayerState() {}

	public static void accept(boolean fight, boolean ko, int lives, boolean cast, float progress, String targetName,
							  boolean revived, String reviver) {
		inFight = fight;
		knockedOut = ko;
		livesLeft = lives;
		casting = cast;
		castProgress = progress;
		castTargetName = targetName == null ? "" : targetName;
		beingRevived = revived;
		reviverName = reviver == null ? "" : reviver;
		receivedAtMillis = System.currentTimeMillis();
		if (beingRevived) showBeingRevivedMessage();
	}

	private static void showBeingRevivedMessage() {
		long now = System.currentTimeMillis();
		if (now - lastOverlayMessageMillis < OVERLAY_MESSAGE_INTERVAL_MILLIS) return;
		lastOverlayMessageMillis = now;
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui == null) return;
		mc.gui.setOverlayMessage(Component.translatable("worldboss.dragonminez.revive.being_revived", reviverName)
				.withStyle(ChatFormatting.GREEN), false);
	}

	public static void clear() {
		inFight = false;
		knockedOut = false;
		livesLeft = 0;
		casting = false;
		castProgress = 0.0f;
		castTargetName = "";
		beingRevived = false;
		reviverName = "";
		receivedAtMillis = 0L;
	}

	public static boolean isInFight() {
		return inFight;
	}

	public static boolean isKnockedOut() {
		return inFight && knockedOut;
	}

	public static int livesLeft() {
		return livesLeft;
	}

	public static boolean isCasting() {
		return casting && System.currentTimeMillis() - receivedAtMillis < CAST_STALE_MILLIS;
	}

	public static float castProgress() {
		return castProgress;
	}

	public static String castTargetName() {
		return castTargetName;
	}

	public static boolean isBeingRevived() {
		return beingRevived && System.currentTimeMillis() - receivedAtMillis < CAST_STALE_MILLIS;
	}

	public static String reviverName() {
		return reviverName;
	}
}
