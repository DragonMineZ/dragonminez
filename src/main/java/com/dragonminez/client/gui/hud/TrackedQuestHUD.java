package com.dragonminez.client.gui.hud;

import com.dragonminez.Reference;
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
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class TrackedQuestHUD {

	private static final int PANEL_WIDTH = 180;
	private static final int MAX_TEXT_WIDTH = PANEL_WIDTH - 16;
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");

	public static final IGuiOverlay HUD_TRACKED_QUEST = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, mc.player).ifPresent(data -> {
			PlayerQuestData pqd = data.getPlayerQuestData();
			String trackedQuestId = pqd.getTrackedQuestId();
			if (trackedQuestId == null || trackedQuestId.isBlank()) return;
			if (!pqd.isQuestAccepted(trackedQuestId) || pqd.isQuestCompleted(trackedQuestId)) return;

			Quest quest = QuestRegistry.getClientQuest(trackedQuestId);
			if (quest == null) return;

			renderPanel(guiGraphics, mc.font, pqd, trackedQuestId, quest, width);
		});
	};

	private static void renderPanel(GuiGraphics guiGraphics, Font font, PlayerQuestData pqd, String questId, Quest quest, int screenWidth) {
		List<FormattedCharSequence> objectiveLines = buildObjectiveLines(font, pqd, questId, quest);
		List<FormattedCharSequence> wrappedTitle = font.split(toComponent(quest.getTitle()), MAX_TEXT_WIDTH);
		if (wrappedTitle.isEmpty()) wrappedTitle = List.of(FormattedCharSequence.forward(questId, net.minecraft.network.chat.Style.EMPTY.withFont(DMZ_FONT)));

		int timeLeftSeconds = remainingTimeSeconds(pqd, questId, quest);
		int lineHeight = 10;
		int lineCount = 1 + wrappedTitle.size() + objectiveLines.size() + (timeLeftSeconds >= 0 ? 1 : 0);
		int panelHeight = 10 + (lineCount * lineHeight) + 6;
		int x = screenWidth - PANEL_WIDTH - 10;
		int y = 10;

		guiGraphics.fill(x, y, x + PANEL_WIDTH, y + panelHeight, 0xA02A2F40);
		guiGraphics.fill(x, y, x + PANEL_WIDTH, y + 1, 0xCC6D8CFF);
		guiGraphics.fill(x, y + panelHeight - 1, x + PANEL_WIDTH, y + panelHeight, 0x66000000);

		int drawY = y + 5;
		guiGraphics.drawString(font, Component.translatable("gui.dragonminez.story.hud.tracked").withStyle(Style.EMPTY.withFont(DMZ_FONT)), x + 6, drawY, 0xE8F0FF, false);
		drawY += lineHeight;

		for (FormattedCharSequence titleLine : wrappedTitle) {
			guiGraphics.drawString(font, titleLine, x + 6, drawY, 0xFFFFFF, false);
			drawY += lineHeight;
		}

		for (FormattedCharSequence line : objectiveLines) {
			guiGraphics.drawString(font, line, x + 6, drawY, 0xCFE1FF, false);
			drawY += lineHeight;
		}

		if (timeLeftSeconds >= 0) {
			Component timeLine = Component.translatable("gui.dragonminez.story.hud.time_left",
					formatSeconds(timeLeftSeconds)).withStyle(Style.EMPTY.withFont(DMZ_FONT));
			guiGraphics.drawString(font, timeLine, x + 6, drawY, timeLeftSeconds < 60 ? 0xFF5555 : 0xFFD700, false);
		}
	}

	/** Remaining game-time seconds on a time-limited quest, or -1 when the quest has no limit. */
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

	private static List<FormattedCharSequence> buildObjectiveLines(Font font, PlayerQuestData pqd, String questId, Quest quest) {
		List<FormattedCharSequence> lines = new ArrayList<>();
		List<QuestObjective> objectives = quest.getObjectives();
		if (objectives.isEmpty()) {
			lines.add(FormattedCharSequence.forward(Component.translatable("gui.dragonminez.story.hud.no_objectives").withStyle(Style.EMPTY.withFont(DMZ_FONT)).getString(), net.minecraft.network.chat.Style.EMPTY.withFont(DMZ_FONT)));
			return lines;
		}

		for (int i = 0; i < objectives.size(); i++) {
			QuestObjective objective = objectives.get(i);
			int progress = pqd.getObjectiveProgress(questId, i);
			int required = quest.getObjectiveRequired(pqd, questId, i);
			if (!quest.isParallelObjectives()) {
				if (progress >= required) continue;
				lines.addAll(splitObjectiveLine(font, objective, progress, required));
				lines.addAll(waypointLines(font, objective, progress));
				return lines;
			}

			if (progress < required) {
				lines.addAll(splitObjectiveLine(font, objective, progress, required));
				lines.addAll(waypointLines(font, objective, progress));
			}
		}

		if (lines.isEmpty()) {
			lines.add(FormattedCharSequence.forward(Component.translatable("gui.dragonminez.quests.status.complete").withStyle(Style.EMPTY.withFont(DMZ_FONT)).getString(), net.minecraft.network.chat.Style.EMPTY.withFont(DMZ_FONT)));
		}

		return lines;
	}

	private static List<FormattedCharSequence> splitObjectiveLine(Font font, QuestObjective objective, int progress, int required) {
		Component text = Component.literal("- ")
				.append(QuestTextFormatter.describeObjective(objective))
				.append(Component.literal(" (" + progress + "/" + required + ")"))
				.withStyle(Style.EMPTY.withFont(DMZ_FONT));
		return font.split(text, MAX_TEXT_WIDTH);
	}

	private static Component toComponent(String raw) {
		return LocalizationUtil.localizedOrReadable(raw).copy().withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}

	/**
	 * Live compass line for objectives with a known position: a facing-relative arrow plus
	 * distance and cardinal direction. Supports COORDS, ESCORT destinations, and the current
	 * CHECKPOINT_RACE checkpoint; extend here when new position-carrying objectives are added.
	 */
	private static List<FormattedCharSequence> waypointLines(Font font, QuestObjective objective, int progress) {
		BlockPos target = null;
		if (objective instanceof CoordsObjective coords) {
			target = coords.getTargetPos();
		} else if (objective instanceof EscortObjective escort) {
			target = escort.getTargetPos();
		} else if (objective instanceof CheckpointRaceObjective race) {
			target = race.getCheckpoint(progress);
		}
		if (target == null) return List.of();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return List.of();
		double dx = target.getX() + 0.5 - mc.player.getX();
		double dz = target.getZ() + 0.5 - mc.player.getZ();
		int distance = (int) Math.sqrt(dx * dx + dz * dz);

		double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
		double relative = Mth.wrapDegrees(targetYaw - mc.player.getYRot());

		Component text = Component.literal("   " + directionGlyph(relative, ARROWS) + " ")
				.append(Component.translatable("gui.dragonminez.story.hud.waypoint",
								distance, directionGlyph(targetYaw, CARDINALS))
						.withStyle(Style.EMPTY.withFont(DMZ_FONT)));
		return font.split(text, MAX_TEXT_WIDTH);
	}

	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
	private static final String[] CARDINALS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private static String directionGlyph(double degrees, String[] sectors) {
		int index = (int) Math.floor(((degrees % 360 + 360 + 22.5) % 360) / 45.0) % 8;
		return sectors[index];
	}
}
