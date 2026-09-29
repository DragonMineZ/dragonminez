package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.tournament.TournamentOverlay;
import com.dragonminez.client.gui.tutorial.TutorialManager;
import com.dragonminez.client.util.KeyBinds;
import com.dragonminez.client.util.LocalizationUtil;
import com.dragonminez.common.quest.PlayerQuestData;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.QuestObjective;
import com.dragonminez.common.quest.QuestRegistry;
import com.dragonminez.common.quest.QuestTextFormatter;
import com.dragonminez.common.quest.objectives.CheckpointRaceObjective;
import com.dragonminez.common.quest.objectives.CoordsObjective;
import com.dragonminez.common.quest.objectives.EscortObjective;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.List;

public final class NotificationFeeds {
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
	private static final String[] CARDINALS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private NotificationFeeds() {
	}

	public static void registerDefaults() {
		NotificationHUD.pin("tracked_quest", NotificationFeeds::trackedQuest, NotificationHUD.PRIORITY_TRACKED_QUEST);
		NotificationHUD.pin("tutorial_hint", NotificationFeeds::tutorialHint, NotificationHUD.PRIORITY_TUTORIAL);
		NotificationHUD.pin("tournament", TournamentOverlay::notification, NotificationHUD.PRIORITY_TOURNAMENT);
	}

	private static NotificationHUD.Content tutorialHint() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.screen != null || !TutorialManager.isEnabled()) return null;
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
		if (data == null || !data.isDataLoaded() || data.getStatus().isHasCreatedCharacter()) return null;
		Component key = KeyBinds.STATS_MENU.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.BOLD);
		return NotificationHUD.Content.of(
				Component.translatable("gui.dragonminez.tutorial.hint.title"),
				null,
				Component.translatable("gui.dragonminez.tutorial.hint.start", key),
				null);
	}

	private static NotificationHUD.Content trackedQuest() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return null;
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
		if (data == null) return null;

		PlayerQuestData pqd = data.getPlayerQuestData();
		String trackedQuestId = pqd.getTrackedQuestId();
		if (trackedQuestId == null || trackedQuestId.isBlank()) return null;
		if (!pqd.isQuestAccepted(trackedQuestId) || pqd.isQuestCompleted(trackedQuestId)) return null;

		Quest quest = QuestRegistry.getClientQuest(trackedQuestId);
		if (quest == null) return null;

		Component title = LocalizationUtil.localizedOrReadable(quest.getTitle());
		if (title.getString().isBlank()) title = Component.literal(trackedQuestId);
		int timeLeft = remainingTimeSeconds(pqd, trackedQuestId, quest);
		Component extra = timeLeft >= 0
				? Component.translatable("gui.dragonminez.story.hud.time_left", formatSeconds(timeLeft))
				.withStyle(timeLeft < 60 ? ChatFormatting.RED : ChatFormatting.GOLD)
				: null;
		return NotificationHUD.Content.of(title, Component.translatable("gui.dragonminez.story.hud.tracked"),
				objectiveText(pqd, trackedQuestId, quest), extra);
	}

	private static int remainingTimeSeconds(PlayerQuestData pqd, String questId, Quest quest) {
		if (!quest.hasTimeLimit()) return -1;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return -1;
		long acceptedGameTime = pqd.getQuestAcceptedGameTime(questId);
		if (acceptedGameTime < 0) return -1;
		long remainingTicks = quest.getTimeLimitSeconds() * 20L - (mc.level.getGameTime() - acceptedGameTime);
		return (int) Math.max(0, remainingTicks / 20);
	}

	private static String formatSeconds(int totalSeconds) {
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
		return String.format("%d:%02d", minutes, seconds);
	}

	private static Component objectiveText(PlayerQuestData pqd, String questId, Quest quest) {
		List<QuestObjective> objectives = quest.getObjectives();
		if (objectives.isEmpty()) return Component.translatable("gui.dragonminez.story.hud.no_objectives");

		MutableComponent text = Component.empty();
		boolean any = false;
		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = pqd.getObjectiveProgress(questId, i);
			int required = quest.getObjectiveRequired(pqd, questId, i);
			if (progress >= required) continue;
			if (any) text.append("\n");
			text.append(Component.literal("- "))
					.append(QuestTextFormatter.describeObjective(objective))
					.append(Component.literal(" (" + progress + "/" + required + ")"));
			Component waypoint = waypointLine(objective, progress);
			if (waypoint != null) text.append("\n").append(waypoint);
			any = true;
			if (!quest.isParallelObjectives()) break;
		}
		return any ? text : Component.translatable("gui.dragonminez.quests.status.complete");
	}

	private static Component waypointLine(QuestObjective objective, int progress) {
		BlockPos target = null;
		if (objective instanceof CoordsObjective coords) {
			target = coords.getTargetPos();
		} else if (objective instanceof EscortObjective escort) {
			target = escort.getTargetPos();
		} else if (objective instanceof CheckpointRaceObjective race) {
			target = race.getCheckpoint(progress);
		}
		if (target == null) return null;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return null;
		double dx = target.getX() + 0.5 - mc.player.getX();
		double dz = target.getZ() + 0.5 - mc.player.getZ();
		int distance = (int) Math.sqrt(dx * dx + dz * dz);

		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double relative = Mth.wrapDegrees(targetYaw - mc.player.getYRot());

		return Component.literal("   " + directionGlyph(relative, ARROWS) + " ")
				.append(Component.translatable("gui.dragonminez.story.hud.waypoint", distance, directionGlyph(targetYaw, CARDINALS)));
	}

	private static String directionGlyph(double degrees, String[] sectors) {
		int index = (int) Math.floor(((degrees % 360 + 360 + 22.5) % 360) / 45.0) % 8;
		return sectors[index];
	}
}
