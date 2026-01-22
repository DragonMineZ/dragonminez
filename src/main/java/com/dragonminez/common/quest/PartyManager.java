package com.dragonminez.common.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;
import java.util.stream.Collectors;

public class PartyManager {
    private static final String PARTY_PREFIX = "dmz_party_";
    private static final Map<UUID, PendingInvite> pendingInvites = new HashMap<>();

    public static PlayerTeam getOrCreateParty(ServerPlayer player) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        String teamName = PARTY_PREFIX + player.getUUID().toString();

        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setAllowFriendlyFire(false);
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }

        return team;
    }

    public static PlayerTeam getParty(ServerPlayer player) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        return scoreboard.getPlayersTeam(player.getScoreboardName());
    }

    public static List<ServerPlayer> getAllPartyMembers(ServerPlayer player) {
        PlayerTeam team = getParty(player);
        if (team == null) {
            return Collections.singletonList(player);
        }

        return team.getPlayers().stream()
                .map(name -> player.getServer().getPlayerList().getPlayerByName(name))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static void sendInvite(ServerPlayer inviter, ServerPlayer invitee) {
        PlayerTeam inviterTeam = getOrCreateParty(inviter);

        PendingInvite invite = new PendingInvite(inviter.getUUID(), inviterTeam.getName());
        pendingInvites.put(invitee.getUUID(), invite);
    }

    public static boolean acceptInvite(ServerPlayer invitee) {
        PendingInvite invite = pendingInvites.remove(invitee.getUUID());
        if (invite == null) {
            return false;
        }

        ServerPlayer inviter = invitee.getServer().getPlayerList().getPlayer(invite.inviterUUID);
        if (inviter == null) {
            return false;
        }

        Scoreboard scoreboard = invitee.getServer().getScoreboard();
        PlayerTeam oldTeam = getParty(invitee);
        if (oldTeam != null && oldTeam.getPlayers().size() == 1) {
            scoreboard.removePlayerTeam(oldTeam);
        } else if (oldTeam != null) {
            scoreboard.removePlayerFromTeam(invitee.getScoreboardName(), oldTeam);
        }

        PlayerTeam newTeam = scoreboard.getPlayerTeam(invite.teamName);
        if (newTeam != null) {
            scoreboard.addPlayerToTeam(invitee.getScoreboardName(), newTeam);
            syncQuestProgress(inviter, invitee);
            return true;
        }

        return false;
    }

    public static void rejectInvite(ServerPlayer invitee) {
        pendingInvites.remove(invitee.getUUID());
    }

    public static PendingInvite getPendingInvite(ServerPlayer player) {
        return pendingInvites.get(player.getUUID());
    }

    public static void leaveParty(ServerPlayer player) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        PlayerTeam team = getParty(player);

        if (team != null) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);

            if (team.getPlayers().isEmpty()) {
                scoreboard.removePlayerTeam(team);
            }

            getOrCreateParty(player);
        }
    }

    private static void syncQuestProgress(ServerPlayer fromPlayer, ServerPlayer toPlayer) {}

	public static void forceJoinParty(ServerPlayer leader, ServerPlayer member) {
		Scoreboard scoreboard = leader.getServer().getScoreboard();
		PlayerTeam leaderTeam = getOrCreateParty(leader);
		leaveParty(member);
		scoreboard.addPlayerToTeam(member.getScoreboardName(), leaderTeam);
	}

    public static class PendingInvite {
        private final UUID inviterUUID;
        private final String teamName;
        private final long timestamp;

        public PendingInvite(UUID inviterUUID, String teamName) {
            this.inviterUUID = inviterUUID;
            this.teamName = teamName;
            this.timestamp = System.currentTimeMillis();
        }

        public UUID getInviterUUID() {
            return inviterUUID;
        }

        public String getTeamName() {
            return teamName;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000;
        }
    }
}

