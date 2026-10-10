package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyFeedback;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.server.commands.DMZPermissions;
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

    public static boolean tryConsumeCooldown(ServerPlayer inviter) {
        long now = inviter.level().getGameTime();
        Long lastInvite = LAST_INVITE_TICK.get(inviter.getUUID());
        if (lastInvite != null && now >= lastInvite && now - lastInvite < INVITE_COOLDOWN_TICKS) return false;
        LAST_INVITE_TICK.put(inviter.getUUID(), now);
        return true;
    }

    public static void clear(UUID playerId) {
        LAST_INVITE_TICK.remove(playerId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer inviter = context.getSender();
            if (inviter == null) return;
            if (!DMZPermissions.hasPermission(inviter.createCommandSourceStack(), DMZPermissions.PARTY_USE)) return;
            if (!tryConsumeCooldown(inviter)) return;

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
