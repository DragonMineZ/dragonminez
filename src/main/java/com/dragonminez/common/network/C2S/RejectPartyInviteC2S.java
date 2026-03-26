package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RejectPartyInviteC2S {

    public RejectPartyInviteC2S() {
    }

    public RejectPartyInviteC2S(FriendlyByteBuf ignored) {
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

            PartyManager.rejectInvite(player);
            player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.rejected")
                    .withStyle(ChatFormatting.YELLOW));

            ServerPlayer inviter = player.getServer().getPlayerList().getPlayer(invite.getInviterUUID());
            if (inviter != null) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.player.rejected", player.getName())
                        .withStyle(ChatFormatting.YELLOW));
            }
        });
        context.setPacketHandled(true);
    }
}
