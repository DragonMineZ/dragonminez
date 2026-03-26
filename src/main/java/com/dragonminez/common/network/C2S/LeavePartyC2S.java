package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class LeavePartyC2S {

    public LeavePartyC2S() {
    }

    public LeavePartyC2S(FriendlyByteBuf ignored) {
    }

    public void encode(FriendlyByteBuf ignored) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!PartyManager.isInParty(player)) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.leave.solo")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            boolean leaderLeaving = PartyManager.isPartyLeader(player);
            List<ServerPlayer> members = PartyManager.getAllPartyMembers(player);
            PartyManager.leaveParty(player);

            if (leaderLeaving) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.disbanded.self")
                        .withStyle(ChatFormatting.YELLOW));
                for (ServerPlayer member : members) {
                    if (member.equals(player)) continue;
                    member.sendSystemMessage(Component.translatable("quest.dmz.party.disbanded.other", player.getName())
                            .withStyle(ChatFormatting.YELLOW));
                }
            } else {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.left")
                        .withStyle(ChatFormatting.YELLOW));
                for (ServerPlayer member : members) {
                    if (!member.equals(player)) {
                        member.sendSystemMessage(Component.translatable("quest.dmz.party.player.left", player.getName())
                                .withStyle(ChatFormatting.YELLOW));
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
