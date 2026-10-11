package com.dragonminez.common.quest;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.C2S.InvitePartyMemberC2S;
import com.dragonminez.common.network.S2C.PartyInviteToastS2C;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.data.PartySavedData;
import com.dragonminez.server.world.tournament.Tournament;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class PartyManager {
    private static final long INVITE_DURATION_MS = 60_000L;
    private static final String TEAM_PREFIX = "dmzp_";
    private static final Set<UUID> BOUTING_PARTIES = ConcurrentHashMap.newKeySet();

    private PartyManager() {}

    private static String getTeamName(UUID partyId) {
        return TEAM_PREFIX + partyId.toString().replace("-", "").substring(0, 11);
    }

    private static void addToMinecraftTeam(MinecraftServer server, UUID partyId, ServerPlayer player) {
        Scoreboard scoreboard = server.getScoreboard();
        String entry = player.getScoreboardName();
        PlayerTeam current = scoreboard.getPlayersTeam(entry);
        if (current != null && !current.getName().startsWith(TEAM_PREFIX)) return;

        String teamName = getTeamName(partyId);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }
        if (current == team) return;
        scoreboard.addPlayerToTeam(entry, team);
        if (current != null && current.getPlayers().isEmpty()) scoreboard.removePlayerTeam(current);
    }

    private static void removeFromPartyTeams(MinecraftServer server, ServerPlayer player) {
        Scoreboard scoreboard = server.getScoreboard();
        String entry = player.getScoreboardName();
        PlayerTeam current = scoreboard.getPlayersTeam(entry);
        if (current == null || !current.getName().startsWith(TEAM_PREFIX)) return;
        scoreboard.removePlayerFromTeam(entry, current);
        if (current.getPlayers().isEmpty()) scoreboard.removePlayerTeam(current);
    }

    private static void applyFriendlyFire(MinecraftServer server, PartySavedData.PartyInstance party) {
        PlayerTeam team = server.getScoreboard().getPlayerTeam(getTeamName(party.getPartyId()));
        if (team != null) team.setAllowFriendlyFire(party.isPvpEnabled() || BOUTING_PARTIES.contains(party.getPartyId()));
    }

    public static void setTournamentFriendlyFire(MinecraftServer server, UUID partyId, boolean bouting) {
        if (server == null || partyId == null) return;

        if (bouting) BOUTING_PARTIES.add(partyId);
        else BOUTING_PARTIES.remove(partyId);

        PartySavedData.PartyInstance party = PartySavedData.get(server).getParty(partyId);
        if (party != null) {
            applyFriendlyFire(server, party);
            return;
        }
        PlayerTeam team = server.getScoreboard().getPlayerTeam(getTeamName(partyId));
        if (team != null) team.setAllowFriendlyFire(bouting);
    }

    private static boolean isInTournament(ServerPlayer player) {
        return player != null && Tournament.Manager.runOf(player) != null;
    }

    private static boolean isPartyInTournament(MinecraftServer server, PartySavedData.PartyInstance party) {
        if (party == null) return false;
        Tournament.Progress progress = Tournament.Progress.get(server.overworld());
        for (UUID memberId : party.getMembers()) {
            if (progress.runOf(memberId) != null) return true;
        }
        return false;
    }

    public static boolean isPartyLocked(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party != null ? isPartyInTournament(player.getServer(), party) : isInTournament(player);
    }

    public static UUID getOrCreateParty(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party != null) {
            return party.getPartyId();
        }
        party = data.createParty(player.getUUID());
        addToMinecraftTeam(player.getServer(), party.getPartyId(), player);
        applyFriendlyFire(player.getServer(), party);
        syncPartyToOnlineMembers(player.getServer(), party);
        return party.getPartyId();
    }

    public static UUID getPartyId(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party != null ? party.getPartyId() : null;
    }

    public static boolean isInParty(ServerPlayer player) {
        return getPartyId(player) != null;
    }

    public static boolean isPartyLeader(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party != null && party.getLeaderId().equals(player.getUUID());
    }

    public static boolean isPartyOwner(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party != null && party.getOwnerId().equals(player.getUUID());
    }

    public static boolean canInvitePlayers(ServerPlayer player) {
        return !isInParty(player) || isPartyLeader(player);
    }

    public static boolean areInSameParty(Player p1, Player p2) {
        if (p1 == null || p2 == null) return false;
        if (p1 instanceof ServerPlayer s1 && p2 instanceof ServerPlayer) {
            PartySavedData.PartyInstance party = PartySavedData.get(s1.getServer()).getPartyOf(p1.getUUID());
            return party != null && party.isMember(p2.getUUID());
        }
        StatsData data1 = getStatsData(p1);
        StatsData data2 = getStatsData(p2);
        if (data1 == null || data2 == null) return false;

        UUID party1 = data1.getPlayerQuestData().getActivePartyId();
        UUID party2 = data2.getPlayerQuestData().getActivePartyId();

        return party1 != null && party1.equals(party2);
    }

    public static boolean isPartyPvpEnabled(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PartySavedData.PartyInstance party = PartySavedData.get(serverPlayer.getServer()).getPartyOf(serverPlayer.getUUID());
            return party != null && party.isPvpEnabled();
        }
        StatsData data = getStatsData(player);
        return data != null && data.getPlayerQuestData().isPartyPvpEnabled();
    }

    public static void togglePartyPvp(ServerPlayer leader) {
        PartySavedData data = PartySavedData.get(leader.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(leader.getUUID());
        if (party != null && party.getLeaderId().equals(leader.getUUID())) {
            party.setPvpEnabled(!party.isPvpEnabled());
            data.setDirty();

            applyFriendlyFire(leader.getServer(), party);
            syncPartyToOnlineMembers(leader.getServer(), party);

            String state = party.isPvpEnabled() ? "✓" : "✕";
            ChatFormatting color = party.isPvpEnabled() ? ChatFormatting.RED : ChatFormatting.GREEN;

            for (ServerPlayer member : getAllPartyMembers(leader)) {
                member.sendSystemMessage(Component.translatable("quest.dmz.party.pvp.toggled", state).withStyle(color));
            }
        }
    }

    public static ServerPlayer getPartyLeader(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        if (party == null) return null;
        return player.getServer().getPlayerList().getPlayer(party.getLeaderId());
    }

    public static List<UUID> getPartyMemberIds(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party == null ? List.of(player.getUUID()) : new ArrayList<>(party.getMembers());
    }

    public static UUID getPartyLeaderId(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party == null ? player.getUUID() : party.getLeaderId();
    }

    public static UUID getPartyOwnerId(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party == null ? player.getUUID() : party.getOwnerId();
    }

    public static List<ServerPlayer> getAllPartyMembers(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        if (party == null) return Collections.singletonList(player);

        List<ServerPlayer> members = new ArrayList<>();
        for (UUID memberId : party.getMembers()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
            if (member != null && !members.contains(member)) members.add(member);
        }

        if (members.isEmpty()) members.add(player);
        return members;
    }

    public static int getPartySize(ServerPlayer player) {
        PartySavedData.PartyInstance party = PartySavedData.get(player.getServer()).getPartyOf(player.getUUID());
        return party == null ? 1 : party.getMembers().size();
    }

    public static int maxLevelGap() {
        return ConfigManager.getServerConfig().getGameplay().getPartyMaxLevelGap();
    }

    public static int maxMembers() {
        return ConfigManager.getServerConfig().getGameplay().getPartyMaxMembers();
    }

    public static int levelOf(ServerPlayer player) {
        StatsData data = player != null ? getStatsData(player) : null;
        return data != null ? data.getLevel() : 0;
    }

    public static Difficulty difficultyOf(ServerPlayer player) {
        StatsData data = player != null ? getStatsData(player) : null;
        return data != null ? data.getPlayerQuestData().getDifficulty() : Difficulty.NORMAL;
    }

    private static boolean validateLevelGap(ServerPlayer leader, ServerPlayer target) {
        int maxGap = maxLevelGap();
        if (maxGap == -1) return true;

        StatsData leaderData = getStatsData(leader);
        StatsData targetData = getStatsData(target);

        if (leaderData == null || targetData == null) return false;

        return Math.abs(leaderData.getLevel() - targetData.getLevel()) <= maxGap;
    }

    public static InviteRequestResult requestInvite(ServerPlayer inviter, ServerPlayer invitee) {
        if (inviter.getUUID().equals(invitee.getUUID())) return InviteRequestResult.CANNOT_INVITE_SELF;
        UUID inviterParty = getPartyId(inviter);
        if (inviterParty != null && inviterParty.equals(getPartyId(invitee))) return InviteRequestResult.ALREADY_IN_PARTY;

        if (isPartyLocked(inviter) || isInTournament(invitee)) return InviteRequestResult.TOURNAMENT_ACTIVE;

        ServerPlayer resolvedLeader = isInParty(inviter) ? getPartyLeader(inviter) : inviter;
        if (resolvedLeader != null && !validateLevelGap(resolvedLeader, invitee)) return InviteRequestResult.LEVEL_GAP;
        if (resolvedLeader != null && difficultyOf(resolvedLeader).ordinal() < difficultyOf(invitee).ordinal()) {
            return InviteRequestResult.DIFFICULTY_TOO_LOW;
        }

        int maxMembers = maxMembers();
        if (maxMembers != -1 && (maxMembers < 2 || getPartySize(inviter) >= maxMembers)) return InviteRequestResult.PARTY_FULL;

        if (isInParty(inviter) && !isPartyLeader(inviter)) {
            ServerPlayer leader = getPartyLeader(inviter);
            if (leader != null) {
                Component acceptBtn = Component.translatable("quest.dmz.party.invite.button")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzparty invite " + invitee.getGameProfile().getName())));

                leader.sendSystemMessage(Component.translatable("quest.dmz.party.invite.suggest", inviter.getGameProfile().getName(), invitee.getGameProfile().getName())
                        .append(Component.literal(" "))
                        .append(acceptBtn));
                return InviteRequestResult.SUGGESTED;
            }
            return InviteRequestResult.NO_PERMISSION;
        }

        sendInvite(inviter, invitee);
        return InviteRequestResult.INVITED;
    }

    public static void sendInvite(ServerPlayer inviter, ServerPlayer invitee) {
        UUID partyId = getOrCreateParty(inviter);
        ServerPlayer leader = getPartyLeader(inviter);
        if (leader == null) leader = inviter;
        PlayerQuestData inviteeQuestData = getQuestData(invitee);
        long expiresAt = System.currentTimeMillis() + INVITE_DURATION_MS;
        inviteeQuestData.addPendingPartyInvite(new PlayerQuestData.PartyInviteData(
                inviter.getUUID(),
                partyId,
                leader.getUUID(),
                inviter.getGameProfile().getName(),
                expiresAt,
                getQuestData(leader).getDifficulty()
        ));
        syncSelf(invitee);
        NetworkHandler.sendToPlayer(new PartyInviteToastS2C(inviter.getGameProfile().getName()), invitee);
    }

    public static InviteAcceptResult acceptInvite(ServerPlayer invitee) {
        return acceptInvite(invitee, false);
    }

    public static InviteAcceptResult acceptInvite(ServerPlayer invitee, boolean confirmedDifficultyChange) {
        return acceptInvite(invitee, confirmedDifficultyChange, null);
    }

    public static InviteAcceptResult acceptInvite(ServerPlayer invitee, boolean confirmedDifficultyChange,
                                                  UUID partyId) {
        PlayerQuestData inviteeQuestData = getQuestData(invitee);
        PlayerQuestData.PartyInviteData invite = inviteeQuestData.getPendingPartyInvite(partyId);

        if (invite == null) return InviteAcceptResult.INVALID;

        MinecraftServer server = invitee.getServer();
        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyInstance party = data.getParty(invite.getPartyId());
        if (party == null) {
            inviteeQuestData.removePendingPartyInvite(invite.getPartyId());
            syncSelf(invitee);
            return InviteAcceptResult.PARTY_DISBANDED;
        }

        if (isInTournament(invitee) || isPartyInTournament(server, party)) {
            return InviteAcceptResult.TOURNAMENT_ACTIVE;
        }

        if (invite.isExpired()) {
            inviteeQuestData.removePendingPartyInvite(invite.getPartyId());
            syncSelf(invitee);
            return InviteAcceptResult.EXPIRED;
        }

        ServerPlayer leader = server.getPlayerList().getPlayer(party.getLeaderId());
        if (leader == null) {
            inviteeQuestData.removePendingPartyInvite(invite.getPartyId());
            syncSelf(invitee);
            return InviteAcceptResult.LEADER_OFFLINE;
        }

        if (!validateLevelGap(leader, invitee)) return InviteAcceptResult.LEVEL_GAP;

        int maxMembers = maxMembers();
        if (maxMembers != -1 && party.getMembers().size() >= maxMembers) {
            inviteeQuestData.removePendingPartyInvite(invite.getPartyId());
            syncSelf(invitee);
            return InviteAcceptResult.PARTY_FULL;
        }

        Difficulty partyDifficulty = getQuestData(leader).getDifficulty();
        Difficulty ownDifficulty = inviteeQuestData.getDifficulty();
        if (partyDifficulty.ordinal() < ownDifficulty.ordinal()) {
            return InviteAcceptResult.DIFFICULTY_TOO_LOW;
        }
        if (partyDifficulty.ordinal() > ownDifficulty.ordinal() && !confirmedDifficultyChange) {
            return InviteAcceptResult.DIFFICULTY_CONFIRM_REQUIRED;
        }

        inviteeQuestData.clearPendingPartyInvite();
        if (isPartyOwner(invitee) && getPartySize(invitee) > 1) PartyFeedback.leaveRequested(invitee);
        else leaveParty(invitee, false);
        joinParty(leader, party, invitee);
        return InviteAcceptResult.SUCCESS;
    }

    public static void rejectInvite(ServerPlayer invitee) {
        rejectInvite(invitee, null);
    }

    public static void rejectInvite(ServerPlayer invitee, UUID partyId) {
        getQuestData(invitee).removePendingPartyInvite(partyId);
        syncSelf(invitee);
    }

    public static PendingInvite getPendingInvite(ServerPlayer player) {
        return getPendingInvite(player, null);
    }

    public static PendingInvite getPendingInvite(ServerPlayer player, UUID partyId) {
        PlayerQuestData.PartyInviteData invite = getQuestData(player).getPendingPartyInvite(partyId);
        if (invite == null) return null;

        return new PendingInvite(
                invite.getInviterUUID(),
                invite.getPartyId() == null ? "" : invite.getPartyId().toString(),
                invite.getPartyLeaderId(),
                invite.getInviterName(),
                invite.getExpiresAtMs(),
                invite.getPartyDifficulty()
        );
    }

    public static void leaveParty(ServerPlayer player) {
        leaveParty(player, true);
    }

    public static void leaveParty(ServerPlayer player, boolean forfeitTournament) {
        if (forfeitTournament) {
            Tournament.Manager.onPartyLeave(player);
        }

        MinecraftServer server = player.getServer();
        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());

        removeFromPartyTeams(server, player);
        getQuestData(player).clearPartyState();
        syncSelf(player);

        if (party == null) return;

        UUID previousLeader = party.getLeaderId();
        data.removePlayer(player.getUUID());
        if (party.getMembers().isEmpty()) {
            BOUTING_PARTIES.remove(party.getPartyId());
            return;
        }

        refreshLeadership(server, party, null, previousLeader, LeaderChange.CHANGED);
        syncPartyToOnlineMembers(server, party);
    }

    public static void disbandParty(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyInstance party = data.getPartyOf(owner.getUUID());
        if (party == null) return;

        List<UUID> memberIds = new ArrayList<>(party.getMembers());
        for (UUID memberId : memberIds) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member == null) continue;
            Tournament.Manager.onPartyLeave(member);
            removeFromPartyTeams(server, member);
            getQuestData(member).clearPartyState();
            syncSelf(member);
        }

        BOUTING_PARTIES.remove(party.getPartyId());
        data.removeParty(party.getPartyId());
        PlayerTeam team = server.getScoreboard().getPlayerTeam(getTeamName(party.getPartyId()));
        if (team != null) server.getScoreboard().removePlayerTeam(team);
    }

    public static KickResult kickMember(ServerPlayer leader, UUID targetId) {
        MinecraftServer server = leader.getServer();
        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyInstance party = data.getPartyOf(leader.getUUID());
        if (party == null) return KickResult.NOT_IN_PARTY;
        if (!party.getLeaderId().equals(leader.getUUID())) return KickResult.NOT_LEADER;
        if (leader.getUUID().equals(targetId)) return KickResult.SELF;
        if (!party.isMember(targetId)) return KickResult.TARGET_NOT_IN_PARTY;
        if (party.getOwnerId().equals(targetId)) return KickResult.OWNER;
        if (isPartyInTournament(server, party)) return KickResult.TOURNAMENT_ACTIVE;

        ServerPlayer target = server.getPlayerList().getPlayer(targetId);
        if (target != null) {
            leaveParty(target, true);
            return KickResult.SUCCESS;
        }

        UUID previousLeader = party.getLeaderId();
        data.removePlayer(targetId);
        refreshLeadership(server, party, null, previousLeader, LeaderChange.CHANGED);
        syncPartyToOnlineMembers(server, party);
        return KickResult.SUCCESS;
    }

    public static void syncPartyDifficulty(ServerPlayer source) {
        ServerPlayer leader = resolveDifficultyOwner(source);
        if (leader == null) return;

        Difficulty difficulty = getQuestData(leader).getDifficulty();
        for (ServerPlayer member : getAllPartyMembers(leader)) {
            PlayerQuestData memberData = getQuestData(member);
            if (!member.getUUID().equals(leader.getUUID())) {
                memberData.setDifficulty(difficulty);
                memberData.setDifficultyChosen(true);
            }
            syncSelf(member);
        }
    }

    public static ServerPlayer resolveDifficultyOwner(ServerPlayer player) {
        if (!isInParty(player) || isPartyLeader(player)) return player;
        ServerPlayer leader = getPartyLeader(player);
        return leader != null ? leader : player;
    }

    public static void beginFusionParty(ServerPlayer leader, ServerPlayer partner) {
        snapshotFusionParty(leader);
        snapshotFusionParty(partner);

        if (isInParty(leader)) {
            joinPartyForFusion(partner, getPartyId(leader), false);
        } else if (isInParty(partner)) {
            joinPartyForFusion(leader, getPartyId(partner), false);
        } else {
            UUID partyId = getOrCreateParty(leader);
            joinPartyForFusion(partner, partyId, false);
        }
    }

    public static void endFusionParty(ServerPlayer player) {
        StatsData data = getStatsData(player);
        if (data == null) return;
        var status = data.getStatus();
        if (!status.isFusionPartyManaged()) return;

        UUID prevPartyId = status.getFusionPrevPartyId();
        boolean prevOwner = status.isFusionPrevPartyLeader();

        status.setFusionPartyManaged(false);
        status.setFusionPrevPartyId(null);
        status.setFusionPrevPartyLeader(false);

        UUID currentPartyId = getPartyId(player);
        if (Objects.equals(currentPartyId, prevPartyId)) return;

        if (prevPartyId == null) {
            leaveParty(player, false);
        } else {
            joinPartyForFusion(player, prevPartyId, prevOwner);
        }
    }

    private static void snapshotFusionParty(ServerPlayer player) {
        StatsData data = getStatsData(player);
        if (data == null) return;
        var status = data.getStatus();
        UUID partyId = getPartyId(player);
        status.setFusionPrevPartyId(partyId);
        status.setFusionPrevPartyLeader(partyId != null && isPartyOwner(player));
        status.setFusionPartyManaged(true);
    }

    private static void joinPartyForFusion(ServerPlayer mover, UUID targetPartyId, boolean restoreOwnership) {
        if (targetPartyId == null) return;
        if (targetPartyId.equals(getPartyId(mover))) return;

        MinecraftServer server = mover.getServer();
        PartySavedData data = PartySavedData.get(server);
        if (data.getParty(targetPartyId) == null) {
            leaveParty(mover, false);
            return;
        }

        leaveParty(mover, false);

        PartySavedData.PartyInstance party = data.getParty(targetPartyId);
        if (party == null) return;

        UUID previousLeader = party.getLeaderId();
        data.addPlayerToParty(targetPartyId, mover.getUUID());
        if (restoreOwnership) {
            party.setOwnerId(mover.getUUID());
            data.setDirty();
        }
        addToMinecraftTeam(server, targetPartyId, mover);
        applyFriendlyFire(server, party);
        refreshLeadership(server, party, null, previousLeader, LeaderChange.CHANGED);
        syncPartyToOnlineMembers(server, party);
    }

    public static void reconcile(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        StatsData stats = getStatsData(player);
        if (stats == null) return;

        PlayerQuestData questData = stats.getPlayerQuestData();
        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party == null) {
            removeFromPartyTeams(server, player);
            if (questData.isInParty()) {
                questData.clearPartyState();
                syncSelf(player);
            }
            return;
        }

        addToMinecraftTeam(server, party.getPartyId(), player);
        applyFriendlyFire(server, party);
        refreshLeadership(server, party, null, party.getLeaderId(), LeaderChange.RETURNED);
        syncPartyToOnlineMembers(server, party);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        StatsData statsData = getStatsData(player);
        if (statsData != null) {
            PlayerQuestData questData = statsData.getPlayerQuestData();
            if (questData.hasPendingPartyInvite()) questData.clearPendingPartyInvite();
        }
        InvitePartyMemberC2S.clear(player.getUUID());

        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party == null) return;
        if (refreshLeadership(player.getServer(), party, player.getUUID(), party.getLeaderId(), LeaderChange.DISCONNECTED)) {
            syncPartyToOnlineMembers(player.getServer(), party, player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        BOUTING_PARTIES.clear();
        MinecraftServer server = event.getServer();
        Set<String> liveTeams = new HashSet<>();
        for (PartySavedData.PartyInstance party : PartySavedData.get(server).getParties()) {
            liveTeams.add(getTeamName(party.getPartyId()));
        }
        Scoreboard scoreboard = server.getScoreboard();
        for (PlayerTeam team : new ArrayList<>(scoreboard.getPlayerTeams())) {
            if (team.getName().startsWith(TEAM_PREFIX) && !liveTeams.contains(team.getName())) {
                scoreboard.removePlayerTeam(team);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BOUTING_PARTIES.clear();
    }

    private enum LeaderChange { CHANGED, RETURNED, DISCONNECTED }

    private static boolean refreshLeadership(MinecraftServer server, PartySavedData.PartyInstance party,
                                             UUID excluded, UUID previousLeader, LeaderChange reason) {
        UUID next = party.getOwnerId();
        if (!isOnline(server, next, excluded)) {
            next = null;
            for (UUID memberId : party.getMembers()) {
                if (isOnline(server, memberId, excluded)) {
                    next = memberId;
                    break;
                }
            }
            if (next == null) next = party.getOwnerId();
        }

        if (next.equals(party.getLeaderId()) && next.equals(previousLeader)) return false;
        party.setLeaderId(next);
        PartySavedData.get(server).setDirty();
        if (next.equals(previousLeader)) return true;

        ServerPlayer newLeader = server.getPlayerList().getPlayer(next);
        if (newLeader == null || (excluded != null && excluded.equals(next))) return true;

        String name = newLeader.getGameProfile().getName();
        Component message;
        if (reason == LeaderChange.DISCONNECTED) {
            message = Component.translatable("quest.dmz.party.leader.transferred", name);
        } else if (reason == LeaderChange.RETURNED && next.equals(party.getOwnerId())) {
            message = Component.translatable("quest.dmz.party.leader.returned", name);
        } else {
            message = Component.translatable("quest.dmz.party.leader.changed", name);
        }
        for (UUID memberId : party.getMembers()) {
            if (excluded != null && excluded.equals(memberId)) continue;
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) member.sendSystemMessage(message.copy().withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    private static boolean isOnline(MinecraftServer server, UUID playerId, UUID excluded) {
        if (playerId == null || (excluded != null && excluded.equals(playerId))) return false;
        return server.getPlayerList().getPlayer(playerId) != null;
    }

    private static void joinParty(ServerPlayer leader, PartySavedData.PartyInstance party, ServerPlayer member) {
        MinecraftServer server = leader.getServer();
        PartySavedData data = PartySavedData.get(server);

        PlayerQuestData memberData = getQuestData(member);
        memberData.setDifficulty(getQuestData(leader).getDifficulty());
        memberData.setDifficultyChosen(true);
        data.addPlayerToParty(party.getPartyId(), member.getUUID());

        addToMinecraftTeam(server, party.getPartyId(), member);
        applyFriendlyFire(server, party);

        syncPartyToOnlineMembers(server, party);
    }

    private static void syncPartyToOnlineMembers(MinecraftServer server, PartySavedData.PartyInstance party) {
        syncPartyToOnlineMembers(server, party, null);
    }

    private static void syncPartyToOnlineMembers(MinecraftServer server, PartySavedData.PartyInstance party, UUID excluded) {
        List<UUID> memberIds = party.getMembers();
        for (UUID id : memberIds) {
            if (excluded != null && excluded.equals(id)) continue;
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) {
                getQuestData(member).setPartyState(party.getPartyId(), party.getLeaderId(), party.getOwnerId(), memberIds, party.isPvpEnabled());
                syncSelf(member);
            }
        }
    }

    private static void syncSelf(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
    }

    private static PlayerQuestData getQuestData(Player player) {
        StatsData data = getStatsData(player);
        if (data == null) throw new IllegalStateException("Missing stats capability for player " + player.getGameProfile().getName());
        return data.getPlayerQuestData();
    }

    private static StatsData getStatsData(Player player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
    }

    public static class PendingInvite {
        @Getter private final UUID inviterUUID;
        @Getter private final String teamName;
        @Getter private final UUID partyLeaderId;
        @Getter private final String inviterName;
        private final long expiresAtMs;
        @Getter private final Difficulty partyDifficulty;

        public PendingInvite(UUID inviterUUID, String teamName, UUID partyLeaderId, String inviterName, long expiresAtMs, Difficulty partyDifficulty) {
            this.inviterUUID = inviterUUID;
            this.teamName = teamName;
            this.partyLeaderId = partyLeaderId;
            this.inviterName = inviterName == null ? "" : inviterName;
            this.expiresAtMs = expiresAtMs;
            this.partyDifficulty = partyDifficulty == null ? Difficulty.NORMAL : partyDifficulty;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }

        public UUID getPartyId() {
            try {
                return teamName == null || teamName.isBlank() ? null : UUID.fromString(teamName);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public enum InviteRequestResult {
        INVITED, SUGGESTED, PARTY_FULL, ALREADY_IN_PARTY, NO_PERMISSION, LEVEL_GAP, CANNOT_INVITE_SELF,
        TOURNAMENT_ACTIVE, DIFFICULTY_TOO_LOW
    }

    public enum InviteAcceptResult {
        SUCCESS, EXPIRED, PARTY_FULL, INVALID, LEVEL_GAP, DIFFICULTY_TOO_LOW, DIFFICULTY_CONFIRM_REQUIRED,
        TOURNAMENT_ACTIVE, LEADER_OFFLINE, PARTY_DISBANDED
    }

    public enum KickResult {
        SUCCESS, NOT_IN_PARTY, NOT_LEADER, SELF, TARGET_NOT_IN_PARTY, OWNER, TOURNAMENT_ACTIVE
    }
}
