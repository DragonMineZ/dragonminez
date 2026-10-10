package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyFeedback;
import com.dragonminez.common.quest.PartyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class InvitePartyMemberC2S {
    private static final Map<UUID, Long> LAST_INVITE_TICK = new ConcurrentHashMap<>();
    private static final long INVITE_COOLDOWN_TICKS = 10L;

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

            long now = inviter.level().getGameTime();
            Long lastInvite = LAST_INVITE_TICK.get(inviter.getUUID());
            if (lastInvite != null && now - lastInvite < INVITE_COOLDOWN_TICKS) return;
            LAST_INVITE_TICK.put(inviter.getUUID(), now);

            ServerPlayer invitee = inviter.getServer().getPlayerList().getPlayer(targetPlayerId);
            if (invitee == null) {
                PartyFeedback.inviteeOffline(inviter);
                return;
            }

            PartyManager.InviteRequestResult result = PartyManager.requestInvite(inviter, invitee);
            PartyFeedback.inviteRequest(inviter, invitee, result);
        });
        context.setPacketHandled(true);
    }
}
