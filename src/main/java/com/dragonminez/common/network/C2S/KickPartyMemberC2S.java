package com.dragonminez.common.network.C2S;

import com.dragonminez.common.quest.PartyFeedback;
import com.dragonminez.server.commands.DMZPermissions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class KickPartyMemberC2S {
	private final UUID targetId;

	public KickPartyMemberC2S(UUID targetId) {
		this.targetId = targetId;
	}

	public KickPartyMemberC2S(FriendlyByteBuf buffer) {
		this.targetId = buffer.readUUID();
	}

	public void encode(FriendlyByteBuf buffer) {
		buffer.writeUUID(targetId);
	}

	public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || targetId == null) return;
			if (!DMZPermissions.hasPermission(player.createCommandSourceStack(), DMZPermissions.PARTY_USE)) return;
			ServerPlayer online = player.getServer().getPlayerList().getPlayer(targetId);
			String name = online != null
					? online.getGameProfile().getName()
					: player.getServer().getProfileCache().get(targetId).map(profile -> profile.getName()).orElse(targetId.toString());
			PartyFeedback.kick(player, targetId, name);
		});
		context.setPacketHandled(true);
	}
}
