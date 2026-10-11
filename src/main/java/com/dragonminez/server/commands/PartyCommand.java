package com.dragonminez.server.commands;

import com.dragonminez.common.network.C2S.InvitePartyMemberC2S;
import com.dragonminez.common.quest.PartyFeedback;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.server.world.data.PartySavedData;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.UUID;

public class PartyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzparty")
                .executes(PartyCommand::listMembers)
                .then(Commands.literal("invite")
                        .requires(PartyCommand::canUse)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PartyCommand::invitePlayer)))
                .then(Commands.literal("accept")
                        .requires(PartyCommand::canUse)
                        .executes(context -> acceptInvite(context, false, null))
                        .then(Commands.literal("confirm")
                                .executes(context -> acceptInvite(context, true, null))
                                .then(Commands.argument("party", UuidArgument.uuid())
                                        .executes(context -> acceptInvite(context, true, UuidArgument.getUuid(context, "party")))))
                        .then(Commands.argument("party", UuidArgument.uuid())
                                .executes(context -> acceptInvite(context, false, UuidArgument.getUuid(context, "party")))))
                .then(Commands.literal("reject")
                        .requires(PartyCommand::canUse)
                        .executes(context -> rejectInvite(context, null))
                        .then(Commands.argument("party", UuidArgument.uuid())
                                .executes(context -> rejectInvite(context, UuidArgument.getUuid(context, "party")))))
                .then(Commands.literal("leave")
                        .executes(PartyCommand::leaveParty))
                .then(Commands.literal("list")
                        .executes(PartyCommand::listMembers))
                .then(Commands.literal("kick")
                        .requires(PartyCommand::canUse)
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(PartyCommand::kickPlayer)))
                .then(Commands.literal("disband")
                        .requires(PartyCommand::canUse)
                        .executes(PartyCommand::disbandParty))
                .then(Commands.literal("pvp")
                        .requires(PartyCommand::canUse)
                        .executes(PartyCommand::togglePvp)));
    }

    private static boolean canUse(CommandSourceStack source) {
        return DMZPermissions.hasPermission(source, DMZPermissions.PARTY_USE);
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer inviter)) return 0;

        try {
            ServerPlayer invitee = EntityArgument.getPlayer(context, "player");
            if (!InvitePartyMemberC2S.tryConsumeCooldown(inviter)) return 0;
            PartyManager.InviteRequestResult result = PartyManager.requestInvite(inviter, invitee);
            PartyFeedback.inviteRequest(inviter, invitee, result);
            return result == PartyManager.InviteRequestResult.INVITED || result == PartyManager.InviteRequestResult.SUGGESTED ? 1 : 0;
        } catch (Exception e) {
            inviter.sendSystemMessage(Component.translatable("command.dragonminez.party.error", e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> context, boolean confirmedDifficultyChange, UUID partyId) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player, partyId);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none").withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyManager.InviteAcceptResult result = PartyManager.acceptInvite(player, confirmedDifficultyChange, partyId);
        PartyFeedback.inviteAccept(player, invite, result);
        return result == PartyManager.InviteAcceptResult.SUCCESS ? 1 : 0;
    }

    private static int rejectInvite(CommandContext<CommandSourceStack> context, UUID partyId) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player, partyId);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none").withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyManager.rejectInvite(player, partyId);
        player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.rejected").withStyle(ChatFormatting.YELLOW));

        ServerPlayer inviter = player.getServer().getPlayerList().getPlayer(invite.getInviterUUID());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.translatable("quest.dmz.party.player.rejected", player.getName()).withStyle(ChatFormatting.YELLOW));
        }

        return 1;
    }

    private static int leaveParty(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;
        if (!PartyManager.isInParty(player)) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.leave.solo").withStyle(ChatFormatting.RED));
            return 0;
        }
        PartyFeedback.leaveRequested(player);
        return 1;
    }

    private static int listMembers(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        MinecraftServer server = player.getServer();
        PartySavedData data = PartySavedData.get(server);
        PartySavedData.PartyInstance party = data.getPartyOf(player.getUUID());

        player.sendSystemMessage(Component.translatable("quest.dmz.party.list.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        if (party == null) {
            player.sendSystemMessage(Component.literal("  - " + player.getGameProfile().getName() + " ⭐").withStyle(ChatFormatting.GOLD));
            return 1;
        }

        for (UUID memberId : party.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            boolean isOnline = member != null;
            boolean isLeader = party.getLeaderId().equals(memberId);
            boolean isOwner = party.getOwnerId().equals(memberId);

            String name = isOnline ? member.getGameProfile().getName() : server.getProfileCache().get(memberId).map(p -> p.getName()).orElse(memberId.toString());

            String suffix = isLeader ? " ⭐" : (isOwner ? " ☆" : "");
            ChatFormatting color = !isOnline ? ChatFormatting.GRAY : (isLeader ? ChatFormatting.GOLD : ChatFormatting.YELLOW);

            player.sendSystemMessage(Component.literal("  - " + name + suffix).withStyle(color));
        }

        return 1;
    }

    private static int togglePvp(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        if (!PartyManager.isInParty(player)) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.leave.solo").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!PartyManager.isPartyLeader(player)) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.not_leader").withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyManager.togglePartyPvp(player);
        return 1;
    }

    private static int kickPlayer(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        try {
            Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
            if (profiles.size() != 1) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.kick.not_in_party").withStyle(ChatFormatting.RED));
                return 0;
            }
            GameProfile target = profiles.iterator().next();
            PartyFeedback.kick(player, target.getId(), target.getName());
            return 1;
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("command.dragonminez.party.error", e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int disbandParty(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        if (!PartyManager.isInParty(player)) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.leave.solo").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (!PartyManager.isPartyOwner(player)) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.not_owner").withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyFeedback.leaveRequested(player);
        return 1;
    }
}