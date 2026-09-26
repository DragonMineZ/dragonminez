package com.dragonminez.client.systems.worldboss;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.S2C.WorldBossContributionS2C;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClientWorldBossState {

	private static final Map<String, State> STATES = new HashMap<>();

	private record State(boolean lairKnown, BlockPos lair, boolean bossAlive, long respawnTicks, long syncedAtMillis) {}

	private ClientWorldBossState() {}

	public static void update(String key, boolean known, BlockPos pos, boolean alive, long remainingTicks) {
		if (key == null) return;
		STATES.put(key, new State(known, pos, alive, remainingTicks, System.currentTimeMillis()));
	}

	public static void clear() {
		STATES.clear();
		Player.clear();
		Contribution.clear();
	}

	public static boolean isLairKnown(String key) {
		State state = STATES.get(key);
		return state != null && state.lairKnown();
	}

	public static BlockPos getLair(String key) {
		State state = STATES.get(key);
		return state == null ? BlockPos.ZERO : state.lair();
	}

	public static boolean isBossAlive(String key) {
		State state = STATES.get(key);
		return state != null && state.bossAlive();
	}

	public static long getRespawnTicksRemaining(String key) {
		State state = STATES.get(key);
		if (state == null || state.respawnTicks() <= 0L || state.syncedAtMillis() == 0L) return 0L;
		long elapsed = (System.currentTimeMillis() - state.syncedAtMillis()) / 50L;
		return Math.max(0L, state.respawnTicks() - elapsed);
	}

	public static final class Player {
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

		private Player() {}

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

	public static final class Contribution {
		private static final long STALE_MILLIS = 6000L;

		private static String bossKey = "";
		private static String bossNameKey = "";
		private static long elapsedTicks;
		private static long syncedAtMillis;
		private static boolean finished;
		private static long finishedAtMillis;
		private static volatile List<WorldBossContributionS2C.Entry> entries = List.of();

		private Contribution() {}

		public static void accept(String key, String nameKey, long elapsed, boolean fin, boolean cleared, List<WorldBossContributionS2C.Entry> incoming) {
			if (cleared) {
				clear();
				return;
			}
			long now = System.currentTimeMillis();
			if (finished && !fin) finished = false;
			bossKey = key == null ? "" : key;
			bossNameKey = nameKey == null ? "" : nameKey;
			elapsedTicks = elapsed;
			syncedAtMillis = now;
			if (fin && !finished) finishedAtMillis = now;
			finished = fin;
			entries = incoming == null ? List.of() : List.copyOf(incoming);
		}

		public static void clear() {
			bossKey = "";
			bossNameKey = "";
			elapsedTicks = 0L;
			syncedAtMillis = 0L;
			finished = false;
			finishedAtMillis = 0L;
			entries = List.of();
		}

		public static boolean isActive() {
			if (entries.isEmpty()) return false;
			long now = System.currentTimeMillis();
			if (finished) return now - finishedAtMillis < ConfigManager.getServerConfig().getWorldBoss().getMeterLingerSeconds() * 1000L;
			return now - syncedAtMillis < STALE_MILLIS;
		}

		public static boolean isFinished() {
			return finished;
		}

		public static float lingerFraction() {
			if (!finished) return 0.0f;
			long linger = ConfigManager.getServerConfig().getWorldBoss().getMeterLingerSeconds() * 1000L;
			if (linger <= 0L) return 1.0f;
			return Math.min(1.0f, (System.currentTimeMillis() - finishedAtMillis) / (float) linger);
		}

		public static List<WorldBossContributionS2C.Entry> entries() {
			return entries;
		}

		public static String bossKey() {
			return bossKey;
		}

		public static String bossNameKey() {
			return bossNameKey;
		}

		public static long elapsedTicks() {
			if (finished || syncedAtMillis == 0L) return elapsedTicks;
			return elapsedTicks + (System.currentTimeMillis() - syncedAtMillis) / 50L;
		}

		public static WorldBossContributionS2C.Entry find(UUID id) {
			for (WorldBossContributionS2C.Entry entry : entries) {
				if (entry.id().equals(id)) return entry;
			}
			return null;
		}
	}
}
