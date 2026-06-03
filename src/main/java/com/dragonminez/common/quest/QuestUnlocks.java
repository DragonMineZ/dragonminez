package com.dragonminez.common.quest;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.world.entity.player.Player;

/**
 * Small helper for gating game features on the completion of a (Bulma) sidequest.
 * <p>
 * Works on both sides — player quest data lives in {@link StatsData} and is synced to clients — so the
 * same check can drive server-side logic and client-side HUD rendering.
 */
public final class QuestUnlocks {

	// --- Scouter intelligence tiers ---
	public static final String SCOUTER_CALIBRATION = "bulma_scouter_calibration";
	public static final String SCOUTER_BIOSCAN = "bulma_scouter_bioscan";
	public static final String SCOUTER_THREAT_DB = "bulma_scouter_threat_db";

	// --- Novel-wave unlocks ---
	public static final String GRAVITY_MK2 = "bulma_gravity_mk2";
	public static final String GRAVITY_MK3 = "bulma_gravity_mk3";

	private QuestUnlocks() {}

	/** Whether {@code player} has completed the quest with the given id (false if stats unavailable). */
	public static boolean isCompleted(Player player, String questId) {
		if (player == null) return false;
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		return data != null && data.getPlayerQuestData().isQuestCompleted(questId);
	}
}
