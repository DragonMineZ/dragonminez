package com.dragonminez.common.quest;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.PartyInviteToastS2C;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class PartyManager {
    private static final long INVITE_DURATION_MS = 60_000L;

    private PartyManager() {
    }

    public static UUID getOrCreateParty(ServerPlayer player) {
        PlayerQuestData questData = getQuestData(player);
        if (questData.getActivePartyId() != null) {
            if (questData.getPartyLeaderId() == null) {
                questData.setPartyState(questData.getActivePartyId(), player.getUUID(), List.of(player.getUUID()));
                syncSelf(player);
            }
            return questData.getActivePartyId();
        }

        UUID partyId = UUID.randomUUID();
        questData.setPartyState(partyId, player.getUUID(), List.of(player.getUUID()));
        syncSelf(player);
        return partyId;
    }

    public static UUID getPartyId(ServerPlayer player) {
        return getQuestData(player).getActivePartyId();
    }

    public static boolean isInParty(ServerPlayer player) {
        return getQuestData(player).isInParty();
    }

    public static boolean isPartyLeader(ServerPlayer player) {
        return getQuestData(player).isPartyLeader(player.getUUID());
    }

    public static boolean canInvitePlayers(ServerPlayer player) {
        return !isInParty(player) || isPartyLeader(player);
    }

    public static boolean canClaimSharedRewards(ServerPlayer player) {
        return !isInParty(player) || isPartyLeader(player);
    }

    public static ServerPlayer getPartyLeader(ServerPlayer player) {
        PlayerQuestData questData = getQuestData(player);
        UUID leaderId = questData.getPartyLeaderId();
        if (leaderId == null) return null;
        return player.getServer().getPlayerList().getPlayer(leaderId);
    }

    public static List<ServerPlayer> getAllPartyMembers(ServerPlayer player) {
        PlayerQuestData questData = getQuestData(player);
        if (!questData.isInParty()) {
            return Collections.singletonList(player);
        }

        List<ServerPlayer> members = new ArrayList<>();
        for (UUID memberId : questData.getPartyMemberIds()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
            if (member != null && !members.contains(member)) {
                members.add(member);
            }
        }

        if (members.isEmpty()) {
            members.add(player);
        } else if (!members.contains(player)) {
            members.add(player);
        }

        return members;
    }

    public static void sendInvite(ServerPlayer inviter, ServerPlayer invitee) {
        UUID partyId = getOrCreateParty(inviter);
        PlayerQuestData inviteeQuestData = getQuestData(invitee);
        long expiresAt = System.currentTimeMillis() + INVITE_DURATION_MS;
        inviteeQuestData.setPendingPartyInvite(new PlayerQuestData.PartyInviteData(
                inviter.getUUID(),
                partyId,
                inviter.getUUID(),
                inviter.getGameProfile().getName(),
                expiresAt
        ));
        syncSelf(invitee);
        NetworkHandler.sendToPlayer(new PartyInviteToastS2C(inviter.getGameProfile().getName()), invitee);
    }

    public static boolean acceptInvite(ServerPlayer invitee) {
        PlayerQuestData inviteeQuestData = getQuestData(invitee);
        PlayerQuestData.PartyInviteData invite = inviteeQuestData.getPendingPartyInviteData();
        if (invite == null || invite.isExpired()) {
            inviteeQuestData.clearPendingPartyInvite();
            syncSelf(invitee);
            return false;
        }

        ServerPlayer leader = invitee.getServer().getPlayerList().getPlayer(invite.getPartyLeaderId());
        if (leader == null || !isPartyLeader(leader) || !Objects.equals(getPartyId(leader), invite.getPartyId())) {
            inviteeQuestData.clearPendingPartyInvite();
            syncSelf(invitee);
            return false;
        }

        inviteeQuestData.clearPendingPartyInvite();
        leaveParty(invitee);
        joinLeaderParty(leader, invitee, true);
        return true;
    }

    public static void rejectInvite(ServerPlayer invitee) {
        PlayerQuestData inviteeQuestData = getQuestData(invitee);
        inviteeQuestData.clearPendingPartyInvite();
        syncSelf(invitee);
    }

    public static PendingInvite getPendingInvite(ServerPlayer player) {
        PlayerQuestData.PartyInviteData invite = getQuestData(player).getPendingPartyInviteData();
        if (invite == null) {
            return null;
        }
        return new PendingInvite(
                invite.getInviterUUID(),
                invite.getPartyId() == null ? "" : invite.getPartyId().toString(),
                invite.getPartyLeaderId(),
                invite.getInviterName(),
                invite.getExpiresAtMs()
        );
    }

    public static void leaveParty(ServerPlayer player) {
        if (!isInParty(player)) {
            return;
        }

        if (isPartyLeader(player)) {
            disbandParty(player, true, true);
        } else {
            removeFollowerFromParty(player, true, true);
        }
    }

    public static void syncPartyQuestState(ServerPlayer sourcePlayer) {
        ServerPlayer leader = resolveQuestController(sourcePlayer);
        if (leader == null) {
            return;
        }

        for (ServerPlayer member : getAllPartyMembers(leader)) {
            if (!member.getUUID().equals(leader.getUUID())) {
                syncQuestProgress(leader, member);
            }
            syncSelf(member);
        }
    }

    public static ServerPlayer resolveQuestController(ServerPlayer player) {
        if (!isInParty(player) || isPartyLeader(player)) {
            return player;
        }
        ServerPlayer leader = getPartyLeader(player);
        return leader != null ? leader : player;
    }

    public static void forceJoinParty(ServerPlayer leader, ServerPlayer member) {
        leaveParty(member);
        joinLeaderParty(leader, member, true);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerQuestData questData = getQuestData(player);
        if (questData.hasPendingPartyInvite()) {
            questData.clearPendingPartyInvite();
        }

        if (!questData.isInParty()) {
            return;
        }

        if (questData.isPartyLeader(player.getUUID())) {
            disbandParty(player, true, false);
        } else {
            removeFollowerFromParty(player, true, false);
        }
    }

    private static void joinLeaderParty(ServerPlayer leader, ServerPlayer member, boolean storeBackup) {
        UUID partyId = getOrCreateParty(leader);
        PlayerQuestData memberQuestData = getQuestData(member);

        if (storeBackup) {
            memberQuestData.savePartyQuestBackup();
        }

        syncQuestProgress(leader, member);

		Set<UUID> memberIds = new LinkedHashSet<>(getQuestData(leader).getPartyMemberIds());
        memberIds.add(member.getUUID());

        updatePartyMembers(partyId, leader.getUUID(), memberIds, resolveOnlinePlayers(leader, memberIds));
    }

    private static void removeFollowerFromParty(ServerPlayer member, boolean restoreBackup, boolean syncLeavingPlayer) {
        PlayerQuestData memberQuestData = getQuestData(member);
        UUID partyId = memberQuestData.getActivePartyId();
        UUID leaderId = memberQuestData.getPartyLeaderId();
        if (partyId == null || leaderId == null) {
            memberQuestData.clearPartyState();
            if (restoreBackup) {
                memberQuestData.restorePartyQuestBackup();
                memberQuestData.clearPartyQuestBackup();
            }
            if (syncLeavingPlayer) {
                syncSelf(member);
            }
            return;
        }

        Set<UUID> remainingIds = new LinkedHashSet<>(memberQuestData.getPartyMemberIds());
        remainingIds.remove(member.getUUID());
        if (remainingIds.isEmpty()) {
            remainingIds.add(leaderId);
        }

        if (restoreBackup) {
            memberQuestData.restorePartyQuestBackup();
        }
        memberQuestData.clearPartyQuestBackup();
        memberQuestData.clearPartyState();

        if (syncLeavingPlayer) {
            syncSelf(member);
        }

        List<ServerPlayer> remainingPlayers = resolveOnlinePlayers(member, remainingIds);
        if (!remainingPlayers.isEmpty()) {
            updatePartyMembers(partyId, leaderId, remainingIds, remainingPlayers);
        }
    }

    private static void disbandParty(ServerPlayer leader, boolean keepLeaderQuestState, boolean syncLeader) {
        PlayerQuestData leaderQuestData = getQuestData(leader);
        UUID partyId = leaderQuestData.getActivePartyId();
        if (partyId == null) {
            if (syncLeader) {
                syncSelf(leader);
            }
            return;
        }

        List<ServerPlayer> members = getAllPartyMembers(leader);
        for (ServerPlayer member : members) {
            if (member.getUUID().equals(leader.getUUID())) {
                continue;
            }

            PlayerQuestData memberQuestData = getQuestData(member);
            memberQuestData.restorePartyQuestBackup();
            memberQuestData.clearPartyQuestBackup();
            memberQuestData.clearPartyState();
            syncSelf(member);
        }

        if (!keepLeaderQuestState) {
            leaderQuestData.restorePartyQuestBackup();
        }
        leaderQuestData.clearPartyQuestBackup();
        leaderQuestData.clearPartyState();

        if (syncLeader) {
            syncSelf(leader);
        }
    }

    private static void updatePartyMembers(UUID partyId, UUID leaderId, Collection<UUID> memberIds, List<ServerPlayer> members) {
        List<UUID> orderedIds = new ArrayList<>();
        if (leaderId != null) {
            orderedIds.add(leaderId);
        }
        for (UUID memberId : memberIds) {
            if (memberId != null && !orderedIds.contains(memberId)) {
                orderedIds.add(memberId);
            }
        }

        for (ServerPlayer member : members) {
            PlayerQuestData questData = getQuestData(member);
            questData.setPartyState(partyId, leaderId, orderedIds);
            syncSelf(member);
        }
    }

    private static List<ServerPlayer> resolveOnlinePlayers(ServerPlayer referencePlayer, Collection<UUID> memberIds) {
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID memberId : memberIds) {
            ServerPlayer member = referencePlayer.getServer().getPlayerList().getPlayer(memberId);
            if (member != null && !players.contains(member)) {
                players.add(member);
            }
        }
        return players;
    }

    private static void syncQuestProgress(ServerPlayer fromPlayer, ServerPlayer toPlayer) {
        StatsData fromData = getStatsData(fromPlayer);
        StatsData toData = getStatsData(toPlayer);
        if (fromData == null || toData == null) {
            return;
        }

        toData.getPlayerQuestData().copyQuestStateFrom(fromData.getPlayerQuestData());
    }

    private static void syncSelf(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
    }

    private static PlayerQuestData getQuestData(ServerPlayer player) {
        StatsData data = getStatsData(player);
        if (data == null) {
            throw new IllegalStateException("Missing stats capability for player " + player.getGameProfile().getName());
        }
        return data.getPlayerQuestData();
    }

    private static StatsData getStatsData(ServerPlayer player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
    }

    public static class PendingInvite {
        @Getter
        private final UUID inviterUUID;
        @Getter
        private final String teamName;
        @Getter
        private final UUID partyLeaderId;
        @Getter
        private final String inviterName;
        private final long expiresAtMs;

        public PendingInvite(UUID inviterUUID, String teamName, UUID partyLeaderId, String inviterName, long expiresAtMs) {
            this.inviterUUID = inviterUUID;
            this.teamName = teamName;
            this.partyLeaderId = partyLeaderId;
            this.inviterName = inviterName == null ? "" : inviterName;
            this.expiresAtMs = expiresAtMs;
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
}
