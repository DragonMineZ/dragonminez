package com.dragonminez.common.network.C2S;

import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.server.world.worldboss.WorldBossSessions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WorldBossReviveC2S {
	private final int targetEntityId;

	public WorldBossReviveC2S(int targetEntityId) {
		this.targetEntityId = targetEntityId;
	}

	public WorldBossReviveC2S(FriendlyByteBuf buf) {
		this.targetEntityId = buf.readVarInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(targetEntityId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		NetworkEvent.Context context = ctx.get();
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) return;
			if (!PacketRateLimiter.allow(player.getUUID(), "worldboss_revive", player.level().getGameTime(), 10L)) return;
			WorldBossSessions.requestRevive(player, targetEntityId);
		});
		context.setPacketHandled(true);
	}
}
