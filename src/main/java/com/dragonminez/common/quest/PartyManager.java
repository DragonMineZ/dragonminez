package com.dragonminez.common.quest;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.PartyInviteToastS2C;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.data.PartySavedData;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class PartyManager {
    private static final long INVITE_DURATION_MS = 60_000L;

    private PartyManager() {}

    public static UUID getOrCreateParty(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party != null) {
            return party.getPartyId();
        }
        party = data.createParty(player.getUUID());
        syncPartyToOnlineMembers(player.getServer(), party);
        return party.getPartyId();
    }

    public static UUID getPartyId(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        return party != null ? party.getPartyId() : null;
    }

    public static boolean isInParty(ServerPlayer player) {
        return getPartyId(player) != null;
    }

    public static boolean isPartyLeader(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        return party != null && party.getLeaderId().equals(player.getUUID());
    }

    public static boolean canInvitePlayers(ServerPlayer player) {
        return !isInParty(player) || isPartyLeader(player);
    }

    public static boolean canClaimSharedRewards(ServerPlayer player) {
        return !isInParty(player) || isPartyLeader(player);
    }

    public static boolean areInSameParty(Player p1, Player p2) {
        StatsData data1 = getStatsData(p1);
        StatsData data2 = getStatsData(p2);
        if (data1 == null || data2 == null) return false;

        UUID party1 = data1.getPlayerQuestData().getActivePartyId();
        UUID party2 = data2.getPlayerQuestData().getActivePartyId();

        return party1 != null && party1.equals(party2);
    }

    public static boolean isPartyPvpEnabled(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PartySavedData data = PartySavedData.get(serverPlayer.getServer());
            PartySavedData.PartyInstance party = data.getPartyOf(serverPlayer.getUUID());
            return party != null && party.isPvpEnabled();
        }
        return false;
    }

    public static void togglePartyPvp(ServerPlayer leader) {
        PartySavedData data = PartySavedData.get(leader.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(leader.getUUID());
        if (party != null && party.getLeaderId().equals(leader.getUUID())) {
            party.setPvpEnabled(!party.isPvpEnabled());
            data.setDirty();

            String state = party.isPvpEnabled() ? "✓" : "✕";
            ChatFormatting color = party.isPvpEnabled() ? ChatFormatting.RED : ChatFormatting.GREEN;

            for (ServerPlayer member : getAllPartyMembers(leader)) {
                member.sendSystemMessage(Component.translatable("quest.dmz.party.pvp.toggled", state).withStyle(color));
            }
        }
    }

    public static ServerPlayer getPartyLeader(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party == null) return null;
        return player.getServer().getPlayerList().getPlayer(party.getLeaderId());
    }

    public static List<ServerPlayer> getAllPartyMembers(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party == null) {
            return Collections.singletonList(player);
        }

        List<ServerPlayer> members = new ArrayList<>();
        for (UUID memberId : party.getMembers()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
            if (member != null && !members.contains(member)) {
                members.add(member);
            }
        }

        if (members.isEmpty()) {
            members.add(player);
        }
        return members;
    }

    public static InviteRequestResult requestInvite(ServerPlayer inviter, ServerPlayer invitee) {
        if (isInParty(invitee)) return InviteRequestResult.ALREADY_IN_PARTY;

        int maxMembers = ConfigManager.getServerConfig().getGameplay().getPartyMaxMembers();
        if (maxMembers != -1) {
            if (isInParty(inviter) && getAllPartyMembers(inviter).size() >= maxMembers) return InviteRequestResult.PARTY_FULL;
            else if (!isInParty(inviter) && maxMembers < 2) return InviteRequestResult.PARTY_FULL;
        }

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

    public static InviteAcceptResult acceptInvite(ServerPlayer invitee) {
        PlayerQuestData inviteeQuestData = getQuestData(invitee);
        PlayerQuestData.PartyInviteData invite = inviteeQuestData.getPendingPartyInviteData();

        if (invite == null) return InviteAcceptResult.INVALID;

        if (invite.isExpired()) {
            inviteeQuestData.clearPendingPartyInvite();
            syncSelf(invitee);
            return InviteAcceptResult.EXPIRED;
        }

        ServerPlayer leader = invitee.getServer().getPlayerList().getPlayer(invite.getPartyLeaderId());
        if (leader == null || !isPartyLeader(leader) || !Objects.equals(getPartyId(leader), invite.getPartyId())) {
            inviteeQuestData.clearPendingPartyInvite();
            syncSelf(invitee);
            return InviteAcceptResult.INVALID;
        }

        int maxMembers = ConfigManager.getServerConfig().getGameplay().getPartyMaxMembers();
        if (maxMembers != -1 && getAllPartyMembers(leader).size() >= maxMembers) {
            inviteeQuestData.clearPendingPartyInvite();
            syncSelf(invitee);
            return InviteAcceptResult.PARTY_FULL;
        }

        inviteeQuestData.clearPendingPartyInvite();
        leaveParty(invitee);
        joinLeaderParty(leader, invitee, true);
        return InviteAcceptResult.SUCCESS;
    }

    public static void rejectInvite(ServerPlayer invitee) {
        PlayerQuestData inviteeQuestData = getQuestData(invitee);
        inviteeQuestData.clearPendingPartyInvite();
        syncSelf(invitee);
    }

    public static PendingInvite getPendingInvite(ServerPlayer player) {
        PlayerQuestData.PartyInviteData invite = getQuestData(player).getPendingPartyInviteData();
        if (invite == null) return null;

        return new PendingInvite(
                invite.getInviterUUID(),
                invite.getPartyId() == null ? "" : invite.getPartyId().toString(),
                invite.getPartyLeaderId(),
                invite.getInviterName(),
                invite.getExpiresAtMs()
        );
    }

    public static void leaveParty(ServerPlayer player) {
        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party == null) return;

        boolean isLeader = party.getLeaderId().equals(player.getUUID());

        PlayerQuestData questData = getQuestData(player);
        questData.restorePartyQuestBackup();
        questData.clearPartyQuestBackup();
        questData.clearPartyState();
        syncSelf(player);

        data.removePlayer(player.getUUID());

        if (isLeader && !party.getMembers().isEmpty()) transferLeadership(player, party);
        else syncPartyToOnlineMembers(player.getServer(), party);

    }

    public static void syncPartyQuestState(ServerPlayer sourcePlayer) {
        ServerPlayer leader = resolveQuestController(sourcePlayer);
        if (leader == null) return;

        for (ServerPlayer member : getAllPartyMembers(leader)) {
            if (!member.getUUID().equals(leader.getUUID())) syncQuestProgress(leader, member);
            syncSelf(member);
        }
    }

    public static ServerPlayer resolveQuestController(ServerPlayer player) {
        if (!isInParty(player) || isPartyLeader(player)) return player;
        ServerPlayer leader = getPartyLeader(player);
        return leader != null ? leader : player;
    }

    public static void forceJoinParty(ServerPlayer leader, ServerPlayer member) {
        leaveParty(member);
        joinLeaderParty(leader, member, true);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerQuestData questData = getQuestData(player);
        if (questData.hasPendingPartyInvite()) questData.clearPendingPartyInvite();

        PartySavedData data = PartySavedData.get(player.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());
        if (party == null) return;
        if (party.getLeaderId().equals(player.getUUID())) transferLeadership(player, party);
    }

    private static void transferLeadership(ServerPlayer oldLeader, PartySavedData.PartyInstance party) {
        UUID newLeaderId = null;
        for (UUID id : party.getMembers()) {
            if (id.equals(oldLeader.getUUID())) continue;

            ServerPlayer onlineMember = oldLeader.getServer().getPlayerList().getPlayer(id);
            if (onlineMember != null) {
                newLeaderId = id;
                break;
            }
        }

        if (newLeaderId != null) {
            party.setLeaderId(newLeaderId);
            PartySavedData.get(oldLeader.getServer()).setDirty();
            syncPartyToOnlineMembers(oldLeader.getServer(), party);

            ServerPlayer finalNewLeader = oldLeader.getServer().getPlayerList().getPlayer(newLeaderId);
            String leaderName = finalNewLeader != null ? finalNewLeader.getGameProfile().getName() : "Unknown";

            for (UUID id : party.getMembers()) {
                ServerPlayer member = oldLeader.getServer().getPlayerList().getPlayer(id);
                if (member != null) {
                    member.sendSystemMessage(Component.translatable("quest.dmz.party.leader.transferred", leaderName).withStyle(ChatFormatting.YELLOW));
                }
            }
        }
    }

    private static void joinLeaderParty(ServerPlayer leader, ServerPlayer member, boolean storeBackup) {
        UUID partyId = getOrCreateParty(leader);
        PartySavedData data = PartySavedData.get(leader.getServer());
        PartySavedData.PartyInstance party = data.getPartyOf(leader.getUUID());

        if (storeBackup) {
            getQuestData(member).savePartyQuestBackup();
        }

        syncQuestProgress(leader, member);
        data.addPlayerToParty(partyId, member.getUUID());
        syncPartyToOnlineMembers(leader.getServer(), party);
    }

    private static void syncPartyToOnlineMembers(net.minecraft.server.MinecraftServer server, PartySavedData.PartyInstance party) {
        List<UUID> memberIds = party.getMembers();
        for (UUID id : memberIds) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null) {
                getQuestData(member).setPartyState(party.getPartyId(), party.getLeaderId(), memberIds);
                syncSelf(member);
            }
        }
    }

    private static void syncQuestProgress(ServerPlayer fromPlayer, ServerPlayer toPlayer) {
        StatsData fromData = getStatsData(fromPlayer);
        StatsData toData = getStatsData(toPlayer);
        if (fromData == null || toData == null) return;

        toData.getPlayerQuestData().copyQuestStateFrom(fromData.getPlayerQuestData());
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

    public enum InviteRequestResult {
        INVITED, SUGGESTED, PARTY_FULL, ALREADY_IN_PARTY, NO_PERMISSION
    }

    public enum InviteAcceptResult {
        SUCCESS, EXPIRED, PARTY_FULL, INVALID
    }
}