package com.dragonminez.common.network.C2S;

import com.dragonminez.server.events.players.combat.StrikeAttackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StrikeAttackC2S {
	private final int targetId;

	public StrikeAttackC2S(int targetId) {
		this.targetId = targetId;
	}

	public StrikeAttackC2S(FriendlyByteBuf buf) {
		this.targetId = buf.readInt();
	}

	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(this.targetId);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player != null) StrikeAttackHandler.requestStrike(player, targetId);
		});
		ctx.get().setPacketHandled(true);
	}
}

