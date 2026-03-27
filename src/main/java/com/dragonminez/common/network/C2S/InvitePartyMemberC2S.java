package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class InvitePartyMemberC2S {
    private final UUID targetPlayerId;

    public InvitePartyMemberC2S(UUID targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public InvitePartyMemberC2S(FriendlyByteBuf buffer) {
        this.targetPlayerId = buffer.readUUID();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(targetPlayerId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer inviter = context.getSender();
            if (inviter == null) return;

            ServerPlayer invitee = inviter.getServer().getPlayerList().getPlayer(targetPlayerId);
            if (invitee == null) return;

            if (inviter.getUUID().equals(invitee.getUUID())) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.self")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            if (!PartyManager.canInvitePlayers(inviter)) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.leader_only")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            UUID inviterParty = PartyManager.getPartyId(inviter);
            UUID inviteeParty = PartyManager.getPartyId(invitee);
            if (inviterParty != null && Objects.equals(inviterParty, inviteeParty)) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.same_party")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            PartyManager.sendInvite(inviter, invitee);

            inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.sent", invitee.getName())
                    .withStyle(ChatFormatting.GREEN));
            invitee.sendSystemMessage(Component.translatable("quest.dmz.party.invite.received", inviter.getName())
                    .withStyle(ChatFormatting.YELLOW));
            invitee.sendSystemMessage(Component.translatable("quest.dmz.party.invite.open_quest")
                    .withStyle(ChatFormatting.GRAY));
        });
        context.setPacketHandled(true);
    }
}
