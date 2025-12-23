package com.dragonminez.server.commands;

import com.dragonminez.common.quest.PartyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class PartyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dmzparty")
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
                        .executes(PartyCommand::listMembers)));
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer inviter)) {
            return 0;
        }

        try {
            ServerPlayer invitee = EntityArgument.getPlayer(context, "player");

            if (inviter.equals(invitee)) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.self")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            PartyManager.sendInvite(inviter, invitee);

            inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.sent", invitee.getName())
                    .withStyle(ChatFormatting.GREEN));

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

            invitee.sendSystemMessage(Component.translatable("quest.dmz.party.invite.received", inviter.getName())
                    .withStyle(ChatFormatting.YELLOW));
            invitee.sendSystemMessage(Component.literal("[")
                    .append(acceptButton)
                    .append(Component.literal("] ["))
                    .append(rejectButton)
                    .append(Component.literal("]")));

            return 1;
        } catch (Exception e) {
            inviter.sendSystemMessage(Component.literal("Error: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (invite.isExpired()) {
            PartyManager.rejectInvite(player);
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.expired")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (PartyManager.acceptInvite(player)) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.joined")
                    .withStyle(ChatFormatting.GREEN));

            ServerPlayer inviter = player.getServer().getPlayerList().getPlayer(invite.getInviterUUID());
            if (inviter != null) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.player.joined", player.getName())
                        .withStyle(ChatFormatting.GREEN));
            }
            return 1;
        }

        return 0;
    }

    private static int rejectInvite(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player);
        if (invite == null) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyManager.rejectInvite(player);
        player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.rejected")
                .withStyle(ChatFormatting.YELLOW));

        ServerPlayer inviter = player.getServer().getPlayerList().getPlayer(invite.getInviterUUID());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.translatable("quest.dmz.party.player.rejected", player.getName())
                    .withStyle(ChatFormatting.YELLOW));
        }

        return 1;
    }

    private static int leaveParty(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        List<ServerPlayer> members = PartyManager.getAllPartyMembers(player);
        if (members.size() <= 1) {
            player.sendSystemMessage(Component.translatable("quest.dmz.party.leave.solo")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        PartyManager.leaveParty(player);
        player.sendSystemMessage(Component.translatable("quest.dmz.party.left")
                .withStyle(ChatFormatting.YELLOW));

        for (ServerPlayer member : members) {
            if (!member.equals(player)) {
                member.sendSystemMessage(Component.translatable("quest.dmz.party.player.left", player.getName())
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        return 1;
    }

    private static int listMembers(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        List<ServerPlayer> members = PartyManager.getAllPartyMembers(player);
        player.sendSystemMessage(Component.translatable("quest.dmz.party.list.header")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        for (ServerPlayer member : members) {
            player.sendSystemMessage(Component.literal("  - ")
                    .append(member.getName())
                    .withStyle(ChatFormatting.GREEN));
        }

        return 1;
    }
}

