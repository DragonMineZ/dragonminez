package com.dragonminez.server.world.raid;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class RaidTeams {

	private static final String MINION_TEAM = "dmz_raid_minion";
	private static final String BOSS_TEAM = "dmz_raid_boss";

	private RaidTeams() {}

	public static void assign(ServerLevel level, Entity entity, boolean boss) {
		Scoreboard scoreboard = level.getScoreboard();
		PlayerTeam team = getOrCreate(scoreboard, boss ? BOSS_TEAM : MINION_TEAM,
				boss ? ChatFormatting.RED : ChatFormatting.DARK_PURPLE);
		scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
	}

	public static void release(ServerLevel level, Entity entity) {
		Scoreboard scoreboard = level.getScoreboard();
		PlayerTeam team = scoreboard.getPlayersTeam(entity.getScoreboardName());
		if (team != null) scoreboard.removePlayerFromTeam(entity.getScoreboardName(), team);
	}

	private static PlayerTeam getOrCreate(Scoreboard scoreboard, String name, ChatFormatting color) {
		PlayerTeam team = scoreboard.getPlayerTeam(name);
		if (team == null) {
			team = scoreboard.addPlayerTeam(name);
			team.setColor(color);
		}
		return team;
	}
}
