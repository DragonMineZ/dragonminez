package com.dragonminez.server.commands;

import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.server.world.data.PartySavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class PartyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzparty")
                .requires(source -> DMZPermissions.hasPermission(source, DMZPermissions.PARTY_USE))
                .executes(PartyCommand::listMembers)
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PartyCommand::invitePlayer)))
                .then(Commands.literal("accept")
                        .executes(PartyCommand::acceptInvite))
                .then(Commands.literal("reject")
                        .executes(PartyCommand::rejectInvite))
                .then(Commands.literal("leave")
                        .executes(PartyCommand::leaveParty))
                .then(Commands.literal("list")
                        .executes(PartyCommand::listMembers))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PartyCommand::kickPlayer)))
                .then(Commands.literal("disband")
                        .executes(PartyCommand::disbandParty))
                .then(Commands.literal("pvp")
                        .executes(PartyCommand::togglePvp)));
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer inviter)) return 0;

        try {
            ServerPlayer invitee = EntityArgument.getPlayer(context, "player");
            PartyManager.InviteRequestResult result = PartyManager.requestInvite(inviter, invitee);
            if (result == PartyManager.InviteRequestResult.CANNOT_INVITE_SELF) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.self").withStyle(ChatFormatting.RED));
                return 0;
            }
            if (result != PartyManager.InviteRequestResult.INVITED) return result == PartyManager.InviteRequestResult.SUGGESTED ? 1 : 0;

            invitee.sendSystemMessage(Component.translatable("quest.dmz.party.invite.received", inviter.getName()));
            Component acceptButton = Component.translatable("quest.dmz.party.invite.accept")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzparty accept"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("quest.dmz.party.invite.accept.hover"))));

            Component rejectButton = Component.translatable("quest.dmz.party.invite.reject")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.RED)
                            .withBold(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dmzparty reject"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("quest.dmz.party.invite.reject.hover"))));

            invitee.sendSystemMessage(Component.literal("[")
                    .append(acceptButton)
                    .append(Component.literal("] ["))
                    .append(rejectButton)
                    .append(Component.literal("]")));

            return 1;
        } catch (Exception e) {
            inviter.sendSystemMessage(Component.translatable("command.dragonminez.party.error", e.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none").withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyManager.InviteAcceptResult result = PartyManager.acceptInvite(player);
        if (result == PartyManager.InviteAcceptResult.EXPIRED) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.expired").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (result == PartyManager.InviteAcceptResult.PARTY_FULL) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.party_full").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (result == PartyManager.InviteAcceptResult.LEVEL_GAP) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.level_gap").withStyle(ChatFormatting.RED));
            return 0;
        }

        if (result != PartyManager.InviteAcceptResult.SUCCESS) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.invalid").withStyle(ChatFormatting.RED));
            return 0;
        }

        player.sendSystemMessage(Component.translatable("quest.dmz.party.joined").withStyle(ChatFormatting.GREEN));

        ServerPlayer inviter = player.getServer().getPlayerList().getPlayer(invite.getInviterUUID());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.translatable("quest.dmz.party.player.joined", player.getName()).withStyle(ChatFormatting.GREEN));
        }
        return 1;
    }

    private static int rejectInvite(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;

        PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none").withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyManager.rejectInvite(player);
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

        boolean leaderLeaving = PartyManager.isPartyLeader(player);
        List<ServerPlayer> members = PartyManager.getAllPartyMembers(player);
        PartyManager.leaveParty(player, true);

        if (leaderLeaving) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.disbanded.self").withStyle(ChatFormatting.YELLOW));
            for (ServerPlayer member : members) {
                if (!member.equals(player)) {
                    member.sendSystemMessage(Component.translatable("quest.dmz.party.disbanded.other", player.getName()).withStyle(ChatFormatting.YELLOW));
                }
            }
        } else {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.left").withStyle(ChatFormatting.YELLOW));
            for (ServerPlayer member : members) {
                if (!member.equals(player)) {
                    member.sendSystemMessage(Component.translatable("quest.dmz.party.player.left", player.getName()).withStyle(ChatFormatting.YELLOW));
                }
            }
        }

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

            String name = isOnline ? member.getGameProfile().getName() : server.getProfileCache().get(memberId).map(p -> p.getName()).orElse(memberId.toString());

            String suffix = isLeader ? " ⭐" : "";
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
            ServerPlayer target = EntityArgument.getPlayer(context, "player");

            if (!PartyManager.isInParty(player)) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.leave.solo").withStyle(ChatFormatting.RED));
                return 0;
            }

            if (!PartyManager.isPartyLeader(player)) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.not_leader").withStyle(ChatFormatting.RED));
                return 0;
            }

            if (player.equals(target)) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.kick.self").withStyle(ChatFormatting.RED));
                return 0;
            }

            if (!PartyManager.areInSameParty(player, target)) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.kick.not_in_party").withStyle(ChatFormatting.RED));
                return 0;
            }

            PartyManager.leaveParty(target);

            player.sendSystemMessage(Component.translatable("quest.dmz.party.kick.success", target.getName()).withStyle(ChatFormatting.GREEN));
            target.sendSystemMessage(Component.translatable("quest.dmz.party.kick.kicked").withStyle(ChatFormatting.RED));

            List<ServerPlayer> members = PartyManager.getAllPartyMembers(player);
            for (ServerPlayer member : members) {
                if (!member.equals(player)) {
                    member.sendSystemMessage(Component.translatable("quest.dmz.party.player.kicked", target.getName()).withStyle(ChatFormatting.YELLOW));
                }
            }

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

        if (!PartyManager.isPartyLeader(player)) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.not_leader").withStyle(ChatFormatting.RED));
            return 0;
        }

        List<ServerPlayer> members = PartyManager.getAllPartyMembers(player);

        for (ServerPlayer member : members) {
            member.sendSystemMessage(Component.translatable("quest.dmz.party.disbanded.self").withStyle(ChatFormatting.YELLOW));
        }

        PartyManager.disbandParty(player);
        return 1;
    }
}