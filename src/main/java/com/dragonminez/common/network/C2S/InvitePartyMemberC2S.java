package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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

            if (invitee.getUUID().equals(inviter.getUUID())) {
                inviter.sendSystemMessage(Component.translatable("quest.dmz.party.invite.self"));
                return;
            }

            PartyManager.InviteRequestResult result = PartyManager.requestInvite(inviter, invitee);
            switch (result) {
                case INVITED -> inviter.sendSystemMessage(
                        Component.translatable("quest.dmz.party.invite.sent", invitee.getGameProfile().getName()));
                case ALREADY_IN_PARTY -> inviter.sendSystemMessage(
                        Component.translatable("quest.dmz.party.invite.same_party"));
                case NO_PERMISSION -> inviter.sendSystemMessage(
                        Component.translatable("quest.dmz.party.invite.leader_only"));
                case LEVEL_GAP -> inviter.sendSystemMessage(
                        Component.translatable("quest.dmz.party.invite.level_gap"));
                case PARTY_FULL -> inviter.sendSystemMessage(
                        Component.translatable("quest.dmz.party.invite.party_full"));
                default -> {
                }
            }
        });
        context.setPacketHandled(true);
    }
}
