package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyManager;
import net.minecraft.network.FriendlyByteBuf;
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

            PartyManager.requestInvite(inviter, invitee);
        });
        context.setPacketHandled(true);
    }
}
