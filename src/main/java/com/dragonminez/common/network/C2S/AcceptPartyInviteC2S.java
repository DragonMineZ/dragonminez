package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AcceptPartyInviteC2S {

    public AcceptPartyInviteC2S() {
    }

    public AcceptPartyInviteC2S(FriendlyByteBuf ignored) {
    }

    public void encode(FriendlyByteBuf ignored) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player);
            if (invite == null) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            PartyManager.InviteAcceptResult result = PartyManager.acceptInvite(player);
            if (result == PartyManager.InviteAcceptResult.EXPIRED) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.expired")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            if (result == PartyManager.InviteAcceptResult.PARTY_FULL) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.party_full")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            if (result != PartyManager.InviteAcceptResult.SUCCESS) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.invalid")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            player.sendSystemMessage(Component.translatable("quest.dmz.party.joined")
                    .withStyle(ChatFormatting.GREEN));

            ServerPlayer inviter = player.getServer().getPlayerList().getPlayer(invite.getInviterUUID());
            if (inviter != null) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.player.joined", player.getName())
                        .withStyle(ChatFormatting.GREEN));
            }
        });
        context.setPacketHandled(true);
    }
}
