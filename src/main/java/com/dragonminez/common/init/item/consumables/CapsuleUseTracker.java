package com.dragonminez.common.init.item.consumables;

import com.dragonminez.Reference;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server-side, non-persistent tracker that accumulates what a player gained from consecutive capsule uses
 * while holding the use key, so the action bar message can show the running total instead of a single use.
 * Shortly after the player lets go, the item's summary (e.g. the stat's current value) replaces that total.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CapsuleUseTracker {
	// Vanilla repeats item use every 4 ticks while the use key is held; a larger gap means the player let go.
	private static final long STREAK_TIMEOUT_TICKS = 10;
	// 1.5 seconds after the last use
	private static final long SUMMARY_DELAY_TICKS = 30;

	private static final Map<Player, Streak> STREAKS = new WeakHashMap<>();

	/** Builds the message shown once the player stops using the capsule, or null to show nothing. */
	public interface SummaryItem {
		Component getStreakSummary(ServerPlayer player, Map<String, Integer> totals);
	}

	private CapsuleUseTracker() {
	}

	/**
	 * Returns the totals for the player's current streak with this item, starting a new one if the player
	 * switched items or stopped holding the use key.
	 */
	public static Map<String, Integer> getStreak(Player player, Item item) {
		long now = player.level().getGameTime();
		Streak streak = STREAKS.get(player);
		if (streak == null || streak.item != item || now - streak.lastUseTick > STREAK_TIMEOUT_TICKS || now < streak.lastUseTick) {
			streak = new Streak(item);
			STREAKS.put(player, streak);
		}
		streak.lastUseTick = now;
		return streak.totals;
	}

	public static int add(Map<String, Integer> totals, String key, int amount) {
		return totals.merge(key, amount, Integer::sum);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
		Streak streak = STREAKS.get(player);
		if (streak == null) return;

		long now = player.level().getGameTime();
		if (now >= streak.lastUseTick && now - streak.lastUseTick < SUMMARY_DELAY_TICKS) return;
		STREAKS.remove(player);

		if (streak.item instanceof SummaryItem summaryItem) {
			Component summary = summaryItem.getStreakSummary(player, streak.totals);
			if (summary != null) player.displayClientMessage(summary, true);
		}
	}

	private static final class Streak {
		private final Item item;
		private final Map<String, Integer> totals = new HashMap<>();
		private long lastUseTick;

		private Streak(Item item) {
			this.item = item;
		}
	}
}
