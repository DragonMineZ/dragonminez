package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyFeedback;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.server.commands.DMZPermissions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class AcceptPartyInviteC2S {

    private final boolean confirmedDifficultyChange;
    private final UUID partyId;

    public AcceptPartyInviteC2S() {
        this(false, null);
    }

    public AcceptPartyInviteC2S(boolean confirmedDifficultyChange) {
        this(confirmedDifficultyChange, null);
    }

    public AcceptPartyInviteC2S(boolean confirmedDifficultyChange, UUID partyId) {
        this.confirmedDifficultyChange = confirmedDifficultyChange;
        this.partyId = partyId;
    }

    public AcceptPartyInviteC2S(FriendlyByteBuf buffer) {
        this.confirmedDifficultyChange = buffer.readBoolean();
        this.partyId = buffer.readBoolean() ? buffer.readUUID() : null;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(confirmedDifficultyChange);
        buffer.writeBoolean(partyId != null);
        if (partyId != null) buffer.writeUUID(partyId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!DMZPermissions.hasPermission(player.createCommandSourceStack(), DMZPermissions.PARTY_USE)) return;

            PartyManager.PendingInvite invite = PartyManager.getPendingInvite(player, partyId);
            if (invite == null) {
                player.sendSystemMessage(Component.translatable("quest.dmz.party.invite.none")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            PartyManager.InviteAcceptResult result = PartyManager.acceptInvite(player, confirmedDifficultyChange, partyId);
            PartyFeedback.inviteAccept(player, invite, result);
        });
        context.setPacketHandled(true);
    }
}
